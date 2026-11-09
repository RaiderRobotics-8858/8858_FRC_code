# LED Display Test

This is a example of how 8858 programs addresable LEDs to create visual effects in out pit.

## Development in VS Code

This sketch can be compiled and uploaded from VS Code using the Arduino CLI. The
official Arduino IDE is not required after the CLI and its board and library
packages are installed.

### One-time setup

1. Install [Arduino CLI](https://arduino.github.io/arduino-cli/latest/installation/)
   and make sure `arduino-cli` is available in PowerShell.
2. In VS Code, run `Arduino: Setup Board & Core` and select the board core package
   to install (`arduino:renesas_uno`, `arduino:esp32`, or both) along with FastLED.
3. Connect the board by USB and run `Arduino: List Serial Boards` to find its serial port.

### Build and upload

- Run `Arduino: Compile and Upload` to compile and upload in one step.
- Or run `Arduino: Compile LED Display` to verify the sketch, then `Arduino: Upload LED Display`.
- Run `Arduino: Monitor Serial Output` to open the serial monitor at `115200` baud.

### Debug & Launch Configurations

VS Code Run & Debug configurations are available in `.vscode/launch.json`:

- **Python: LED Control Web Panel** — Debugs the web app in `Arduino/LED_Control_Web/app.py`.
- **Cortex-Debug: Arduino Nano ESP32 (Built-in USB JTAG)** — Hardware debug for the Nano ESP32 using its built-in USB JTAG controller via `cortex-debug`, `openocd`, and `xtensa-esp-elf-gdb`.

Keep all `.ino` files in this directory. Arduino CLI must build the directory as
one sketch so that the Arduino preprocessor includes the supporting files.

The sketch waits for the USB serial connection during startup. Open the serial
monitor after uploading if the board appears not to continue booting.

### Serial commands

Commands are entered through the serial monitor at `115200` baud. In text mode:

- `fifo chk` reports the number of pending messages and remaining FIFO capacity.
- `fifo clr` clears all pending messages. The message currently being printed is
   allowed to finish and is not cleared.
- `cancel` stops the current print, clears the display, and removes all pending
   messages from the FIFO.

## Hardware

This code can be easily modified to work with a number of different configurations but the hardware uesd to verify behavior was:

1. Arduino Uno R4 Wifi
   - Nanos can likely handle this job if you have them lying around
2. [WS2182B](https://randomnerdtutorials.com/guide-for-ws2812b-addressable-rgb-led-strip-with-arduino/) Addresable LEDs
   - Some animations require up to 5 LED strips, modify the [`NUM_ROWS`](LED_Display.ino) definition to match the number of parallel LED strips you plan to use
   - functions like [`string_to_led_map()`](string_to_led_map.ino) won't work unless the character bit maps are updated for the new height of the characters
   - Testing was done on LED strips with 300 pixels, modify the [`NUM_LEDS`](LED_Display.ino) definition to match the number of pixels in the LED strip you use, functions automatically scale to this value
3. 5V DC Power Supply
   - Consider how much power your application might use when choosing your supply
   - *Rough Math*: max current draw per LED is roughly 4mA, each Pixel is 3 LEDs (RGB) so ~12mA per Pixel, however you likely won't have EVERY LED on max brightness at any given time.
   - *Rough Math example*: Max current draw for 5 LED strips of 300 pixels each would be:

      `5 * 300 * 12mA = 18000mA -> 18A`

[![Logo of Team 8858 'Beast from the East'](../../images/logos/8858_logo.png)](https://www.thebluealliance.com/team/8858)
