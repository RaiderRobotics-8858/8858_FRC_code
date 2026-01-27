// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import java.util.Optional;

import com.pathplanner.lib.auto.NamedCommands;

import edu.wpi.first.util.datalog.DataLog;
import edu.wpi.first.util.datalog.DoubleLogEntry;

import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.DataLogManager;
// import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import frc.robot.subsystems.swervedrive.AlgaeSubsystem;
import frc.robot.subsystems.swervedrive.ClimberSubsystem;
import frc.robot.subsystems.swervedrive.CoralIntakeSubsystem;
import frc.robot.subsystems.swervedrive.ElevatorSubsystem;
import frc.robot.subsystems.swervedrive.LEDSubsystem;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as
 * described in the TimedRobot documentation. If you change the name of this
 * class or the package after creating this
 * project, you must also update the build.gradle file in the project.
 */
public class Robot extends TimedRobot {

    private static Robot instance;
    private Command m_autonomousCommand;
    private AnalogInput tc_fl;
    private AnalogInput tC_fr;
    private AnalogInput tc_bl;
    private AnalogInput tC_br;
    private DataLog log;
    private DoubleLogEntry odometryXLog, odometryYLog, headingLog;


    private RobotContainer m_robotContainer;
    // private final ElevatorSubsystem elevatorSubsystem = new ElevatorSubsystem();

    private Timer disabledTimer;

    public static XboxController controller = new XboxController(0);

    public Robot() {
        instance = this;
    }

    public static Robot getInstance() {
        return instance;
    }

    /**
     * This function is run when the robot is first started up and should be used
     * for any initialization code.
     */
    @Override
    public void robotInit() {
        // Instantiate our RobotContainer. This will perform all our button bindings,
        // and put our
        // autonomous chooser on the dashboard.
        m_robotContainer = new RobotContainer();
        m_robotContainer.init();

        DataLogManager.start();
        log = DataLogManager.getLog();
        odometryXLog = new DoubleLogEntry(log, "Swerve/Odometry/X");
        odometryYLog = new DoubleLogEntry(log, "Swerve/Odometry/Y");
        headingLog = new DoubleLogEntry(log, "Swerve/Heading");


        // Thermocouples
        tc_fl = new AnalogInput(Constants.OperatorConstants.TC_FL);
        tC_fr = new AnalogInput(Constants.OperatorConstants.TC_FR);
        tc_bl = new AnalogInput(Constants.OperatorConstants.TC_BL);
        tC_br = new AnalogInput(Constants.OperatorConstants.TC_BR);


        // Create a timer to disable motor brake a few seconds after disable. This will
        // let the robot stop
        // immediately when disabled, but then also let it be pushed more
        disabledTimer = new Timer();

        if (isSimulation()) {
            DriverStation.silenceJoystickConnectionWarning(true);
        }
    }

