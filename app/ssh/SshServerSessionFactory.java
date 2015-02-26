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

import models.User;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.sshd.common.Session.AttributeKey;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.io.mina.MinaSession;
import org.apache.sshd.common.session.AbstractSession;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.session.SessionFactory;

class SshServerSessionFactory extends SessionFactory {
    public static final AttributeKey<User> USER_KEY = new AttributeKey<>();

    @Override
    protected AbstractSession createSession(final IoSession io) throws Exception {
        if (io instanceof MinaSession) {
            if (((MinaSession)io).getSession().getConfig() instanceof SocketSessionConfig) {
                ((SocketSessionConfig)((MinaSession)io).getSession().getConfig()).setKeepAlive(true);
            }
        }

        final ServerSession session = (ServerSession)super.createSession(io);

        return session;
    }
}
