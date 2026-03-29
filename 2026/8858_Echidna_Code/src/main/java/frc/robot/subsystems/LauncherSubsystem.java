package frc.robot.subsystems;

import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.SparkLowLevel.MotorType;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.Constants.AimPoints;

import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * Constructor for the LauncherSubsystem. Primary function is to control the flywheel launch motor,
 * the turret rotation motor, and the kicker motor that feeds game pieces into the launcher.
 * There's cake at the end of this file
 *
 * @param robotPoseSupplier A Supplier that provides the current Pose2d of the robot when called.
 * This is used to calculate the turret angle and launcher velocity needed to aim at targets on the field.
 */
public class LauncherSubsystem extends SubsystemBase {
    private final SparkMax launchMotor; // Motor controller for the flywheel that launches the game pieces
    private final SparkMax kickerMotor; // Motor controller for the mechanism that feeds game pieces into the launcher
    private final SparkMax turretAngle; // Motor controller for the turret's rotation
    private final DigitalInput turretAngleZero, launchOutput;

    /**
     * Need to know robot pose to calculate turret angle to target,
     * so we take a supplier of the robot pose as a dependency
     */
    private final Supplier<Pose2d> robotPoseSupplier;

    /** PID controller for maintaining launch speed */
    private final PIDController LaunchPIDController;
    private final double launch_kP = Constants.LAUNCH_KP;
    private final double launch_kI = Constants.LAUNCH_KI;
    private final double launch_kD = Constants.LAUNCH_KD;

    /** PID controller for maintaining turret angle */
    private final PIDController anglePIDController;
    private final double angle_kP = Constants.TURRET_ANGLE_KP;
    private final double angle_kI = Constants.TURRET_ANGLE_KI;
    private final double angle_kD = Constants.TURRET_ANGLE_KD;

    private final TreeMap<Double, Double> distanceToSpeed = new TreeMap<>();

    /**
     * {@link LauncherSubsystem} constructor.
     */
    public LauncherSubsystem(Supplier<Pose2d> robotPoseSupplier) {
        this.robotPoseSupplier = robotPoseSupplier;
        // CAN_LAUNCH_LEFT is the primary launch motor, CAN_LAUNCH_RIGHT is set to follow but inverted
        launchMotor = new SparkMax(Constants.CAN_LAUNCH_LEFT, MotorType.kBrushless);
        kickerMotor = new SparkMax(Constants.CAN_KICKER_MOTOR, MotorType.kBrushless);
        turretAngle = new SparkMax(Constants.CAN_TURRET_ANGLE, MotorType.kBrushless);
        turretAngleZero = new DigitalInput(Constants.DIO_TURRET_RING);
        launchOutput = new DigitalInput(Constants.DIO_TURRET_OUTPUT);
        LaunchPIDController = new PIDController(launch_kP, launch_kI, launch_kD);
        anglePIDController = new PIDController(angle_kP, angle_kI, angle_kD);
        initMap();
    }

    /**
     * Maps various distances to the corresponding speeds
     */
    private void initMap() {
        distanceToSpeed.put(1.75, 2.06);
        distanceToSpeed.put(2.0, 2.16);
        distanceToSpeed.put(2.5, 2.24);
        distanceToSpeed.put(3.0, 2.31);
        distanceToSpeed.put(3.5, 2.6);
        distanceToSpeed.put(4.0, 2.91);
        distanceToSpeed.put(4.9, 3.2);
        distanceToSpeed.put(5.2, 3.5);
    }

    public Pose2d getRobotPose() {
        return robotPoseSupplier.get();
    }

    /**
     * Computes the turret rotation needed (relative to the robot) to aim at a field target.
     *
     * @param targetPose Field-relative target pose.
     * @return Desired turret rotation relative to the robot's heading.
     */
    public Rotation2d getTurretTargetRotation(Pose2d targetPose) {
        Pose2d robotPose = getRobotPose();
        Translation2d toTarget = targetPose.getTranslation().minus(robotPose.getTranslation());
        double fieldAngleRadians = Math.atan2(toTarget.getY(), toTarget.getX());
        double relativeRadians = MathUtil.angleModulus(fieldAngleRadians - robotPose.getRotation().getRadians());
        return new Rotation2d(relativeRadians);
    }

    /**
     * Convenience helper returning the target turret angle in degrees.
     *
     * @param targetPose Field-relative target pose.
     * @return Desired turret angle in degrees (relative to robot heading).
     */
    public double getTurretTargetAngleDegrees(Pose2d targetPose) {
        return getTurretTargetRotation(targetPose).getDegrees();
    }

