import org.apache.log4j.Logger;

import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class DNS_Server extends UDPConnection implements Runnable{
    private static final Logger logger = Logger.getLogger(DNS_Server.class);
    private Thread serviceTread;
    private final ThreadPoolExecutor executorPool;

    public DNS_Server(String address,int port) throws SocketException, UnknownHostException {
        super(address,port);
        this.executorPool = new ThreadPoolExecutor(14, 28, 10,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(14),
                Executors.defaultThreadFactory(),
                new RejectedExecutionHandlerImpl());
        logger.debug("成功创建Relay服务实例     "+"Relay地址: "+address+"监听端口: "+port);
    }

    public void service(){
        this.serviceTread = new Thread(this);
        this.serviceTread.setDaemon(true);
        this.serviceTread.start();
        logger.info("Relay正在处理DNS请求...");
    }
    public void stop(){
        this.serviceTread.interrupt();
        this.serviceTread = null;
        executorPool.shutdown();
        logger.info("Relay服务已中止 !");
    }
    @Override
    public void run() {
        while (!this.serviceTread.isInterrupted()){
            DatagramPacket receivedPacket = this.receiveMessage();
            executorPool.execute(new DNSRequestHandler(receivedPacket,this));
        }
    }
}
class RejectedExecutionHandlerImpl implements RejectedExecutionHandler {
    private static final Logger logger = Logger.getLogger(RejectedExecutionHandlerImpl.class);
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        logger.error(r.toString() + " 被拒绝！ 线程池溢出！");
    }

}
class cacheSavingTimer extends TimerTask{
    private static final Logger logger = Logger.getLogger(cacheSavingTimer.class);
    private Timer timer;
    public void run() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Utils.writeCacheToFile();
        logger.debug(Utils.cacheMap);
        String strTime = sdf.format(new Date());
        logger.info("["+strTime+"] 正在保存缓存文件....done");

        Utils.readBannedListFromFile();
        strTime = sdf.format(new Date());
        logger.info("["+strTime+"] 正在检阅黑名单文件....done");
    }
    public void start(long second){
        this.timer = new Timer();
        timer.schedule(this, new Date(), second*1000);
        logger.info("Relay每 "+second+" 秒保存缓存文件！");
    }
    public void stop(){
        this.timer.cancel();
    }
}
