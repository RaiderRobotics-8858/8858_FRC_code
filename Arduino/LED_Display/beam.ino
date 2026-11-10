// Stores position and direction of beam by which strip it's pointing to
int solid_beam_pos[NUM_ROWS] = {0};
int solid_beam_dir[NUM_ROWS] = {1};

/*
Synchronizes the position and directions of all beams for each strip
 */
void beam_sync(){
    for (int i = 0; i < NUM_ROWS; i++){
        solid_beam_pos[i] = 0;
        solid_beam_dir[i] = 1;
    }
}

/*
Synchronizes the position and directions of all beams for each strip
 */
void beam_randomize(){
    for (int i = 0; i < NUM_ROWS; i++){
        solid_beam_pos[i] = random(0, NUM_LEDS);
    }
}

/*
Displays a Beam of a solid color

Can be used in `loop()`
 */
void beam(int t_seconds, int width, CRGB color, int strip_num) {

    unsigned long update_rate = (t_seconds*1000) / (NUM_LEDS - width);
    int led_skip = (millis() - last_LEDupdate[strip_num]) / update_rate;

    if(millis() > last_LEDupdate[strip_num] + update_rate){
        last_LEDupdate[strip_num] = millis();
        fill_solid(leds[strip_num], NUM_LEDS, CRGB::Black);
        for (int i = 0; i < width; i++) {
            int led_idx = solid_beam_pos[strip_num] + i;
            if(led_idx >= 0 && led_idx < NUM_LEDS) {
                leds[strip_num][led_idx] = color;
            }
        }

        solid_beam_pos[strip_num] += solid_beam_dir[strip_num] * (1 + led_skip);

        if(solid_beam_pos[strip_num] >= NUM_LEDS - width){
            solid_beam_dir[strip_num] = -1;
        } else if(solid_beam_pos[strip_num] <= 0){
            solid_beam_dir[strip_num] = 1;
        }
    }
}