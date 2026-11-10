package frc.robot.util;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Constants;
import frc.robot.subsystems.IntakeSubsystem;
import frc.robot.subsystems.LauncherSubsystem;
import frc.robot.subsystems.SwerveSubsystem;
import org.littletonrobotics.junction.Logger;

/**
 * Logs articulated robot-part poses for AdvantageScope 3D assets in
 * {@code advantagescope_assets/Echidna_Kinematic}.
 *
 * <p>Topics logged (Pose3d):
 * <ul>
 *   <li>{@code EchidnaKinematic/Chassis}</li>
 *   <li>{@code EchidnaKinematic/Intake}</li>
 *   <li>{@code EchidnaKinematic/Turret}</li>
 * </ul>
 */
public class EchidnaKinematicLogger {

    // Intake endpoint calibrations from raised/lowered snapshots.
    // We interpolate between these so visual motion can match both endpoints even if
    // model rigging isn't centered exactly on the CAD joint.
    private static final Translation3d INTAKE_PIVOT_RAISED_OFFSET_METERS = new Translation3d(0.30, -1.71, 0.38);
    private static final Translation3d INTAKE_PIVOT_LOWERED_OFFSET_METERS = new Translation3d(-0.89, -1.71, 0.44);
    private static final Translation3d TURRET_PIVOT_OFFSET_METERS = new Translation3d(0.20, 0.00, 0.41);

    // Intake arm visual mapping from encoder range to pitch rotation.
    private static final double INTAKE_PITCH_RAISED_DEG = 71.00;
    private static final double INTAKE_PITCH_LOWERED_DEG = 230.00;

    private static final String TUNE_PREFIX = "EchidnaKinematic/Tuning/";
    private static final String INTAKE_PIVOT_RAISED_X_KEY = TUNE_PREFIX + "IntakePivotRaisedX_m";
    private static final String INTAKE_PIVOT_RAISED_Y_KEY = TUNE_PREFIX + "IntakePivotRaisedY_m";
    private static final String INTAKE_PIVOT_RAISED_Z_KEY = TUNE_PREFIX + "IntakePivotRaisedZ_m";
    private static final String INTAKE_PIVOT_LOWERED_X_KEY = TUNE_PREFIX + "IntakePivotLoweredX_m";
    private static final String INTAKE_PIVOT_LOWERED_Y_KEY = TUNE_PREFIX + "IntakePivotLoweredY_m";
    private static final String INTAKE_PIVOT_LOWERED_Z_KEY = TUNE_PREFIX + "IntakePivotLoweredZ_m";
    private static final String INTAKE_PIVOT_FIXED_X_KEY = TUNE_PREFIX + "IntakePivotFixedX_m";
    private static final String INTAKE_PIVOT_FIXED_Y_KEY = TUNE_PREFIX + "IntakePivotFixedY_m";
    private static final String INTAKE_PIVOT_FIXED_Z_KEY = TUNE_PREFIX + "IntakePivotFixedZ_m";
    private static final String INTAKE_USE_MOVING_AXIS_KEY = TUNE_PREFIX + "IntakeUseMovingAxis";
    private static final String TURRET_PIVOT_X_KEY = TUNE_PREFIX + "TurretPivotX_m";
    private static final String TURRET_PIVOT_Y_KEY = TUNE_PREFIX + "TurretPivotY_m";
    private static final String TURRET_PIVOT_Z_KEY = TUNE_PREFIX + "TurretPivotZ_m";
    private static final String INTAKE_PITCH_RAISED_KEY = TUNE_PREFIX + "IntakePitchRaised_deg";
    private static final String INTAKE_PITCH_LOWERED_KEY = TUNE_PREFIX + "IntakePitchLowered_deg";
    private static final String INTAKE_DIRECTION_SIGN_KEY = TUNE_PREFIX + "IntakeDirectionSign";
    private static final String INTAKE_MODEL_OFFSET_X_KEY = TUNE_PREFIX + "IntakeModelOffsetX_m";
    private static final String INTAKE_MODEL_OFFSET_Y_KEY = TUNE_PREFIX + "IntakeModelOffsetY_m";
    private static final String INTAKE_MODEL_OFFSET_Z_KEY = TUNE_PREFIX + "IntakeModelOffsetZ_m";
    private static final String SIM_INTAKE_KP_KEY = TUNE_PREFIX + "SimIntakePidKp";
    private static final String SIM_INTAKE_KD_KEY = TUNE_PREFIX + "SimIntakePidKd";
    private static final String SIM_INTAKE_MAX_RATE_KEY = TUNE_PREFIX + "SimIntakeMaxRatePerSec";
    private static final String SIM_TURRET_KP_KEY = TUNE_PREFIX + "SimTurretPidKp";
    private static final String SIM_TURRET_KD_KEY = TUNE_PREFIX + "SimTurretPidKd";
    private static final String SIM_TURRET_MAX_RATE_KEY = TUNE_PREFIX + "SimTurretMaxRateDegPerSec";

