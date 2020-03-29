package handler;


import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;

import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;

import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import model.edge.ControlEdge;
import model.edge.SequenceEdge;
import model.node.APINode;
import model.node.ControlNode;
import model.node.MethodNode;
import model.MyGraph;
import model.node.MyNode;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class GraphBuilder {

    MyGraph g = null;
    List<MyGraph> gs = null;
    String PATH = null;
    String COID = null;
    Set<String> completeImports;

    private CombinedTypeSolver combinedTypeSolver;

    public GraphBuilder() {
        // 默认情况下，设置为ReflectionTypeSolver,解析jdk中的类型
        combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
    }

    public List<MyGraph> build(File file, String path) {
        gs = new LinkedList<>();
        PATH = path;
        COID = null;
        completeImports = new HashSet<>();

        try {
            CompilationUnit cu = JavaParser.parse(file);
            cu.accept(new MyVisitor(), null);
        } catch (ParseProblemException e) {
            System.err.println("can't parse:" + path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return gs;
    }

    public class MyVisitor extends VoidVisitorAdapter<Void> {

        @Override
        public void visit(ImportDeclaration importDeclaration, Void arg){
            String name = importDeclaration.getNameAsString();
            if(name.startsWith("java.") || name.startsWith("javax.")) {
                if (!importDeclaration.isAsterisk()) {
                    completeImports.add(name);
                }
            }
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration node, Void arg) {
            COID = node.getNameAsString();
            super.visit(node, arg);
        }

        @Override
        public void visit(MethodDeclaration node, Void arg) {
            Optional<BlockStmt> body = node.getBody();
            if (body.isPresent()) {

                // 每个类中的方法作为API-Graph的root节点，包含这个Graph的元信息，这里准备构造根节点
                MethodNode n = new MethodNode();

                // 抽取根节点内需要包含的元信息并存储到节点上
                String METH = node.getDeclarationAsString();
                n.setPath(PATH);
                n.setClassOrInterfaceDeclaration(COID);
                n.setMethod(METH);

                setBELine(n, node);

                // 对每个方法中的API构造Graph
                g = new MyGraph(n);

                BlockStmt b = body.get();
                g = g.transplant(buildSubGraph(b),g,new SequenceEdge());

                gs.add(g);
            }
            super.visit(node, arg);
        }

        public String myVisit(ObjectCreationExpr node, String apicalltoken) {
            String name = getNodeDescribe(node);
            if (name != null && !name.equals("null")) {
                apicalltoken = getNodeDescribe(node) + ".new(";
                NodeList<Expression> args = node.getArguments();
                apicalltoken = handleArguments(args, apicalltoken);
                apicalltoken = apicalltoken + ")";
            }
            return apicalltoken;
        }

        // [[ 将参数作为原有参数类型 的方法时需要的visit解析方法
        public String myVisit(MethodCallExpr node, String apicalltoken){
            Optional<Expression> scopes = node.getScope();
            if(scopes.isPresent()) {
                Expression scope = scopes.get();
                apicalltoken = apicalltoken + handleScope(node, scope, apicalltoken);
            }
            return apicalltoken;
        }
        // ]]

        public String myVisit(FieldAccessExpr node, String apicalltoken){
            Expression scope = node.getScope();
            String name = getNodeDescribe(scope);
            if (name != null && !name.equals("null")) {
                apicalltoken = name + node.getNameAsString();
            }
            return apicalltoken;
        }

        private String handleArguments(NodeList<Expression> args, String apicalltoken) {
            for (int i = 0; i < args.size(); i++) {
                Expression e = args.get(i);
                // [[ 该方法将参数作为原有API中的参数类型
                if (i == args.size() - 1) {
                    apicalltoken = apicalltoken + getNodeDescribe(e);
                } else {
                    apicalltoken = apicalltoken + getNodeDescribe(e) + ",";
                }
                // ]]
            }
            return apicalltoken;
        }

        // [[ 解析MethodCallExpr的内部参数
        private String handleScope(MethodCallExpr node, Expression scope, String apicalltoken) {
            String name = getNodeDescribe(scope);
            if (name != null && !name.equals("null")) {
                apicalltoken = apicalltoken + name + "." + node.getName() + "(";
                NodeList<Expression> args = node.getArguments();
                apicalltoken = handleArguments(args,apicalltoken);
                apicalltoken = apicalltoken + ")";
            }
            return apicalltoken;
        }
        // ]]

        private String getNodeDescribe(Node node){
            ResolvedType rt = getNodeDescribeRT(node);
            if(rt != null){
                return rt.describe();
            }
            else {
                // 查找是否已经存在完整类
                for (String aName: completeImports) {
                    if(aName.endsWith("." + node.toString())){
                        return aName;
                    }
                }
            }
            return null;
        }

        // 获取Node的ResolvedType，Solver类型由超参combinedTypeSolver决定
        private ResolvedType getNodeDescribeRT(Node node) {
            ResolvedType rt = null;
            try {
                rt = JavaParserFacade.get(combinedTypeSolver).getType(node);
            } catch (RuntimeException e) {
                // skip
            }
            return rt;
        }

        private MyGraph buildSubGraph(Node childNode) {
            MyGraph subGraph = new MyGraph();

            if(childNode instanceof Expression){
                if(childNode instanceof ObjectCreationExpr){
                    List<Expression> arguments = ((ObjectCreationExpr) childNode).getArguments();
                    for (Expression argument : arguments) {
                        MyGraph subArgGraph = buildSubGraph(argument);
                        subGraph = subGraph.transplant(subArgGraph,subGraph,new SequenceEdge());// 可以考虑这里是否加入新类型的边
                    }

                    String creation = myVisit((ObjectCreationExpr) childNode, "");

                    if(!creation.equals("")) {
                        APINode node = new APINode();
                        node.setLabel(creation);
                        setBELine(node, childNode);
                        MyGraph creationGraph = new MyGraph(node);
                        subGraph = subGraph.transplant(creationGraph, subGraph, new SequenceEdge());
                    }
                    return subGraph;
                }
                else if(childNode instanceof VariableDeclarationExpr){
                    List<VariableDeclarator> vds = ((VariableDeclarationExpr) childNode).getVariables();
                    for (VariableDeclarator vd: vds) {
                        Optional<Expression> expression1 = vd.getInitializer();
                        if(expression1.isPresent()){
                            Expression e1 = expression1.get();
                            return buildSubGraph(e1);
                        }
                    }
                }
                else if(childNode instanceof MethodCallExpr){
                    // 先解析MethodCallExpr中可能带有的其他Expression
                    List<Expression> arguments = ((MethodCallExpr) childNode).getArguments();
                    for (Expression argument : arguments) {
                        MyGraph subArgGraph = buildSubGraph(argument);
                        subGraph = subGraph.transplant(subArgGraph, subGraph, new SequenceEdge());
                    }
                    String methodcall = myVisit((MethodCallExpr) childNode, "");

                    if(!methodcall.equals("")) {
                        APINode node = new APINode();
                        node.setLabel(methodcall);
                        setBELine(node, childNode);
                        MyGraph methodGraph = new MyGraph(node);
                        subGraph = subGraph.transplant(methodGraph, subGraph, new SequenceEdge());
                    }
                    return subGraph;
                }
                else if(childNode instanceof BinaryExpr){
                    Expression left = ((BinaryExpr) childNode).getLeft();
                    Expression right = ((BinaryExpr) childNode).getRight();
                    subGraph = buildSubGraph(left);// leftGraph
                    MyGraph rightGraph = buildSubGraph(right);
                    subGraph = subGraph.transplant(rightGraph, subGraph, new SequenceEdge());
                }
                else if(childNode instanceof FieldAccessExpr){
                    String fieldAccess = myVisit((FieldAccessExpr) childNode, "");
                    if(!"".endsWith(fieldAccess)) {
                        APINode node = new APINode();
                        node.setLabel(fieldAccess);
                        setBELine(node, childNode);
                        MyGraph filedAccessGraph = new MyGraph(node);
                        subGraph = subGraph.transplant(filedAccessGraph, subGraph, new SequenceEdge());
                    }
                }
                else if(childNode instanceof  AssignExpr){
                    Expression value = ((AssignExpr) childNode).getValue();
                    subGraph = buildSubGraph(value);
                }
            }
            else if(childNode instanceof ExpressionStmt){
                Expression expression = ((ExpressionStmt) childNode).getExpression();
                return buildSubGraph(expression);
            }
            // 接下来是一些控制结构体的处理
            else if(childNode instanceof IfStmt){

                ControlNode root = new ControlNode();
                root.setLabel("IF");
                setBELine(root, childNode);
                subGraph.setRoot(root);

                Expression condition = ((IfStmt) childNode).getCondition();
                MyGraph subConditionGraph = buildSubGraph(condition);
                ControlNode conditionNode = new ControlNode();
                conditionNode.setLabel("CONDITION");
                setBELine(conditionNode, condition);
                subConditionGraph = subConditionGraph.transplant(subConditionGraph, conditionNode, new SequenceEdge());
                subGraph = subGraph.transplant(subConditionGraph, subGraph, root, new ControlEdge());// 只要加到if上即可
                Set<MyNode> conditionLeaves = conditionNode.getLeaves();

                Statement thenStmt = ((IfStmt) childNode).getThenStmt();
                MyGraph subThenGraph = buildSubGraph(thenStmt);
                ControlNode thenNode = new ControlNode();
                thenNode.setLabel("THEN");
                setBELine(thenNode, thenStmt);
                subThenGraph = subThenGraph.transplant(subThenGraph, thenNode, new SequenceEdge());
                subGraph = subGraph.transplant(subThenGraph, subGraph, root, new ControlEdge());
                // 还需要将condition也作为自己的爸爸
                for (MyNode leaf: conditionLeaves) {
                    subGraph.addEdge(leaf, thenNode, new SequenceEdge());
                }

                Optional<Statement> elseStmt = ((IfStmt) childNode).getElseStmt();
                if(elseStmt.isPresent()){
                    Statement elsestmt = elseStmt.get();
                    MyGraph subElseGraph = buildSubGraph(elsestmt);
                    ControlNode elseNode = new ControlNode();
                    elseNode.setLabel("ELSE");
                    setBELine(elseNode, elsestmt);
                    subElseGraph = subElseGraph.transplant(subElseGraph, elseNode, new SequenceEdge());
                    subGraph = subGraph.transplant(subElseGraph, subGraph,root, new ControlEdge());
                    // 还需要将condition也作为自己的爸爸
                    for (MyNode leaf: conditionLeaves) {
                        subGraph.addEdge(leaf, elseNode, new SequenceEdge());
                    }
                }
                else{
                    for (MyNode leaf: conditionLeaves) {
                        leaf.setFakeLeaf(true);// 这里先假装都是叶子节点
                    }
                }
            }
            else if(childNode instanceof WhileStmt){
                ControlNode root = new ControlNode();
                root.setLabel("WHILE");
                setBELine(root, childNode);
                subGraph.setRoot(root);

                Expression condition = ((WhileStmt) childNode).getCondition();
                MyGraph subConditionGraph = buildSubGraph(condition);
                ControlNode conditionNode = new ControlNode();
                conditionNode.setLabel("CONDITION");
                setBELine(conditionNode, condition);
                subConditionGraph = subConditionGraph.transplant(subConditionGraph, conditionNode, new SequenceEdge());
                subGraph = subGraph.transplant(subConditionGraph, subGraph, root, new ControlEdge());
                Set<MyNode> conditionLeaves = conditionNode.getLeaves();
                for (MyNode leaf: conditionLeaves) {
                    leaf.setFakeLeaf(true);// 这里先假装都是叶子节点
                }

                Statement body = ((WhileStmt) childNode).getBody();
                MyGraph subBodyGraph = buildSubGraph(body);
                ControlNode bodyNode = new ControlNode();
                bodyNode.setLabel("BODY");
                setBELine(bodyNode, body);
                subBodyGraph = subBodyGraph.transplant(subBodyGraph, bodyNode, new SequenceEdge());
                subGraph = subGraph.transplant(subBodyGraph, subGraph, root, new ControlEdge());
                // 将condition也作为爸爸
                for (MyNode leaf: conditionLeaves) {
                    subGraph.addEdge(leaf, bodyNode, new SequenceEdge());
                }
            }
            else if(childNode instanceof TryStmt){
                ControlNode root = new ControlNode();
                root.setLabel("TRY");
                setBELine(root, childNode);
                subGraph.setRoot(root);

                BlockStmt tryBlock = ((TryStmt) childNode).getTryBlock();
                MyGraph subTryBlockGraph = buildSubGraph(tryBlock);
                ControlNode tryBlockNode = new ControlNode();
                tryBlockNode.setLabel("TRYBLOCK");
                setBELine(tryBlockNode, tryBlock);
                subTryBlockGraph = subTryBlockGraph.transplant(subTryBlockGraph, tryBlockNode, new SequenceEdge());
                subGraph = subGraph.transplant(subTryBlockGraph, subGraph, root, new ControlEdge());

                Set<MyNode> tryBlockNodeLeaves = tryBlockNode.getLeaves();
                List<MyNode> leaves  = new LinkedList<>();
                leaves.addAll(tryBlockNodeLeaves);

                // 这里会有多个Catch需要处理，每个catch的直属parent是tryBlock
                List<CatchClause> catchClauses = ((TryStmt) childNode).getCatchClauses();
                for (CatchClause cc : catchClauses) {
                    MyGraph subBodyGraph = buildSubGraph(cc.getBody());

                    ControlNode catchNode = new ControlNode();
                    catchNode.setLabel("CATCH");
                    setBELine(catchNode, cc);

                    subBodyGraph = subBodyGraph.transplant(subBodyGraph, catchNode, new SequenceEdge());
                    subGraph = subGraph.transplant(subBodyGraph, subGraph, root, new ControlEdge());

                    // 将tryblock也作为爸爸
                    for (MyNode leaf: tryBlockNodeLeaves) {
                        subGraph.addEdge(leaf, catchNode, new SequenceEdge());
                    }

                    leaves.addAll(catchNode.getLeaves());
                }

                Optional<BlockStmt> finallyBlock = ((TryStmt) childNode).getFinallyBlock();
                if(finallyBlock.isPresent()){
                    Statement finallyStmt = finallyBlock.get();
                    MyGraph subFinalGraph = buildSubGraph(finallyStmt);
                    ControlNode finalNode = new ControlNode();
                    finalNode.setLabel("FINALLY");
                    setBELine(finalNode, finallyStmt);
                    subFinalGraph = subFinalGraph.transplant(subFinalGraph, finalNode, new SequenceEdge());
                    subGraph = subGraph.transplant(subFinalGraph, subGraph,root, new ControlEdge());
                    // 还需要将catch们和tryblock也作为自己的爸爸
                    for (MyNode leaf: leaves) {
                        subGraph.addEdge(leaf, finalNode, new SequenceEdge());
                    }
                }
                else{
                    for (MyNode leaf: leaves) {
                        leaf.setFakeLeaf(true);// 这里先假装都是叶子节点
                    }
                }
            }
            else if(childNode instanceof ForStmt){

                ControlNode root = new ControlNode();
                root.setLabel("FOR");
                setBELine(root, childNode);
                subGraph.setRoot(root);

                List<Expression> initializations = ((ForStmt) childNode).getInitialization();
                ControlNode initializationNode = new ControlNode();
                initializationNode.setLabel("INITIALIZATION");
                setBELine(initializationNode, childNode);// here, use for's begin&end line
                MyGraph initializationGraph = new MyGraph(initializationNode);
                MyGraph subInitializationGraph;// a null graph

                for (Expression initialization : initializations) {
                    setELine(initializationNode, initialization);// update the end line
                    subInitializationGraph = buildSubGraph(initialization);
                    initializationGraph = initializationGraph.transplant(subInitializationGraph, initializationGraph, new SequenceEdge());
                }
                subGraph = subGraph.transplant(initializationGraph, subGraph, root, new ControlEdge());
                Set<MyNode> initializationLeaves = initializationNode.getLeaves();

                Optional<Expression> compares = ((ForStmt) childNode).getCompare();
                ControlNode compareNode = null;
                if(compares.isPresent()){
                    Expression compare = compares.get();
                    compareNode = new ControlNode();
                    compareNode.setLabel("COMPARE");
                    setBELine(compareNode, compare);
                    MyGraph subCompareGraph = buildSubGraph(compare);
                    subCompareGraph = subCompareGraph.transplant(subCompareGraph, compareNode, new SequenceEdge());
                    subGraph = subGraph.transplant(subCompareGraph, subGraph, root, new ControlEdge());

                    // 将initialization的leaves也作为自己的爸爸们
                    for (MyNode leaf: initializationLeaves) {
                        subGraph.addEdge(leaf, compareNode, new SequenceEdge());
                    }
                }
                else {
                    for (MyNode leaf: initializationLeaves) {
                        leaf.setFakeLeaf(true);
                    }
                }

                // for's body
                ControlNode bodyNode = new ControlNode();
                bodyNode.setLabel("BODY");
                Statement body = ((ForStmt) childNode).getBody();
                setBELine(bodyNode, body);

                MyGraph subBodyGraph = buildSubGraph(body);
                subBodyGraph = subBodyGraph.transplant(subBodyGraph, bodyNode, new SequenceEdge());
                // body's parents: initialization/(compare)/For
                subGraph = subGraph.transplant(subBodyGraph, subGraph,root, new ControlEdge());

                if(compareNode != null) {
                    for (MyNode leaf: compareNode.getLeaves()) {
                        subGraph.addEdge(leaf, bodyNode, new SequenceEdge());
                    }
                }

                Set<MyNode> bodyLeaves = bodyNode.getLeaves();

                ControlNode updateNode = new ControlNode();
                updateNode.setLabel("UPDATE");
                setBELine(updateNode, childNode);// here, using parent's begin&end line

                MyGraph subUpdateGraph = new MyGraph(updateNode);
                List<Expression> update = ((ForStmt) childNode).getUpdate();
                for (Expression expression : update) {
                    setELine(updateNode, expression);
                    MyGraph subExprGraph = buildSubGraph(expression);
                    subUpdateGraph = subUpdateGraph.transplant(subExprGraph, subUpdateGraph, new SequenceEdge());
                }

                subGraph = subGraph.transplant(subUpdateGraph, subGraph, root, new ControlEdge());

                for (MyNode leave : bodyLeaves) {
                    subGraph.addEdge(leave, updateNode, new SequenceEdge());
                }
            }
            else if(childNode instanceof  ForeachStmt){
                MyNode root = new ControlNode();
                root.setLabel("FOREACH");
                setBELine(root, childNode);
                subGraph.setRoot(root);

                // VARIABLE
                MyNode variableNode = new ControlNode();
                variableNode.setLabel("VARIABLE");
                VariableDeclarationExpr variableDeclarationExpr = ((ForeachStmt) childNode).getVariable();
                setBELine(variableNode,variableDeclarationExpr);
                MyGraph variableSubGraph = buildSubGraph(variableDeclarationExpr);
                variableSubGraph = variableSubGraph.transplant(variableSubGraph, variableNode, new SequenceEdge());
                subGraph = subGraph.transplant(variableSubGraph,subGraph,root,new ControlEdge());
                Set<MyNode> variableLeaves = variableNode.getLeaves();

                // ITERABLE
                MyNode iterableNode = new ControlNode();
                iterableNode.setLabel("ITERABLE");
                Expression iterableExpr = ((ForeachStmt) childNode).getIterable();
                setBELine(iterableNode,iterableExpr);
                MyGraph iterableGraph = buildSubGraph(iterableExpr);
                iterableGraph = iterableGraph.transplant(iterableGraph, iterableNode, new SequenceEdge());
                subGraph = subGraph.transplant(iterableGraph,subGraph, root, new ControlEdge());
                // set "variable" as parent, too.
                for (MyNode leaf : variableLeaves) {
                    subGraph.addEdge(leaf, iterableNode, new SequenceEdge());
                }
                Set<MyNode> iterableLeaves = iterableNode.getLeaves();

                // BODY
                ControlNode bodyNode = new ControlNode();
                bodyNode.setLabel("BODY");
                Statement body = ((ForeachStmt) childNode).getBody();
                setBELine(bodyNode, body);
                MyGraph bodyGraph = buildSubGraph(body);
                bodyGraph = bodyGraph.transplant(bodyGraph, bodyNode, new SequenceEdge());
                subGraph = subGraph.transplant(bodyGraph,subGraph,root,new ControlEdge());
                for (MyNode leaf: iterableLeaves) {
                    subGraph.addEdge(leaf, bodyNode, new SequenceEdge());
                }
            }
            // 特殊的BlockStmt是由多个statement组成，处理后，返回一个串行结构图
            else if(childNode instanceof BlockStmt){
                List<Statement> statements = ((BlockStmt) childNode).getStatements();
                for (Statement statement : statements) {
                    MyGraph subStmtGraph = buildSubGraph(statement);
                    subGraph = subGraph.transplant(subStmtGraph, subGraph, new SequenceEdge());
                }
            }
            else if(childNode instanceof ReturnStmt){
                Optional<Expression> expression = ((ReturnStmt) childNode).getExpression();
                if(expression.isPresent()) {
                    subGraph = buildSubGraph(expression.get());
                }
            }
            return subGraph;
        }

        private void setELine(MyNode node, Node expression) {
            Optional<Range> range = expression.getRange();
            if (range.isPresent()) {
                Range r = range.get();
                int e = r.end.line;
                node.setEndLine(e);
            }
        }

        private void setBELine(MyNode node, Node expression) {
            Optional<Range> range = expression.getRange();
            if (range.isPresent()) {
                Range r = range.get();
                int b = r.begin.line;
                int e = r.end.line;
                node.setBeginLine(b);
                node.setEndLine(e);
            }
        }
    }
}