package frc.robot.commands.intake;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Constants;
import frc.robot.subsystems.swervedrive.AlgaeSubsystem;

public class algaeSmartIntake extends Command {
    private final AlgaeSubsystem algaeSubsystem;

    // Used to determine how long to 'blind run' coral motor after limit switches can't see the coral anymore
    Timer algaeIntakeTimer = new Timer();
    boolean algaeTimerActive = false;
    double algaeIntakeEndTime;
    double algaeBlindRunTime = 2.0;

    public algaeSmartIntake (AlgaeSubsystem algaeSubsystem){
        this.algaeSubsystem = algaeSubsystem;


        addRequirements(algaeSubsystem);
    }

    @Override
    public void initialize() {
        algaeIntakeTimer.start();
        algaeTimerActive = false;
        // shouldn't need this as it will be overwritten but good to have ~some~ value
        algaeIntakeEndTime = algaeIntakeTimer.get() + algaeBlindRunTime;
    }


    @Override
    public void execute() {
        SmartDashboard.putBoolean("Algae Intake Done", false);
        algaeSubsystem.algaeIntake(-Constants.ALG_M_SPEED);
        if(!algaeTimerActive){
            algaeTimerActive = true;
            algaeIntakeEndTime = algaeIntakeTimer.get() + algaeBlindRunTime;
        }
    }

    @Override
    public boolean isFinished() {
        SmartDashboard.putNumber("Algae Blind Run Timer", algaeIntakeEndTime - algaeIntakeTimer.get());
        if (algaeTimerActive && (algaeIntakeTimer.get() > algaeIntakeEndTime)){
            return (algaeSubsystem.getAlgaeCurrent() > 14);
        } else {
            return false;
        }
    }

    @Override
    public void end(boolean interrupted) {
        algaeSubsystem.algaeIntake(0);
        SmartDashboard.putBoolean("Algae Intake Done", true);
    }

}
