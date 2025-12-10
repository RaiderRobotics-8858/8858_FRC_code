#include <FastLED.h>

// LED strip configuration
#define NUM_LEDS    300
#define NUM_ROWS    5
#define FIFO_SIZE   500

#define LED_PIN0 9
#define LED_PIN1 10
#define LED_PIN2 11
#define LED_PIN3 12
#define LED_PIN4 13

CRGB leds0[NUM_LEDS];
CRGB leds1[NUM_LEDS];
CRGB leds2[NUM_LEDS];
CRGB leds3[NUM_LEDS];
CRGB leds4[NUM_LEDS];
CRGB* leds[NUM_ROWS] = {
    leds0,
    leds1,
    leds2,
    leds3,
    leds4
};

bool rainbow_row_en[NUM_ROWS] = {
    true,   // Strip 0
    true,  // Strip 1
    true,  // Strip 2
    true,  // Strip 3
    true   // Strip 4
};

unsigned long last_LEDupdate[NUM_ROWS];

int boot_time_s = 10;

/**
  * This is a command that can be run to clear the Serial Monitor feed
  */
void clearscreen(){
    Serial.println("\033[2J");
};

void printTime(){
    Serial.print("INFO : Time = ");
    Serial.print(millis() / 1000.0);
    Serial.println(" seconds");
}

void setup() {
    Serial.begin(115200);
    while(!Serial);

    clearscreen();

    for (int i = 0; i < NUM_ROWS; i++){
        // FastLED.addLeds<WS2812, led_gpio[i], GRB>(leds[i], NUM_LEDS);
    }
    FastLED.addLeds<WS2812, LED_PIN0, GRB>(leds[0], NUM_LEDS);
    FastLED.addLeds<WS2812, LED_PIN1, GRB>(leds[1], NUM_LEDS);
    FastLED.addLeds<WS2812, LED_PIN2, GRB>(leds[2], NUM_LEDS);
    FastLED.addLeds<WS2812, LED_PIN3, GRB>(leds[3], NUM_LEDS);
    FastLED.addLeds<WS2812, LED_PIN4, GRB>(leds[4], NUM_LEDS);

    FastLED.setBrightness(20);

    unsigned long finish_time = millis() + (1000 * boot_time_s);
    bool boot_done = false;
    while(!boot_done){
        boot_done = boot_animation(finish_time);
    }

    Serial.println("INFO : Entering main loop");
    printTime();
    beam_sync();
}

// Enumerated type to select a display mode
enum disp_mode {
    RBEAM,  // Rainbow Beam
    RWALL,  // Rainbow Wall
    TEXT,   // Text mode
    BEAM    // Beam mode
};
disp_mode mode_sel = BEAM;

