// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.SparkLowLevel.MotorType;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PneumaticsModuleType;
import edu.wpi.first.wpilibj.Servo;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.PWM;
import edu.wpi.first.wpilibj.PneumaticHub;

// Phoenix 6 is in the com.ctre.phoenix6.* packages
import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.sim.TalonFXSimState;
import com.ctre.phoenix.led.CANdle;
import com.ctre.phoenix.led.RainbowAnimation;
import com.ctre.phoenix.led.RgbFadeAnimation;
import com.ctre.phoenix.led.TwinkleAnimation;
import com.ctre.phoenix.led.LarsonAnimation.BounceMode;
import com.ctre.phoenix.led.TwinkleAnimation.TwinklePercent;
import com.ctre.phoenix.led.FireAnimation;
import com.ctre.phoenix.led.LarsonAnimation;
import com.ctre.phoenix.led.ColorFlowAnimation;

// All hardware classes already have WPILib integration
// final TalonFX m_talonFX = new TalonFX(0);
// final CANcoder m_cancoder = new CANcoder(0);

// final TalonFXSimState m_talonFXSim = m_talonFX.getSimState();

// final DutyCycleOut m_talonFXOut = new DutyCycleOut(0);

// final TalonFXConfiguration m_talonFXConfig = new TalonFXConfiguration();
// final CANcoderConfiguration m_cancoderConfig = new CANcoderConfiguration();

// InvertedValue m_talonFXInverted = InvertedValue.CounterClockwise_Positive;

// m_talonFX.setControl(m_talonFXOut);/* */

