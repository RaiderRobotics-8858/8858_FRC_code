package frc.robot.subsystems.swervedrive;

import edu.wpi.first.wpilibj2.command.SubsystemBase;

import java.util.Optional;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class LimelightSubsystem extends SubsystemBase {
    private final NetworkTable limelightTable;

    public LimelightSubsystem() {
        limelightTable = NetworkTableInstance.getDefault().getTable("limelight");
    }

    public boolean isConnected() {
        // Latency will be -1 if no connection or not updating
        double latency = limelightTable.getEntry("tl").getDouble(-1);
        return latency >= 0;
    }

    public boolean hasValidTarget() {
        return limelightTable.getEntry("tv").getDouble(0) == 1.0;
    }

    // Horizontal offset
    public double getX() {
        return limelightTable.getEntry("tx").getDouble(0.0);
    }

     // Vertical offset
    public double getY() {
        return limelightTable.getEntry("ty").getDouble(0.0);
    }

    // Target area
    public double getArea() {
        return limelightTable.getEntry("ta").getDouble(0.0);
    }

    public Optional<Pose2d> getBotPoseRed(){
        double[] pose = limelightTable.getEntry("botpose_wpiblue").getDoubleArray(new double[6]);

        if(pose.length < 6 || ((pose[0] == 0.0) && (pose[1] == 0.0) && (pose[5] == 0.0))){
            return Optional.empty();
        }

        return Optional.of(new Pose2d(
            pose[0],
            pose[1],
            Rotation2d.fromDegrees(pose[5])
        ));
    }

    public Optional<Pose2d> getBotPoseBlue(){
        double[] pose = limelightTable.getEntry("botpose_wpiblue").getDoubleArray(new double[6]);

        if(pose.length < 6 || ((pose[0] == 0.0) && (pose[1] == 0.0) && (pose[5] == 0.0))){
            return Optional.empty();
        }

        return Optional.of(new Pose2d(
            pose[0],
            pose[1],
            Rotation2d.fromDegrees(pose[5])
        ));
    }

    public double getVisionTimestampSeconds(){
        double c1 = limelightTable.getEntry("c1").getDouble(0.0);
        double t1 = limelightTable.getEntry("t1").getDouble(0.0);
        double totalLatencyMs = c1 + t1;
        return Timer.getFPGATimestamp() - (totalLatencyMs / 1000.0);
    }

    public Pose2d getTagRelativePose(){
        double[] poseArray = limelightTable.getEntry("botpose").getDoubleArray(new double[6]);

        // if (poseArray.length < 6);

        Pose2d camFramePose = new Pose2d(
            poseArray[0],
            poseArray[1],
            Rotation2d.fromDegrees(poseArray[5])
        );

        Transform2d cameraToRobot = new Transform2d(
            new Translation2d(0.236,0.32344),
            Rotation2d.fromDegrees(-90)
        );

        Pose2d correctedPose = camFramePose.transformBy(cameraToRobot);

        return correctedPose;
    }

    @Override
    public void periodic() {
        // Useful debugging on dashboard
        SmartDashboard.putBoolean("Limelight Connected", isConnected());
        SmartDashboard.putBoolean("Limelight Has Target", hasValidTarget());
        SmartDashboard.putNumber("Limelight tx", getX());
        SmartDashboard.putNumber("Limelight ty", getY());
        SmartDashboard.putNumber("Limelight ta", getArea());
    }
}
