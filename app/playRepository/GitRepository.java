package playRepository;

import controllers.ProjectApp;
import models.Project;
import models.PullRequest;
import models.enumeration.ResourceType;
import models.enumeration.State;
import models.resource.Resource;
import org.codehaus.jackson.node.ObjectNode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.tmatesoft.svn.core.SVNException;
import play.Logger;
import play.libs.Json;
import playRepository.support.CloneAndFetchOperation;
import playRepository.support.CloneAndFetchTemplate;
import utils.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Git 저장소
 */
public class GitRepository implements PlayRepository {

    /**
     * Git 저장소 베이스 디렉토리
     */
    private static String repoPrefix = "repo/git/";

    /**
     * Git 저장소 베이스 디렉토리
     */
    private static String repoForMergingPrefix = "repo/git-merging/";

    public static String getRepoPrefix() {
        return repoPrefix;
    }

    public static void setRepoPrefix(String repoPrefix) {
        GitRepository.repoPrefix = repoPrefix;
    }

    public static String getRepoForMergingPrefix() {
        return repoForMergingPrefix;
    }

    public static void setRepoForMergingPrefix(String repoForMergingPrefix) {
        GitRepository.repoForMergingPrefix = repoForMergingPrefix;
    }

    private final Repository repository;
    private final String ownerName;
    private final String projectName;

    /**
     * 매개변수로 전달받은 {@code ownerName}과 {@code projectName}을 사용하여 Git 저장소를 참조할 {@link GitRepository}를 생성한다.
     *
     * @param ownerName
     * @param projectName
     * @throws IOException
     * @see #buildGitRepository(String, String)
     */
    public GitRepository(String ownerName, String projectName) throws IOException {
        this.ownerName = ownerName;
        this.projectName = projectName;
        this.repository = buildGitRepository(ownerName, projectName);
    }

    /**
     * {@code project} 정보를 사용하여 Git 저장소를 참조할 {@link GitRepository}를 생성한다.
     *
     * @param project
     * @throws IOException
     * @see #GitRepository(String, String)
     */
    public GitRepository(Project project) throws IOException {
        this(project.owner, project.name);
    }

    /**
     * {@code ownerName}과 {@code projectName}을 받아서 {@link Repository} 객체를 생성한다.
     *
     * 실제 저장소를 생성하는건 아니고, Git 저장소를 참조할 수 있는 {@link Repository} 객체를 생성한다.
     * 생성된 {@link Repository} 객체를 사용해서 기존에 만들어져있는 Git 저장소를 참조할 수도 있고, 새 저장소를 생성할 수도 있다.
     *
     * @param ownerName
     * @param projectName
     * @return
     * @throws IOException
     */
    public static Repository buildGitRepository(String ownerName, String projectName) throws IOException {
        return new RepositoryBuilder().setGitDir(
                new File(getGitDirectory(ownerName, projectName))).build();
    }

    /**
     * {@code project}의 {@link Project#owner}와 {@link Project#name}을 사용하여 {@link Repository} 객체를 생성한다.
     *
     * @param project
     * @return
     * @throws IOException
     * @see #buildGitRepository(String, String)
     */
    public static Repository buildGitRepository(Project project) throws IOException {
        return buildGitRepository(project.owner, project.name);
    }

    /**
     * bare 모드로 Git 저장소를 생성한다.
     *
     * @throws IOException
     * @see <a href="https://www.kernel.org/pub/software/scm/git/docs/gitglossary.html#def_bare_repository">bare repository</a>
     * @see Repository#create()
     */
    @Override
    public void create() throws IOException {
        this.repository.create(true);
    }

    /**
     * {@link Constants#HEAD}에서 {@code path}가 디렉토리일 경우에는 해당 디렉토리에 들어있는 파일과 디렉토리 목록을 JSON으로 반환하고,
     * 파일일 경우에는 해당 파일 정보를 JSON으로 반환한다.
     *
     * @param path
     * @return {@code path}가 디렉토리이면 그 안에 들어있는 파일과 디렉토리 목록을 담고있는 JSON, 파일이면 해당 파일 정보를 담고 있는 JSON
     * @throws IOException
     * @throws NoHeadException
     * @throws GitAPIException
     * @throws SVNException
     * @see #findFileInfo(String, String)
     */
    @Override
    public ObjectNode findFileInfo(String path) throws IOException, NoHeadException, GitAPIException, SVNException {
        return findFileInfo(null, path);
    }

