package frc.robot.util;

import java.util.Optional;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;

// https://docs.wpilib.org/en/stable/docs/yearly-overview/2026-game-data.html
// https://firstfrc.blob.core.windows.net/frc2026/Manual/2026GameManual.pdf
// Game manual section 6.4.1

// shift            time left      duration (s)
// =================== auto ===================
// AUTO             (0:20 - 0:00)  20
// ================== teleop ==================
// TRANSITION SHIFT (2:20 - 2:10)  10
// SHIFT 1          (2:10 - 1:45)  25
// SHIFT 2          (1:45 - 1:20)  25
// SHIFT 3          (1:20 - 0:55)  25
// SHIFT 4          (0:55 - 0:30)  25
// END GAME         (0:30 - 0:00)  30

public class HubTimer {
    /**
     * returns true if your alliance's hub is active
     * 
     * @return
     */
    public static boolean isActive() {
        Optional<Alliance> alliance = DriverStation.getAlliance();
        if (alliance.isEmpty()) // no DS or FMS connected
            return false;
        if (DriverStation.isAutonomousEnabled()) // short-circuit for auton
            return true;

        double matchTime = DriverStation.getMatchTime(); // seconds remaining in match period
        String gameData = DriverStation.getGameSpecificMessage(); // "R", "B" or ""
        Alliance firstShift = Alliance.Blue; // which alliance is active in shift 1

        if (gameData.isEmpty()) {
            return false;
        } else if (gameData.charAt(0) == 'R') { // red scored more, blue goes first
            firstShift = Alliance.Blue;
        } else if (gameData.charAt(0) == 'B') { // blue scored more, red goes first
            firstShift = Alliance.Red;
        } else {
            return true;
        }
        if (matchTime == -1)
            return true;
        
        if (matchTime > time(2, 10)) // transition shift
            return true;
        else if (matchTime > time(1, 45)) // shift 1
            return alliance.get() == firstShift;
        else if (matchTime > time(1, 20)) // shift 2
            return alliance.get() != firstShift;
        else if (matchTime > time(0, 55)) // shift 3
            return alliance.get() == firstShift;
        else if (matchTime > time(0, 30)) // shift 4
            return alliance.get() != firstShift;
        else // end game
            return true;
    }

    /** helper method for readability */
    private static int time(int min, int sec) {
        return sec + (60 * min);
    }
}
