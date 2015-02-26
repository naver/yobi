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

import utils.Constants;

public class SshDaemon {
    private static final SshServer sshd = SshServer.setUpDefaultServer();

    public void start() {
        // Configure SshDaemon
        sshd.setPort(Constants.WELLKNOWN_SSH);
        sshd.setKeyPairProvider(new PEMGeneratorHostKeyProvider(Constants.HOST_KEY, Constants.ALG_RSA, Constants.SIZE_RSA));
        sshd.setSessionFactory(new SshServerSessionFactory());
        sshd.setPublickeyAuthenticator(new SshPublicKeyAuth());
        sshd.setCommandFactory(new SshCommandFactory());

        // Start SshDaemon
        try {
            sshd.start();
        } catch (IOException e) {
            play.Logger.error("SSHD isn't start", e);
            sshd.close(true);
        }
    }

    public void stop() {
        try {
            sshd.stop(true);
        } catch (InterruptedException e) {
            play.Logger.error("SSHD is stop anormaly", e);
        }
        sshd.close(true);
        sshd = null;
    }

    public static boolean isRunning() {
        return !sshd.isClosed();
    }

    public static int getCurrentPort() {
        return sshd.getPort();
    }
}
