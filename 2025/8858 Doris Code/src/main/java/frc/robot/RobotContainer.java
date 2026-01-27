// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import java.io.File;

import com.ctre.phoenix.led.CANdle;
import com.ctre.phoenix.led.TwinkleAnimation;
import com.ctre.phoenix.led.TwinkleAnimation.TwinklePercent;
import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.auto.NamedCommands;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.button.CommandJoystick;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.Constants.OperatorConstants;
import frc.robot.commands.camera.SwitchCamera;
import frc.robot.commands.climber.MoveClimber;
import frc.robot.commands.climber.MoveClimberToPosition;
import frc.robot.commands.controller.RumbleCommand;
import frc.robot.commands.elevator.MoveElevatorToPosition;
import frc.robot.commands.elevator.MoveElevatorToPositionAuto;
import frc.robot.commands.intake.AutoCoralIntake;
import frc.robot.commands.intake.AutoScoreAlgae;
import frc.robot.commands.intake.AutoScoreCoral;
import frc.robot.commands.intake.algaeIntake;
import frc.robot.commands.intake.algaeSmartIntake;
import frc.robot.commands.intake.coralIntake;
import frc.robot.commands.intake.preScoreAutoCoralIntake;
import frc.robot.commands.leds.SetLEDStateCommand;
import frc.robot.commands.vision.AutoAlign;
import frc.robot.commands.vision.DriveToAprilTagFieldPose;
import frc.robot.commands.vision.DriveToPositionPathPlanner;
import frc.robot.subsystems.swervedrive.AlgaeSubsystem;
import frc.robot.subsystems.swervedrive.CameraSubsystem;
import frc.robot.subsystems.swervedrive.ClimberSubsystem;
import frc.robot.subsystems.swervedrive.CoralIntakeSubsystem;
import frc.robot.subsystems.swervedrive.ElevatorSubsystem;
import frc.robot.subsystems.swervedrive.LEDSubsystem;
import frc.robot.subsystems.swervedrive.LimelightSubsystem;
import frc.robot.subsystems.swervedrive.SwerveSubsystem;
import swervelib.SwerveInputStream;

/**
 * This class is where the bulk of the robot should be declared. Since
 * Command-based is a "declarative" paradigm, very
 * little robot logic should actually be handled in the {@link Robot} periodic
 * methods (other than the scheduler calls).
 * Instead, the structure of the robot (including subsystems, commands, and
 * trigger mappings) should be declared here.
 */
public class RobotContainer {

    // Replace with CommandPS4Controller or CommandJoystick if needed
    public final CommandXboxController driverXbox = new CommandXboxController(0);
    public final CommandJoystick controller_2 = new CommandJoystick(1);
    public final CommandJoystick safety_controller = new CommandJoystick(2);
    // The robot's subsystems and commands are defined here...
    public final SwerveSubsystem drivebase = new SwerveSubsystem(new File(Filesystem.getDeployDirectory(),
            "swerve/neo"));

    public final AutoAlign autoAlign = new AutoAlign(drivebase);

    TwinkleAnimation twinkle_anim = new TwinkleAnimation(0, 255, 0, 0, 0.2, 38, TwinklePercent.Percent18);

    public LimelightSubsystem getLimelightSubsystem() {
        return limelightSubsystem;
    }

    private final ElevatorSubsystem elevatorSubsystem = new ElevatorSubsystem();
    private final AlgaeSubsystem algaeSubsystem = new AlgaeSubsystem();
    private final CoralIntakeSubsystem coralSubsystem = new CoralIntakeSubsystem();
    private final ClimberSubsystem climberSubsystem = new ClimberSubsystem();
    private final CameraSubsystem cameraSubsystem = new CameraSubsystem();
    private final LimelightSubsystem limelightSubsystem = new LimelightSubsystem();
    private final LEDSubsystem ledSubsystem = LEDSubsystem.getInstance();
    private int preset = 0;

    double algaeTuneValue = 0.0;
    double coralTuneValue = 0.0;
    double driveSpeedTuner = 1.0;
    double rotateSpeedTuner = 1.0;
    boolean student_student_driver = false;

