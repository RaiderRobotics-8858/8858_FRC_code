package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.ClimberSubsystem;

public class moveClimber extends Command {
    private final ClimberSubsystem climberSubsystem;
    private final double climberSpeed;

    /** Moves the hopper at a specified speed
     * @param climberSubsystem The hopper subsystem to control
     * @param climberSpeed The speed to set the hopper motors to (range -1.0 to 1.0)
     */
    public moveClimber(ClimberSubsystem climberSubsystem, double climberSpeed) {
        this.climberSubsystem = climberSubsystem;
        this.climberSpeed = climberSpeed;
        addRequirements(climberSubsystem);
    }

    @Override
    public void execute() {
        climberSubsystem.setClimberSpeed(climberSpeed);
    }

    @Override
    public void end(boolean interrupted) {
        climberSubsystem.setClimberSpeed(0); // Stop hopper movement when command ends
    }
}
