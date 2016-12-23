package nsoushi.elasticsearch

import org.elasticsearch.action.search.SearchResponse

/**
 *
 * @author nsoushi
 */
open class Application {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            val indices = "elasticsearch.test"
            val type = "scroll"
            val keepAlive = "10m"

            val client = SearchClientImpl(
                    indices = indices,
                    type = type,
                    size = 1,
                    keepAlive = keepAlive)

            val scrollSearch = ScrollSearch(client)

            fun scroll(response: SearchResponse) {
                    // responseを出力してscrollIdを取得する、scrollIdがnullであれば終了
                    val scrollId = scrollSearch.print(response) ?: return
                    // スナップショットが有効であるか確認するためにレコードを1件追加する／追加が成功するとログ出力
                    scrollSearch.addSource(indices, type)
                    // 取得したscrollIdで次の検索を行う
                    val nextResponse = scrollSearch.searchScroll()(scrollId, keepAlive)

                    scroll(nextResponse)
            }

            scroll(scrollSearch.search())
        }
    }
}
