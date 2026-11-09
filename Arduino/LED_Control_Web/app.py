"""Local Flask control panel for the 8858 LED display Arduino."""

from __future__ import annotations

import os
import shutil
import subprocess
import threading
from datetime import date
from pathlib import Path
from typing import Any

from flask import Flask, jsonify, render_template, request
from tba_analysis.analysis_func import BASE_URL, fetch_tba_json
from tba_analysis.match_func import get_match_details, get_match_rank_deltas

from serial_bridge import (
    BRIGHTNESS_COMMANDS,
    SUPPORTED_COLORS,
    SUPPORTED_MODES,
    SerialBridge,
    SerialResponse,
)


app = Flask(__name__)
bridge = SerialBridge()
hardware_operation_lock = threading.Lock()

REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
ARDUINO_SKETCH_DIR = REPOSITORY_ROOT / "Arduino" / "LED_Display"
SUPPORTED_BOARDS = {
    "uno_r4": "arduino:renesas_uno:unor4wifi",
    "nano_esp32": "arduino:esp32:nano_nora:PinNumbers=byGPIONumber",
}
ARDUINO_FQBN = os.environ.get(
    "ARDUINO_FQBN",
    SUPPORTED_BOARDS.get(
        os.environ.get("ARDUINO_BOARD", "uno_r4"),
        "arduino:renesas_uno:unor4wifi",
    ),
)
ARDUINO_CLI_TIMEOUT = 180


def _arduino_cli_path() -> str:
    local_app_data = os.environ.get("LOCALAPPDATA")
    if local_app_data:
        installed_cli = Path(local_app_data) / "ArduinoCLI" / "arduino-cli.exe"
        if installed_cli.is_file():
            return str(installed_cli)
    return shutil.which("arduino-cli") or "arduino-cli"


def _run_arduino_cli(arguments: list[str]) -> tuple[int, str]:
    try:
        completed = subprocess.run(
            [_arduino_cli_path(), *arguments],
            cwd=ARDUINO_SKETCH_DIR,
            capture_output=True,
            text=True,
            check=False,
            shell=False,
            timeout=ARDUINO_CLI_TIMEOUT,
        )
    except FileNotFoundError:
        return 127, "Arduino CLI was not found. Install it or add it to PATH."
    except subprocess.TimeoutExpired as exc:
        output = "\n".join(
            part for part in (exc.stdout, exc.stderr) if part
        )
        return 124, f"Arduino CLI timed out after {ARDUINO_CLI_TIMEOUT} seconds.\n{output}".strip()

    output = "\n".join(
        part for part in (completed.stdout, completed.stderr) if part
    ).strip()
    return completed.returncode, output


def _update_firmware(port: str, fqbn: str | None = None) -> dict[str, Any]:
    target_fqbn = fqbn or ARDUINO_FQBN
    compile_code, compile_output = _run_arduino_cli(
        ["compile", "--fqbn", target_fqbn, "--build-path", "build", "."]
    )
    result: dict[str, Any] = {
        "compile": {"success": compile_code == 0, "output": compile_output},
        "upload": None,
        "output": compile_output,
    }
    if compile_code != 0:
        result["success"] = False
        result["error"] = "Firmware compilation failed"
        return result

    upload_code, upload_output = _run_arduino_cli(
        ["upload", "--fqbn", target_fqbn, "--build-path", "build", "--port", port, "."]
    )
    result["upload"] = {"success": upload_code == 0, "output": upload_output}
    result["output"] = "\n".join(
        output for output in (compile_output, upload_output) if output
    )
    result["success"] = upload_code == 0
    if upload_code != 0:
        result["error"] = "Firmware upload failed"
    return result


def _hardware_busy():
    return _error("Another hardware operation is already in progress", 409)


def _payload() -> dict[str, Any]:
    data = request.get_json(silent=True)
    return data if isinstance(data, dict) else request.form.to_dict()


def _error(message: str, status: int = 400):
    return jsonify({"success": False, "error": message, "connection": bridge.state()}), status


def _response_data(
    responses: list[SerialResponse], cancelled: bool = False
) -> dict[str, Any]:
    failed = next((response for response in responses if not response.success), None)
    return {
        "success": failed is None,
        "commands": [response.command for response in responses],
        "output": "\n".join(
            response.output for response in responses if response.output
        ),
        "error": failed.error if failed else None,
        "cancelled": cancelled,
        "connection": bridge.state(),
    }


