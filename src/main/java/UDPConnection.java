
import java.io.IOException;
import java.net.*;

public class UDPConnection {
    protected final DatagramSocket datagramSocket;
    public UDPConnection(String address,int port) throws SocketException, UnknownHostException {
        InetAddress ipAddress = InetAddress.getByName(address);
        this.datagramSocket = new DatagramSocket(new InetSocketAddress(ipAddress, port));
    }
    public DatagramPacket receiveMessage(){
        try {
            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
            datagramSocket.receive(packet);
            return packet;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public synchronized void sendMessage(InetAddress address, int port, byte[] message){
        DatagramPacket sentPacket = new DatagramPacket(new byte[1024], 1024);
        sentPacket.setData(message);
        sentPacket.setPort(port);
        sentPacket.setAddress(address);
        try {
            this.datagramSocket.send(sentPacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
