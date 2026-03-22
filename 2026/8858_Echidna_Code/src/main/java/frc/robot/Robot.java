// Copyright (c) FIRST and other WPILib contributors.

// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import org.littletonrobotics.junction.LogFileUtil;
import org.littletonrobotics.junction.LoggedRobot;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.networktables.NT4Publisher;
import org.littletonrobotics.junction.wpilog.WPILOGReader;
import org.littletonrobotics.junction.wpilog.WPILOGWriter;
import org.littletonrobotics.urcl.URCL;

public class Robot extends LoggedRobot {
  public static enum Mode {
    /** Running on a real robot. */
    REAL,

    /** Running a physics simulator. */
    SIM,

    /** Replaying from a log file. */
    REPLAY
  }

  private Command autonomousCommand;

  public final RobotContainer robotContainer;
  public static final Mode simMode     = Mode.SIM;
  /// Change to Mode.REPLAY to enable REPLAy.
  public static final Mode currentMode = RobotBase.isReal() ? Mode.REAL : simMode;

  public Robot() {
    switch (currentMode) {
      case REAL:
        // Running on a real robot, log to a USB stick ("/U/logs")
        Logger.addDataReceiver(new WPILOGWriter());
        Logger.addDataReceiver(new NT4Publisher());
        break;

      case SIM:
        // Running a physics simulator, log to NT
        Logger.addDataReceiver(new NT4Publisher());
        break;

      case REPLAY:
        // Replaying a log, set up replay source
        setUseTiming(false); // Run as fast as possible
        String logPath = LogFileUtil.findReplayLog();
        Logger.setReplaySource(new WPILOGReader(logPath));
        Logger.addDataReceiver(new WPILOGWriter(LogFileUtil.addPathSuffix(logPath, "_sim")));
        break;
    }

    // Initialize URCL
    Logger.registerURCL(URCL.startExternal());

    // Start AdvantageKit logger
    Logger.start();

    robotContainer = new RobotContainer();

    // Publish SmartDashboard values used for tuning.
    SmartDashboard.putNumber("configlaunch", 0);
    SmartDashboard.putNumber("Launcher/ManualMotorSpeed", 0);
    SmartDashboard.putNumber("Intake/IntakeRoller", 0); 
    SmartDashboard.putNumber("Intake/RollerSpeed", Constants.HOPPER_ROLLER_SPEED);
    SmartDashboard.putBoolean("TESTMODE", false);
    SmartDashboard.putNumber("Launcher/launch_kP", Constants.LAUNCH_KP);
    SmartDashboard.putNumber("Launcher/launch_kI", Constants.LAUNCH_KI);
    SmartDashboard.putNumber("Launcher/launch_kD", Constants.LAUNCH_KD);
    SmartDashboard.putNumber("Launcher/launch_ff", Constants.LAUNCH_FF);
    SmartDashboard.putNumber("Launcher/angle_kP", Constants.TURRET_ANGLE_KP);
    SmartDashboard.putNumber("Launcher/angle_kI", Constants.TURRET_ANGLE_KI);
    SmartDashboard.putNumber("Launcher/angle_kD", Constants.TURRET_ANGLE_KD);
    SmartDashboard.putNumber("Intake/arm_kP", Constants.INTAKE_ARM_KP);
    SmartDashboard.putNumber("Intake/arm_kI", Constants.INTAKE_ARM_KI);
    SmartDashboard.putNumber("Intake/arm_kD", Constants.INTAKE_ARM_KD);
    SmartDashboard.putNumber("Climber/climb_kP", Constants.CLIMBER_KP);
    SmartDashboard.putNumber("Climber/climb_kI", Constants.CLIMBER_KI);
    SmartDashboard.putNumber("Climber/climb_kD", Constants.CLIMBER_KD);
    SmartDashboard.putBoolean("Launcher/TurretZeroedFlag", false);
    SmartDashboard.putNumber("Launcher/AngleAdjust", 0);
  }

  @Override
  public void robotPeriodic() {
    CommandScheduler.getInstance().run();
    robotContainer.periodic();
  }

  @Override
  public void disabledInit() {
  }

  @Override
  public void disabledPeriodic() {
  }

  @Override
  public void disabledExit() {
  }

  @Override
  public void autonomousInit() {
    autonomousCommand = robotContainer.getAutonomousCommand();

    if (autonomousCommand != null) {
      CommandScheduler.getInstance().schedule(autonomousCommand);
    }
  }

  @Override
  public void autonomousPeriodic() {
  }

  @Override
  public void autonomousExit() {
  }

  @Override
  public void teleopInit() {
    if (autonomousCommand != null) {
      autonomousCommand.cancel();
    }
  }

  @Override
  public void teleopPeriodic() {
  }

  @Override
  public void teleopExit() {
  }

  @Override
  public void testInit() {
    CommandScheduler.getInstance().cancelAll();
  }

  @Override
  public void testPeriodic() {
  }

  @Override
  public void testExit() {
  }
}