    /**
     * {@code branch}에서 {@code path}가 디렉토리일 경우에는 해당 디렉토리에 들어있는 파일과 디렉토리 목록을 JSON으로 반환하고,
     * 파일일 경우에는 해당 파일 정보를 JSON으로 반환한다.
     *
     *
     * {@code branch}가 null이면 {@link Constants#HEAD}의 Commit을 가져오고, null이 아닐때는 해당 브랜치의 head Commit을 가져온다.
     * Commit의 루트 Tree를 가져오고, {@link TreeWalk}로 해당 Tree를 순회하며 {@code path}에 해당하는 디렉토리나 파일을 찾는다.
     * {@code path}가 디렉토리이면 해당 디렉토리로 들어가서 그 안에 있는 파일과 디렉토리 목록을 JSON으로 만들어서 변환한다.
     * {@code path}가 파일이면 해당 파일 정보를 JSON으로 만들어 반환한다.
     *
     * @param branch
     * @param path
     * @return {@code path}가 디렉토리이면 그 안에 들어있는 파일과 디렉토리 목록을 담고있는 JSON, 파일이면 해당 파일 정보를 담고 있는 JSON
     * @throws IOException
     * @throws GitAPIException
     */
    @Override
    public ObjectNode findFileInfo(String branch, String path) throws IOException, GitAPIException {
        ObjectId headCommit;

        if (branch == null){
            headCommit = repository.resolve(Constants.HEAD);
        } else {
            headCommit = repository.resolve(branch);
        }
        // 만약 특정 커밋을 얻오오고싶다면 바꾸어 주면 된다.
        if (headCommit == null) {
            Logger.debug("GitRepository : init Project - No Head commit");
            return null;
        }

        RevWalk revWalk = new RevWalk(repository);
        RevTree revTree = revWalk.parseTree(headCommit);
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(revTree);

        try {
            PathFilter pathFilter = PathFilter.create(path);
            treeWalk.setFilter(pathFilter);
            while (treeWalk.next()) {
                if (pathFilter.isDone(treeWalk)) {
                    break;
                } else if (treeWalk.isSubtree()) {
                    treeWalk.enterSubtree();
                }
            }
        } catch (IllegalArgumentException e) {
            return treeAsJson(treeWalk, headCommit);
        }

        if (treeWalk.isSubtree()) {
            treeWalk.enterSubtree();
            return treeAsJson(treeWalk, headCommit);
        } else {
            return fileAsJson(treeWalk, headCommit);
        }
    }

    /**
     * {@code treeWalk}가 현재 위치한 파일 메타데이터를 JSON 데이터로 변환하여 반환한다.
     * 그 파일에 대한 {@code untilCommitId} 혹은 그 이전 커밋 중에서 가장 최근 커밋 정보를 사용하여 Commit 메시지와 author 정보등을 같이 반홚다.
     *
     * @param treeWalk
     * @param untilCommitId
     * @return
     * @throws IOException
     * @see <a href="https://www.kernel.org/pub/software/scm/git/docs/git-log.html">git log until</a>
     */
    private ObjectNode fileAsJson(TreeWalk treeWalk, AnyObjectId untilCommitId) throws IOException, GitAPIException {
        Git git = new Git(repository);

        RevCommit commit = git.log()
            .add(untilCommitId)
            .addPath(treeWalk.getPathString())
            .call()
            .iterator()
            .next();

        ObjectNode result = Json.newObject();
        long commitTime = commit.getCommitTime() * 1000L;
        result.put("type", "file");
        result.put("msg", commit.getShortMessage());
        result.put("author", commit.getAuthorIdent().getName());
        result.put("createdDate", commitTime);
        result.put("commitMessage", commit.getShortMessage());
        result.put("commiter", commit.getAuthorIdent().getName());
        result.put("commitDate", commitTime);
        String str = new String(repository.open(treeWalk.getObjectId(0)).getBytes());
        result.put("data", str);
        return result;
    }

