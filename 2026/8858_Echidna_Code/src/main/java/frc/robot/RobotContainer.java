// Copyright (c) FIRST and other WPILib contributors.

// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import java.io.File;

import com.pathplanner.lib.auto.AutoBuilder;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;

import static edu.wpi.first.units.Units.Inches;
import edu.wpi.first.units.measure.Distance;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.commands.launchCommand;
import frc.robot.controls.DriverControls;
import frc.robot.subsystems.HopperSubsystem;
import frc.robot.subsystems.IntakeSubsystem;
import frc.robot.subsystems.LEDSubsystem;
import frc.robot.subsystems.LauncherSubsystem;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.util.HubTimer;
import frc.robot.subsystems.ClimberSubsystem;
import swervelib.SwerveDrive;

public class RobotContainer {

    private final SwerveSubsystem drive = new SwerveSubsystem(new File(Filesystem.getDeployDirectory(), "swerve/neo"));
    private final LauncherSubsystem launcherSubsystem = new LauncherSubsystem(drive::getPose);
    private final HopperSubsystem hopperSubsystem = new HopperSubsystem();
    private final IntakeSubsystem intakeSubsystem = new IntakeSubsystem();
    private final LEDSubsystem ledSubsystem = new LEDSubsystem();
    private final ClimberSubsystem climberSubsystem = new ClimberSubsystem();

    private SendableChooser<Command> autoChooser;

    // Track current alliance for change detection
    private Alliance currentAlliance = Alliance.Red;

    public RobotContainer()
    {
        // Configure the trigger bindings
        configureBindings();
        // buildNamedAutoCommands();

        // Initialize alliance (default to red if not present)
        onAllianceChanged(getAlliance());

        // Set up trigger to detect alliance changes
        new Trigger(() -> getAlliance() != currentAlliance)
            .onTrue(Commands.runOnce(() -> onAllianceChanged(getAlliance())).ignoringDisable(true));

        // Triggers for auto aim/pass poses
        new Trigger(() -> isInAllianceZone())
            .onChange(Commands.runOnce(() -> onZoneChanged()).ignoringDisable(true));

        new Trigger(() -> isOnAllianceOutpostSide())
            .onChange(Commands.runOnce(() -> onZoneChanged()).ignoringDisable(true));

        if (!Robot.isReal() || true) {
        DriverStation.silenceJoystickConnectionWarning(true);
        }

        // Have the autoChooser pull in all PathPlanner autos as options. PathPlanner's
        // AutoBuilder may not be configured on the RoboRIO if GUI settings are
        // missing or setupPathPlanner failed, which throws at runtime and will
        // crash the robot. Try to build the chooser and fall back to an empty
        // chooser when that happens.
        try {
            autoChooser = AutoBuilder.buildAutoChooser();
        } catch (Exception e) {
            // Report the problem but keep the robot running with a simple chooser
            // so the rest of the robot code can operate.
            DriverStation.reportWarning("AutoBuilder.buildAutoChooser() failed: " + e.toString(), true);
            autoChooser = new SendableChooser<>();
        }

        // Set the default auto (do nothing)
        autoChooser.setDefaultOption("Do Nothing", Commands.none());

        // Add a simple auto option to have the robot drive forward for 1 second then
        // stop
        autoChooser.addOption("Drive Forward",
            new SequentialCommandGroup(
                drive.driveBackwards().withTimeout(2),
                new launchCommand(launcherSubsystem, hopperSubsystem, ledSubsystem),
                drive.driveRight().withTimeout(2)
            )
        );

        // Add each auto name returned by PathPlanner's AutoBuilder as an option
        // AutoBuilder.getAllAutoNames() may return a collection/array of names.
        try {
            for (Object nameObj : AutoBuilder.getAllAutoNames()) {
                String name = String.valueOf(nameObj);
                try {
                    autoChooser.addOption(name, drive.getAutonomousCommand(name));
                } catch (Exception e) {
                    DriverStation.reportWarning("Failed to create auto command for '" + name + "': " + e.toString(), false);
                }
            }
        } catch (Exception e) {
            DriverStation.reportWarning("Failed to list autos from AutoBuilder: " + e.toString(), false);
        }

        // Put the autoChooser on the SmartDashboard
        SmartDashboard.putData("Auto Chooser", autoChooser);

        DriverStation.silenceJoystickConnectionWarning(true);
        configureBindings();
    }

    private void configureBindings(){
        DriverControls.configure(
          Constants.XBOX_CONTROLLER_PORT,
          drive,
          launcherSubsystem,
          intakeSubsystem,
          hopperSubsystem,
          ledSubsystem,
          climberSubsystem
        );
    }

