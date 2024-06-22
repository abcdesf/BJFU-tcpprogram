import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPserver {

    public static void main(String[] args) throws IOException {
        DatagramSocket socket = new DatagramSocket(8800);
        ExecutorService threadPool = Executors.newFixedThreadPool(10); // 创建线程池
        System.out.println("服务器端已经启动");

        while (true) {
            byte[] data = new byte[128];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            socket.receive(packet);
            threadPool.execute(new ClientHandler(socket, packet)); // 将任务交给线程池
        }
    }
}
//对发来的包进行处理并回传，同时模拟建立连接和断开连接
class ClientHandler implements Runnable {
    private DatagramSocket socket;
    private DatagramPacket packet;

    public ClientHandler(DatagramSocket socket, DatagramPacket packet) {
        this.socket = socket;
        this.packet = packet;
    }

    @Override
    public void run() {
        try {
            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            byte[] data = packet.getData();
            String start = new String(data, 0, packet.getLength());
            //模拟的是第二次挥手
            if (start.equals("hello")) {
                System.out.println("响应客户端请求连接");
                byte[] da = ("hi").getBytes();
                DatagramPacket pa = new DatagramPacket(da, da.length, address, port);
                socket.send(pa);
            } else if (start.equals("OK")) {//正式开始传递数据包
                System.out.println("连接成功，开始传送数据包");

                while (true) {
                    Thread.sleep(5);

                    Random random = new Random();
                    int x = random.nextInt(2);
                    if (x == 0) {
                        Thread.sleep(50);
                        continue;
                    }

                    byte[] receiveData = new byte[128];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(receivePacket);
                    port = receivePacket.getPort();
                    address = receivePacket.getAddress();
                    String info = new String(receiveData, 0, receivePacket.getLength());
                    udpheader udp_header = new udpheader().decode(info);
                    System.out.println(udp_header.toStringP());
                    //收到结束的报文请求，模拟第二次挥手并跳出传输报文这一阶段
                    if (udp_header.getConnected() == 0) {
                        System.out.println("已收到客户端发来的断开连接请求");
                        byte[] da = ("OK").getBytes();
                        DatagramPacket pa = new DatagramPacket(da, da.length, address, port);
                        socket.send(pa);
                        Thread.sleep(1000);
                        break;
                    }

                    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
                    Date date = new Date(System.currentTimeMillis());
                    udp_header.setSendTime(formatter.format(date));
                    udp_header.setFrom("Server");
                    udp_header.setTo("Client");
                    udp_header.setData("This is reply " + udp_header.getSeq_no());
                    System.out.println(udp_header.toStringP());

                    byte[] da = udp_header.toString().getBytes();
                    DatagramPacket pa = new DatagramPacket(da, da.length, address, port);
                    socket.send(pa);
                }
                // 模拟断开连接四次挥手，这里是第三次挥手过程
                byte[] dat = "Over".getBytes();
                packet = new DatagramPacket(dat, dat.length, address, port);

                while (true) {
                    data = new byte[128];

                    // 第三次挥手
                    System.out.println("服务器向客户端请求结束连接");
                    socket.send(packet);
                    Thread.sleep(100);
                    packet = new DatagramPacket(data, data.length, address, port);
                    try {
                        socket.setSoTimeout(2000);
                        socket.receive(packet);
                    } catch (Exception e) {
                        continue;
                    }

                    // 接收最后一次客户端的回应，之后关闭连接
                    String s = new String(data, 0, packet.getLength());
                    if (s.equals("OK")) {
                        System.out.println("服务器端已断开连接");
                        break;
                    }
                }
                socket.close();

            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
