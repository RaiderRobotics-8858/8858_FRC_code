# Elevator Subsystem

The Elevator Subsystem's purpose is to lift the Algae and Coral intakes and hold at steady heights so that

## Coding

```java
// Example instantiation within RobotContainer.java
private final ElevatorSubsystem elevatorSubsystem = new ElevatorSubsystem();
```

### Tele-op control

* In Tele-Op mode, the Elevator will be controlled by setting a target position
* Code snippit of how go-to-position commands are called in `RobotContainer.java`:

```java
controller.button(0).onTrue(new ParallelCommandGroup(
        new SequentialCommandGroup(
            new MoveElevatorToPositionAuto(
                elevatorSubsystem,
                20
            ),
            new ParallelCommandGroup(
                Commands.run(
                    () -> elevatorSubsystem.MoveElevatorToPosition(20)
                )
            )
        )
    )
);
```

### Autonomous control

* Autonomous will re-use the functions designed for Tele-Op to
* set target positions corresponding to the motor's encoder value
* use embedded PID controller with encoder's value used as feedback

## Electrical

### Feedback

* Using Motor's encoder, this should provide an accurate estimation of the elevator's height based on how many rotations the motor has made.

### Interface types

* Controlled via CAN bus through a [REV Robotics SPARK MAX](https://www.revrobotics.com/rev-11-2158)
* CAN ID: **15**

## Mechanical

* Initial source material source
* [link to the source](https://example.com/) if available

### CAD Models

* Picture(s) of the CAD Model, **NOT** from a phone camera
* Label parts of the model.
