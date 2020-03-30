package sssii.billing.server.security;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sssii.billing.common.entity.rs.User;
import sssii.billing.common.security.UserService;
import sssii.billing.server.Application;

public class LdapUserService implements UserService
{
    private Logger log = LoggerFactory.getLogger(LdapUserService.class);

    private HashMap<String, String> groupsToRoles = new HashMap<>();

    public LdapUserService() {
        // ldap.groups.to.roles.map = BillingAdmins:admin; BillingUsers:user
        String prop = Application.getOptions().getProperty("ldap.groups.to.roles.map");
        String[] parts = prop.split(";");
        for(String s: parts) {
            String[] keyval = s.trim().split(":");
            groupsToRoles.put(keyval[0].trim(), keyval[1].trim());
        }

    }

    private LdapContext getLdapContext(String dn, String password) throws NamingException
    {
        // check login for allowed chars, no spaces etc to prevent injection attack just in case?

        // authenticate
//        log.debug("ldap url: " + Application.getOptions().getProperty("ldap.url"));

        //String baseDn = Application.getOptions().getProperty("ldap.base.dn");
        //String userDn = "cn=" + login + ",ou=People," + baseDn;

        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, Application.getOptions().getProperty("ldap.url"));
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        //env.put(Context.SECURITY_AUTHENTICATION, "DIGEST-MD5"); // principal format?
        env.put(Context.SECURITY_PRINCIPAL, dn);
        env.put(Context.SECURITY_CREDENTIALS, password);

        LdapContext ldapCtx;

        try{
            ldapCtx = new InitialLdapContext(env, null);
        }
        catch(AuthenticationException ex){
            // no error, authentication not passed
            log.debug("login failed: " + ex.getMessage());
            return null;
        }
        catch(NamingException ex){
            log.error("failed to authenticate: " + ex.getMessage());
            throw ex;
        }

        return ldapCtx;
    }

    private LdapContext getUserLdapContext(String username, String password) throws NamingException
    {
        String baseDn = Application.getOptions().getProperty("ldap.base.dn");
        String userDn = "cn=" + username + ",ou=People," + baseDn;

        return getLdapContext(userDn, password);
    }

    private LdapContext getAppLdapContext() throws NamingException
    {
        return getLdapContext(Application.getOptions().getProperty("ldap.app.dn"),
                Application.getOptions().getProperty("ldap.app.password"));
    }

    @Override
    public User findByUsername(String username) {
        LdapContext ldapCtx;
        try {
            ldapCtx = getAppLdapContext();
            return getUser(username, ldapCtx);
        } catch (NamingException e) {
            log.error("failed", e);
            return null;
        }
    }


    private User getUser(String username, LdapContext ldapCtx) {

        User u = new User();
        HashSet<String> roles = new HashSet<>();

        try {
            //LdapContext ldapCtx = getAppLdapContext();

            String baseDn = Application.getOptions().getProperty("ldap.base.dn");
            String userDn = "cn=" + username + ",ou=People," + baseDn;

            Attributes attr = ldapCtx.getAttributes(userDn);

//            NamingEnumeration<String> ids = attr.getIDs();
//            while(ids.hasMoreElements()){
//                String attrId = ids.next();
//                log.debug(attrId + " - " + attr.get(attrId).get());
//            }

            u.setUsername(attr.get("cn").get().toString());

            Attribute optAttr;

            optAttr = attr.get("givenName");
            u.setFirstName(optAttr == null ? "" : optAttr.get().toString());
            optAttr = attr.get("sn");
            u.setLastName(optAttr == null ? "" : optAttr.get().toString());
            optAttr = attr.get("mail");
            u.setEmail(optAttr == null ? "" : optAttr.get().toString());

            // groups
            SearchControls ctl = new SearchControls();
            ctl.setReturningAttributes(new String[] {"cn"});

            // opendj 3 seems creates groupOfNames
            //NamingEnumeration<SearchResult> res = ldapCtx.search("ou=Groups," + baseDn, "(&(objectClass=groupOfNames)(member={0}))", new Object[]{userDn}, ctl);

            // opendj ce 2.6.4 seems uses groupOfUniqueNames
            NamingEnumeration<SearchResult> res = ldapCtx.search(
                    "ou=Groups," + baseDn,
                    "(|(" +
                    "(&(objectClass=groupOfNames)(member={0}))" +
                    "(&(objectClass=groupOfUniqueNames)(uniqueMember={0}))" +
                    "))"
                    ,
                    new Object[]{userDn}, ctl);

            while(res.hasMoreElements()){
                SearchResult sr = res.next();

                String group = sr.getAttributes().get("cn").get().toString();
//                log.debug("group: " + group);

                if(groupsToRoles.containsKey(group)) {
                    roles.add(groupsToRoles.get(group));
                }

//                log.debug("-- " + sr.getName());
//
//                Attributes retAttr = sr.getAttributes();
//                NamingEnumeration<? extends Attribute> aenum = retAttr.getAll();
//
//                while(aenum.hasMoreElements()) {
//                    Attribute a = aenum.next();
//                    log.debug("---- " + a.getID());
//                }

            }

        } catch (NamingException e) {
            log.error("failed: " + e.getMessage());
            return null;
        }

        u.setAuthorities(roles);

        return u;
    }

    @Override
    public User validateCredentials(String username, String password) {
        try {
            LdapContext userCtx = getUserLdapContext(username, password);
            if(userCtx == null) {
                return null;
            }
            return getUser(username, userCtx);
        } catch (NamingException e) {
            log.error("failed: " + e.getMessage(), e);
            return null;
        }

    }

}
