package javking.util;

import com.google.common.base.Strings;

import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

public class PropertiesLoadingService {
    private static final HashMap<String, String> stringHashMap = new HashMap<>();
//  used while program is running
    public static String requireProperty(String key) {
        String property = stringHashMap.get(key);
        if (!Strings.isNullOrEmpty(property)) {
            return property;
        } else if (!Strings.isNullOrEmpty((property = loadProperty(key)))) {
            return property;
        } else {
            throw new IllegalStateException("Property " + key + " not set");
        }
    }
//  only run during startup
    @Nullable
    public static String loadProperty(String key) {
        try {
            FileInputStream in = new FileInputStream("./src/main/resources/default.properties");
            Properties properties = new Properties();
            properties.load(in);
            in.close();

            String value = properties.getProperty(key);

            stringHashMap.put(key, value);

            return value;
        } catch (FileNotFoundException ignored) {
            String value = System.getenv(key);
            
            stringHashMap.put(key, value);

            return value;
        } catch (IOException e) {
            throw new RuntimeException("Exception while loading property " + key, e);
        }
    }
}
