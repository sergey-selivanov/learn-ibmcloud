package sssii.billing.server;

import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.internal.process.MappableException;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class AppEventListener implements ApplicationEventListener {

    private Logger log = LoggerFactory.getLogger(AppEventListener.class);
    private RequestEventListener ls = new ExceptionRequestEventListener();

    @Override
    public void onEvent(ApplicationEvent event) {

        log.debug("onEvent: " + event.getType());

    }

    @Override
    public RequestEventListener onRequest(RequestEvent requestEvent) {

        return ls;
    }

    public static class ExceptionRequestEventListener implements RequestEventListener {

        private Logger log = LoggerFactory.getLogger(ExceptionRequestEventListener.class);

        @Override
        public void onEvent(RequestEvent event) {
            switch (event.getType()){
                case MATCHING_START:
                    //log.debug("request: " + event.getContainerRequest().getRequestUri());
                    break;
                case ON_EXCEPTION:
                    Throwable t = event.getException();
                    if(t instanceof MappableException){

                        //MappableException me = (MappableException)t;

                        log.error("mappable exception: " + t.getMessage());    // omit stack trace

//                        if(me.getCause() != null && me.getCause() instanceof AccessDeniedException){
//
//                            // must be some kind of audit
//
//                            log.debug("access denied for following request:");
//                            log.debug("uri: " + event.getContainerRequest().getAbsolutePath());
//                            //log.warn("permission violation: " + s.getUsername());
//                            for(Entry<String, List<String>> e: event.getContainerRequest().getRequestHeaders().entrySet()){
//                                for(String header: e.getValue()){
//                                    log.debug("header: " + e.getKey() + " = " + header);
//                                }
//                            }
//
//                        }
                    }
                    else{
                        log.error("got exception", t);
                    }
                    break;
                default:
                    //logger.debug("onRequest: " + event.getType() + ", " + event.getContainerRequest().getPath(true));
                    break;
            }
        }

    }

}
