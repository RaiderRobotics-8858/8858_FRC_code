package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.IntakeSubsystem;

public class activateIntake extends Command {
    private final IntakeSubsystem intakeSubsystem;
    private final double armPosition;
    private final double rollerSpeed;

    /** Activates the intake system by setting the intake arm to a specified position,
     * running the intake rollers at a specified speed, and running the hopper at a specified speed.
     *
     * @param intakeSubsystem The intake subsystem to control
     * @param hopperSubsystem The hopper subsystem to control
     * @param armPosition The target position for the intake arm
     * @param rollerSpeed The speed to set the intake rollers to, from -1.0 to 1.0
     * @param hopperSpeed The speed to set the hopper to, from -1.0 to 1.0
     */
    public activateIntake(IntakeSubsystem intakeSubsystem, double armPosition, double rollerSpeed) {
        this.intakeSubsystem = intakeSubsystem;
        this.armPosition = armPosition;
        this.rollerSpeed = rollerSpeed;
        addRequirements(intakeSubsystem);
    }

    @Override
    public void initialize() {
        intakeSubsystem.resetPID();
    }

    @Override
    public void execute() {
        intakeSubsystem.setIntakeArmPosition(armPosition);
        intakeSubsystem.setIntakeSpeed(rollerSpeed); // Set intake roller speed
    }

    @Override
    public void end(boolean interrupted) {
        intakeSubsystem.setIntakeSpeed(0); // Stop intake roller movement when command ends
    }
}