    public CANdle candle = new CANdle(OperatorConstants.CAN_dle);

    /**
     * Converts driver input into a field-relative ChassisSpeeds that is controlled
     * by angular velocity.
     */
    SwerveInputStream driveAngularVelocity = SwerveInputStream.of(drivebase.getSwerveDrive(),
            () -> -driverXbox.getLeftY() * -1,
            () -> -driverXbox.getLeftX() * -1)
            .withControllerRotationAxis(driverXbox::getRightX)
            .deadband(OperatorConstants.DEADBAND)
            .scaleTranslation(0.8)
            .allianceRelativeControl(true);

    /**
     * Clone's the angular velocity input stream and converts it to a fieldRelative
     * input stream.
     */
    SwerveInputStream driveDirectAngle = driveAngularVelocity.copy().withControllerHeadingAxis(driverXbox::getRightX,
            driverXbox::getRightY)
            .headingWhile(true);

    /**
     * Clone's the angular velocity input stream and converts it to a robotRelative
     * input stream.
     */
    SwerveInputStream driveRobotOriented = driveAngularVelocity.copy().robotRelative(true)
            .allianceRelativeControl(false);

    SwerveInputStream driveAngularVelocityKeyboard = SwerveInputStream.of(drivebase.getSwerveDrive(),
            () -> -Math.pow(driverXbox.getLeftY(),3),
            () -> -Math.pow(driverXbox.getLeftX(),3))
            .withControllerRotationAxis(() -> driverXbox.getRawAxis(
                    2))
            .deadband(OperatorConstants.DEADBAND)
            .scaleTranslation(0.8)
            .allianceRelativeControl(true);
    // Derive the heading axis with math!
    SwerveInputStream driveDirectAngleKeyboard = driveAngularVelocityKeyboard.copy()
            .withControllerHeadingAxis(() -> Math.sin(driverXbox.getRawAxis(2) * Math.PI) * (Math.PI * 2),
                    () -> Math.cos(driverXbox.getRawAxis(2) * Math.PI) * (Math.PI * 2))
            .headingWhile(true);
    public Command activateGodzilla = new SetLEDStateCommand(LEDSubsystem.Mode.RED_GODZILLA, ledSubsystem);
    public Command activateTwinkle = new SetLEDStateCommand(LEDSubsystem.Mode.TWINKLE_BLUE, ledSubsystem);

    /**
     * The container for the robot. Contains subsystems, OI devices, and commands.
     */
    public RobotContainer() {
        // Configure the trigger bindings
        configureBindings();
        // configureButtonBindings();
        DriverStation.silenceJoystickConnectionWarning(true);
        NamedCommands.registerCommand("test", Commands.print("I EXIST"));
        NamedCommands.registerCommand("L1", new MoveElevatorToPositionAuto(elevatorSubsystem, Constants.ELE_L1));
        NamedCommands.registerCommand("L2", new MoveElevatorToPositionAuto(elevatorSubsystem, Constants.ELE_L2));
        NamedCommands.registerCommand("L3", new MoveElevatorToPositionAuto(elevatorSubsystem, Constants.ELE_L3));
        NamedCommands.registerCommand("L4", new MoveElevatorToPositionAuto(elevatorSubsystem, Constants.ELE_L4));
        NamedCommands.registerCommand("ALGH", new MoveElevatorToPositionAuto(elevatorSubsystem, Constants.ELE_ALGHI));
        NamedCommands.registerCommand("ALGL", new MoveElevatorToPositionAuto(elevatorSubsystem, Constants.ELE_ALGLO));
        NamedCommands.registerCommand("ALGL_IN", new algaeSmartIntake(algaeSubsystem));
        NamedCommands.registerCommand("CoralIntake", new AutoCoralIntake(coralSubsystem));
        NamedCommands.registerCommand("PrepCoral", new preScoreAutoCoralIntake(coralSubsystem));
        NamedCommands.registerCommand("ScoreCoral", new AutoScoreCoral(coralSubsystem));
        NamedCommands.registerCommand("Godzilla", activateGodzilla);
    }


