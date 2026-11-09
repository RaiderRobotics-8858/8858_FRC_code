package frc.robot.util;

import static edu.wpi.first.units.Units.Meters;
import static edu.wpi.first.units.Units.MetersPerSecond;
import static edu.wpi.first.units.Units.Radians;

import org.littletonrobotics.junction.Logger;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.Timer;
import frc.robot.Constants;
import frc.robot.Constants.AimPoints;
import frc.robot.subsystems.IntakeSubsystem;
import swervelib.SwerveDrive;
import swervelib.simulation.ironmaple.simulation.SimulatedArena;
import swervelib.simulation.ironmaple.simulation.drivesims.SwerveDriveSimulation;
import swervelib.simulation.ironmaple.simulation.gamepieces.GamePieceProjectile;
import swervelib.simulation.ironmaple.simulation.seasonspecific.rebuilt2026.RebuiltFuelOnFly;

/**
 * Simulation-only launcher. The real flywheel, hood and turret motors are not simulated, so this
 * solves the ballistic launch directly: it aims the turret at the target, picks the speed that
 * lands the fuel on it, and spawns a projectile from the hopper while the launch command runs.
 */
public class LauncherSimulation {
    private static final double GRAVITY = 9.81;

    private final SwerveDriveSimulation driveSim;
    private final IntakeSubsystem intake;
    private double lastLaunchTime = 0;
    private int launched = 0;
    private final java.util.List<TrackedShot> tracked = new java.util.ArrayList<>();

    public LauncherSimulation(SwerveDrive swerveDrive, IntakeSubsystem intake) {
        this.driveSim = swerveDrive.getMapleSimDrive().orElse(null);
        this.intake = intake;
    }

    public int getLaunchedCount() {
        return launched;
    }

    public boolean isAvailable() {
        return driveSim != null;
    }

    /** Fires at most one fuel per launch interval toward the target if the turret can reach it. */
    public void tryLaunch(AimPoints target) {
        if (driveSim == null) {
            return;
        }
        double now = Timer.getFPGATimestamp();
        if (now - lastLaunchTime < Constants.SIM_LAUNCH_INTERVAL_SECONDS) {
            return;
        }

        var robotPose = driveSim.getSimulatedDriveTrainPose();
        ChassisSpeeds fieldSpeeds = driveSim.getDriveTrainSimulatedChassisSpeedsFieldRelative();
        Translation2d muzzle = robotPose.getTranslation().plus(
            new Translation2d(Constants.SIM_SHOOTER_X, Constants.SIM_SHOOTER_Y).rotateBy(robotPose.getRotation()));

        // Aim at the arena's hub opening, not the team's tuned AimPoints (which sit below the rim)
        boolean isHub = target == AimPoints.BLUE_HUB || target == AimPoints.RED_HUB;
        Translation3d targetPoint = target.value;
        if (isHub) {
            double hubX = target == AimPoints.BLUE_HUB
                ? Constants.SIM_HUB_BLUE_X
                : Constants.SIM_FIELD_LENGTH_METERS - Constants.SIM_HUB_BLUE_X;
            targetPoint = new Translation3d(hubX, Constants.SIM_HUB_Y, Constants.SIM_HUB_OPENING_Z);
        }

        // Lead the target by the robot's own motion so the fuel still lands on it
        double pitch = Math.toRadians(isHub ? Constants.SIM_HUB_PITCH_DEG : Constants.SIM_PASS_PITCH_DEG);
        Translation2d aimPoint = targetPoint.toTranslation2d();
        double speed = 0;
        for (int i = 0; i < 3; i++) {
            double distance = muzzle.getDistance(aimPoint);
            speed = requiredSpeed(distance, targetPoint.getZ() - Constants.SIM_SHOOTER_Z, pitch);
            double flight = distance / Math.max(1e-3, speed * Math.cos(pitch));
            aimPoint = targetPoint.toTranslation2d().minus(
                new Translation2d(fieldSpeeds.vxMetersPerSecond * flight, fieldSpeeds.vyMetersPerSecond * flight));
        }

        Rotation2d fieldYaw = aimPoint.minus(muzzle).getAngle();
        double turretDeg = MathUtil.inputModulus(
            fieldYaw.minus(robotPose.getRotation()).getDegrees(), -180, 180);
        boolean reachable = turretDeg >= Constants.TURRET_RIGHT_LIMIT_DEG
            && turretDeg <= Constants.TURRET_LEFT_LIMIT_DEG;
        Logger.recordOutput("Launcher/SimTurretAngleDeg", turretDeg);
        Logger.recordOutput("Launcher/SimTurretReachable", reachable);
        if (!reachable || intake.getSimFuelCount() <= 0 || !intake.consumeSimFuel()) {
            return;
        }

        lastLaunchTime = now;
        launched++;
        GamePieceProjectile shot = new RebuiltFuelOnFly(
            muzzle,
            Translation2d.kZero,
            fieldSpeeds,
            fieldYaw,
            Meters.of(Constants.SIM_SHOOTER_Z),
            MetersPerSecond.of(speed * Constants.SIM_LAUNCH_SPEED_SCALE),
            Radians.of(pitch));
        SimulatedArena.getInstance().addGamePieceProjectile(shot);
        if (isHub) {
            tracked.add(new TrackedShot(shot, targetPoint.toTranslation2d(),
                target == AimPoints.BLUE_HUB, targetPoint.getZ()));
        }
        Logger.recordOutput("Launcher/SimLaunchSpeedMps", speed);
        Logger.recordOutput("Launcher/SimFuelLaunched", launched);
    }