    private static final double SIM_INTAKE_KP_DEFAULT = 8.0;
    private static final double SIM_INTAKE_KD_DEFAULT = 0.5;
    private static final double SIM_INTAKE_MAX_RATE_DEFAULT = 1.5;
    private static final double SIM_TURRET_KP_DEFAULT = 10.0;
    private static final double SIM_TURRET_KD_DEFAULT = 0.75;
    private static final double SIM_TURRET_MAX_RATE_DEFAULT = 240.0;
    private static final double INTAKE_DIRECTION_SIGN_DEFAULT = -1.0;
    private static final boolean INTAKE_USE_MOVING_AXIS_DEFAULT = false;
    private static final Translation3d INTAKE_PIVOT_FIXED_OFFSET_METERS = new Translation3d(-0.28, -1.71, 0.26);
    // private static final Translation3d INTAKE_PIVOT_FIXED_OFFSET_METERS = new Translation3d(
    //     (INTAKE_PIVOT_RAISED_OFFSET_METERS.getX() + INTAKE_PIVOT_LOWERED_OFFSET_METERS.getX()) / 2.0,
    //     (INTAKE_PIVOT_RAISED_OFFSET_METERS.getY() + INTAKE_PIVOT_LOWERED_OFFSET_METERS.getY()) / 2.0,
    //     (INTAKE_PIVOT_RAISED_OFFSET_METERS.getZ() + INTAKE_PIVOT_LOWERED_OFFSET_METERS.getZ()) / 2.0);
    private static final Translation3d INTAKE_MODEL_OFFSET_DEFAULT_METERS = new Translation3d(-0.34, 0.0, -0.49);

    private final SwerveSubsystem drive;
    private final IntakeSubsystem intake;
    private final LauncherSubsystem launcher;
    private final PIDController intakeSimPid = new PIDController(SIM_INTAKE_KP_DEFAULT, 0.0, SIM_INTAKE_KD_DEFAULT);
    private final PIDController turretSimPid = new PIDController(SIM_TURRET_KP_DEFAULT, 0.0, SIM_TURRET_KD_DEFAULT);

    private boolean simStateInitialized = false;
    private double simIntakeReference = Constants.INTAKE_ARM_RAISED;
    private double simTurretYawDegrees = 0.0;
    private double simLastTimestamp = 0.0;

    public EchidnaKinematicLogger(
        SwerveSubsystem drive,
        IntakeSubsystem intake,
        LauncherSubsystem launcher
    ) {
        this.drive = drive;
        this.intake = intake;
        this.launcher = launcher;
        turretSimPid.enableContinuousInput(-180.0, 180.0);
        publishDefaultTunables();
    }

