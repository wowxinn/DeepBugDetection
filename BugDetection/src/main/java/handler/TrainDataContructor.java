package handler;

import util.TimeUtil;

import java.io.*;
import java.util.Scanner;

public class TrainDataContructor {
    public static void main(String[] args) throws FileNotFoundException {
        TimeUtil timer = new TimeUtil();

        String dir = "./BugDetection/newdata/jce/";
        // 加载id数据
        String testdataPath = dir + "traindata.txt";
        String testdataTracePath = dir + "trace.txt";
        // 输出路径
        String contextOutputPath = dir + "train_data.txt";
        String goldenOutputPath = dir  + "train_label.txt";
        String traceOutputPath = dir + "train_trace.txt";

        Scanner scanner = new Scanner(new FileInputStream(new File(testdataPath)));
        Scanner tscanner = new Scanner(new FileInputStream(new File(testdataTracePath)));

        PrintWriter contextWriter = new PrintWriter(new BufferedOutputStream(new FileOutputStream(new File(contextOutputPath))));
        PrintWriter goldentWriter = new PrintWriter(new BufferedOutputStream(new FileOutputStream(new File(goldenOutputPath))));
        PrintWriter traceWriter = new PrintWriter(new BufferedOutputStream(new FileOutputStream(new File(traceOutputPath))));

        System.out.println("[INFO] construct data.");
        timer.start();

        while (scanner.hasNextLine()) {
            String[] tmpseq = scanner.nextLine().split(" ");
            String trace = tscanner.nextLine();
            int len = tmpseq.length;
            for (int i = len - 1; i > 0; i--) {
                String golden = tmpseq[i];
                StringBuilder buff = new StringBuilder();
                for (int j = 0; j < i; j++) {
                    buff.append(" ").append(tmpseq[j]);
                }
                contextWriter.write(buff.toString().trim() + "\n");
                goldentWriter.write(golden + "\n");
                traceWriter.write(trace + "\n");
            }
        }

        goldentWriter.flush();
        goldentWriter.close();
        contextWriter.flush();
        contextWriter.close();
        traceWriter.flush();
        traceWriter.close();
        scanner.close();
        tscanner.close();

        System.out.println("[INFO] construct data in " + timer.end() + "ms.");
    }
}
