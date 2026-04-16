package frc.robot.controls;

import org.ironmaple.simulation.SimulatedArena;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.util.Color;
import com.ctre.phoenix6.signals.RGBWColor;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.Constants;
import frc.robot.Robot;
import frc.robot.commands.activateIntake;
import frc.robot.commands.aimAtTarget;
import frc.robot.commands.launchCommand;
import frc.robot.commands.moveClimber;
import frc.robot.commands.moveHoodtoPos;
import frc.robot.commands.moveHopper;
import frc.robot.commands.resetCommand;
import frc.robot.commands.setclimberpos;
import frc.robot.subsystems.HopperSubsystem;
import frc.robot.subsystems.IntakeSubsystem;
import frc.robot.subsystems.LEDSubsystem;
import frc.robot.subsystems.LauncherSubsystem;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.subsystems.ClimberSubsystem;
import swervelib.SwerveInputStream;

public class DriverControls {

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

        /**
         * Maps Controller inputs to a SwerveInputStream for
         * field-oriented driving with alliance-relative
         * controls and a deadband
         */
        SwerveInputStream driveInputStream = SwerveInputStream.of(
            drivetrain.getSwerveDrive(),
            () -> controller.getLeftY() * -1,
            () -> controller.getLeftX() * -1
        )
        .withControllerRotationAxis(() -> controller.getRightX() * -1)
        .robotRelative(false)
        .allianceRelativeControl(true)
        .scaleTranslation(Constants.TRANSLATION_SCALE)
        .scaleRotation(Constants.ROTATION_SCALE)
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

            // Lower climber to preset position
            controller.leftBumper().onTrue(
                    new setclimberpos(climberSubsystem, Constants.CLIMB_LOWER_POS)
            );

            // Raise climber to preset position
            controller.rightBumper().onTrue(
                    new setclimberpos(climberSubsystem, Constants.CLIMB_EXTENDED_POS)
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