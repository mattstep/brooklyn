package org.overpaas.event;

import groovy.lang.Closure;

import org.overpaas.types.Sensor;
import org.overpaas.types.SensorEvent;

public class EventFilters {
    private EventFilters() {}
    
    public static <T> EventFilter<T> sensorName(final String name) {
        return new EventFilter<T>() {
            public boolean apply(SensorEvent<T> event) {
                Sensor<T> sensor = event.getSensor();
                return sensor.name.equals(name);
            }
        };
    }
 
    public static <T> EventFilter<T> sensorValue(final T value) {
        return new EventFilter<T>() {
            public boolean apply(SensorEvent<T> event) {
                return event.getValue().equals(value);
            }
        };
    }
    
    public static <T> EventFilter<T> sensor(final Closure<Boolean> expr) {
        return new EventFilter<T>() {
            public boolean apply(SensorEvent<T> event) {
                return expr.call(event.getValue());
            }
        };
    }
    
    public static <T> EventFilter<T> entityId(final String id) {
        return new EventFilter<T>() {
            public boolean apply(SensorEvent<T> event) {
                return event.getEntity().getId().equals(id);
            }
        };
    }
    
    public static <T> EventFilter<T> all() {
        return new EventFilter<T>() {
            public boolean apply(SensorEvent<T> sensor) {
                return true;
            }
        };
    }
}