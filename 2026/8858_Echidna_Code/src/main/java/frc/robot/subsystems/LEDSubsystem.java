package frc.robot.subsystems;

import static edu.wpi.first.units.Units.*;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.controls.LarsonAnimation;
import com.ctre.phoenix6.controls.RainbowAnimation;
import com.ctre.phoenix6.controls.RgbFadeAnimation;
import com.ctre.phoenix6.controls.TwinkleAnimation;
import com.ctre.phoenix6.hardware.CANdle;
import com.ctre.phoenix6.signals.AnimationDirectionValue;
import com.ctre.phoenix6.signals.RGBWColor;

/**
 * Subsystem that controls an addressable LED strip using a CANdle.
 */
public class LEDSubsystem extends SubsystemBase {
    private final CANBus kCANBus = new CANBus("rio");
    private final CANdle m_candle = new CANdle(Constants.CAN_CANDLE, kCANBus);

    /**
     * Enum for selecting different LED animations to display on the LED strip.
     */
    public enum AnimationSelector {

        // Larson (beam) animations
        ALLIANCE_BEAM,
        YELLOW_BEAM,
        GREEN_BEAM,
        BLUE_BEAM,
        PURPLE_BEAM,

        // Color fade animation
        COLOR_FADE,

        // Default Animation
        RAINBOW
    }

    private final RainbowAnimation m_rainbow = new RainbowAnimation(0, 74)
        .withSlot(0)
        .withBrightness(1)
        .withDirection(AnimationDirectionValue.Forward)
        .withFrameRate(Hertz.of(100));

    private final LarsonAnimation m_larson_base = new LarsonAnimation(0, 74)
        .withSize(20)
        .withFrameRate(Hertz.of(60));

    private final RgbFadeAnimation m_rgb_fade = new RgbFadeAnimation(0, 74)
        .withSlot(0)
        .withBrightness(1);

    private final TwinkleAnimation m_TwinkleAnimation = new TwinkleAnimation(8, 74)
        .withFrameRate(Hertz.of(20))
        .withColor(new RGBWColor(Color.kWhite));
    
    public LEDSubsystem() {
        // setDefaultCommand(defaultCommand());
    }

    /**
     * Chooses the default command based on whether the turret is zeroed.
     *
     * @return Command to run
     */
    public Command defaultCommand() {
        // if(SmartDashboard.getBoolean("Launcher/TurretZeroedFlag", false)) {
        //     return allianceLarson();
        // } else {
        //     return rainbow();
        // }
       return allianceLarson();
    }
   
    public void setTwinkle (){
        m_candle.setControl(m_TwinkleAnimation);
    }

    public Command twinkleCommand (){
        return run(() -> setTwinkle());
    }

    /**
     * Sets the LED strip to display a rainbow animation.
     */
    public void setRainbow(){
        m_candle.setControl(m_rainbow);
    }

    /**
     * Runs a rainbow animation.
     *
     * @return Command to run
     */
    public Command rainbow() {
        return run(() -> setRainbow());
    }

    /**
     * Sets the color of the Larson animation.
     *
     * @param color the color to set
     */
    public void setLarsonColor(RGBWColor color) {
        m_candle.setControl(m_larson_base.clone().withColor(color));
    }

    /**
     * Sets the LED strip to display a color fade animation.
     */
    public void setColorFade() {
        m_candle.setControl(m_rgb_fade);
    }

    /**
     * Runs a Larson animation in the provided color.
     *
     * @param color the RGBW color to display as a beam
     * @return Command to run
     */
    public Command larsonWithColor(RGBWColor color) {
        return run(() -> setLarsonColor(color));
    }

    /**
     * Runs a Larson animation in the provided color.
     *
     * @param color the RGBW color to display
     * @return Command to run
     */
    public void setAllinceLarsonColor() {
        RGBWColor color;
        var alliance = DriverStation.getAlliance().orElse(null);
        if (alliance == DriverStation.Alliance.Red) {
            color = new RGBWColor(Color.kRed);
        } else if (alliance == DriverStation.Alliance.Blue) {
            color = new RGBWColor(Color.kBlue);
        } else {
            color = new RGBWColor(Color.kPurple);
        }
        setLarsonColor(color);
    }

    public Command allianceLarson() {
        return run(() -> setAllinceLarsonColor());
    }
    /**
     * Selects and runs an animation based on the provided selector.
     *
     * @param selector the animation to run
     * @return Command to run
     */
    public Command selectAnimation(AnimationSelector selector) {
        return run(() -> {
            switch (selector) {
                case RAINBOW:
                    setRainbow();
                    break;
                case ALLIANCE_BEAM:
                    setLarsonColor(DriverStation.getAlliance().get() == DriverStation.Alliance.Red ? new RGBWColor(Color.kRed) : new RGBWColor(Color.kBlue));
                    break;
                case YELLOW_BEAM:
                    setLarsonColor(new RGBWColor(Color.kYellow));
                    break;
                case GREEN_BEAM:
                    setLarsonColor(new RGBWColor(Color.kGreen));
                    break;
                case BLUE_BEAM:
                    setLarsonColor(new RGBWColor(Color.kBlue));
                    break;
                case PURPLE_BEAM:
                    setLarsonColor(new RGBWColor(Color.kPurple));
                    break;
                case COLOR_FADE:
                    setColorFade();
                    break;
                default:
                    setRainbow();
            }
        });
    }
}
