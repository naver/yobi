package playRepository.support;

import models.Project;
import models.PullRequest;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import playRepository.GitRepository;
import playRepository.RepositoryService;
import utils.FileUtil;

import java.io.File;
import java.io.IOException;

/**
 * {@link models.PullRequest}에 들어있는 {@code toProject}를 clone 하고
 * {@code toProject}의 {@code toBranch}와 {@code fromProject}의 {@code fromBranch}를 fetch 한다.
 * 그런 다음 {@link CloneAndFetchOperation}을 호출하여 이 후 작업을 진행 한다.
 *
 * when: {@link GitRepository#merge(models.PullRequest)}, {@link GitRepository#isSafeToMerge(models.PullRequest)}
 * , {@link GitRepository#getPullingCommits(models.PullRequest)}에서 반복되는 예외 처리와 자원 반납 코드를 줄일 때 사용한다.
 */
public class CloneAndFetchTemplate {

    private PullRequest pullRequest;
    private String directory;
    private Repository cloneRepository;
    private String destToBranchName;
    private String destFromBranchName;

    public String getDirectory() {
        return directory;
    }

    public Repository getCloneRepository() {
        return cloneRepository;
    }

    public String getDestToBranchName() {
        return destToBranchName;
    }

    public String getDestFromBranchName() {
        return destFromBranchName;
    }

    public CloneAndFetchTemplate(PullRequest pullRequest) {
        this.pullRequest = pullRequest;
    }

    public void runWith(CloneAndFetchOperation operation) {
        try {
            cloneRepository = buildCloneRepository();

            String srcToBranchName = pullRequest.toBranch;
            destToBranchName = srcToBranchName + "-to";
            String srcFromBranchName = pullRequest.fromBranch;
            destFromBranchName = srcFromBranchName + "-from";

            // 코드를 받아오면서 생성될 브랜치를 미리 삭제한다.
            deleteBranch(cloneRepository, destToBranchName);
            deleteBranch(cloneRepository, destFromBranchName);

            // 코드를 받을 브랜치에 해당하는 코드를 fetch 한다.
            fetch(cloneRepository, pullRequest.toProject, srcToBranchName, destToBranchName);

            // 코드를 보내는 브랜치에 해당하는 코드를 fetch 한다.
            fetch(cloneRepository, pullRequest.fromProject, srcFromBranchName, destFromBranchName);

            operation.invoke(this);
        } catch (GitAPIException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            if(cloneRepository != null) {
                cloneRepository.close();
            }
        }
    }

    public void deleteBranch(Repository repository, String branchName) throws GitAPIException {
        new Git(repository).branchDelete()
                .setBranchNames(branchName)
                .setForce(true)
                .call();
    }

    public Repository buildCloneRepository() throws GitAPIException, IOException {
        Project cloneProject = createCloneProject(pullRequest.toProject);

        // merge 할 때 사용할 Git 저장소 디렉토리 경로를 생성한다.
        directory = GitRepository.getDirectoryForMerging(cloneProject.owner, cloneProject.name);

        // clone으로 생성될 디렉토리를 미리 삭제한다.
        FileUtil.rm_rf(new File(directory));

        // 코드 받을 쪽 프로젝트를 clone 한다.
        cloneRepository(pullRequest.toProject, directory);
        return buildRepository(directory);
    }

    /**
     * {@link Project}의 Git 저장소를 {@code workingTreePath}에
     * non-bare 모드로 clone 한다.
     *
     * @param project clone 받을 프로젝트
     * @param workingTreePath clone 프로젝트를 생성할 워킹트리 경로
     * @throws GitAPIException
     * @throws IOException
     */
    private void cloneRepository(Project project, String workingTreePath) throws GitAPIException, IOException {
        Git.cloneRepository()
                .setURI(GitRepository.getGitDirectoryURL(project))
                .setDirectory(new File(workingTreePath))
                .call();
    }

    public Project createCloneProject(Project original) {
        Project forkingProject = new Project();
        forkingProject.owner = original.owner;
        forkingProject.name = original.name;
        forkingProject.vcs = RepositoryService.VCS_GIT;
        return forkingProject;
    }

    /**
     * {@code workingTreePath}를 기준으로 {@link Repository}를 생성한다.
     *
     * @param workingTreePath 워킹트리 경로
     * @return
     * @throws IOException
     */
    private Repository buildRepository(String workingTreePath) throws IOException {
        return new RepositoryBuilder()
                .setWorkTree(new File(workingTreePath))
                .setGitDir(new File(workingTreePath + "/.git"))
                .build();
    }

    /**
     * {@link Project}의 Git 저장소의 {@code fromBranch}에 있는 내용을
     * 현재 사용중인 Git 저장소의 {@code toBranch}로 fetct 한다.
     *
     * @param repository fetch 실행할 Git 저장소
     * @param project fetch 대상 프로젝트
     * @param fromBranch fetch source 브랜치
     * @param toBranch fetch destination 브랜치
     * @throws GitAPIException
     * @throws IOException
     * @see <a href="https://www.kernel.org/pub/software/scm/git/docs/git-fetch.html">git-fetch</a>
     */
    public void fetch(Repository repository, Project project, String fromBranch, String toBranch) throws GitAPIException, IOException {
        new Git(repository).fetch()
                .setRemote(GitRepository.getGitDirectoryURL(project))
                .setRefSpecs(new RefSpec(fromBranch + ":" + toBranch))
                .call();
    }
}