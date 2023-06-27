import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UDPConnection {
    protected final DatagramSocket datagramSocket;
    public UDPConnection(int port) throws SocketException {
        this.datagramSocket = new DatagramSocket(port);
    }
    public DatagramPacket receiveMessage(){
        try {
            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
            datagramSocket.receive(packet);
            System.out.println(packet.getAddress().getHostName() + "(" + packet.getPort() + "):" + new String(packet.getData(), 0, packet.getLength()));
            return packet;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public void sendMessage(InetAddress address, int port, String message){
        DatagramPacket sentPacket = new DatagramPacket(new byte[1024], 1024);
        sentPacket.setData(message.getBytes());
        sentPacket.setPort(port);
        sentPacket.setAddress(address);
        try {
            this.datagramSocket.send(sentPacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
