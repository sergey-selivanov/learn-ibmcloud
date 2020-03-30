package sssii.billing.server.resource;

import java.util.HashMap;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import sssii.billing.common.BillingRole;
import sssii.billing.common.entity.rs.MapWrapper;
import sssii.billing.server.Application;
import sssii.billing.server.ServletContextHelper;

@Path("server")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ServerResource {

    //private Logger log = LoggerFactory.getLogger(ServerResource.class);

    @GET
    @Path("sync")
    @RolesAllowed({BillingRole.ADMIN, BillingRole.USER})
    public Response syncData() {
        ServletContextHelper.syncProjectData();

        return Response.ok().build();
    }

    @GET
    @Path("version")
    @PermitAll
    public MapWrapper getVersion() {
        HashMap<String, String> map = new HashMap<>();
        for(String key: Application.getVersion().stringPropertyNames()) {
            map.put(key, Application.getVersion().getProperty(key));
        }

        MapWrapper mw = new MapWrapper();
        mw.setMap(map);

        return mw;
    }

}
