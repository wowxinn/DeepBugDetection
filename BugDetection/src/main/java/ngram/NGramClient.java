package ngram;

import backend.entity.InputEntity;
import backend.entity.PredictEntity;
import backend.entity.SeqEntity;
import com.github.javaparser.utils.Pair;
import model.trie.Trie;
import model.trie.TrieNode;
import org.codehaus.jackson.map.ObjectMapper;
import util.VocabUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * Client
 *
 * 计算N-gram推荐项
 *
 * */
public class NGramClient {

    // for build vocab or load vocab
    VocabUtil vocabUtil;
    // 存储Prob
    PredictEntity lastPredictEntity;

    // N for N-gram {3,4,5}
    static final int N = 3;
    boolean isSmoothing = true;

    // Tries
    LinkedList<Trie> tries;
    // lambdas
    LinkedList<Double> lambdas;

    public NGramClient(String vocabPath, String dir) {
        vocabUtil = new VocabUtil();
        // 加载词表
        try {
            vocabUtil.loadVocabulary(vocabPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 加载id数据
        String trainDataPath = dir + "traindata.txt";
        // 加载Trie
        loadTrie(trainDataPath);
    }

    /**
     * 训练N-Gram, 并存储到Trie上.
     *
     * @param trainDataPath: 训练数据所在文件(API调用编号序列)
     * */
    private void loadTrie(String trainDataPath) {

        tries = new LinkedList<>();
        lambdas = new LinkedList<>();

        // 不采用平滑
        double factor = 1d;
        if(isSmoothing){
            // 采用平滑
            factor = 0.5;
        }

        for (int n = N; n >= 1; n--) {// 构建 1~N-gram
            // 以 N 为窗口构造Sequence并加入trie
            try {
                Trie trie = new Trie();
                Scanner scanner = new Scanner(new BufferedInputStream(new FileInputStream(new File(trainDataPath))));
                while(scanner.hasNextLine()){
                    String ids = scanner.nextLine();
                    String[] largeSequence = ids.split(" ");
                    if(largeSequence.length > n-1) {
                        LinkedList<String> list = new LinkedList<>();
                        for (int i = 0; i < largeSequence.length; i++) {
                            list.addLast(largeSequence[i]);
                        }
                        for (int i = 0; i < list.size() - n + 1; i++) {
                            trie.addSequence(list.subList(i, i+n));
                        }
                    }
                }
                tries.addLast(trie);
                lambdas.addLast(factor);

                scanner.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            if(!isSmoothing){
                break;
            }
        }
    }

    public ArrayList<Double> getPredictValues(){
        return lastPredictEntity.getPredictValues();
    }

    /**
     * @param seq : 输入的是API调用词序列.
     * */
    public ArrayList<String> predict(List<String> seq){
        assert seq.size() == N - 1;
        PredictEntity predictEntity = new PredictEntity();
        List<String> words = new ArrayList<>();
        for (int i = 0; i < seq.size(); i++) {
            words.add(vocabUtil.getLong(seq.get(i))+"");
        }

        ArrayList<Pair<Double,Integer>> pairs = new ArrayList<>();

        for (int i = 0; i < tries.size(); i++) {
            Trie trie = tries.get(i);// 使用所有的trie做差值

            TrieNode parent = trie.getNodeWithPrefix(words.subList(i,words.size()));
            List<TrieNode> nodes = parent.getChildren();

            for (int j = 0; j < nodes.size(); j++) {
                TrieNode node = nodes.get(j);
                double nodeCount = node.getCount();
                if(nodeCount > 0) {
                    double value = nodeCount / parent.getCount();
                    int id = Integer.parseInt(node.getValue());

                    boolean isFind = false;
                    for (int k = 0; k < pairs.size(); k++) {
                        Pair<Double, Integer> p = pairs.get(k);
                        if(pairs.get(k).b == id){
                            pairs.set(k, new Pair<>(p.a + value*lambdas.get(i), id));
                            isFind = true;
                        }
                    }
                    if(!isFind){
                        Pair<Double,Integer> pair = new Pair<>(value*lambdas.get(i), id);
                        pairs.add(pair);
                    }
                }
            }
        }



        ArrayList<Double> predictValues = new ArrayList<>();
        ArrayList<Integer> predictIndices = new ArrayList<>();
        pairs.sort((o1, o2) -> {
            if(o1.a < o2.a){
                return 1;
            }
            else if(o1.a.equals(o2.a)){
                return 0;
            }
            else {
                return -1;
            }
        });

        int k = 0;
        for (k = 0; k < pairs.size(); k++) {
            double value = pairs.get(k).a;
            int id = pairs.get(k).b;
            predictIndices.add(id);
            predictValues.add(value);
//            System.out.println(id + ":" + value);
        }
        for (; k < 20; k++) {
            predictIndices.add(0);
            predictValues.add(0d);
        }

        predictEntity.setPredictIndices(predictIndices);
        predictEntity.setPredictValues(predictValues);
        lastPredictEntity = predictEntity;
        ArrayList<Integer> predictionIndices = predictEntity.getPredictIndices();
        int size = predictionIndices.size();
        ArrayList<String> labels = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            labels.add(vocabUtil.getWord(predictionIndices.get(i)));
        }
        return labels;
    }
}
