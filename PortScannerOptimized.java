
//package org.example;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class PortScannerOptimized {

    private static final String CMD_URL_FORMAT1 = "http://127.0.0.1:%d/cmd.xml?cmd=switch_chan&id=1c3c73215afd42558101c3ee65737202&server=108.181.32.169:%d";
    private static final String CMD_URL_FORMAT2 = "http://127.0.0.1:%d/cmd.xml?cmd=switch_chan&id=5ff7074800001bcccbe59f3d4ff00590&server=108.181.20.159:%d";
    //private static final String PP_SHELL_CMD_FORMAT = "C:\\Program Files (x86)\\ForceP2P Media Player\\ppshell.exe -o %d"; // 假设ppshell.exe在PATH环境变量中可找到
    //private static final String PP_SHELL_CMD_FORMAT = "C:\\ForceP2P Media Player\\ppshell.exe -o %d"; // 假设ppshell.exe在PATH环境变量中可找到
   private static final String PP_SHELL_CMD_FORMAT = "D:\\a\\java\\java\\ForceP2PMediaPlayer\\ppshell.exe -o %d"; // 假设ppshell.exe在PATH环境变量中可找到
    private static final ExecutorService executor = Executors.newFixedThreadPool(50); // 创建线程池
    //private static List<Process> processes = new ArrayList<>();
    private static volatile boolean continueScanning = true;
    private static AtomicInteger foundPort = new AtomicInteger(-1);

    public static void main(String[] args) throws InterruptedException {
        //while (true) {
            int startPort = 10006;
            int endPort = 60000;
            //CountDownLatch latch = new CountDownLatch(50); // 线程池大小
            //doThread(startPort, endPort, 1, latch);
           // latch.await(); // 等待cmdType为2的任务完成
            CountDownLatch latch1 = new CountDownLatch(50); // 线程池大小
            doThread(startPort, endPort, 2, latch1);
            latch1.await();
            System.out.println("进行exe资源释放！");
            String processName = "ppshell.exe";
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("taskkill", "/F", "/IM", processName);
                Process process = processBuilder.start();
            } catch (IOException e) {
                System.err.println("执行命令时发生错误: " + e.getMessage());
            }
			System.out.println("扫描完成。");
			System.exit(0);
            //Thread.sleep(60 * 30 * 1000); // 隔30分钟执行下一轮
        //}
    }

    private static void doThread(int startPort, int endPort, int cmdType, CountDownLatch latch) {
        String CMD_URL_FORMAT = cmdType == 1 ? CMD_URL_FORMAT1 : CMD_URL_FORMAT2;
        continueScanning = true; // 重置标志变量
        int numThreads = 50; // 线程池大小
        int portsPerThread = (endPort - startPort + 1) / numThreads;

        for (int i = 0; i < numThreads; i++) {
            int threadStartPort = startPort + i * portsPerThread;
            int threadEndPort = (i == numThreads - 1) ? endPort : threadStartPort + portsPerThread - 1;
            executor.submit(() -> scanPorts(threadStartPort, threadEndPort, CMD_URL_FORMAT, latch));
        }
    }

    private static void scanPorts(int startPort, int endPort, String CMD_URL_FORMAT, CountDownLatch latch) {
        if (!continueScanning) {
            long currentTimeMillis = System.currentTimeMillis();
            LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMillis), ZoneId.systemDefault());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = localDateTime.format(formatter);
            System.out.println(formattedDateTime + " 直接扫描端口: " + startPort + " 到 " + endPort);
            latch.countDown(); //任务完成后减少计数器
            return;
        }
        for (int port = startPort; port <= endPort; port++) {
            if (!continueScanning) break;
            scanAndStartPort(port, CMD_URL_FORMAT);
        }
        latch.countDown(); //任务完成后减少计数器
    }

    private static void scanAndStartPort(int port, String CMD_URL_FORMAT) {
        if (!continueScanning) return;
        Process process = null;
        try {
            String command = String.format(PP_SHELL_CMD_FORMAT, port);
            process = Runtime.getRuntime().exec(command);
            Thread.sleep(1000); // 等待1秒
            if (process.isAlive()) {
               // processes.add(process);
                HttpURLConnection connection = null;
                HttpURLConnection connection2 = null;
                HttpURLConnection connection3 = null;
                try {
                    URL url1 = new URL(String.format(CMD_URL_FORMAT, port, port));
                    connection = (HttpURLConnection) url1.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(3000);
                    connection.setReadTimeout(3000);
                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200) {
                        Thread.sleep(3000); // 等待3秒
                        try {
                            URL url2 = new URL(String.format(CMD_URL_FORMAT, port, port));
                            connection2 = (HttpURLConnection) url2.openConnection();
                            connection2.setRequestMethod("GET");
                            connection2.setConnectTimeout(3000);
                            connection2.setReadTimeout(3000);
                            int responseCode2 = connection2.getResponseCode();
                            if (responseCode2 != 200) {
                            } else {
                                if (process.isAlive()) {
                                    process.destroy();
                                    //processes.remove(process);
                                }
                                long currentTimeMillis = System.currentTimeMillis();
                                LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMillis), ZoneId.systemDefault());
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                String formattedDateTime = localDateTime.format(formatter);
                                System.out.println(formattedDateTime + " 扫描端口: " + port);
                                if (process != null && process.isAlive()) process.destroy();
                            }
                        } catch (Exception e) {
                            System.out.println("发现端口: " + port);
                            String url = "http://api.ximiba.cn/proxy/iptv/putPort.php?port=";
                            if (CMD_URL_FORMAT.equals(CMD_URL_FORMAT1)) {
                                url = "http://api.ximiba.cn/proxy/iptv/putPort.php?port=";
                            }
                            if (CMD_URL_FORMAT.equals(CMD_URL_FORMAT2)) {
                                url = "http://api.ximiba.cn/proxy/iptv/putPort1.php?port=";
                            }
                            System.out.println("put url: " + url);
                            URL url2 = new URL(url + port);
                            connection3 = (HttpURLConnection) url2.openConnection();
                            connection3.setRequestMethod("GET");
                            connection3.setConnectTimeout(3000);
                            connection3.setReadTimeout(3000);
                            int responseCode2 = connection3.getResponseCode();
                            if (responseCode2 == 200) {
                                System.out.println("put 端口: " + port + " 成功");
                            }
                            continueScanning = false;
                        }
                    }
                } catch (InterruptedException e1) {
                    System.out.println("Interrupted");
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("异常端口: " + port);
                    if (process != null && process.isAlive()) {
                        process.destroy();
                        //processes.remove(process);
                    }
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                    if (connection2 != null) {
                        connection2.disconnect();
                    }
                    if (connection3 != null) {
                        connection3.disconnect();
                    }
                }
            }
        } catch (IOException e) {
            if (process != null && process.isAlive()) process.destroy();
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            if (process != null) process.destroy();
        }
        if (process != null && process.isAlive()) process.destroy();
    }
}