    /**
     * Resets the PID controllers for both launch speed and turret angle.
     * Should be called when starting a new aiming/launching action to prevent
     * integral windup and ensure accurate control from the start.
     */
    public void resetPID() {

        LaunchPIDController.setPID(
            SmartDashboard.getNumber("Launcher/launch_kP", launch_kP),
            SmartDashboard.getNumber("Launcher/launch_kI", launch_kI),
            SmartDashboard.getNumber("Launcher/launch_kD", launch_kD)
        );

        anglePIDController.setPID(
            SmartDashboard.getNumber("Launcher/angle_kP", angle_kP),
            SmartDashboard.getNumber("Launcher/angle_kI", angle_kI),
            SmartDashboard.getNumber("Launcher/angle_kD", angle_kD)
        );
        LaunchPIDController.reset();
        anglePIDController.reset();
    }

    /**
     * Returns the state of the launch output sensor.
     * @return true if the launch output sensor is triggered, false otherwise.
     */
    public boolean getLaunchOutput() {
        return launchOutput.get();
    }

    /**
     * Returns the state of the turret zero sensor.
     * @return true if the turret zero sensor is triggered, false otherwise.
     */
    public boolean getTurretZeroOutput() {
        if(turretAngleZero.get()){
            zeroTurretAngle();
            return true;
        }
        return false;
    }

    /**
     * Zeros the turret angle encoder.
     * Should be called when the turret is at its zero position.
     */
    public void zeroTurretAngle() {
        SmartDashboard.putBoolean("Launcher/TurretZeroedFlag", true);
        turretAngle.getEncoder().setPosition(0.0);
    }

    /**
     * Returns the Current of the launcher motor in Amps
     */
    public double getLauncherCurrent() {
        return launchMotor.getOutputCurrent();
    }

    /**
     * Returns the Current of the kicker motor in Amps
     */
    public double getKickerCurrent() {
        return kickerMotor.getOutputCurrent();
    }

    /**
     * Readback of the current launch speed in encoder velocity units
     * @return Current launch speed as measured by the encoder velocity of the launch motor
     */
    public double getLaunchSpeed() {
        return launchMotor.getEncoder().getVelocity();
    }

    /**
     * Checks if the launch motors are at the target speed within a certain threshold
     * @param targetSpeed The desired launch speed to check against
     * @return true if the current launch speed is within the threshold of the target speed, false otherwise
     */
    public boolean isAtTargetSpeed(double targetSpeed) {
        boolean atSpeed = Math.abs(getLaunchSpeed() - targetSpeed) < Constants.LAUNCH_THRESHOLD;
        return atSpeed;
    }

    public double getLaunchSpeed(AimPoints target) {
          Transform2d difference = new Pose2d (
            target.value.getX(),
            target.value.getY(),
            new Rotation2d()
        ).minus(getRobotPose());

        double distance = Math.sqrt(Math.pow(difference.getX(),2)+Math.pow(difference.getY(),2));
        return getSpeedForDistance(distance);
    }

    /**
     * Sets the launch motors to maintain the target speed using PID control
     * @param targetSpeed The desired launch speed to maintain
     */
    public void setLaunchSpeed(double targetSpeed) {
        SmartDashboard.putNumber("Launcher/Target_Speed", targetSpeed);
        double pidOutput = LaunchPIDController.calculate(getLaunchSpeed(), targetSpeed) + SmartDashboard.getNumber("Launcher/launch_ff", Constants.LAUNCH_FF);
        double motorCommand = MathUtil.clamp(pidOutput, 0.0, 1.0);
        SmartDashboard.putNumber("Launcher/Launch_PID_result", motorCommand);
        SmartDashboard.putBoolean("Launcher/At_Target_Speed", isAtTargetSpeed(targetSpeed));
        if(SmartDashboard.getBoolean("TESTMODE", false)){
            motorCommand = SmartDashboard.getNumber("Launcher/ManualMotorSpeed", 0);
        }
        launchMotor.set(motorCommand);
    }


    /**
     * Checks if the turret is at the target angle within a certain threshold
     * @param targetAngle The desired turret angle to check against
     * @return true if the current turret angle is within the threshold of the target angle, false otherwise
     */
    public boolean isAtTargetAngle(double targetAngle) {
        return Math.abs(getTurretAngleDegrees() - targetAngle) < Constants.TURRET_ANGLE_THRESHOLD;
    }

    /**
     * Helper method to convert encoder units of the turret angle motor to degrees for easier understanding and control
     * @param units The raw encoder units from the turret angle motor
     * @return The equivalent angle in degrees based on the defined conversion factor
     */
    private double turretUnitsToDegrees(double units) {
        return units * Constants.TURRET_DEGREES_PER_UNIT;
    }

