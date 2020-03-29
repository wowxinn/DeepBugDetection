package backend;

import backend.entity.ReportEntity;
import handler.GraphBuilder;
import handler.SequenceExtractor;
import model.MyGraph;
import model.node.MethodNode;
import model.node.MyNode;
import ngram.NGramClient;
import util.BasicRuleUtil;
import util.FileHandler;
import util.TimeUtil;
import util.VocabUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * @author wangxin
 */
public class BaselineDetector {

    VocabUtil vocabUtil;
    NGramClient client;
    GraphBuilder builder;
    SequenceExtractor extractor;
    static HashSet<String> TB;// true bugs
    static HashMap<String, Integer> detections;

    public BaselineDetector(String vocabPath, String tb) throws FileNotFoundException {
        System.out.println();
        System.out.println("[INFO] initializing client.");
        String dir = "./BugDetection/newdata/ngram/jce/";
        client = new NGramClient(vocabPath, dir);
        builder = new GraphBuilder();
        extractor = new SequenceExtractor();
        System.out.println("[INFO] preparing vocab util.");
        vocabUtil = new VocabUtil();
        vocabUtil.loadVocabulary(vocabPath);

        detections = new HashMap<>();
        loadTB(tb);
    }

    private void loadTB(String tb) {
        System.out.println("[INFO] load TB set...");
        TB = new HashSet<>();
        LinkedList<String> tbs = FileHandler.getFilePaths(tb);

        for (int i = 0; i < tbs.size(); i++) {
            String b = tbs.get(i);
            TB.add(b);
        }
    }

    static private void addToDetection(String report) {
        if (detections.containsKey(report)) {
            detections.put(report, detections.get(report) + 1);
        } else {
            detections.put(report, 1);
        }
    }

    static int id;// 从0开始递增的id

    // 这里是测试连接部分的代码
    public static void main(String[] args) throws FileNotFoundException {
        System.out.println("[RESULT]Precision, Recall, F1");

        System.out.println(test(0, 1, 0));

    }

    public static String test(double topN, double rrlimit, double problimit) throws FileNotFoundException {
        StringBuilder sb = new StringBuilder();

        String vocabPath = "./BugDetection/newdata/jce/vocab.txt";
        String tb = "./BugDetection/data/test/tb";

        BaselineDetector detector = new BaselineDetector(vocabPath, tb);
        String pathsDir = "./BugDetection/data/test/testpath";

        id = 0;// 从0开始递增的id
        LinkedList<String> paths = FileHandler.getFilePaths(pathsDir);

        TimeUtil timer = new TimeUtil();
        timer.start();

        for (int i = 0; i < paths.size(); i++) {
            List<ReportEntity> reports = detector.detect(paths.get(i), rrlimit, problimit);
//            System.out.println("<reports>");
            for (ReportEntity report : reports) {
//                System.out.println(report.toXML());
                String k = "<trace>" + report.getTrace() + "</trace><location>" + report.getLocation() + "</location>";
                String v = "<begin:end>" + report.getBeginEnd() + "</begin:end><label>" + report.getLabel() + "</label>";
                addToDetection(k + "<-->" + v);
            }
//            System.out.println("/<reports>");
//            System.out.println();
        }

        double tp = 0;
        for (String detection : detections.keySet()) {
            if (TB.contains(detection)) {
                tp++;
//                System.out.println("[INFO] tp_" + tp);
//                System.out.println(detection);
                int repeat = detections.get(detection);
//                System.out.println("[Repeat] " + repeat);
            }
        }

        int totalReport = detections.size();
        int truebugs = TB.size();
        System.out.println("==========================================");
        System.out.println("[INFO] time: " + timer.end() + "ms");
        System.out.println("[INFO] top-" + topN + " as a threshold.");
        System.out.println("[INFO] there are " + totalReport + " detections.");
        double fp = (totalReport - tp);
        System.out.println("[INFO] TP: " + tp + ", FP: " + fp);
        System.out.println("==========================================");
        double precision = (tp / (double) totalReport);
        System.out.println("[Precision]: " + precision);
        double recall = (tp / (double) truebugs);
        System.out.println("[Recall]: " + recall);
        double f1 = (2 * tp / (((double) totalReport) + (double) truebugs));
        System.out.println("[F1]: " + f1);
        System.out.println("=========================================");

        sb.append(precision).append(",").append(recall).append(",").append(f1);

        return sb.toString();
    }

    public List<ReportEntity> detect(String testPath, double rrlimit, double problimit) {

        List<ReportEntity> reports = new LinkedList<>();
        File file = FileHandler.getFile(testPath);
        List<MyGraph> gs = builder.build(file, testPath);
        for (MyGraph g : gs) {
            LinkedList<LinkedList<MyNode>> sequences = extractor.getAPISequence(g);
            for (LinkedList<MyNode> seq : sequences) {
                List<String> sequence = new ArrayList<>();

                // 这里将MyNode的label抽取出来
                for (MyNode aNode : seq) {
                    sequence.add(aNode.getLabel());
                }
                // 这里加结束符
                sequence.add("EOS");

                int len = sequence.size();
                for (int j = 1; j < len; j++) {// 从有API调用的地方开始预测.
                    List<String> subSequence = sequence.subList(0, j);
                    String golden = sequence.get(j);

                    if (BasicRuleUtil.isBasicRule(subSequence.get(subSequence.size() - 1), golden)) {
                        continue;
                    }

                    ReportEntity report = new ReportEntity();
                    report.setId(++id);
                    report.setRR(0);
                    report.setTrace(testPath);
                    report.setLocation(((MethodNode) g.getRoot()).getMethod());
                    report.setSequence(subSequence);
                    report.setLabel(golden);
                    report.setGoldens(new LinkedList<>());
                    report.setProbs(new LinkedList<>());

                    String be = "EOS";
                    if (j < len - 1) {
                        MyNode node = seq.get(j);
                        be = node.getBeginLine() + ":" + node.getEndLine();
                    }
                    report.setBeginEnd(be);
                    reports.add(report);
                }
            }
        }
        return reports;
    }
}
