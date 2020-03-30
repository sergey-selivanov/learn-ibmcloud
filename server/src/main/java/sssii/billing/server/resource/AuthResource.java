package sssii.billing.server.resource;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sssii.billing.common.BillingRole;
import sssii.billing.common.entity.rs.User;
import sssii.billing.common.entity.rs.UserCredentials;
import sssii.billing.common.entity.rs.UserWithToken;
import sssii.billing.common.security.AuthenticationToken;
import sssii.billing.common.security.AuthenticationTokenDetails;
import sssii.billing.common.security.AuthenticationTokenService;
import sssii.billing.common.security.InvalidAuthenticationTokenException;
import sssii.billing.common.security.UserService;
import sssii.billing.server.Application;
import sssii.billing.server.security.LdapUserService;

@Path("auth")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class AuthResource {

    private Logger log = LoggerFactory.getLogger(AuthResource.class);

    UserService userService = new LdapUserService();

    AuthenticationTokenService authenticationTokenService = new AuthenticationTokenService(Application.getAuthTokenSettings());

    // http://localhost:8080/billing-server/rest/auth?username=aa&password=bb

    //@POST
    @GET
    @PermitAll
    //public Response login(UserCredentials credentials) {
    public Response testLogin(@QueryParam("username") String username, @QueryParam("password") String password) {
        log.debug("login");

        //return Response.ok().build();
        //User user = usernamePasswordValidator.validateCredentials(credentials.getUsername(), credentials.getPassword());
        User user = new User();
        user.setUsername(username);

        String token = authenticationTokenService.issueToken(user.getUsername(), user.getAuthorities());
        AuthenticationToken authenticationToken = new AuthenticationToken();
        authenticationToken.setToken(token);
        return Response.ok(authenticationToken).build();
    }


    // http://localhost:8080/billing-server/rest/auth/test

    @GET
    @Path("test")
    @RolesAllowed({BillingRole.ADMIN, BillingRole.USER})
    public Response testGet() {
        return Response.ok("Success").build();
    }

    @POST
    @PermitAll
    public Response login(UserCredentials credentials) {

        log.debug("login with credentials");

        User user;
        UserWithToken uwt = new UserWithToken();

        user = userService.validateCredentials(credentials.getLogin(), credentials.getPassword());

//        try {
//
//        }
//        catch(Exception ex) {
//            log.error("can't validate credentials", ex);
//            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
//        }

        if(user == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        if(user.getAuthorities().isEmpty()) {
            // no any billing roles
            return Response.status(Status.FORBIDDEN).build();
        }

        String token = authenticationTokenService.issueToken(user.getUsername(), user.getAuthorities());
//        AuthenticationToken authenticationToken = new AuthenticationToken();
//        authenticationToken.setToken(token);

        uwt.setUser(user);
        uwt.setToken(token);

        //return Response.ok(authenticationToken).build();
        ResponseBuilder rb = Response.ok(uwt);
//        if(credentials.getRememberMe()) {
//            rb.cookie(new NewCookie("billing-cookie", token));
//        }

        return rb.build();
    }

    @POST
    @Path("token")
    @PermitAll
    public Response loginByToken(String token) {

        log.debug("login with token");

        try {
            AuthenticationTokenDetails authenticationTokenDetails = authenticationTokenService.parseToken(token);
            User user = userService.findByUsername(authenticationTokenDetails.getUsername());

            UserWithToken uwt = new UserWithToken();
            uwt.setUser(user);
            uwt.setToken(token);

            ResponseBuilder rb = Response.ok(uwt);

            return rb.build();

        }
        catch(InvalidAuthenticationTokenException e) {
            log.debug("bad token: " + e.getMessage());
            return Response.status(Status.FORBIDDEN).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("renew")
    @RolesAllowed({BillingRole.ADMIN, BillingRole.USER})
    public Response renewToken(String oldToken) {
        log.debug("renew token");

        AuthenticationTokenDetails authenticationTokenDetails = authenticationTokenService.parseToken(oldToken);
        User user = userService.findByUsername(authenticationTokenDetails.getUsername());

        if(user == null) {
            return Response.status(Status.FORBIDDEN).build();
        }

        if(user.getAuthorities().isEmpty()) {
            // no any billing roles
            return Response.status(Status.FORBIDDEN).build();
        }

        String token = authenticationTokenService.issueToken(user.getUsername(), user.getAuthorities());
        UserWithToken uwt = new UserWithToken();
        uwt.setUser(user);
        uwt.setToken(token);

        return Response.ok(uwt).build();
    }
}
