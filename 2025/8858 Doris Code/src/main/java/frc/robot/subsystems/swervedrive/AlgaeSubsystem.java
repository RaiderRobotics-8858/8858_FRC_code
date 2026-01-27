package frc.robot.subsystems.swervedrive;

import com.revrobotics.spark.SparkLowLevel.MotorType;

import java.security.KeyStore.PrivateKeyEntry;

import com.revrobotics.spark.SparkMax;

import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.wpilibj.PowerDistribution;
import edu.wpi.first.wpilibj.PowerDistribution.ModuleType;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.Constants.OperatorConstants;
public class AlgaeSubsystem extends SubsystemBase {

    private final SparkMax leftAlgaeIntakeMotor;
    private final SparkMax rightAlgaeIntakeMotor;
    public static AlgaeSubsystem algae_intake_instance;
    private final SlewRateLimiter speedLimiter;


    PowerDistribution m_pdp = new PowerDistribution(Constants.OperatorConstants.CAN_PDP, ModuleType.kRev);

    public AlgaeSubsystem() {
        // initialize motors
        leftAlgaeIntakeMotor = new SparkMax(OperatorConstants.CAN_ALGAE_L, MotorType.kBrushless);
        rightAlgaeIntakeMotor = new SparkMax(OperatorConstants.CAN_ALGAE_R, MotorType.kBrushless);
        algae_intake_instance = this; // save subsystem so it can be accessed anywhere
        speedLimiter = new SlewRateLimiter(0.4);
        
        setDefaultCommand(new Command() { // run this command when the subsystem isn't being used by another command
            {
                addRequirements(algae_intake_instance);
            }
            @Override
            public void initialize() {
                // algae_intake_instance.algaeIntake(0); // reset the PID controller
            }
            @Override
            public void execute() {
                algae_intake_instance.algaeIntake(-Constants.ALG_M_HOLD_SPEED); // hold the elevator at the last set position
            }
        });

    }

    /** move intake at speed */
    public void algaeIntake(double speed) {
        speed = speedLimiter.calculate(speed);
        if(SmartDashboard.getBoolean("Safety Disable", false)){
            speed = 0;
        }
        leftAlgaeIntakeMotor.set(speed);
        rightAlgaeIntakeMotor.set(-speed);
    }

    public double getAlgaeCurrent(){
        // return m_pdp.getCurrent(Constants.OperatorConstants.PDP_ALGAE);
        return rightAlgaeIntakeMotor.getOutputCurrent();
    }
}