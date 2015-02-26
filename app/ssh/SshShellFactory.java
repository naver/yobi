/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2015 NAVER Corp.
 * http://yobi.io
 *
 * @author Hyeok Oh, KiSeong Park
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
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;
import org.eclipse.jgit.lib.Constants;

/**
 * A factory for creating SshShell objects.
 * (If you don't want to gives a detailed rejection message you don't need to use this class)
 */
class SshShellFactory implements Factory<Command> {
    @Override
    public Command create() {
        return new SendMessage();
    }

    /**
     * This class gives a detailed rejection message to the user who direct access to the shell.
     * ex) Hi _____! You're successfully authenticated, but [Yobi] does not provide shell access.
     *
     * @author Hyeok Oh
     *
     */
    private static class SendMessage implements Command, SessionAware {
        /** The in. */
        private InputStream in;

        /** The out. */
        private OutputStream out;

        /** The err. */
        private OutputStream err;

        /** The exit. */
        private ExitCallback exit;

        /** The session. */
        private ServerSession session;

        @Override
        public void setInputStream(final InputStream in) {
            this.in = in;
        }

        @Override
        public void setOutputStream(final OutputStream out) {
            this.out = out;
        }

        @Override
        public void setErrorStream(final OutputStream err) {
            this.err = err;
        }

        @Override
        public void setExitCallback(final ExitCallback callback) {
            this.exit = callback;
        }

        @Override
        public void setSession(final ServerSession session) {
            this.session = session;
        }

        @Override
        public void start(final Environment env) throws IOException {
            final String user = session.getAttribute(SshServerSessionFactory.USER_KEY).loginId;
            final StringBuilder msgBuilder = new StringBuilder();

            msgBuilder.append("Hi ").append(user).append("! You've successfully authenticated, but [Yobi] does not provide shell access.\n");

            err.write(Constants.encode(msgBuilder.toString()));
            err.flush();

            in.close();
            out.close();
            err.close();
            exit.onExit(utils.Constants.COMMAND_ERROR_EXIT);
        }

        @Override
        public void destroy() {
            this.session = null;
        }
    }
}