    /**
     * Use this method to define your trigger->command mappings. Triggers can be
     * created via the
     * {@link Trigger#Trigger(java.util.function.BooleanSupplier)} constructor with
     * an arbitrary predicate, or via the
     * named factories in
     * {@link edu.wpi.first.wpilibj2.command.button.CommandGenericHID}'s subclasses
     * for
     * {@link CommandXboxController
     * Xbox}/{@link edu.wpi.first.wpilibj2.command.button.CommandPS4Controller PS4}
     * controllers or {@link edu.wpi.first.wpilibj2.command.button.CommandJoystick
     * Flight joysticks}.
     */
    private void configureBindings() {
        SmartDashboard.putStringArray("Auto List", AutoBuilder.getAllAutoNames().toArray(new String[0]));
        Command driveFieldOrientedDirectAngle = drivebase.driveFieldOriented(driveDirectAngle);
        SwerveInputStream normalDrive = SwerveInputStream.of(drivebase.getSwerveDrive(),
        () -> (driveSpeedTuner * driverXbox.getLeftY()),
        () -> (driveSpeedTuner * driverXbox.getLeftX()))
        .withControllerRotationAxis(() -> (rotateSpeedTuner * driverXbox.getRightX()) * -0.85)
        .deadband(OperatorConstants.DEADBAND)
        .scaleTranslation(0.8)
        .allianceRelativeControl(true);
        Command driveFieldOrientedAnglularVelocity = drivebase.driveFieldOriented(normalDrive);
        Command driveLeftCoral = drivebase.driveFieldOriented(SwerveInputStream.of(drivebase.getSwerveDrive(),
            () -> -0.1,
            () -> 0)
            .withControllerRotationAxis(() -> 0 * -0.85)
            .scaleTranslation(0.8)
            .robotRelative(true));
        Command driveRightCoral = drivebase.driveFieldOriented(SwerveInputStream.of(drivebase.getSwerveDrive(),
            () -> 0.1,
            () -> 0)
            .withControllerRotationAxis(() -> 0 * -0.85)
            .scaleTranslation(0.8)
            .robotRelative(true));
        Command driveRobotOrientedAngularVelocity = drivebase.driveFieldOriented(driveRobotOriented);
        Command driveSetpointGen = drivebase.driveWithSetpointGeneratorFieldRelative(
                driveDirectAngle);
        Command driveFieldOrientedDirectAngleKeyboard = drivebase.driveFieldOriented(driveDirectAngleKeyboard);
        Command driveFieldOrientedAnglularVelocityKeyboard = drivebase.driveFieldOriented(driveAngularVelocityKeyboard);
        Command driveSetpointGenKeyboard = drivebase.driveWithSetpointGeneratorFieldRelative(
                driveDirectAngleKeyboard);

        if (RobotBase.isSimulation()) {
            drivebase.setDefaultCommand(driveFieldOrientedDirectAngleKeyboard);
        } else {
            drivebase.setDefaultCommand(driveFieldOrientedAnglularVelocity);
        }

        if (Robot.isSimulation()) {
            driverXbox.start()
                    .onTrue(Commands.runOnce(() -> drivebase.resetOdometry(new Pose2d(3, 3, new Rotation2d()))));
            driverXbox.button(1).whileTrue(drivebase.sysIdDriveMotorCommand());

        }
        if (DriverStation.isTest()) {
            drivebase.setDefaultCommand(driveFieldOrientedAnglularVelocity); // Overrides drive command above!

            driverXbox.x().whileTrue(Commands.runOnce(drivebase::lock, drivebase).repeatedly());
            driverXbox.y().whileTrue(drivebase.driveToDistanceCommand(1.0, 0.2));
            driverXbox.start().onTrue((Commands.runOnce(drivebase::zeroGyro)));
            driverXbox.back().whileTrue(drivebase.centerModulesCommand());
        } else { // configure controls for teleop
            /*
            Notes for controls:

            .onTrue() will run a command the button is initially pressed
            .onFalse() will run a command when the button is released

            .whileTrue() will run a command when the button is pressed and cancel it when the button is released
            .whileFalse() will run a command when the button is released and cancel it when the button is pressed

            a ParallelCommandGroup will run multiple commands at the same time until the last command in the list finishes

            general control format: driverXbox.<button name>().<when it should run>(command);
            */

            // Level 1 coral (the bin thing at the bottom of the reef)
            controller_2.button(3).onTrue(new ParallelCommandGroup(
                    new SwitchCamera(cameraSubsystem, Constants.OperatorConstants.CAM_CORAL),
                    new SequentialCommandGroup(
                        new MoveElevatorToPositionAuto(elevatorSubsystem, Constants.ELE_L1 + (coralTuneValue)),
                        new ParallelCommandGroup(
                            Commands.run(() -> elevatorSubsystem.MoveElevatorToPosition(Constants.ELE_L1 + (coralTuneValue)))
                            // new preScoreAutoCoralIntake(coralSubsystem)
                        )
                    )
                )
            );
            // Level 2 coral (lowest arm on the reef)
            controller_2.button(4).onTrue(new ParallelCommandGroup(
                    new SwitchCamera(cameraSubsystem, Constants.OperatorConstants.CAM_CORAL),
                    new SequentialCommandGroup(
                        new MoveElevatorToPositionAuto(elevatorSubsystem, Constants.ELE_L2 + (coralTuneValue)),
                        new ParallelCommandGroup(
                            Commands.run(() -> elevatorSubsystem.MoveElevatorToPosition(Constants.ELE_L2 + (coralTuneValue)))
                            // new preScoreAutoCoralIntake(coralSubsystem)
                        )
                    )
                )
            );

            // Level 3 coral (middle arm on the reef)
            controller_2.button(5).onTrue(new ParallelCommandGroup(
                    new SwitchCamera(cameraSubsystem, Constants.OperatorConstants.CAM_CORAL),
                    new SequentialCommandGroup(
                        new MoveElevatorToPositionAuto(elevatorSubsystem, Constants.ELE_L3 + (coralTuneValue)),
                        new ParallelCommandGroup(
                            Commands.run(() -> elevatorSubsystem.MoveElevatorToPosition(Constants.ELE_L3 + (coralTuneValue)))
                            // new preScoreAutoCoralIntake(coralSubsystem)
                        )
                    )
                )
            );

            // Level 4 coral (top arm on the reef)
            controller_2.button(6).onTrue(new ParallelCommandGroup(
                    new SwitchCamera(cameraSubsystem, Constants.OperatorConstants.CAM_CORAL),
                    new SequentialCommandGroup(
                        activateTwinkle,
                        new MoveElevatorToPositionAuto(elevatorSubsystem, Constants.ELE_L4 + (coralTuneValue)),
                        new ParallelCommandGroup(
                            Commands.run(() -> elevatorSubsystem.MoveElevatorToPosition(Constants.ELE_L4 + (coralTuneValue)))
                            // new preScoreAutoCoralIntake(coralSubsystem)
                        )
                    )
                )
            );

            // low algae
            driverXbox.leftTrigger(0.5).onTrue(new ParallelCommandGroup(
                    new SequentialCommandGroup(
                        new MoveElevatorToPositionAuto(elevatorSubsystem, Constants.ELE_ALGLO + (algaeTuneValue)),
                        new ParallelCommandGroup(
                            Commands.run(() -> elevatorSubsystem.MoveElevatorToPosition(Constants.ELE_ALGLO + (algaeTuneValue)))
                        )
                    )
                )
            );

            // high algae
            driverXbox.rightTrigger(0.5).onTrue(new ParallelCommandGroup(
                new SequentialCommandGroup(
                    new MoveElevatorToPositionAuto(elevatorSubsystem, Constants.ELE_ALGHI + (algaeTuneValue)),
                    new ParallelCommandGroup(
                        Commands.run(() -> elevatorSubsystem.MoveElevatorToPosition(Constants.ELE_ALGHI + (algaeTuneValue)))
                    )
                )
            )
            );

            // move coral intake. left bumper is intake, right bumper is outtake
            driverXbox.leftBumper().onTrue(new ParallelCommandGroup(
                    new MoveElevatorToPositionAuto(elevatorSubsystem, Constants.ELE_COR_IN),
                    new SequentialCommandGroup(
                        new AutoCoralIntake(coralSubsystem),
                        new RumbleCommand(driverXbox, 1.0, 1.0)
                    )
                )
            );

            /*
             * Algae Intake Out
             */
            driverXbox.rightBumper().onTrue(
                // new algaeIntake(algaeSubsystem, Constants.ALG_M_SPEED)
                new AutoScoreAlgae(algaeSubsystem)
            );
            // move algae intake
            driverXbox.a().onTrue(
                new algaeSmartIntake(algaeSubsystem)
            );

            /* Climber Up */
            // driverXbox.x().onTrue(new MoveClimberToPosition(climberSubsystem, 0.8, 0.1));

            /* Climber Down */
            driverXbox.b().whileTrue( new ParallelCommandGroup(
                    new MoveClimberToPosition(climberSubsystem, 0.39-0.166667, 0.4),
                    new SwitchCamera(cameraSubsystem, Constants.OperatorConstants.CAM_CLIMB)
                )
            );
            driverXbox.b().onFalse(
                new MoveClimber(climberSubsystem, 0.0)
            );

            // auto-score Algae (blue)
            driverXbox.povDown().onTrue(new SequentialCommandGroup(
                    new SetLEDStateCommand(LEDSubsystem.Mode.BLINK_GREEN, ledSubsystem),
                    // new MoveElevatorToPositionAuto(elevatorSubsystem, Constants.ELE_ALGHI),
                    new algaeSmartIntake(algaeSubsystem),
                    new MoveElevatorToPositionAuto(elevatorSubsystem, Constants.ELE_L1),
                    new SetLEDStateCommand(LEDSubsystem.Mode.SOLID_GREEN, ledSubsystem),
                    new RumbleCommand(driverXbox, 1.0, 1.0),
                    new DriveToAprilTagFieldPose(drivebase, AprilTagFieldLayout.loadField(AprilTagFields.kDefaultField), 3, 0, -1, 90),
                    new DriveToAprilTagFieldPose(drivebase, AprilTagFieldLayout.loadField(AprilTagFields.kDefaultField), 3, 0, -.7, 90),
                    new AutoScoreAlgae(algaeSubsystem),
                    new SetLEDStateCommand(LEDSubsystem.Mode.BLINK_ALLIANCE, ledSubsystem)
                )
            );

            driverXbox.povLeft().onTrue(new SequentialCommandGroup(
                    new SetLEDStateCommand(LEDSubsystem.Mode.BLINK_GREEN, ledSubsystem),
                    new MoveElevatorToPositionAuto(elevatorSubsystem, 0),
                    new AutoCoralIntake(coralSubsystem),
                    new SetLEDStateCommand(LEDSubsystem.Mode.SOLID_GREEN, ledSubsystem),
                    new RumbleCommand(driverXbox, 1.0, 1.0),
                    // Red
                    // new DriveToAprilTagFieldPose(drivebase, AprilTagFieldLayout.loadField(AprilTagFields.kDefaultField), 7, 2, 0.5, -90),
                    // new DriveToAprilTagFieldPose(drivebase, AprilTagFieldLayout.loadField(AprilTagFields.kDefaultField), 7, 0.5, 0.5, -90),
                    // Blue
                    new DriveToAprilTagFieldPose(drivebase, AprilTagFieldLayout.loadField(AprilTagFields.kDefaultField), 18, -2.523, 0.92, 90),
                    // new DriveToAprilTagFieldPose(drivebase, AprilTagFieldLayout.loadField(AprilTagFields.kDefaultField), 18, -0.523, 0.92, 90),
                    new MoveElevatorToPositionAuto(elevatorSubsystem, Constants.ELE_L4),
                    // new AutoScoreCoral(coralSubsystem),
                    new SetLEDStateCommand(LEDSubsystem.Mode.BLINK_ALLIANCE, ledSubsystem),
                    new MoveElevatorToPositionAuto(elevatorSubsystem, Constants.ELE_COR_IN)
                )
            );

            driverXbox.povRight().onTrue(new SequentialCommandGroup(
                    new SetLEDStateCommand(LEDSubsystem.Mode.BLINK_GREEN, ledSubsystem),
                    new MoveElevatorToPositionAuto(elevatorSubsystem, 0),
                    new AutoCoralIntake(coralSubsystem),
                    new SetLEDStateCommand(LEDSubsystem.Mode.SOLID_GREEN, ledSubsystem),
                    new RumbleCommand(driverXbox, 1.0, 1.0),
                    // Red
                    // new DriveToAprilTagFieldPose(drivebase, AprilTagFieldLayout.loadField(AprilTagFields.kDefaultField), 7, -2, 0.3, -90),
                    // new DriveToAprilTagFieldPose(drivebase, AprilTagFieldLayout.loadField(AprilTagFields.kDefaultField), 7, -0.5, 0.2, -90),
                    // Blue
                    // new DriveToPositionPathPlanner(drivebase, AprilTagFieldLayout.loadField(AprilTagFields.kDefaultField), 18, -2.523, 0.52, 90),
                    autoAlign.driveToPosition(
                        new Pose2d(
                            1.16,
                            4.4,
                            Rotation2d.fromDegrees(90)
                        )
                    ),
                    // new DriveToAprilTagFieldPose(drivebase, AprilTagFieldLayout.loadField(AprilTagFields.kDefaultField), 18, -0.523, 0.22, 90),
                    new MoveElevatorToPositionAuto(elevatorSubsystem, Constants.ELE_COR_IN),
                    // new AutoScoreCoral(coralSubsystem),
                    new SetLEDStateCommand(LEDSubsystem.Mode.BLINK_ALLIANCE, ledSubsystem),
                    new MoveElevatorToPositionAuto(elevatorSubsystem, Constants.ELE_COR_IN)
                )
            );


            // Reset the elevator, wrist and gyro
            driverXbox.start().onTrue(new ParallelCommandGroup(
                new MoveElevatorToPosition(elevatorSubsystem, 0),
                new algaeIntake(algaeSubsystem, 0),
                new coralIntake(coralSubsystem, 0),
                new SwitchCamera(cameraSubsystem, Constants.OperatorConstants.CAM_CLIMB)

            ));
            driverXbox.start().onTrue(new SequentialCommandGroup(
                    (Commands.runOnce(drivebase::zeroGyroWithAlliance)),
                    Commands.runOnce(()->{
                        Rotation3d rot = drivebase.getSwerveDrive().getGyroRotation3d();
                        drivebase.getSwerveDrive().setGyro(new Rotation3d(rot.getX(), rot.getY(), rot.getZ() + Math.PI));
                    })
                )
            );
            controller_2.button(9).whileTrue(driveLeftCoral);
            controller_2.button(10).whileTrue(driveRightCoral);
            controller_2.button(1).onTrue(
                new AutoScoreCoral(coralSubsystem)
                // Commands.runOnce(()->{
                //     CoralIntakeSubsystem.coral_intake_instance.coralIntake(Constants.COR_M_SPEED);
                // })
            );
            controller_2.button(1).onFalse(
                Commands.runOnce(()->{
                    CoralIntakeSubsystem.coral_intake_instance.coralIntake(0);
                })
            );
            controller_2.button(2).onTrue(
                Commands.runOnce(()->{
                    CoralIntakeSubsystem.coral_intake_instance.coralIntake(-Constants.COR_M_SPEED);
                })
            );
            controller_2.button(2).onFalse(
                Commands.runOnce(()->{
                    CoralIntakeSubsystem.coral_intake_instance.coralIntake(0);
                })
            );
            controller_2.button(7).onTrue(
                Commands.runOnce(()->{
                    drivebase.getDefaultCommand().cancel();
                    drivebase.removeDefaultCommand();
                    drivebase.setDefaultCommand(drivebase.driveFieldOriented(SwerveInputStream.of(drivebase.getSwerveDrive(),
                    () -> driverXbox.getLeftY(),
                    () -> driverXbox.getLeftX())
                    .withControllerRotationAxis(() -> Math.abs(driverXbox.getRightX()) > 0.1 ? driverXbox.getRightX() * -0.85 : 0.0)
                    .scaleTranslation(0.2)
                    .robotRelative(true)));
                    cameraSubsystem.SwitchCamera(Constants.OperatorConstants.CAM_CLIMB);
                })
            );
            controller_2.button(7).onFalse(
                Commands.runOnce(()->{
                    drivebase.getDefaultCommand().cancel();
                    drivebase.removeDefaultCommand();
                    drivebase.setDefaultCommand(drivebase.driveFieldOriented(normalDrive));
                    cameraSubsystem.SwitchCamera(Constants.OperatorConstants.CAM_CORAL);
                })
            );
        }

    }

