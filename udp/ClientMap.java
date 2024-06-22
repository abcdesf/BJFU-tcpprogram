import java.net.DatagramPacket;
import java.util.HashMap;

/**
 * 本类已弃用，原本是存储客户端的来源信息，并以此建立确认回传机制
 */
public class ClientMap {
    public HashMap<String,DatagramPacket> map = new HashMap<>();/*Name包含地址和端口*/

    public void addClient(String Name,DatagramPacket packet){
        map.put(Name,packet);
    }

    public DatagramPacket getPacketByName(String name){
        return map.get(name);
    }
}
