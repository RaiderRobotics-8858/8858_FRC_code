package frc.robot.commands.elevator;

import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.swervedrive.ElevatorSubsystem;

public class MoveElevator extends Command {
    private final ElevatorSubsystem elevatorSubsystem;
    private final double speed;

    public MoveElevator(ElevatorSubsystem elevatorSubsystem, double speed) {
        this.elevatorSubsystem = elevatorSubsystem;
        this.speed = speed; // motor speed
        addRequirements(elevatorSubsystem); // add requirement so that multiple commands using the same subsystem don't run at the same time
    }

    @Override
    public void initialize(){ // runs when the command starts
        elevatorSubsystem.resetPID(); // reset PID controller
    }

    @Override
    public void execute(){ // runs periodically while the command is scheduled
        elevatorSubsystem.MoveElevator(speed); // move elevator at speed
    }

    @Override
    public boolean isFinished(){ // check if the command should stop running
        return false; // never finish
    }

    @Override
    public void end(boolean interrupted){ // runs when the command ends
        elevatorSubsystem.MoveElevator(0); // stop elevator when command ends
    }
}