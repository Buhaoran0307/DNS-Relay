import java.net.*;

public class DNS_Client extends UDPConnection {
    public DNS_Client(int port) throws SocketException{
        super(port);
    }
    public static void main(String[] args) throws SocketException, UnknownHostException {
        DNS_Client dnsClient = new DNS_Client(1413);
        dnsClient.sendMessage(InetAddress.getLocalHost(), Utils.SERVER_PORT, "Hello Server");
        DatagramPacket packet = dnsClient.receiveMessage();
    }
}
