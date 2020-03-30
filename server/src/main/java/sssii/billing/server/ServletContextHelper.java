package sssii.billing.server;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sssii.billing.common.TaskProvider;
import sssii.billing.common.entity.Customer;
import sssii.billing.common.entity.Project;

@WebListener
public class ServletContextHelper implements ServletContextListener {

    private static Logger log = LoggerFactory.getLogger(ServletContextHelper.class);
    private static EntityManagerFactory emf = null;
    private static TaskProvider taskProvider;

    private static Properties sqls;

    static {
        sqls = new Properties();
        try {
            sqls.loadFromXML(ServletContextHelper.class.getResourceAsStream("/sqls.xml"));
        } catch (IOException ex) {
            log.error("failed to load sql definitions", ex);
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.debug("contextInitialized");

        // https://vladmihalcea.com/2017/03/14/how-to-store-date-time-and-timestamps-in-utc-time-zone-with-jdbc-and-hibernate/

        // Date saved as UTC in mysql, similar to as -Duser.timezone
        //TimeZone.setDefault(TimeZone.getTimeZone("UTC"));    // causes context reload, breaks timestamps in logs

//        System.setProperty("org.jboss.logging.provider", "slf4j");    // for hibernate

        // for c3p0
//        System.setProperty("com.mchange.v2.log.MLog", "slf4j");
        System.setProperty("com.mchange.v2.log.NameTransformer", "com.mchange.v2.log.PackageNames");

        try{
            if(emf == null){
                emf = Persistence.createEntityManagerFactory("billing", Application.getOptions());
            }
            else{
                log.warn("billing entity manager factory is already not null in contextInitialized");
            }

            if(taskProvider == null){
                //taskProvider = (TaskProvider)Class.forName("sssii.billing.jobcard.JobcardTaskProvider").newInstance();
                taskProvider = (TaskProvider)Class.forName(Application.getOptions().getProperty("taskprovider.class")).newInstance();
                Properties props = new Properties();
                final String PREFIX = "taskprovider.";
                for(Object keyo: Application.getOptions().keySet()) {
                    String key = keyo.toString();
                    if(key.startsWith(PREFIX)) {
                        String property = Application.getOptions().getProperty(key);
                        if(!property.isEmpty()) {
                            props.setProperty(key.substring(PREFIX.length()), property);
                        }
                    }
                }

                taskProvider.init(props);
            }
            else{
                log.warn("tasks entity manager factory is already not null in contextInitialized");
            }

        }
        catch(Exception ex){
            log.error("failed to init servlet context - further operations could fail", ex);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.debug("contextDestroyed");
        emf.close();
        taskProvider.close();
    }

    public static EntityManager createEm(){
        log.trace("create entity manager");

        if(emf == null) {
            throw new IllegalStateException("entity manager factory is null (Context is not initialized yet?)");
        }

        return emf.createEntityManager();
    }

    public static TaskProvider getTaskProvider(){
        log.trace("get task provider");

        if(taskProvider == null) {
            throw new IllegalStateException("taskProvider is null (Context is not initialized yet?)");
        }

        return taskProvider;
    }

    public static synchronized void syncProjectData() {

        try {
            EntityManager em = createEm();

            // pull new projects

            Timestamp latest = (Timestamp)em.createQuery("select max(p.createdDate) from Project p").getSingleResult();
            boolean firstSync = (latest == null);
            log.debug("latest project: " + latest);

            // TODO exception NPE: em broken in provider?
            List<Project> items = ServletContextHelper.getTaskProvider().getProjectList(latest);

            HashMap<Integer, Customer> origCustomers = new HashMap<>();
            if(!items.isEmpty()) {
                List<Customer> list = ServletContextHelper.getTaskProvider().getCustomerList();
                if(list != null) {
                    for (Customer customer : list) {
                        origCustomers.put(customer.getOriginalId(), customer);
                    }
                }
            }

            TypedQuery<Customer> custQuery = em.createQuery("select c from Customer c where c.originalId = :id", Customer.class);

            // https://stackoverflow.com/questions/12034055/how-to-save-multiple-entities-in-hibernate
            EntityTransaction tx = em.getTransaction();
            tx.begin();

            int addedProjects = 0;
            for(Project item: items) {

                // validate customer
                try {
                    //Customer c =
                            custQuery.setParameter("id", item.getOriginalCustomerId()).getSingleResult();
                    //log.debug("customer already exists for project " + item.getName() + " : " + c.getName());
                }
                catch(NoResultException ex) {
                    // no customer, import

                    if(origCustomers.containsKey(item.getOriginalCustomerId())) {
                        Customer c = origCustomers.get(item.getOriginalCustomerId());

                        // many projects are old
                        if(!firstSync) {
                            c.setActive(true);
                        }

                        em.persist(c);
                    }
                }

                // many projects are old, make all inactive on first bulk sync
                if(!firstSync) {
                    item.setActive(true);
                }

                em.persist(item);
                if (addedProjects % 20 == 0) { //20, same as the JDBC batch size
                    //flush a batch of inserts and release memory:
                    em.flush();
                    em.clear();
                }

                addedProjects++;
            }

            tx.commit();
            log.debug("inserted new projects: " + addedProjects);

            if(addedProjects > 0) {
                // assign projects to customers
                Query q = em.createNativeQuery(sqls.getProperty("update_projects_with_customers"));
                tx = em.getTransaction();
                tx.begin();
                int rowsAffected = 0;
                rowsAffected = q.executeUpdate();
                tx.commit();
                log.debug("updated customers in projects: " + rowsAffected);
            }
        }
        catch(Exception ex) {
            log.error("failed to sync projects", ex);
        }
    }
}
