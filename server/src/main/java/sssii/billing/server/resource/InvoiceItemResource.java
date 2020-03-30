package sssii.billing.server.resource;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.persistence.TypedQuery;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import sssii.billing.common.BillingRole;
import sssii.billing.common.Constants;
import sssii.billing.common.entity.InvoiceItem;
import sssii.billing.common.entity.Project;
import sssii.billing.server.ServletContextHelper;

@Path("invoiceItems")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class InvoiceItemResource {

    //private Logger log = LoggerFactory.getLogger(InvoiceItemResource.class);

    //public InvoiceItemResource() {}

    @GET
    @Path("candidates")
    @RolesAllowed({BillingRole.ADMIN, BillingRole.USER})
    public List<InvoiceItem> getCandidates(
            @QueryParam(Constants.QPARAM_PROJECTID) Integer billingProjectId,    // billing project id, not original id
//            @QueryParam(Constants.QPARAM_FROMDATE) LocalDate from,
//            @QueryParam(Constants.QPARAM_TODATE) LocalDate to
            @QueryParam(Constants.QPARAM_FROMDATE) String fromStr,
            @QueryParam(Constants.QPARAM_TODATE) String toStr
            ) throws ParseException{


        //LocalDate from = LocalDate.parse(fromStr, DateTimeFormatter.ISO_DATE);
        //LocalDate to = LocalDate.parse(toStr, DateTimeFormatter.ISO_DATE);

        Timestamp from = new Timestamp(new SimpleDateFormat("yyyy-MM-dd").parse(fromStr).getTime());
        Timestamp to = new Timestamp(new SimpleDateFormat("yyyy-MM-dd").parse(toStr).getTime());

        TypedQuery<Project> q = ServletContextHelper.createEm().createQuery("select p from Project p where p.id = :id", Project.class);

        Project project = q.setParameter("id", billingProjectId).getSingleResult();

        //log.debug("project id: " + project.getId() + ", original: " + project.getOriginalId());

        List<InvoiceItem> workedTime = ServletContextHelper.getTaskProvider().getWorkedTimeList(project.getOriginalId(), from, to);

        // Group hours by task
        // TODO rewrite with stream
        HashMap<String, InvoiceItem> tasks = new HashMap<>();
        for(InvoiceItem w: workedTime) {
            String key = w.getTaskNumber();
            if(tasks.containsKey(key)) {
                BigDecimal sum = tasks.get(key).getInvoicedTimeHours();
                sum = sum.add(w.getInvoicedTimeHours());
                tasks.get(key).setInvoicedTimeHours(sum);
            }
            else {
                w.setRate(project.getDefaultRate());
                tasks.put(key, w);
            }
        }
/*
        ArrayList<InvoiceItem> items = new ArrayList<InvoiceItem>(tasks.values());
        Collections.sort(items, new Comparator<InvoiceItem>() {    // sort by task # asc
            @Override
            public int compare(InvoiceItem o1, InvoiceItem o2) {
              if(o1 == null || o2 == null) {
                  return 0;
              }
              else {
                  return o1.getTaskNumber().compareTo(o2.getTaskNumber());
              }
            }
        });
*/
        List<InvoiceItem> items = tasks.values().stream().sorted((o1, o2) ->

            // why nulls? tasks always has number
            //(o1 == null || o2 == null) ? 0 : o1.getTaskNumber().compareTo(o2.getTaskNumber())
            //o1.getTaskNumber().compareTo(o2.getTaskNumber())
            o1.getOriginalTaskId().compareTo(o2.getOriginalTaskId())

        ).collect(Collectors.toList());

        return items;
    }
}
