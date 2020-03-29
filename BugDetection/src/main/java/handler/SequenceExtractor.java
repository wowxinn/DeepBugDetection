package handler;

import model.MyGraph;
import model.node.APINode;
import model.node.ControlNode;
import model.node.MethodNode;
import model.node.MyNode;

import java.util.*;

/**
 * SequenceExtractor
 * 根据给出的Graph 抽取API调用序列
 * */
public class SequenceExtractor {

    private List<String> appendControlNodes;
    private HashMap<String, List<String>> blockEdgeControlNodesMap;

    public SequenceExtractor() {
        String[] appendControlNodesStrings = {
                "IF", "CONDITION", "THEN", "ELSE",
                "WHILE","BODY",
                "TRY", "TRYBLOCK", "CATCH", "FINALLY",
                "FOR", "INITIALIZATION", "COMPARE", "UPDATE",
                "FOREACH","VARIABLE", "ITERABLE",
        };
        appendControlNodes = Arrays.asList(appendControlNodesStrings);

        blockEdgeControlNodesMap = new HashMap<>();

        String[] blockNodesOfIFStrings = {"THEN","ELSE"};
        List<String> blockNodesOfIF = Arrays.asList(blockNodesOfIFStrings);
        blockEdgeControlNodesMap.put("IF", blockNodesOfIF);

        String[] blockNodesOfWHILEStrings = {"BODY"};
        List<String> blockNodesOfWHILE = Arrays.asList(blockNodesOfWHILEStrings);
        blockEdgeControlNodesMap.put("WHILE", blockNodesOfWHILE);

        String[] blockNodesOfTRYStrings = {"CATCH", "FINALLY"};
        List<String> blockNodesOfTRY = Arrays.asList(blockNodesOfTRYStrings);
        blockEdgeControlNodesMap.put("TRY", blockNodesOfTRY);

        String[] blockNodesOfFORStrings = {"COMPARE", "BODY", "UPDATE"};
        List<String> blockNodesOfFOR = Arrays.asList(blockNodesOfFORStrings);
        blockEdgeControlNodesMap.put("FOR", blockNodesOfFOR);

        String[] blockNodesOfFOREACHStrings = {"ITERABLE", "BODY"};
        List<String> blockNodesOfFOREACH = Arrays.asList(blockNodesOfFOREACHStrings);
        blockEdgeControlNodesMap.put("FOREACH", blockNodesOfFOREACH);
    }

    /**
     * 抽取图中最长路径并返回
     * @param graph: 待抽取的图
     * @return LinkedList<LinkedList<MyNode>>: 从graph中抽取出的n条最长路径
     * */
    public LinkedList<LinkedList<MyNode>> getAPISequence(MyGraph graph){
        // 算法:
        // 1. curr = root, paths,
        // 2. if curr.isAPINode
        // 3.      append curr to linkedList pn
        // 4. if curr.isControlNode
        // 5.      append curr to linkedList pn if necessary (IF, WHILE, TRY, CATCH, FINALLY, FOR, FOREACH)
        // 6. curr = root.children
        // 7. if children.size == 0
        // 8.      append curr to linkedList pn, add pn to paths
        // 9. if children.size == 1
        // 10.     curr = the only child of curr
        // 11.     goto 2.
        // 12. if children.size > 1
        // 13.     pn -> pn1, pn2, ..., pnx
        // 14.     curr1 = child1, curr2 = child2, ..., currx = pnx
        // 15.     goto 2.
        LinkedList<LinkedList<MyNode>> paths = new LinkedList<>();
        LinkedList<MyNode> p0 = new LinkedList<>();
        MyNode curr = graph.getRoot();
        getAPISequence(curr, p0, paths, false);
        return paths;
    }

    /**
     * 判断节点类型，并按需加入到序列路径上，或者将该序列路径加入到最终的序列路径集合中
     * @param curr: 当前节点
     * @param path: 节点所在的序列路径
     * @param paths: 已经完成的序列路径集合
     * @param existAPINode: 这条路径上是否已经存在APINode，是值true，非值false
     **/
    private void getAPISequence(MyNode curr, LinkedList<MyNode> path, LinkedList<LinkedList<MyNode>> paths, boolean existAPINode) {
        if(curr instanceof MethodNode){
            if(curr.getChildren() != null && curr.getChildren().size()>0) {
                MyNode root = curr.getChildren().get(0);
                getAPISequence(root, path, paths, false);
            }
        }
        else if(curr instanceof APINode){
            // append curr to the path
            path.addLast(curr);
            // handle the children
            LinkedList<MyNode> children = curr.getChildren();
            if(children == null || children.size() == 0){// is Leaf, is the end of a path
                paths.addLast(path);
            }
            else {
                for (MyNode child: children) {// handle child
                    LinkedList<MyNode> cPath = (LinkedList<MyNode>) path.clone();// 这里是浅拷贝
                    getAPISequence(child, cPath, paths, true);
                }
            }
        }
        else if(curr instanceof ControlNode){
            // append curr to the path: (IF, WHILE, TRY, CATCH, FINALLY, FOR, FOREACH)
            String label = curr.getLabel();
            if(appendControlNodes.contains(label)) {
                path.addLast(curr);
            }
            // handle the children
            LinkedList<MyNode> children = curr.getChildren();
            if(children == null || children.size() == 0){// is Leaf, is the end of a path
                if(existAPINode) {
                    paths.addLast(path);
                }
            }
            else {
                List<String> blockEdgeControlNodes = new LinkedList<>();
                if(blockEdgeControlNodesMap.containsKey(label)){
                    blockEdgeControlNodes = blockEdgeControlNodesMap.get(label);
                }
                for (MyNode child: children) {// handle child
                    if(!blockEdgeControlNodes.contains(child.getLabel())) {
                        LinkedList<MyNode> cPath = (LinkedList<MyNode>) path.clone();// 这里是浅拷贝
                        getAPISequence(child, cPath, paths,existAPINode);
                    }
                }
            }
        }
    }
}
