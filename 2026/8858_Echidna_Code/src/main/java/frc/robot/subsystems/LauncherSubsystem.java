package frc.robot.subsystems;

import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.SparkLowLevel.MotorType;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.Constants.AimPoints;
import frc.robot.util.HubTimer;

import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * Constructor for the LauncherSubsystem. Primary function is to control the flywheel launch motor,
 * the turret rotation motor, and the kicker motor that feeds game pieces into the launcher.
 * There's cake at the end of this file
 *
 * @param robotPoseSupplier A Supplier that provides the current Pose2d of the robot when called.
 * This is used to calculate the turret angle and launcher velocity needed to aim at targets on the field.
 * @param robotVelocitySupplier A Supplier that provides the current robot-relative velocity when called.
 */
public class LauncherSubsystem extends SubsystemBase {
    private final SparkMax launchMotor; // Motor controller for the flywheel that launches the game pieces
    private final SparkMax kickerMotor; // Motor controller for the mechanism that feeds game pieces into the launcher
    private final SparkMax turretAngle; // Motor controller for the turret's rotation
    private final SparkMax hoodMotor; // Motor controller for the hood shooting straight(if applicable)
    private final DigitalInput turretAngleZero, launchOutput;

    /**
     * Indicates when the Turret is at the target angle within a threshold.
     */
    private boolean turretAtTarget = false;

    /**
     * Need to know robot pose to calculate turret angle to target,
     * so we take a supplier of the robot pose as a dependency
     */
    private final Supplier<Pose2d> robotPoseSupplier;

    /**
     * Need robot velocity for motion-compensated launch calculations
     */
    private final Supplier<ChassisSpeeds> robotVelocitySupplier;

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

    /** PID controller for maintaining hood angle */
    private final PIDController hoodPIDController;
    private final double hood_kP = Constants.HOOD_ANGLE_KP;
    private final double hood_kI = Constants.HOOD_ANGLE_KI;
    private final double hood_kD = Constants.HOOD_ANGLE_KD;

    private final TreeMap<Double, Double> distanceToSpeedScoring = new TreeMap<>();
    private final TreeMap<Double, Double> distanceToSpeedYeeting = new TreeMap<>();

    /**
     * Maps launcher velocity (encoder units) to expected projectile flight time (seconds).
     * Populate with empirically measured values.
     */
    private final TreeMap<Double, Double> velocityToFlightTime = new TreeMap<>();

    /**
     * {@link LauncherSubsystem} constructor.
     */
    public LauncherSubsystem(Supplier<Pose2d> robotPoseSupplier, Supplier<ChassisSpeeds> robotVelocitySupplier) {
        this.robotPoseSupplier = robotPoseSupplier;
        this.robotVelocitySupplier = robotVelocitySupplier;
        // CAN_LAUNCH_LEFT is the primary launch motor, CAN_LAUNCH_RIGHT is set to follow but inverted
        launchMotor = new SparkMax(Constants.CAN_LAUNCH_LEFT, MotorType.kBrushless);
        kickerMotor = new SparkMax(Constants.CAN_KICKER_MOTOR, MotorType.kBrushless);
        hoodMotor = new SparkMax(Constants.CAN_HOOD_MOTOR, MotorType.kBrushless);
        turretAngle = new SparkMax(Constants.CAN_TURRET_ANGLE, MotorType.kBrushless);
        turretAngleZero = new DigitalInput(Constants.DIO_TURRET_RING);
        launchOutput = new DigitalInput(Constants.DIO_TURRET_OUTPUT);
        LaunchPIDController = new PIDController(launch_kP, launch_kI, launch_kD);
        anglePIDController = new PIDController(angle_kP, angle_kI, angle_kD);
        hoodPIDController = new PIDController(hood_kP, hood_kI, hood_kD);
        InitMap();
    }

