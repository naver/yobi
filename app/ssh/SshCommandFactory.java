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

import java.util.StringTokenizer;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.command.UnknownCommand;

import controllers.GitApp;

class SshCommandFactory implements CommandFactory {
    @Override
    public Command createCommand(final String inputCommand) {
        play.Logger.info("inputCommand {}", inputCommand);
        // args[0] => command ex:git-upload-pack, args[1] => pathName ex:username/repo.git
        StringTokenizer commandTokens = new StringTokenizer(inputCommand, " ");
        final String[] args = new String[commandTokens.countTokens()];
        for(int i = 0; commandTokens.hasMoreElements(); i++){
            args[i] = commandTokens.nextToken();
        }

        if (GitApp.isSupportedService(args[0])) {
            // names[0] => ownerName, names[1] => projectName
            final String[] names = args[1].replace("\'", "").substring(1).split("/");

            return new SshService(args[0], names[0], names[1]);
        }
        play.Logger.info("command with UnknownCommand");
        return new UnknownCommand(inputCommand);
    }
}
