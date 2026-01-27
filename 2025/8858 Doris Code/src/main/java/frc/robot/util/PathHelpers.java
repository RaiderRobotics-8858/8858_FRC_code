package frc.robot.util;
import java.util.List;

import com.pathplanner.lib.path.GoalEndState;
import com.pathplanner.lib.path.PathConstraints;
import com.pathplanner.lib.path.PathPlannerPath;
import com.pathplanner.lib.path.Waypoint;

import edu.wpi.first.math.geometry.Pose2d;
import frc.robot.Constants;

/**
 * Utility class for building paths using PathPlanner.
 */
public class PathHelpers {

    /**
     * Builds a simple PathPlannerPath from the current robot pose to the target pose.
     *
     * Based on example from https://pathplanner.dev/pplib-create-a-path-on-the-fly.html with slight modifications.
     *
     * @param currentPose The current pose of the robot.
     * @param targetPose The target pose to drive to.
     */
    public static PathPlannerPath buildPath(Pose2d currentPose, Pose2d targetPose){

        // Create a list of waypoints from poses. Each pose represents one waypoint.
        List<Waypoint> waypoints = PathPlannerPath.waypointsFromPoses(
            currentPose,
            targetPose
        );

        // Generate a path based on the waypoints generated above
        PathPlannerPath path = new PathPlannerPath(
            waypoints,
            getDefaultConstraints(),
            null,
            new GoalEndState(0, targetPose.getRotation())
        );
        path.preventFlipping = true;

        return path;
    }

    /**
     * Returns the default path constraints for the robot.
     * These constraints define how fast the robot can move and accelerate.
     *
     * @return The default PathConstraints.
     */
    public static PathConstraints getDefaultConstraints() {

        // Set Constraints on how fast the robot can move/accelerate
        PathConstraints constraints = new PathConstraints(
            Constants.MAX_DRIVE_VELO,
            Constants.MAX_DRIVE_ACCEL,
            Constants.MAX_ANGLE_ACCEL,
            Constants.MAX_ANGLE_ACCEL
        );

        return constraints;
    }

}