package brooklyn.entity.dns;

import static java.util.concurrent.TimeUnit.*
import static org.testng.Assert.*
import groovy.lang.MetaClass

import java.util.Map
import java.util.concurrent.TimeUnit

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import com.google.common.collect.Iterables;

import brooklyn.entity.Entity
import brooklyn.entity.basic.AbstractApplication
import brooklyn.entity.basic.DynamicGroup
import brooklyn.entity.group.DynamicFabric
import brooklyn.entity.trait.Startable
import brooklyn.location.Location
import brooklyn.location.basic.SimulatedLocation
import brooklyn.location.basic.SshMachineLocation
import brooklyn.location.geo.HostGeoInfo
import brooklyn.test.entity.TestEntity
import brooklyn.util.internal.EntityStartUtils
import brooklyn.util.internal.Repeater
import brooklyn.util.internal.TimeExtras

public class AbstractGeoDnsServiceTest {
    public static final Logger log = LoggerFactory.getLogger(AbstractGeoDnsServiceTest.class);
    static { TimeExtras.init() }

    private static final String WEST_IP = "208.95.232.123";
    private static final String EAST_IP = "216.150.144.82";
    private static final double WEST_LATITUDE = 37.43472, WEST_LONGITUDE = -121.89500;
    private static final double EAST_LATITUDE = 41.10361, EAST_LONGITUDE = -73.79583;
    
    private static final Location WEST_PARENT = new SimulatedLocation(
        name: "West parent", latitude: WEST_LATITUDE, longitude: WEST_LONGITUDE);
    private static final Location WEST_CHILD = new SshMachineLocation(
        name: "West child", address: WEST_IP, parentLocation: WEST_PARENT); 
    private static final Location WEST_CHILD_WITH_LOCATION = new SshMachineLocation(
        name: "West child with location", address: WEST_IP, parentLocation: WEST_PARENT,
        latitude: WEST_LATITUDE, longitude: WEST_LONGITUDE); 
    
    private static final Location EAST_PARENT = new SimulatedLocation(
        name: "East parent", latitude: EAST_LATITUDE, longitude: EAST_LONGITUDE);
    private static final Location EAST_CHILD = new SshMachineLocation(
        name: "East child", address: EAST_IP, parentLocation: EAST_PARENT); 
    private static final Location EAST_CHILD_WITH_LOCATION = new SshMachineLocation(
        name: "East child with location", address: EAST_IP, parentLocation: EAST_PARENT,
        latitude: EAST_LATITUDE, longitude: EAST_LONGITUDE); 
    
    private AbstractApplication app;
    private DynamicFabric fabric
    private TestService geoDns;
    

    @BeforeMethod
    public void setup() {
        def template = { properties -> new TestEntity(properties) }
        app = new AbstractApplication() { };
        fabric = new DynamicFabric(owner:app, factory:template);
    }

    @AfterMethod
    public void shutdown() {
        if (fabric != null && fabric.getAttribute(Startable.SERVICE_UP)) {
            EntityStartUtils.stopEntity(fabric)
        }
    }

    
    @Test
    public void testGeoInfoOnLocation() {
        DynamicFabric fabric = new DynamicFabric(factory:{ Map properties -> return new TestEntity(properties) }, app)
        DynamicGroup testEntities = new DynamicGroup([:], app, { Entity e -> (e instanceof TestEntity) });
        geoDns = new TestService(app);
        geoDns.setTargetEntityProvider(testEntities);
        
        app.start( [ WEST_CHILD_WITH_LOCATION, EAST_CHILD_WITH_LOCATION ] );
        
        waitForTargetHosts(geoDns);
        assertTrue(geoDns.targetHostsByName.containsKey("West child with location"));
        assertTrue(geoDns.targetHostsByName.containsKey("East child with location"));
    }
    
    @Test
    public void testGeoInfoOnParentLocation() {
        DynamicFabric fabric = new DynamicFabric(factory:{ Map properties -> return new TestEntity(properties) }, app)
        DynamicGroup testEntities = new DynamicGroup([:], app, { Entity e -> (e instanceof TestEntity) });
        geoDns = new TestService(app);
        geoDns.setTargetEntityProvider(testEntities);
        
        app.start( [ WEST_CHILD, EAST_CHILD ] );
        
        waitForTargetHosts(geoDns);
        assertTrue(geoDns.targetHostsByName.containsKey("West child"));
        assertTrue(geoDns.targetHostsByName.containsKey("East child"));
    }
    
    //TODO
//    @Test
//    public void testMissingGeoInfo() {
//    }
//    
//    @Test
//    public void testEmptyGroup() {
//    }
    
    private static void waitForTargetHosts(TestService service) {
        new Repeater("Wait for target hosts")
            .repeat()
            .every(500 * MILLISECONDS)
            .until { service.targetHostsByName.size() == 2 }
            .limitIterationsTo(20)
            .run();
    }
    
    
    private class TestService extends AbstractGeoDnsService {
        public Map<String, HostGeoInfo> targetHostsByName = new LinkedHashMap<String, HostGeoInfo>();
        
        public TestService(properties=[:], Entity owner) {
            super(properties, owner);
        }
        
        protected boolean addTargetHost(Entity e, boolean doUpdate) {
            //ignore geo lookup, override parent menu
            log.info("TestService adding target host $e");
            Location l = Iterables.getOnlyElement(e.locations);
            HostGeoInfo geoInfo = new HostGeoInfo("127.0.0.1", l.name, 
                l.findLocationProperty("latitude"), l.findLocationProperty("longitude"));
            targetHosts.put(e, geoInfo);
            if (doUpdate) update();
            return true;
        }
        
        @Override
        protected void reconfigureService(Collection<HostGeoInfo> targetHosts) {
            targetHostsByName.clear();
            for (HostGeoInfo host : targetHosts) {
                if (host != null) targetHostsByName.put(host.displayName, host);
            }
        }

        @Override
        public String getHostname() {
            return "localhost";
        }
    }
    
}
