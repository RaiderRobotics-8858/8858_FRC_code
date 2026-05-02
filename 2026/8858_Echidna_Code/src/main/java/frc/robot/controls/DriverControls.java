package frc.robot.controls;

import org.ironmaple.simulation.SimulatedArena;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import java.util.Arrays;
import java.util.function.DoubleSupplier;
import com.ctre.phoenix6.signals.RGBWColor;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.Constants;
import frc.robot.Robot;
import frc.robot.commands.activateIntake;
import frc.robot.commands.aimAtTarget;
import frc.robot.commands.launchCommand;
import frc.robot.commands.moveClimber;
import frc.robot.commands.moveHoodtoPos;
import frc.robot.commands.moveHopper;
import frc.robot.commands.rumbleController;
import frc.robot.commands.setclimberpos;
import frc.robot.subsystems.HopperSubsystem;
import frc.robot.subsystems.IntakeSubsystem;
import frc.robot.subsystems.LEDSubsystem;
import frc.robot.subsystems.LauncherSubsystem;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.util.HubTimer;
import frc.robot.subsystems.ClimberSubsystem;
import swervelib.SwerveInputStream;

public class DriverControls {
private static boolean hubActive = false;
    public static void configure(
        int controllerPort,
        SwerveSubsystem drivetrain,
        LauncherSubsystem launcherSubsystem,
        IntakeSubsystem intakeSubsystem,
        HopperSubsystem hopperSubsystem,
        LEDSubsystem ledSubsystem,
        ClimberSubsystem climberSubsystem
    ) {
        CommandXboxController controller = new CommandXboxController(controllerPort);

        // Rotates driver translation inputs for alternate POV driving modes.
        // When enabled, offsets are alliance-based: +90° for Blue, -90° for Red.
        final boolean[] isAlternatePovEnabled = {false};
        SmartDashboard.putString(
            "Driver POV Mode",
            isAlternatePovEnabled[0] ? "Screen" : "Normal"
        );

        // Configure the match-time points (seconds remaining) when the sweep should start.
        final double sweepDurationSeconds = 0.45;
        final double sweepPeriodSeconds = 1.0;
        final double sweepGapSeconds = Math.max(0.0, sweepPeriodSeconds - sweepDurationSeconds);
        final double[] sweepStartTimesSeconds = {
            HubTimer.time(2, 10),
            HubTimer.time(1, 45),
            HubTimer.time(1, 20),
            HubTimer.time(0, 55),
            HubTimer.time(0, 30)
        };
        final boolean[] sweepTriggered = new boolean[sweepStartTimesSeconds.length];

        new Trigger(() -> DriverStation.isDisabled() && DriverStation.getMatchTime() > 130.0)
            .onTrue(
                Commands.runOnce(() -> Arrays.fill(sweepTriggered, false)).ignoringDisable(true)
            );

        for (int i = 0; i < sweepStartTimesSeconds.length; i++) {
            final int index = i;
            new Trigger(() -> HubTimer.isActive2sEarly() && DriverStation.isTeleop())
                .onTrue(
                    Commands.runOnce(() -> {
                        hubActive = true;
                        sweepTriggered[index] = true;
                        CommandScheduler.getInstance().schedule(
                            Commands.repeatingSequence(
                                new rumbleController(
                                    controller.getHID(),
                                    () -> DriverStation.getAlliance().orElse(DriverStation.Alliance.Blue)
                                            == DriverStation.Alliance.Blue
                                        ? rumbleController.Pattern.SWEEP_LEFT_TO_RIGHT
                                        : rumbleController.Pattern.SWEEP_RIGHT_TO_LEFT
                                ),
                                Commands.waitSeconds(sweepGapSeconds)
                            ).withTimeout(5.0)
                        );
                    })
                );
        }

        DoubleSupplier translationRotationOffset = () -> {
            if (!isAlternatePovEnabled[0]) {
                return 0.0;
            }
            return DriverStation.getAlliance().orElse(DriverStation.Alliance.Blue)
                    == DriverStation.Alliance.Blue
                ? (-Math.PI / 2.0)
                : (Math.PI / 2.0);
        };
        DoubleSupplier rotatedTranslationX = () -> {
            double x = controller.getLeftX() * -1;
            double y = controller.getLeftY() * -1;
            double offset = translationRotationOffset.getAsDouble();
            double cos = Math.cos(offset);
            double sin = Math.sin(offset);
            return (x * cos) - (y * sin);
        };
        DoubleSupplier rotatedTranslationY = () -> {
            double x = controller.getLeftX() * -1;
            double y = controller.getLeftY() * -1;
            double offset = translationRotationOffset.getAsDouble();
            double cos = Math.cos(offset);
            double sin = Math.sin(offset);
            return (x * sin) + (y * cos);
        };

        /**
         * Maps Controller inputs to a SwerveInputStream for
         * field-oriented driving with alliance-relative
         * controls and a deadband
         */
        SwerveInputStream driveInputStream = SwerveInputStream.of(
            drivetrain.getSwerveDrive(),
            rotatedTranslationY,
            rotatedTranslationX
        )
        .withControllerRotationAxis(() -> controller.getRightX() * -1)
        .robotRelative(false)
        .allianceRelativeControl(true)
        .scaleTranslation(Constants.TRANSLATION_SCALE)
        .scaleRotation(Constants.ROTATION_SCALE)
        .deadband(Constants.DEADBAND);

        SwerveInputStream driveInputStreamSlow = SwerveInputStream.of(
            drivetrain.getSwerveDrive(),
            rotatedTranslationY,
            rotatedTranslationX
        )
        .withControllerRotationAxis(() -> controller.getRightX() * -1)
        .robotRelative(false)
        .allianceRelativeControl(true)
        .scaleTranslation(Constants.TRANSLATION_SCALE_SLOW)
        .scaleRotation(Constants.ROTATION_SCALE_SLOW)
        .deadband(Constants.DEADBAND);

        drivetrain.setDefaultCommand(
            drivetrain.driveFieldOriented(driveInputStream).withName("Drive" + ".test")
        );

        ledSubsystem.setDefaultCommand(
            ledSubsystem.getDefaultCommand()
        );

        // Default command to keep the turret aimed at the target when not manually controlling it
        launcherSubsystem.setDefaultCommand(
            new aimAtTarget(
                launcherSubsystem
            )
        );

        if (DriverStation.isTest()) {

            controller.b().whileTrue(drivetrain.centerModulesCommand());
            controller.x().whileTrue(Commands.runOnce(drivetrain::lock, drivetrain).repeatedly());
            controller.y().onTrue((Commands.runOnce(drivetrain::zeroGyro)));

            controller.start().whileTrue(drivetrain.sysIdAngleMotorCommand());
            controller.back().whileTrue(drivetrain.sysIdDriveMotorCommand());
        } else if (Robot.isSimulation()) {
            // Fire fuel 10 times per second while button is held
            controller.back().whileTrue(
                Commands.repeatingSequence(
                    fireFuel(drivetrain /*, superstructure */),
                    Commands.waitSeconds(0.1)
                )
            );
        } else {

            // Alternate drive mode for viewing field video from the opposite side.
            // pressing both sticks toggles alliance-based rotation (+90° Blue, -90° Red).
            controller.leftStick().and(controller.rightStick()).onTrue(
                Commands.sequence(
                    Commands.runOnce(() -> {
                        isAlternatePovEnabled[0] = !isAlternatePovEnabled[0];
                        SmartDashboard.putString(
                            "Driver POV Mode",
                            isAlternatePovEnabled[0] ? "Normal" : "Screen"
                        );
                    }),
                    new rumbleController(
                        controller.getHID(),
                        () -> {
                            if (!isAlternatePovEnabled[0]) {
                                return rumbleController.Pattern.DOUBLE_BOTH;
                            }
                            return DriverStation.getAlliance().orElse(DriverStation.Alliance.Blue)
                                    == DriverStation.Alliance.Blue
                                ? rumbleController.Pattern.LEFT
                                : rumbleController.Pattern.RIGHT;
                        }
                    )
                )
            );

            // Software Reset
            controller.start().onTrue((Commands.runOnce(drivetrain::zeroGyro)));

            // command to launch Fuel
            controller.rightTrigger(0.6).whileTrue(
                new launchCommand(
                    launcherSubsystem,
                    hopperSubsystem,
                    ledSubsystem
                )
            );

            // slowly extend climber
            controller.povRight().whileTrue(
                new moveClimber(climberSubsystem, 0.1, false)
            );

            // slowly retract climber
            controller.povLeft().whileTrue(
                new moveClimber(climberSubsystem, -0.1, true)
            );

            // move the hood down
            controller.povDown().and(controller.a()).onTrue(
                new moveHoodtoPos(launcherSubsystem, Constants.HOOD_LOW_LIMIT)
            );

            // move the hood down
            controller.povDown().and(controller.b()).onTrue(
                new moveHoodtoPos(launcherSubsystem, Constants.HOOD_HIGH_LIMIT)
            );
            // Puts the intake arm all the way up.
            controller.x().onTrue(
                new activateIntake(intakeSubsystem,
                Constants.INTAKE_ARM_RAISED,
                0)
            );

            // Lower climber to preset position
            controller.leftBumper().onFalse(
                new setclimberpos(climberSubsystem, 0)
            );

            // Lower climber to preset position
            controller.rightBumper().onFalse(
                new setclimberpos(climberSubsystem, Constants.CLIMB_LOWER_POS)
            );

            // Raise climber to preset position
            controller.rightBumper().or(controller.leftBumper()).onTrue(
                new setclimberpos(climberSubsystem, Constants.CLIMB_EXTENDED_POS)
            );

            // Field-oriented drive at reduced speed for precision control while the right bumper is held
            controller.rightBumper().or(controller.rightTrigger(0.6)).whileTrue(
                drivetrain.driveFieldOriented(driveInputStreamSlow).withName("Drive" + ".test")
            );

            // Oil Spill Mode for Intake and Hopper
            controller.back().whileTrue(
                new ParallelCommandGroup(
                    new activateIntake(
                        intakeSubsystem,
                        Constants.INTAKE_ARM_LOWERED,
                        -Constants.INTAKE_ROLLER_SPEED
                    ),
                    new moveHopper(
                        hopperSubsystem,
                        -Constants.HOPPER_ROLLER_SPEED
                        )
                )
            );

            // Default intake mode is active
            controller.leftTrigger(0.6).whileFalse(
                new activateIntake(
                    intakeSubsystem,
                    Constants.INTAKE_ARM_LOWERED,
                    Constants.INTAKE_ROLLER_SPEED
                )
            );

            // Raises the intake arm
            controller.leftTrigger(0.6).whileTrue(
                new activateIntake(
                    intakeSubsystem,
                    Constants.INTAKE_ARM_RAISED,
                    0
                )
            );

            // Begin Color Modes
            controller.povUp().and(controller.a()).onTrue(
                ledSubsystem.larsonWithColor(new RGBWColor(Color.kGreen))
            );

            controller.povUp().and(controller.b()).onTrue(
                ledSubsystem.larsonWithColor(new RGBWColor(Color.kRed))
            );

            controller.povUp().and(controller.x()).onTrue(
                ledSubsystem.larsonWithColor(new RGBWColor(Color.kBlue))
            );

            controller.povUp().and(controller.y()).onTrue(
                ledSubsystem.larsonWithColor(new RGBWColor(Color.kYellow))
            );
            // End Color Modes
        }
  }

  public static Command fireFuel(SwerveSubsystem drivetrain /*, Superstructure superstructure */) {
    return Commands.runOnce(() -> {
            SimulatedArena.getInstance();
    });
  }
}