// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.util.Units;
import swervelib.math.Matter;

/**
 * The Constants class provides a convenient place for teams to hold robot-wide
 * numerical or boolean constants. This
 * class should not be used for any other purpose. All constants should be
 * declared globally (i.e. public static). Do
 * not put anything functional in this class.
 *
 * <p>
 * It is advised to statically import this class (or one of its inner classes)
 * wherever the
 * constants are needed, to reduce verbosity.
 */
public final class Constants {

    public static final double ROBOT_MASS = (148 - 20.3) * 0.453592; // 32lbs * kg per pound
    public static final Matter CHASSIS = new Matter(new Translation3d(0, 0, Units.inchesToMeters(8)), ROBOT_MASS);
    public static final double LOOP_TIME = 0.13; // s, 20ms + 110ms sprk max velocity lag
    public static final double MAX_SPEED = Units.feetToMeters(14.5);

    // Path Constraints
    public static final double MAX_DRIVE_VELO = 1.0;
    public static final double MAX_DRIVE_ACCEL = 1.0;
    public static final double MAX_ANGLE_VELO = Units.degreesToRadians(90);
    public static final double MAX_ANGLE_ACCEL = Units.degreesToRadians(360);

    // Positions for coral elevator height for levels 1-4
    public static final double ELE_L1 = (0.0);
    public static final double ELE_L2 = (20.205);
    public static final double ELE_L3 = (48.607);
    public static final double ELE_L4 = (93.0);
    public static final double ELE_ALGLO = (44.381);
    public static final double ELE_ALGHI = (74.64);
    public static final double ELE_TOL = (0.35);

    // Positions for coral wrist angle for levels 1-4
    public static final double WR_L1 = (0.667);
    public static final double WR_L2 = (0.66);
    public static final double WR_L3 = (0.66);
    public static final double WR_L4 = (0.691);

    // Speed for Algae and Coral intake Motors
    public static final double ALG_M_SPEED = (0.4);
    public static final double ALG_M_HOLD_SPEED = (0.05);
    public static final double COR_M_SPEED = (0.5);
    public static final double COR_M_PRESCORE_SPEED = (0.2);
    // public static final double ALG_M_MIN_SPEED = (-0.5);

    // Positions for Wrist and elevator for coral intake
    public static final double ELE_COR_IN = (0.0);
    public static final double WR_COR_IN = (0.6);

    // Climber speed stuff
    public static final double CL_M_SPEED = (0.1);

    // PID coefficients for driving (taken from pidfproperties.json)
    public static final double DRIVE_KP = 0.00023;
    public static final double DRIVE_KI = 0.0000015;
    public static final double DRIVE_KD = 1;

    // PID coefficients for turning (taken from pidfproperties.json)
    public static double ANGLE_KP = 0.01;
    public static double ANGLE_KI = 0.000038975;
    public static double ANGLE_KD = 1;

    // Maximum speed of the robot in meters per second, used to limit acceleration.

    // public static final class AutonConstants
    // {
    //
    // public static final PIDConstants TRANSLATION_PID = new PIDConstants(0.7, 0,
    // 0);
    // public static final PIDConstants ANGLE_PID = new PIDConstants(0.4, 0, 0.01);
    // }

    public static final class DrivebaseConstants {

        // Hold time on motor brakes when disabled
        public static final double WHEEL_LOCK_TIME = 10; // seconds
    }

    public static class OperatorConstants {

        // Joystick Deadband
        public static final double DEADBAND = 0.1;
        public static final double LEFT_Y_DEADBAND = 0.1;
        public static final double RIGHT_X_DEADBAND = 0.1;
        public static final double TURN_CONSTANT = 6;

        // CAN IDs
        public static final int CAN_FL_ANGLE = 1;
        public static final int CAN_FL_DRIVE = 2;
        public static final int CAN_FR_ANGLE = 3;
        public static final int CAN_FR_DRIVE = 4;
        public static final int CAN_BL_DRIVE = 5;
        public static final int CAN_BL_ANGLE = 6;
        public static final int CAN_BR_DRIVE = 7;
        public static final int CAN_BR_ANGLE = 8;
        public static final int CAN_LEFT_CLIMB_MOTOR = 9;
        public static final int CAN_RIGHT_CLIMB_MOTOR = 10;
        public static final int CAN_BL_CANCODER = 11;
        public static final int CAN_BR_CANCODER = 12;
        public static final int CAN_FR_CANCODER = 13;
        public static final int CAN_FL_CANCODER = 14;
        public static final int CAN_ELEVATOR_R = 15;
        public static final int CAN_ELE_L = 16; // not in use
        public static final int CAN_dle = 17;
        public static final int CAN_ALGAE_L = 18;
        public static final int CAN_ALGAE_R = 19;
        public static final int CAN_PDP = 20;
        public static final int CAN_CORAL_INTAKE_MOTOR = 21;
        public static final int CAN_WRIST_MOTOR = 22; // not in use

        // PDP channels
        public static final int PDP_ALGAE = 5;

        // Camera
        public static final int CAM_FPS = 10;
        public static final int CAM_CORAL = 1;
        public static final int CAM_CLIMB = 0;

        // Thermocuples
        public static final int TC_FL = 0;
        public static final int TC_FR = 1;
        public static final int TC_BL = 2;
        public static final int TC_BR = 3;

        // Coral
        public static final int LS_TOP = 1;
        public static final int LS_BOT = 2;
    }
}
