package brooklyn.util;

import java.util.concurrent.Callable;

/** handy methods available in groovy packaged so they can be consumed from java,
 *  and other conversion/conveniences; but see JavaGroovyEquivalents for faster alternatives */
public class GroovyJavaMethods {

    //TODO use named subclasses, would that be more efficient?
    
    public static Closure closureFromRunnable(final Runnable job) {
        return { it ->
            if (job in Callable) { job.call() }
            else { job.run(); null; }
        };
    }
    
    public static Closure closureFromCallable(final Callable job) {
        return { it -> job.call(); };
    }

    public static boolean truth(Object o) {
        if (o) return true;
        return false;
    }

    public static <T> T elvis(Object preferred, Object fallback) {
        return fix(preferred ?: fallback);
    }
    
    public static <T> T fix(Object o) {
        if (o in GString) return (o as String);
        return o;
    }
}
