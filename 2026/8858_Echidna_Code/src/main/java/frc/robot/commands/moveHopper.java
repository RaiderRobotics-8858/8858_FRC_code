package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.HopperSubsystem;

public class moveHopper extends Command {
    private final HopperSubsystem hopperSubsystem;
    private final double hopperSpeed;

    /** Moves the hopper at a specified speed
     * @param hopperSubsystem The hopper subsystem to control
     * @param hopperSpeed The speed to set the hopper motors to (range -1.0 to 1.0)
     */
    public moveHopper(HopperSubsystem hopperSubsystem, double hopperSpeed) {
        this.hopperSubsystem = hopperSubsystem;
        this.hopperSpeed = hopperSpeed;
        addRequirements(hopperSubsystem);
    }

    @Override
    public void execute() {
        hopperSubsystem.setHopperSpeed(hopperSpeed);
        
    }

    @Override
    public void end(boolean interrupted) {
        hopperSubsystem.setHopperSpeed(0); // Stop hopper movement when command ends
    }
}
