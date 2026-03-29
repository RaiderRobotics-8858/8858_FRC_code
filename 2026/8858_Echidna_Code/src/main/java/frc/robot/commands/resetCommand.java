package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.ClimberSubsystem;
import frc.robot.subsystems.HopperSubsystem;
import frc.robot.subsystems.IntakeSubsystem;
import frc.robot.subsystems.LEDSubsystem;
import frc.robot.subsystems.LauncherSubsystem;

public class resetCommand extends Command {
    private final IntakeSubsystem intakeSubsystem;
    private final LauncherSubsystem launcherSubsystem;
    private final HopperSubsystem hopperSubsystem;
    private final ClimberSubsystem climberSubsystem;
    private final LEDSubsystem ledSubsystem;

    public resetCommand(IntakeSubsystem intakeSubsystem, LauncherSubsystem launcherSubsystem, HopperSubsystem hopperSubsystem, ClimberSubsystem climberSubsystem, LEDSubsystem ledSubsystem) {
        this.intakeSubsystem = intakeSubsystem;
        this.launcherSubsystem = launcherSubsystem;
        this.hopperSubsystem = hopperSubsystem;
        this.climberSubsystem = climberSubsystem;
        this.ledSubsystem = ledSubsystem;
        addRequirements(intakeSubsystem, launcherSubsystem, hopperSubsystem, climberSubsystem, ledSubsystem);
    }

    @Override
    public void initialize() {
        // Reset all subsystems to their default states
        intakeSubsystem.resetPID(); // Reset intake arm's PID controller
        intakeSubsystem.setIntakeSpeed(0); // Stop the intake rollers
        hopperSubsystem.setHopperSpeed(0); // Stop the hopper
        launcherSubsystem.setLaunchSpeed(0); // Stop the launcher motors
        launcherSubsystem.resetPID(); // Reset the launcher's PID controllers
        climberSubsystem.resetPID(); // Reset the climber's PID controllers
        ledSubsystem.allianceLarson(); // Reset LED pattern (or set to a default color)
    }

    @Override
    public void execute() {
        // No continuous action needed; all reset actions are performed in initialize()
    }
}
