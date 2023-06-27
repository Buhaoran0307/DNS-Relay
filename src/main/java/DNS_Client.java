import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class DNS_Client extends UDPConnection {
    public DNS_Client(int port) throws SocketException{
        super(port);
    }
    public static void main(String[] args) throws SocketException, UnknownHostException {
        DNS_Client dnsClient = new DNS_Client(1413);
        dnsClient.sendMessage(InetAddress.getLocalHost(), Utils.SERVER_PORT, "Hello Server".getBytes());
        DatagramPacket packet = dnsClient.receiveMessage();
    }
}
