import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

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
        Message relayResponse;
        try {
            clientRequest = new Message(this.receivedPacket.getData());
            relayResponse = clientRequest.clone();
            List<Record> records = clientRequest.getSection(Section.QUESTION);
            String domain = clientRequest.getQuestion().getName().toString();
            HashMap<String,Object> info = new HashMap<>();
            String ip;
            ArrayList<String> ipv4 = new ArrayList<>();
            ArrayList<String> ipv6 = new ArrayList<>();
            List<Record> relayRecords = localQuest(records);
            if (relayRecords.isEmpty()){
                relayRecords = remoteQuest();
                for (Record record : relayRecords){
                    //System.out.println(">>>>>>>>>>>"+record.getType());
                    switch (record.getType()){
                        case 1 -> {
                            ARecord aRecord = (ARecord)record;
                            ip = aRecord.getAddress().getHostAddress();
                            ipv4.add(ip);
                        }
                        case 28 -> {
                            AAAARecord aaaaRecord = (AAAARecord)record;
                            ip = aaaaRecord.getAddress().getHostAddress();
                            ipv6.add(ip);
                            //System.out.println("============"+ipv6);
                        }
                    }
                }
                info.put("v4",ipv4);
                info.put("v6",ipv6);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
                String strTime = sdf.format(new Date());
                info.put("timeout",strTime);
                Utils.cacheMap.put(domain,info);
                //System.out.println(Utils.cacheMap);
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
        ArrayList<String> ipString;
        for(Record record : records){
            if (!record.getName().toString().equals("1.0.0.127.in-addr.arpa.")){
                if (Utils.cacheMap.containsKey(record.getName().toString())){
                    HashMap<String,Object> searchMap = Utils.searchIPFromCache(record.getName().toString());
                    switch (record.getType()) {
                        case 1 -> {
                            ipString = Utils.castList(searchMap.get("v4"), String.class);
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
                            ipString = Utils.castList(searchMap.get("v6"), String.class);
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}