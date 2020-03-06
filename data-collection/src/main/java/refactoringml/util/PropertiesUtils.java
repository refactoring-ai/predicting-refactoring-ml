package refactoringml.util;

import org.apache.log4j.Logger;
import refactoringml.App;
import java.io.FileInputStream;
import java.util.Properties;

public class PropertiesUtils {
    private static final Logger log = Logger.getLogger(App.class);

    //config properties for the data-collection app at resources/config.property
    private static Properties configProperties;
    private static String configName = "/config.properties";

    private static Properties fetchProperties(){
        if(configProperties!= null)
            return configProperties;

        String propertiesPath = PropertiesUtils.class.getResource(configName).getPath();
        configProperties = new Properties();
        try{
            configProperties.load(new FileInputStream(propertiesPath));
        } catch (Exception e) {
            log.error(e.getClass().getCanonicalName() + " while loading config file from: " + propertiesPath, e);
            throw new RuntimeException("Could not load config properties.");
        }
        return configProperties;
    }

    //query the config properties for the given config name
    public static String getProperty(String propertyName) {
        return fetchProperties().getProperty(propertyName);
    }

    //Only use this for tests
    @Deprecated
    public static Object setProperty(String propertyName, String propertyValue){
        return fetchProperties().setProperty(propertyName, propertyValue);
    }
}
