package brooklyn.event;

import java.util.Map;

public class AttributeSensor<T> extends Sensor<T> {
    public AttributeSensor(String name, Class<T> type, Map<String, Object> properties) {
        super(name, type, properties);
    }
}