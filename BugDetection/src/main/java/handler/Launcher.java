package handler;

import model.MyGraph;
import model.node.MyNode;
import util.FileHandler;
import util.TimeUtil;
import util.VocabUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

/**
 * Launcher 构造新的数据
 * 1. 构造词表
 * 2. 构造id形式数据
 */
public class Launcher {

    // 通过遍历所有Java文件，对每个方法建立起API-Graph
    public static void main(String[] args) throws FileNotFoundException {

        // for train data's output
        String outputDir = "./BugDetection/newdata/jce";
        // for build vocab or load vocab
        String vocabPath = "./BugDetection/newdata/jce/vocab.txt";
        // for build vocab or train data
        String dataDir = "./BugDetection/data/jce-dataset";

        System.out.println("[INFO] start serving...");
        GraphBuilder builder = new GraphBuilder();
        SequenceExtractor extractor = new SequenceExtractor();

        TimeUtil timer = new TimeUtil();

        boolean isBuildVocabulary = false;
        boolean isBuildTrainData = true;
        int LIMIT = 30;

        VocabUtil vocabUtil = new VocabUtil();

        if(isBuildVocabulary){
            // [[ 该方法遍历特定路径目录，对其中的Java文件进行解析
            File dir = new File(dataDir);
            LinkedList<String> paths = new LinkedList<>();
            System.out.println("[INFO] traverse file.");
            timer.start();
            FileHandler.getFilePaths(dir, paths);
            System.out.println("[INFO] traverse file done, total files: " + paths.size() + ", cost time:" + timer.end() + "ms.");

            int cnt = 1;
            int fileCnt = 1;
            System.out.println("[INFO] building graph, extract sequences.");
            timer.start();
            for (String p : paths) {
                File file = FileHandler.getFile(p);
                List<MyGraph> gs = builder.build(file, p);
                for (MyGraph g: gs) {
                    LinkedList<LinkedList<MyNode>> sequence = extractor.getAPISequence(g);
                    if(sequence.size() < LIMIT) {
                        for (LinkedList<MyNode> seq : sequence) {
                            for (MyNode node : seq) {
                                vocabUtil.add2Vocab(node.getLabel());
                            }
                            cnt++;
                            if (cnt % 100 == 0) {
                                System.out.println("[INFO] build sequences: " + cnt + ", cost time:" + timer.end() + "ms.");
                            }
                        }
                    }
                }
                if(fileCnt % 10 == 0) {
                    System.out.println("[INFO] handle file: " + fileCnt + ", cost time:" + timer.end() + "ms.");
                    System.gc();
                }
                fileCnt++;
            }
            System.out.println("[INFO] build sequences done, total: " + cnt + ", cost time: " + timer.end() + "ms.");
            vocabUtil.printVocab(vocabPath);
            // ]]
        }

        if(isBuildTrainData){
            vocabUtil.loadVocabulary(vocabPath);
            // [[ 该方法遍历特定路径目录，对其中的Java文件进行解析
            File dir = new File(dataDir);
            LinkedList<String> paths = new LinkedList<>();
            System.out.println("[INFO] traverse file.");
            timer.start();
            FileHandler.getFilePaths(dir, paths);
            System.out.println("[INFO] traverse file done, total files: " + paths.size() + ", cost time:" + timer.end() + "ms.");

            // 输出: rawdata.txt, train_data.txt, trace.txt
            PrintWriter rawdataWriter = new PrintWriter(new FileOutputStream(new File(outputDir+"/rawdata.txt")));
            PrintWriter traindataWriter = new PrintWriter(new FileOutputStream(new File(outputDir+"/traindata.txt")));
            PrintWriter traceWriter = new PrintWriter(new FileOutputStream(new File(outputDir+"/trace.txt")));

            int cnt = 1;
            int fileCnt = 1;
            System.out.println("[INFO] building graph, extract sequences.");
            timer.start();
            for (String p : paths) {
                File file = FileHandler.getFile(p);
                List<MyGraph> gs = builder.build(file, p);
                for (MyGraph g: gs) {
                    String trace = g.getTrace();
                    LinkedList<LinkedList<MyNode>> sequence = extractor.getAPISequence(g);
                    if(sequence.size() < LIMIT) {
                        for (LinkedList<MyNode> seq : sequence) {
                            StringBuilder rawdata = new StringBuilder();
                            StringBuilder traindata = new StringBuilder();

                            // 为训练数据加开头
                            traindata.append(vocabUtil.getLong("START"));
                            for (MyNode node : seq) {
                                String label = node.getLabel().trim();
                                long idx = vocabUtil.getLong(label);
                                if(idx > 0) {
                                    // 根据每个sequence，输出原始训练数据 rawdata
                                    rawdata.append(" - ").append(label);
                                    // 根据每个sequence，输出整数化训练数据 traindata
                                    traindata.append(" ").append(idx);
                                }
                            }
                            // 加上结束符
                            traindata.append(" ").append(vocabUtil.getLong("EOS"));

                            // 记录数据
                            String rawdataline = rawdata.toString();
                            if(rawdataline.length() > 2) {
                                rawdataWriter.write(rawdataline.substring(2).trim() + "\n");
                                traindataWriter.write(traindata.toString().trim() + "\n");

                                // 记录数据来源
                                traceWriter.write(trace + "\n");

                                cnt++;
                                if (cnt % 100 == 0) {
                                    System.out.println("[INFO] build sequences: " + cnt + ", cost time:" + timer.end() + "ms.");
                                    rawdataWriter.flush();
                                    traindataWriter.flush();
                                    traceWriter.flush();
                                }
                            }
                        }
                    }
                }
                if(fileCnt % 10 == 0) {
                    System.out.println("[INFO] handle file: " + fileCnt + ", cost time:" + timer.end() + "ms.");
                    System.gc();
                }
                fileCnt++;
            }
            rawdataWriter.flush();
            traindataWriter.flush();
            traceWriter.flush();
            rawdataWriter.close();
            traindataWriter.close();
            traceWriter.close();
            System.out.println("[INFO] build sequences done, total: " + cnt + ", cost time: " + timer.end() + "ms.");
            // ]]
        }
        System.out.println("[INFO] end.............");
    }
}
