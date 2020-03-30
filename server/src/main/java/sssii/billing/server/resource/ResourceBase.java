package sssii.billing.server.resource;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sssii.billing.common.BillingRole;
import sssii.billing.common.Constants;
import sssii.billing.server.ServletContextHelper;

// @RolesAllowed on base class does not apply to its methods called in children
// @RolesAllowed set on base methods works for children

//@RolesAllowed({BillingRole.ADMIN, BillingRole.USER})
public abstract class ResourceBase<T> {

    @Context
    public HttpServletRequest request;

    private Logger log = LoggerFactory.getLogger(ResourceBase.class);

    protected EntityManager em;

    protected Class<T> entityClass;

    public ResourceBase()
    {
        em = ServletContextHelper.createEm();
        log.trace("base construstor: real class " + this.getClass().getSimpleName() + ", em created");
    }

    protected void closeEm(){
        if(em != null){
            try{
                em.close();
                log.debug("closeEm: em closed");
            }
            catch(Exception ex){
                log.error("failed to close em", ex);
            }
        }
        else{
            log.debug("closeEm: em is null, nothing to close");
        }
    }

    @GET
    @RolesAllowed({BillingRole.ADMIN, BillingRole.USER})
    public List<T> getList(
            @QueryParam(Constants.QPARAM_ISACTIVE) Boolean isActive,
            @QueryParam(Constants.QPARAM_ASC) String asc,
            @QueryParam(Constants.QPARAM_DESC) String desc
            ){

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(entityClass);
        Root<T> root = cq.from(entityClass);

        if(isActive != null) {
            cq = cq.where(cb.equal(root.get("isActive"), isActive));
        }

        if(desc != null) {
            cq.orderBy(cb.desc(root.get(desc)));
        }
        else if(asc != null) {
            cq.orderBy(cb.asc(root.get(asc)));
        }

        TypedQuery<T> query = em.createQuery(cq);


//        query.setFirstResult(start);
//        if(nolimit == null)
//        {
//            query.setMaxResults(DEFAULT_MAX_RESULTS);
//        }

        log.debug("getList: getting entities...");

        List<T> entities = query.getResultList();

        //em.getTransaction().commit();
        //em.close();
        closeEm();

        return entities;
    }

    @GET
    @Path("{id}")
    @RolesAllowed({BillingRole.ADMIN, BillingRole.USER})
    public T getSingle(@PathParam("id") Integer id) {

        T entity = em.find(entityClass, id);
        em.close();
        return entity;
    }

    @POST
    @RolesAllowed({BillingRole.ADMIN, BillingRole.USER})
    public T insert(T t)    // TODO must clear id?
    {
        try
        {
            T entity = t;
            //((TPCEntity)t).setId(null);
            log.debug("inserting entity " + entity.toString());
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            closeEm();

            return entity;
        }
        catch (EntityExistsException ex)
        {
            log.error("failed to persist entity", ex);
            closeEm();
            throw ex;
        }
    }

    @PUT
    @Path("{id}")
    @RolesAllowed({BillingRole.ADMIN, BillingRole.USER})
    public Response update(@PathParam("id") Integer id, T entity) {    // must accept id parameter https://restfulapi.net/rest-put-vs-post/
        log.debug("updating entity " + id + " " + entity.toString());
        em.getTransaction().begin();
        em.merge(entity);
        em.getTransaction().commit();
        closeEm();

        Response r = Response.ok().build();
        return r;
    }

    @DELETE
    @Path("{id}")
    @RolesAllowed(BillingRole.ADMIN)
    public Response delete(@PathParam("id") Integer id) {

        T entity = em.find(entityClass, id);

        em.getTransaction().begin();
        em.remove(entity);
        em.getTransaction().commit();
        closeEm();

        Response r = Response.ok().build();
        return r;
    }


    public void setClass(Class<T> class1) {
        entityClass = class1;
    }
}
