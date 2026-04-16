package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.LauncherSubsystem;

public class moveHoodtoPos extends Command {
    private final LauncherSubsystem launcherSubsystem;
    private final double targetPos;

    public moveHoodtoPos(LauncherSubsystem launcherSubsystem, double targetPos) {
        this.launcherSubsystem = launcherSubsystem;
        this.targetPos = targetPos;
        addRequirements(launcherSubsystem);
    }

    @Override
    public void initialize(){
        launcherSubsystem.resetPID();
    }

    @Override
    public void execute() {
        launcherSubsystem.setHoodPosition(targetPos);
    }

    @Override
    public boolean isFinished(){
        return (Math.abs(launcherSubsystem.getHoodPos() - targetPos) < 0.02);
    }

    @Override
    public void end(boolean interrupted) {
        launcherSubsystem.setHoodSpeed(0); // Stop turret movement when command ends
    }
}
