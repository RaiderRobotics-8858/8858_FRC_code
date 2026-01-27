package frc.robot.commands.controller;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;

public class RumbleCommand extends Command {
    private final CommandXboxController controller;
    private final double rumbleIntensity;
    private final double durationSeconds;
    private double startTime;

    public RumbleCommand(CommandXboxController controller, double rumbleIntensity, double durationSeconds){
        this.controller = controller;
        this.rumbleIntensity = rumbleIntensity;
        this.durationSeconds = durationSeconds;
    }

    @Override
    public void initialize(){
        startTime = Timer.getFPGATimestamp();
        controller.setRumble(RumbleType.kBothRumble, rumbleIntensity);
    }

    @Override
    public void execute(){
        double elapsed = Timer.getFPGATimestamp() - startTime;
        if(elapsed > durationSeconds){
            controller.setRumble(RumbleType.kBothRumble, 0);
        }
    }

    @Override
    public boolean isFinished() {
        return Timer.getFPGATimestamp() - startTime > durationSeconds;
    }

    @Override
    public void end(boolean interrupted){
        controller.setRumble(RumbleType.kBothRumble, 0);
    }
}
