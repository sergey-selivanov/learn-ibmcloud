package sssii.billing.server.resource;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

import javax.annotation.security.RolesAllowed;
import javax.persistence.Query;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sssii.billing.common.BillingRole;
import sssii.billing.common.entity.Project;
import sssii.billing.server.ServletContextHelper;

@Path(Project.REST_PATH)
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ProjectResource extends ResourceBase<Project> {
    private Logger log = LoggerFactory.getLogger(ProjectResource.class);

    public ProjectResource() {
        super.setClass(Project.class);
    }

    @GET
    @Path("{id}/lastinvoiceddate")
    @RolesAllowed({BillingRole.ADMIN, BillingRole.USER})
    public Response getLastInvoicedDate(
            @PathParam("id") Integer projectId)
    {
        Query q = em.createQuery("select max(i.workToDate) from Invoice i where i.projectId = :projectId")
                .setParameter("projectId", projectId);
        //Object o = q.getSingleResult();
        Timestamp ts = (Timestamp) q.getSingleResult();

        log.debug("date: " + ts);

        if(ts == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
// TODO how to correctly pass dates, not strings?
        String s = DateTimeFormatter.ISO_DATE.format(ts.toLocalDateTime());

        return Response.ok(s).build();

    }

    @GET
    @Path("{id}/lastworkreporteddate")
    @RolesAllowed({BillingRole.ADMIN, BillingRole.USER})
    public Response getLastWorkReportedDate(
            @PathParam("id") Integer originalProjectId)
    {
        // TODO accept our id, not original id
        // TODO bil-46 exception
        java.sql.Date ts = ServletContextHelper.getTaskProvider().getLastWorkReported(originalProjectId);

        log.debug("date: " + ts);

        if(ts == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        //String s = DateTimeFormatter.ISO_DATE.format(ts.toLocalDateTime()); // failed, sql.Date has no time component
        String s = new SimpleDateFormat("yyyy-MM-dd").format(ts);

        return Response.ok(s).build();
    }
}
