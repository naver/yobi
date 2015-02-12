/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2015 NAVER Corp.
 * http://yobi.io
 *
 * @author Hyeok Oh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ssh;

import java.io.IOException;

import org.apache.sshd.SshServer;
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider;

import utils.Config;
import utils.Constants;
import utils.Diagnostic;
import utils.SimpleDiagnostic;

public class SshDaemon {
    private static final SshServer sshd = SshServer.setUpDefaultServer();

    public void start() {
        boolean isOutOfRange = false;

        // GetPortNumber and check available
        int sshPort = Config.getSshPort();

        if (!(0 <= sshPort && sshPort <= 65535)) {
            isOutOfRange = true;
        }

        if (!isOutOfRange) {
            // Configure SshDaemon
            sshd.setPort(sshPort);
            sshd.setKeyPairProvider(new PEMGeneratorHostKeyProvider(Constants.HOST_KEY, Constants.ALG_RSA, Constants.SIZE_RSA));
            sshd.setSessionFactory(new SshServerSessionFactory());
            sshd.setPublickeyAuthenticator(new SshPublicKeyAuth());
            sshd.setCommandFactory(new SshCommandFactory());
            sshd.setShellFactory(new SshShellFactory());

            // Start SshDaemon
            try {
                sshd.start();
            } catch (IOException e) {
                // Port conflict. this port is already used other program
                sshd.close(true);
            }
        }
        else {
            sshd.close(true);
        }

        final boolean finalIsOutOfRange = isOutOfRange;
        Diagnostic.register(new SimpleDiagnostic() {
            @Override
            public String checkOne() {
                if (finalIsOutOfRange) {
                    return "Ssh Daemon is not running\n- port out of range. port must be in 0 ~ 65535";
                } else if (sshd.isClosed()) {
                    return "Ssh Daemon is not running\n- port conflict or other problems";
                } else {
                    return null;
                }
            }
        });
    }

    public void stop() {
        try {
            sshd.stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sshd.close(true);
    }

    public static boolean isRunning() {
        return !sshd.isClosed();
    }

    public static int getCurrentPort() {
        return sshd.getPort();
    }
}
