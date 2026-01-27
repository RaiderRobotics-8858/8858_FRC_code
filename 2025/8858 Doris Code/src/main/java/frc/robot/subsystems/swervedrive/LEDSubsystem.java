package frc.robot.subsystems.swervedrive;

import com.ctre.phoenix.led.CANdle;
import com.ctre.phoenix.led.TwinkleAnimation;
import com.ctre.phoenix.led.CANdle.LEDStripType;
import com.ctre.phoenix.led.LarsonAnimation.BounceMode;
import com.ctre.phoenix.led.TwinkleAnimation.TwinklePercent;
import com.ctre.phoenix.led.LarsonAnimation;
import com.ctre.phoenix.led.RainbowAnimation;
import com.ctre.phoenix.led.Animation;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class LEDSubsystem extends SubsystemBase {
    private static LEDSubsystem m_instance = null;
    private CANdle candle = null;
    private static final int LED_COUNT = 38; // was 38
    private Timer m_timer = null;
    private Mode mode = null;
    private boolean countDownTimerActive = false;
    private int countDownTimerLength = 0;
    private Timer m_countDownTimer;
    public static final Mode default_state = Mode.BEAM_ALLIANCE;
    private static final TwinkleAnimation blue_twinkle_anim = new TwinkleAnimation(0, 255, 0, 0, 0.2, LED_COUNT,
            TwinklePercent.Percent18);

    private static final LarsonAnimation blueBeam = new LarsonAnimation(0, 0, 255, 0, 0.5, LED_COUNT, BounceMode.Center, 5);
    private static final LarsonAnimation redBeam = new LarsonAnimation(255, 0, 0, 0, 0.5, LED_COUNT, BounceMode.Center, 5);
    private static final LarsonAnimation purpleBeam = new LarsonAnimation(255, 0, 255, 0, 0.5, LED_COUNT, BounceMode.Center, 5);
    private static final RainbowAnimation rainbow = new RainbowAnimation(255, 0.25, LED_COUNT);

    public static enum Mode {
        OFF,
        RAINBOW,
        SOLID_ALLIANCE,
        SOLID_RED,
        SOLID_BLUE,
        SOLID_GREEN,
        SOLID_PURPLE,
        RED_GODZILLA,
        BLUE_GODZILLA,
        BLINK_ALLIANCE,
        BLINK_RED,
        BLINK_BLUE,
        BLINK_GREEN,
        BLINK_PURPLE,
        TWINKLE_BLUE,
        BEAM_ALLIANCE,
        BEAM_BLUE,
        BEAM_RED
    }

    private LEDSubsystem() {
        candle = new CANdle(17);
        candle.configLEDType(LEDStripType.BRG);
        m_timer = new Timer();
        m_countDownTimer = new Timer();
        setMode(default_state);
        m_instance = this;
        setDefaultCommand(new Command() {
            {
                addRequirements(m_instance);
            }

            @Override
            public void execute() {
                m_instance.holdState();
            }
        });
    }

    public void startTimer(int t_seconds){
        countDownTimerActive = true;
        countDownTimerLength = t_seconds;
        m_countDownTimer.start();
    }

    public static LEDSubsystem getInstance() {
        if (m_instance == null) {
            m_instance = new LEDSubsystem();
        }
        return m_instance;
    }

    public void holdState() {
        double time = m_timer.get();
        SmartDashboard.putNumber("LED Animation Timer", time);
        if(countDownTimerActive){
            double time_remaining = countDownTimerLength - m_countDownTimer.get();

            if(time_remaining > 0){
                candle.setLEDs(255, 255, 255, 255, (int)(LED_COUNT * (time_remaining / countDownTimerLength)), LED_COUNT);
                candle.setLEDs(255, 255, 255, 255, (int)(LED_COUNT * (time_remaining / countDownTimerLength)), LED_COUNT);
            } else {
                countDownTimerActive = false;
                countDownTimerLength = 0;
                m_countDownTimer.stop();
                m_countDownTimer.reset();
            }
        } else {
            switch (mode) {
                case OFF:
                    setColor(0, 0, 0);
                    break;
                case RAINBOW:
                    candle.animate(rainbow);
                    break;
                case SOLID_ALLIANCE:
                    if(DriverStation.getAlliance().get() == Alliance.Red){
                        setColor(255, 0, 0);
                    } else if (DriverStation.getAlliance().get() == Alliance.Blue){
                        setColor(0, 255, 0);
                    } else {
                        setColor(255, 2555, 255);
                    }
                    break;
                case SOLID_RED:
                    setColor(255, 0, 0);
                    break;
                case SOLID_BLUE:
                    setColor(0, 0, 255);
                    break;
                case SOLID_GREEN:
                    setColor(0, 255, 0);
                    break;
                case SOLID_PURPLE:
                    setColor(255, 0, 255);
                    break;
                case RED_GODZILLA:
                    runGodzilla(time, 255, 0, 0);
                    break;
                case BLUE_GODZILLA:
                    runGodzilla(time, 0, 0, 255);
                    break;
                case BLINK_ALLIANCE:
                    if(DriverStation.getAlliance().get() == Alliance.Red){
                        blink(1, time, 255, 0, 0);
                    } else if (DriverStation.getAlliance().get() == Alliance.Blue){
                        blink(1, time, 0, 0, 255);
                    } else {
                        blink(1, time, 255, 255, 255);
                    }
                    break;
                case BLINK_RED:
                    blink(1, time, 255, 0, 0);
                    break;
                case BLINK_BLUE:
                    blink(1, time, 0, 0, 255);
                    break;
                case BLINK_GREEN:
                    blink(1, time, 0, 255, 0);
                    break;
                case BLINK_PURPLE:
                    blink(1, time, 255, 0, 255);
                    break;
                case TWINKLE_BLUE:
                    candle.animate(blue_twinkle_anim);
                    break;
                case BEAM_ALLIANCE:
                    if(DriverStation.getAlliance().get() == Alliance.Red){
                        candle.animate(redBeam);
                    } else if (DriverStation.getAlliance().get() == Alliance.Blue){
                        candle.animate(blueBeam);
                    } else {
                        candle.animate(purpleBeam);
                    }
                    break;
                case BEAM_BLUE:
                    candle.animate(blueBeam);
                    break;
                case BEAM_RED:
                    candle.animate(redBeam);
                    break;
            }
        }
    }

    public void setMode(Mode mode) {
        if (this.mode == Mode.RED_GODZILLA) {
            return;
        }
        if (this.mode == Mode.BLUE_GODZILLA) {
            return;
        }
        SmartDashboard.putString("LED Mode", mode.toString());
        candle.clearAnimation(0);
        this.mode = mode;
        m_timer.reset(); // reset animation timer
        m_timer.start(); // start animation timer
        last_blink = 0; // reset blink timer
        blink_state = true; // reset blink state
    }
    
    public void manualOverride(Mode mode) {
        this.mode = mode;
        setMode(mode);
    }

    /**
     * 
     * @param rate seconds per blink
     * @param time the led animation timer
     * @param r    amount of red
     * @param g    amount of green
     * @param b    amount of blue
     */
    private void blink(double rate, double time, int r, int g, int b) {
        if (time % rate < (rate / 2)) {
            setColor(r, g, b);
        } else {
            if(countDownTimerActive){
                double time_remaining = countDownTimerLength - m_countDownTimer.get();
    
                if(time_remaining > 0){
                    candle.setLEDs(r, g, b, 255, (int)(LED_COUNT * (time_remaining / countDownTimerLength)), LED_COUNT);
                } else {
                    countDownTimerActive = false;
                    countDownTimerLength = 0;
                    m_countDownTimer.stop();
                    m_countDownTimer.reset();
                }
            } else {
                setColor(0, 0, 0);
            }
        }
    }

    private void setColor(int r, int g, int b) {
        Color col = new Color(r, g, b);
        SmartDashboard.putString("LED Color", col.toHexString());
        candle.setLEDs(r, g, b, 0, 0, LED_COUNT);
    }

    private double last_blink = 0;
    private boolean blink_state = true;
    private final double godzilla_blink_rate = 17;

    /**
     * blink the leds starting slowly, but speeding up over time, with the max blink
     * speed occuring at 15 seconds
     * blink rate is number of blinks per second, and should max out at 8
     * 
     * @param time
     */
    private void runGodzilla(double time, int red, int green, int blue) {
        double blink_rate = time > godzilla_blink_rate ? 8 : 1 + (time / godzilla_blink_rate) * 7; // max out at 8
                                                                                                   // blinks per second
        if (time - last_blink > 1.0 / blink_rate) {
            last_blink = time;
            if (blink_state) {
                setColor(red, green, blue);
            } else {
                setColor(0, 0, 0);
            }
            blink_state = !blink_state;
        }
    }
}
