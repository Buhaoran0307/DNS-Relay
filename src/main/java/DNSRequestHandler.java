import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DNSRequestHandler implements Runnable {
    private final DatagramPacket receivedPacket;
    private final DNS_Server dnsServer;
    private final Object cacheLock = new Object();

    public DNSRequestHandler(DatagramPacket receivedPacket, DNS_Server dnsServer){
        this.receivedPacket = receivedPacket;
        this.dnsServer =dnsServer;
    }
    @Override
    public void run() {
        Message clientRequest;
        Message relayResponse;
        //Utils.readCacheFromFile();
        try {
            clientRequest = new Message(this.receivedPacket.getData());
            relayResponse = clientRequest.clone();
            List<Record> records = clientRequest.getSection(Section.QUESTION);
            List<Record> relayRecords = localQuest(records);
            if (relayRecords.isEmpty()){
                relayRecords = remoteQuest();
            }
            for (Record record : relayRecords){
                relayResponse.addRecord(record,Section.ANSWER);
            }
            this.dnsServer.sendMessage(receivedPacket.getAddress(), receivedPacket.getPort(), relayResponse.toWire());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private List<Record> localQuest(List<Record> records){
        List<Record> replayRecord = new ArrayList<>();
        InetAddress answerIp;
        Record responseRecord;
        String[] ipString;
        for(Record record : records){
            if (!record.getName().toString().equals("1.0.0.127.in-addr.arpa.")){
                if (Utils.cacheMap.containsKey(record.getName().toString())){
                    HashMap<String,Object> searchMap = Utils.searchIPFromCache(record.getName().toString());
                    switch (record.getType()) {
                        case 1 -> {
                            ipString = (String[]) searchMap.get("v4");
                            for (String i : ipString) {
                                try {
                                    answerIp = InetAddress.getByName(i);
                                    responseRecord = new ARecord(record.getName(), record.getDClass(), 64, answerIp);
                                    replayRecord.add(responseRecord);
                                } catch (UnknownHostException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                        case 28 -> {
                            ipString = (String[]) searchMap.get("v6");
                            for (String i : ipString) {
                                try {
                                    answerIp = InetAddress.getByName(i);
                                    responseRecord = new AAAARecord(record.getName(), record.getDClass(), 64, answerIp);
                                    replayRecord.add(responseRecord);
                                } catch (UnknownHostException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                    System.out.println("In the cache!");
                }else {
                    System.out.println("Not exist in cache!");
                }
            }
        }
        return replayRecord;
    }
    private List<Record> remoteQuest(){
        try {
            UDPConnection relaySocket = new UDPConnection(0);
            relaySocket.sendMessage(InetAddress.getByName(Utils.LOCAL_DNS_ADDRESS),53,this.receivedPacket.getData());
            DatagramPacket relayRespond = relaySocket.receiveMessage();
            Message replay  = new Message(relayRespond.getData());
            return replay.getSection(Section.ANSWER);
            //this.dnsServer.sendMessage(receivedPacket.getAddress(), receivedPacket.getPort(), relayRespond.getData());
            //Message messageResponse = new Message(relayRespond.getData());
            //System.out.println(messageResponse);
            //List<Record> records = messageResponse.getSection(Section.ANSWER);
            //System.out.println(records);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}