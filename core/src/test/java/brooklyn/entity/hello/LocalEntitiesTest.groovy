package brooklyn.entity.hello;

import static brooklyn.event.basic.DependentConfiguration.*
import static org.testng.Assert.*

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testng.annotations.Test

import brooklyn.entity.basic.AbstractApplication
import brooklyn.event.SensorEvent
import brooklyn.event.SensorEventListener
import brooklyn.location.basic.SimulatedLocation;
import brooklyn.management.Task
import brooklyn.util.task.ParallelTask

/** tests effector invocation and a variety of sensor accessors and subscribers */
class LocalEntitiesTest {
	
	public static final Logger log = LoggerFactory.getLogger(LocalEntitiesTest.class);
			
    @Test
    public void testEffectorUpdatesAttributeSensor() {
        AbstractApplication a = new AbstractApplication() {}
        HelloEntity h = new HelloEntity(owner:a)
        a.start([new SimulatedLocation()])
        
        h.setAge(5)
        assertEquals(5, h.getAttribute(HelloEntity.AGE))
    }

    //REVIEW 1459 - new test
    //subscriptions get notified in separate thread
    @Test
    public void testEffectorEmitsAttributeSensor() {
        AbstractApplication a = new AbstractApplication() {}
        HelloEntity h = new HelloEntity(owner:a)
        a.start([new SimulatedLocation()])
        
        AtomicReference<SensorEvent> evt = new AtomicReference()
        a.getSubscriptionContext().subscribe(h, HelloEntity.AGE, { 
            SensorEvent e -> 
            evt.set(e)
            synchronized (evt) {
                evt.notifyAll();
            }
        } as SensorEventListener)
        long startTime = System.currentTimeMillis()
        synchronized (evt) {
//            h.setAge(5)
            new ParallelTask([h /*, otherEntity, anotherEntity */ ].
                    collect { it.invoke(HelloEntity.SET_AGE, age: 5) });
            evt.wait(5000)
        }
        
        assertNotNull(evt.get())
        assertEquals(HelloEntity.AGE, evt.get().sensor)
        assertEquals(h, evt.get().source)
        assertEquals(5, evt.get().value)
        assertTrue(System.currentTimeMillis() - startTime < 5000)  //shouldn't have blocked for all 5s
    }
    
    //REVIEW 1459 - new test
    @Test
    public void testEffectorEmitsTransientSensor() {
        AbstractApplication a = new AbstractApplication() {}
        HelloEntity h = new HelloEntity(owner:a)
        a.start([new SimulatedLocation()])
        
        AtomicReference<SensorEvent> evt = new AtomicReference()
        a.getSubscriptionContext().subscribe(h, HelloEntity.ITS_MY_BIRTHDAY, {
            SensorEvent e ->
            evt.set(e)
            synchronized (evt) {
                evt.notifyAll();
            }
        })
        long startTime = System.currentTimeMillis()
        synchronized (evt) {
            h.setAge(5)
            evt.wait(5000)
        }
        assertNotNull(evt.get())
        assertEquals(HelloEntity.ITS_MY_BIRTHDAY, evt.get().sensor)
        assertEquals(h, evt.get().source)
        assertNull(evt.get().value)
        assertTrue(System.currentTimeMillis() - startTime < 5000)  //shouldn't have blocked for all 5s
    }

    @Test
    public void testSendMultipleInOrderThenUnsubscribe() {
        AbstractApplication a = new AbstractApplication() {}
        HelloEntity h = new HelloEntity(owner:a)
        a.start([new SimulatedLocation()])

        List data = []       
        a.getSubscriptionContext().subscribe(h, HelloEntity.AGE, { SensorEvent e -> 
            data << e.value
            Thread.sleep((int)(20*Math.random()))
            synchronized (data) { 
                log.info "Thread "+Thread.currentThread()+" notify on subscription received for "+e.value+", data is "+data
                data.notifyAll()
                data.wait(2000) 
            } 
        });
        //need for notify-then-wait (above) and wait-then-notify (below) is ugly but simplest way (i could find) 
        //to ensure they are in lock step; otherwise above might notify twice in succession, below not successful at resuming in between   
        long startTime = System.currentTimeMillis()
        synchronized (data) {
            (1..5).each { h.setAge(it) }
            (1..5).each { log.info "Thread "+Thread.currentThread()+" waiting on $it"; data.wait(2000); data.notifyAll(); }
        }
        a.getSubscriptionContext().unsubscribeAll();
        h.setAge(6)
        Thread.sleep(50);
        assertEquals((1..5), data)
        assertTrue(System.currentTimeMillis() - startTime < 2000)  //shouldn't have blocked for anywhere close to 2s
    }

