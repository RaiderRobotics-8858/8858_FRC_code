# Elevator Subsystem

* The Elevator Subsystem raises up to 4 different levels. Using motors and encoders to figure out its position.

## Coding

* Include example definition of the subsystem's class:

```java
// Example instantiation within RobotContainer.java
private final ElevatorSubsystem elevatorSubsystem = new ElevatorSubsystem();
```

### Tele-op control

* A Player Operator can click a button for each level that the elevator can go to.
* Buttons L1 L2 L3 L4

### Autonomous control

* Can be accessed using pathplanner to allow for it to go to each height

## Electrical

### Feedback

* Sensor Types: Encoders to check the positions. Spark Max's to allow for the elevator to move.

### Interface types

* CAN ID 15
* Using CANBUS to control it through the roborio
* Wiring Diagram here.

## Mechanical

  
* [Penn State Elevator Design](https://www.youtube.com/watch?v=OwnkyOrcR3U)

### CAD Models

* This is a picture of a CAD Model
