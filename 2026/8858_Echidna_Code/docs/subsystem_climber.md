
# Climber Subsystem

The system that controls the robots climbing mechanism allowing for us to score hanging points in auton and teleop. This system allows for only a L1 climb, which is not enough to earn a climbing ranking point, but is a very quick method to score points in endgame.

## Coding

Example instantiation:

```java
// within RobotContainer.java
private final ClimberSubsystem climberSubsystem = new ClimberSubsystem();
```

Useful functions within `ClimberSubsystem.java`:

```java
/**
 * Moves the Climber to a target position using PID control
 * @param target The target position for the Climber in encoder units
 */
public void setClimberPos(double target){
    double output = climberpid.calculate(getClimberPos(),target);
    setClimberSpeed(output);
}

/**
 * Manually moves the Climber without PID control
 * @param speed The speed to set the Climber motor to, from -1.0 to 1.0
 */
public  void setClimberSpeed(double speed) {
    ClimberMotor.set(speed);
}
```

### Tele-op control

* pressing the right bumper, the hanger will extend to full length
* releasing the right bumper, the hanger will retract to a point where the robot is off of the ground.
* pressing and releasing the left bumper will retract the hanger to the fully retracted position.

### Autonomous control

* Will be used through commands in pathplanner to extend and then retract the hanger.
* Due to vulnerability during climbing, commands should have slight pauses to allow for driver to A-stop if needed.

## Electrical

### Feedback

* Motor Encoder for position feedback relative to fully retracted position.

### Interface types

Table: Climber CAN IDs

| CAN ID | Function                |
|--------|-------------------------|
|23      |Climber Motor            |

## Mechanical

Climber repurposed from 2024 Robot "[Cerberus](https://www.youtube.com/watch?v=_fLz5JAc2zU)".

## Cad Models

![Isometric of Echidna with Climber Raised](images/Echidna%20Climber%20Extended.png)

<!-- pagebreak -->
