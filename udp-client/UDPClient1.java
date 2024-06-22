import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

public class UDPClient1 {
    // 定义超时时间，单位ms
    public static long TTL = 100;
    //getConnected函数是用来模拟建立连接的过程
    public static void getConnected(InetAddress address, int port, DatagramSocket socket)
            throws IOException, InterruptedException {

        // 在应用层建立连接，发送“hello”
        byte[] data = "hello".getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);

        // 这个while循环模拟了建立连接，setSoTimeout函数表示如果当前发送的hello，服务器端响应的时间超出了这个数值（单位：ms），则不执行后面的语句，即解决了socket.receive卡住的问题
        while (true) {

            // 一次握手
            System.out.println("向服务器端请求连接");
            socket.send(packet);
            Thread.sleep(1000);
            packet = new DatagramPacket(data, data.length, address, port);
            try {
                socket.setSoTimeout(2000);
                socket.receive(packet);
            } catch (Exception e) {
                continue;
            }

            // 接受服务器端的响应，即同意连接，此时客户端再发送一个OK，然后发送数据包
            String s = new String(data, 0, packet.getLength());
            if (s.equals("hi")) {

                // 三次握手
                System.out.println("服务器端已响应请求连接");
                data = "OK".getBytes();
                address = packet.getAddress();
                port = packet.getPort();
                packet = new DatagramPacket(data, data.length, address, port);
                socket.send(packet);
                break;
            }
        }
    }
    //disConnected函数是用来模拟释放连接的过程
    public static void disConnected(InetAddress address, int port, DatagramSocket socket)
            throws IOException, InterruptedException {

        // 发送一个udpheader，设置isConnected为0，代表断开连接
        udpheader udpheader = new udpheader(0, 0, 2, "", "", "", "");
        byte[] data = udpheader.toString().getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);

        while (true) {

            // 一次挥手
            System.out.println("客户端向服务器端请求断开连接");
            socket.send(packet);
            Thread.sleep(500);
            try {
                socket.setSoTimeout(5000);
                socket.receive(packet);
            } catch (Exception e) {
                continue;
            }

            // 接收服务器端响应的字符串
            String s = new String(packet.getData(), 0, packet.getLength());
            if (s.equals("OK")) {

                // 二次挥手
                System.out.println("服务器端已响应断开连接");
                break;
            }
        }

        while (true) {
            data = new byte[128];
            packet = new DatagramPacket(data, data.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                continue;
            }
            String info = new String(packet.getData(), 0, packet.getLength());
            if (info.equals("Over")) {

                // 四次挥手
                System.out.println("收到服务器端结束连接请求");
                data = "OK".getBytes();
                address = packet.getAddress();
                port = packet.getPort();
                packet = new DatagramPacket(data, data.length, address, port);
                socket.send(packet);
                break;
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        // 输入服务器地址以及端口号，二者有一个不匹配都建立不上链接，服务器的端口号是固定的
        System.out.print("服务器地址和端口号：");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String input = br.readLine();
        String[] str = input.split(" ");

        // 建立连接，三次握手
        InetAddress address = InetAddress.getByName(str[0]);
        DatagramSocket socket = new DatagramSocket();
        int port = Integer.parseInt(str[1]);
        getConnected(address, port, socket);

        // 数据包的发送与接收
        Thread thread1 = new Communicate(str[0], str[1], socket);

        // 线程开始
        thread1.start();
        thread1.join();

        // 断开连接，四次挥手
        disConnected(address, port, socket);
    }
}