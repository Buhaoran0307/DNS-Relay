import java.net.SocketException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws SocketException {
        Utils.readCacheFromFile();
        Utils.readBannedListFromFile();

        DNS_Server dnsServer = new DNS_Server(Utils.SERVER_PORT);
        dnsServer.service();

        (new cacheSavingTimer()).start(5);

        Scanner scanner = new Scanner(System.in);
        while (true){
            if (scanner.next().equals("stop")){
                dnsServer.stop();
                break;
            }
        }
    }
}