    /** Computes and logs current articulated part poses. */
    public void log() {
        Pose3d chassisPose = drive.getPose3d();
        double measuredIntakeReference = intake.getIntakeArmPosition();
        double measuredTurretYawDegrees = launcher.getTurretAngleDegrees();
        double intakeReference;
        double turretYawDegrees;

        if (RobotBase.isSimulation()) {
            double intakeTargetReference = intake.getIntakeArmTarget();
            double turretTargetYawDegrees = launcher.getTurretTargetDegrees();
            updateSimMechanismState(intakeTargetReference, turretTargetYawDegrees);
            intakeReference = simIntakeReference;
            turretYawDegrees = simTurretYawDegrees;
        } else {
            intakeReference = measuredIntakeReference;
            turretYawDegrees = measuredTurretYawDegrees;
            simStateInitialized = false;
        }

        double intakeNormalized = getIntakeNormalized(intakeReference);
        double intakeDirectionSign = SmartDashboard.getNumber(INTAKE_DIRECTION_SIGN_KEY, INTAKE_DIRECTION_SIGN_DEFAULT);
        double intakeVisualBlend = intakeDirectionSign >= 0.0 ? intakeNormalized : 1.0 - intakeNormalized;

        Translation3d intakePivotRaised = new Translation3d(
            SmartDashboard.getNumber(INTAKE_PIVOT_RAISED_X_KEY, INTAKE_PIVOT_RAISED_OFFSET_METERS.getX()),
            SmartDashboard.getNumber(INTAKE_PIVOT_RAISED_Y_KEY, INTAKE_PIVOT_RAISED_OFFSET_METERS.getY()),
            SmartDashboard.getNumber(INTAKE_PIVOT_RAISED_Z_KEY, INTAKE_PIVOT_RAISED_OFFSET_METERS.getZ()));

        Translation3d intakePivotLowered = new Translation3d(
            SmartDashboard.getNumber(INTAKE_PIVOT_LOWERED_X_KEY, INTAKE_PIVOT_LOWERED_OFFSET_METERS.getX()),
            SmartDashboard.getNumber(INTAKE_PIVOT_LOWERED_Y_KEY, INTAKE_PIVOT_LOWERED_OFFSET_METERS.getY()),
            SmartDashboard.getNumber(INTAKE_PIVOT_LOWERED_Z_KEY, INTAKE_PIVOT_LOWERED_OFFSET_METERS.getZ()));

        Translation3d intakePivotMovingOffset = new Translation3d(
            MathUtil.interpolate(intakePivotRaised.getX(), intakePivotLowered.getX(), intakeVisualBlend),
            MathUtil.interpolate(intakePivotRaised.getY(), intakePivotLowered.getY(), intakeVisualBlend),
            MathUtil.interpolate(intakePivotRaised.getZ(), intakePivotLowered.getZ(), intakeVisualBlend));

        Translation3d intakePivotFixedOffset = new Translation3d(
            SmartDashboard.getNumber(INTAKE_PIVOT_FIXED_X_KEY, INTAKE_PIVOT_FIXED_OFFSET_METERS.getX()),
            SmartDashboard.getNumber(INTAKE_PIVOT_FIXED_Y_KEY, INTAKE_PIVOT_FIXED_OFFSET_METERS.getY()),
            SmartDashboard.getNumber(INTAKE_PIVOT_FIXED_Z_KEY, INTAKE_PIVOT_FIXED_OFFSET_METERS.getZ()));
        boolean intakeUseMovingAxis = SmartDashboard.getBoolean(
            INTAKE_USE_MOVING_AXIS_KEY,
            INTAKE_USE_MOVING_AXIS_DEFAULT);
        Translation3d intakePivotOffset = intakeUseMovingAxis ? intakePivotMovingOffset : intakePivotFixedOffset;

        Translation3d turretPivotOffset = new Translation3d(
            SmartDashboard.getNumber(TURRET_PIVOT_X_KEY, TURRET_PIVOT_OFFSET_METERS.getX()),
            SmartDashboard.getNumber(TURRET_PIVOT_Y_KEY, TURRET_PIVOT_OFFSET_METERS.getY()),
            SmartDashboard.getNumber(TURRET_PIVOT_Z_KEY, TURRET_PIVOT_OFFSET_METERS.getZ()));

        double intakePitchRaisedDeg =
            SmartDashboard.getNumber(INTAKE_PITCH_RAISED_KEY, INTAKE_PITCH_RAISED_DEG);
        double intakePitchLoweredDeg =
            SmartDashboard.getNumber(INTAKE_PITCH_LOWERED_KEY, INTAKE_PITCH_LOWERED_DEG);

        double intakePitchRad = getIntakePitchRadians(intakeVisualBlend, intakePitchRaisedDeg, intakePitchLoweredDeg);
        Translation3d intakeModelOffset = new Translation3d(
            SmartDashboard.getNumber(INTAKE_MODEL_OFFSET_X_KEY, INTAKE_MODEL_OFFSET_DEFAULT_METERS.getX()),
            SmartDashboard.getNumber(INTAKE_MODEL_OFFSET_Y_KEY, INTAKE_MODEL_OFFSET_DEFAULT_METERS.getY()),
            SmartDashboard.getNumber(INTAKE_MODEL_OFFSET_Z_KEY, INTAKE_MODEL_OFFSET_DEFAULT_METERS.getZ()));
        Pose3d intakeAxisPose = chassisPose.transformBy(new Transform3d(intakePivotOffset, new Rotation3d()));
        Pose3d intakeAxisRaisedPose = chassisPose.transformBy(new Transform3d(intakePivotRaised, new Rotation3d()));
        Pose3d intakeAxisLoweredPose = chassisPose.transformBy(new Transform3d(intakePivotLowered, new Rotation3d()));
        Pose3d intakePose = chassisPose
            .transformBy(new Transform3d(intakePivotOffset, new Rotation3d()))
            .transformBy(new Transform3d(new Translation3d(), new Rotation3d(0.0, intakePitchRad, 0.0)))
            .transformBy(new Transform3d(intakeModelOffset, new Rotation3d()));

        double turretYawRad = Units.degreesToRadians(turretYawDegrees);
        Pose3d turretPose = chassisPose.transformBy(
            new Transform3d(turretPivotOffset, new Rotation3d(0.0, 0.0, turretYawRad)));

        Logger.recordOutput("EchidnaKinematic/Chassis", chassisPose);
        Logger.recordOutput("EchidnaKinematic/Intake", intakePose);
    Logger.recordOutput("EchidnaKinematic/IntakeAxis", intakeAxisPose);
    Logger.recordOutput("EchidnaKinematic/IntakeAxisRaised", intakeAxisRaisedPose);
    Logger.recordOutput("EchidnaKinematic/IntakeAxisLowered", intakeAxisLoweredPose);
        Logger.recordOutput("EchidnaKinematic/Turret", turretPose);
        Logger.recordOutput("EchidnaKinematic/AllParts", new Pose3d[] {chassisPose, intakePose, turretPose});
    Logger.recordOutput("EchidnaKinematic/IntakeBlend", intakeNormalized);
        Logger.recordOutput("EchidnaKinematic/IntakeVisualBlend", intakeVisualBlend);
    Logger.recordOutput("EchidnaKinematic/IntakeUseMovingAxis", intakeUseMovingAxis);
        Logger.recordOutput("EchidnaKinematic/IntakeReference", intakeReference);
        Logger.recordOutput("EchidnaKinematic/IntakeReferenceMeasured", measuredIntakeReference);
        Logger.recordOutput("EchidnaKinematic/IntakePitchDeg", Units.radiansToDegrees(intakePitchRad));
    Logger.recordOutput("EchidnaKinematic/IntakePivotFixedOffset", intakePivotFixedOffset);
    Logger.recordOutput("EchidnaKinematic/IntakePivotMovingOffset", intakePivotMovingOffset);
        Logger.recordOutput("EchidnaKinematic/IntakeModelOffset", intakeModelOffset);
        Logger.recordOutput("EchidnaKinematic/TurretYawDeg", turretYawDegrees);
        Logger.recordOutput("EchidnaKinematic/TurretYawMeasuredDeg", measuredTurretYawDegrees);
    }

