package sssii.billing.server.resource;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import sssii.billing.common.BillingRole;
import sssii.billing.common.entity.Setting;
import sssii.billing.common.entity.rs.Settings;
import sssii.billing.server.ServletContextHelper;

@Path(Setting.REST_PATH)
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class SettingResource extends ResourceBase<Setting> {
    //private Logger log = LoggerFactory.getLogger(SettingResource.class);

    public SettingResource() {
        super.setClass(Setting.class);
    }

    @GET
    @Path("all")
    @RolesAllowed({BillingRole.ADMIN, BillingRole.USER})
    public Settings getAll() {
        List<Setting> list = getList(null, null, null);

        Settings settings = new Settings();

        Map<String, Setting> smap = list.stream().collect(Collectors.toMap(Setting::getName, Function.identity()));    // a -> a
        settings.setSettings(smap);

        return settings;
    }

    @PUT
    @Path("all")
    @RolesAllowed(BillingRole.ADMIN)
    public Settings setAll(Settings sett) {
        EntityManager em = ServletContextHelper.createEm();

        EntityTransaction tx = em.getTransaction();
        tx.begin();

        for(Setting s: sett.getSettings().values()) {
            em.merge(s);
        }

        tx.commit();
        em.close();

        return getAll();
    }
}
