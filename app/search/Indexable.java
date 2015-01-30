package search;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * @author Keeun Baik
 */
public interface Indexable {

    /**
     * A unique ID for a document.
     *
     * @return id
     * @see <a href="http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-id-field.html">mapping-id-field</a>
     */
    String indexId();

    /**
     * A document that will be serialized into JSON and stored in Elasticsearch under a unique ID.
     *
     * @return
     * @see <a href="http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/document.html">document</a>
     */
    @Nonnull
    Map<String, Object> source();

}