    /**
     * {@code treeWalk}가 현재 위치한 디렉토리에 들어있는 파일과 디렉토리 메타데이터를 JSON 데이터로 변환하여 반환한다.
     * 각 파일과 디렉토리에 대한 {@code untilCommitId} 혹은 그 이전 커밋 중에서 가장 최근 커밋 정보를 사용하여 Commit 메시지와 author 정보등을 같이 반홚다.
     *
     * @param treeWalk
     * @param untilCommitId
     * @return
     * @throws IOException
     * @throws GitAPIException
     * @see <a href="https://www.kernel.org/pub/software/scm/git/docs/git-log.html">git log until</a>
     */
    private ObjectNode treeAsJson(TreeWalk treeWalk, AnyObjectId untilCommitId) throws IOException, GitAPIException {
        Git git = new Git(repository);
        ObjectNode result = Json.newObject();
        result.put("type", "folder");

        ObjectNode listData = Json.newObject();

        while (treeWalk.next()) {
            RevCommit commit = git.log()
                .add(untilCommitId)
                .addPath(treeWalk.getPathString())
                .call()
                .iterator()
                .next();
            ObjectNode data = Json.newObject();
            data.put("type", treeWalk.isSubtree() ? "folder" : "file");
            data.put("msg", commit.getShortMessage());
            data.put("author", commit.getAuthorIdent().getName());
            data.put("createdDate", commit.getCommitTime() * 1000l);
            listData.put(treeWalk.getNameString(), data);
        }
        result.put("data", listData);
        return result;
    }

    /* (non-Javadoc)
     *
     */

    /**
     * {@link Constants#HEAD}에서 {@code path}에 해당하는 파일을 반환한다.
     *
     * @param path
     * @return {@code path}가 디렉토리일 경우에는 null, 아닐때는 해당 파일
     * @throws IOException
     */
    @Override
    public byte[] getRawFile(String path) throws IOException {
        RevTree tree = new RevWalk(repository).parseTree(repository.resolve(Constants.HEAD));
        TreeWalk treeWalk = TreeWalk.forPath(repository, path, tree);
        if (treeWalk.isSubtree()) {
            return null;
        } else {
            return repository.open(treeWalk.getObjectId(0)).getBytes();
        }
    }

    /**
     * Git 저장소 디렉토리를 삭제한다.
     */
    @Override
    public void delete() {
        FileUtil.rm_rf(repository.getDirectory());
    }

    /**
     * {@code rev}에 해당하는 리비전의 변경 내역을 반환한다.
     *
     * {@code rev}에 해당하는 커밋의 root tree와 그 이전 커밋의 root tree를 비교한다.
     * 이전 커밋이 없을 때는 비어있는 트리와 비교한다.
     *
     * @param rev
     * @return
     * @throws GitAPIException
     * @throws IOException
     */
    @Override
    public String getPatch(String rev) throws GitAPIException, IOException {
        // Get the trees, from current commit and its parent, as treeWalk.
        ObjectId commitId = repository.resolve(rev);
        TreeWalk treeWalk = new TreeWalk(repository);
        RevWalk revWalk = new RevWalk(repository);
        RevCommit commit = revWalk.parseCommit(commitId);
        if (commit.getParentCount() > 0) {
            RevTree tree = revWalk.parseCommit(commit.getParent(0).getId()).getTree();
            treeWalk.addTree(tree);
        } else {
            treeWalk.addTree(new EmptyTreeIterator());
        }
        treeWalk.addTree(commit.getTree());

        // Render the difference from treeWalk which has two trees.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DiffFormatter diffFormatter = new DiffFormatter(out);
        diffFormatter.setRepository(repository);
        diffFormatter.format(DiffEntry.scan(treeWalk));

        return out.toString("UTF-8");
    }

    /**
     * {@code untilRevName}에 해당하는 리비전까지의 커밋 목록을 반환한다.
     * {@code untilRevName}이 null이면 HEAD 까지의 커밋 목록을 반환한다.
     *
     * @param pageNumber 0부터 시작
     * @param pageSize
     * @param untilRevName
     * @return
     * @throws IOException
     * @throws GitAPIException
     */
    @Override
    public List<Commit> getHistory(int pageNumber, int pageSize, String untilRevName)
            throws IOException, GitAPIException {
        // Get the list of commits from HEAD to the given pageNumber.
        LogCommand logCommand = new Git(repository).log();
        if (untilRevName != null) {
            logCommand.add(repository.resolve(untilRevName));
        }
        Iterable<RevCommit> iter = logCommand.setMaxCount(pageNumber * pageSize + pageSize).call();
        List<RevCommit> list = new LinkedList<>();
        for (RevCommit commit : iter) {
            if (list.size() >= pageSize) {
                list.remove(0);
            }
            list.add(commit);
        }

        List<Commit> result = new ArrayList<>();
        for (RevCommit commit : list) {
            result.add(new GitCommit(commit));
        }

        return result;
    }

