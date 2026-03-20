package frc.robot.commands;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Constants;
import frc.robot.Constants.AimPoints;
import frc.robot.subsystems.HopperSubsystem;
import frc.robot.subsystems.IntakeSubsystem;
import frc.robot.subsystems.LauncherSubsystem;

public class launchCommand extends Command {
    private final LauncherSubsystem launcherSubsystem;
    private final HopperSubsystem hopperSubsystem;
    private final IntakeSubsystem intakeSubsystem;
    private boolean hit_speed_flag;

    /**
     * Creates a command to launch fuel at a specified target speed and pose.
     * The command will continuously adjust the launcher's speed and angle to
     * maintain the target speed and aim at the target pose, activating the
     * kicker when conditions are met.
     *
     * @param launcherSubsystem The launcher subsystem to control
     * @param targetSpeed The target speed for the launcher
     * @param targetPose The pose of the target for the launcher to aim at
     */
    public launchCommand(LauncherSubsystem launcherSubsystem, HopperSubsystem hopperSubsystem, IntakeSubsystem intakeSubsystem) {
        this.launcherSubsystem = launcherSubsystem;
        this.hopperSubsystem = hopperSubsystem;
        this.intakeSubsystem = intakeSubsystem;
        addRequirements(launcherSubsystem);
    }

    @Override
    public void initialize() {
        launcherSubsystem.resetPID(); // Reset PID controllers for speed and angle
        hit_speed_flag = false;
    }

    @Override
    public void execute() {
        AimPoints target = launcherSubsystem.findTarget();

        double targetSpeed = launcherSubsystem.getLaunchSpeed(target);
        launcherSubsystem.setLaunchSpeed(targetSpeed); // Set launch motors to maintain target speed
        // launcherSubsystem.findTarget();
        intakeSubsystem.setIntakeSpeed(0.6); // Set intake roller speed

        if (launcherSubsystem.isAtTargetSpeed(targetSpeed) && !hit_speed_flag) {
            if(!SmartDashboard.getBoolean("TESTMODE", false)){
                hit_speed_flag = true;
            }
        }

        if (hit_speed_flag) {
            hopperSubsystem.setHopperSpeed(SmartDashboard.getNumber("Intake/RollerSpeed", Constants.HOPPER_ROLLER_SPEED));
            intakeSubsystem.setIntakeArmPosition(Constants.INTAKE_ARM_RAISED);
            launcherSubsystem.activateKicker(); // Feed the kicker only when at target speed and angle
        } else {
            hopperSubsystem.setHopperSpeed(0);
            intakeSubsystem.setIntakeArmPosition(Constants.INTAKE_ARM_LOWERED);
        }
    }

    @Override
    public void end(boolean interrupted) {
        hit_speed_flag = false;
        launcherSubsystem.setLaunchSpeed(0); // Stop launch motors when command ends
        launcherSubsystem.setTurretSpeed(0); // Stop turret movement when command ends
        launcherSubsystem.setKickerSpeed(0); // Stop kicker movement when command ends
        hopperSubsystem.setHopperSpeed(0);
    }

    @Override
    public boolean isFinished() {
        return false; // always active command, never finishes on its own
    }
}