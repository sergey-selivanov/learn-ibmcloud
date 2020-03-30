package sssii.billing.server.security;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sssii.billing.common.entity.rs.User;
import sssii.billing.common.security.AuthenticatedUserDetails;
import sssii.billing.common.security.AuthenticationTokenDetails;
import sssii.billing.common.security.AuthenticationTokenService;
import sssii.billing.common.security.InvalidAuthenticationTokenException;
import sssii.billing.common.security.UserService;
import sssii.billing.server.Application;
import sssii.billing.server.security.misc.TokenBasedSecurityContext;


@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter
{
    private Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);


    AuthenticationTokenService authenticationTokenService = new AuthenticationTokenService(Application.getAuthTokenSettings());
    UserService userService = new LdapUserService();

    @Override
    public void filter(ContainerRequestContext requestContext) {

//        log.debug("filter");

        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String authenticationToken = authorizationHeader.substring(7);
            handleTokenBasedAuthentication(authenticationToken, requestContext);
            return;
        }
    }

    private void handleTokenBasedAuthentication(String authenticationToken, ContainerRequestContext requestContext) {

        try {
            AuthenticationTokenDetails authenticationTokenDetails = authenticationTokenService.parseToken(authenticationToken);
            User user = userService.findByUsername(authenticationTokenDetails.getUsername());
            AuthenticatedUserDetails authenticatedUserDetails = new AuthenticatedUserDetails(user.getUsername(), user.getAuthorities());

            boolean isSecure = requestContext.getSecurityContext().isSecure();
            SecurityContext securityContext = new TokenBasedSecurityContext(authenticatedUserDetails, authenticationTokenDetails, isSecure);
            requestContext.setSecurityContext(securityContext);
        }
        catch(InvalidAuthenticationTokenException e) {
            log.debug("invalid token: " + e.getMessage());
        }
    }
}
