
/*
Returns `true` after shifting in a dot
Subsequent calls of the function should pass in `false` to `newdot` argument
*/
bool dot(int t_seconds, bool newdot, int strip_num, CRGB color){
    bool done = false;
    unsigned long update_rate = (t_seconds*1000) / (NUM_LEDS);

    if(millis() > last_LEDupdate[strip_num] + update_rate){
        for(int i = NUM_LEDS - 1; i > 0; i--){
            leds[strip_num][i] = leds[strip_num][i - 1];
        }
        if(newdot){
            leds[strip_num][0] = color;
            done = true;
        } else {
            leds[strip_num][0] = CRGB::Black;
            done = true;
        }
    }

    return done; // indicates when the dot has been shifted in
}