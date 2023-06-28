import java.net.DatagramPacket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class DNS_Server extends UDPConnection implements Runnable{
    private Thread serviceTread;
    private final ThreadPoolExecutor executorPool;

    public DNS_Server(int port) throws SocketException {
        super(port);
        this.executorPool = new ThreadPoolExecutor(50, 100, 10,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(50),
                Executors.defaultThreadFactory(),
                new RejectedExecutionHandlerImpl());
    }

    public void service(){
        this.serviceTread = new Thread(this);
        this.serviceTread.setDaemon(true);
        this.serviceTread.start();
    }
    public void stop(){
        this.serviceTread.interrupt();
        this.serviceTread = null;
    }
    @Override
    public void run() {
        while (!this.serviceTread.isInterrupted()){
            DatagramPacket receivedPacket = this.receiveMessage();
            executorPool.execute(new DNSRequestHandler(receivedPacket,this));
        }
    }
    public static void main(String[] args) throws SocketException {
        DNS_Server dnsServer = new DNS_Server(Utils.SERVER_PORT);
        dnsServer.service();

        (new cacheSavingTimer()).start(2);

        Scanner scanner = new Scanner(System.in);
        while (true){
            if (scanner.next().equals("stop")){
                dnsServer.stop();
            }
        }
    }
}
class RejectedExecutionHandlerImpl implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        System.out.println(r.toString() + " is rejected");
    }

}

class cacheSavingTimer extends TimerTask{
    private Timer timer;
    public void run() {
        //编写所需要的任务
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String strTime = sdf.format(new Date());
        System.out.println(strTime);
    }
    public void start(long second){
        this.timer = new Timer();
        timer.schedule(this, new Date(), second*1000);
    }
    public void stop(){
        this.timer.cancel();
    }
}
