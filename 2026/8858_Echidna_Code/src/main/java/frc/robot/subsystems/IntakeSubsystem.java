package frc.robot.subsystems;

import static edu.wpi.first.units.Units.Inches;

import com.revrobotics.spark.SparkLowLevel.MotorType;
import org.littletonrobotics.junction.Logger;
import swervelib.SwerveDrive;
import swervelib.simulation.ironmaple.simulation.IntakeSimulation;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.Timer;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.wpilibj.DutyCycleEncoder;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;

import com.revrobotics.spark.SparkMax;

/**
 * Subsystem for controlling the intake mechanism, which consists
 * of an intake arm and rollers to pull in game pieces.
 */
public class IntakeSubsystem extends SubsystemBase {
    private final SparkMax rollerMotor;
    private final SparkMax armIntakeMotor;
    private final DutyCycleEncoder armIntakeEncoder;
    private final SlewRateLimiter intakeRateLimiter; 

    /** PID controller for maintaining intake arm position */
    private final PIDController armPIDController;
    private final double arm_kP = Constants.INTAKE_ARM_KP;
    private final double arm_kI = Constants.INTAKE_ARM_KI;
    private final double arm_kD = Constants.INTAKE_ARM_KD;

    private IntakeSimulation simIntake;
    private double[] pickupTimes = new double[0];
    private int lastCount = 0;
    private java.util.function.Supplier<edu.wpi.first.math.geometry.Pose2d> driveSimPose;
    private double armTarget = Constants.INTAKE_ARM_RAISED;
    private double simRollerSpeed = 0;

    /**
     * {@link IntakeSubsystem} constructor.
     */
    public IntakeSubsystem() {
        rollerMotor = new SparkMax(Constants.CAN_INTAKE_ROLLER, MotorType.kBrushless);
        armIntakeMotor = new SparkMax(Constants.CAN_INTAKE_EXT, MotorType.kBrushless);
        armIntakeEncoder = new DutyCycleEncoder(Constants.DIO_INTAKE_ABS);
        armPIDController = new PIDController(arm_kP, arm_kI, arm_kD);
        intakeRateLimiter = new SlewRateLimiter(1);
    }

    /**
     * Creates the MapleSim intake zone on the simulated drivetrain so rollers can
     * pick up fuel. Does nothing outside simulation.
     * @param swerveDrive The swerve drive whose simulated chassis the intake attaches to
     */
    public void setupSimulation(SwerveDrive swerveDrive) {
        if (!RobotBase.isSimulation()) {
            return;
        }
        swerveDrive.getMapleSimDrive().ifPresent(driveSim -> {
            driveSimPose = driveSim::getSimulatedDriveTrainPose;
            simIntake = IntakeSimulation.OverTheBumperIntake(
                "Fuel",
                driveSim,
                Inches.of(Constants.SIM_INTAKE_WIDTH_INCHES),
                Inches.of(Constants.SIM_INTAKE_EXTENSION_INCHES),
                Constants.SIM_INTAKE_SIDE,
                Constants.SIM_HOPPER_CAPACITY);
            // MapleSim counts a fuel once per contact event but removes it once, so a single
            // fuel could fill two hopper slots. Accept each fuel only the first time it is touched.
            var alreadyCounted = java.util.Collections.newSetFromMap(
                new java.util.IdentityHashMap<Object, Boolean>());
            simIntake.setCustomIntakeCondition(alreadyCounted::add);
            simIntake.register();
        });
    }

    /** Empties the simulated hopper. */
    public void clearSimFuel() {
        if (simIntake != null) {
            simIntake.setGamePiecesCount(0);
        }
    }

    /** @return fuel currently held in the simulated hopper, or 0 outside simulation */
    public int getSimFuelCount() {
        return simIntake == null ? 0 : simIntake.getGamePiecesAmount();
    }

    /** Removes one fuel from the simulated hopper (e.g. when launched). */
    public boolean consumeSimFuel() {
        return simIntake != null && simIntake.obtainGamePieceFromIntake();
    }

    @Override
    public void simulationPeriodic() {
        if (simIntake == null) {
            return;
        }
        // The arm encoder isn't simulated, so treat "lowered and rollers pulling in" as deployed.
        boolean deployed = armTarget >= Constants.INTAKE_ARM_LOWERED - 0.05;
        boolean shouldRun = deployed && simRollerSpeed > 0.1;
        if (shouldRun && !simIntake.isRunning()) {
            simIntake.startIntake();
        } else if (!shouldRun && simIntake.isRunning()) {
            simIntake.stopIntake();
        }
        Logger.recordOutput("Intake/SimRunning", simIntake.isRunning());
        Logger.recordOutput("Intake/SimFuelCount", simIntake.getGamePiecesAmount());
        Logger.recordOutput("FieldSimulation/HeldFuel", getHeldFuelPoses());
    }

