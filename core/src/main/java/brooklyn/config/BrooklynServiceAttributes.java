package brooklyn.config;

import brooklyn.management.ManagementContext;
import brooklyn.util.internal.BrooklynSystemProperties.StringSystemProperty;

/** attributes which callers can set and a service application
 * (such as servlet or osgi) will pay attention to,
 * contained in one place for convenience
 * 
 * @author alex
 */
public class BrooklynServiceAttributes {

    /*
     * These fields are contained here so that they are visible both to web console
     * and to launcher, without needing a separate web-console-support project,
     * or battling maven etc to build web-console as jar available to launcher
     * (which would contain a lot of crap as well).
     */

	
    /** used to hold the instance of ManagementContext which should be used */
    public static final String BROOKLYN_MANAGEMENT_CONTEXT = ManagementContext.class.getName();

    /** poor-man's security, to specify a user to be automatically logged in
     * (e.g. to bypass security, during dev/test); 'admin' is usually a sensible choice.
     * if not specified (the default) username+password is required. */
    public static final String BROOKLYN_AUTOLOGIN_USERNAME = "brooklyn.autologin.username";
    
    /** poor-man's security, to specify a default password for access */
    public static final String BROOKLYN_DEFAULT_PASSWORD = "brooklyn.default.password";

    // FIXME use BrooklynSystemProperties classes above, following pattern below
    // (and move BrooklynSystemProperties constants here)
    
    /** in some cases localhost does not resolve correctly 
     * (e.g. to an interface which is defined locally but not in operation,
     * or where multiple NICs are available and java's InetAddress.getLocalHost() strategy is not doing what is desired);
     * use this to supply a specific address (e.g. "127.0.0.1" or a specific IP on a specific NIC or FW)
     */
    public static StringSystemProperty LOCALHOST_IP_ADDRESS = new StringSystemProperty("brooklyn.localhost.address");
    
}
