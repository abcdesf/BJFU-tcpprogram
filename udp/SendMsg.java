import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.IOException;
/**
 * 本类已弃用，与JOSNControler相匹配
*/
public class SendMsg implements Runnable {
    private DatagramPacket packet;
    private DatagramSocket serverSocket;
    private String msg;

    public SendMsg(DatagramPacket packet, DatagramSocket serverSocket, String msg) {
        this.packet = packet;
        this.serverSocket = serverSocket;
        this.msg = msg;
    }

    @Override
    public void run() {
        try {
            // 获取客户端地址和端口
            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            
            // 将消息转换为字节数组
            byte[] data = msg.getBytes();
            
            // 创建一个新的数据包
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, address, port);
            
            // 通过服务器Socket发送数据包
            serverSocket.send(sendPacket);
            
            System.out.println("消息已发送到客户端: " + msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
