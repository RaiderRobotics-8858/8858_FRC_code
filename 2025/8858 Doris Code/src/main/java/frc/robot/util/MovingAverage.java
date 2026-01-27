package frc.robot.util;

import java.util.LinkedList;

public class MovingAverage {
    private final int windowSize;
    private final LinkedList<Double> values = new LinkedList<>();
    private double sum = 0.0;

    public MovingAverage(int size){
        this.windowSize = size;
    }

    public void add(double value){
        values.add(value);
        sum += value;

        if (values.size() > windowSize){
            sum -= values.removeFirst();
        }
    }

    public double getAverage(){
        return values.isEmpty() ? 0.0 : sum / values.size();
    }
}