    /**
     * Readback of the current turret angle in degrees
     * @return The current turret angle in degrees
     */
    public double getTurretAngleDegrees() {
        return turretUnitsToDegrees(turretAngle.getEncoder().getPosition());
    }

    /**
     * Readback of the current turret angle in encoder units
     * @return The current turret angle in encoder units
     */
    public double getTurretAngle() {
        return turretAngle.getEncoder().getPosition();
    }

    /**
     * Uses PID control to move turret to desired angle
     * @param targetAngle The desired turret angle to move to
     */
    public void setTurretAngle(double targetAngle) {
        targetAngle = targetAngle + SmartDashboard.getNumber("Launcher/AngleAdjust", 0);
        // Clamp target angle (degrees) to within turret limits to prevent damage to turret
        SmartDashboard.putNumber("Launcher/TurretTarget", targetAngle);
        if(targetAngle > Constants.TURRET_LEFT_LIMIT_DEG || targetAngle < Constants.TURRET_RIGHT_LIMIT_DEG){
            targetAngle = 0.0;
        }
        double angleSpeed = anglePIDController.calculate(getTurretAngleDegrees(), targetAngle);
        angleSpeed = MathUtil.clamp(angleSpeed, -Constants.TURRET_MAX_SPEED, Constants.TURRET_MAX_SPEED);
        SmartDashboard.putBoolean("Launcher/Turret_At_Position", isAtTargetAngle(targetAngle));
        SmartDashboard.putNumber("Launcher/TurretPosition", getTurretAngleDegrees());
        turretAngle.set(angleSpeed);
    }

    /**
     * Manually moves the turret without PID control
     * @param speed The speed at which to move the turret
     */
    public void setTurretSpeed(double speed) {
        turretAngle.set(speed);
    }

    /**
     * This equation was found through testing from different positions from the bot
     * @param disinfeet The distance in feet
     * @return The calculated velocity based on the distance
     */
    public double getvelocityfromdistance(double disinfeet){
       return(20*disinfeet+390);
    }
    /**
     * Converts the distance from meters to feet and calculates the velocity
     * @param disinmeters The distance in meters
     * @return The calculated velocity based on the distance in meters
     */
    public double getvelocityfrommeters(double disinmeters){
        return(getvelocityfromdistance(disinmeters*3.2808));
    }

    /**
     * Finds the target based on the robot's current position and alliance,
     * then aims the turret at that target. If no valid target is found, it outputs
     * "notarget" to the SmartDashboard.
     */
    public AimPoints findTarget() {
        Pose2d robotpositionPose2d = getRobotPose();
        Alliance alliance = DriverStation.getAlliance().orElse(Alliance.Blue);
        if (alliance == Alliance.Blue){
            if(robotpositionPose2d.getX() < 4.5){
                // If in Blue scoring zone, aim at hub for scoring.
                aimatTarget(Constants.AimPoints.BLUE_HUB);
                SmartDashboard.putString("aimtarget", "BLUE_HUB");
                return Constants.AimPoints.BLUE_HUB;
            } else if (robotpositionPose2d.getX() > 4.5){
                setTurretAngle(0);
                if (robotpositionPose2d.getY() > 4.03){
                    // If in right half of midfield, aim at outpost for passing back
                    // aimatTarget(Constants.AimPoints.BLUE_OUTPOST); // TODO undo once midfield targetting is fixed
                    SmartDashboard.putString("aimtarget", "BLUE_OUTPOST");
                    return Constants.AimPoints.BLUE_OUTPOST;
                } else if (robotpositionPose2d.getY() < 4.03){
                    // If in left half of midfield, aim at far side for passing back
                    // aimatTarget(Constants.AimPoints.BLUE_FAR_SIDE); // TODO undo once midfield targetting is fixed
                    SmartDashboard.putString("aimtarget", "BLUE_FAR_SIDE");
                    return Constants.AimPoints.BLUE_FAR_SIDE;
                }
            }
        }
        if (alliance == Alliance.Red){
            if(robotpositionPose2d.getX() > 11.9){
                // If in Red scoring zone, aim at hub for scoring.
                aimatTarget(Constants.AimPoints.RED_HUB);
                SmartDashboard.putString("aimtarget", "RED_HUB");
                return Constants.AimPoints.RED_HUB;
            } else if (robotpositionPose2d.getX() < 11.9){
                setTurretAngle(0);
                if (robotpositionPose2d.getY() < 4.03){
                    // If in left half of midfield, aim at outpost for passing back
                    // aimatTarget(Constants.AimPoints.RED_OUTPOST); // TODO undo once midfield targetting is fixed
                    SmartDashboard.putString("aimtarget", "RED_OUTPOST");
                    return Constants.AimPoints.RED_OUTPOST;
                } else if (robotpositionPose2d.getY() > 4.03){
                    // If in right half of midfield, aim at far side for passing back
                    // aimatTarget(Constants.AimPoints.RED_FAR_SIDE); // TODO undo once midfield targetting is fixed
                    SmartDashboard.putString("aimtarget", "RED_FAR_SIDE");
                    return Constants.AimPoints.RED_FAR_SIDE;
                }
            }
        }
        SmartDashboard.putString("aimtarget", "notarget");
        return null;
    }

