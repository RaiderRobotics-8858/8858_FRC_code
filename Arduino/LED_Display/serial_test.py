import serial
import time
import geocoder

# Replace COM10 with the appropriate port if different
PORT = 'COM10'
BAUDRATE = 115200  # Match this with your device's baud rate
TIMEOUT = 1      # In seconds
g = geocoder.ip('me')

instructions = f"""
text
you are in city:
color
blue
{g.city}, {g.state}
"""

def main():
    try:
        # Open serial port
        with serial.Serial(PORT, BAUDRATE, timeout=TIMEOUT) as ser:
            print(f"Connected to {PORT} at {BAUDRATE} baud.")

            commands = instructions.strip().splitlines()

            for command in commands:
                ser.write((command + '\n').encode('utf-8'))
                time.sleep(0.5)

    except serial.SerialException as e:
        print(f"Serial error: {e}")
    except KeyboardInterrupt:
        print("\nInterrupted by user.")

if __name__ == "__main__":
    main()
