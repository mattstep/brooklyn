/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package brooklyn.util.internal.ssh;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.util.internal.ssh.SshjTool.SshAction;

import com.google.common.base.Objects;
import com.google.common.net.HostAndPort;

public class SshjClientConnection implements SshAction<SSHClient> {

    private static final Logger LOG = LoggerFactory.getLogger(SshjClientConnection.class);

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        protected HostAndPort hostAndPort;
        protected String username;
        protected String password;
        protected String privateKey;
        protected File privateKeyFile;
        protected int connectTimeout;
        protected int sessionTimeout;
        protected boolean strictHostKeyChecking;

        public Builder hostAndPort(HostAndPort hostAndPort) {
            this.hostAndPort = hostAndPort;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String val) {
            this.password = val;
            return this;
        }

        public Builder privateKey(String val) {
            this.privateKey = val;
            return this;
        }

        public Builder privateKeyFile(File val) {
            this.privateKeyFile = val;
            return this;
        }
        
        public Builder strictHostKeyChecking(boolean val) {
            this.strictHostKeyChecking = val;
            return this;
        }

        public Builder connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder sessionTimeout(int sessionTimeout) {
            this.sessionTimeout = sessionTimeout;
            return this;
        }

        public SshjClientConnection build() {
            return new SshjClientConnection(this);
        }

        protected static Builder fromSSHClientConnection(SshjClientConnection in) {
            return new Builder().hostAndPort(in.getHostAndPort()).connectTimeout(in.getConnectTimeout()).sessionTimeout(
                    in.getSessionTimeout()).username(in.username).password(in.password).privateKey(in.privateKey).privateKeyFile(in.privateKeyFile);
        }
    }

    private final HostAndPort hostAndPort;
    private final String username;
    private final String password;
    private final String privateKey;
    private final File privateKeyFile;
    private final boolean strictHostKeyChecking;
    private final int connectTimeout;
    private final int sessionTimeout;
    
    SSHClient ssh;

    private SshjClientConnection(Builder builder) {
        this.hostAndPort = checkNotNull(builder.hostAndPort);
        this.username = builder.username;
        this.password = builder.password;
        this.privateKey = builder.privateKey;
        this.privateKeyFile = builder.privateKeyFile;
        this.strictHostKeyChecking = builder.strictHostKeyChecking;
        this.connectTimeout = builder.connectTimeout;
        this.sessionTimeout = builder.sessionTimeout;
    }

    public boolean isConnected() {
        return ssh != null && ssh.isConnected();
    }

    @Override
    public void clear() {
        if (ssh != null && ssh.isConnected()) {
            try {
                ssh.disconnect();
            } catch (IOException e) {
                LOG.debug("<< exception disconnecting from {}: {}", e, e.getMessage());
            }
            ssh = null;
        }
    }

    @Override
    public SSHClient create() throws Exception {
        ssh = new net.schmizz.sshj.SSHClient();
        if (!strictHostKeyChecking) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
        }
        if (connectTimeout != 0) {
            ssh.setConnectTimeout(connectTimeout);
        }
        if (sessionTimeout != 0) {
            ssh.setTimeout(sessionTimeout);
        }
        ssh.connect(hostAndPort.getHostText(), hostAndPort.getPortOrDefault(22));
        
        if (password != null) {
            ssh.authPassword(username, password);
        } else if (privateKey != null) {
            OpenSSHKeyFile key = new OpenSSHKeyFile();
            key.init(privateKey, null);
            ssh.authPublickey(username, key);
        } else if (privateKeyFile != null) {
            OpenSSHKeyFile key = new OpenSSHKeyFile();
            key.init(privateKeyFile);
            ssh.authPublickey(username, key);
        } else {
            // Accept defaults (in ~/.ssh)
            ssh.authPublickey(username);
        }
        
        return ssh;
    }

    /**
     * @return host and port, where port if not present defaults to {@code 22}
     */
    public HostAndPort getHostAndPort() {
        return hostAndPort;
    }

    /**
     * @return username used in this ssh
     */
    public String getUsername() {
        return username;
    }

    /**
     * 
     * @return how long to wait for the initial connection to be made
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * 
     * @return how long to keep the ssh open, or {@code 0} for indefinitely
     */
    public int getSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * 
     * @return the current ssh or {@code null} if not connected
     */
    public SSHClient getSSHClient() {
        return ssh;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SshjClientConnection that = SshjClientConnection.class.cast(o);
        return equal(this.hostAndPort, that.hostAndPort) && equal(this.username, that.username) 
                && equal(this.password, that.password) && equal(this.privateKey, that.privateKey)
                && equal(this.privateKeyFile, that.privateKeyFile) && equal(this.ssh, that.ssh);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(hostAndPort, username, password, privateKey, ssh);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper("").add("hostAndPort", hostAndPort).add("user", username)
                .add("ssh", ssh != null ? ssh.hashCode() : null).add("password", password)
                .add("privateKeyFile", privateKeyFile).add("privateKey", (privateKey != null ? null : "xxxxxx"))
                .add("connectTimeout", connectTimeout).add("sessionTimeout", sessionTimeout).toString();
    }
}
