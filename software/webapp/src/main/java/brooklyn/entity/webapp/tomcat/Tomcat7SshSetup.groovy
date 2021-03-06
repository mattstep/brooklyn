package brooklyn.entity.webapp.tomcat

import static brooklyn.entity.webapp.PortPreconditions.checkPortValid

import java.util.List
import java.util.Map

import brooklyn.entity.basic.Attributes
import brooklyn.entity.webapp.PortPreconditions;
import brooklyn.location.PortRange
import brooklyn.location.basic.SshMachineLocation
import brooklyn.util.SshBasedJavaWebAppSetup

/**
 * Start a {@link TomcatServer} in a {@link Location} accessible over ssh.
 */
public class Tomcat7SshSetup extends SshBasedJavaWebAppSetup {
	
    public static final String DEFAULT_VERSION = "7.0.27"
    public static final String DEFAULT_INSTALL_DIR = DEFAULT_INSTALL_BASEDIR+"/"+"tomcat"
    public static final String DEFAULT_DEPLOY_SUBDIR = "webapps"
    
    /**
     * Tomcat insists on having a port you can connect to for the sole purpose of shutting it down;
     * don't see an easy way to disable it; causes collisions in its default location of 8005,
     * so moving it to some anonymous high-numbered location
     */
    private int tomcatShutdownPort;
    
    public static Tomcat7SshSetup newInstance(TomcatServer entity, SshMachineLocation machine) {
        Integer suggestedTomcatVersion = entity.getConfig(TomcatServer.SUGGESTED_VERSION)
        String suggestedInstallDir = entity.getConfig(TomcatServer.SUGGESTED_INSTALL_DIR)
        String suggestedRunDir = entity.getConfig(TomcatServer.SUGGESTED_RUN_DIR)
        PortRange suggestedJmxPort = entity.getConfig(TomcatServer.JMX_PORT)
        PortRange suggestedShutdownPort = entity.getConfig(TomcatServer.SUGGESTED_SHUTDOWN_PORT)
        PortRange suggestedHttpPort = entity.getConfig(TomcatServer.HTTP_PORT)
        Map<String,Map<String,String>> propFilesToGenerate = entity.getConfig(TomcatServer.PROPERTY_FILES) ?: [:]
        
        String version = suggestedTomcatVersion ?: DEFAULT_VERSION
        String installDir = suggestedInstallDir ?: "$DEFAULT_INSTALL_DIR/${version}/apache-tomcat-${version}"
//        String runDir = suggestedRunDir ?: "$BROOKLYN_HOME_DIR/${entity.application.id}/tomcat-${entity.id}"
//        String deployDir = "${runDir}/$DEFAULT_DEPLOY_SUBDIR"
//        String logFileLocation = "$runDir/logs/catalina.out"

        int jmxPort = checkPortValid(machine.obtainPort(suggestedJmxPort), "jmxPort (suggested $suggestedJmxPort)")
        int httpPort = checkPortValid(machine.obtainPort(suggestedHttpPort), "httpPort (suggested $suggestedHttpPort)")
        int shutdownPort = checkPortValid(machine.obtainPort(suggestedShutdownPort), "shutdownPort (suggested $suggestedShutdownPort)")

        Tomcat7SshSetup result = new Tomcat7SshSetup(entity, machine)
        result.setJmxPort(jmxPort)
        result.setHttpPort(httpPort)
        result.setShutdownPort(shutdownPort)
        result.setVersion(version)
        result.setInstallDir(installDir)
        result.setDeployDir("${result.runDir}/$DEFAULT_DEPLOY_SUBDIR")
//        result.setRunDir(runDir)
        result.setEnvironmentPropertyFiles(propFilesToGenerate)
		entity.setAttribute(Attributes.LOG_FILE_LOCATION, "${result.runDir}/logs/catalina.out")
		
		return result
    }
    
    public Tomcat7SshSetup(TomcatServer entity, SshMachineLocation machine) {
        super(entity, machine)
    }

    public void setShutdownPort(int val) {
        tomcatShutdownPort = val
    }
    
    @Override
    protected void setEntityAttributes() {
		super.setEntityAttributes()
        entity.setAttribute(TomcatServer.TOMCAT_SHUTDOWN_PORT, tomcatShutdownPort)
    }
    
    @Override
    public List<String> getInstallScript() {
        makeInstallScript([
                "wget http://download.nextag.com/apache/tomcat/tomcat-7/v${version}/bin/apache-tomcat-${version}.tar.gz",
                "tar xvzf apache-tomcat-${version}.tar.gz",
            ])
    }

    public List<String> getRunScript() {
        List<String> script = [
            "cd ${runDir}",
			"${installDir}/bin/startup.sh",
        ]
        return script
    }
    
    public Map<String, String> getShellEnvironment() {
        def result = super.getShellEnvironment();
		result << [
    			"CATALINA_BASE" : "${runDir}",
    			"CATALINA_OPTS" : result.JAVA_OPTS,
    			"CATALINA_PID" : "pid.txt" ]
    }

    @Override
    public List<String> getConfigScript() {
        List<String> script = [
            "mkdir -p ${runDir}",
            "cd ${runDir}",
            "mkdir conf logs webapps temp",
            "cp ${installDir}/conf/{server,web}.xml conf/",
            "sed -i.bk s/8080/${httpPort}/g conf/server.xml",
            "sed -i.bk s/8005/${tomcatShutdownPort}/g conf/server.xml",
            "sed -i.bk /8009/D conf/server.xml",
        ]
        return script
    }

    /** @see brooklyn.util.SshBasedJavaAppSetup#getCheckRunningScript() */
    public List<String> getCheckRunningScript() {
       return makeCheckRunningScript("tomcat")
    }
    
    @Override
    protected void postShutdown() {
        machine.releasePort(jmxPort)
        machine.releasePort(httpPort);
        machine.releasePort(tomcatShutdownPort);
    }
}
