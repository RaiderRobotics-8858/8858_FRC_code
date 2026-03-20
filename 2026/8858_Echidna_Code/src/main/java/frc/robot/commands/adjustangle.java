package frc.robot.commands;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;

public class adjustangle extends Command {
    private final double angle;

    /** Moves the hopper at a specified speed
     * @param climberSpeed The speed to set the hopper motors to (range -1.0 to 1.0)
     */
    public adjustangle(
        double angle) {
        this.angle = angle;
    }

    @Override
    public void execute() {
        double currentangle = SmartDashboard.getNumber("Launcher/AngleAdjust", angle);
        SmartDashboard.putNumber("Launcher/AngleAdjust", currentangle+angle);
        
    }
    
}
