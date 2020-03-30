package sssii.billing.server.security.misc;

import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sssii.billing.common.security.AuthenticatedUserDetails;
import sssii.billing.common.security.AuthenticationTokenDetails;

import java.security.Principal;

/**
 * {@link SecurityContext} implementation for token-based authentication.
 *
 * @author cassiomolin
 */
public class TokenBasedSecurityContext implements SecurityContext {

    private Logger log = LoggerFactory.getLogger(TokenBasedSecurityContext.class);

    private AuthenticatedUserDetails authenticatedUserDetails;
    private AuthenticationTokenDetails authenticationTokenDetails;
    private final boolean secure;

    public TokenBasedSecurityContext(AuthenticatedUserDetails authenticatedUserDetails, AuthenticationTokenDetails authenticationTokenDetails, boolean secure) {
        this.authenticatedUserDetails = authenticatedUserDetails;
        this.authenticationTokenDetails = authenticationTokenDetails;
        this.secure = secure;
    }

    @Override
    public Principal getUserPrincipal() {
//        log.debug("getUserPrincipal");
        return authenticatedUserDetails;
    }

    @Override
    public boolean isUserInRole(String s) {
//        log.debug("isUserInRole " + s);
        return authenticatedUserDetails.getAuthorities().contains(s);
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public String getAuthenticationScheme() {
        return "Bearer";
    }

    public AuthenticationTokenDetails getAuthenticationTokenDetails() {
        return authenticationTokenDetails;
    }
}