    /** Lays out held fuel in the hopper; newly picked-up fuel flies in from the intake mouth. */
    private Pose3d[] getHeldFuelPoses() {
        Pose3d robot = new Pose3d(driveSimPose.get());
        int count = simIntake.getGamePiecesAmount();
        double now = Timer.getFPGATimestamp();
        if (pickupTimes.length < count) {
            pickupTimes = java.util.Arrays.copyOf(pickupTimes, count);
        }
        for (int i = lastCount; i < count; i++) {
            pickupTimes[i] = now;
        }
        lastCount = count;

        int perLayer = Constants.SIM_HOPPER_COLUMNS * Constants.SIM_HOPPER_ROWS;
        double spacing = Constants.SIM_FUEL_SPACING_METERS;
        Translation3d mouth = new Translation3d(
            Constants.SIM_INTAKE_MOUTH_X, Constants.SIM_INTAKE_MOUTH_Y, Constants.SIM_INTAKE_MOUTH_Z);
        Pose3d[] poses = new Pose3d[count];
        for (int i = 0; i < count; i++) {
            int layer = i / perLayer;
            int col = (i % perLayer) % Constants.SIM_HOPPER_COLUMNS;
            int row = (i % perLayer) / Constants.SIM_HOPPER_COLUMNS;
            Translation3d slot = new Translation3d(
                Constants.SIM_HOPPER_CENTER_X + (col - (Constants.SIM_HOPPER_COLUMNS - 1) / 2.0) * spacing,
                Constants.SIM_HOPPER_CENTER_Y + (row - (Constants.SIM_HOPPER_ROWS - 1) / 2.0) * spacing,
                Constants.SIM_HOPPER_FLOOR_Z + layer * spacing);
            double t = Math.min(1.0, (now - pickupTimes[i]) / Constants.SIM_FLY_IN_SECONDS);
            Translation3d offset = mouth.interpolate(slot, t);
            // Small arc so the fuel appears to be lifted by the roller
            offset = offset.plus(new Translation3d(0, 0, 0.15 * Math.sin(Math.PI * t)));
            poses[i] = robot.transformBy(new Transform3d(offset, new Rotation3d()));
        }
        return poses;
    }

    /**
     * Resets the PID controllers for both launch speed and turret angle.
     * Should be called when starting a new aiming/launching action to prevent
     * integral windup and ensure accurate control from the start.
     */
    public void resetPID() {
        armPIDController.setPID(
            SmartDashboard.getNumber("Intake/arm_kP", arm_kP),
            SmartDashboard.getNumber("Intake/arm_kI", arm_kI),
            SmartDashboard.getNumber("Intake/arm_kD", arm_kD)
        );
        armPIDController.reset();
    }

    /**
     * Manually moves the intake without PID control
     * @param speed The speed to set the intake roller motor to, from -1.0 to 1.0
     */
    public void setIntakeSpeed(double speed) {
        double rollerSpeed = intakeRateLimiter.calculate(speed);
        simRollerSpeed = rollerSpeed;
        rollerMotor.set(rollerSpeed);
    }

    /**
     * Returns the current drawn by the intake roller motor
     * @return The current drawn by the intake roller motor
     */
    public double getIntakeRollerCurrent() {
        double current = rollerMotor.getOutputCurrent();
        if(SmartDashboard.getBoolean("TESTMODE", false)){
            if(current >= 25){
                SmartDashboard.putBoolean("Intake/Overcurrent Roller", false);
            }
        }
        return current;
    }


    /**
     * Returns the current position of the intake arm in encoder units
     * @return The current position of the intake arm
     */
    public double getIntakeArmPosition() {
        // return armIntakeMotor.getEncoder().getPosition();
        return armIntakeEncoder.get();
    }

    /**
     * Returns the most recent commanded arm target position.
     * Useful in simulation because the absolute encoder is not simulated.
     */
    public double getIntakeArmTarget() {
        return armTarget;
    }

    public boolean armAtTarget(double target){
        if(Math.abs(getIntakeArmPosition() - target) < 0.1){
            return true;
        }
        return false;
    }

    /**
     * Manually moves the intake arm without PID control
     * @param speed The speed to set the intake arm motor to, from -1.0 to 1.0
     */
    public void setIntakeArmSpeed(double speed) {
        armIntakeMotor.set(speed);
    }

    /**
     * Moves the intake arm to the target position using PID control
     * @param targetPosition The target position for the intake arm
     */
    public void setIntakeArmPosition(double targetPosition) {
        armTarget = targetPosition;
        double output = armPIDController.calculate(getIntakeArmPosition(), targetPosition);
        setIntakeArmSpeed(output);
    }
    // Gets Roller Temp
     public double getRollerTemp() {
        return rollerMotor.getMotorTemperature();
    }
    // Gets Intake Temp
     public double getIntakeTemp() {
        return armIntakeMotor.getMotorTemperature();
    }
}
