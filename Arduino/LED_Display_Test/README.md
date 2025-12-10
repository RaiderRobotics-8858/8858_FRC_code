# LED Display Test

This is a example of how 8858 programs addresable LEDs to create visual effects in out pit.

## Hardware

This code can be easily modified to work with a number of different configurations but the hardware uesd to verify behavior was:

1. Arduino Uno R4 Wifi
   - Nanos can likely handle this job if you have them lying around
2. [WS2182B](https://randomnerdtutorials.com/guide-for-ws2812b-addressable-rgb-led-strip-with-arduino/) Addresable LEDs
   - Some animations require up to 5 LED strips, modify the [`NUM_ROWS`](LED_Display_Test.ino) definition to match the number of parallel LED strips you plan to use
   - functions like [`string_to_led_map()`](string_to_led_map.ino) won't work unless the character bit maps are updated for the new height of the characters
   - Testing was done on LED strips with 300 pixels, modify the [`NUM_LEDS`](LED_Display_Test.ino) definition to match the number of pixels in the LED strip you use, functions automatically scale to this value
3. 5V DC Power Supply
   - Consider how much power your application might use when choosing your supply
   - *Rough Math*: max current draw per LED is roughly 4mA, each Pixel is 3 LEDs (RGB) so ~12mA per Pixel, however you likely won't have EVERY LED on max brightness at any given time.
   - *Rough Math example*: Max current draw for 5 LED strips of 300 pixels each would be:

      `5 * 300 * 12mA = 18000mA -> 18A`

[![Logo of Team 8858 'Beast from the East'](../../images/logos/8858_logo.png)](https://www.thebluealliance.com/team/8858)