    /**
     * Git 저장소의 모든 브랜치 이름을 반환한다.
     * @return
     */
    @Override
    public List<String> getBranches() {
        return new ArrayList<>(repository.getAllRefs().keySet());
    }

    @Override
    public Resource asResource() {
        return new Resource() {
            @Override
            public Long getId() {
                return null;
            }

            @Override
            public Project getProject() {
                return ProjectApp.getProject(ownerName, projectName);
            }

            @Override
            public ResourceType getType() {
                return ResourceType.CODE;
            }

        };
    }

    /**
     * Git 디렉토리 경로를 반환한다.
     *
     * when: Git 저장소를 참조하는 {@link Repository} 객체를 생성할 때 주로 사용한다.
     *
     * @param project
     * @return
     * @see #getGitDirectory(String, String)
     */
    public static String getGitDirectory(Project project) {
        return getGitDirectory(project.owner, project.name);
    }

    /**
     * Git 디렉토리 URL을 반환한다.
     *
     * when: 로컬 저장소에서 clone, fetch, push 커맨드를 사용할 때 저장소를 참조할 URL이 필요할 때 사용한다.
     *
     * @param project
     * @return
     * @throws IOException
     */
    public static String getGitDirectoryURL(Project project) throws IOException {
        String currentDirectory = new java.io.File( "." ).getCanonicalPath();
        return currentDirectory + "/" + getGitDirectory(project);
    }

    /**
     * Git 디렉토리 경로를 반환한다.
     *
     * when: Git 저장소를 참조하는 {@link Repository} 객체를 생성할 때 주로 사용한다.
     *
     * @param ownerName
     * @param projectName
     * @return
     */
    public static String getGitDirectory(String ownerName, String projectName) {
        return getRepoPrefix() + ownerName + "/" + projectName + ".git";
    }

    /**
     * {@code project}의 Git 저장소를 반환한다.
     * <p/>
     * when: {@link RepositoryService#gitAdvertise(models.Project, String, play.mvc.Http.Response)}와
     * {@link RepositoryService#gitRpc(models.Project, String, play.mvc.Http.Request, play.mvc.Http.Response)}에서 사용한다.
     * <p/>
     * {@link GitRepository#buildGitRepository(models.Project)}를 사용하여 Git 저장소를 참조할 객체를 생성한다.
     *
     * @param project
     * @return
     * @throws IOException
     */
    public static Repository createGitRepository(Project project) throws IOException {
        return GitRepository.buildGitRepository(project);
    }


    /**
     * {@code originalProject}의 Git 저장소를 clone 하는 {@code forkingProject}의 Git 저장소를 생성한다.
     *
     * 모든 브랜치를 복사하며 bare 모드로 생성한다.
     *
     * @param originalProject
     * @param forkingProject
     * @throws GitAPIException
     * @throws IOException
     * * @see <a href="https://www.kernel.org/pub/software/scm/git/docs/gitglossary.html#def_bare_repository">bare repository</a>
     */
    public static void cloneRepository(Project originalProject, Project forkingProject) throws GitAPIException, IOException {
        String directory = getGitDirectory(forkingProject);
        Git.cloneRepository()
                .setURI(getGitDirectoryURL(originalProject))
                .setDirectory(new File(directory))
                .setCloneAllBranches(true)
                .setBare(true)
                .call();
    }


