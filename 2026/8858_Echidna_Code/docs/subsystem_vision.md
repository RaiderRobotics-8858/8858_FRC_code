# Vision Subsystem

In FRC, knowledge of where a robot is on the field is required to automate certain tasks. For example, launching game pieces into a stationary target requires knowing a robot's relative position to that target. For this reason, we utilize a Limelight in order to read [AprilTags](https://docs.wpilib.org/en/stable/docs/software/vision-processing/apriltag/apriltag-intro.html) which provide the information of where on the field a camera is located.

Using the internal process of the Limelight, we provide the location on the robot where our camera is located and the Limelight is able to continuously estimate our robot's position on the field. This information can be used to continuously update our odometry, allowing us to create automations based on field location with a high degree of confidence.

## Coding

Code to setup the limelight is contained within `SwerveSubsystem.java`.

```java
public SwerveSubsystem(File directory) {

    // Other swerve setup code here

    // Example instantiation within SwerveSubsystem.java
    limelight = new Limelight("limelight");
    poseEstimator = limelight.createPoseEstimator(EstimationMode.MEGATAG1);
}
```

Pose estimation is done within the `periodic()` method of `SwerveSubsystem.java`.

### Tele-op control

Limelight will constantly update pose-estimation when an AprilTag is in view. Otherwise, Swerve Odometry will be used to estimate the robot's position on the field.

### Autonomous control

Limelight is disabled during autonomous as handshaking with PathPlanner is not reliable in our testing. Instead, we will rely on the NavX gyro and encoders for odometry and path following.

## Electrical

### Interface types

* Ethernet
* [http://limelight.local](http://limelight.local)

### CAD Models

![Mount for Limelight on Swerve Module](images/Limelight%20Mount.png)

<!-- pagebreak -->
