package frc.robot.commands.vision;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Constants;
import frc.robot.subsystems.swervedrive.SwerveSubsystem;
import swervelib.SwerveInputStream;

public class DriveToAprilTagFieldPose extends Command {
    private final SwerveSubsystem swerve;
    private final AprilTagFieldLayout layout;
    private final int TargetTagID;
    private final Pose2d targetPose;

    private final SwerveInputStream stream;

    private final double kPoseTolerance = 0.20;
    private final double kAngleTolerance = 1.0;

    // Constraints
    private final double maxDriveVelo   = 1.5;
    private final double maxDriveAccel  = 1.0;
    private final double maxAngleVelo   = 2.0;
    private final double maxAngleAccel  = 1.0;

    private final ProfiledPIDController drive_pidController;
    private final ProfiledPIDController angle_pidController;

    public DriveToAprilTagFieldPose(SwerveSubsystem swerve, AprilTagFieldLayout layout, int tagID, double xOffset, double yOffset, int rotation_deg){
        this.swerve = swerve;
        this.layout = layout;
        this.TargetTagID = tagID;
        Pose3d pose3d = layout.getTagPose(tagID).orElse(null);
        double x = pose3d.getX();
        double y = pose3d.getY();

        TrapezoidProfile.Constraints drive_constraints = new TrapezoidProfile.Constraints(maxDriveVelo, maxDriveAccel);
        TrapezoidProfile.Constraints angle_constraints = new TrapezoidProfile.Constraints(maxAngleVelo, maxAngleAccel);
        drive_pidController = new ProfiledPIDController(Constants.DRIVE_KP, Constants.DRIVE_KI, Constants.DRIVE_KD, drive_constraints);
        angle_pidController = new ProfiledPIDController(Constants.ANGLE_KP, Constants.ANGLE_KI, Constants.ANGLE_KD, angle_constraints);

        Rotation3d rot3d = pose3d.getRotation();
        Rotation2d rot2d = Rotation2d.fromDegrees(rot3d.getZ());
        Transform2d offset = new Transform2d(
            new Translation2d(xOffset, yOffset),
            Rotation2d.fromDegrees(rotation_deg)
        );
        this.targetPose = new Pose2d(x, y, rot2d).transformBy(offset);

        if(targetPose == null){
            throw new IllegalArgumentException("AprilTag ID " + tagID + " not found in field layout.");
        }

        stream = SwerveInputStream.of(
            swerve.getSwerveDrive(),

            // Placeholders
            ()->0.0,
            ()->0.0
        );

        addRequirements(swerve);
    }

    @Override
    public void initialize(){

        // Reset the PID controllers
        drive_pidController.reset(0);
        angle_pidController.reset(0);

        // Set a target position for the robot
        stream.driveToPose(
            () -> targetPose,
            drive_pidController,
            angle_pidController
        );

        // Enable the stream to drive to the pose
        stream.driveToPoseEnabled(true);
    }

    @Override
    public void execute() {

        // Pass the stream into the Swerve Drive itself
        swerve.drive(stream.get());

        // Telemtry for debugging
        SmartDashboard.putNumber("Target X", targetPose.getX());
        SmartDashboard.putNumber("Target Y", targetPose.getY());
        SmartDashboard.putNumber("Target Degrees", targetPose.getRotation().getDegrees());
    }

    @Override
    public boolean isFinished() {
        Pose2d currentPose = swerve.getPose();

        double dx = targetPose.getX() - currentPose.getX();
        double dy = targetPose.getY() - currentPose.getY();
        double dTheta = targetPose.getRotation().getDegrees() - currentPose.getRotation().getDegrees();

        double pos_error = Math.hypot(dx, dy);

        SmartDashboard.putNumber("Positional Error", pos_error);
        SmartDashboard.putNumber("Angle Error", dTheta);

        return (pos_error < kPoseTolerance) && (Math.abs(dTheta) < kAngleTolerance);
    }

    @Override
    public void end(boolean interrupted){
        swerve.drive(new Translation2d(), 0, true);
        stream.driveToPoseEnabled(false);
    }
}
