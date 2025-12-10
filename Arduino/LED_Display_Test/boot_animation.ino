
/*
# boot_animation
Runs an animation for a set amount of time

Returns True when time reaches `finish_time`
*/
bool boot_animation(unsigned long finish_time){
    bool done = false;

    if(millis() < finish_time){
        done = string_to_led_map("Booting............", boot_time_s, system_color, false, false);
    } else {
        done = true;
    }

    FastLED.show();
    return done;
}