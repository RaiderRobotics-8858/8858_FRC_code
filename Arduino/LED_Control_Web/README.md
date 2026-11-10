# LED Control Web Interface

This local Flask app controls the 8858 LED display Arduino over USB serial.

## Setup

From the repository root, install the app dependencies into the workspace virtual environment:

```powershell
.venv\Scripts\python.exe -m pip install -r Arduino\LED_Control_Web\requirements.txt
```

The Arduino firmware uses `115200` baud. Close the Arduino serial monitor before connecting because Windows normally gives the serial port to one application at a time.

Event lookup and score posting use The Blue Alliance API through the existing `tba_analysis` package. Configure `TBA_API_KEY` in the environment, or configure the existing `analysis_scripts/tba_analysis/api_key.py` file before using the event controls.

## Run

Use the `Arduino: Run LED Control Web Panel` VS Code task, or run the app directly:

```powershell
.venv\Scripts\python.exe Arduino\LED_Control_Web\app.py
```

Open `http://127.0.0.1:5003`. Click **Refresh ports**, select the Arduino port, and connect. The existing `Arduino: List Serial Boards` task can help identify the port.

The app supports display modes, the firmware's named colors, brightness changes, queued text messages, team event lookup, and qualification-score posting. Score posting follows the same match-by-match red/blue/tie format as `arduino_driver.py`. The app stays bound to localhost because it controls physical hardware and does not provide authentication.

## Update Arduino Firmware

The connection panel includes **Compile & upload firmware** and a board selector dropdown (Arduino Uno R4 WiFi or Arduino Nano ESP32 ABX00092). Before using it:

1. Install Arduino CLI and make sure the appropriate board core (`arduino:renesas_uno` or `arduino:esp32`) and FastLED are installed.
2. Close Arduino IDE, Serial Monitor, or any other program using the Arduino's
   serial port.
3. Refresh ports and connect to the Arduino in this web app.
4. Select the target board model and click **Compile & upload firmware**.

The app compiles the complete `Arduino\LED_Display` sketch for the chosen board model (`arduino:renesas_uno:unor4wifi` or `arduino:esp32:nano_nora:PinNumbers=byGPIONumber`). It uploads only when compilation succeeds, then reconnects to the same port after the board resets.

The manual VS Code tasks remain available as a fallback:

- `Arduino: Setup Board & Core`
- `Arduino: Compile LED Display`
- `Arduino: Upload LED Display`

## Tests

Run the hardware-free tests from this directory:

```powershell
..\..\.venv\Scripts\python.exe -m unittest discover -s tests -v
```
