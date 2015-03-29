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
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;

import utils.Constants;

abstract class AbstractSshBaseCommand implements Command, SessionAware {
    protected InputStream in;
    protected OutputStream out;
    protected OutputStream err;
    protected ExitCallback callback;
    protected ServerSession session;
    protected String service;
    protected String ownerName;
    protected String projectName;

    public AbstractSshBaseCommand(final String service, final String ownerName, final String projectName) {
        this.service = service;
        this.ownerName = ownerName;
        this.projectName = projectName;
    }

    @Override
    public void setInputStream(final InputStream in) {
        synchronized (this) {
            this.in = in;
        }
    }

    @Override
    public void setOutputStream(final OutputStream out) {
        synchronized (this) {
            this.out = out;
        }
    }

    @Override
    public void setErrorStream(final OutputStream err) {
        this.err = err;
    }

    @Override
    public void setExitCallback(final ExitCallback callback) {
        this.callback = callback;
    }

    @Override
    public void setSession(final ServerSession session) {
        this.session = session;
    }

    @Override
    public void start(final Environment env) throws IOException {
        final Thread thread = new Thread(new RunnableNewTask());
        thread.start();
    }

    protected abstract void runTask() throws IOException, UnsupportedOperationException, ServletException;

    private class RunnableNewTask implements Runnable {
        @Override
        public void run() {
            try {
                runTask();
                callback.onExit(Constants.NOT_ERROR_EXIT);
            } catch (IOException e) {
                callback.onExit(Constants.WIDE_ERROR_EXIT);
            } catch (UnsupportedOperationException e) {
                callback.onExit(Constants.COMMAND_ERROR_EXIT);
            } catch (ServletException e) {
                callback.onExit(Constants.WIDE_ERROR_EXIT);
            }
        }
    }
}
