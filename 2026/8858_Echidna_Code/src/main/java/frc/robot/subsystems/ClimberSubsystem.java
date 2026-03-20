package frc.robot.subsystems;

import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.thethriftybot.devices.ThriftyNova.PIDConfig;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;

import com.revrobotics.spark.SparkMax;

/**
 * Subsystem for controlling the climber mechanism.
 */
public class ClimberSubsystem extends SubsystemBase {
    private final SparkMax ClimberMotor;

    private final PIDController climberpid;
    private final double clim_kP = 0.8;
    private final double clim_kI = 0.0;
    private final double clim_kD = 0.0;
    /**
     * {@link ClimberSubsystem} constructor.
     */
    public ClimberSubsystem() {
        ClimberMotor = new SparkMax(Constants.CAN_CLIMBER_LEFT, MotorType.kBrushless);
        climberpid = new PIDController(clim_kP, clim_kI, clim_kD);
    }

    public double getclimberpos(){
      return  ClimberMotor.getEncoder().getPosition();
    }

    /**
     * Returns the current of the Climber motor in Amps
     */
    public double getCurrent(){
        return ClimberMotor.getOutputCurrent();
    }

    public void resetPID(){
        climberpid.reset();
    }
    
    public void setClimberPos(double target){
        double output = climberpid.calculate(getclimberpos(),target);
        setClimberSpeed(output);

    }

    /**
     * Manually moves the Climber without PID control
     * @param speed The speed to set the Climber motor to, from -1.0 to 1.0
     */
    public  void setClimberSpeed(double speed) {
        ClimberMotor.set(speed);
    }
}
