
# Climber Subsystem

* The system that controls the robots climbing mechanism allowing for us to score hanging points in auton and teleop.

## Coding

```java
// Example instantiation within RobotContainer.java
private final MySubsystem ClimberSubsystem = new ClimberSubsystem();
```

### Tele-op control

* By Pressing (Insert Button here) the hanger will extend out and by clicking (Insert Button here) The hanger will retract.

### Autonomous control

* Will be used through commands in pathplanner to extend and then retract the hanger.

## Electrical

### Feedback

* Absolute Encoder to see position

### Interface types

Table: Climber CAN IDs

| CAN ID | Function                |
|--------|-------------------------|
|23      |Climber Left Motor       |
|24      |Climber Right Motor      |

## Mechanical

* Initial source material source
* [link to the source](https://example.com/) if available

### CAD Models

* Picture(s) of the CAD Model, **NOT** from a phone camera
* Label parts of the model.

<!-- pagebreak -->
