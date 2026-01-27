package frc.robot.commands.intake;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.swervedrive.CoralIntakeSubsystem;

public class AutoScoreCoral extends Command {
    private final CoralIntakeSubsystem coralIntakeSubsystem;

    // Used to determine how long to 'blind run' coral motor after limit switches can't see the coral anymore
    Timer coralIntakeTimer = new Timer();
    boolean coralTimerActive = false;
    double coralIntakeEndTime;
    double coralBlindRunTime = 0.5;

    public AutoScoreCoral(CoralIntakeSubsystem coralIntakeSubsystem) {
        this.coralIntakeSubsystem = coralIntakeSubsystem;
        addRequirements(coralIntakeSubsystem); // add requirement so that multiple commands using the same subsystem don't run at the same time
    }

    @Override
    public void execute() { // runs periodically while the command is scheduled
        coralIntakeSubsystem.AutoScoreCoral();

        if(!(coralIntakeSubsystem.getTop() || coralIntakeSubsystem.getBot())){
            if(!coralTimerActive){
                coralTimerActive = true;
                coralIntakeEndTime = coralIntakeTimer.get() + coralBlindRunTime;
            }
        }
    }

    @Override
    public boolean isFinished() { // check if the command should stop running
        // return (!(coralIntakeSubsystem.getTop() || coralIntakeSubsystem.getBot()));
        SmartDashboard.putNumber("Coral Blind Run Timer", coralIntakeTimer.get() - coralIntakeEndTime);
        return (coralTimerActive && (coralIntakeTimer.get() > coralIntakeEndTime));
    }

    @Override
    public void end(boolean interrupted) { // runs when the command ends
        coralIntakeSubsystem.coralIntake(0);
    }

    @Override
    public void initialize(){
        coralIntakeTimer.start();
        coralTimerActive = false;

        // shouldn't need this as it will be overwritten but good to have ~some~ value
        coralIntakeEndTime = coralIntakeTimer.get() + coralBlindRunTime;
    }
}