//import edu.wpi.first.wpilibj.XboxController;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to
 * each mode, as described in the TimedRobot documentation. If you change the
 * name of this class or
 * the package after creating this project, you must also update the
 * build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {

    public SparkMax leftFrontMotor = new SparkMax(Constants.CAN_LEFTFRONT_DRIVE, MotorType.kBrushed);
    public SparkMax leftBackMotor = new SparkMax(Constants.CAN_LEFTBACK_DRIVE, MotorType.kBrushed);
    public SparkMax rightFrontMotor = new SparkMax(Constants.CAN_RIGHTFRONT_DRIVE, MotorType.kBrushed);
    public SparkMax rightBackMotor = new SparkMax(Constants.CAN_RIGHTBACK_DRIVE, MotorType.kBrushed);
    public SparkMax armMotor = new SparkMax(Constants.CAN_LAUNCH_ANG, MotorType.kBrushless);
    public Boolean armMotionRequested = false;
    public double targetPosition = 0;
    public double armtargetPosition = 0.0;
    public double targetSpeed = 0;
    public static Servo shooter = new Servo(1);
    public static final double MANUALARMSPEED = 0.1;
    public static final int MANUALDOWNBUTTON = 3;
    public static final int MANUALUPBUTTON = 5;
    public static final double AUTOARMSPEED = 0.05;
    public static final int AUTODOWNBUTTON = 4;
    public static final int AUTOUPBUTTON = 6;
    public static final double ENCODERTODEGREES = 15.0;
    public static final double ENCODERTOLERANCE = 1.0 / ENCODERTODEGREES;
    public static final long SHORTSLEEP = 50;
    // public static final double NOTLAUNCH = 0;
    public static final double LAUNCH = 30;
    public Compressor compressor = new Compressor(PneumaticsModuleType.REVPH);
    public PWM lights = new PWM(2);
    public Solenoid m_Solenoid = new Solenoid(PneumaticsModuleType.REVPH, 1);
    public Boolean solenoid = true;
    public Joystick joystick = new Joystick(0);
    public static double driveLimiter = 0.6;
    final TalonFX m_talonFX = new TalonFX(0);
    final CANcoder m_cancoder = new CANcoder(0);
    final PneumaticHub PH = new PneumaticHub();

    final TalonFXSimState m_talonFXSim = m_talonFX.getSimState();

    final TalonFXConfiguration m_talonFXConfig = new TalonFXConfiguration();
    final CANcoderConfiguration m_cancoderConfig = new CANcoderConfiguration();

    InvertedValue m_talonFXInverted = InvertedValue.CounterClockwise_Positive;

    private final PIDController arm_pidController = new PIDController(Constants.ARM_kP, Constants.ARM_kI, Constants.ARM_Kd);
    private final SlewRateLimiter driveSlewRateLimiter = new SlewRateLimiter(Constants.DRIVE_ACCEL_COEFFICIENT);
    private final SlewRateLimiter turnSlewRateLimiter = new SlewRateLimiter(Constants.TURN_ACCEL_COEFFICIENT);

    public CANdle candle = new CANdle(Constants.CAN_LEDS);
    private RainbowAnimation rainbow_anim = new RainbowAnimation(0.2, 0.5, 8);
    private FireAnimation fire_anim = new FireAnimation(0.2, .25, 8, 0.7, 0.1);
    private RgbFadeAnimation fde_anim = new RgbFadeAnimation(.2, 1, 105);
    private TwinkleAnimation twinkle_anim = new TwinkleAnimation(0, 0, 255, 0, 1, 105, TwinklePercent.Percent18);
    private LarsonAnimation larson_anim = new LarsonAnimation(50, 150, 25, 0, .2, 105, BounceMode.Back, 6);
    private LarsonAnimation larson_anim_blue = new LarsonAnimation(0, 255, 0, 0, .5, 105, BounceMode.Back, 6);
    private LarsonAnimation larson_anim2 = new LarsonAnimation(0, 0, 255, 0, .25, 10, BounceMode.Back, 6);
    private ColorFlowAnimation flow_anim = new ColorFlowAnimation(50, 150, 25);
    private boolean manual_arm_ctrl = false;
    double leftMotorSpeed  = 0.0;
    double rightMotorSpeed = 0.0;
    double armSpeed = 0.0;
    double drive_input;
    double turn_input;

    /**
     * This function is run when the robot is first started up and should be used
     * for any
     * initialization code.
     */
    @Override
    public void robotInit() {
        // compressor.enableDigital();
        compressor.enableAnalog(80, 100);
        // candle.animate(anim);
    }

    /**
     * This function is called every 20 ms, no matter the mode. Use this for items
     * like diagnostics
     * that you want ran during disabled, autonomous, teleoperated and test.
     *
     * <p>
     * This runs after the mode specific periodic functions, but before LiveWindow
     * and
     * SmartDashboard integrated updating.
     */
    @Override
    public void robotPeriodic() {
        // 0.80 = Red
        // 0.83 = Orange
        // 0.87 = Yellow
        // 0.90 = Green
        // 0.93 = Blue
        // 0.95 = Purple
        // Red = 255, 0, 0
        // Orange = 200, 10, 0
        // Yellow = 75, 50, 0
        // Green = 0, 255, 0
        // Sky Blue = 0, 255, 255
        // Blue = 0, 0, 255
        // Purple = 200, 0, 255
        // Pink = 255, 80, 60
        lights.setPosition(0.80);
        double armPosition = armMotor.getEncoder().getPosition();
        double shooterPosition = shooter.getPosition();
        SmartDashboard.putNumber("Arm value", armPosition);
        SmartDashboard.putNumber("Shooter Value", shooterPosition);
        SmartDashboard.putNumber("i_RightFront (A)", rightFrontMotor.getOutputCurrent());
        SmartDashboard.putNumber("i_RightBack (A)", rightBackMotor.getOutputCurrent());
        SmartDashboard.putNumber("i_LeftFront (A)", leftFrontMotor.getOutputCurrent());
        SmartDashboard.putNumber("i_LeftBack (A)", leftBackMotor.getOutputCurrent());
    }

    public void notLaunch() throws InterruptedException {
        Thread.sleep(500);
    }

    public void launch() throws InterruptedException {
        shooter.setPosition(20);
        Thread.sleep(250);
        shooter.setPosition(0);
        candle.setLEDs(0, 0, 255, 0, 0, 8);
        candle.setLEDs(0, 255, 0, 0, 8, 97);
    }

    public void stop() throws InterruptedException {
        Thread.sleep(1000);
        candle.setLEDs(0, 0, 0, 0, 0, 105);
    }

    public void charge() throws InterruptedException {
        candle.setLEDs(255, 0, 0, 0, 104, 2);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 101, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 98, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 95, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 92, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 89, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 86, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 83, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 80, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 77, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 74, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 71, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 68, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 65, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 62, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 59, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 56, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 53, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 50, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 47, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 44, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 41, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 38, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 35, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 32, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 29, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 26, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 23, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 20, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 17, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 14, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 11, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 8, 3);
        Thread.sleep(SHORTSLEEP);
        candle.setLEDs(255, 0, 0, 0, 0, 1);
        candle.setLEDs(255, 0, 0, 0, 7, 1);
        Thread.sleep(500);
        candle.setLEDs(255, 0, 0, 0, 1, 1);
        candle.setLEDs(255, 0, 0, 0, 6, 1);
        Thread.sleep(500);
        candle.setLEDs(255, 0, 0, 0, 2, 1);
        candle.setLEDs(255, 0, 0, 0, 5, 1);
        Thread.sleep(500);
        candle.setLEDs(255, 0, 0, 0, 3, 1);
        candle.setLEDs(255, 0, 0, 0, 4, 1);
        Thread.sleep(500);
        launch();
    }

    /**
     * This autonomous (along with the chooser code above) shows how to select
     * between different
     * autonomous modes using the dashboard. The sendable chooser code works with
     * the Java
     * SmartDashboard. If you prefer the LabVIEW Dashboard, remove all of the
     * chooser code and
     * uncomment the getString line to get the auto name from the text box below the
     * Gyro
     *
     * <p>
     * You can add additional auto modes by adding additional comparisons to the
     * switch structure
     * below with additional strings. If using the SendableChooser make sure to add
     * them to the
     * chooser code above as well.
     */
    @Override
    public void autonomousInit() {
        // m_autoSelected = SmartDashboard.getString("Auto Selector", kDefaultAuto);
    }

    /** This function is called periodically during autonomous. */
    @Override
    public void autonomousPeriodic() {
    }

    /** This function is called once when teleop is enabled. */
    @Override
    public void teleopInit() {
        candle.clearAnimation(0);
    }

    /** This function is called periodically during operator control. */
    @Override
    public void teleopPeriodic() {

        if (joystick.getRawButtonPressed(Constants.ARM_DOWN_BUTTON)) {
            armtargetPosition = 0.0;
            candle.setLEDs(0, 0, 0, 0, 0, 107);
            arm_pidController.reset();
        }

        if (joystick.getRawButtonPressed(Constants.ARM_L1_BUTTON)) {
            armtargetPosition = Constants.L1_Arm_Pos;
            candle.setLEDs(0, 0, 255, 0, 0, 107);
            arm_pidController.reset();
        }

        if (joystick.getRawButtonPressed(Constants.ARM_L2_BUTTON)) {
            armtargetPosition = Constants.L2_Arm_Pos;
            candle.setLEDs(0, 255, 255, 0, 0, 107);
            arm_pidController.reset();
        }

        if (joystick.getRawButtonPressed(Constants.ARM_L3_BUTTON)) {
            armtargetPosition = Constants.L3_Arm_Pos;
            candle.setLEDs(255, 0, 255, 0, 0, 107);
            arm_pidController.reset();
        }

        if (joystick.getRawButtonPressed(Constants.ARM_L4_BUTTON)) {
            armtargetPosition = Constants.L4_Arm_Pos;
            candle.setLEDs(0, 255, 0, 0, 0, 107);
            arm_pidController.reset();
        }

        // toggle Manual arm control mode.
        if (joystick.getRawButtonPressed(Constants.ARM_MAN_CTRL_BUTTON)){
            manual_arm_ctrl = !manual_arm_ctrl;
        }

        if (joystick.getRawButton(Constants.SHOOT_BUTTON)) {
            try {
                charge();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (joystick.getRawButton(12)) {
          compressor.enableDigital();
          compressor.enableAnalog(80, 100);
        }
        else{
          compressor.enableDigital();
          // compressor.enableAnalog(80, 100);
        }

        if (joystick.getRawButton(7)) {
            candle.setLEDs(0, 0, 0, 0, 0, 107);
        }

        drive_input = joystick.getRawAxis(1) * Constants.DRIVE_SPEED_COEFFICIENT;
        turn_input = joystick.getRawAxis(2) * Constants.TURN_SPEED_COEFFICIENT;

        // apply deadbands to the controller (threshold before a non-zero input is passed to the motors)
        if(Math.abs(drive_input) < Constants.CTLR_Y_DEADBAND){
            drive_input = 0.0;
        }
        if(Math.abs(turn_input) < Constants.CTLR_X_DEADBAND){
            turn_input = 0.0;
        }

        // manage the acceleration rate
        drive_input = driveSlewRateLimiter.calculate(drive_input);
        turn_input = turnSlewRateLimiter.calculate(turn_input);

        // find the values to be written to the motors
        leftMotorSpeed = (drive_input - turn_input);
        rightMotorSpeed = (-drive_input - turn_input);
        leftBackMotor.set(leftMotorSpeed);
        leftFrontMotor.set(leftMotorSpeed);
        rightBackMotor.set(rightMotorSpeed);
        rightFrontMotor.set(rightMotorSpeed);

        if(manual_arm_ctrl){
            armtargetPosition = (Constants.ARM_MAX_POS) * (-joystick.getRawAxis(3) + 1.0) / (2.0);
        }

        armSpeed = arm_pidController.calculate(armMotor.getEncoder().getPosition(), armtargetPosition);
        armSpeed = Math.max(Constants.armMinOutput, Math.min(Constants.armMaxOutput, armSpeed));
        // avoid driving arm motor into the ground
        if((armtargetPosition < 0.2) && (armMotor.getEncoder().getPosition() < 0.2)) armSpeed = 0.0;
        armMotor.set(armSpeed);
    }

    /** This function is called once when the robot is disabled. */
    @Override
    public void disabledInit() {
    }

    /** This function is called periodically when disabled. */
    @Override
    public void disabledPeriodic() {
        candle.animate(larson_anim_blue);
    }

    /** This function is called once when test mode is enabled. */
    @Override
    public void testInit() {
    }

    /** This function is called periodically during test mode. */
    @Override
    public void testPeriodic() {
    }

    /** This function is called once when the robot is first started up. */
    @Override
    public void simulationInit() {
    }

    /** This function is called periodically whilst in simulation. */
    @Override
    public void simulationPeriodic() {
    }
}
