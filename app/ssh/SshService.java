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

import javax.servlet.ServletException;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.ReceivePack;
import org.eclipse.jgit.transport.UploadPack;

import playRepository.GitRepository;
import playRepository.PlayRepository;
import playRepository.RepositoryService;
import utils.AccessControl;
import models.Project;
import models.User;
import models.enumeration.Operation;

class SshService extends AbstractSshBaseCommand {
    public SshService(final String command, final String ownerName, final String projectName) {
        super(command, ownerName, projectName);
    }

    private static boolean isAllowed(final User user, final Project project, final String service) throws
    UnsupportedOperationException, IOException, ServletException {
        Operation operation = Operation.UPDATE;
        if (service.equals("git-upload-pack")) {
            operation = Operation.READ;
        }

        final PlayRepository repository = RepositoryService.getRepository(project);
        return AccessControl
                .isAllowed(user, repository.asResource(), operation);
    }

    @Override
    protected void runTask() throws IOException, UnsupportedOperationException, ServletException {
        final StringBuilder msg = new StringBuilder();
        final Project project = Project.findByOwnerAndProjectName(ownerName, projectName);

        if (project == null) {
            msg.append(ownerName).append('/').append(projectName)
            .append(" is not exist project\n");
            err.write(Constants.encode(msg.toString()));
            err.flush();
            throw new IOException();
        }

        if (!project.vcs.equals(RepositoryService.VCS_GIT)) {
            msg.append(ownerName).append('/').append(projectName)
            .append(" is not git repository\n");
            err.write(Constants.encode(msg.toString()));
            err.flush();
            throw new IOException();
        }

        final User user = session.getAttribute(SshServerSessionFactory.USER_KEY);

        if (!isAllowed(user, project, service)) {
            msg.append(ownerName)
            .append(user.loginId)
            .append(" has no permission to ")
            .append(ownerName).append('/').append(projectName)
            .append('\n');
            err.write(Constants.encode(msg.toString()));
            err.flush();
            throw new IOException();
        }
        synchronized (this) {
            try {
                switch (service) {
                    case "git-upload-pack":
                        final UploadPack up = new UploadPack(GitRepository.buildGitRepository(project));
                        up.upload(in, out, err);
                        break;
                    case "git-receive-pack":
                        final ReceivePack receive = new ReceivePack(GitRepository.buildGitRepository(project));
                        receive.receive(in, out, err);
                        break;
                    default:
                        in.close();
                        break;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void destroy() {
    }
}
