
# LED Subsystem

* Helps alert the driver of a change in the game and adds style.

## Coding

* (if re-using external code) credit the original source
* [link to the source](https://example.com/) if available
* Include example definition of the subsystem's class:

```java
// Example instantiation within RobotContainer.java
private final MySubsystem mySubsystem = new MySubsystem();
```

### Tele-op control

* Timer-based when the drive starts autonomus mode and change with the phases of the game.

### Autonomous control

* Flashes different colors for different points of autonomous mode.

## Electrical

### Feedback

N/A

### Interface types

* Controlled through CANdle
* CAN ID 17
* LED strip is controlled through [WS2812B](https://www.digikey.com/htmldatasheets/production/1829370/0/0/1/ws2812b.pdf)
* Wiring Diagram showing both data *and* power connections

<!-- pagebreak -->