    /**
     * The arena only scores fuel that has landed, and projectiles have no collision, so hub shots
     * are watched here: fuel dropping through the opening scores, is removed, and re-emerges out
     * the back of the hub instead of flying through its walls.
     */
    public void update() {
        var arena = SimulatedArena.getInstance();
        for (var it = tracked.iterator(); it.hasNext();) {
            TrackedShot t = it.next();
            // Dropped by a field reset or already landed: it no longer exists, so it must not spawn exit fuel
            if (!arena.gamePieceLaunched().contains(t.shot)) {
                it.remove();
                continue;
            }
            var pose = t.shot.getPose3d();
            double z = pose.getZ();
            boolean over = pose.getTranslation().toTranslation2d().getDistance(t.hub)
                < Constants.SIM_HUB_OPENING_RADIUS;
            Translation2d xy = pose.getTranslation().toTranslation2d();
            boolean inBody = Math.abs(xy.getX() - t.hub.getX()) < Constants.SIM_HUB_HALF_WIDTH
                && Math.abs(xy.getY() - t.hub.getY()) < Constants.SIM_HUB_HALF_WIDTH;
            if (over && t.lastZ >= Constants.SIM_HUB_RIM_Z && z < Constants.SIM_HUB_RIM_Z) {
                arena.removeProjectile(t.shot);
                // Reuses the dashboard flag set from HubTimer; fuel into an inactive hub passes through unscored
                if (edu.wpi.first.wpilibj.smartdashboard.SmartDashboard.getBoolean("Game/Hub Active", true)) {
                    arena.addToScore(t.blue, 1);
                    Logger.recordOutput("FieldSimulation/LastShotScored", true);
                } else {
                    Logger.recordOutput("FieldSimulation/LastShotScored", false);
                }
                // Scored fuel always leaves the same side: away from its own alliance wall
                Rotation2d exitDir = t.blue ? Rotation2d.kZero : Rotation2d.k180deg;
                Translation2d exitStart = t.hub.plus(new Translation2d(Constants.SIM_HUB_EXIT_OFFSET, exitDir));
                arena.addGamePieceProjectile(new RebuiltFuelOnFly(
                    exitStart, Translation2d.kZero, new ChassisSpeeds(), exitDir,
                    Meters.of(Constants.SIM_HUB_EXIT_Z), MetersPerSecond.of(Constants.SIM_HUB_EXIT_SPEED),
                    Radians.of(0)));
                it.remove();
                continue;
            }
            if (inBody && z < Constants.SIM_HUB_RIM_Z && !over) {
                // Missed the opening and hit the hub body: bounce back toward the shooter
                arena.removeProjectile(t.shot);
                Rotation2d back = t.launchOrigin.minus(t.hub).getAngle();
                Translation2d start = t.hub.plus(new Translation2d(Constants.SIM_HUB_EXIT_OFFSET, back));
                arena.addGamePieceProjectile(new RebuiltFuelOnFly(
                    start, Translation2d.kZero, new ChassisSpeeds(), back,
                    Meters.of(Math.max(0.3, z)), MetersPerSecond.of(1.0), Radians.of(0)));
                it.remove();
                continue;
            }
            t.lastZ = z;
            if (z < 0.1 || t.shot.hasHitGround()) {
                it.remove();
            }
        }
        Logger.recordOutput("FieldSimulation/HubShotsInFlight", tracked.size());
    }

    private static final class TrackedShot {
        final GamePieceProjectile shot;
        final Translation2d hub;
        final Translation2d launchOrigin;
        final boolean blue;
        double lastZ;

        TrackedShot(GamePieceProjectile shot, Translation2d hub, boolean blue, double aimZ) {
            this.shot = shot;
            this.hub = hub;
            this.blue = blue;
            this.launchOrigin = shot.getPose3d().getTranslation().toTranslation2d();
            this.lastZ = shot.getPose3d().getZ();
        }
    }

    /** Speed needed for a fixed-pitch shot to pass through a point deltaZ above the muzzle. */
    private static double requiredSpeed(double distance, double deltaZ, double pitch) {
        double denom = 2 * Math.pow(Math.cos(pitch), 2) * (distance * Math.tan(pitch) - deltaZ);
        if (denom <= 1e-6) {
            return Constants.SIM_MAX_LAUNCH_SPEED_MPS;
        }
        double v = Math.sqrt(GRAVITY * distance * distance / denom);
        return Math.min(v, Constants.SIM_MAX_LAUNCH_SPEED_MPS);
    }
}