CRGB color_dec[] = {
    CRGB::White, // default color
    CRGB::AliceBlue,
    CRGB::Amethyst,
    CRGB::AntiqueWhite,
    CRGB::Aqua,
    CRGB::Aquamarine,
    CRGB::Azure,
    CRGB::Beige,
    CRGB::Bisque,
    CRGB::Black,
    CRGB::BlanchedAlmond,
    CRGB::Blue,
    CRGB::BlueViolet,
    CRGB::Brown,
    CRGB::BurlyWood,
    CRGB::CadetBlue,
    CRGB::Chartreuse,
    CRGB::Chocolate,
    CRGB::Coral,
    CRGB::CornflowerBlue,
    CRGB::Cornsilk,
    CRGB::Crimson,
    CRGB::Cyan,
    CRGB::DarkBlue,
    CRGB::DarkCyan,
    CRGB::DarkGoldenrod,
    CRGB::DarkGray,
    CRGB::DarkGrey,
    CRGB::DarkGreen,
    CRGB::DarkKhaki,
    CRGB::DarkMagenta,
    CRGB::DarkOliveGreen,
    CRGB::DarkOrange,
    CRGB::DarkOrchid,
    CRGB::DarkRed,
    CRGB::DarkSalmon,
    CRGB::DarkSeaGreen,
    CRGB::DarkSlateBlue,
    CRGB::DarkSlateGray,
    CRGB::DarkSlateGrey,
    CRGB::DarkTurquoise,
    CRGB::DarkViolet,
    CRGB::DeepPink,
    CRGB::DeepSkyBlue,
    CRGB::DimGray,
    CRGB::DimGrey,
    CRGB::DodgerBlue,
    CRGB::FireBrick,
    CRGB::FloralWhite,
    CRGB::ForestGreen,
    CRGB::Fuchsia,
    CRGB::Gainsboro,
    CRGB::GhostWhite,
    CRGB::Gold,
    CRGB::Goldenrod,
    CRGB::Gray,
    CRGB::Grey,
    CRGB::Green,
    CRGB::GreenYellow,
    CRGB::Honeydew,
    CRGB::HotPink,
    CRGB::IndianRed,
    CRGB::Indigo,
    CRGB::Ivory,
    CRGB::Khaki,
    CRGB::Lavender,
    CRGB::LavenderBlush,
    CRGB::LawnGreen,
    CRGB::LemonChiffon,
    CRGB::LightBlue,
    CRGB::LightCoral,
    CRGB::LightCyan,
    CRGB::LightGoldenrodYellow,
    CRGB::LightGreen,
    CRGB::LightGrey,
    CRGB::LightPink,
    CRGB::LightSalmon,
    CRGB::LightSeaGreen,
    CRGB::LightSkyBlue,
    CRGB::LightSlateGray,
    CRGB::LightSlateGrey,
    CRGB::LightSteelBlue,
    CRGB::LightYellow,
    CRGB::Lime,
    CRGB::LimeGreen,
    CRGB::Linen,
    CRGB::Magenta,
    CRGB::Maroon,
    CRGB::MediumAquamarine,
    CRGB::MediumBlue,
    CRGB::MediumOrchid,
    CRGB::MediumPurple,
    CRGB::MediumSeaGreen,
    CRGB::MediumSlateBlue,
    CRGB::MediumSpringGreen,
    CRGB::MediumTurquoise,
    CRGB::MediumVioletRed,
    CRGB::MidnightBlue,
    CRGB::MintCream,
    CRGB::MistyRose,
    CRGB::Moccasin,
    CRGB::NavajoWhite,
    CRGB::Navy,
    CRGB::OldLace,
    CRGB::Olive,
    CRGB::OliveDrab,
    CRGB::Orange,
    CRGB::OrangeRed,
    CRGB::Orchid,
    CRGB::PaleGoldenrod,
    CRGB::PaleGreen,
    CRGB::PaleTurquoise,
    CRGB::PaleVioletRed,
    CRGB::PapayaWhip,
    CRGB::PeachPuff,
    CRGB::Peru,
    CRGB::Pink,
    CRGB::Plaid,
    CRGB::Plum,
    CRGB::PowderBlue,
    CRGB::Purple,
    CRGB::Red,
    CRGB::RosyBrown,
    CRGB::RoyalBlue,
    CRGB::SaddleBrown,
    CRGB::Salmon,
    CRGB::SandyBrown,
    CRGB::SeaGreen,
    CRGB::Seashell,
    CRGB::Sienna,
    CRGB::Silver,
    CRGB::SkyBlue,
    CRGB::SlateBlue,
    CRGB::SlateGray,
    CRGB::SlateGrey,
    CRGB::Snow,
    CRGB::SpringGreen,
    CRGB::SteelBlue,
    CRGB::Tan,
    CRGB::Teal,
    CRGB::Thistle,
    CRGB::Tomato,
    CRGB::Turquoise,
    CRGB::Violet,
    CRGB::Wheat,
    CRGB::White,
    CRGB::WhiteSmoke,
    CRGB::Yellow,
    CRGB::YellowGreen
};

