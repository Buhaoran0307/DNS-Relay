import org.xbill.DNS.Message;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class DNSRequestHandler implements Runnable {
    private final DatagramPacket receivedPacket;
    private final DNS_Server dnsServer;

    public DNSRequestHandler(DatagramPacket receivedPacket, DNS_Server dnsServer){
        this.receivedPacket = receivedPacket;
        this.dnsServer =dnsServer;
    }
    @Override
    public void run() {
        Message request;
        try {
            request = new Message(this.receivedPacket.getData());
            System.out.println("捕获："+request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            UDPConnection relaySocket = new UDPConnection(0);
            relaySocket.sendMessage(InetAddress.getByName(Utils.LOCAL_DNS_ADDRESS),53,this.receivedPacket.getData());
            DatagramPacket relayRespond = relaySocket.receiveMessage();
            this.dnsServer.sendMessage(receivedPacket.getAddress(), receivedPacket.getPort(), relayRespond.getData());
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
