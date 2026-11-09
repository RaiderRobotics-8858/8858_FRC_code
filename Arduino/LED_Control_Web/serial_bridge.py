"""Serial connection and command handling for the LED display controller."""

from __future__ import annotations

import time
from dataclasses import dataclass
from threading import Event, Lock, RLock
from typing import Any, Callable

import serial
from serial.tools import list_ports


BAUDRATE = 115200
DEFAULT_TIMEOUT = 1.0
RESPONSE_WINDOW = 0.6
SUPPORTED_MODES = frozenset({"text", "beam", "rbeam", "rwall"})
SUPPORTED_COLORS = frozenset(
    {"red", "blue", "green", "cyan", "purple", "coral", "white"}
)
BRIGHTNESS_COMMANDS = frozenset({"bup", "bdn"})


@dataclass(frozen=True)
class SerialResponse:
    """Result of one command sent to the controller."""

    command: str
    success: bool
    output: str = ""
    error: str | None = None


@dataclass(frozen=True)
class BatchResult:
    """Result of an ordered batch, including whether it was cancelled."""

    responses: list[SerialResponse]
    cancelled: bool = False


class SerialBridge:
    """Own one serial connection and prevent concurrent command interleaving."""

    def __init__(
        self,
        serial_factory: Callable[..., Any] = serial.Serial,
        baudrate: int = BAUDRATE,
        timeout: float = DEFAULT_TIMEOUT,
        response_window: float = RESPONSE_WINDOW,
    ) -> None:
        self._serial_factory = serial_factory
        self.baudrate = baudrate
        self.timeout = timeout
        self.response_window = response_window
        self._connection: Any | None = None
        self._port: str | None = None
        self._lock = RLock()
        self._batch_lock = Lock()
        self._batch_state_lock = Lock()
        self._batch_cancel = Event()
        self._batch_active = False

    @property
    def connected(self) -> bool:
        return self._connection is not None and bool(self._connection.is_open)

    @property
    def port(self) -> str | None:
        return self._port if self.connected else None

    def state(self) -> dict[str, Any]:
        return {"connected": self.connected, "port": self.port}

    @staticmethod
    def available_ports() -> list[dict[str, str | None]]:
        ports = []
        for port in sorted(list_ports.comports(), key=lambda item: item.device):
            ports.append(
                {
                    "device": port.device,
                    "description": port.description,
                    "hardware_id": port.hwid,
                }
            )
        return ports

    def connect(self, port: str) -> dict[str, Any]:
        port = port.strip()
        if not port:
            raise ValueError("A serial port is required")

        with self._lock:
            self.disconnect()
            try:
                self._connection = self._serial_factory(
                    port, self.baudrate, timeout=self.timeout
                )
            except (serial.SerialException, OSError) as exc:
                self._connection = None
                self._port = None
                raise ConnectionError(f"Could not open {port}: {exc}") from exc
            self._port = port
            return self.state()

    def disconnect(self) -> None:
        with self._lock:
            if self._connection is not None:
                try:
                    self._connection.close()
                finally:
                    self._connection = None
                    self._port = None

    def send(self, command: str) -> SerialResponse:
        self._validate_command(command)
        with self._batch_lock:
            with self._lock:
                return self._send_locked(command)

    def send_batch(self, commands: list[str]) -> BatchResult:
        """Send an ordered group without allowing another request between commands."""
        for command in commands:
            self._validate_command(command)

        with self._batch_lock:
            with self._batch_state_lock:
                self._batch_cancel.clear()
                self._batch_active = True
            try:
                responses = []
                for command in commands:
                    if self._batch_cancel.is_set():
                        break
                    with self._lock:
                        response = self._send_locked(command)
                    responses.append(response)
                    if not response.success:
                        break
                return BatchResult(responses, self._batch_cancel.is_set())
            finally:
                with self._batch_state_lock:
                    self._batch_active = False

    def cancel_batch(self) -> SerialResponse:
        """Stop a running batch before sending the controller cancel command."""
        with self._batch_state_lock:
            if self._batch_active:
                self._batch_cancel.set()
        with self._lock:
            return self._send_locked("cancel")

    def _send_locked(self, command: str) -> SerialResponse:
        if not self.connected:
            return SerialResponse(
                command=command,
                success=False,
                error="The Arduino is not connected",
            )

        try:
            self._connection.write((command + "\n").encode("utf-8"))
            output = self._read_response()
        except (serial.SerialException, OSError) as exc:
            self.disconnect()
            return SerialResponse(
                command=command,
                success=False,
                error=f"Serial communication failed: {exc}",
            )

        if "ERROR :" in output:
            return SerialResponse(
                command=command,
                success=False,
                output=output,
                error=output.strip(),
            )
        return SerialResponse(command=command, success=True, output=output)

    def _read_response(self) -> str:
        output = bytearray()
        deadline = time.monotonic() + self.response_window
        while time.monotonic() < deadline:
            waiting = getattr(self._connection, "in_waiting", 0)
            if waiting:
                output.extend(self._connection.read(waiting))
            else:
                time.sleep(0.01)
        return output.decode("utf-8", errors="replace").strip()

    @staticmethod
    def _validate_command(command: str) -> None:
        if not isinstance(command, str) or not command.strip():
            raise ValueError("Command must be a non-empty string")
        if "\r" in command or "\n" in command:
            raise ValueError("Commands cannot contain newlines")
