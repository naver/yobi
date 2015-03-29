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

import java.security.PublicKey;

import models.UserSshKey;

import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import utils.Constants;

class SshPublicKeyAuth implements PublickeyAuthenticator {
    @Override
    public boolean authenticate(final String username, final PublicKey key, final ServerSession session) {
        // if username isn't 'yobi' -> deny (username@domain)
        if (!username.equals(Constants.SSH_USERNAME)) {
            return false;
        }
        final String clientkey = UserSshKey.makePublicKeyB64(key);
        final UserSshKey result = UserSshKey.findByKey(clientkey);

        if (result != null) {
            // if public key exists
            result.updateLastUsedDate();
            session.setAttribute(SshServerSessionFactory.USER_KEY, result.user);
            return true;
        }
        // if public key not exists
        return false;
    }
}
