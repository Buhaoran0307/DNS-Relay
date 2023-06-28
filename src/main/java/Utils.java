import java.io.*;
import java.util.*;

public class Utils {
    public static int SERVER_PORT = 53;
    public static String  LOCAL_DNS_ADDRESS = "202.106.0.20";
    public static volatile HashMap<String,HashMap<String,Object>> cacheMap;
    public static ArrayList<String> bannedList;

    static {
        cacheMap = new HashMap<>();
        bannedList = new ArrayList<>();
    }

    public static HashMap<String,Object> searchIPFromCache(String DNS){
        return cacheMap.get(DNS);
    }

    // 从文件中读取信息
    public static void readCacheFromFile(){
        /*try (FileReader fileReader = new FileReader("src/main/resources/cache.txt")) {
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            String[] terms;
            String DN;
            HashMap<String,Object> info = new HashMap<>();
            String type;
            String[] ips;
            line = bufferedReader.readLine();
            while (line != null){
                terms = line.split(" ");
                DN = terms[1];
                type = terms[2];
                ips = Arrays.copyOfRange(terms, 3, terms.length);
                info.put(type,ips);
                cacheMap.put(DN,info);
                line = bufferedReader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
        try (FileReader fileReader = new FileReader("src/main/resources/cache.txt")) {
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            String[] terms;
            String DN;
            HashMap<String,Object> info;
            line = bufferedReader.readLine();
            while (line != null){
                terms = line.split(" ");
                DN = terms[1];
                info = cacheMap.get(DN);
                if (info == null){
                    info = new HashMap<>();
                    info.put("v4",new ArrayList<>());
                    info.put("v6",new ArrayList<>());
                    info.put("timeout",terms[0]);
                    cacheMap.put(DN,info);
                }
                ArrayList<String> ipTerms;
                ipTerms = castList(info.get(terms[2]),String.class);
                ipTerms.addAll(Arrays.asList(terms).subList(3, terms.length));
                info.put(terms[2],ipTerms);
                line = bufferedReader.readLine();
            }
            fileReader.close();
            bufferedReader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void writeCacheToFile(){
        try (FileWriter fileWriter = new FileWriter("src/main/resources/cache.txt")) {
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            Set<String> keySet = cacheMap.keySet();
            Iterator<String> kepIterator = keySet.iterator();
            String DN;
            HashMap<String,Object> info;
            StringBuilder line;
            String base;
            ArrayList<String> ipTerms;
            while (kepIterator.hasNext()){
                line = new StringBuilder();
                DN = kepIterator.next();
                info = cacheMap.get(DN);
                line.append(info.get("timeout")).append(" ");
                line.append(DN).append(" ");
                base = line.toString();
                bufferedWriter.write(base);
                bufferedWriter.write("v4 ");
                ipTerms = castList(info.get("v4"), String.class);
                for (String s : ipTerms){
                    bufferedWriter.write(s);
                    bufferedWriter.write(" ");
                }
                bufferedWriter.newLine();
                bufferedWriter.write(base);
                bufferedWriter.write("v6 ");
                ipTerms = castList(info.get("v6"), String.class);
                for (String s : ipTerms){
                    bufferedWriter.write(s);
                    bufferedWriter.write(" ");
                }
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
            fileWriter.close();
            bufferedWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void readBannedListFromFile(){
        try (FileReader fileReader = new FileReader("src/main/resources/bannedList.txt")) {
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            line = bufferedReader.readLine();
            while (line != null){
                bannedList.add(line);
                line = bufferedReader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void writeBannedListToFile(){
        try (FileWriter fileWriter = new FileWriter("src/main/resources/bannedList.txt")) {
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            for (String s : bannedList){
                bufferedWriter.write(s);
                bufferedWriter.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // List<String> list = castList(obj, String.class);
    public static <T> ArrayList<T> castList(Object obj, Class<T> clazz){
        ArrayList<T> result = new ArrayList<>();
        if(obj instanceof ArrayList<?>){
            for (Object o : (ArrayList<?>) obj){
                result.add(clazz.cast(o));
            }
            return result;
        }
        return new ArrayList<>();
    }

}
