package nsoushi.elasticsearch

import org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder
import org.slf4j.LoggerFactory

/**
 *
 * @author nsoushi
 */
class ScrollSearch constructor(val client: SearchClient) : SearchClient by client {
    val logger = LoggerFactory.getLogger("ELASTICSEARCH")

    fun addSource(indices: String, type: String) {
        ReleasableLoan.using( client.getClient(), {c ->
            val response = c.prepareIndex(indices, type)
                    .setSource(jsonBuilder()
                            .startObject()
                            .field("userId", 1L)
                            .field("categoryId", 100L)
                            .field("content", "Elasticsearch scroll")
                            .endObject()
                    ).get()

            logger.info("complete add source id={%s}".format(response.id))
        })
    }
}