    private void updateSimMechanismState(double intakeTargetReference, double turretTargetYawDegrees) {
        double now = Timer.getFPGATimestamp();
        if (!simStateInitialized) {
            simIntakeReference = intakeTargetReference;
            simTurretYawDegrees = turretTargetYawDegrees;
            simLastTimestamp = now;
            intakeSimPid.reset();
            turretSimPid.reset();
            simStateInitialized = true;
            return;
        }

        double dt = MathUtil.clamp(now - simLastTimestamp, 1e-3, 0.05);
        simLastTimestamp = now;

        intakeSimPid.setPID(
            SmartDashboard.getNumber(SIM_INTAKE_KP_KEY, SIM_INTAKE_KP_DEFAULT),
            0.0,
            SmartDashboard.getNumber(SIM_INTAKE_KD_KEY, SIM_INTAKE_KD_DEFAULT));
        turretSimPid.setPID(
            SmartDashboard.getNumber(SIM_TURRET_KP_KEY, SIM_TURRET_KP_DEFAULT),
            0.0,
            SmartDashboard.getNumber(SIM_TURRET_KD_KEY, SIM_TURRET_KD_DEFAULT));

        double intakeMaxRate = SmartDashboard.getNumber(SIM_INTAKE_MAX_RATE_KEY, SIM_INTAKE_MAX_RATE_DEFAULT);
        double turretMaxRate = SmartDashboard.getNumber(SIM_TURRET_MAX_RATE_KEY, SIM_TURRET_MAX_RATE_DEFAULT);

        double intakeRate = MathUtil.clamp(
            intakeSimPid.calculate(simIntakeReference, intakeTargetReference),
            -intakeMaxRate,
            intakeMaxRate);
        simIntakeReference += intakeRate * dt;

        double turretRate = MathUtil.clamp(
            turretSimPid.calculate(simTurretYawDegrees, turretTargetYawDegrees),
            -turretMaxRate,
            turretMaxRate);
        simTurretYawDegrees = MathUtil.inputModulus(simTurretYawDegrees + turretRate * dt, -180.0, 180.0);

        Logger.recordOutput("EchidnaKinematic/SimIntakeTarget", intakeTargetReference);
        Logger.recordOutput("EchidnaKinematic/SimTurretTargetDeg", turretTargetYawDegrees);
        Logger.recordOutput("EchidnaKinematic/SimDtSeconds", dt);
    }

