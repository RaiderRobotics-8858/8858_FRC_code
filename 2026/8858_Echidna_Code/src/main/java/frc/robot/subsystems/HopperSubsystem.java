package frc.robot.subsystems;

import com.revrobotics.spark.SparkLowLevel.MotorType;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;

import com.revrobotics.spark.SparkMax;

/**
 * Subsystem for controlling the hopper mechanism, which moves fuel from the intake to the launcher.
 */
public class HopperSubsystem extends SubsystemBase {
    private final SparkMax hopperMotor;

    /**
     * {@link HopperSubsystem} constructor.
     */
    public HopperSubsystem() {
        hopperMotor = new SparkMax(Constants.CAN_HOPPER_MOTOR, MotorType.kBrushless);
    }

    /**
     * Returns the current of the Hopper motor in Amps
     */
    public double getCurrent(){
        return hopperMotor.getOutputCurrent();
    }

    /**
     * Manually moves the hopper without PID control
     * @param speed The speed to set the hopper motor to, from -1.0 to 1.0
     */
    public void setHopperSpeed(double speed) {
        hopperMotor.set(speed);
    }
    // Gets Hopper Temp
     public double getHopperTemp() {
        return hopperMotor.getMotorTemperature();
    }
}
