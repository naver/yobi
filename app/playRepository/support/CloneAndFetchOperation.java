package playRepository.support;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;

/**
 * {@link CloneAndFetchTemplate#runWith(CloneAndFetchOperation)}에
 * 넘겨줄 매개변수 타입이며 이 인터페이스 구현체에서는 {@link models.PullRequest}에 들어있는 정보로
 * clone과 fetch가 끝난 이후에 할 작업을 정의한다.
 *
 * @see {@link CloneAndFetchTemplate}
 */
public interface CloneAndFetchOperation {

    /**
     * {@link CloneAndFetchTemplate}타입의 객체를 참조하여 clone과 fetch 이후에 할 작업을 정의한다.
     *
     * @param cloneAndFetchTemplate
     * @throws java.io.IOException
     * @throws org.eclipse.jgit.api.errors.GitAPIException
     * @see {@link CloneAndFetchTemplate}
     */
    public void invoke(CloneAndFetchTemplate cloneAndFetchTemplate) throws IOException, GitAPIException;
}
