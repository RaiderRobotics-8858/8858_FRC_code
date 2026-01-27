
package frc.robot.commands.vision;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.path.PathPlannerPath;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.WaitUntilCommand;
import frc.robot.subsystems.swervedrive.SwerveSubsystem;
import frc.robot.util.PathHelpers;

public class DriveToPositionPathPlanner extends Command {
    private final SwerveSubsystem swerve;
    private final AprilTagFieldLayout layout;
    private final int TargetTagID;
    private final Pose2d targetPose;

    private final double kPoseTolerance = 0.20;
    private final double kAngleTolerance = 1.0;

    private final PathPlannerPath path;

    /**
     * Drives the robot to a position relative to an AprilTag using PathPlanner.
     * @param swerve Swerve subsystem to control the robot's movement.
     * @param layout AprilTag field layout containing the tag poses.
     * @param tagID ID of the AprilTag to drive towards.
     * @param xOffset X offset from the tag's position in meters.
     * @param yOffset Y offset from the tag's position in meters.
     * @param rotation_deg Rotation offset in degrees from the tag's orientation.
     */
    public DriveToPositionPathPlanner(SwerveSubsystem swerve, AprilTagFieldLayout layout, int tagID, double xOffset, double yOffset, int rotation_deg){
        this.swerve = swerve;
        this.layout = layout;
        this.TargetTagID = tagID;
        addRequirements(swerve);

        // Calculate the Target Pose
        Pose3d pose3d = layout.getTagPose(tagID).orElse(null);
        Rotation3d rot3d = pose3d.getRotation();
        Rotation2d rot2d = Rotation2d.fromDegrees(rot3d.getZ());
        Transform2d offset = new Transform2d(
            new Translation2d(xOffset, yOffset),
            Rotation2d.fromDegrees(rotation_deg)
        );
        this.targetPose = new Pose2d(
            pose3d.getX(),
            pose3d.getY(),
            rot2d
        ).transformBy(offset);

        // Telemtry for debugging
        SmartDashboard.putNumber("Target X", targetPose.getX());
        SmartDashboard.putNumber("Target Y", targetPose.getY());
        SmartDashboard.putNumber("Target Degrees", targetPose.getRotation().getDegrees());

        if(targetPose == null){
            throw new IllegalArgumentException("AprilTag ID " + tagID + " not found in field layout.");
        }

        Pose2d currentPose = swerve.getPose();

        path = PathHelpers.buildPath(currentPose, targetPose);
    }

    @Override
    public void initialize(){
        SmartDashboard.putString("PathPlanner Path", "Moving to Target Pose: " + targetPose.toString());
    }

    @Override
    public void execute() {
        AutoBuilder.pathfindThenFollowPath(
            path,
            PathHelpers.getDefaultConstraints()

        ).andThen(new WaitUntilCommand(() -> false));
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
        SmartDashboard.putString("PathPlanner Path", "Arrived at Target Pose: " + targetPose.toString());
    }
}
