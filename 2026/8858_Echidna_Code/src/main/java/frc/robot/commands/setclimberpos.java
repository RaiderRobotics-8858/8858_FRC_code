package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.ClimberSubsystem;


public class setclimberpos extends Command{
    private final ClimberSubsystem climberSubsystem;
    private final double TargetPosition;

    public setclimberpos(ClimberSubsystem climberSubsystem, double TargetPosition) {
        this.climberSubsystem = climberSubsystem;
        this.TargetPosition = TargetPosition;
        addRequirements(climberSubsystem);
    }

    @Override
    public void initialize(){
        climberSubsystem.resetPID();
    }

    @Override
    public void execute() {
        climberSubsystem.setClimberPos(TargetPosition);

    }

    @Override
    public void end(boolean interrupted){
        climberSubsystem.setClimberSpeed(0);
    }
}
