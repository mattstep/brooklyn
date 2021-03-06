package brooklyn.entity.messaging.activemq

import groovy.lang.MetaClass

import java.util.Collection
import java.util.Map
import java.util.concurrent.TimeUnit

import javax.management.ObjectName

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import brooklyn.entity.Entity
import brooklyn.entity.basic.Attributes
import brooklyn.entity.basic.SoftwareProcessEntity
import brooklyn.entity.basic.UsesJmx
import brooklyn.entity.messaging.JMSBroker
import brooklyn.entity.messaging.JMSDestination
import brooklyn.entity.messaging.Queue
import brooklyn.entity.messaging.Topic
import brooklyn.event.adapter.JmxHelper
import brooklyn.event.adapter.JmxSensorAdapter
import brooklyn.event.adapter.SensorRegistry
import brooklyn.event.basic.BasicConfigKey
import brooklyn.event.basic.PortAttributeSensorAndConfigKey
import brooklyn.location.basic.SshMachineLocation
import brooklyn.util.flags.SetFromFlag
import brooklyn.util.internal.Repeater

/**
 * An {@link brooklyn.entity.Entity} that represents a single ActiveMQ broker instance.
 */
public class ActiveMQBroker extends JMSBroker<ActiveMQQueue, ActiveMQTopic> implements UsesJmx {
	private static final Logger log = LoggerFactory.getLogger(ActiveMQBroker.class)

    @SetFromFlag("version")
    public static final BasicConfigKey<String> SUGGESTED_VERSION = [ SoftwareProcessEntity.SUGGESTED_VERSION, "5.5.1" ]

    @SetFromFlag("openWirePort")
	public static final PortAttributeSensorAndConfigKey OPEN_WIRE_PORT = [ "openwire.port", "OpenWire port", "61616+" ]

	public ActiveMQBroker(Map properties=[:], Entity owner=null) {
		super(properties, owner)

        //TODO test, then change keys to be jmxUser, jmxPassword, configurable on the keys themselves
		setConfigIfValNonNull(Attributes.JMX_USER, properties.user ?: "admin")
		setConfigIfValNonNull(Attributes.JMX_PASSWORD, properties.password ?: "activemq")
	}

	public void setBrokerUrl() {
		setAttribute(BROKER_URL, String.format("tcp://%s:%d", getAttribute(HOSTNAME), getAttribute(OPEN_WIRE_PORT)))
	}
	
	public ActiveMQQueue createQueue(Map properties) {
		return new ActiveMQQueue(properties);
	}

	public ActiveMQTopic createTopic(Map properties) {
		return new ActiveMQTopic(properties);
	}

	public ActiveMQSshDriver newDriver(SshMachineLocation machine) {
        return new ActiveMQSshDriver(this, machine)        
	}

    transient JmxSensorAdapter jmxAdapter;
    
    @Override     
    protected void connectSensors() {
       jmxAdapter = sensorRegistry.register(new JmxSensorAdapter());
       jmxAdapter.objectName("org.apache.activemq:BrokerName=localhost,Type=Broker")
           .attribute("BrokerId").subscribe(SERVICE_UP, { it as Boolean });
       jmxAdapter.activateAdapter()
	}

	@Override
	public Collection<String> toStringFieldsToInclude() {
		return super.toStringFieldsToInclude() + ['openWirePort']
	}

	public void waitForServiceUp() {
		if (!Repeater.create(timeout: 60*TimeUnit.SECONDS)
			    .rethrowException().repeat().every(1*TimeUnit.SECONDS).until { getAttribute(SERVICE_UP) }.
                run()) {
            throw new IllegalStateException("Could not connect via JMX to determine ${this} is up");
		}
		log.info("started JMS $this")
	}
    
}

public abstract class ActiveMQDestination extends JMSDestination {
	protected ObjectName broker
	protected transient SensorRegistry sensorRegistry
	protected transient JmxSensorAdapter jmxAdapter

	public ActiveMQDestination(Map properties=[:], Entity owner=null) {
		super(properties, owner)
	}
    
	public void init() {
        //assume just one BrokerName at this endpoint
		broker = new ObjectName("org.apache.activemq:BrokerName=localhost,Type=Broker")
        if (!sensorRegistry) sensorRegistry = new SensorRegistry(this)
        def helper = new JmxHelper(owner)
        helper.connect();
        jmxAdapter = sensorRegistry.register(new JmxSensorAdapter(helper));
	}
}

public class ActiveMQQueue extends ActiveMQDestination implements Queue {
    public static final Logger log = LoggerFactory.getLogger(ActiveMQQueue.class);
            
	public ActiveMQQueue(Map properties=[:], Entity owner=null) {
		super(properties, owner)
	}

	@Override
	public void init() {
		setAttribute QUEUE_NAME, name
		super.init()
	}

	public void create() {
		if (log.isDebugEnabled()) log.debug("${this} adding queue ${name} to broker "+jmxAdapter.helper.getAttribute(broker, "BrokerId"))
        
		jmxAdapter.helper.operation(broker, "addQueue", name)
        
        connectSensors();
        sensorRegistry.activateAdapters();
	}

	public void delete() {
		jmxAdapter.helper.operation(broker, "removeQueue", name)
        sensorRegistry.deactivateAdapters()
	}

    @Override
    public void connectSensors() {
        String queue = "org.apache.activemq:BrokerName=localhost,Type=Queue,Destination=${name}"
        jmxAdapter.objectName(queue).attribute("QueueSize").subscribe(QUEUE_DEPTH_MESSAGES)
    }

}

public class ActiveMQTopic extends ActiveMQDestination implements Topic {
	public ActiveMQTopic(Map properties=[:], Entity owner=null) {
		super(properties, owner)
	}

	@Override
	public void init() {
		setAttribute TOPIC_NAME, name
		super.init()
	}

	public void create() {
		jmxAdapter.helper.operation(broker, "addTopic", name)
		connectSensors()
        sensorRegistry.activateAdapters();
	}

	public void delete() {
		jmxAdapter.helper.operation(broker, "removeTopic", name)
		sensorRegistry.deactivateAdapters()
	}

	public void connectSensors() {
		//TODO add sensors for topics
	}
}
