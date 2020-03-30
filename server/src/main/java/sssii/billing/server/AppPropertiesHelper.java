package sssii.billing.server;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppPropertiesHelper {

    private Logger logger = LoggerFactory.getLogger(AppPropertiesHelper.class);

    private String version = "undefined";
    private String buildNumber = "undefined";

    private Properties props = new Properties();

    public AppPropertiesHelper(){


        try {
            props.load(getClass().getResourceAsStream("/version.properties"));

            if(!props.getProperty("version").startsWith("@")){
                version = props.getProperty("version");
            }
            else{
                version = "current";
            }

            if(!props.getProperty("jenkins.build.number").startsWith("@")){
                buildNumber = props.getProperty("jenkins.build.number");
            }
            else{
                buildNumber = ""; // not substituted, probably not under hudson, use blank
            }
        }
        catch(IOException e) {
            logger.error("Failed to read version.properties", e);
        }

        logger.info("Loaded version: " + version + ", build #: " + buildNumber);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String ver) {
        this.version = ver;
    }

    private String signature = null;

    public String getHtmlBuildAndVersion(){
        if(signature == null){
            signature = "Version " + version;

            signature += ", Git rev: " + props.getProperty("git.commit", "unknown");

            if(!buildNumber.isEmpty()){
                signature += ", Jenkins build #" + buildNumber;
            }
        }

        return signature;
    }
}