def _event_endpoint(event_key: str) -> str:
    if not isinstance(event_key, str) or not event_key.strip():
        raise ValueError("Select an event before posting scores")
    if any(character in event_key for character in "/\\?#\r\n"):
        raise ValueError("Invalid event key")
    return f"{BASE_URL}/event/{event_key.strip()}"


def _encode_display_text(text: str) -> str:
    return text.translate(
        str.maketrans(
            {
                "◇": "~",
                "↑": "\x01",
                "↓": "\x02",
                "←": "\x03",
                "→": "\x04",
            }
        )
    )


def _format_team_rank(team: str, rank_info: dict[str, Any]) -> str:
    post_rank = rank_info["post_rank"]
    delta = rank_info.get("delta")
    arrow = ""
    if delta is not None:
        if delta > 0:
            arrow = "↑"
        elif delta < 0:
            arrow = "↓"
    return _encode_display_text(f"{team} ({arrow}{post_rank})")


def _score_commands(
    match: list[Any], rank_deltas: dict[str, dict[str, Any]] | None = None
) -> list[str]:
    match_num, red_teams, red_score, blue_teams, blue_score = match
    commands = []
    commands.extend(["color", "green", "~"])
    commands.extend(["color", "white", f"Match {match_num}:"])
    if red_score > blue_score:
        commands.extend(["color", "red", "RED WINS!"])
    elif blue_score > red_score:
        commands.extend(["color", "blue", "BLUE WINS!"])
    else:
        commands.extend(["color", "purple", "TIE!"])

    commands.extend([
        "color",
        "red",
        f"{red_teams[0]}, {red_teams[1]}, {red_teams[2]} - {red_score}",
        "color",
        "blue",
        f"{blue_score} - {blue_teams[0]}, {blue_teams[1]}, {blue_teams[2]}",
    ])
    if rank_deltas is not None:
        commands.extend([
            "color",
            "white",
            ", ".join(_format_team_rank(team, rank_deltas[team]) for team in red_teams),
            "color",
            "white",
            ", ".join(_format_team_rank(team, rank_deltas[team]) for team in blue_teams),
        ])
    return commands


@app.route("/")
def index():
    return render_template(
        "index.html",
        modes=sorted(SUPPORTED_MODES),
        colors=sorted(SUPPORTED_COLORS),
        current_year=date.today().year,
    )


@app.route("/ping", methods=["GET"])
def ping():
    return jsonify({"success": True, "connection": bridge.state()})


@app.route("/ports", methods=["GET"])
def ports():
    return jsonify(
        {"success": True, "ports": bridge.available_ports(), "connection": bridge.state()}
    )


@app.route("/team_events", methods=["POST"])
def team_events():
    data = _payload()
    team_num = data.get("team_num")
    year = data.get("year")
    if not isinstance(team_num, str) or not team_num.isdigit():
        return _error("Enter a valid FRC team number")
    if not isinstance(year, str) or not year.isdigit() or not 1992 <= int(year) <= 2100:
        return _error("Enter a valid event year")

    try:
        events = fetch_tba_json(f"{BASE_URL}/team/frc{team_num}/events/{year}")
        event_list = [
            {
                "key": event["key"],
                "name": event["name"],
                "start_date": event.get("start_date", ""),
            }
            for event in events
        ]
    except Exception as exc:
        return _error(f"Could not load team events: {exc}", 502)

    event_list.sort(key=lambda event: event["start_date"])
    return jsonify({"success": True, "events": event_list})


@app.route("/post_scores", methods=["POST"])
def post_scores():
    if not hardware_operation_lock.acquire(blocking=False):
        return _hardware_busy()
    try:
        return _post_scores_locked()
    finally:
        hardware_operation_lock.release()


def _post_scores_locked():
    data = _payload()
    try:
        event_endpoint = _event_endpoint(data.get("event_key"))
    except ValueError as exc:
        return _error(str(exc))

    try:
        matches = get_match_details(event_endpoint=event_endpoint, match_type="qm")
    except Exception as exc:
        return _error(f"Could not load event scores: {exc}", 502)

    show_ranks = data.get("show_ranks", False) is True
    commands = ["text"]
    for match in matches:
        rank_deltas = None
        if show_ranks:
            try:
                rank_deltas = {
                    rank["team"]: rank
                    for rank in get_match_rank_deltas(
                        event_endpoint, match[0], match_type="qm"
                    )
                }
            except Exception as exc:
                return _error(f"Could not load post-match rankings: {exc}", 502)
        commands.extend(_score_commands(match, rank_deltas))

    batch = bridge.send_batch(commands)
    result = _response_data(batch.responses, batch.cancelled)
    result["matches"] = len(matches)
    result["posted_commands"] = len(batch.responses)
    if batch.cancelled:
        result["success"] = False
        result["error"] = "Score posting cancelled"
    return jsonify(result), (200 if result["success"] else 502)


