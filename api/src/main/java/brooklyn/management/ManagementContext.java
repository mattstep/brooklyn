package brooklyn.management;

import java.util.Collection;

import brooklyn.entity.Entity;
import brooklyn.entity.EntitySummary;

/**
 * This is the entry point for accessing and interacting with OverPaas. 
 * 
 * For example, policies and the web-app can use this to retrieve the desired entities.  
 * 
 * @author aled
 */
public interface ManagementContext {

    /**
     * The applications known about in this OverPaas context.
     */
    Collection<EntitySummary> getApplicationSummaries();

    /**
     * All the entities associated with this application (i.e. the entire graph of entitities involved in this app). 
     */
    Collection<EntitySummary> getEntitySummariesInApplication(String id);

    /**
     * All entities known about in this OverPaas context.
     */
    Collection<EntitySummary> getAllEntitySummaries();

    /**
     * @return The entity with the given identifier.
     */
    Entity getEntity(String id);
    
    // TODO relationship of application?
}