package brooklyn.entity.brooklyn

import static brooklyn.test.TestUtils.*
import static org.testng.Assert.*

import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import brooklyn.event.SensorEventListener
import brooklyn.location.basic.SimulatedLocation
import brooklyn.test.entity.TestApplication
import brooklyn.test.entity.TestEntity

class BrooklynMetricsTest {

    private static final long TIMEOUT_MS = 2*1000
    
    TestApplication app
    SimulatedLocation loc
    BrooklynMetrics brooklynMetrics
    
    @BeforeMethod
    public void setUp() {
        app = new TestApplication()
        loc = new SimulatedLocation()
        brooklynMetrics = new BrooklynMetrics(updatePeriod:10L, owner:app)
    }
    
    @Test
    public void testInitialBrooklynMetrics() {
        app.start([loc])

        executeUntilSucceeds(timeout:TIMEOUT_MS) {
            assertEquals(brooklynMetrics.getAttribute(BrooklynMetrics.TOTAL_EFFECTORS_INVOKED), 1)
            assertTrue(brooklynMetrics.getAttribute(BrooklynMetrics.TOTAL_TASKS_SUBMITTED) > 0)
            assertEquals(brooklynMetrics.getAttribute(BrooklynMetrics.NUM_INCOMPLETE_TASKS), 0)
            assertEquals(brooklynMetrics.getAttribute(BrooklynMetrics.NUM_ACTIVE_TASKS), 0)
            assertTrue(brooklynMetrics.getAttribute(BrooklynMetrics.TOTAL_EVENTS_PUBLISHED) > 0)
            assertEquals(brooklynMetrics.getAttribute(BrooklynMetrics.TOTAL_EVENTS_DELIVERED), 0)
            assertEquals(brooklynMetrics.getAttribute(BrooklynMetrics.NUM_SUBSCRIPTIONS), 0)
        }
    }
    
    @Test
    public void testBrooklynMetricsIncremented() {
        TestEntity e = new TestEntity(owner:app)
        app.start([loc])

        executeUntilSucceeds(timeout:TIMEOUT_MS) {
            assertEquals(brooklynMetrics.getAttribute(BrooklynMetrics.TOTAL_EFFECTORS_INVOKED), 2) // for app and testEntity's start
        }

        long effsInvoked = brooklynMetrics.getAttribute(BrooklynMetrics.TOTAL_EFFECTORS_INVOKED)
        long tasksSubmitted = brooklynMetrics.getAttribute(BrooklynMetrics.TOTAL_TASKS_SUBMITTED)
        long eventsPublished = brooklynMetrics.getAttribute(BrooklynMetrics.TOTAL_EVENTS_PUBLISHED)
        long eventsDelivered = brooklynMetrics.getAttribute(BrooklynMetrics.TOTAL_EVENTS_DELIVERED)
        long subscriptions = brooklynMetrics.getAttribute(BrooklynMetrics.NUM_SUBSCRIPTIONS)

        // Invoking an effector increments effector/task count
        e.myEffector()
        
        executeUntilSucceeds(timeout:TIMEOUT_MS) {
            assertEquals(brooklynMetrics.getAttribute(BrooklynMetrics.TOTAL_EFFECTORS_INVOKED), effsInvoked+1)
            assertEquals(brooklynMetrics.getAttribute(BrooklynMetrics.TOTAL_TASKS_SUBMITTED), tasksSubmitted+1)
        }
        
        // Setting attribute causes event to be published and delivered to the subscriber
        // Note that the brooklyn metrics entity itself is also publishing sensors
        app.subscribe(e, TestEntity.SEQUENCE, {} as SensorEventListener)
        e.setAttribute(TestEntity.SEQUENCE, 1)
        
        executeUntilSucceeds(timeout:TIMEOUT_MS) {
            assertTrue(brooklynMetrics.getAttribute(BrooklynMetrics.TOTAL_EVENTS_PUBLISHED) > eventsPublished)
            assertEquals(brooklynMetrics.getAttribute(BrooklynMetrics.TOTAL_EVENTS_DELIVERED), eventsDelivered+1)
            assertEquals(brooklynMetrics.getAttribute(BrooklynMetrics.NUM_SUBSCRIPTIONS), 1)
        }
    }
}
