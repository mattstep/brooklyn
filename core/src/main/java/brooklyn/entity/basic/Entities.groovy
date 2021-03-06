package brooklyn.entity.basic;

import brooklyn.entity.ConfigKey
import brooklyn.entity.Effector
import brooklyn.entity.Entity
import brooklyn.entity.trait.Startable
import brooklyn.event.AttributeSensor
import brooklyn.event.Sensor
import brooklyn.location.Location
import brooklyn.management.ManagementContext
import brooklyn.management.Task
import brooklyn.util.task.ParallelTask


/** Convenience methods for working with entities. 
 * Also see the various *Methods classes for traits 
 * (eg StartableMethods for Startable implementations). */
public class Entities {
	
	public static Collection<ConfigKey<?>> getConfigKeys(Entity e) {
		if (e in EntityLocal) return ((EntityLocal)e).getConfigKeys().values();
		else return e.getEntityClass().getConfigKeys();
	}
	public static Collection<Sensor<?>> getSensors(Entity e) {
		if (e in EntityLocal) return ((EntityLocal)e).getSensors().values();
		else return e.getEntityClass().getSensors();
	}
	public static Collection<Effector<?>> getEffectors(Entity e) {
		if (e in EntityLocal) return ((EntityLocal)e).getEffectors().values();
		else return e.getEntityClass().getEffectors();
	}

	/** invokes the given effector with the given named arguments on the entitiesToCall, from the calling context of the callingEntity;
	 * intended for use only from the callingEntity
	 * @return ParallelTask containing a results from each invocation; calling get() on the result will block until all complete,
	 * and throw error if any threw error   
	 */
	public static <T> Task<List<T>> invokeEffectorList(EntityLocal callingEntity, Collection<Entity> entitiesToCall, Effector<T> effector, Map<String,?> parameters=[:]) {
		if (!entitiesToCall || entitiesToCall.isEmpty()) return null
		List<Task> tasks = entitiesToCall.collect { entity -> { -> entity.invoke(effector, parameters).get() } }
        //above formulation is complicated, but it is building up a list of tasks, without blocking on them initially,
        //but ensuring that when the parallel task is gotten it does block on all of them
		ParallelTask invoke = new ParallelTask(tasks)
		callingEntity.executionContext.submit(invoke)
		return invoke
	}

    public static boolean isSecret(String name) {
        name.contains("password") || name.contains("credential") || name.contains("secret") || name.contains("private")
    }

    public static boolean isTrivial(Object v) {
        v==null || ((v in Map || v in Collection || v in String) && (v.isEmpty()))
    }    
	public static void dumpInfo(Entity e, Writer out=new PrintWriter(System.out), String currentIndentation="", String tab="  ") {
		out << currentIndentation+e.toString()+"\n"
		getConfigKeys(e).each {
            def v = e.getConfig(it)
            if (!isTrivial(v)) {
                out << currentIndentation+tab+tab+it.name;
                out << " = ";
                if (isSecret(it.name)) out << "xxxxxxxx";
                else out << v;
                out << "\n"
            }
		}
		getSensors(e).each {
			if (it in AttributeSensor) {
                def v = e.getAttribute(it)
                if (!isTrivial(v)) {
                    out << currentIndentation+tab+tab+it.name;
                    out << ": ";
                    if (isSecret(it.name)) out << "xxxxxxxx";
                    else out << v;
                    out << "\n";
                }
			}
		}
		e.getOwnedChildren().each {
			dumpInfo(it, out, currentIndentation+tab, tab)
		}
		out.flush()
	}

	public static boolean isAncestor(Entity descendant, Entity potentialAncestor) {
		AbstractEntity ancestor = descendant.getOwner()
		while (ancestor) {
			if (ancestor.equals(potentialAncestor)) return true
			ancestor = ancestor.getOwner()
		}
		return false
	}

	/** note, it is usually preferred to use isAncestor() and swap the order, it is a cheaper method */
	public static boolean isDescendant(Entity ancestor, Entity potentialDescendant) {
		Set<Entity> inspected = [] as HashSet
		List<Entity> toinspect = [ancestor]
		
		while (!toinspect.isEmpty()) {
			Entity e = toinspect.pop()
			if (e.getOwnedChildren().contains(potentialDescendant)) {
				return true
			}
			inspected.add(e)
			toinspect.addAll(e.getOwnedChildren())
			toinspect.removeAll(inspected)
		}
		
		return false
	}

    /** Interim method for assisting with destroying entities */
    public static Entity start(ManagementContext context, Entity e, Collection<Location> locations) {
        context?.manage(e);
        if (e in Startable) ((Startable)e).start(locations);
        e
    }

    /** Interim method for assisting with destroying entities */
    public static void destroy(ManagementContext context, Entity e) {
        if (e in Startable) ((Startable)e).stop();
        if (e in AbstractEntity) ((AbstractEntity)e).destroy();
        context?.unmanage(e);
    }

}
