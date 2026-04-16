package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Constants;
import frc.robot.subsystems.LauncherSubsystem;

public class aimAtTarget extends Command {
    private final LauncherSubsystem launcherSubsystem;

    /** Keeps the Turret aimed at ideal target point
     * @param launcherSubsystem The launcher subsystem to control
     */
    public aimAtTarget(LauncherSubsystem launcherSubsystem) {
        this.launcherSubsystem = launcherSubsystem;
        addRequirements(launcherSubsystem);
    }

    @Override
    public void initialize() {
        launcherSubsystem.resetPID(); // Reset PID controllers for speed and angle
    }

    @Override
    public void execute() {
        launcherSubsystem.findTarget();
        launcherSubsystem.setHoodPosition(Constants.HOOD_HIGH_LIMIT);
    }
}