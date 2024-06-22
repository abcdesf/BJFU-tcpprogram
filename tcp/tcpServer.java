import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

public class tcpServer {
    //定义一些全局需要的变量属性
    private static final int BUFSIZE = 1024;
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 7777;
    private static final int TIMEOUT = 600000; // 30秒超时
    private static final int CLIENT_NUM = 7;

    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(SERVER_IP, SERVER_PORT));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("服务器端已经启动");
        
        while (true) {
            if (selector.select(TIMEOUT) == 0) {
                System.out.println("长时间未收到信息，关闭连接");
                break;
            }

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> it = selectedKeys.iterator();

            while (it.hasNext()) {
                SelectionKey key = it.next();

                if (key.isAcceptable()) {
                    handleAccept(key);
                } else if (key.isReadable()) {
                    handleRead(key);
                }

                it.remove();
            }
        }

        serverSocketChannel.close();
        selector.close();
    }
    //开始接收函数
    private static void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(key.selector(), SelectionKey.OP_READ);
        System.out.println("正确连接来自 " + socketChannel.getRemoteAddress());
    }
    //读、处理、返回结果函数
    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(BUFSIZE);
        int bytesRead = socketChannel.read(buffer);

        if (bytesRead == -1) {
            System.out.println("断开连接来自 " + socketChannel.getRemoteAddress());
            socketChannel.close();
            return;
        }

        buffer.flip();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        buffer.clear();

        String message = new String(data, StandardCharsets.UTF_8).trim();
        System.out.println("收到消息：" + message);

        // 模拟处理数据并返回结果
        String response = processMessage(message);
        socketChannel.write(ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8)));
    }
    //解析并处理报文
    private static String processMessage(String message) {
        boolean judge = false;
        int maxN = 0;
        tcpheader accpet = new tcpheader().decode(message);
        if (accpet.getN() == maxN && accpet.getType() == 3) {
            judge = true;
        }
        tcpheader tcpheader;

        if (accpet.getType() == 1) {
            tcpheader = new tcpheader(2, accpet.getN(), accpet.getLength(), "Accept");
            maxN = accpet.getN();
        } else {
            tcpheader = new tcpheader(4, accpet.getN(), accpet.getLength(), new StringBuffer(accpet.getData()).reverse().toString());
        }

        return tcpheader.toString();
    }
}