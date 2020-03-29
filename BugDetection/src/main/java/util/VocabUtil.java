package util;

import java.io.*;
import java.util.*;

/**
 * VocabUtil
 * 与词表相关的功能类
 */

public class VocabUtil {

    private HashMap<String, Long> vocab;

    private HashMap<String, Long> word2long;
    private HashMap<Long, String> long2word;

    private long MINFREQUENCY = 2;

    public VocabUtil() {
        vocab = new HashMap<>();
        word2long = new HashMap<>();
        long2word = new HashMap<>();
    }

    /**
     * 统计词频，计入词表
     * */
    public void add2Vocab(String word){
        if(word != null && !word.equals("")) {
            if (vocab.containsKey(word)) {
                vocab.put(word, vocab.get(word) + 1);
            } else {
                vocab.put(word, 1L);
            }
        }
    }

    /**
     * 将现有词表做排序统计后输出至词表文件
     * */
    public void printVocab(String vocabPath) throws FileNotFoundException {
        // 排序
        System.out.println("[INFO] sorting vocabulary.");
        List<Map.Entry<String, Long>> vocabList = new ArrayList<>(vocab.entrySet());
        Comparator<Map.Entry<String, Long>> comparator = Comparator.comparing(Map.Entry::getValue);
        vocabList.sort(comparator.reversed());
        System.out.println("[INFO] sorting vocabulary done, vocab size: " + vocabList.size());

        // 按序输出
        System.out.println("[INFO] print vocabulary.");
        PrintWriter writer = new PrintWriter(new BufferedOutputStream(new FileOutputStream(vocabPath)));

        // 加上0: UNK ; 1: EOS
        writer.write( "UNK\n");
        writer.write( "EOS\n");
        writer.flush();

        for (Map.Entry<String, Long> aVocab : vocabList) {
            // 低频词的过滤
            if(aVocab.getValue() >= MINFREQUENCY) {
                writer.write(aVocab.getKey() + "\n");
                writer.flush();
            }
        }
        writer.close();
        System.out.println("[INFO] print vocabulary done, output path: " + vocabPath);

    }

    public void loadVocabulary(String vocabPath) throws FileNotFoundException {
        System.out.println("[INFO] loading vocabulary from " + vocabPath + " ...");
        Scanner scanner = new Scanner(new FileInputStream(new File(vocabPath)));
        long cnt = 0;
        while (scanner.hasNextLine()){
            String word = scanner.nextLine().trim();
            word2long.put(word, cnt);
            long2word.put(cnt, word);
            cnt++;
        }
        System.out.println("[INFO] loading vocabulary done.");
    }

    public long getLong(String label) {
        if(word2long.containsKey(label.trim())){
            return word2long.get(label.trim());
        }
        else{
            return 0;// 0: UNK
        }
    }

    public String getWord(long l) {
        if(long2word.containsKey(l)){
            return long2word.get(l);
        }
        else{
            return "UNK";
        }
    }

    public List<String> getWords(List<Integer> seq){
        List<String> words = new LinkedList<>();
        for (Integer aSeq : seq) {
            words.add(getWord(aSeq));
        }
        return words;
    }
}
