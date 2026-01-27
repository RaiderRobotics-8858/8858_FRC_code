package frc.robot.commands.elevator;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Constants;
import frc.robot.subsystems.swervedrive.ElevatorSubsystem;

public class MoveElevatorToPositionAuto extends Command {
    private final ElevatorSubsystem elevatorSubsystem;
    private final double targetPosition;

    public MoveElevatorToPositionAuto(ElevatorSubsystem elevatorSubsystem, double targetPosition) {
        this.elevatorSubsystem = elevatorSubsystem;
        this.targetPosition = targetPosition; // set target position
        addRequirements(elevatorSubsystem); // add requirement so that multiple commands using the same subsystem don't run at the same time
    }

    @Override
    public void initialize(){
        elevatorSubsystem.resetPID(); // reset PID controller
        SmartDashboard.putString("elevator command", "init");
    }

    @Override
    public void execute(){
        elevatorSubsystem.MoveElevatorToPosition(targetPosition); // move elevator to target position
        SmartDashboard.putString("elevator command", "execute");
    }

    @Override
    public boolean isFinished(){
        if(Math.abs(elevatorSubsystem.getElevatorVelocity()) < 20){
            return (Math.abs(targetPosition - elevatorSubsystem.getEncoderPosition()) <= 10.0);
        } else {
            if(DriverStation.isAutonomous()){
                if ( Math.abs(targetPosition - elevatorSubsystem.getEncoderPosition()) <= Constants.ELE_TOL * 1.5){
                    return true;
                }else{
                    return false;
                }
                } else{
                if ( Math.abs(targetPosition - elevatorSubsystem.getEncoderPosition()) <= Constants.ELE_TOL){
                    return true;
                }else{
                    return false;
                }
            }
        }
    }

    @Override
    public void end(boolean interrupted){
        elevatorSubsystem.MoveElevatorToPosition(targetPosition);
        SmartDashboard.putString("elevator command", "end");
    }
}