import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Communicate extends Thread {
    //宏定义，本功能需要的一些变量
    public static final int PACKET_NUM = 12;
    public String serverIP;
    public int serverPort;
    public DatagramSocket clientSocket;

    public Communicate(String serverIP, String serverPort, DatagramSocket clientSocket) {
        this.serverIP = serverIP;
        this.serverPort = Integer.parseInt(serverPort);
        this.clientSocket = clientSocket;
    }

    @Override//发送报文与接收，输出总结
    public void run() {
        List<Double> allRtt = new ArrayList<>();

        int packetNum = 0;
        String firstResponse = null;
        String lastResponse = null;
        InetAddress address = null;
        try {
            address = InetAddress.getByName(serverIP);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        try {
            clientSocket.setSoTimeout(100);
            //发送12个报文以及接收
            for (int seqNo = 1; seqNo <= PACKET_NUM; seqNo++) {
                int reCnt = 0;
                // 设置数据报文的各项值
                udpheader udp_header = new udpheader();
                udp_header.setConnected(1);
                udp_header.setSeq_no(seqNo);
                SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
                Date date = new Date(System.currentTimeMillis());
                udp_header.setSendTime(formatter.format(date));
                udp_header.setFrom("Client");
                udp_header.setTo("Server");
                udp_header.setData("This is the request" + seqNo);

                while (true) {
                    try {
                        byte[] requestPacket = udp_header.toString().getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(requestPacket, requestPacket.length, address, serverPort);
                        long startTime = System.nanoTime();
                        //区分是否重传
                        if(reCnt == 1){
                            try {
                                Thread.sleep(50);
                            } catch (Exception f) {
                            }
                        }
                        clientSocket.send(sendPacket); 

                        
                        byte[] buffer = new byte[1024];
                        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                        clientSocket.receive(receivePacket); 
                        long endTime = System.nanoTime();

                        double rtt = (endTime - startTime) / 1e6;
                        allRtt.add(rtt);
                        packetNum++;

                        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        udpheader udpheader = new udpheader().decode(response);
                        int responseSeqNo = udpheader.getSeq_no();
                        String serverIP = receivePacket.getAddress().getHostAddress();
                        int serverPort = receivePacket.getPort();
                        String times = udp_header.getSendTime();

                        if (firstResponse == null) {
                            firstResponse = times;
                        }
                        lastResponse = times;
                        System.out.println("sequence no " + seqNo + "， " + address.toString().substring(1) + ":" + serverPort + "，RTT="
                    + rtt + "ms");
                        break;
                    } catch (SocketTimeoutException e) {
                        //超时处理的逻辑
                        reCnt++;
                        if (reCnt > 2) {
                            System.out.printf("sequence no: %d, request time out!%n", seqNo);
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
            //输出总结
            if (!allRtt.isEmpty()) {
                System.out.println("\n");
                System.out.println("Received " + packetNum + " udp packets");
                System.out.printf("Loss rate: %.2f%%%n", ((PACKET_NUM - packetNum) / (double) PACKET_NUM) * 100);

                double stdRtt = allRtt.get(0);
                if (allRtt.size() > 1) {
                    // Calculate standard deviation of RTT
                    stdRtt = calculateStdDev(allRtt);
                }
                System.out.printf("RTT max: %.2f ms, RTT min: %.2f ms, RTT average: %.2f ms, RTT std dev: %.2f ms%n",
                        getMaxRtt(allRtt), getMinRtt(allRtt), getAverageRtt(allRtt), stdRtt);

                if (firstResponse != null && lastResponse != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
                    LocalTime firstResponseTime = LocalTime.parse(firstResponse, formatter);
                    LocalTime lastResponseTime = LocalTime.parse(lastResponse, formatter);
                    long totalResponseTimeSeconds = java.time.Duration.between(firstResponseTime, lastResponseTime).getSeconds();
                    System.out.println("Total response time: " + totalResponseTimeSeconds + " seconds");
                }
            } else {
                System.out.println("Received no packets");
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } 
    }
    //计算最大RTT的函数
    private double getMaxRtt(List<Double> rttList) {
        return rttList.stream().mapToDouble(Double::doubleValue).max().orElse(0);
    }
    //计算最小RTT的函数
    private double getMinRtt(List<Double> rttList) {
        return rttList.stream().mapToDouble(Double::doubleValue).min().orElse(0);
    }
    //计算平均RTT的函数
    private double getAverageRtt(List<Double> rttList) {
        return rttList.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }
    //计算RTT标准差的函数
    private double calculateStdDev(List<Double> rttList) {
        double mean = getAverageRtt(rttList);
        double sum = rttList.stream().mapToDouble(rtt -> Math.pow(rtt - mean, 2)).sum();
        return Math.sqrt(sum / (rttList.size() - 1));
    }

}
