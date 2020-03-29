package util;

/**
 * @author wangxin
 */
public class BasicRuleUtil {
    public static boolean isBasicRule(String p, String c){
        if(p.equals("IF") && c.equals("CONDITION")){
            return true;
        }
        if(p.equals("WHILE") && c.equals("CONDITION")){
            return true;
        }
        if(p.equals("TRY") && c.equals("TRYBLOCK")){
            return true;
        }
        if(p.equals("FOR") && c.equals("INITIALIZATION")){
            return true;
        }
        if(p.equals("FOREACH") && c.equals("VARIABLE")){
            return true;
        }
        return false;
    }
}