    public Command getAutonomousCommand() {
      return autoChooser.getSelected();
    }

    public SwerveDrive getSwerveDrive() {
        return drive.getSwerveDrive();
    }

    public Pose2d getRobotPose() {
        return drive.getPose();
    }

    private Alliance getAlliance() {
        return DriverStation.getAlliance().orElse(Alliance.Red);
    }

    private boolean isInAllianceZone() {
        Alliance alliance = getAlliance();
        Distance blueZone = Inches.of(182);
        Distance redZone = Inches.of(469);

        if (alliance == Alliance.Blue && drive.getPose().getMeasureX().lt(blueZone)) {
            return true;
        } else if (alliance == Alliance.Red && drive.getPose().getMeasureX().gt(redZone)) {
            return true;
        }

        return false;
    }

    private boolean isOnAllianceOutpostSide() {
        Alliance alliance = getAlliance();
        Distance midLine = Inches.of(158.84375);

        if (alliance == Alliance.Blue && drive.getPose().getMeasureY().lt(midLine)) {
            return true;
        } else if (alliance == Alliance.Red && drive.getPose().getMeasureY().gt(midLine)) {
            return true;
        }

        return false;
    }

    private void onZoneChanged() {
        // if (isInAllianceZone()) {
        //   superstructure.setAimPoint(Constants.AimPoints.getAllianceHubPosition());
        // } else {
        //   if (isOnAllianceOutpostSide()) {
        //     superstructure.setAimPoint(Constants.AimPoints.getAllianceOutpostPosition());
        //   } else {
        //     superstructure.setAimPoint(Constants.AimPoints.getAllianceFarSidePosition());
        //   }
        // }
    }

    private void onAllianceChanged(Alliance alliance) {
        currentAlliance = alliance;

        // Update aim point based on alliance
        // if (alliance == Alliance.Blue) {
        //   superstructure.setAimPoint(Constants.AimPoints.BLUE_HUB.value);
        // } else {
        //   superstructure.setAimPoint(Constants.AimPoints.RED_HUB.value);
        // }

        System.out.println("Alliance changed to: " + alliance);
    }

    public void periodic() {
        // Launcher stats
        SmartDashboard.putNumber("Launcher/LaunchSpeed", launcherSubsystem.getLaunchSpeed());
        SmartDashboard.putNumber("Launcher/TurretAngle", launcherSubsystem.getTurretAngleDegrees());
        SmartDashboard.putNumber("Launcher/Turret Encoder", launcherSubsystem.getTurretAngle());
        SmartDashboard.putNumber("Launcher/KickerCurrent", launcherSubsystem.getKickerCurrent());
        SmartDashboard.putNumber("Launcher/KickerVelo", launcherSubsystem.getKickerVelocity());
        SmartDashboard.putNumber("Launcher/LauncherCurrent", launcherSubsystem.getLauncherCurrent());
        SmartDashboard.putBoolean("Launcher/TurretZero", launcherSubsystem.getTurretZeroOutput());
        SmartDashboard.putBoolean("Launcher/LaunchSense", launcherSubsystem.getLaunchOutput());


        // Intake stats
        SmartDashboard.putNumber("Intake/ArmPosition", intakeSubsystem.getIntakeArmPosition());
        SmartDashboard.putNumber("Launcher Temp", launcherSubsystem.getLauncherTemp());
        SmartDashboard.putNumber("KickerTemp", launcherSubsystem.getKickerTemp());
        SmartDashboard.putNumber("Turret Angle Temp", launcherSubsystem.getTurretAngleTemp());
        SmartDashboard.putNumber("Hopper Temp", hopperSubsystem.getHopperTemp());
        SmartDashboard.putNumber("Intake Temp", intakeSubsystem.getIntakeTemp());
        SmartDashboard.putNumber("Roller Temp", intakeSubsystem.getRollerTemp());

        SmartDashboard.putNumber("Intake/HopperCurrent", hopperSubsystem.getCurrent());
        SmartDashboard.putNumber("Climb/Position", climberSubsystem.getClimberPos());

        //Hub Values
        SmartDashboard.putBoolean("Game/Hub Active", HubTimer.isActive());
        SmartDashboard.putNumber("Game/Match Time", DriverStation.getMatchTime());

        Pose3d pose = drive.getPose3d();
        SmartDashboard.putNumber("Robot/pos x", pose.getX());
        SmartDashboard.putNumber("Robot/pos y", pose.getY());
        SmartDashboard.putNumber("Robot/rotation", pose.getRotation().getAngle());
    }
}