package model;

import model.edge.MyEdge;
import model.edge.SequenceEdge;
import model.node.ControlNode;
import model.node.MethodNode;
import model.node.MyNode;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * MyGraph
 *
 * 表示API-Graph的实体对象
 *
 * */
public class MyGraph {

    // 根节点，表示一个有向图的开始
    MyNode root;

    public MyGraph() {
    }

    public MyGraph(MyNode root) {
        this.root = root;
    }

    public MyNode getRoot() {
        return this.root;
    }

    public void setRoot(MyNode n) {
        this.root = n;
    }

    /**
     * 将图a移植到图b中 a->b, 此操作会对b产生影响，操作结束后，b的每个叶子节点都作为a的root的父节点
     * */
    public MyGraph transplant(MyGraph aGraph, MyGraph bGraph, MyEdge edge){
        MyNode bRoot = bGraph.getRoot();
        MyNode aRoot = aGraph.getRoot();
        if(bRoot == null){
            bGraph = aGraph;
        }
        else {
            Set<MyNode> leaves = bRoot.getLeaves();
            for (MyNode leaf : leaves) {
                leaf.addChild(aRoot, edge);
                leaf.setFakeLeaf(false);//注意这里的fakeleaf使用一次就失效，因此fakeleaf只适用于一条路径上的一次伪装
            }
        }
        return bGraph;
    }

    /**
     * 将图a移植到某个固定的节点下 a->node, 此操作会对node产生影响，操作结束后，返回node作为root的新Graph, node含有a
     * */
    public MyGraph transplant(MyGraph aGraph, MyNode node, MyEdge edge){
        MyGraph bGraph = new MyGraph(node);
        bGraph = bGraph.transplant(aGraph, bGraph, edge);
        return bGraph;
    }

    /**
     * 将图a移植到图中某个固定的节点下 a->b.node, 此操作会对b.node产生影响，操作结束后，返回新Graph
     * */
    public MyGraph transplant(MyGraph aGraph, MyGraph bGraph, MyNode node, MyEdge edge) {
        node.addChild(aGraph.getRoot(),edge);
        return bGraph;
    }

    /**
     * 为图上的两个节点连上一条边
     * */
    public void addEdge(MyNode father, MyNode son, MyEdge edge){
        father.addChild(son, edge);
    }

    /**
     * 获取来源信息，一般记录在根节点中
     * */
    public String getTrace() {
        if (this.root instanceof MethodNode) {
            return ((MethodNode) this.root).getPath() + " -> " + ((MethodNode) this.root).getMethod();
        } else {
            return "unknown";
        }
    }

    @Override
    public String toString(){
        if(this.root == null){
            return "[INFO]a null graph";
        }
        else{
            return root.toString();
        }
    }

}