enum color_enum {
    DEFAULT_COLOR,
    AliCEBLUE,
    AMETHYST,
    ANTIQUEWHITE,
    AQUA,
    AQUAMARINE,
    AZURE,
    BEIGE,
    BISQUE,
    BLACK,
    BLANCHEDALMOND,
    BLUE,
    BLUEVIOLET,
    BROWN,
    BURLYWOOD,
    CADETBLUE,
    CHARTREUSE,
    CHOCOLATE,
    CORAL,
    CORNFLOWERBLUE,
    CORNSILK,
    CRIMSON,
    CYAN,
    DARKBLUE,
    DARKCYAN,
    DARKGOLDENROD,
    DARKGRAY,
    DARKGREY,
    DARKGREEN,
    DARKKHAKI,
    DARKMAGENTA,
    DARKOLIVEGREEN,
    DARKORANGE,
    DARKORCHID,
    DARKRED,
    DARKSALMON,
    DARKSEAGREEN,
    DARKSLATEBLUE,
    DARKSLATEGRAY,
    DARKSLATEGREY,
    DARKTURQUOISE,
    DARKVIOLET,
    DEEPPINK,
    DEEPSKYBLUE,
    DIMGRAY,
    DIMGREY,
    DODGERBLUE,
    FIREBRICK,
    FLORALWHITE,
    FORESTGREEN,
    FUCHSIA,
    GAINSBORO,
    GHOSTWHITE,
    GOLD,
    GOLDENROD,
    GRAY,
    GREY,
    GREEN,
    GREENYELLOW,
    HONEYDEW,
    HOTPINK,
    INDIANRED,
    INDIGO,
    IVORY,
    KHAKI,
    LAVENDER,
    LAVENDERBLUSH,
    LAWNGREEN,
    LEMONCHIFFON,
    LIGHTBLUE,
    LIGHTCORAL,
    LIGHTCYAN,
    LIGHTGOLDENRODYELLOW,
    LIGHTGREEN,
    LIGHTGREY,
    LIGHTPINK,
    LIGHTSALMON,
    LIGHTSEAGREEN,
    LIGHTSKYBLUE,
    LIGHTSLATEGRAY,
    LIGHTSLATEGREY,
    LIGHTSTEELBLUE,
    LIGHTYELLOW,
    LIME,
    LIMEGREEN,
    LINEN,
    MAGENTA,
    MAROON,
    MEDIUMAQUAMARINE,
    MEDIUMBLUE,
    MEDIUMORCHID,
    MEDIUMPURPLE,
    MEDIUMSEAGREEN,
    MEDIUMSLATEBLUE,
    MEDIUMSPRINGGREEN,
    MEDIUMTURQUOISE,
    MEDIUMVIOLETRED,
    MIDNIGHTBLUE,
    MINTCREAM,
    MISTYROSE,
    MOCCASIN,
    NAVAJOWHITE,
    NAVY,
    OLDLACE,
    OLIVE,
    OLIVEDRAB,
    ORANGE,
    ORANGERED,
    ORCHID,
    PALEGOLDENROD,
    PALEGREEN,
    PALETURQUOISE,
    PALEVIOLETRED,
    PAPAYAWHIP,
    PEACHPUFF,
    PERU,
    PINK,
    PLAID,
    PLUM,
    POWDERBLUE,
    PURPLE,
    RED,
    ROSYBROWN,
    ROYALBLUE,
    SADDLEBROWN,
    SALMON,
    SANDYBROWN,
    SEAGREEN,
    SEASHELL,
    SIENNA,
    SILVER,
    SKYBLUE,
    SLATEBLUE,
    SLATEGRAY,
    SLATEGREY,
    SNOW,
    SPRINGGREEN,
    STEELBLUE,
    TAN,
    TEAL,
    THISTLE,
    TOMATO,
    TURQUOISE,
    VIOLET,
    WHEAT,
    WHITE,
    WHITESMOKE,
    YELLOW,
    YELLOWGREen
};
color_enum color_sel = DEFAULT_COLOR;
color_enum curr_color = DEFAULT_COLOR;
color_enum color_sel_fifo[FIFO_SIZE] = {DEFAULT_COLOR};

// Input Handling
String str_in = "default";
String inputString = "";
String str_in_last = "default";

// String disp_str = "TEAM 8858 BEAST FROM THE EAST - WYLIE EAST HS - WYLIE, TEXAS";
String disp_str = "Entering text mode...";
int disp_speed = 10; // seconds

