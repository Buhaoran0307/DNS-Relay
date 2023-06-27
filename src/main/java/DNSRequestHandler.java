import org.xbill.DNS.Message;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;

import java.io.IOException;
import java.net.*;
import java.util.List;

public class DNSRequestHandler implements Runnable {
    private final DatagramPacket receivedPacket;
    private final DNS_Server dnsServer;

    public DNSRequestHandler(DatagramPacket receivedPacket, DNS_Server dnsServer){
        this.receivedPacket = receivedPacket;
        this.dnsServer =dnsServer;
    }
    @Override
    public void run() {
        Message clientRequest;
        try {
            clientRequest = new Message(this.receivedPacket.getData());
            List<Record> records = clientRequest.getSection(Section.QUESTION);
            for(Record record : records){
                System.out.println("record   " + record);
                System.out.println("type     " + record.getType());
                if(check(record)){
                    localQuest();
                }else{
                    remoteQuest();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean check(Record record) {
        switch (record.getType()){
            case 1:

                return true;
            case 28:

                return true;
            default:
                return false;
        }
    }

    private void localQuest(){
        try {
            UDPConnection relaySocket = new UDPConnection(0);
            relaySocket.sendMessage(InetAddress.getByName(Utils.LOCAL_DNS_ADDRESS),53,this.receivedPacket.getData());
            DatagramPacket relayRespond = relaySocket.receiveMessage();
            this.dnsServer.sendMessage(receivedPacket.getAddress(), receivedPacket.getPort(), relayRespond.getData());
            Message messageResponse = new Message(relayRespond.getData());
            //System.out.println(messageResponse);
            List<Record> records = messageResponse.getSection(Section.ANSWER);
            //System.out.println(records);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void remoteQuest(){
        try {
            UDPConnection relaySocket = new UDPConnection(0);
            relaySocket.sendMessage(InetAddress.getByName(Utils.LOCAL_DNS_ADDRESS),53,this.receivedPacket.getData());
            DatagramPacket relayRespond = relaySocket.receiveMessage();
            this.dnsServer.sendMessage(receivedPacket.getAddress(), receivedPacket.getPort(), relayRespond.getData());
            Message messageResponse = new Message(relayRespond.getData());
            //System.out.println(messageResponse);
            List<Record> records = messageResponse.getSection(Section.ANSWER);
            //System.out.println(records);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
