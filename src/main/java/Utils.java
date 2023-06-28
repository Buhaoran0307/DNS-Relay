import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Utils {
    public static int SERVER_PORT = 53;
    public static String  LOCAL_DNS_ADDRESS = "202.106.0.20";
    public static HashMap<String,HashMap<String,Object>> cacheMap;

    static {
        cacheMap = new HashMap<>();
    }

    public static HashMap<String,Object> searchIPFromCache(String DNS){

        return new HashMap<>();
    }
    // 从文件中读取信息
    public static void readCacheFromFile() throws FileNotFoundException {
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
                }
                Object ipTerms;
                ipTerms = info.get(terms[2]);
                switch (terms[2]){
                    case "v4":

                        break;
                    case "v6":

                        break;
                }
                line = bufferedReader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
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
