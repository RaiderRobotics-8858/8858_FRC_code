
uint8_t rainbow_hue[NUM_ROWS] = {0};
int rainbow_last_update = 0;

void rainbow(unsigned long update_rate, int width, bool row_enable[NUM_ROWS]){

    static uint8_t hue = 0;
    static uint8_t color_change_rate = 0xFF / width;

    if(millis() > rainbow_last_update + update_rate){
        rainbow_last_update = millis();
        for (int i = 0; i < NUM_LEDS; i++) {
            for(int strip_num = 0; strip_num < NUM_ROWS; strip_num++){
                if(row_enable[strip_num]){
                    leds[strip_num][i] = CHSV(hue + (color_change_rate * i), 255, brightness);
                }
            }
            hue++;
        }
    }
}