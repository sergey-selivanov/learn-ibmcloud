package sssii.billing.server;

import java.io.IOException;

import javax.servlet.annotation.WebListener;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@WebListener("")
public class RequestFilter implements ContainerRequestFilter {

    private static Logger log = LoggerFactory.getLogger(RequestFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        MultivaluedMap<String, String> headers = requestContext.getHeaders();
        for(String header: headers.keySet()) {
            log.debug(header + ": " + headers.getFirst(header));
        }
    }

}
