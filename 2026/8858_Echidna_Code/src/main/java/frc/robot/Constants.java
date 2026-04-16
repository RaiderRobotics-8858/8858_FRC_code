package frc.robot;

import com.pathplanner.lib.config.RobotConfig;

import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation;
import swervelib.math.Matter;

public class Constants {

    // CAN IDs
    public static final int CAN_FL_ENC = 1;
    public static final int CAN_FL_ANGLE = 2;
    public static final int CAN_FL_DRIVE= 3;
    public static final int CAN_FR_ENC = 4;
    public static final int CAN_FR_ANGLE = 5;
    public static final int CAN_FR_DRIVE= 6;
    public static final int CAN_BL_ENC = 7;
    public static final int CAN_BL_ANGLE = 8;
    public static final int CAN_BL_DRIVE= 9;
    public static final int CAN_BR_ENC = 10;
    public static final int CAN_BR_ANGLE = 11;
    public static final int CAN_BR_DRIVE= 12;
    public static final int CAN_PDH = 13;
    public static final int CAN_INTAKE_ROLLER = 14;
    public static final int CAN_INTAKE_EXT = 15;
    public static final int CAN_CANDLE = 16;
    public static final int CAN_HOPPER_MOTOR = 17;
    public static final int CAN_KICKER_MOTOR = 18;
    public static final int CAN_TURRET_ANGLE = 19;
    public static final int CAN_LAUNCH_LEFT = 20;
    public static final int CAN_LAUNCH_RIGHT = 21;
    public static final int CAN_HOOD_MOTOR = 22;
    public static final int CAN_CLIMBER = 23;

    // Digital I/O
    public static final int DIO_INTAKE_ABS = 0;
    public static final int DIO_TURRET_OUTPUT = 1;
    public static final int DIO_TURRET_RING = 2;
    public static final int DIO_HOPPER_LEVEL_0 = 3;
    public static final int DIO_HOPPER_LEVEL_1 = 4;
    public static final int DIO_HOPPER_LEVEL_2 = 5;

    public static final double ROBOT_LENGTH = 28;
    public static final double ROBOT_WIDTH = 26;
    public static final double MAX_FUEL = 45;
    
    // Swerve Constants
    public static final double ROBOT_MASS = Units.lbsToKilograms(120); // 32lbs * kg per pound
    public static final Matter CHASSIS = new Matter(new Translation3d(0, 0, Units.inchesToMeters(8)), ROBOT_MASS);
    public static final double LOOP_TIME = 0.13; // s, 20ms + 110ms spark max velocity lag
    public static final double MAX_SPEED = Units.feetToMeters(30);

    // Launch subsystem Constants
    public static final double TURRET_ANGLE_KP = 0.02;
    public static final double TURRET_ANGLE_KI = 0.01;
    public static final double TURRET_ANGLE_KD = 0;
    public static final double HOOD_ANGLE_KP = 0.5;
    public static final double HOOD_ANGLE_KI = 0.3;
    public static final double HOOD_ANGLE_KD = 0;
    public static final double LAUNCH_KP = 0.2;
    public static final double LAUNCH_KI = 0.7;
    public static final double LAUNCH_KD = 0.0;
    public static final double LAUNCH_FF = 0.2;
    public static final double KICKER_SPEED = 1.0; // Speed at which the kicker motor should run when activated
    public static final double LAUNCH_THRESHOLD = 0.5; // Threshold for launch speed
    public static final double TURRET_MAX_SPEED = 0.5; // Max speed the turret can rotate
    public static final double TURRET_ANGLE_THRESHOLD = 5; // Threshold for turret angle (degrees)
    public static final double TURRET_RIGHT_90DEG = -9.95; // Encoder setting to turn turret 90 degrees to the right
    public static final double TURRET_LEFT_90DEG = 9.98; // Encoder setting to turn turret 90 degrees to the left
    public static final double TURRET_UNITS_PER_DEGREE = TURRET_LEFT_90DEG / 90; // Encoder units per degree based on 90 degree setting
    public static final double TURRET_DEGREES_PER_UNIT = 90 / TURRET_LEFT_90DEG; // Degrees per encoder unit based on 90 degree setting
    public static final double TURRET_RIGHT_LIMIT = -12.5; // Right limit for turret angle (encoder units)
    public static final double TURRET_LEFT_LIMIT = 12.5; // Left limit for turret angle (encoder units)
    public static final double TURRET_RIGHT_LIMIT_DEG = TURRET_RIGHT_LIMIT * TURRET_DEGREES_PER_UNIT; // Right limit for turret angle (degrees)
    public static final double TURRET_LEFT_LIMIT_DEG = TURRET_LEFT_LIMIT * TURRET_DEGREES_PER_UNIT; // Left limit for turret angle (degrees)
    public static final double TURRET_CENTER = 0.0; // Center position for turret angle (degrees)
    public static final double HOOD_HIGH_LIMIT = 0.274;
    public static final double HOOD_LOW_LIMIT = 0.800;

    // Intake susbystems constants
    public static final double INTAKE_ARM_KP = 1.2;
    public static final double INTAKE_ARM_KI = 0.1;
    public static final double INTAKE_ARM_KD = 0;
    public static final double INTAKE_ARM_LOWERED = 0.79;
    public static final double INTAKE_ARM_HALF_RAISED = 0.60;
    public static final double INTAKE_ARM_RAISED = 0.40;
    public static final double INTAKE_ROLLER_SPEED = 1.0;
    public static final double HOPPER_ROLLER_SPEED = 1.0;

    // Climber subsystem constants
    public static final double CLIMB_EXTENDED_POS = 65.0;
    public static final double CLIMB_LOWER_POS = 30.0;
    public static final double CLIMBER_KP = 0.2;
    public static final double CLIMBER_KI = 0.6;
    public static final double CLIMBER_KD = 0;


    public static enum AimPoints {
        RED_HUB(new Translation3d(11.938, 4.034536, 1.5748)),
        RED_OUTPOST(new Translation3d(15.75, 7.25, 0)),
        RED_FAR_SIDE(new Translation3d(15.75, 0.75, 0)),

        BLUE_HUB(new Translation3d(4.5974, 4.034536, 1.5748)),
        BLUE_OUTPOST(new Translation3d(0.75, 0.75, 0)),
        BLUE_FAR_SIDE(new Translation3d(0.75, 7.25, 0));

        public final Translation3d value;

        private AimPoints(Translation3d value) {
            this.value = value;
        }

        public static final Translation3d getAllianceHubPosition() {
            return DriverStation.getAlliance().get() == DriverStation.Alliance.Red ? RED_HUB.value : BLUE_HUB.value;
        }

        public static final Translation3d getAllianceOutpostPosition() {
            return DriverStation.getAlliance().get() == DriverStation.Alliance.Red ? RED_OUTPOST.value : BLUE_OUTPOST.value;
        }

        public static final Translation3d getAllianceFarSidePosition() {
            return DriverStation.getAlliance().get() == DriverStation.Alliance.Red ? RED_FAR_SIDE.value : BLUE_FAR_SIDE.value;
        }
    }

    public static final int kDriverControllerPort = 0;
    public static final int kOperatorControllerPort = 1;
    public static final int kPoseControllerPort = 2;

    // Controller Constants
    public static final int XBOX_CONTROLLER_PORT = 0;
    public static final double DEADBAND = 0.1;
    public static final double TRANSLATION_SCALE = 0.5;
    public static final double ROTATION_SCALE = 1.0;

}
