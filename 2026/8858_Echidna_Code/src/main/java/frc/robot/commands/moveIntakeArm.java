package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.IntakeSubsystem;

public class moveIntakeArm extends Command {
    private final IntakeSubsystem intakeSubsystem;
    private final double armSpeed;

    public moveIntakeArm(IntakeSubsystem intakeSubsystem, double armSpeed) {
        this.intakeSubsystem = intakeSubsystem;
        this.armSpeed = armSpeed;
        addRequirements(intakeSubsystem);
    }

    @Override
    public void execute() {
        intakeSubsystem.setIntakeArmSpeed(armSpeed);
    }

    @Override
    public void end(boolean interrupted) {
        intakeSubsystem.setIntakeArmSpeed(0); // Stop intake arm movement when command ends
    }
}
