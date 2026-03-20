package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.LauncherSubsystem;

public class moveTurret extends Command {
    private final LauncherSubsystem launcherSubsystem;
    private final double turretSpeed;

    public moveTurret(LauncherSubsystem launcherSubsystem, double turretSpeed) {
        this.launcherSubsystem = launcherSubsystem;
        this.turretSpeed = turretSpeed;
        addRequirements(launcherSubsystem);
    }

    @Override
    public void execute() {
        launcherSubsystem.setTurretSpeed(turretSpeed);
    }

    @Override
    public void end(boolean interrupted) {
        launcherSubsystem.setTurretSpeed(0); // Stop turret movement when command ends
    }
}