    /**
     * {@code pullRequest}를 merge 한다.
     *
     * 자동으로 merge해도 안전한지 확인한 다음 admin 계정으로 코드를 받을 저장소로 자동으로 병합한 코드를 push 한다.
     *
     * @param pullRequest
     */
    public static void merge(final PullRequest pullRequest) {
        boolean isSafeToMerge = isSafeToMerge(pullRequest);
        if(!isSafeToMerge) {
            return;
        }

        new CloneAndFetchTemplate(pullRequest).runWith(new CloneAndFetchOperation() {
            @Override
            public void invoke(CloneAndFetchTemplate cloneAndFetch) throws IOException, GitAPIException {
                Git git = new Git(cloneAndFetch.getCloneRepository());
                String srcToBranchName = pullRequest.toBranch;
                String destToBranchName = srcToBranchName + "-to";

                // 코드 받을 프로젝트의 코드 받을 브랜치로 clone한 프로젝트의 merge 한 브랜치의 코드를 push 한다.
                git.push()
                    .setRemote(getGitDirectoryURL(pullRequest.toProject))
                    .setRefSpecs(new RefSpec(destToBranchName + ":" + srcToBranchName))
                    .call();

                // 풀리퀘스트 완료
                pullRequest.state = State.CLOSED;
            }
        });
    }

    /**
     * {@code pullRequest}를 자동으로 merge해도 괜찮은지 확인한다.
     *
     * bare repository는 checkout 등의 명령어를 사용할 수 없기 때문에
     * 코드를 받을 프로젝트를 clone하는 non-bare repository를 생성하고,
     * 그 repository로 코드를 보내는 저장소의 브랜치와 코드를 받는 저장소의 브랜치를 체크아웃 받은다음
     * '코드를 받을 저장소의 브랜치'를 fetch 받은 브랜치에서
     * '코드를 보내는 저장소의 브랜치'를 fetch 받은 브랜치를 merge한다.
     *
     * 이때 merge한 결과 conflict가 발생하면 false를 리턴하고 그렇지 않은 경우에는 true를 반환한다.
     *
     * @param pullRequest
     * @return
     */
    public static boolean isSafeToMerge(PullRequest pullRequest) {
        final MergeResult[] mergeResult = {null};

        new CloneAndFetchTemplate(pullRequest).runWith(new CloneAndFetchOperation() {
            @Override
            public void invoke(CloneAndFetchTemplate cloneAndFetch) throws IOException, GitAPIException {
                Repository clonedRepository = cloneAndFetch.getCloneRepository();
                Git git = new Git(clonedRepository);

                // 코드를 받을 브랜치로 이동(checkout)한다.
                git.checkout()
                    .setName(cloneAndFetch.getDestToBranchName())
                    .setCreateBranch(false)
                    .call();

                // 코드를 보낸 브랜치의 코드를 merge 한다.
                ObjectId fromBranch = clonedRepository.resolve(cloneAndFetch.getDestFromBranchName());
                mergeResult[0] = git.merge()
                    .setFastForward(MergeCommand.FastForwardMode.NO_FF) // create a merge commit
                    .include(fromBranch)
                    .call();
            }
        });

        // merge 결과를 반환한다.
        return mergeResult[0].getMergeStatus().isSuccessful();
    }

    /**
     * 코드를 merge할 때 사용할 working tree 경로를 반환한다.
     *
     * @param owner
     * @param projectName
     * @return
     */
    public static String getDirectoryForMerging(String owner, String projectName) {
        return getRepoForMergingPrefix() + owner + "/" + projectName + ".git";
    }

    public static List<GitCommit> getPullingCommits(PullRequest pullRequest) {
        final List<GitCommit> commits = new ArrayList<>();

        new CloneAndFetchTemplate(pullRequest).runWith(new CloneAndFetchOperation() {
            @Override
            public void invoke(CloneAndFetchTemplate cloneAndFetch) throws IOException {
                Repository clonedRepository = cloneAndFetch.getCloneRepository();
                String destFromBranchName = cloneAndFetch.getDestFromBranchName();
                String destToBranchName = cloneAndFetch.getDestToBranchName();

                RevWalk walk = null;
                try {
                    walk = new RevWalk(clonedRepository);
                    ObjectId from = clonedRepository.resolve(destFromBranchName);
                    ObjectId to = clonedRepository.resolve(destToBranchName);

                    walk.markStart(walk.parseCommit(from));
                    walk.markUninteresting(walk.parseCommit(to));

                    Iterator<RevCommit> iterator = walk.iterator();
                    while (iterator.hasNext()) {
                        RevCommit commit = iterator.next();
                        commits.add(new GitCommit(commit));
                    }
                } finally {
                    walk.release();
                }
            }
        });

        return commits;
    }
}