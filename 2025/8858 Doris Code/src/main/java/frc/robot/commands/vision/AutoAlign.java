package frc.robot.commands.vision;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.path.PathPlannerPath;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.subsystems.swervedrive.SwerveSubsystem;
import frc.robot.util.PathHelpers;

public class AutoAlign {

    private final SwerveSubsystem drivetrain;
    public static Pose2d targetPose;

    public AutoAlign(SwerveSubsystem drivetrain){
        this.drivetrain = drivetrain;
    }

    public Command driveToPosition(Pose2d targetPose){

        Pose2d currentPose = drivetrain.getPose();

        PathPlannerPath path = PathHelpers.buildPath(currentPose, targetPose);

        // Telemtry for debugging
        SmartDashboard.putNumber("Target X", targetPose.getX());
        SmartDashboard.putNumber("Target Y", targetPose.getY());
        SmartDashboard.putNumber("Target Degrees", targetPose.getRotation().getDegrees());

        double dx = targetPose.getX() - currentPose.getX();
        double dy = targetPose.getY() - currentPose.getY();
        double dTheta = targetPose.getRotation().getDegrees() - currentPose.getRotation().getDegrees();

        double pos_error = Math.hypot(dx, dy);

        SmartDashboard.putNumber("Positional Error", pos_error);
        SmartDashboard.putNumber("Angle Error", dTheta);

        return AutoBuilder.pathfindThenFollowPath(
            path,
            PathHelpers.getDefaultConstraints()
        );
    }

    public Trigger createCheckpointTrigger (Double checkpointDistance) {
        return new Trigger(()->
            targetPose != null &&
            drivetrain.getPose().getTranslation().getDistance(targetPose.getTranslation()) <= checkpointDistance
        );
    }
}
