package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.LauncherSubsystem;

public class moveHood extends Command {
    private final LauncherSubsystem launcherSubsystem;
    private final double hoodSpeed;

    public moveHood(LauncherSubsystem launcherSubsystem, double hoodSpeed) {
        this.launcherSubsystem = launcherSubsystem;
        this.hoodSpeed = hoodSpeed;
        addRequirements(launcherSubsystem);
    }

    @Override
    public void execute() {
        launcherSubsystem.setHoodSpeed(hoodSpeed);
    }

    @Override
    public void end(boolean interrupted) {
        launcherSubsystem.setHoodSpeed(0); // Stop turret movement when command ends
    }
}
