package frc.robot.subsystems;

import com.revrobotics.spark.SparkLowLevel.MotorType;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;

import com.revrobotics.spark.SparkMax;

/**
 * Subsystem for controlling the climber mechanism.
 */
public class ClimberSubsystem extends SubsystemBase {
    private final SparkMax ClimberMotor;

    private final PIDController climberpid;
    private final double clim_kP = Constants.CLIMBER_KP;
    private final double clim_kI = Constants.CLIMBER_KI;
    private final double clim_kD = Constants.CLIMBER_KD;
    /**
     * {@link ClimberSubsystem} constructor.
     */
    public ClimberSubsystem() {
        ClimberMotor = new SparkMax(Constants.CAN_CLIMBER, MotorType.kBrushless);
        climberpid = new PIDController(clim_kP, clim_kI, clim_kD);
    }

    public void zeroClimber(){
        ClimberMotor.getEncoder().setPosition(0);
    }

    /**
     * Returns the current position of the Climber in encoder units
     */
    public double getClimberPos(){
      return  ClimberMotor.getEncoder().getPosition();
    }

    /**
     * Returns the current of the Climber motor in Amps
     */
    public double getCurrent(){
        return ClimberMotor.getOutputCurrent();
    }

    /**
     * Resets the PID controller for the climber.
     * Should be called when starting a new climbing action to prevent
     * integral windup and ensure accurate control from the start.
     */
    public void resetPID(){
        climberpid.setPID(
            SmartDashboard.getNumber("Climber/climb_kP", clim_kP),
            SmartDashboard.getNumber("Climber/climb_kI", clim_kI),
            SmartDashboard.getNumber("Climber/climb_kD", clim_kD)
        );
        climberpid.reset();
    }

    /**
     * Moves the Climber to a target position using PID control
     * @param target The target position for the Climber in encoder units
     */
    public void setClimberPos(double target){
        double output = climberpid.calculate(getClimberPos(),target);
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
