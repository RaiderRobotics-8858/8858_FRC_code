import sys
import threading
import unittest
from pathlib import Path

APP_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(APP_DIR))

import app as app_module
from serial_bridge import SerialBridge


class FakeSerial:
    def __init__(self, port, baudrate, timeout):
        self.port = port
        self.baudrate = baudrate
        self.timeout = timeout
        self.is_open = True
        self.writes = []
        self.responses = []

    @property
    def in_waiting(self):
        return len(self.responses[0]) if self.responses else 0

    def write(self, payload):
        self.writes.append(payload.decode("utf-8"))

    def read(self, amount):
        return self.responses.pop(0)

    def close(self):
        self.is_open = False


class LedControlTests(unittest.TestCase):
    def setUp(self):
        self.fake_serial = None

        def factory(*args, **kwargs):
            self.fake_serial = FakeSerial(*args, **kwargs)
            return self.fake_serial

        self.bridge = SerialBridge(
            serial_factory=factory,
            response_window=0.01,
        )
        app_module.app.config.update(TESTING=True)
        self.client = app_module.app.test_client()
        self.original_bridge = app_module.bridge
        app_module.bridge = self.bridge

    def tearDown(self):
        self.bridge.disconnect()
        app_module.bridge = self.original_bridge

    def test_team_events_are_sorted_for_selection(self):
        original_fetch = app_module.fetch_tba_json
        app_module.fetch_tba_json = lambda endpoint: [
            {"key": "2026-late", "name": "Late Event", "start_date": "2026-04-01"},
            {"key": "2026-early", "name": "Early Event", "start_date": "2026-03-01"},
        ]
        self.addCleanup(lambda: setattr(app_module, "fetch_tba_json", original_fetch))

        response = self.client.post(
            "/team_events", json={"team_num": "8858", "year": "2026"}
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            [event["key"] for event in response.get_json()["events"]],
            ["2026-early", "2026-late"],
        )

    def test_post_scores_matches_driver_sequence(self):
        self.bridge.connect("COM5")
        original_matches = app_module.get_match_details
        original_rank_deltas = app_module.get_match_rank_deltas
        app_module.get_match_details = lambda event_endpoint, match_type: [
            [1, ["8858", "1111", "2222"], 20, ["3333", "4444", "5555"], 15]
        ]
        app_module.get_match_rank_deltas = lambda event_endpoint, match_number, match_type: []
        self.addCleanup(lambda: setattr(app_module, "get_match_details", original_matches))
        self.addCleanup(lambda: setattr(app_module, "get_match_rank_deltas", original_rank_deltas))

        response = self.client.post("/post_scores", json={"event_key": "2026-test"})

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.get_json()["matches"], 1)
        self.assertEqual(
            self.fake_serial.writes,
            [
                "text\n",
                "color\n",
                "green\n",
                "~\n",
                "color\n",
                "white\n",
                "Match 1:\n",
                "color\n",
                "red\n",
                "RED WINS!\n",
                "color\n",
                "red\n",
                "8858, 1111, 2222 - 20\n",
                "color\n",
                "blue\n",
                "15 - 3333, 4444, 5555\n",
            ],
        )

    def test_post_scores_can_include_post_match_rankings(self):
        self.bridge.connect("COM5")
        original_matches = app_module.get_match_details
        original_rank_deltas = app_module.get_match_rank_deltas
        app_module.get_match_details = lambda event_endpoint, match_type: [
            [1, ["8858", "1111", "2222"], 20, ["3333", "4444", "5555"], 15]
        ]
        app_module.get_match_rank_deltas = lambda event_endpoint, match_number, match_type: [
            {"team": "8858", "post_rank": 3, "delta": 1},
            {"team": "1111", "post_rank": 5, "delta": 0},
            {"team": "2222", "post_rank": 8, "delta": -1},
            {"team": "3333", "post_rank": 2, "delta": 1},
            {"team": "4444", "post_rank": 6, "delta": 0},
            {"team": "5555", "post_rank": 9, "delta": -1},
        ]
        self.addCleanup(lambda: setattr(app_module, "get_match_details", original_matches))
        self.addCleanup(lambda: setattr(app_module, "get_match_rank_deltas", original_rank_deltas))

        response = self.client.post(
            "/post_scores", json={"event_key": "2026-test", "show_ranks": True}
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            self.fake_serial.writes[-6:],
            [
                "color\n",
                "white\n",
                "8858 (\x013), 1111 (5), 2222 (\x028)\n",
                "color\n",
                "white\n",
                "3333 (\x012), 4444 (6), 5555 (\x029)\n",
            ],
        )

    def test_cancel_stops_batch_before_later_commands_are_sent(self):
        self.bridge.connect("COM5")
        first_response_started = threading.Event()
        release_first_response = threading.Event()

        class BlockingSerial(FakeSerial):
            @property
            def in_waiting(self):
                first_response_started.set()
                release_first_response.wait(timeout=1)
                return 0

        self.bridge.disconnect()

        def blocking_factory(*args, **kwargs):
            self.fake_serial = BlockingSerial(*args, **kwargs)
            return self.fake_serial

        self.bridge = SerialBridge(
            serial_factory=blocking_factory,
            response_window=0.01,
        )
        self.bridge.connect("COM5")
        batch_result = []

        batch_thread = threading.Thread(
            target=lambda: batch_result.append(
                self.bridge.send_batch(["text", "first", "second"])
            )
        )
        batch_thread.start()
        self.assertTrue(first_response_started.wait(timeout=1))

        cancel_result = []
        cancel_thread = threading.Thread(
            target=lambda: cancel_result.append(self.bridge.cancel_batch())
        )
        cancel_thread.start()
        release_first_response.set()
        batch_thread.join(timeout=1)
        cancel_thread.join(timeout=1)

        self.assertTrue(cancel_result[0].success)
        self.assertEqual(self.fake_serial.writes, ["text\n", "cancel\n"])
        self.assertTrue(batch_result[0].cancelled)
        self.assertEqual([response.command for response in batch_result[0].responses], ["text"])

    def test_text_command_uses_firmware_sequence(self):
        self.bridge.connect("COM5")

        response = self.client.post(
            "/command",
            json={"action": "text", "text": "HELLO", "color": "red"},
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            self.fake_serial.writes,
            ["text\n", "color\n", "red\n", "HELLO\n"],
        )

    def test_text_command_encodes_diamond_for_firmware(self):
        self.bridge.connect("COM5")

        response = self.client.post(
            "/command",
            json={"action": "text", "text": "◇", "color": "green"},
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            self.fake_serial.writes,
            ["text\n", "color\n", "green\n", "~\n"],
        )

    def test_text_command_encodes_arrows_for_firmware(self):
        self.bridge.connect("COM5")

        response = self.client.post(
            "/command",
            json={"action": "text", "text": "↑↓←→", "color": "blue"},
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            self.fake_serial.writes,
            ["text\n", "color\n", "blue\n", "\x01\x02\x03\x04\n"],
        )

    def test_text_rejects_newlines_before_serial_write(self):
        self.bridge.connect("COM5")

        response = self.client.post(
            "/command",
            json={"action": "text", "text": "HELLO\nred", "color": "white"},
        )

        self.assertEqual(response.status_code, 400)
        self.assertIn("single line", response.get_json()["error"])
        self.assertEqual(self.fake_serial.writes, [])

    def test_cancel_stops_display_and_clears_fifo(self):
        self.bridge.connect("COM5")

        response = self.client.post("/command", json={"action": "cancel"})

        self.assertEqual(response.status_code, 200)
        self.assertEqual(self.fake_serial.writes, ["cancel\n"])

    def test_unknown_mode_is_rejected(self):
        self.bridge.connect("COM5")

        response = self.client.post(
            "/command", json={"action": "mode", "mode": "sparkle"}
        )

        self.assertEqual(response.status_code, 400)
        self.assertEqual(self.fake_serial.writes, [])

    def test_bridge_rejects_newline_commands(self):
        with self.assertRaises(ValueError):
            self.bridge.send("beam\nreset")

    def test_firmware_update_requires_connection(self):
        response = self.client.post("/firmware_update")

        self.assertEqual(response.status_code, 400)
        self.assertIn("Connect the Arduino", response.get_json()["error"])

    def test_firmware_update_compiles_then_uploads_and_reconnects(self):
        self.bridge.connect("COM5")
        calls = []
        original_runner = app_module._run_arduino_cli

        def fake_runner(arguments):
            calls.append((arguments, self.bridge.connected))
            return 0, "ok"

        app_module._run_arduino_cli = fake_runner
        self.addCleanup(lambda: setattr(app_module, "_run_arduino_cli", original_runner))

        response = self.client.post("/firmware_update")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            calls,
            [
                (["compile", "--fqbn", app_module.ARDUINO_FQBN, "--build-path", "build", "."], False),
                (
                    [
                        "upload",
                        "--fqbn",
                        app_module.ARDUINO_FQBN,
                        "--build-path",
                        "build",
                        "--port",
                        "COM5",
                        ".",
                    ],
                    False,
                ),
            ],
        )
        self.assertTrue(response.get_json()["connection"]["connected"])
        self.assertEqual(response.get_json()["connection"]["port"], "COM5")

    def test_firmware_update_supports_nano_esp32_board_option(self):
        self.bridge.connect("COM5")
        calls = []
        original_runner = app_module._run_arduino_cli

        def fake_runner(arguments):
            calls.append((arguments, self.bridge.connected))
            return 0, "ok"

        app_module._run_arduino_cli = fake_runner
        self.addCleanup(lambda: setattr(app_module, "_run_arduino_cli", original_runner))

        response = self.client.post("/firmware_update", json={"board": "nano_esp32"})

        expected_fqbn = "arduino:esp32:nano_nora:PinNumbers=byGPIONumber"
        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            calls,
            [
                (["compile", "--fqbn", expected_fqbn, "--build-path", "build", "."], False),
                (
                    [
                        "upload",
                        "--fqbn",
                        expected_fqbn,
                        "--build-path",
                        "build",
                        "--port",
                        "COM5",
                        ".",
                    ],
                    False,
                ),
            ],
        )

    def test_firmware_update_does_not_upload_after_compile_failure(self):
        self.bridge.connect("COM5")
        calls = []
        original_runner = app_module._run_arduino_cli

        def fake_runner(arguments):
            calls.append(arguments)
            return 1, "compile error"

        app_module._run_arduino_cli = fake_runner
        self.addCleanup(lambda: setattr(app_module, "_run_arduino_cli", original_runner))

        response = self.client.post("/firmware_update")

        self.assertEqual(response.status_code, 502)
        self.assertEqual(len(calls), 1)
        self.assertEqual(calls[0][0], "compile")
        self.assertFalse(response.get_json()["success"])
        self.assertTrue(response.get_json()["connection"]["connected"])

    def test_firmware_update_rejects_overlapping_hardware_operation(self):
        acquired = app_module.hardware_operation_lock.acquire(blocking=False)
        self.assertTrue(acquired)
        self.addCleanup(app_module.hardware_operation_lock.release)

        response = self.client.post("/firmware_update")

        self.assertEqual(response.status_code, 409)
        self.assertIn("already in progress", response.get_json()["error"])


if __name__ == "__main__":
    unittest.main()
