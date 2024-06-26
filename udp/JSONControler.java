import java.util.HashMap;

/**
 * 本类已弃用
 * 格式：报文类型，报文来源，报文内容
 */
public class JSONControler {
    String UnKnowString = null;
    HashMap<String,String> map;
    private int CharPosition = 0;
    private int SringLength;

    class Element{
        String content;
        String Type;
        Element(String a ,String b){
            this.content = a;
            this.Type = b;
        }
    }

    JSONControler(String s) {
        this.UnKnowString = s;
        getSringLength();
    }

    public void setUnKnowString(String s){
        this.UnKnowString = s;
        this.getSringLength();
        this.CharPosition = 0;
    }

    private void getSringLength() {
        this.SringLength = UnKnowString.length();
    }

    private char[] LexString() {
        char nowChar;
        nowChar = UnKnowString.charAt(CharPosition);
        CharPosition++;
        while((nowChar == ' ' || nowChar == '\n' || nowChar == '\\' || nowChar == '"' || nowChar == ',')&& CharPosition <= SringLength - 1){
            nowChar = UnKnowString.charAt(CharPosition++);
        }
        while ((nowChar == '{' || nowChar == '}' || nowChar == '[' || nowChar == ']'
                || nowChar == ':' ) && CharPosition <= SringLength - 1) {
            char[] data = {nowChar};
            return data;
        }
//        if (('a' <= nowChar && nowChar <= 'z') || ('A' <= nowChar && nowChar <= 'Z')) {
            String s = "";
            while (CharPosition < SringLength && nowChar != '"') {
                s += nowChar;
                nowChar = UnKnowString.charAt(CharPosition++);
            }
            if(s == "") return null;
            return s.toCharArray();
   //     }

     //   return null;
    }
    private HashMap<String,String> setMSG(){
        String data = "";
        char[] a;
        String cacheString = "";
        HashMap<String,String> map = new HashMap<>();
        while((a = LexString()) != null){
            data = new String(a);
            switch (data){
                case "{":case "}":
                    break;
                case "[":
                    break;
                case "]":
                    break;
                case ":":
                    data = new String(LexString());
                    if(data.equals("[")){
                        Element type = new Element(cacheString,"Type");
                        map.put(type.Type,type.content);
                        break;
                    }
                    Element element = new Element(data,cacheString);
                    map.put(element.Type,element.content);
                    break;
                default:
                    cacheString =data;
                    break;
            }
        }
        return map;
    }

    public HashMap<String,String> getMap(){
        return setMSG();
    }

    public String toJSONString(String Type,String ReceiveName,String msg){
        String JSONString = "";
        JSONString = "{\n" + "\"" + Type + "\":[\n" +
                "\"name\":\"" + ReceiveName + "\",\"msg\":\"" + msg + "\"},\n"
                + "]\n" +
                "}";
        return JSONString;
    }

}
