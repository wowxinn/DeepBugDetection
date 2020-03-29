package backend;

import backend.entity.InputEntity;
import backend.entity.PredictEntity;
import backend.entity.SeqEntity;
import org.codehaus.jackson.map.ObjectMapper;
import util.VocabUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Client
 * 与Python后台交互的客户端
 *
 * */
public class Client {

    // server url
    String url = "http://127.0.0.1:5000/apis/predicts";
    // for build vocab or load vocab
    VocabUtil vocabUtil;
    // 存储Prob
    PredictEntity lastPredictEntity;

    public Client(String vocabPath) {
        vocabUtil = new VocabUtil();
        // 加载词表
        try {
            vocabUtil.loadVocabulary(vocabPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Double> getPredictValues(){
        return lastPredictEntity.getPredictValues();
    }

    public ArrayList<String> predict(List<String> seq){
        PredictEntity predictEntity = null;

        String parameter = "";
        ObjectMapper objmapper = new ObjectMapper();
        InputEntity inputEntity = new InputEntity();
        SeqEntity seqEntity = new SeqEntity();
        seqEntity.setSeq(seq);
        inputEntity.setInput(seqEntity);

        try {
            parameter = objmapper.writeValueAsString(inputEntity);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String response = Client.httpRequest(url, "POST", parameter);
//        System.out.println(response);
        try {
            predictEntity = objmapper.readValue(response, PredictEntity.class);
        } catch (IOException e) {
            System.err.println("[ERROR] can not get response from server.");
            e.printStackTrace();
        }

        lastPredictEntity = predictEntity;
        ArrayList<Integer> predictionIndices = predictEntity.getPredictIndices();
        int size = predictionIndices.size();
        ArrayList<String> labels = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            labels.add(vocabUtil.getWord(predictionIndices.get(i)));
        }
        return labels;
    }


    //处理http请求  requestUrl为请求地址  requestMethod请求方式，值为"GET"或"POST"
    private static String httpRequest(String requestUrl,String requestMethod,String outputStr){
        StringBuffer buffer=null;
        try{
            URL url=new URL(requestUrl);
            HttpURLConnection conn=(HttpURLConnection)url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod(requestMethod);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.connect();
            //往服务器端写内容 也就是发起http请求需要带的参数
            if(null!=outputStr){
                OutputStream os=conn.getOutputStream();
                os.write(outputStr.getBytes("utf-8"));
                os.close();
            }

            //读取服务器端返回的内容
            InputStream is=conn.getInputStream();
            InputStreamReader isr=new InputStreamReader(is,"utf-8");
            BufferedReader br=new BufferedReader(isr);
            buffer=new StringBuffer();
            String line=null;
            while((line=br.readLine())!=null){
                buffer.append(line);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return buffer.toString();
    }

}
