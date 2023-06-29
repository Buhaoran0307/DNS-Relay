import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
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
        boolean ipv6Flag = false;

        try {
            clientRequest = new Message(this.receivedPacket.getData());
            relayResponse = clientRequest.clone();
            Record recordsQuest = clientRequest.getQuestion();
            String domain = clientRequest.getQuestion().getName().toString();
            if (domain.equals("1.0.0.127.in-addr.arpa.")){
                domain = "";
            }
            HashMap<String,Object> info = Utils.cacheMap.get(domain);;
            ArrayList<String> ipv4 = new ArrayList<>();
            ArrayList<String> ipv6 = new ArrayList<>();
            ArrayList<InetAddress> relayIps = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            Date today = new Date();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(today);
            calendar.add(Calendar.DATE, 1);
            String strTime = sdf.format(today);
            switch (recordsQuest.getType()){
                case 12 -> {

                }
                case 1 -> {
                    if (info == null || strTime.compareTo((String) info.get("timeout")) >= 0){
                        if(info == null){
                            System.out.println("Not in cacheMap!");
                        }else {
                            System.out.println("cache timeout!");
                            Utils.cacheMap.remove(domain);
                        }
                        info = new HashMap<>();
                        relayIps = remoteQuest();
                        for (InetAddress inetAddress : relayIps){
                            ipv4.add(inetAddress.getHostAddress());
                        }
                        info.put("v4",ipv4);
                        info.put("timeout",sdf.format(calendar.getTime()));
                        Utils.cacheMap.put(domain,info);
                    }else {
                        System.out.println("In cacheMap!");
                        ArrayList<String> v4Strings = Utils.castList(info.get("v4"), String.class);
                        if (v4Strings.isEmpty()) {
                            System.out.println("Do not have ipv4 cache!");
                            relayIps = remoteQuest();
                            for (InetAddress inetAddress : relayIps) {
                                ipv4.add(inetAddress.getHostAddress());
                            }
                            info.put("v4", ipv4);
                            Utils.cacheMap.put(domain, info);
                        } else {
                            relayIps = localQuest(recordsQuest, info);
                        }
                    }
                }
                case 28 -> {
                    if (info == null || strTime.compareTo((String) info.get("timeout")) >= 0){
                        if(info == null){
                            System.out.println("Not in cacheMap!");
                        }else {
                            System.out.println("cache timeout!");
                            Utils.cacheMap.remove(domain);
                        }
                        info = new HashMap<>();
                        System.out.println("Not in cacheMap!");
                        relayIps = remoteQuest();
                        for (InetAddress inetAddress : relayIps){
                            ipv6.add(inetAddress.getHostAddress());
                        }
                        info.put("v6",ipv6);
                        info.put("timeout",sdf.format(calendar.getTime()));
                        Utils.cacheMap.put(domain,info);
                    }else {
                        System.out.println("In cacheMap!");
                        info = Utils.cacheMap.get(domain);
                        ArrayList<String> v6Strings = Utils.castList(info.get("v6"),String.class);
                        if (v6Strings.isEmpty()){
                            System.out.println("Do not have ipv6 cache!");
                            relayIps = remoteQuest();
                            for (InetAddress inetAddress : relayIps){
                                ipv6.add(inetAddress.getHostAddress());
                            }
                            info.put("v6",ipv6);
                            Utils.cacheMap.put(domain,info);
                        }
                        relayIps = localQuest(recordsQuest,info);
                    }
                }
            }
            for (InetAddress inetAddress : relayIps){
                Record record = null;
                switch (recordsQuest.getType()){
                    case 1 -> {
                        record = new ARecord(recordsQuest.getName(),recordsQuest.getDClass(),64,inetAddress);
                    }
                    case 28 -> {
                        record = new AAAARecord(recordsQuest.getName(),recordsQuest.getDClass(),64,inetAddress);
                    }
                }
                relayResponse.addRecord(record,Section.ANSWER);
            }
            this.dnsServer.sendMessage(receivedPacket.getAddress(), receivedPacket.getPort(), relayResponse.toWire());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private ArrayList<InetAddress> localQuest(Record record, HashMap<String,Object> info){
        ArrayList<InetAddress> replayIps = new ArrayList<>();
        InetAddress answerIp;
        Record responseRecord;
        ArrayList<String> ipString = new ArrayList<>();

        switch (record.getType()) {
            case 1 -> {
                ipString = Utils.castList(info.get("v4"), String.class);
            }
            case 28 -> {
                ipString = Utils.castList(info.get("v6"), String.class);
            }
        }
        for (String i : ipString) {
            try {
                answerIp = InetAddress.getByName(i);
                replayIps.add(answerIp);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
        return replayIps;
    }
    private ArrayList<InetAddress> remoteQuest(){
        try {
            UDPConnection relaySocket = new UDPConnection(0);
            relaySocket.sendMessage(InetAddress.getByName(Utils.LOCAL_DNS_ADDRESS),53,this.receivedPacket.getData());
            DatagramPacket relayRespond = relaySocket.receiveMessage();
            Message replay  = new Message(relayRespond.getData());
            ArrayList<InetAddress> relayIps = new ArrayList<>();
            for (Record record : replay.getSection(Section.ANSWER)){
                switch (record.getType()) {
                    case 1 -> {
                        ARecord aRecord = (ARecord)record;
                        relayIps.add(aRecord.getAddress());
                    }
                    case 28 -> {
                        AAAARecord aaaaRecord = (AAAARecord) record;
                        relayIps.add(aaaaRecord.getAddress());
                    }
                }
            }
            return relayIps;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}