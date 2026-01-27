package frc.robot.subsystems.swervedrive;

import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkMax;

import frc.robot.Constants;
import frc.robot.Constants.OperatorConstants;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class CoralIntakeSubsystem extends SubsystemBase {

    private final SparkMax coralIntakeMotor;
    public static CoralIntakeSubsystem coral_intake_instance;
    private final DigitalInput lsTop, lsBot;

    public CoralIntakeSubsystem() {
        // initialize motor
        coralIntakeMotor = new SparkMax(OperatorConstants.CAN_CORAL_INTAKE_MOTOR, MotorType.kBrushless);
        coral_intake_instance = this; // save subsystem so it can be accessed anywhere
        lsTop = new DigitalInput(Constants.OperatorConstants.LS_TOP);
        lsBot = new DigitalInput(Constants.OperatorConstants.LS_BOT);
    }

    /** move intake at speed */
    public void coralIntake(double speed) {
        if(SmartDashboard.getBoolean("Safety Disable", false)){
            speed = 0;
        }
        coralIntakeMotor.set(speed);
    }

    /* Top limit switch read true false pressed down */
    public boolean getTop(){
        return !(lsTop.get());
    }
    /* Bottom limit switch read true false pressed down */
    public boolean getBot(){
        return !(lsBot.get());
    }

    public void AutoScoreCoral(){
        if(!SmartDashboard.getBoolean("Safety Disable", false)){
            coralIntakeMotor.set(Constants.COR_M_SPEED);
        }
    }

    public void AutoCoralIntake(){
        if(!SmartDashboard.getBoolean("Safety Disable", false)){
            if (getTop()){
                if(getBot()){
                    coralIntakeMotor.set(0);
                } else {
                    coralIntakeMotor.set(Constants.COR_M_PRESCORE_SPEED);
                }
            } else {
                if(getBot()){
                    coralIntakeMotor.set(-Constants.COR_M_PRESCORE_SPEED);
                } else {
                    coralIntakeMotor.set(0);
                }
            }
        }
    }

    public void preScoreAutoCoralIntake(){
        if(!SmartDashboard.getBoolean("Safety Disable", false)){
            if (getTop()){
                if(getBot()){
                    coralIntakeMotor.set(Constants.COR_M_PRESCORE_SPEED);
                } else {
                    coralIntakeMotor.set(Constants.COR_M_PRESCORE_SPEED);
                }
            } else {
                if(getBot()){
                    coralIntakeMotor.set(0);
                } else {
                    coralIntakeMotor.set(0);
                }
            }
        }
    }
}