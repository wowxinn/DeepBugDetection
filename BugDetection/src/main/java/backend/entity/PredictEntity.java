package backend.entity;

import java.util.ArrayList;

public class PredictEntity {

    ArrayList<Double> predictValues;

    ArrayList<Integer> predictIndices;

    public PredictEntity() {
    }

    public ArrayList<Double> getPredictValues() {
        return predictValues;
    }

    public void setPredictValues(ArrayList<Double> predictValues) {
        this.predictValues = predictValues;
    }

    public ArrayList<Integer> getPredictIndices() {
        return predictIndices;
    }

    public void setPredictIndices(ArrayList<Integer> predictIndices) {
        this.predictIndices = predictIndices;
    }
}
