// Stores position and direction of beam by which strip it's pointing to
int rainbow_beam_pos[NUM_ROWS] = {0};
int rainbow_beam_dir[NUM_ROWS] = {1};
uint8_t rainbow_beam_hue[NUM_ROWS] = {0};

/*
Synchronizes the position and directions of all beams for each strip
 */
void rainbow_beam_sync(){
    for (int i = 0; i < NUM_ROWS; i++){
        rainbow_beam_pos[i] = 0;
        rainbow_beam_dir[i] = 1;
        rainbow_beam_hue[i] = 0;
    }
}

/*
Synchronizes the position and directions of all beams for each strip
 */
void rainbow_beam_randomize(){
    for (int i = 0; i < NUM_ROWS; i++){
        rainbow_beam_pos[i] = random(0, NUM_LEDS);
        rainbow_beam_hue[i] = random(0, 255);
    }
}

/*
Displays a Beam with a rainbow pattern

Can be used in `loop()`
 */
void rainbowBeam(int t_seconds, int width, int strip_num) {

    unsigned long update_rate = (t_seconds*1000) / (NUM_LEDS - width);

    int led_skip = (millis() - last_LEDupdate[strip_num]) / update_rate;

    if(millis() > last_LEDupdate[strip_num] + update_rate){
        last_LEDupdate[strip_num] = millis();
        fill_solid(leds[strip_num], NUM_LEDS, CRGB::Black);
        for (int i = 0; i < width; i++) {
            int led_idx = rainbow_beam_pos[strip_num] + i;
            if(led_idx >= 0 && led_idx < NUM_LEDS) {
                leds[strip_num][led_idx] = CHSV(rainbow_beam_hue[strip_num] + i * 10, 255, brightness);  // Rainbow gradient
            }
        }

        rainbow_beam_pos[strip_num] += rainbow_beam_dir[strip_num] * (1 + led_skip);

        if(rainbow_beam_pos[strip_num] >= NUM_LEDS - width){
            rainbow_beam_dir[strip_num] = -1;
        } else if(rainbow_beam_pos[strip_num] <= 0){
            rainbow_beam_dir[strip_num] = 1;
        }

        rainbow_beam_hue[strip_num] += 4;
    }
}
