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
import com.ctre.phoenix6.hardware.CANdle;
import com.ctre.phoenix6.signals.AnimationDirectionValue;
import com.ctre.phoenix6.signals.RGBWColor;

/**
 * Subsystem that controls an addressable LED strip using a CANdle.
 */
public class LEDSubsystem extends SubsystemBase {
    private final CANBus kCANBus = new CANBus("rio");
    private final CANdle m_candle = new CANdle(Constants.CAN_CANDLE, kCANBus);

    private final RainbowAnimation m_rainbow = new RainbowAnimation(0, 42)
        .withSlot(0)
        .withBrightness(1)
        .withDirection(AnimationDirectionValue.Forward)
        .withFrameRate(Hertz.of(100));

    private final LarsonAnimation m_larson_base = new LarsonAnimation(0, 42)
        .withSize(8)
        .withFrameRate(Hertz.of(20));

    public LEDSubsystem() {
        setDefaultCommand(defaultCommand());
    }

    /**
     * Chooses the default command based on whether the turret is zeroed.
     *
     * @return Command to run
     */
    public Command defaultCommand() {
        if(SmartDashboard.getBoolean("Launcher/TurretZeroedFlag", false)) {
            return allianceLarson();
        } else {
            return rainbow();
        }
    }

    /**
     * Runs a rainbow animation.
     *
     * @return Command to run
     */
    public Command rainbow() {
        return run(() -> m_candle.setControl(m_rainbow));
    }

    /**
     * Runs a Larson animation in the provided color.
     *
     * @param color the RGBW color to display as a beam
     * @return Command to run
     */
    public Command larsonWithColor(RGBWColor color) {
        return run(() -> m_candle.setControl(m_larson_base.clone().withColor(color)));
    }

    /**
     * Runs a Larson animation in the provided color.
     *
     * @param color the RGBW color to display
     * @return Command to run
     */
    public Command allianceLarson() {
        RGBWColor color;
        var alliance = DriverStation.getAlliance().orElse(null);
        if (alliance == DriverStation.Alliance.Red) {
            color = new RGBWColor(Color.kRed);
        } else if (alliance == DriverStation.Alliance.Blue) {
            color = new RGBWColor(Color.kBlue);
        } else {
            color = new RGBWColor(Color.kPurple);
        }
        return larsonWithColor(color);
    }
}