// Brightness Variables
uint8_t min_brightness = 0x00;
uint8_t max_brightness = 0xFF;
uint8_t bstep = 0x10;
uint8_t brightness = 0xA0;
CRGB system_color = CRGB::Cyan;
bool stringComplete = false;
bool text_mode_serial_feedback = false;
bool text_mode_disp_done = false;
String disp_str_fifo[FIFO_SIZE];
int fifo_cnt = 0;
bool cmd_keyword = false;
bool color_input_en = false;

void loop() {

    if(stringComplete){

        str_in_last = str_in; // save the last input just in case you need to restore

        // Get the input
        str_in = inputString;
        inputString = "";
        stringComplete = false;
        cmd_keyword = false;
        clearscreen();

        if(str_in == "reset"){
            NVIC_SystemReset();
        }

        // Randomizes the positions of all beams
        if(str_in == "rand"){
            Serial.println("INFO : Randomizing Beams");
            beam_randomize();
            rainbow_beam_randomize();
            str_in = str_in_last;
            cmd_keyword = true;
        }

        // Synchronizes the positions of all beams
        if(str_in == "sync") {
            Serial.println("INFO : Synchronizing Beams");
            beam_sync();
            rainbow_beam_sync();
            str_in = str_in_last;
            cmd_keyword = true;
        }

        // Raises Brightness
        if(str_in == "bup"){
            str_in = str_in_last;
            if(brightness < max_brightness - bstep){
                brightness += bstep;
                Serial.print("INFO : Raising Brightness to ");
                Serial.print(brightness);
                Serial.print("/");
                Serial.println(max_brightness);
            } else {
                brightness = max_brightness;
                Serial.print("INFO : Already Max Brightness ");
                Serial.print(brightness);
                Serial.print("/");
                Serial.println(max_brightness);
            }
            FastLED.setBrightness(brightness);
            cmd_keyword = true;
        }

        // Lowers Brightness
        if(str_in == "bdn"){
            str_in = str_in_last;
            if(brightness > min_brightness + bstep){
                brightness -= bstep;
                Serial.print("INFO : Lowering Brightness to ");
                Serial.print(brightness);
                Serial.print("/");
                Serial.println(max_brightness);
            } else {
                brightness = min_brightness;
                Serial.print("INFO : Already Min Brightness ");
                Serial.print(brightness);
                Serial.print("/");
                Serial.println(max_brightness);
            }
            FastLED.setBrightness(brightness);
            cmd_keyword = true;
        }

        if(str_in == "red"){
            system_color = CRGB::Red;
            if(color_input_en){
                color_sel = RED;
            }
            cmd_keyword = true;
        }

        if(str_in == "blue"){
            system_color = CRGB::Blue;
            if(color_input_en){
                color_sel = BLUE;
            }
            cmd_keyword = true;
        }

        if(str_in == "green"){
            system_color = CRGB::Green;
            if(color_input_en){
                color_sel = GREEN;
            }
            cmd_keyword = true;
        }

        if(str_in == "cyan"){
            system_color = CRGB::Cyan;
            if(color_input_en){
                color_sel = CYAN;
            }
            cmd_keyword = true;
        }

        if(str_in == "purple"){
            system_color = CRGB::Purple;
            if(color_input_en){
                color_sel = PURPLE;
            }
            cmd_keyword = true;
        }

        if(str_in == "coral"){
            system_color = CRGB::Coral;
            if(color_input_en){
                color_sel = CORAL;
            }
            cmd_keyword = true;
        }

        if(str_in == "white"){
            system_color = CRGB::White;
            if(color_input_en){
                color_sel = WHITE;
            }
            cmd_keyword = true;
        }

        if(str_in == "rbeam"){
            mode_sel = RBEAM;
            cmd_keyword = true;
        }

        if(str_in == "rwall"){
            mode_sel = RWALL;
            cmd_keyword = true;
        }

        if(str_in == "beam"){
            mode_sel = BEAM;
            cmd_keyword = true;
        }

        if(str_in == "text"){
            mode_sel = TEXT;
            cmd_keyword = true;
        }

        if(str_in == "color"){
            color_input_en = true;
            cmd_keyword = true;
        }

        if(str_in == "fifo chk"){
            Serial.print("FIFO Status: ");
            Serial.print(fifo_cnt);
            Serial.print(" / ");
            Serial.println(FIFO_SIZE);
            Serial.print("(");
            Serial.print(FIFO_SIZE - fifo_cnt);
            Serial.println(" spots remaining)");
            cmd_keyword = true;
        }


        // in text mode, feed through input string to the display function
        if(mode_sel == TEXT){
            if(!cmd_keyword){
                if(text_mode_disp_done){
                    disp_str = str_in;
                    curr_color = color_sel;
                    text_mode_disp_done = false;
                    string_to_led_map(disp_str, disp_speed, CRGB::Black, true, true);
                    if(text_mode_serial_feedback){
                        Serial.print("INFO : Printing \"");
                        Serial.print(disp_str);
                        Serial.println("\"...");
                    }
                } else {
                    if(fifo_cnt < FIFO_SIZE){
                        disp_str_fifo[fifo_cnt] = str_in; // pad FIFO additions with some space to keep them from bunching up on LED display
                        color_sel_fifo[fifo_cnt] = color_sel;
                        if(text_mode_serial_feedback){
                            Serial.print("INFO : Adding \"");
                            Serial.print(str_in);
                            Serial.print("\", color = ");
                            Serial.print(color_sel);
                            Serial.println(" to FIFO...");
                        }
                        color_sel = DEFAULT_COLOR;
                        color_input_en = false;
                        fifo_cnt++;
                    } else {
                        Serial.println("ERROR : current string has not finished, and FIFO is full!");
                    }
                }
            }
        }
    }

    EVERY_N_MILLIS(20){
        if(mode_sel == RBEAM){
            rainbowBeam(2, 40, 0);
            rainbowBeam(2, 40, 1);
            rainbowBeam(2, 40, 2);
            rainbowBeam(2, 40, 3);
            rainbowBeam(2, 40, 4);
        }

        if(mode_sel == RWALL){
            rainbow(5000 / NUM_LEDS, NUM_LEDS / 1, rainbow_row_en);
        }

        if(mode_sel == TEXT){
            if((disp_str != "") && (!text_mode_disp_done)){
                if(string_to_led_map(disp_str, disp_speed, color_dec[curr_color], false, false)){
                    // str_in = "disp_done";
                    if(text_mode_serial_feedback){
                        Serial.print("INFO : Done printing message \"");
                        Serial.print(disp_str);
                        Serial.println("\"");
                    }

                    if(fifo_cnt){
                        disp_str = disp_str_fifo[0];
                        curr_color = color_sel_fifo[0];
                        for(int i = 0; i < fifo_cnt - 1; i++){
                            disp_str_fifo[i] = disp_str_fifo[i + 1];
                            color_sel_fifo[i] = color_sel_fifo[i + 1];
                        }
                        color_sel_fifo[FIFO_SIZE - 1] = DEFAULT_COLOR; // ensure the default color gets into the last position in the FIFO
                        fifo_cnt--;
                        string_to_led_map(disp_str, disp_speed, color_dec[curr_color], true, true);
                        if(text_mode_serial_feedback){
                            Serial.print("INFO : Pulling \"");
                            Serial.print(disp_str);
                            Serial.print("\", color = ");
                            Serial.print(curr_color);
                            Serial.println(" from FIFO...");
                        }
                    } else {
                        text_mode_disp_done = true; // indicate script is ready to accept a new string
                        disp_str = ""; // clear the display string variable
                    }
                }
            } else {
                string_to_led_map(disp_str, disp_speed, CRGB::Black, true, true);
                disp_str = ""; // clear the display string variable
                text_mode_disp_done = true; // indicate script is ready to accept a new string
            }
        }

        if(mode_sel == BEAM){
            beam(2, 10, system_color, 0);
            beam(2, 10, system_color, 1);
            beam(2, 10, system_color, 2);
            beam(2, 10, system_color, 3);
            beam(2, 10, system_color, 4);
        }

        // Tell the LEDs to update once per cycle through the loop()
        FastLED.show();
    }

    while(Serial.available()){
        char inChar = (char)Serial.read();
        if (inChar == '\n') {
            stringComplete = true;
        } else {
            inputString += inChar;
        }
    }
}
