import org.apache.log4j.Logger;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class DNSRequestHandler implements Runnable {
    private final DatagramPacket receivedPacket;
    private final DNS_Server dnsServer;
    private static final Logger logger = Logger.getLogger(DNSRequestHandler.class);

    public DNSRequestHandler(DatagramPacket receivedPacket, DNS_Server dnsServer) {
        this.receivedPacket = receivedPacket;
        this.dnsServer = dnsServer;
    }

    @Override
    public void run() {
        Message clientRequest;
        Message relayResponse;
        try {
            clientRequest = new Message(this.receivedPacket.getData());
            relayResponse = clientRequest.clone();
            Record recordsQuest = clientRequest.getQuestion();
            String domain = clientRequest.getQuestion().getName().toString();
            logger.debug("已捕获DNS解析请求    域名: " + domain);
            if(Utils.bannedList.contains(domain)){
                logger.info("域名: ["+domain+"] 已被禁止访问");
                Header header = relayResponse.getHeader();
                header.setRcode(3);
                relayResponse.setHeader(header);
                this.dnsServer.sendMessage(receivedPacket.getAddress(), receivedPacket.getPort(), relayResponse.toWire());
                return;
            }
            if (domain.equals("1.0.0.127.in-addr.arpa.")) {
                logger.info("反向IP地址解析，不做进一步响应。");
                this.dnsServer.sendMessage(receivedPacket.getAddress(), receivedPacket.getPort(), new byte[1024]);
                return;
            }
            ArrayList<String> ipv4 = new ArrayList<>();
            ArrayList<String> ipv6 = new ArrayList<>();
            ArrayList<InetAddress> relayIps = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            Date today = new Date();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(today);
            calendar.add(Calendar.DATE, 1);
            String strTime = sdf.format(today);
            HashMap<String, Object> info = Utils.cacheMap.get(domain);
            if (info == null || strTime.compareTo((String) info.get("timeout")) >= 0) {
                if (info == null) {
                    logger.info("首次查询该域名! 正在远程获取...");
                } else {
                    logger.info("缓存中该域名信息已超时！");
                    Utils.cacheMap.remove(domain);
                }
                info = new HashMap<>();
                info.put("timeout", sdf.format(calendar.getTime()));
                Utils.cacheMap.put(domain, info);
            }else {
                logger.info("已在缓存中命中该域名！");
            }
            switch (recordsQuest.getType()) {
                case 1 -> {
                    logger.info("获取ipv4地址...");
                    ArrayList<String> v4Strings = Utils.castList(info.get("v4"), String.class);
                    if (v4Strings.isEmpty()) {
                        logger.info("缓存中缺少该域名ipv4地址，正在远程获取...");
                        relayIps = remoteQuest(info);
                        if((int)info.get("RCode") == 3){
                            logger.info("该域名不存在 !");
                            Utils.bannedList.add(domain);
                            Utils.cacheMap.remove(domain);
                            break;
                        }
                        for (InetAddress inetAddress : relayIps) {
                            ipv4.add(inetAddress.getHostAddress());
                        }
                        info.put("v4", ipv4);
                        Utils.cacheMap.put(domain, info);
                    } else {
                        relayIps = localQuest(recordsQuest, info);
                    }
                    logger.info("获取ipv4地址...done");
                }
                case 28 -> {
                    logger.info("获取ipv6地址...");
                    info = Utils.cacheMap.get(domain);
                    ArrayList<String> v6Strings = Utils.castList(info.get("v6"), String.class);
                    if (v6Strings.isEmpty()) {
                        logger.info("缓存中缺少该域名ipv6地址，正在远程获取...");
                        relayIps = remoteQuest(info);
                        if((int)info.get("RCode") == 3){
                            logger.info("该域名不存在 !");
                            Utils.bannedList.add(domain);
                            Utils.cacheMap.remove(domain);
                            break;
                        }
                        for (InetAddress inetAddress : relayIps) {
                            ipv6.add(inetAddress.getHostAddress());
                        }
                        info.put("v6", ipv6);
                        Utils.cacheMap.put(domain, info);
                    } else {
                        relayIps = localQuest(recordsQuest, info);
                    }
                    logger.info("获取ipv6地址...done");
                }
                default -> logger.info("非ipv4或ipv6请求，不做进一步响应。");
            }
            if((int)info.get("RCode")==3){
                Header header = relayResponse.getHeader();
                header.setRcode(3);
                relayResponse.setHeader(header);
            }else {
                for (InetAddress inetAddress : relayIps) {
                    Record record = null;
                    switch (recordsQuest.getType()) {
                        case 1 -> record = new ARecord(recordsQuest.getName(), recordsQuest.getDClass(), 64, inetAddress);
                        case 28 ->record = new AAAARecord(recordsQuest.getName(), recordsQuest.getDClass(), 64, inetAddress);
                    }
                    relayResponse.addRecord(record, Section.ANSWER);
                }
            }
            this.dnsServer.sendMessage(receivedPacket.getAddress(), receivedPacket.getPort(), relayResponse.toWire());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private ArrayList<InetAddress> localQuest(Record record, HashMap<String, Object> info) {
        ArrayList<InetAddress> replayIps = new ArrayList<>();
        InetAddress answerIp;
        ArrayList<String> ipString = new ArrayList<>();

        switch (record.getType()) {
            case 1 -> {
                ipString = Utils.castList(info.get("v4"), String.class);
                logger.info("获取" + ipString.size() + "个ipv4地址： " + ipString);
            }
            case 28 -> {
                ipString = Utils.castList(info.get("v6"), String.class);
                logger.info("获取" + ipString.size() + "个ipv6地址： " + ipString);
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

    private ArrayList<InetAddress> remoteQuest(HashMap<String,Object> info) {
        try(DatagramSocket relaySocket = new DatagramSocket(0)) {
            DatagramPacket relayRequest = new DatagramPacket(new byte[1024], 1024);
            relayRequest.setData(this.receivedPacket.getData());
            relayRequest.setPort(53);
            relayRequest.setAddress(InetAddress.getByName(Utils.LOCAL_DNS_ADDRESS));
            relaySocket.send(relayRequest);
            DatagramPacket relayRespond = new DatagramPacket(new byte[1024], 1024);
            relaySocket.receive(relayRespond);
            Message replay = new Message(relayRespond.getData());
            Header header = replay.getHeader();
            int rCode = header.getRcode();
            info.put("RCode",rCode);
            ArrayList<InetAddress> relayIps = new ArrayList<>();
            for (Record record : replay.getSection(Section.ANSWER)) {
                switch (record.getType()) {
                    case 1 -> {
                        ARecord aRecord = (ARecord) record;
                        relayIps.add(aRecord.getAddress());
                        logger.info("远程获取ipv4地址：" + aRecord.getAddress().getHostAddress());
                    }
                    case 28 -> {
                        AAAARecord aaaaRecord = (AAAARecord) record;
                        relayIps.add(aaaaRecord.getAddress());
                        logger.info("远程获取ipv6地址：" + aaaaRecord.getAddress().getHostAddress());
                    }
                }
            }
            return relayIps;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}