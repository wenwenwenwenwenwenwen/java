//package org.example;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PortScanner1 {

    private static final String CMD_URL_FORMAT = "http://127.0.0.1:%d/cmd.xml?cmd=switch_chan&id=5ff7074800001bcccbe59f3d4ff00590&server=108.181.20.159:%d";
    private static final String PP_SHELL_CMD_FORMAT = "D:\\a\\java\\java\\ForceP2PMediaPlayer\\ppshell.exe -o %d"; // 假设ppshell.exe在PATH环境变量中可找到
    //private static final String PP_SHELL_CMD_FORMAT = "C:\\ForceP2P Media Player\\ppshell.exe -o %d"; // 假设ppshell.exe在PATH环境变量中可找到
    private static List<Process> processes = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        //while (true) {
            List<Integer> except = new ArrayList<>();
            int startPort = 10001;
            int endPort = 65352;
            doThred(startPort,endPort,except);
           /* if (except.size()>0){
                doThred(10001,10001,except);
            }*/
            //Thread.sleep(3600*1000*4); // 每隔2小时执行一次任务
            //Thread.sleep(60*4*1000); // 每隔10分钟执行一次任务
        //}
    }
    private static  void doThred(int startPort,int endPort,List<Integer> excepts){
        ExecutorService executor = Executors.newFixedThreadPool(30); // 创建线程池
        if(excepts.size()==0){
            for (int port = startPort; port <= endPort; port++) {
                int finalPort = port;
                executor.submit(() -> scanAndStartPort(finalPort,excepts,executor));
                if(port>=endPort){
                    executor.shutdown();
                }
            }
        }else{
            int exceptSize = excepts.size();
            for (int i=0; i<exceptSize;i++) {
                int finalPort = excepts.get(i);
                executor.submit(() -> scanAndStartPort(finalPort,excepts,executor));
                if(i>=exceptSize-1)executor.shutdown();
            }
        }

        while (!executor.isTerminated()) {
            try {
                Thread.sleep(10000); // 短暂休眠减少CPU占用
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        try {
            Thread.sleep(120000); // 等待2分钟再资源回收
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }        System.out.println("进行exe资源回收 ！");
        String processName = "ppshell.exe";
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("taskkill", "/F", "/IM", processName);
            Process process = processBuilder.start();
        } catch (IOException e) {
            System.err.println("执行命令时发生错误: " + e.getMessage());
        }
    }

    private static void scanAndStartPort(int port,List<Integer> except,ExecutorService executor) {
        Process process =null;
        try {
            int exePort=port;
            if(port>65352) exePort=port-50000;
           String command = String.format(PP_SHELL_CMD_FORMAT, exePort);
           process = Runtime.getRuntime().exec(command);
            Thread.sleep(2000); // 等待3秒
            if(process.isAlive()){
                processes.add(process);
                HttpURLConnection connection = null;
                HttpURLConnection connection2 = null;
                HttpURLConnection connection3 = null;
                try {
                    URL url1 = new URL(String.format(CMD_URL_FORMAT, exePort, port));
                    connection = (HttpURLConnection) url1.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(3000);
                    connection.setReadTimeout(3000);
                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200) {
                        Thread.sleep(2500); // 等待3秒
                        try {
                            URL url2 = new URL(String.format(CMD_URL_FORMAT, exePort, port));
                            connection2 = (HttpURLConnection) url2.openConnection();
                            connection2.setRequestMethod("GET");
                            connection2.setConnectTimeout(3000);
                            connection2.setReadTimeout(3000);
                            int responseCode2 = connection2.getResponseCode();
                            if (responseCode2 != 200) {
                            }else{
                                // 终止当前线程并立即退出
                                if(process.isAlive()){
                                    process.destroy();
                                    processes.remove(process);
                                }
                                // 获取当前时间的毫秒值
                                long currentTimeMillis = System.currentTimeMillis();
                                // 将毫秒值转换为 LocalDateTime
                                LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMillis), ZoneId.systemDefault());
                                // 定义日期时间格式
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                // 格式化输出
                                String formattedDateTime = localDateTime.format(formatter);
                                System.out.println(formattedDateTime+" Scan port: " + port);
                                 if(process !=null && process.isAlive()){
                                    process.destroy();
                                    //processes.remove(process);
                                }
                            }
                        }catch (ConnectException e){
                            System.out.println("Found port: " + port);
                            URL url2 = new URL("http://api.ximiba.cn/proxy/iptv/putPort1.php?port="+port);
                            connection3 = (HttpURLConnection) url2.openConnection();
                            connection3.setRequestMethod("GET");
                            connection3.setConnectTimeout(3000);
                            connection3.setReadTimeout(3000);
                            int responseCode2 = connection3.getResponseCode();
                            if (responseCode2 == 200) {
                                System.out.println("put port: " + port+"success");
                            }
                            executor.shutdownNow();
                        }

                    }
                }catch (InterruptedException e1){
                    System.out.println("interrput2");
                    Thread.currentThread().interrupt();
                  /*  if(process.isAlive()){
                        process.destroy();
                        processes.remove(process);
                    }*/
                  /*  for (Process process1 : processes) {
                        if(process1.isAlive())process1.destroy();
                    }*/
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Except port: " + port);
                    except.add(port);
                    if(process!=null && process.isAlive()){
                        process.destroy();
                        processes.remove(process);
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
            e.printStackTrace();
        }catch (InterruptedException e) {
            System.out.println("interrput1");
            Thread.currentThread().interrupt();
           /* for (Process process1 : processes) {
                if(process1.isAlive())process1.destroy();
            }*/
        }catch (Exception e){
            /*for (Process process1 : processes) {
                if(process1.isAlive())process1.destroy();
            }*/
        }
    }
}
