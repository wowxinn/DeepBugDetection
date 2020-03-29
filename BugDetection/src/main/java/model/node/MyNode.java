package model.node;

import model.edge.MyEdge;
import model.edge.SequenceEdge;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * MyNode
 *
 * Graph上的一个Node
 *
 * */
public class MyNode {

    private String label;

    private int beginLine;
    private int endLine;

    private String type;

    private LinkedList<MyNode> children;
    private LinkedList<MyEdge> edges;

    private boolean isFakeLeaf = false;

    public MyNode() {
        children = new LinkedList<>();
        edges = new LinkedList<>();
    }

    public Set<MyNode> getLeaves(){
        Set<MyNode> leaves = new HashSet<>();
        if(isFakeLeaf || children == null || children.size() == 0){
            leaves.add(this);
            this.setFakeLeaf(false);
            return leaves;
        }
        for (MyNode c: children) {
            leaves.addAll(c.getLeaves());
        }
        return leaves;
    }

    public void addChild(MyNode node, MyEdge edge) {
        if(node == null){
            return;
        }

        if(children == null || children.size() == 0){
            children = new LinkedList<>();
            edges = new LinkedList<>();
        }

        children.addLast(node);
        edges.addLast(edge);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append(":").append(label).append(" - ");
        for (int i = 0; i < children.size(); i++) {
            sb.append(children.get(i));
        }
        return sb.toString();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LinkedList<MyNode> getChildren() {
        return children;
    }

    public void setChildren(LinkedList<MyNode> children) {
        this.children = children;
    }

    public LinkedList<MyEdge> getEdges() {
        return edges;
    }

    public void setEdges(LinkedList<MyEdge> edges) {
        this.edges = edges;
    }

    public int getBeginLine() {
        return beginLine;
    }

    public void setBeginLine(int beginLine) {
        this.beginLine = beginLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public boolean isFakeLeaf() {
        return isFakeLeaf;
    }

    public void setFakeLeaf(boolean fakeLeaf) {
        isFakeLeaf = fakeLeaf;
    }

    public void setLabel(String label){
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
