# Coral Intake

* Intakes, holds, and releases coral to score on the reef in both teleop and auton. Less precision required for this subsystem. 

## Coding

* Include example definition of the subsystem's class:

```java
// Example instantiation within RobotContainer.java
private final CoralIntakeSubsystem coralSubsystem = new CoralIntakeSubsystem();
```

### Tele-op control

* Driver Input through a button box

### Autonomous control

* Two limit switches
* Path Planner

## Electrical

* Schematic Here
* CAN ID 21

### Feedback

* Sensor Types and their use-cases
* Limit switches detect how far in or out ocral is in the intake

### Interface types

* CAN Bus
* CAN ID 21 
* Wiring Diagram showing both data *and* power connections
* Pretend this is an image of the schematic :3 
* Top limit sqitch is Digital input 1 
* Botton switch digital input 2

## Mechanical

* Inspired by [Team 6201 The Highlanders](https://www.thebluealliance.com/team/6201)

### CAD Models

![Coral Intake (front)](images/Coral%20Intake%203D%20view.png)

![Coral Intake (rear)](images/Coral%20Intake%203D%20rear%20view.png)
