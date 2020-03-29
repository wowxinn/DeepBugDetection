package util;

/**
 * Time
 * */
public class TimeUtil {

    long startTime;
    long endTime;

    public void start(){
        startTime = System.currentTimeMillis();
    }

    public long end(){
        endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

}