    /**
     * This function is called every 20 ms, no matter the mode. Use this for items
     * like diagnostics that you want ran
     * during disabled, autonomous, teleoperated and test.
     *
     * <p>
     * This runs after the mode specific periodic functions, but before LiveWindow
     * and
     * SmartDashboard integrated updating.
     */
    @Override
    public void robotPeriodic() {
        // Runs the Scheduler. This is responsible for polling buttons, adding
        // newly-scheduled
        // commands, running already-scheduled commands, removing finished or
        // interrupted commands,
        // and running subsystem periodic() methods. This must be called from the
        // robot's periodic
        // block in order for anything in the Command-based framework to work.
        CommandScheduler.getInstance().run();
        SmartDashboard.putNumber("z_Elevator height", ElevatorSubsystem.elevatorinstance.getEncoderPosition());
        SmartDashboard.putNumber("z_Climber Encoder", ClimberSubsystem.m_instance.getEncoder());
        SmartDashboard.putBoolean("z_Limit Switch Top", CoralIntakeSubsystem.coral_intake_instance.getTop());
        SmartDashboard.putBoolean("z_Limit Switch Bottom", CoralIntakeSubsystem.coral_intake_instance.getBot());
        // SmartDashboard.putNumber("z_Thermocouple 0 (K)", (tc_fl.getVoltage() * 200) + 23.15);
        // SmartDashboard.putNumber("z_Thermocouple 0 (C)", (tc_fl.getVoltage() * 200) - 250);
        SmartDashboard.putNumber("z_Thermocouple fl (F)", (((tc_fl.getVoltage() * 200) - 250) * 1.8) + 32);
        SmartDashboard.putNumber("z_Thermocouple fr (F)", (((tC_fr.getVoltage() * 200) - 250) * 1.8) + 32);
        SmartDashboard.putNumber("z_Thermocouple bl (F)", (((tc_bl.getVoltage() * 200) - 250) * 1.8) + 32);
        SmartDashboard.putNumber("z_Thermocouple br (F)", (((tC_br.getVoltage() * 200) - 250) * 1.8) + 32);
        SmartDashboard.putNumber("Robot X Position", (m_robotContainer.drivebase.getPose().getX()));
        SmartDashboard.putNumber("Robot Y Position", (m_robotContainer.drivebase.getPose().getY()));
        SmartDashboard.putNumber("Algae Intake Current (A)", AlgaeSubsystem.algae_intake_instance.getAlgaeCurrent());

        //Advantage Scope Logging
        odometryXLog.append(m_robotContainer.drivebase.getPose().getX());
        odometryYLog.append(m_robotContainer.drivebase.getPose().getY());
        headingLog.append(m_robotContainer.drivebase.getHeading().getDegrees());

        m_robotContainer.periodic();

    }

    /**
     * This function is called once each time the robot enters Disabled mode.
     */
    @Override
    public void disabledInit() {
        log.flush();
        m_robotContainer.setMotorBrake(true);
        disabledTimer.reset();
        disabledTimer.start();
        LEDSubsystem.getInstance().manualOverride(LEDSubsystem.Mode.BEAM_ALLIANCE);
        LEDSubsystem.getInstance().holdState();
    }

    @Override
    public void disabledPeriodic() {
        if (disabledTimer.hasElapsed(Constants.DrivebaseConstants.WHEEL_LOCK_TIME)) {
            m_robotContainer.setMotorBrake(false);
            disabledTimer.stop();
        }
    }

    /**
     * This autonomous runs the autonomous command selected by your
     * {@link RobotContainer} class.
     */
    @Override
    public void autonomousInit() {
        m_robotContainer.setMotorBrake(true);
        m_autonomousCommand = m_robotContainer.drivebase.getAutonomousCommand(SmartDashboard.getString("Auto Selector", ""));
        // schedule the autonomous command (example)
        if (m_autonomousCommand != null) {
            m_autonomousCommand.schedule();
        }
        LEDSubsystem.getInstance().manualOverride(LEDSubsystem.Mode.RAINBOW);
    }

    /**
     * This function is called periodically during autonomous.
     */
    @Override
    public void autonomousPeriodic() {
    }

    private Timer gameTimer = new Timer();
    @Override
    public void teleopInit() {
        // This makes sure that the autonomous stops running when
        // teleop starts running. If you want the autonomous to
        // continue until interrupted by another command, remove
        // this line or comment it out.
        if (m_autonomousCommand != null) {
            m_autonomousCommand.cancel();
        } else {
            CommandScheduler.getInstance().cancelAll();
        }
        LEDSubsystem.getInstance().manualOverride(LEDSubsystem.Mode.BEAM_ALLIANCE);
        gameTimer.reset();
        gameTimer.start();
    }

    /**
     * This function is called periodically during operator control.
     */
    @Override
    public void teleopPeriodic() {

        // start a 20 second timer at the end of teleop
        if(gameTimer.get() > 115){
            LEDSubsystem.getInstance().startTimer(20);
        }
    }

    @Override
    public void testInit() {
        // Cancels all running commands at the start of test mode.
        CommandScheduler.getInstance().cancelAll();
    }

    /**
     * This function is called periodically during test mode.
     */
    @Override
    public void testPeriodic() {
    }

    /**
     * This function is called once when the robot is first started up.
     */
    @Override
    public void simulationInit() {
    }

    /**
     * This function is called periodically whilst in simulation.
     */
    @Override
    public void simulationPeriodic() {
    }
}
