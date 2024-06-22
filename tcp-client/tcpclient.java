import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Random;

public class tcpclient {
    private static SocketChannel socketChannel;

    private static int Lmin;
    private static int Lmax;
    private static final ArrayList<String> block_msg = new ArrayList<>();
    private static final ArrayList<Integer> block_len = new ArrayList<>();
    private static final ArrayList<String> receive_msg = new ArrayList<>();

    public tcpclient() {}

    // 输入相关信息，并判断是否能连接
    public static boolean init() throws IOException {
        System.out.print("服务器地址和端口号：");
        BufferedReader br1 = new BufferedReader(new InputStreamReader(System.in));
        String input1 = br1.readLine();
        String[] str1 = input1.split(" ");

        String serverAddress = str1[0];
        int port = Integer.parseInt(str1[1]);

        try {
            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(serverAddress, port));
        } catch (IOException e) {
            System.out.println("无法连接至服务器！");
            return false;
        }

        System.out.print("设置发送报文长度范围：");
        BufferedReader br2 = new BufferedReader(new InputStreamReader(System.in));
        String input2 = br2.readLine();
        String[] str2 = input2.split(" ");

        Lmin = Integer.parseInt(str2[0]);
        Lmax = Integer.parseInt(str2[1]);

        if (Lmax > 200) {
            System.out.println("Lmax不得超过200！");
            return false;
        } else if (Lmin <= 0) {
            System.out.println("Lmin必须为正数！");
            return false;
        } else if (Lmin > Lmax) {
            System.out.println("Lmin不得大于Lmax！");
            return false;
        } else {
            return true;
        }
    }

    // 读取txt文本文件函数
    public static boolean readFile(String filepath) {
        try {
            File file = new File(filepath);
            if (file.isFile() && file.exists()) {
                InputStreamReader read = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(read);
                String text = "";
                String srcText = "";
                while ((text = br.readLine()) != null) {
                    srcText += text + " ";
                }
                read.close();

                while (true) {
                    if (srcText.length() <= Lmax) {
                        block_msg.add(srcText);
                        block_len.add(srcText.length());
                        break;
                    }

                    Random random = new Random();
                    int length = random.nextInt(Lmax - Lmin + 1) + Lmin;
                    String msg = srcText.substring(0, length);
                    srcText = srcText.substring(length);
                    block_msg.add(msg);
                    block_len.add(length);
                }
                return true;
            } else {
                System.out.println("读取文件出错！");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 建立连接，等待服务器端回应Accept
    public static void getConnected() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (true) {
                System.out.println("发送Initialization报文");
                tcpheader tcpheader = new tcpheader(1, block_msg.size(), 0, "Initialization");
                buffer.clear();
                buffer.put(tcpheader.toString().getBytes(StandardCharsets.UTF_8));
                buffer.flip();
                socketChannel.write(buffer);
                Thread.sleep(100);

                buffer.clear();
                int bytesRead = socketChannel.read(buffer);
                if (bytesRead != -1) {
                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    String msg = new String(data, StandardCharsets.UTF_8);
                    tcpheader = new tcpheader().decode(msg);
                    if (tcpheader.getType() == 2) {
                        System.out.println("收到Accept报文，开始传送数据");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // 发送报文和接收报文
    public static void SendReceive() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            for (int i = 0; i < block_msg.size(); i++) {

                // 传送数据
                tcpheader tcpheader = new tcpheader(3, i + 1, block_len.get(i), block_msg.get(i));
                buffer.clear();
                buffer.put(tcpheader.toString().getBytes(StandardCharsets.UTF_8));
                buffer.flip();
                socketChannel.write(buffer);
                Thread.sleep(1000);

                // 接收数据
                buffer.clear();
                int bytesRead = socketChannel.read(buffer);
                if (bytesRead != -1) {
                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    String msg = new String(data, StandardCharsets.UTF_8);
                    tcpheader = new tcpheader().decode(msg);
                    if (tcpheader.getType() == 4) {
                        System.out.println(tcpheader.getN() + ": " + tcpheader.getData());
                        receive_msg.add(tcpheader.getData());
                    }
                }
            }
            socketChannel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // 写txt文本文件函数
    public static void writeFile(String filepath) throws IOException {
        File file = new File(filepath);
        file.createNewFile();
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        for (int i = receive_msg.size() - 1; i >= 0; i--) {
            bw.write(receive_msg.get(i) + "\n");
        }
        bw.flush();
        bw.close();
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        while (true) {
            if (init()) {

                if (!readFile("input.txt")) {
                    break;
                }

                getConnected();

                SendReceive();

                writeFile("result.txt");

                break;
            }
        }
        socketChannel.close();
        System.exit(0);
    }
}
