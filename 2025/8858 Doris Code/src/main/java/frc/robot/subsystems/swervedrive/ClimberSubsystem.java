package frc.robot.subsystems.swervedrive;

import com.revrobotics.spark.SparkFlex;
import com.revrobotics.spark.SparkLowLevel.MotorType;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj.DutyCycleEncoder;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
/*
NOTE FOR THE BUILD TEAM PLEASE TELL THEM THIS!!!!!!

There are two sharpie marks on the climber through bore encoder that indicates it's position.
align the two sharpie marks then make sure the climber arms point straight up at this position
*/
public class ClimberSubsystem extends SubsystemBase {

    private final SparkFlex leftClimbMotor;
    private final SparkFlex rightClimbMotor;
    private final DutyCycleEncoder throughBore;
    private final PIDController pidController;

    // PID Coefficients
    private final double kP = 0.5;
    private final double kI = 0.0;
    private final double kD = 0.0;
    private final double kFF = 0.0;
    private final double kMaxOutput = 0.3;
    private final double kMinOutput = -0.3;
    private double lastTargetPosition = 0.0;

    public static ClimberSubsystem m_instance;

    public ClimberSubsystem() {
        leftClimbMotor = new SparkFlex(Constants.OperatorConstants.CAN_LEFT_CLIMB_MOTOR, MotorType.kBrushless);
        rightClimbMotor = new SparkFlex(Constants.OperatorConstants.CAN_RIGHT_CLIMB_MOTOR, MotorType.kBrushless);
        throughBore = new DutyCycleEncoder(0);
        pidController = new PIDController(kP, kI, kD);
        m_instance = this; // save subsystem so it can be accessed anywhere

        leftClimbMotor.set(0); // left motor runs slightly faster for some reason, so make it slower
        rightClimbMotor.set(0);

    }

    /** Move the climber to a certain position */
    public void MoveClimberToPosition(double targetPosition, double speed){ // move the elevator to a certain position
        this.lastTargetPosition = targetPosition; // save the target position
        double currentPosition = throughBore.get();
        // double output = pidController.calculate(currentPosition, targetPosition);

        double output;
        if (currentPosition > targetPosition){
            output = -Math.abs(speed);
        } else {
            output = Math.abs(speed);
        }

        if (SmartDashboard.getBoolean("Student Student Driver", false)){
            output = 0.0;
        }

        if(Math.abs(currentPosition - targetPosition) < 0.005){
            leftClimbMotor.set(0); // left motor runs slightly faster for some reason, so make it slower
            rightClimbMotor.set(0);
        } else {
            leftClimbMotor.set(output * 0.97); // left motor runs slightly faster for some reason, so make it slower
            rightClimbMotor.set(-output);
        }
    }

    /** Reset the PID controller */
    public void resetPID(){
        pidController.reset();
    }

    /** Hold the elevator at the last target position */
    public void HoldPosition(){ // hold the elevator at the last target position
        this.MoveClimberToPosition(lastTargetPosition, Constants.CL_M_SPEED);
    }

    /** get encoder position */
    public double getEncoder() {
        return throughBore.get();
    }

    /** move climber at speed */
    public void move(double speed) {
        leftClimbMotor.set(speed * .97); // left motor runs slightly faster for some reason, so make it slower
        rightClimbMotor.set(-speed);
    }
}