@app.route("/connect", methods=["POST"])
def connect():
    if not hardware_operation_lock.acquire(blocking=False):
        return _hardware_busy()
    try:
        return _connect_locked()
    finally:
        hardware_operation_lock.release()


def _connect_locked():
    port = _payload().get("port", "")
    if not isinstance(port, str) or not port.strip():
        return _error("Select a serial port before connecting")
    try:
        state = bridge.connect(port)
    except (ConnectionError, ValueError) as exc:
        return _error(str(exc), 503)
    return jsonify({"success": True, "connection": state})


@app.route("/disconnect", methods=["POST"])
def disconnect():
    if not hardware_operation_lock.acquire(blocking=False):
        return _hardware_busy()
    try:
        bridge.disconnect()
        return jsonify({"success": True, "connection": bridge.state()})
    finally:
        hardware_operation_lock.release()


@app.route("/firmware_update", methods=["POST"])
def firmware_update():
    if not hardware_operation_lock.acquire(blocking=False):
        return _hardware_busy()
    try:
        port = bridge.port
        if port is None:
            return _error("Connect the Arduino before updating its firmware")

        board_arg = _payload().get("board") or _payload().get("fqbn")
        fqbn = SUPPORTED_BOARDS.get(board_arg, board_arg) if isinstance(board_arg, str) and board_arg.strip() else None

        bridge.disconnect()
        try:
            result = _update_firmware(port, fqbn=fqbn)
        finally:
            reconnect_error = None
            try:
                bridge.connect(port)
            except (ConnectionError, ValueError) as exc:
                reconnect_error = str(exc)

        if reconnect_error:
            result["success"] = False
            result["reconnect_error"] = reconnect_error
            result["error"] = (
                f"{result.get('error', 'Firmware update completed')}; "
                f"could not reconnect: {reconnect_error}"
            )
        result["connection"] = bridge.state()
        return jsonify(result), (200 if result["success"] else 502)
    finally:
        hardware_operation_lock.release()


@app.route("/command", methods=["POST"])
def command():
    if not hardware_operation_lock.acquire(blocking=False):
        return _hardware_busy()
    try:
        return _command_locked()
    finally:
        hardware_operation_lock.release()


def _command_locked():
    data = _payload()
    action = data.get("action")
    if action == "mode":
        mode = data.get("mode")
        if mode not in SUPPORTED_MODES:
            return _error("Unsupported display mode")
        responses = [bridge.send(mode)]
    elif action == "color":
        color = data.get("color")
        if color not in SUPPORTED_COLORS:
            return _error("Unsupported display color")
        responses = [bridge.send("color"), bridge.send(color)]
    elif action == "brightness":
        brightness_command = data.get("command")
        if brightness_command not in BRIGHTNESS_COMMANDS:
            return _error("Unsupported brightness command")
        responses = [bridge.send(brightness_command)]
    elif action == "cancel":
        responses = [bridge.cancel_batch()]
    elif action == "text":
        text = data.get("text")
        color = data.get("color")
        if not isinstance(text, str) or not text.strip():
            return _error("Text must not be empty")
        if "\r" in text or "\n" in text:
            return _error("Text must be a single line")
        if len(text) > 200:
            return _error("Text is too long for the display queue")
        if color not in SUPPORTED_COLORS:
            return _error("Select a supported color")
        text = _encode_display_text(text)
        responses = [
            bridge.send("text"),
            bridge.send("color"),
            bridge.send(color),
            bridge.send(text),
        ]
    else:
        return _error("Unknown command action")

    result = _response_data(responses)
    return jsonify(result), (200 if result["success"] else 502)


@app.route("/shutdown", methods=["POST"])
def shutdown():
    if not hardware_operation_lock.acquire(blocking=False):
        return _hardware_busy()
    try:
        bridge.disconnect()
        threading.Timer(0.25, lambda: os._exit(0)).start()
        return jsonify({"success": True})
    finally:
        hardware_operation_lock.release()


if __name__ == "__main__":
    app.run(
        host=os.environ.get("LED_CONTROL_HOST", "127.0.0.1"),
        port=int(os.environ.get("LED_CONTROL_PORT", "5003")),
        debug=True,
        use_reloader=False,
    )
