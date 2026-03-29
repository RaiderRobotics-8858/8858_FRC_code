package frc.robot.commands;

import com.ctre.phoenix6.signals.RGBWColor;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Constants;
import frc.robot.Constants.AimPoints;
import frc.robot.subsystems.HopperSubsystem;
import frc.robot.subsystems.LEDSubsystem;
import frc.robot.subsystems.LauncherSubsystem;

public class launchCommand extends Command {
    private final LauncherSubsystem launcherSubsystem;
    private final HopperSubsystem hopperSubsystem;
    private final LEDSubsystem ledSubsystem;
    private boolean hit_speed_flag;
    private boolean lastLaunchOutputState;

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
    public launchCommand(LauncherSubsystem launcherSubsystem, HopperSubsystem hopperSubsystem, LEDSubsystem ledSubsystem) {
        this.launcherSubsystem = launcherSubsystem;
        this.hopperSubsystem = hopperSubsystem;
        this.ledSubsystem = ledSubsystem;
        addRequirements(launcherSubsystem, hopperSubsystem);
    }

    @Override
    public void initialize() {
        launcherSubsystem.resetPID(); // Reset PID controllers for speed and angle
        hit_speed_flag = false;
        lastLaunchOutputState = true;
    }

    @Override
    public void execute() {
        AimPoints target = launcherSubsystem.findTarget();

        double targetSpeed = launcherSubsystem.getLaunchSpeed(target);
        launcherSubsystem.setLaunchSpeed(targetSpeed); // Set launch motors to maintain target speed
        ledSubsystem.larsonWithColor(new RGBWColor(Color.kYellow)); // Set LED pattern to indicate launching

        if (launcherSubsystem.isAtTargetSpeed(targetSpeed) && !hit_speed_flag) {
            if(!SmartDashboard.getBoolean("TESTMODE", false)){
                hit_speed_flag = true;
            }
        }

        if (hit_speed_flag) {
            hopperSubsystem.setHopperSpeed(SmartDashboard.getNumber("Intake/RollerSpeed", Constants.HOPPER_ROLLER_SPEED));
            launcherSubsystem.activateKicker(); // Feed the kicker only when at target speed and angle
        } else {
            hopperSubsystem.setHopperSpeed(0);
        }

        // Keep track of how long its been since Fuel was launched
        if (!launcherSubsystem.getLaunchOutput()){
            SmartDashboard.putNumber("Launcher/Time Since Last Fuel", DriverStation.getMatchTime());
        }

        // Keep track of how many fuel get launched (triggered on rising edge of launch output, indicating fuel is leaving the turret)
        if (launcherSubsystem.getLaunchOutput() && !lastLaunchOutputState) {
            SmartDashboard.putNumber("Launcher/Fuel Launched", SmartDashboard.getNumber("Launcher/Fuel Launched", 0) + 1);
        }
        lastLaunchOutputState = launcherSubsystem.getLaunchOutput();
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
        double lastfueltime = SmartDashboard.getNumber("Launcher/Time Since Last Fuel", 0);
        if (lastfueltime - 3 > DriverStation.getMatchTime()){
            SmartDashboard.putNumber("Launcher/Time Since Last Fuel", -1); // reset the flag to not prevent future launches
            return true;
        } else {
            return false; // always active command, never finishes on its own
        }
    }
}