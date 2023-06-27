import java.net.DatagramPacket;
import java.net.SocketException;

public class DNS_Server extends UDPConnection {
    public DNS_Server(int port) throws SocketException {
        super(port);
    }

    public static void main(String[] args) throws SocketException {
        DNS_Server dnsServer = new DNS_Server(Utils.SERVER_PORT);
        DatagramPacket receivedPacket = dnsServer.receiveMessage();
        dnsServer.sendMessage(receivedPacket.getAddress(), receivedPacket.getPort(), "Hello Client");
    }
}