    /**
     * Maps various distances to the corresponding speeds
     */
    private void InitMap() {
        // While hood is up
        distanceToSpeedScoring.put(1.75, 2.16);
        distanceToSpeedScoring.put(2.0, 2.21);
        distanceToSpeedScoring.put(2.5, 2.34);
        distanceToSpeedScoring.put(3.0, 2.41);
        distanceToSpeedScoring.put(3.5, 2.62);
        distanceToSpeedScoring.put(4.0, 2.83);
        distanceToSpeedScoring.put(4.5, 2.93);
        distanceToSpeedScoring.put(4.9, 3.04);
        distanceToSpeedScoring.put(5.0, 3.05);
        distanceToSpeedScoring.put(5.2, 3.12);
        distanceToSpeedScoring.put(5.5, 3.21);
        distanceToSpeedScoring.put(6.0, 3.4);
        distanceToSpeedScoring.put(8.2, 4.0);

        // While hood is down
        distanceToSpeedYeeting.put(5.0, 2.06);
        distanceToSpeedYeeting.put(5.5, 2.11);
        distanceToSpeedYeeting.put(6.0, 2.24);
        distanceToSpeedYeeting.put(6.5, 2.31);
        distanceToSpeedYeeting.put(7.0, 2.52);
        distanceToSpeedYeeting.put(7.5, 2.68);
        distanceToSpeedYeeting.put(8.0, 3.05);
        distanceToSpeedYeeting.put(8.5, 3.2);
        distanceToSpeedYeeting.put(9.0, 4.0);

        // Launcher velocity (encoder units) -> projectile flight time (seconds)
        // These values should be updated with real measurements from the robot.
        velocityToFlightTime.put(2.31, 1.0);
        velocityToFlightTime.put(2.4, 1.1);
        velocityToFlightTime.put(2.8, 1.3);
        velocityToFlightTime.put(3.5, 1.9);
        velocityToFlightTime.put(4.0, 2.0);
    }

    public Pose2d getRobotPose() {
        return robotPoseSupplier.get();
    }

    public ChassisSpeeds getRobotVelocity() {
        return robotVelocitySupplier.get();
    }

