package frc.robot;

public final class Constants {

    // CAN ID Assignments
    public static final int CAN_LAUNCH_ANG = 5;
    public static final int CAN_LEFTFRONT_DRIVE = 4;
    public static final int CAN_LEFTBACK_DRIVE = 3;
    public static final int CAN_RIGHTFRONT_DRIVE = 2;
    public static final int CAN_RIGHTBACK_DRIVE = 6;
    public static final int CAN_LEDS = 11;

    // Controller Settings
    public static final double CTLR_X_DEADBAND = 0.1;
    public static final double CTLR_Y_DEADBAND = 0.1;
    public static final int SHOOT_BUTTON = 1;
    public static final int ARM_DOWN_BUTTON = 2;
    public static final int ARM_L1_BUTTON = 5;
    public static final int ARM_L2_BUTTON = 3;
    public static final int ARM_L3_BUTTON = 4;
    public static final int ARM_L4_BUTTON = 6;
    public static final int ARM_MAN_CTRL_BUTTON = 7;

    // Drive Characteristics
    public static final double DRIVE_SPEED_COEFFICIENT = 0.6;
    public static final double TURN_SPEED_COEFFICIENT  = 0.3;
    public static final double DRIVE_ACCEL_COEFFICIENT = 0.6;
    public static final double TURN_ACCEL_COEFFICIENT  = 0.3;

    // PID Constants
    public static final double ARM_kP = 0.1;
    public static final double ARM_kI = 0.0;
    public static final double ARM_Kd = 0.0;

    public static final double armMinOutput = -0.1;
    public static final double armMaxOutput = 0.1;
    
    public static final double L1_Arm_Pos = 2.5;
    public static final double L2_Arm_Pos = 3.0;
    public static final double L3_Arm_Pos = 5.0;
    public static final double L4_Arm_Pos = 6.0;
    public static final double ARM_MAX_POS = 6.0;
    public static final double ARM_STOW_POS = 6.0;
}
