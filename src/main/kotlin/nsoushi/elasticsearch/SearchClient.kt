package nsoushi.elasticsearch

import org.elasticsearch.ElasticsearchException
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.slf4j.LoggerFactory
import java.net.InetAddress

/**
 *
 * @author nsoushi
 */
interface SearchClient {
    fun getClient(clusterName: String = "elasticsearch"
                  , nodeList: List<String> = listOf("localhost:9300")): TransportClient

    fun search(): SearchResponse

    fun searchScroll(clusterName: String = "elasticsearch"
                  , nodeList: List<String> = listOf("localhost:9300")): (String, String) -> SearchResponse

    fun print(response: SearchResponse): String?
}

class SearchClientImpl constructor(
        val indices: String,
        val type: String,
        val size: Int,
        val keepAlive: String) : SearchClient {

    val logger = LoggerFactory.getLogger("ELASTICSEARCH")

    override fun getClient(clusterName: String, nodeList: List<String>): TransportClient {
        val client = TransportClient
                .builder().settings(Settings.settingsBuilder().put("cluster.name", clusterName).build()).build()
        nodeList.forEach {
            client.addTransportAddress(InetSocketTransportAddress(InetAddress.getByName(it.split(":").first()), it.split(":").last().toInt()))
        }
        return client
    }

    override fun search(): SearchResponse {
        return ReleasableLoan.using( getClient(), { c ->
            val response = c.prepareSearch(indices).setTypes(type)
                    .setScroll(keepAlive)
                    .setSize(size)
                    .execute().actionGet()

            if (response.hits.totalHits == 0L)
                throw ElasticsearchException("data not found")

            response
        })
    }

    override fun print(response: SearchResponse): String? {
        if (response.hits.hits().isEmpty()) return null

        response.hits.hits().forEach {
            logger.info("totalCount={%s}, id={%s}".format(response.hits.totalHits, it.id))
        }

        return response.scrollId
    }

    override fun searchScroll(clusterName: String, nodeList: List<String>): (String, String) -> SearchResponse {
        return { scrollId, keepAlive ->
            val response = ReleasableLoan.using( getClient(clusterName, nodeList), { c ->
                c.prepareSearchScroll(scrollId).setScroll(keepAlive).execute().actionGet()
            })

            if (response.hits.hits().isEmpty()) {
                ReleasableLoan.using( getClient(clusterName, nodeList), { c ->
                    c.prepareClearScroll().setScrollIds(listOf(scrollId)).execute().actionGet()
                    logger.info("clear scrollId={%s}".format(scrollId))
                })
            }
            response
        }
    }
}