    /**
     * Aims the turret at a specified target.
     * @param target The target to aim at
     */
    public void aimatTarget (AimPoints target){
        // Aim the turret at the given field target.
        // The AimPoints enum already contains alliance-specific coordinates, so we
        // compute the angle-to-target in field coordinates, subtract the robot
        // heading, normalize to [-180,180], and send that as the turret setpoint
        // (in degrees, relative to the robot forward direction).

        SmartDashboard.putString("aimtarget", target.name());

        // vector from robot to target (field coordinates)
        Translation2d difference = target.value.toTranslation2d()
        .minus(getRobotPose().getTranslation());

        double dx = difference.getX();
        double dy = difference.getY();
        double distance = Math.hypot(dx, dy);
        SmartDashboard.putNumber("Distance_to_Target", distance);
        SmartDashboard.putNumber("Launcher/DiffY", dy);
        SmartDashboard.putNumber("Launcher/DiffX", dx);

        double fieldAngleRad = Math.atan2(dy, dx);
        double robotHeadingRad = getRobotPose().getRotation().getRadians();
        double relativeRad = MathUtil.angleModulus(fieldAngleRad - robotHeadingRad);
        double turretAngleDeg = Math.toDegrees(relativeRad);

        // if (alliance == DriverStation.)
        turretAngleDeg = MathUtil.inputModulus(turretAngleDeg + 180, -180, 180);

        SmartDashboard.putNumber("Launcher/ComputedTurretAngle", turretAngleDeg);

        // TODO delete this AI generated comment
        // send to turret controller
        setTurretAngle(turretAngleDeg * 1.0);

    }

    /**
     * Activates the Kicker
     */
    public void activateKicker() {
        kickerMotor.set(Constants.KICKER_SPEED); // Run kicker at predefined speed if at target launch speed and angle
    }

    /**
     * Gets the Kicker's Velocity in mystery units
     * @return idk
     */
    public double getKickerVelocity() {
        return kickerMotor.getEncoder().getVelocity();
    }

    /**
     * Manually moves the kicker without PID control
     * @param speed The speed to set the kicker motor
     */
    public void setKickerSpeed(double speed) {
        double driveSpeed = speed;
        if(SmartDashboard.getBoolean("TESTMODE", false)){
            driveSpeed = SmartDashboard.getNumber("Launcher/KickerMotorSpeed", 0);
        }
        kickerMotor.set(driveSpeed);
    }

    /**
     * Gets the appropriate launch speed for a given distance to the target using linear interpolation
     * between the points defined in the distanceToSpeed map. If the distance is outside the range of
     * the map, it will return the speed of the nearest endpoint.
     *
     * @param distance The distance to the target for which to get the launch speed
     * @return The calculated launch speed based on the distance
     */
    public double getSpeedForDistance(double distance) {
        if (distanceToSpeed.isEmpty()) {
            throw new IllegalStateException("distance->speed map is empty");
        }

        // exact match
        Double exact = distanceToSpeed.get(distance);
        if (exact != null) return exact;

        Double lowKey = distanceToSpeed.floorKey(distance);
        Double highKey = distanceToSpeed.ceilingKey(distance);

        // out of range: use nearest endpoint
        if (lowKey == null) return distanceToSpeed.get(highKey);
        if (highKey == null) return distanceToSpeed.get(lowKey);

        double lowDist = lowKey;
        double highDist = highKey;
        double lowSpeed = distanceToSpeed.get(lowKey);
        double highSpeed = distanceToSpeed.get(highKey);

        if (highDist == lowDist) return lowSpeed; // guard division by zero

        double t = (distance - lowDist) / (highDist - lowDist);
        double offset_adjust = SmartDashboard.getNumber("configlaunch", 0);
        return lowSpeed + t * (highSpeed - lowSpeed) + offset_adjust;
    }
    // Checks Temp of the Launcher
    public double getLauncherTemp() {
        return launchMotor.getMotorTemperature();
    }
    // Checks the Temp of the Kicker
     public double getKickerTemp() {
        return kickerMotor.getMotorTemperature();
    }
    // Checks the Temp of the Angle Motor
     public double getTurretAngleTemp() {
        return turretAngle.getMotorTemperature();
    }
}
// the cake was a lie