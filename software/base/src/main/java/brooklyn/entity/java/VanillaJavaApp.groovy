package brooklyn.entity.java

import groovy.time.TimeDuration

import java.util.List
import java.util.concurrent.TimeUnit

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import brooklyn.entity.Entity
import brooklyn.entity.basic.SoftwareProcessEntity
import brooklyn.entity.basic.UsesJava
import brooklyn.entity.basic.UsesJavaMXBeans
import brooklyn.entity.basic.UsesJmx
import brooklyn.entity.basic.lifecycle.JavaStartStopSshDriver
import brooklyn.event.adapter.ConfigSensorAdapter
import brooklyn.event.adapter.JmxSensorAdapter
import brooklyn.event.basic.BasicConfigKey
import brooklyn.location.basic.SshMachineLocation
import brooklyn.util.flags.SetFromFlag


public class VanillaJavaApp extends SoftwareProcessEntity implements UsesJava, UsesJmx, UsesJavaMXBeans {

    // FIXME classpath values: need these to be downloaded and installed?
    
    // TODO Make jmxPollPeriod @SetFromFlag easier to use: currently a confusion over long and TimeDuration, and 
    // no ability to set default value (can't just set field because config vals read/set in super-constructor :-(
     
    private static final Logger log = LoggerFactory.getLogger(VanillaJavaApp.class)
    
    @SetFromFlag("args")
    public static final BasicConfigKey<String> ARGS = [ List, "vanillaJavaApp.args", "Arguments for launching the java app", [] ]
    
    @SetFromFlag
    String main

    @SetFromFlag
    List<String> classpath

    @SetFromFlag
    long jmxPollPeriod

    JmxSensorAdapter jmxAdapter
    
    public VanillaJavaApp(Map props=[:], Entity owner=null) {
        super(props, owner)
    }

    @Override
    protected void connectSensors() {
        super.connectSensors();
        
        sensorRegistry.register(new ConfigSensorAdapter());
        TimeDuration jmxPollPeriod = (jmxPollPeriod > 0 ? jmxPollPeriod : 500)*TimeUnit.MILLISECONDS
        jmxAdapter = sensorRegistry.register(new JmxSensorAdapter(period:jmxPollPeriod));
        
        JavaAppUtils.connectMXBeanSensors(this, jmxAdapter)
    }
    
    @Override
    protected void preStop() {
        jmxAdapter?.deactivateAdapter();
        super.preStop();
    }

    public VanillaJavaAppSshDriver newDriver(SshMachineLocation loc) {
        new VanillaJavaAppSshDriver(this, loc)
    }
}

public class VanillaJavaAppSshDriver extends JavaStartStopSshDriver {

    public VanillaJavaAppSshDriver(VanillaJavaApp entity, SshMachineLocation machine) {
        super(entity, machine)
    }

    public VanillaJavaApp getEntity() { super.getEntity() }

    protected String getLogFileLocation() {
        return "$runDir/console"
    }
    
    @Override
    public void install() {
        // TODO install classpath entries?
        
        newScript(INSTALLING).
            failOnNonZeroResultCode()
            .execute();
    }

    @Override
    public void customize() {
        // no-op
    }
    
    @Override
    public void launch() {
        // TODO Use JAVA_OPTIONS config, once that is fixed to support more than sys properties
        // TODO quote args?
        String classpath = entity.classpath.join(":")
        String clazz = entity.main
        String args = entity.getConfig(VanillaJavaApp.ARGS).join(" ")
        
        newScript(LAUNCHING, usePidFile:true).
            body.append(
                "java \$JAVA_OPTS -cp \"$classpath\" $clazz $args "+
                    " >> $runDir/console 2>&1 </dev/null &",
            ).execute();
    }
    
    @Override
    public boolean isRunning() {
        //TODO use PID instead
        newScript(CHECK_RUNNING, usePidFile: true)
                .execute() == 0;
    }
    
    @Override
    public void stop() {
        newScript(STOPPING, usePidFile: true)
                .execute();
    }

    @Override
    protected List<String> getCustomJavaConfigOptions() {
        return super.getCustomJavaConfigOptions() + ["-Xms128m", "-Xmx512m", "-XX:MaxPermSize=512m"]
    }
}