    /**
     * Checks if the robot is moving slower than the configured translational
     * and rotational velocity thresholds.
     *
     * @return true when both linear and angular speeds are below thresholds
     */
    public boolean isRobotVelocityBelowThreshold() {
        ChassisSpeeds speeds = getRobotVelocity();
        double translationalSpeed = Math.hypot(speeds.vxMetersPerSecond, speeds.vyMetersPerSecond);
        return translationalSpeed < Constants.ROBOT_TRANSLATIONAL_SPEED_THRESHOLD
            && Math.abs(speeds.omegaRadiansPerSecond) < Constants.ROBOT_ANGULAR_SPEED_THRESHOLD;
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

        hoodPIDController.setPID(
            SmartDashboard.getNumber("Launcher/hood_kP", hood_kP),
            SmartDashboard.getNumber("Launcher/hood_kI", hood_kI),
            SmartDashboard.getNumber("Launcher/hood_kD", hood_kD)
        );
        LaunchPIDController.reset();
        anglePIDController.reset();
        hoodPIDController.reset();
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

    public void setHoodSpeed(double speed){
        hoodMotor.set(speed);
    }

    public void setHoodPosition(double target){
        double speed = hoodPIDController.calculate(getHoodPos(), target);
        setHoodSpeed(speed);
    }

    public double getHoodPos(){
        return hoodMotor.getAbsoluteEncoder().getPosition();
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

    public double getTargetLaunchSpeed(AimPoints target) {
        // Use the motion-compensated virtual target for a more accurate launch speed.
        Translation2d virtualTarget = getMotionCompensatedTargetTranslation(target);
        double distance = getRobotPose().getTranslation().getDistance(virtualTarget);
        return getSpeedForDistance(distance, target);
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

        // Check prior to adjusting if out-of-bounds
        turretAtTarget = isAtTargetAngle(targetAngle) || !SmartDashboard.getBoolean("USELIMELIGHT", true);

        // Cases to go to turret zero position
        if( targetAngle > Constants.TURRET_LEFT_LIMIT_DEG ||
            targetAngle < Constants.TURRET_RIGHT_LIMIT_DEG ||
            !SmartDashboard.getBoolean("USELIMELIGHT", true)
        ){
            targetAngle = 0.0;
        }

        double angleSpeed = anglePIDController.calculate(getTurretAngleDegrees(), targetAngle);
        angleSpeed = MathUtil.clamp(angleSpeed, -Constants.TURRET_MAX_SPEED, Constants.TURRET_MAX_SPEED);
        SmartDashboard.putBoolean("Launcher/Turret_At_Position", turretAtTarget);
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
                    aimatTarget(Constants.AimPoints.BLUE_FAR_SIDE);
                    SmartDashboard.putString("aimtarget", "BLUE_FAR_SIDE");
                    return Constants.AimPoints.BLUE_FAR_SIDE;
                } else if (robotpositionPose2d.getY() < 4.03){
                    // If in left half of midfield, aim at far side for passing back
                    aimatTarget(Constants.AimPoints.BLUE_OUTPOST);
                    SmartDashboard.putString("aimtarget", "BLUE_OUTPOST");
                    return Constants.AimPoints.BLUE_OUTPOST;
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
                    aimatTarget(Constants.AimPoints.RED_FAR_SIDE);
                    SmartDashboard.putString("aimtarget", "RED_FAR_SIDE");
                    return Constants.AimPoints.RED_FAR_SIDE;
                } else if (robotpositionPose2d.getY() > 4.03){
                    // If in right half of midfield, aim at far side for passing back
                    aimatTarget(Constants.AimPoints.RED_OUTPOST);
                    SmartDashboard.putString("aimtarget", "RED_OUTPOST");
                    return Constants.AimPoints.RED_OUTPOST;
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

        // Compute motion-compensated virtual target so the projectile arrives at the
        // real target even while the robot is moving during flight.
        Translation2d virtualTarget = getMotionCompensatedTargetTranslation(target);

        // vector from robot to virtual target (field coordinates)
        Translation2d difference = virtualTarget
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
        turretAngleDeg = MathUtil.inputModulus(turretAngleDeg, -180, 180);

        SmartDashboard.putNumber("Launcher/ComputedTurretAngle", turretAngleDeg);

        // TODO delete this AI generated comment
        // send to turret controller
        setTurretAngle(turretAngleDeg * 1.0);

    }

    /**
     * Activates the Kicker
     */
    public void activateKicker(AimPoints target) {
        double matchTime = DriverStation.getMatchTime();
        // Turret Check      Robot Velocity                      Timer Check start             Target gating
        if(turretAtTarget /* && isRobotVelocityBelowThreshold() */ && (HubTimer.isActive2sEarly() || HubTimer.isActive() || !(target == Constants.AimPoints.BLUE_HUB || target == Constants.AimPoints.RED_HUB)) || matchTime == -1){
            setKickerSpeed(Constants.KICKER_SPEED); // Run kicker at predefined speed if at target launch speed and angle
        } else {
            setKickerSpeed(0); // Otherwise, stop the kicker to prevent feeding balls when not ready
        }
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
        kickerMotor.set(driveSpeed);
    }

    /**
     * Gets the appropriate launch speed for a given distance to the target using linear interpolation
     * between the points defined in the distanceToSpeedScoring map. If the distance is outside the range of
     * the map, it will return the speed of the nearest endpoint.
     *
     * @param distance The distance to the target for which to get the launch speed
     * @return The calculated launch speed based on the distance
     */
    public double getSpeedForDistance(double distance, AimPoints target) {
        if (distanceToSpeedScoring.isEmpty()) {
            throw new IllegalStateException("distance->Scoring speed map is empty");
        }

        if (distanceToSpeedYeeting.isEmpty()) {
            throw new IllegalStateException("distance->Yeet speed map is empty");
        }

        double lowDist;
        double highDist;
        double lowSpeed;
        double highSpeed;

        if(target == Constants.AimPoints.BLUE_HUB || target == Constants.AimPoints.RED_HUB){
            SmartDashboard.putString("Launcher/SpeedMap", "Scoring");
            // Hood is up, use scoring map
            Double exact = distanceToSpeedScoring.get(distance);
            if (exact != null) return exact;

            Double lowKey = distanceToSpeedScoring.floorKey(distance);
            Double highKey = distanceToSpeedScoring.ceilingKey(distance);

            // out of range: use nearest endpoint
            if (lowKey == null) return distanceToSpeedScoring.get(highKey);
            if (highKey == null) return distanceToSpeedScoring.get(lowKey);

            lowDist = lowKey;
            highDist = highKey;
            lowSpeed = distanceToSpeedScoring.get(lowKey);
            highSpeed = distanceToSpeedScoring.get(highKey);
        } else {
            SmartDashboard.putString("Launcher/SpeedMap", "Yeeting");
            // Hood is down, use yeeting map
            Double exactYeet = distanceToSpeedYeeting.get(distance);
            if (exactYeet != null) return exactYeet;

            Double lowKeyYeet = distanceToSpeedYeeting.floorKey(distance);
            Double highKeyYeet = distanceToSpeedYeeting.ceilingKey(distance);

            // out of range: use nearest endpoint
            if (lowKeyYeet == null) return distanceToSpeedYeeting.get(highKeyYeet);
            if (highKeyYeet == null) return distanceToSpeedYeeting.get(lowKeyYeet);

            lowDist = lowKeyYeet;
            highDist = highKeyYeet;
            lowSpeed = distanceToSpeedYeeting.get(lowKeyYeet);
            highSpeed = distanceToSpeedYeeting.get(highKeyYeet);
        }

        if (highDist == lowDist) return lowSpeed; // guard division by zero

        double t = (distance - lowDist) / (highDist - lowDist);
        double offset_adjust = SmartDashboard.getNumber("configlaunch", 0);
        if(SmartDashboard.getBoolean("USELIMELIGHT", true)){
            return lowSpeed + t * (highSpeed - lowSpeed) + offset_adjust;
        } else {
            return 3.0;
        }
    }

    /**
     * Converts the robot-relative {@link ChassisSpeeds} from the velocity supplier into
     * field-relative X/Y velocity components.
     *
     * @return Field-relative velocity as a {@link Translation2d} (metres per second).
     */
    private Translation2d getFieldRelativeVelocity() {
        ChassisSpeeds speeds = getRobotVelocity();
        double heading = getRobotPose().getRotation().getRadians();
        double fieldVx = speeds.vxMetersPerSecond * Math.cos(heading)
                       - speeds.vyMetersPerSecond * Math.sin(heading);
        double fieldVy = speeds.vxMetersPerSecond * Math.sin(heading)
                       + speeds.vyMetersPerSecond * Math.cos(heading);
        return new Translation2d(fieldVx, fieldVy);
    }

    /**
     * Computes a motion-compensated virtual target position.
     *
     * <p>The robot moves during the projectile's flight, so we predict where it will be when
     * the projectile arrives and shift the target accordingly. Uses one iteration:
     * <ol>
     *   <li>Compute initial distance → look up launch speed → look up flight time.</li>
     *   <li>Compute robot displacement during that flight time (field-relative).</li>
     *   <li>Return {@code realTarget − displacement} as the virtual aim point.</li>
     * </ol>
     *
     * @param target The {@link AimPoints} value representing the real field target.
     * @return A field-relative {@link Translation2d} of the virtual (motion-compensated) target.
     */
    public Translation2d getMotionCompensatedTargetTranslation(AimPoints target) {
        Translation2d robotPos  = getRobotPose().getTranslation();
        Translation2d targetPos = target.value.toTranslation2d();

        // Step 1: initial distance → launch speed → flight time
        double initialDistance = robotPos.getDistance(targetPos);
        double initialSpeed    = getSpeedForDistance(initialDistance, target);
        double flightTime      = getFlightTimeForVelocity(initialSpeed);

        // Step 2: robot displacement during flight (field-relative)
        Translation2d fieldVelocity    = getFieldRelativeVelocity();
        Translation2d robotDisplacement = new Translation2d(
            fieldVelocity.getX() * flightTime,
            fieldVelocity.getY() * flightTime
        );

        // Step 3: virtual target = real target shifted back by robot displacement
        Translation2d virtualTarget = targetPos.minus(robotDisplacement);

        SmartDashboard.putNumber("Launcher/MotionComp/FlightTime",      flightTime);
        SmartDashboard.putNumber("Launcher/MotionComp/DisplacementX",   robotDisplacement.getX());
        SmartDashboard.putNumber("Launcher/MotionComp/DisplacementY",   robotDisplacement.getY());
        SmartDashboard.putNumber("Launcher/MotionComp/VirtualTargetX",  virtualTarget.getX());
        SmartDashboard.putNumber("Launcher/MotionComp/VirtualTargetY",  virtualTarget.getY());
        return virtualTarget;
    }

    /**
     * Gets the expected projectile flight time (seconds) for a given launcher velocity
     * using linear interpolation between the points defined in {@code velocityToFlightTime}.
     * If the velocity is outside the mapped range the nearest endpoint value is returned.
     *
     * @param velocity The launcher velocity in encoder units (same units used by {@link #getLaunchSpeed()})
     * @return The interpolated flight time in seconds
     */
    public double getFlightTimeForVelocity(double velocity) {
        if (velocityToFlightTime.isEmpty()) {
            throw new IllegalStateException("velocity->flightTime map is empty");
        }

        Double exact = velocityToFlightTime.get(velocity);
        if (exact != null) return exact;

        Double lowKey  = velocityToFlightTime.floorKey(velocity);
        Double highKey = velocityToFlightTime.ceilingKey(velocity);

        // Out of range: return nearest endpoint
        if (lowKey  == null) return velocityToFlightTime.get(highKey);
        if (highKey == null) return velocityToFlightTime.get(lowKey);

        double t = (velocity - lowKey) / (highKey - lowKey);
        double lowTime  = velocityToFlightTime.get(lowKey);
        double highTime = velocityToFlightTime.get(highKey);
        return lowTime + t * (highTime - lowTime);
    }

    /**
     *  Checks Temp of the Launcher
     */
    public double getLauncherTemp() {
        return launchMotor.getMotorTemperature();
    }

    /**
     *  Checks the Temp of the Kicker
     */
    public double getKickerTemp() {
        return kickerMotor.getMotorTemperature();
    }

    /**
     *  Checks the Temp of the Angle Motor
     */
    public double getTurretAngleTemp() {
        return turretAngle.getMotorTemperature();
    }

    /**
     *  Checks the Temp of the Hood Motor
     */
    public double getHoodTemp() {
        return hoodMotor.getMotorTemperature();
    }
}
// the cake was a lie