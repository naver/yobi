package playRepository.support;

import models.Project;
import models.PullRequest;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import playRepository.GitRepository;
import utils.FileUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

/**
 * @author Keesun Baik
 */
public class CloneAndFetchTemplateTest {

    @Before
    public void before() {
        GitRepository.setRepoPrefix("resources/test/repo/git/");
        GitRepository.setRepoForMergingPrefix("resources/test/repo/git-merging/");
    }

    @After
    public void after() {
        FileUtil.rm_rf(new File(GitRepository.getRepoPrefix()));
        FileUtil.rm_rf(new File(GitRepository.getRepoForMergingPrefix()));
    }

    @Test
    public void create(){
        // Given
        Project original = createProject("keesun", "test");
        PullRequest pullRequest = new PullRequest();
        pullRequest.toProject = original;

        // When
        CloneAndFetchTemplate template = new CloneAndFetchTemplate(pullRequest);

        // Then
        assertThat(template).isNotNull();
    }

    @Test
    public void buildCloneRepository() throws GitAPIException, IOException {
        // Given
        Project original = createProject("keesun", "test");
        CloneAndFetchTemplate template = createTemplate(original);
        new GitRepository("keesun", "test").create();

        // When
        Repository repository = template.buildCloneRepository();

        // Then
        assertThat(repository).isNotNull();
        assertThat(template.getDirectory()).isNotNull();
        String clonePath = "resources/test/repo/git-merging/keesun/test.git";
        assertThat(template.getDirectory()).isEqualTo(clonePath);
        assertThat(new File(clonePath).exists()).isTrue();
        assertThat(new File(clonePath + "/.git").exists()).isTrue();
    }

    @Test
    public void deleteBranch() throws IOException, GitAPIException {
        // Given
        Project original = createProject("keesun", "test");
        CloneAndFetchTemplate template = createTemplate(original);
        new GitRepository("keesun", "test").create();
        Repository repository = template.buildCloneRepository();

        Git git = new Git(repository);
        String branchName = "refs/heads/maste";

        // When
        template.deleteBranch(repository, branchName);

        // Then
        List<Ref> refs = git.branchList().call();
        for(Ref ref : refs) {
            if(ref.getName().equals(branchName)) {
                fail("deleting branch was failed");
            }
        }
    }

    @Test
    public void fetch() throws IOException, GitAPIException {
        // Given
        Project original = createProject("keesun", "test");
        CloneAndFetchTemplate template = createTemplate(original);
        new GitRepository("keesun", "test").create();
        Repository repository = template.buildCloneRepository();

        String workingTreePath = template.getDirectory();
        Git git = new Git(repository);
        String readmeFileName = "readme.md";
        String testFilePath = workingTreePath + "/" + readmeFileName;
        BufferedWriter out = new BufferedWriter(new FileWriter(testFilePath));
        out.write("hello 1");
        out.flush();
        git.add().addFilepattern(readmeFileName).call();
        RevCommit commit = git.commit().setMessage("commit 1").call();
        git.push().call();

        // When
        String dstBranchName = "refs/heads/master-fetch";
        template.fetch(repository, original, "refs/heads/master", dstBranchName);

        // Then
        ObjectId branch = repository.resolve(dstBranchName);
        assertThat(branch.getName()).isEqualTo(commit.getId().getName());
    }

    private CloneAndFetchTemplate createTemplate(Project project) {
        PullRequest pullRequest = new PullRequest();
        pullRequest.toProject = project;
        return new CloneAndFetchTemplate(pullRequest);
    }

    private Project createProject(String owner, String name) {
        Project project = new Project();
        project.owner = owner;
        project.name = name;
        return project;
    }
}
