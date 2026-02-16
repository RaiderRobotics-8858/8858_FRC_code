# Summary

FRC Team [8858 Beast From the East's](https://www.thebluealliance.com/team/8858) 2025 robot is named ['Doris'](https://en.wikipedia.org/wiki/Doris_(Oceanid)). Initial inspiration of this robot came from the [Penn State RI3D](https://www.youtube.com/watch?v=OwnkyOrcR3U) design with modifications made as the season went on.

## Capabilities

[(Reveal Video)](https://www.youtube.com/watch?v=EAzFxX81t_A)

* Score coral at **all levels** of the reef
* Intake Coral from feeder station
* Remove **high and low** algae
* Score algae in **processor**
* Hang from **deep barge** in endgame

![3D Render of Doris](images/Doris%203D%20View.png)

![Front view of Doris](images/Doris%20Front%20View.png)

![Side view of Doris](images/Doris%20Side%20View.png)
<!-- pagebreak -->

## Button Mapping

Control of this robot requires 2 operators:

1. Main Driver with Xbox Controller responsible for moving around field and Aglae.
2. Operator responsible for fine movement near reef while scoring coral.

<!-- table: Main Driver Xbox Controller Mapping -->
| Button Name       | Press or Hold | Functionality                                                         |
|-------------------|---------------|-----------------------------------------------------------------------|
| Left Stick        | N/A           | Translation Movement (*ie forward/backward, strafing*)                |
| Right Stick       | N/A           | Rotational Movement (*ie spinning*)                                   |
| A                 | **Press**     | Algae Intake (starts intake, automatically stops when algae detected) |
| B                 | **Hold**      | Climber Ascend (climber down, robot up)                               |
| X                 | N/A           | None                                                                  |
| Y                 | N/A           | None                                                                  |
| Left Bumper       | **Press**     | Coral Intake (Lowers Elevator, enable coral auto-intake mode)         |
| Right Bumper      | **Press**     | Score Algae (runs Algae motor for 1 second)                           |
| Left Trigger      | **Press**     | Low Algae (raises elevator only)                                      |
| Right Trigger     | **Press**     | High Algae (raises elevator only)                                     |
| Back              | N/A           | None                                                                  |
| Start             | **Press**     | lower elevator, stop motors, resets odometry                          |
| Left Stick Press  | N/A           | None                                                                  |
| Right Stick Press | N/A           | None                                                                  |
| D-Pad Up          | N/A           | None                                                                  |
| D-Pad Down        | **Press**     | *Experimental* Auto-Score Algae                                       |
| D-Pad Left        | **Press**     | *Experimental* Auto-Score Coral method 1                              |
| D-Pad Right       | **Press**     | *Experimental* Auto-Score Coral method 2                              |
<!-- pagebreak -->
<!-- table: Operator Button Box Mapping -->
| Button # | Button Box Lable | Press or Hold | Functionality                                                           |
|----------|------------------|---------------|-------------------------------------------------------------------------|
| 1        | Coral Out        | **Press**     | Score Coral (runs 0.5s after last limit switch released)                |
| 2        | Coral In         | **Hold**      | Coral Intake (normally unused)                                          |
| 3        | L1               | **Press**     | Level 1 Coral prep (raises elevator)                                    |
| 4        | L2               | **Press**     | Level 2 Coral prep (raises elevator)                                    |
| 5        | L3               | **Press**     | Level 3 Coral prep (raises elevator)                                    |
| 6        | L4               | **Press**     | Level 4 Coral prep (raises elevator)                                    |
| 7        | Switch Camera    | **Hold**      | Enable Climb prep (switch camera, slow drive speed relative to camera)  |
| 8        |                  |               | N/A                                                                     |
| 9        | Drive Left       | **Hold**      | Drive Left relative to Camera                                           |
| 10       | Drive Right      | **Hold**      | Drive Right relative to Camera                                          |
<!-- pagebreak -->
