package frc.robot.subsystems;

import com.revrobotics.spark.SparkLowLevel.MotorType;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.wpilibj.DutyCycleEncoder;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;

import com.revrobotics.spark.SparkMax;

/**
 * Subsystem for controlling the intake mechanism, which consists
 * of an intake arm and rollers to pull in game pieces.
 */
public class IntakeSubsystem extends SubsystemBase {
    private final SparkMax rollerMotor;
    private final SparkMax armIntakeMotor;
    private final DutyCycleEncoder armIntakeEncoder;
    private final SlewRateLimiter intakeRateLimiter; 

    /** PID controller for maintaining intake arm position */
    private final PIDController armPIDController;
    private final double arm_kP = Constants.INTAKE_ARM_KP;
    private final double arm_kI = Constants.INTAKE_ARM_KI;
    private final double arm_kD = Constants.INTAKE_ARM_KD;

    /**
     * {@link IntakeSubsystem} constructor.
     */
    public IntakeSubsystem() {
        rollerMotor = new SparkMax(Constants.CAN_INTAKE_ROLLER, MotorType.kBrushless);
        armIntakeMotor = new SparkMax(Constants.CAN_INTAKE_EXT, MotorType.kBrushless);
        armIntakeEncoder = new DutyCycleEncoder(Constants.DIO_INTAKE_ABS);
        armPIDController = new PIDController(arm_kP, arm_kI, arm_kD);
        intakeRateLimiter = new SlewRateLimiter(1);
    }

    /**
     * Resets the PID controllers for both launch speed and turret angle.
     * Should be called when starting a new aiming/launching action to prevent
     * integral windup and ensure accurate control from the start.
     */
    public void resetPID() {
        armPIDController.setPID(
            SmartDashboard.getNumber("Intake/arm_kP", arm_kP),
            SmartDashboard.getNumber("Intake/arm_kI", arm_kI),
            SmartDashboard.getNumber("Intake/arm_kD", arm_kD)
        );
        armPIDController.reset();
    }

    /**
     * Manually moves the intake without PID control
     * @param speed The speed to set the intake roller motor to, from -1.0 to 1.0
     */
    public void setIntakeSpeed(double speed) {
        double rollerSpeed = intakeRateLimiter.calculate(speed);
        rollerMotor.set(rollerSpeed);
    }

    /**
     * Returns the current drawn by the intake roller motor
     * @return The current drawn by the intake roller motor
     */
    public double getIntakeRollerCurrent() {
        double current = rollerMotor.getOutputCurrent();
        if(SmartDashboard.getBoolean("TESTMODE", false)){
            if(current >= 25){
                SmartDashboard.putBoolean("Intake/Overcurrent Roller", false);
            }
        }
        return current;
    }


    /**
     * Returns the current position of the intake arm in encoder units
     * @return The current position of the intake arm
     */
    public double getIntakeArmPosition() {
        // return armIntakeMotor.getEncoder().getPosition();
        return armIntakeEncoder.get();
    }

    public boolean armAtTarget(double target){
        if(Math.abs(getIntakeArmPosition() - target) < 0.1){
            return true;
        }
        return false;
    }

    /**
     * Manually moves the intake arm without PID control
     * @param speed The speed to set the intake arm motor to, from -1.0 to 1.0
     */
    public void setIntakeArmSpeed(double speed) {
        armIntakeMotor.set(speed);
    }

    /**
     * Moves the intake arm to the target position using PID control
     * @param targetPosition The target position for the intake arm
     */
    public void setIntakeArmPosition(double targetPosition) {
        double output = armPIDController.calculate(getIntakeArmPosition(), targetPosition);
        setIntakeArmSpeed(output);
    }
    // Gets Roller Temp
     public double getRollerTemp() {
        return rollerMotor.getMotorTemperature();
    }
    // Gets Intake Temp
     public double getIntakeTemp() {
        return armIntakeMotor.getMotorTemperature();
    }
}