    @Test
    public void testConfigSetFromAttribute() {
        AbstractApplication a = new AbstractApplication() {}
        a.setConfig(HelloEntity.MY_NAME, "Bob")
        
        HelloEntity dad = new HelloEntity(owner:a)
        HelloEntity son = new HelloEntity(owner:dad)
        
        //config is inherited
        assertEquals("Bob", a.getConfig(HelloEntity.MY_NAME))
        assertEquals("Bob", dad.getConfig(HelloEntity.MY_NAME))
        assertEquals("Bob", son.getConfig(HelloEntity.MY_NAME))
        
        //attributes are not
        a.setAttribute(HelloEntity.FAVOURITE_NAME, "Carl")
        assertEquals("Carl", a.getAttribute(HelloEntity.FAVOURITE_NAME))
        assertEquals(null, dad.getAttribute(HelloEntity.FAVOURITE_NAME))
    }
	@Test
	public void testConfigSetFromAttributeWhenReady() {
		AbstractApplication a = new AbstractApplication() {}
		a.setConfig(HelloEntity.MY_NAME, "Bob")
		
        HelloEntity dad = new HelloEntity(owner:a)
        HelloEntity son = new HelloEntity(owner:dad)
		
        //config can be set from an attribute
        son.setConfig(HelloEntity.MY_NAME, attributeWhenReady(dad, HelloEntity.FAVOURITE_NAME
            /* third param is closure; defaults to groovy truth (see google), but could be e.g.
               , { it!=null && it.length()>0 && it!="Jebediah" }
             */ ));
		a.start([new SimulatedLocation()])
		 
        final Semaphore s1 = new Semaphore(0)
        Object[] sonsConfig = new Object[1]
        Thread t = new Thread( { 
			log.info "started"
			s1.release()
        	log.info "getting config "+sonsConfig[0]
        	sonsConfig[0] = son.getConfig(HelloEntity.MY_NAME);
        	log.info "got config "+sonsConfig[0]
            s1.release()
        } );
		log.info "starting"
        long startTime = System.currentTimeMillis();
		t.start();
		log.info "waiting {}", System.identityHashCode(sonsConfig)
        if (!s1.tryAcquire(2, TimeUnit.SECONDS)) fail("race mismatch, missing permits");
        
        //thread should be blocking on call to getConfig
        assertTrue(t.isAlive());
		assertTrue(System.currentTimeMillis() - startTime < 1500)
        synchronized (sonsConfig) {
            assertEquals(null, sonsConfig[0]);
            for (Task tt in dad.getExecutionContext().getTasks()) { log.info "task at dad:  $tt, "+tt.getStatusDetail(false) }
            for (Task tt in son.getExecutionContext().getTasks()) { log.info "task at son:  $tt, "+tt.getStatusDetail(false) }
            dad.setAttribute(HelloEntity.FAVOURITE_NAME, "Dan");
            if (!s1.tryAcquire(2, TimeUnit.SECONDS)) fail("race mismatch, missing permits");
        }
		log.info "dad: "+dad.getAttribute(HelloEntity.FAVOURITE_NAME)
		log.info "son: "+son.getConfig(HelloEntity.MY_NAME)
		
        //shouldn't have blocked for very long at all
        assertTrue(System.currentTimeMillis() - startTime < 1500)
        //and sons config should now pick up the dad's attribute
        assertEquals(sonsConfig[0], "Dan")
	}
	@Test
	public void testConfigSetFromAttributeWhenReadyTransformations() {
		AbstractApplication a = new AbstractApplication() {}
		a.setConfig(HelloEntity.MY_NAME, "Bob")
		
        HelloEntity dad = new HelloEntity(owner:a)
        HelloEntity son = new HelloEntity(owner:dad)
		
        //and config can have transformations
        son.setConfig(HelloEntity.MY_NAME, transform(attributeWhenReady(dad, HelloEntity.FAVOURITE_NAME), { it+it[-1]+"y" }))
		dad.setAttribute(HelloEntity.FAVOURITE_NAME, "Dan");
		a.start([new SimulatedLocation()])
        assertEquals(son.getConfig(HelloEntity.MY_NAME), "Danny")
    }


}
