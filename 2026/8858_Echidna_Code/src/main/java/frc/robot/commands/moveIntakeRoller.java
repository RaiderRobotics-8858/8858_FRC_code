package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.IntakeSubsystem;

public class moveIntakeRoller extends Command {
    private final IntakeSubsystem intakeSubsystem;
    private final double rollerSpeed;

    public moveIntakeRoller(IntakeSubsystem intakeSubsystem, double rollerSpeed) {
        this.intakeSubsystem = intakeSubsystem;
        this.rollerSpeed = rollerSpeed;
        addRequirements(intakeSubsystem);
    }

    @Override
    public void execute() {
        intakeSubsystem.setIntakeSpeed(rollerSpeed);
    }

    @Override
    public void end(boolean interrupted) {
        intakeSubsystem.setIntakeSpeed(0); // Stop intake roller movement when command ends
    }
}