    public void periodic(){

        // Get tune values from SmartDashboard
        algaeTuneValue = SmartDashboard.getNumber("Algae Elevator Adjust", 0.0);
        coralTuneValue = SmartDashboard.getNumber("Coral Elevator Adjust", 0.0);
        driveSpeedTuner = SmartDashboard.getNumber("Drive Speed Adjust", 1.0);
        rotateSpeedTuner = SmartDashboard.getNumber("Rotate Speed Adjust", 1.0);
        student_student_driver = SmartDashboard.getBoolean("Student Student Driver", false);

        if(student_student_driver){
            driveSpeedTuner = 0.5 * driveSpeedTuner;
            rotateSpeedTuner = 0.5 * rotateSpeedTuner;
            if(safety_controller.button(2).getAsBoolean()){
                SmartDashboard.putBoolean("Safety Disable", true);
                driveSpeedTuner = 0.0;
                rotateSpeedTuner = 0.0;
            } else {
                SmartDashboard.putBoolean("Safety Disable", false);
            }
        }

        // injects the limelight pose estimation if enabled
        if(SmartDashboard.getBoolean("Limelight Pose Estimation", true)){
            if(DriverStation.getAlliance().get() == DriverStation.Alliance.Blue){
                limelightSubsystem.getBotPoseBlue().ifPresent(
                    pose -> {
                        double ts = limelightSubsystem.getVisionTimestampSeconds();
                        drivebase.addVisionReading(pose, ts);
                    }
                );
            } else {
                limelightSubsystem.getBotPoseRed().ifPresent(
                    pose -> {
                        double ts = limelightSubsystem.getVisionTimestampSeconds();
                        drivebase.addVisionReading(pose, ts);
                    }
                );
            }
        }
        SmartDashboard.putNumber("Position X", drivebase.getPose().getX());
        SmartDashboard.putNumber("Position Y", drivebase.getPose().getY());
        SmartDashboard.putNumber("Rotation", drivebase.getPose().getRotation().getDegrees());
    }

    public void init(){
        ElevatorSubsystem.elevatorinstance.resetEncoder();

        // Initialize the SmartDashboard selector
        SmartDashboard.putBoolean("Limelight Pose Estimation", true);
        SmartDashboard.putNumber("Algae Elevator Adjust", 0.0);
        SmartDashboard.putNumber("Coral Elevator Adjust", 0.0);
        SmartDashboard.putNumber("Drive Speed Adjust", 1.0);
        SmartDashboard.putNumber("Rotate Speed Adjust", 1.0);
        SmartDashboard.putBoolean("Student Student Driver", false);
        SmartDashboard.putBoolean("Safety Disable", false);
    }

    public void setMotorBrake(boolean brake) {
        drivebase.setMotorBrake(brake);
    }
}
