package frc.robot.commands.vision;

import frc.robot.subsystems.swervedrive.LimelightSubsystem;
import frc.robot.subsystems.swervedrive.SwerveSubsystem;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.wpilibj2.command.Command;

public class DriveToTagCommand extends Command {
    private final LimelightSubsystem limelight;
    private final SwerveSubsystem swerve;

    // Tuning Constants
    private final double kForwardGain = 1.0;
    private final double kStrafeGain = 1.0;
    private final double kTurnGain = 0.1;
    private final double kTargetArea = 30;

    public DriveToTagCommand(LimelightSubsystem limelight, SwerveSubsystem swerve){
        this.limelight = limelight;
        this.swerve = swerve;
        addRequirements(swerve);
    }

    @Override
    public void execute(){

        if(!limelight.hasValidTarget()){
            swerve.drive(new Translation2d(0, 0), 0, true);
            return;
        }

        Pose2d tagPoseCameraFrame = limelight.getTagRelativePose();

        double cameraX = tagPoseCameraFrame.getX();
        double cameraY = tagPoseCameraFrame.getY();

        double robotForward = -cameraY;
        double robotStrafe = cameraX;

        double rawYaw = tagPoseCameraFrame.getRotation().getDegrees();
        double robotYaw = rawYaw - 90;
        SmartDashboard.putNumber("LL Yaw", rawYaw);

        double forwardSpeed = MathUtil.clamp(robotForward * kForwardGain, -0.5, 0.5);
        double strafeSpeed = MathUtil.clamp(robotStrafe * kStrafeGain, -0.5, 0.5);

        double turnSpeed = MathUtil.clamp(robotYaw * kTurnGain, -0.5, 0.5);

        Translation2d translation = new Translation2d(forwardSpeed, strafeSpeed);

        swerve.drive(translation, turnSpeed, true);
    }

    @Override
    public void end(boolean interrupted){
        swerve.drive(new Translation2d(), 0, true);
    }

    @Override
    public boolean isFinished() {
        return limelight.getArea() >= kTargetArea;
    }
}
