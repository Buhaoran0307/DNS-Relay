import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws SocketException, UnknownHostException {
        Utils.readCacheFromFile();
        Utils.readBannedListFromFile();

        DNS_Server dnsServer = new DNS_Server(Utils.SERVER_ADDRESS,Utils.SERVER_PORT);
        dnsServer.service();

        (new cacheSavingTimer()).start(10);

        Scanner scanner = new Scanner(System.in);
        while (true){
            if (scanner.next().equals("stop")){
                dnsServer.stop();
                break;
            }
        }
        System.exit(0);
    }
}