    private static double getIntakeNormalized(double intakeArmPosition) {
        double normalized = MathUtil.inverseInterpolate(
            Constants.INTAKE_ARM_RAISED,
            Constants.INTAKE_ARM_LOWERED,
            intakeArmPosition);
        return MathUtil.clamp(normalized, 0.0, 1.0);
    }

    private static double getIntakePitchRadians(
        double intakeNormalized,
        double intakePitchRaisedDeg,
        double intakePitchLoweredDeg
    ) {
        double degrees = MathUtil.interpolate(intakePitchRaisedDeg, intakePitchLoweredDeg, intakeNormalized);
        return Units.degreesToRadians(degrees);
    }

    private static void publishDefaultTunables() {
        SmartDashboard.putNumber(INTAKE_PIVOT_RAISED_X_KEY, INTAKE_PIVOT_RAISED_OFFSET_METERS.getX());
        SmartDashboard.putNumber(INTAKE_PIVOT_RAISED_Y_KEY, INTAKE_PIVOT_RAISED_OFFSET_METERS.getY());
        SmartDashboard.putNumber(INTAKE_PIVOT_RAISED_Z_KEY, INTAKE_PIVOT_RAISED_OFFSET_METERS.getZ());
        SmartDashboard.putNumber(INTAKE_PIVOT_LOWERED_X_KEY, INTAKE_PIVOT_LOWERED_OFFSET_METERS.getX());
        SmartDashboard.putNumber(INTAKE_PIVOT_LOWERED_Y_KEY, INTAKE_PIVOT_LOWERED_OFFSET_METERS.getY());
        SmartDashboard.putNumber(INTAKE_PIVOT_LOWERED_Z_KEY, INTAKE_PIVOT_LOWERED_OFFSET_METERS.getZ());
    SmartDashboard.putNumber(INTAKE_PIVOT_FIXED_X_KEY, INTAKE_PIVOT_FIXED_OFFSET_METERS.getX());
    SmartDashboard.putNumber(INTAKE_PIVOT_FIXED_Y_KEY, INTAKE_PIVOT_FIXED_OFFSET_METERS.getY());
    SmartDashboard.putNumber(INTAKE_PIVOT_FIXED_Z_KEY, INTAKE_PIVOT_FIXED_OFFSET_METERS.getZ());
    SmartDashboard.putBoolean(INTAKE_USE_MOVING_AXIS_KEY, INTAKE_USE_MOVING_AXIS_DEFAULT);
        SmartDashboard.putNumber(TURRET_PIVOT_X_KEY, TURRET_PIVOT_OFFSET_METERS.getX());
        SmartDashboard.putNumber(TURRET_PIVOT_Y_KEY, TURRET_PIVOT_OFFSET_METERS.getY());
        SmartDashboard.putNumber(TURRET_PIVOT_Z_KEY, TURRET_PIVOT_OFFSET_METERS.getZ());
        SmartDashboard.putNumber(INTAKE_PITCH_RAISED_KEY, INTAKE_PITCH_RAISED_DEG);
        SmartDashboard.putNumber(INTAKE_PITCH_LOWERED_KEY, INTAKE_PITCH_LOWERED_DEG);
        SmartDashboard.putNumber(INTAKE_DIRECTION_SIGN_KEY, INTAKE_DIRECTION_SIGN_DEFAULT);
        SmartDashboard.putNumber(INTAKE_MODEL_OFFSET_X_KEY, INTAKE_MODEL_OFFSET_DEFAULT_METERS.getX());
        SmartDashboard.putNumber(INTAKE_MODEL_OFFSET_Y_KEY, INTAKE_MODEL_OFFSET_DEFAULT_METERS.getY());
        SmartDashboard.putNumber(INTAKE_MODEL_OFFSET_Z_KEY, INTAKE_MODEL_OFFSET_DEFAULT_METERS.getZ());
        SmartDashboard.putNumber(SIM_INTAKE_KP_KEY, SIM_INTAKE_KP_DEFAULT);
        SmartDashboard.putNumber(SIM_INTAKE_KD_KEY, SIM_INTAKE_KD_DEFAULT);
        SmartDashboard.putNumber(SIM_INTAKE_MAX_RATE_KEY, SIM_INTAKE_MAX_RATE_DEFAULT);
        SmartDashboard.putNumber(SIM_TURRET_KP_KEY, SIM_TURRET_KP_DEFAULT);
        SmartDashboard.putNumber(SIM_TURRET_KD_KEY, SIM_TURRET_KD_DEFAULT);
        SmartDashboard.putNumber(SIM_TURRET_MAX_RATE_KEY, SIM_TURRET_MAX_RATE_DEFAULT);
    }
}
