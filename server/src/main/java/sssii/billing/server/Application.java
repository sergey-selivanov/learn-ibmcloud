package sssii.billing.server;

import java.io.IOException;
import java.util.Properties;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sssii.billing.common.security.AuthenticationTokenSettings;

@ApplicationPath("rest")
public class Application extends ResourceConfig
{
    private static Logger log = LoggerFactory.getLogger(Application.class);

    private static Properties options = new Properties();
    private static Properties version = new Properties();

    static {

        try {
            log.debug("loading options");
            options.load(Application.class.getResourceAsStream("/options.properties"));
            version.load(Application.class.getResourceAsStream("/version.properties"));
        } catch (IOException ex) {
            log.error("failed to load options", ex);
        }

        log.info("=================================================================");
        log.info("Billing Server " + version.getProperty("version", "unknown version"));
        log.info("rev  " + version.getProperty("git.commit", "unknown") + " " + version.getProperty("git.date", ""));
        log.info("built        " + version.getProperty("build.date", " at unknown date"));
        log.info("built by " + version.getProperty("build.host", "- host unknown"));
        log.info("build number " + version.getProperty("hudson.build.number", "unknown"));
        log.info(System.getProperty("java.vm.name") + " " + System.getProperty("java.runtime.version") + " " + System.getProperty("java.home"));
        log.info("-----------------------------------------------------------------");
    }

    public Application(){

        try {
            packages("sssii.billing.server");
            //register(RequestFilter.class);
            register(RolesAllowedDynamicFeature.class);

        }
        catch(Exception ex){
            log.error("boom", ex);
        }
    }

    public static Properties getOptions() {
        return options;
    }

    public static Properties getVersion() {
        return version;
    }

    public static AuthenticationTokenSettings getAuthTokenSettings() {
        AuthenticationTokenSettings settings= new AuthenticationTokenSettings();

        settings.setAudience("audience");
        settings.setAuthoritiesClaimName("authoritiesClaimName");
        settings.setClockSkew(10l);
        settings.setIssuer("issuer");
        settings.setRefreshCountClaimName("refreshCountClaimName");
        settings.setRefreshLimitClaimName("refreshLimitClaimName");
        settings.setSecret("secret");

        return settings;
    }

}
