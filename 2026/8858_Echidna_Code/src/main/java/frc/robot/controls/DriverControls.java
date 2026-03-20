package frc.robot.controls;

import org.ironmaple.simulation.SimulatedArena;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.util.Color;
import com.ctre.phoenix6.signals.RGBWColor;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.Constants;
import frc.robot.Robot;
import frc.robot.commands.activateIntake;
import frc.robot.commands.adjustangle;
import frc.robot.commands.aimAtTarget;
import frc.robot.commands.launchCommand;
import frc.robot.commands.moveClimber;
import frc.robot.commands.moveHopper;
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
            () -> controller.getLeftY() * 1,
            () -> controller.getLeftX() * 1
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

        // default to having intake active and hopper running at low speed to prevent jams, with arm lowered for intaking from the floor
        intakeSubsystem.setDefaultCommand(
            new activateIntake(
                intakeSubsystem,
                hopperSubsystem,
                Constants.INTAKE_ARM_LOWERED,
                Constants.INTAKE_ROLLER_SPEED,
                0.0
            )
        );

        if (DriverStation.isTest()) {
            // drivetrain.setDefaultCommand(driveFieldOrientedAngularVelocity);
            // Overrides drive command above!
            // Might be useful for robot-oriented controls in testing

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
            controller.start().onTrue((Commands.runOnce(drivetrain::zeroGyro)));

            // command to launch Fuel
            controller.rightTrigger(0.6).whileTrue(
                new launchCommand(
                    launcherSubsystem,
                    hopperSubsystem,
                    intakeSubsystem
                )
            );

            // command to climb up
            controller.rightBumper().whileTrue(
               new moveClimber(
                climberSubsystem,
                .4
               ) 
                
            );

            // command to climb down
            controller.leftBumper().whileTrue(
                new moveClimber(
                  climberSubsystem,
                  (-.3)
                )
            );

            controller.povLeft().onTrue(
                new adjustangle(1)
            );

            controller.povRight().onTrue(
                new adjustangle(-1)
            );

            controller.leftTrigger(0.6).whileTrue(
                new activateIntake(
                    intakeSubsystem,
                    hopperSubsystem,
                    Constants.INTAKE_ARM_LOWERED,
                    Constants.INTAKE_ROLLER_SPEED,
                    Constants.HOPPER_ROLLER_SPEED
                )
            );

            controller.x().whileTrue(
                new activateIntake(
                    intakeSubsystem,
                    hopperSubsystem,
                    Constants.INTAKE_ARM_LOWERED,
                    Constants.INTAKE_ROLLER_SPEED,
                    -Constants.HOPPER_ROLLER_SPEED
                )
            );

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
        }
  }

  public static Command fireFuel(SwerveSubsystem drivetrain /*, Superstructure superstructure */) {
    return Commands.runOnce(() -> {
            SimulatedArena.getInstance();

      /* TODO : Remove superstructure comments when superstructure is implemented
      GamePieceProjectile fuel = new RebuiltFuelOnFly(
          drivetrain.getPose().getTranslation(),
          new Translation2d(
              superstructure.turret.turretTranslation.getX() * -1,
              superstructure.turret.turretTranslation.getY()),
          drivetrain.getSwerveDrive().getRobotVelocity(),
          drivetrain.getPose().getRotation().rotateBy(superstructure.getAimRotation3d().toRotation2d()),
          superstructure.turret.turretTranslation.getMeasureZ(),

          // 0.5 times because we're applying spin to the fuel as we shoot it
          superstructure.getTangentialVelocity().times(0.5),
          superstructure.getHoodAngle());

      // Configure callbacks to visualize the flight trajectory of the projectile
      fuel.withProjectileTrajectoryDisplayCallBack(
          // Callback for when the note will eventually hit the target (if configured)
          (pose3ds) -> Logger.recordOutput("FieldSimulation/Shooter/ProjectileSuccessfulShot",
              pose3ds.toArray(Pose3d[]::new)),
          // Callback for when the note will eventually miss the target, or if no target
          // is configured
          (pose3ds) -> Logger.recordOutput("FieldSimulation/Shooter/ProjectileUnsuccessfulShot",
              pose3ds.toArray(Pose3d[]::new)));

      arena.addGamePieceProjectile(fuel); //*/
    });
  }
}