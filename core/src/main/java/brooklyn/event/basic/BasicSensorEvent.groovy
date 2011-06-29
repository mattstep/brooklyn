package brooklyn.event.basic;

import groovy.transform.InheritConstructors

import brooklyn.entity.Entity
import brooklyn.event.SensorEvent
import brooklyn.event.Sensor

/**
 * A {@link SensorEvent} containing data from a {@link Sensor} generated by an {@link Entity}.
 * 
 * @see AttributeEvent
 * @see LogEvent
 */
public class BasicSensorEvent<T> implements SensorEvent<T> {
    private final Sensor<T> sensor;
    private final Entity source;
    private final T value;
    
    public T getValue() { return value; }

    public Sensor<T> getSensor() { return sensor; }

    public Entity getSource() { return source; }
    
    public BasicSensorEvent(Sensor<T> sensor, Entity source, T value) {
        this.sensor = sensor;
        this.source = source;
        this.value = value;
    }
}

@InheritConstructors
public class LogEvent<String> extends BasicSensorEvent<String> {
    String level
    String topic
    
    public LogEvent(Sensor<String> sensor, Entity source, String value, String level, String topic) {
        this(sensor, source, value)
        
        this.level = level
        this.topic = topic
    }
}