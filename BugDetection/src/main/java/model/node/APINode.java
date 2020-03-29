package model.node;

import java.util.LinkedList;

public class APINode extends MyNode {

    private LinkedList<String> labels;
    private LinkedList<Double> probs;

    public APINode() {
        labels = new LinkedList<>();
        probs = new LinkedList<>();
        this.setType("API");
    }

    public LinkedList<String> getLabels() {
        return labels;
    }

    public void setLabels(LinkedList<String> labels) {
        this.labels = labels;
    }

    public LinkedList<Double> getProbs() {
        return probs;
    }

    public void setProbs(LinkedList<Double> probs) {
        this.probs = probs;
    }
}
