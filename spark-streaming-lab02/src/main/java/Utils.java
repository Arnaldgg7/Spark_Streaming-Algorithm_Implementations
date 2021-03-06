import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.streaming.twitter.TwitterUtils;

import twitter4j.auth.Authorization;

public class Utils {

	static {
		Logger.getLogger("org.apache.spark").setLevel(Level.WARN);
		Logger.getLogger("org.apache.spark.storage.BlockManager").setLevel(
				Level.ERROR);
	}

	static void setupTwitter(String path) throws Exception {
		Properties props = new Properties();
		props.load(new FileReader(path));

		String consumerKey = props.getProperty("consumerKey");
		String consumerSecret = props.getProperty("consumerSecret");
		String accessToken = props.getProperty("accessToken");
		String accessTokenSecret = props.getProperty("accessTokenSecret");

		configureTwitterCredentials(consumerKey, consumerSecret, accessToken,
				accessTokenSecret);
	}

	static void configureTwitterCredentials(String apiKey, String apiSecret,
			String accessToken, String accessTokenSecret) throws Exception {
		HashMap<String, String> configs = new HashMap<String, String>();
		configs.put("apiKey", apiKey);
		configs.put("apiSecret", apiSecret);
		configs.put("accessToken", accessToken);
		configs.put("accessTokenSecret", accessTokenSecret);
		Object[] keys = configs.keySet().toArray();
		for (int k = 0; k < keys.length; k++) {
			String key = keys[k].toString();
			String value = configs.get(key).trim();
			if (value.isEmpty()) {
				throw new Exception("Error setting authentication - value for "
						+ key + " not set");
			}
			String fullKey = "twitter4j.oauth."
					+ key.replace("api", "consumer");
			System.setProperty(fullKey, value);
			System.out.println("\tProperty " + key + " set as [" + value + "]");
		}
		System.out.println();
	}
	
	static void setEnv(Map<String, String> newenv)
	{
	  try
	    {
	        Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
	        Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
	        theEnvironmentField.setAccessible(true);
	        Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
	        env.putAll(newenv);
	        Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
	        theCaseInsensitiveEnvironmentField.setAccessible(true);
	        Map<String, String> cienv = (Map<String, String>)     theCaseInsensitiveEnvironmentField.get(null);
	        cienv.putAll(newenv);
	    }
	    catch (NoSuchFieldException e)
	    {
	      try {
	        Class[] classes = Collections.class.getDeclaredClasses();
	        Map<String, String> env = System.getenv();
	        for(Class cl : classes) {
	            if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
	                Field field = cl.getDeclaredField("m");
	                field.setAccessible(true);
	                Object obj = field.get(env);
	                Map<String, String> map = (Map<String, String>) obj;
	                map.clear();
	                map.putAll(newenv);
	            }
	        }
	      } catch (Exception e2) {
	        e2.printStackTrace();
	      }
	    } catch (Exception e1) {
	        e1.printStackTrace();
	    } 
	}

}
