package sssii.billing.server.security;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
//import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@Provider
@Priority(Priorities.AUTHORIZATION)
public class AuthorizationFilter implements ContainerRequestFilter
{
    private Logger log = LoggerFactory.getLogger(AuthorizationFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        log.debug("filter");

    }

}
