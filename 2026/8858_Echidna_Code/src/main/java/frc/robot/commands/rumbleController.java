package frc.robot.commands;

import java.util.function.Supplier;

import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;

public class rumbleController extends Command {
    public enum Pattern {
        LEFT,
        RIGHT,
        DOUBLE_BOTH,
        SWEEP_LEFT_TO_RIGHT,
        SWEEP_RIGHT_TO_LEFT
    }

    private final GenericHID hid;
    private final Supplier<Pattern> patternSupplier;
    private final Timer timer = new Timer();
    private Pattern pattern;

    public rumbleController(GenericHID hid, Supplier<Pattern> patternSupplier) {
        this.hid = hid;
        this.patternSupplier = patternSupplier;
    }

    @Override
    public void initialize() {
        pattern = patternSupplier.get();
        timer.reset();
        timer.start();
    }

    @Override
    public void execute() {
        double elapsed = timer.get();
        switch (pattern) {
            case LEFT:
                applySingleSide(elapsed, RumbleType.kLeftRumble);
                break;
            case RIGHT:
                applySingleSide(elapsed, RumbleType.kRightRumble);
                break;
            case DOUBLE_BOTH:
                applyDoubleBoth(elapsed);
                break;
            case SWEEP_LEFT_TO_RIGHT:
                applySweep(elapsed, true);
                break;
            case SWEEP_RIGHT_TO_LEFT:
                applySweep(elapsed, false);
                break;
            default:
                stopRumble();
                break;
        }
    }

    @Override
    public boolean isFinished() {
        if (pattern == Pattern.DOUBLE_BOTH) {
            return timer.get() >= 0.5;
        }
        if (pattern == Pattern.SWEEP_LEFT_TO_RIGHT || pattern == Pattern.SWEEP_RIGHT_TO_LEFT) {
            return timer.get() >= 0.45;
        }
        return timer.get() >= 0.2;
    }

    @Override
    public void end(boolean interrupted) {
        stopRumble();
    }

    private void applySingleSide(double elapsed, RumbleType rumbleType) {
        if (elapsed < 0.2) {
            hid.setRumble(rumbleType, 1.0);
        } else {
            stopRumble();
        }
    }

    private void applyDoubleBoth(double elapsed) {
        if (elapsed < 0.2) {
            hid.setRumble(RumbleType.kBothRumble, 1.0);
        } else if (elapsed < 0.3) {
            hid.setRumble(RumbleType.kBothRumble, 0.0);
        } else if (elapsed < 0.5) {
            hid.setRumble(RumbleType.kBothRumble, 1.0);
        } else {
            hid.setRumble(RumbleType.kBothRumble, 0.0);
        }
    }

    private void applySweep(double elapsed, boolean leftToRight) {
        if (elapsed < 0.15) {
            hid.setRumble(leftToRight ? RumbleType.kLeftRumble : RumbleType.kRightRumble, 1.0);
            hid.setRumble(leftToRight ? RumbleType.kRightRumble : RumbleType.kLeftRumble, 0.0);
        } else if (elapsed < 0.3) {
            hid.setRumble(RumbleType.kBothRumble, 1.0);
        } else if (elapsed < 0.45) {
            hid.setRumble(leftToRight ? RumbleType.kRightRumble : RumbleType.kLeftRumble, 1.0);
            hid.setRumble(leftToRight ? RumbleType.kLeftRumble : RumbleType.kRightRumble, 0.0);
        } else {
            stopRumble();
        }
    }

    private void stopRumble() {
        hid.setRumble(RumbleType.kLeftRumble, 0.0);
        hid.setRumble(RumbleType.kRightRumble, 0.0);
    }
}
