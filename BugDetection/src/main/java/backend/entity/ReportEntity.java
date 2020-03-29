package backend.entity;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.List;

/**
 * Report Entity
 * 记录检测的缺陷信息
 *
 * */
public class ReportEntity {

    int id;
    double rr;
    String trace;
    String location;
    List<String> sequence;
    String label;
    String beginEnd;
    List<String> goldens;
    List<Double> probs;

    public double getRR() {
        return rr;
    }

    public void setRR(double rr) {
        this.rr = rr;
    }

    public String getTrace() {
        return trace;
    }

    public void setTrace(String trace) {
        this.trace = trace;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<String> getSequence() {
        return sequence;
    }

    public void setSequence(List<String> sequence) {
        this.sequence = sequence;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<String> getGoldens() {
        return goldens;
    }

    public void setGoldens(List<String> goldens) {
        this.goldens = goldens;
    }

    public List<Double> getProbs() {
        return probs;
    }

    public void setProbs(List<Double> probs) {
        this.probs = probs;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBeginEnd() {
        return beginEnd;
    }

    public void setBeginEnd(String beginEnd) {
        this.beginEnd = beginEnd;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder("{\"id\":"+id+"}");
        ObjectMapper objmapper = new ObjectMapper();
        try {
            return objmapper.writeValueAsString(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public String toXML(){
        StringBuilder sb = new StringBuilder();
        sb.append("<report>\n");
        sb.append("<id>").append(id).append("</id>\n");
        sb.append("<trace>").append(trace).append("</trace>\n");
        sb.append("<location>").append(location).append("</location>\n");
        sb.append("<rr>").append(rr).append("</rr>\n");
        sb.append("<begin:end>").append(beginEnd).append("</begin:end>\n");
        sb.append("<label>").append(label).append("</label>\n");
        sb.append("<sequence>").append(sequence).append("</sequence>\n");
        sb.append("<golden>").append(goldens).append("</golden>\n");
        sb.append("<probs>").append(probs).append("</probs>\n");
        sb.append("</report>");
        return sb.toString();
    }

}
