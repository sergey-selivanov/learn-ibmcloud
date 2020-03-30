package sssii.billing.server.resource;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.persistence.Query;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sssii.billing.common.BillingRole;
import sssii.billing.common.entity.Customer;
import sssii.billing.common.entity.rs.CustomerProjectCount;

@Path(Customer.REST_PATH)
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class CustomerResource extends ResourceBase<Customer> {

    private Logger log = LoggerFactory.getLogger(CustomerResource.class);
    private Properties sqls;

    public CustomerResource() {
        super.setClass(Customer.class);

        try {
            sqls = new Properties();
            sqls.loadFromXML(CustomerResource.class.getResourceAsStream("/sqls.xml"));
        } catch (IOException ex) {
            log.error("failed to load sql definitions", ex);
        }
    }

    @GET
    @Path("projectcounts")
    @RolesAllowed({BillingRole.ADMIN, BillingRole.USER})
    public List<CustomerProjectCount> getProjectCounts() {
        Query q = em.createNativeQuery(sqls.getProperty("projectcounts"));

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();

        List<CustomerProjectCount> list = rows.stream().map(oArr -> {
            CustomerProjectCount pc = new CustomerProjectCount();
            pc.setCustomerId((Integer) oArr[0]);
            pc.setOriginalId((Integer) oArr[1]);
            pc.setProjectCount(((BigInteger) oArr[2]).intValue());
            return pc;
        }).collect(Collectors.toList());

        return list;
    }

    @Override
    @RolesAllowed(BillingRole.ADMIN)
    public Response update(Integer id, Customer entity) {
        log.debug("customer update");
        return super.update(id, entity);
    }

    @Override
    @RolesAllowed(BillingRole.ADMIN)
    public Response delete(Integer id) {
        return super.delete(id);
    }
}
