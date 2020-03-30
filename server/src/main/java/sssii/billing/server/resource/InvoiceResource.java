package sssii.billing.server.resource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import sssii.billing.common.BillingRole;
import sssii.billing.common.entity.Invoice;
import sssii.billing.common.entity.InvoiceItem;
import sssii.billing.common.entity.rs.NewInvoice;
import sssii.billing.server.ServletContextHelper;

@Path(Invoice.REST_PATH)
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class InvoiceResource extends ResourceBase<Invoice> {
    private Logger log = LoggerFactory.getLogger(InvoiceResource.class);


    public InvoiceResource() {
        super.setClass(Invoice.class);
    }


    @POST
    @Path("withitems")
    @RolesAllowed({BillingRole.ADMIN, BillingRole.USER})
    public Response saveNew(NewInvoice newInvoice) throws URISyntaxException {
        EntityManager em = ServletContextHelper.createEm();

        EntityTransaction tx = em.getTransaction();
        tx.begin();

        Invoice inv = newInvoice.getInvoice();
        em.persist(inv);
        em.flush();    // sets id

        log.debug(inv.getName() + ": " + inv.getId());

        for(InvoiceItem i: newInvoice.getItems()) {
            i.setInvoiceId(inv.getId());
            //i.setName(i.getName());
            em.persist(i);
        }

        tx.commit();
        em.close();

//        log.debug("pathInfo: " + request.getPathInfo());                 // /invoices/withitems
//        log.debug("contextpath: " + request.getContextPath());             // /billing-server
//        log.debug("pathTranslated: " + request.getPathTranslated());    // D:\workspace\.metadata\.plugins\org.eclipse.wst.server.core\tmp1\wtpwebapps\billing-server\invoices\withitems
//        log.debug("requesturi: " + request.getRequestURI());            // /billing-server/rest/invoices/withitems
//        log.debug("servletpath: " + request.getServletPath());             // /rest

        String location = request.getRequestURI().replace("withitems", inv.getId().toString()); // somewhat dirty replace

        Response r = Response.created(new URI(location))    // adds Location header
                //.header(HttpHeaders.LOCATION, "invoices/432")
                .build();

        return r;
    }

    private class PdfWork implements Work
    {
        private Integer id;
        private byte[] pdfBytes;

        public PdfWork(Integer id) {
            this.id = id;
        }

        @Override
        public void execute(Connection connection) throws SQLException {
            // https://community.jaspersoft.com/wiki/jasperreports-library-reference-materials
            // http://ensode.net/jasperreports_intro.html
            try {
                log.debug("generating report...");

                // TODO move compiled report(s) to some subdir
                JasperReport rep = (JasperReport)JRLoader.loadObject(getClass().getResourceAsStream("/invoice.jasper"));

                Map<String, String> jProps = DefaultJasperReportsContext.getInstance().getProperties();
//                for(String key: jProps.keySet()) {
//                    log.debug("- " + key + " - " + jProps.get(key));
//                }

                // http://jasperreports.sourceforge.net/config.reference.html

                //jProps.put("net.sf.jasperreports.default.pdf.encoding", "Cp1251");
                jProps.put("net.sf.jasperreports.default.pdf.embedded", "true");


                HashMap<String, Object> params = new HashMap<>();
                params.put("invoice_id", id);
                JasperPrint print = JasperFillManager.fillReport(rep, params, connection);
                //JasperExportManager.exportReportToPdfFile(print, "d:/tmp/testreport.pdf");

                // TODO export to temp file?
                pdfBytes = JasperExportManager.exportReportToPdf(print);

                log.debug("generated report, bytes: " + pdfBytes.length);
            } catch (JRException ex) {
                log.error("failed to generate report", ex);
            }
        }

        public byte[] getBytes() {
            return pdfBytes;
        }
    }

    @GET
    @Path("{id}/pdf")
    @Produces("application/pdf")
    @RolesAllowed({BillingRole.ADMIN, BillingRole.USER})
    public Response getPdf(@PathParam("id") Integer id) {

        // https://www.theserverside.com/tip/How-to-get-the-Hibernate-Session-from-the-JPA-20-EntityManager

        Session sess = em.unwrap(org.hibernate.Session.class);

        PdfWork work = new PdfWork(id);
        sess.doWork(work);
        byte[] pdfBytes = work.getBytes();

        String name = getSingle(id).getName();

        Response r = Response.ok(

            new StreamingOutput() {

                @Override
                public void write(OutputStream output) throws IOException, WebApplicationException {
                    output.write(pdfBytes);
                    // TODO flush, close?
                }
            })
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.type("attachment").fileName(name + ".pdf").build())
                .build();

        return r;
    }

}
