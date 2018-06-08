package soko.ekibun.bangumi.api.bangumi.bean

import android.support.annotation.IntDef
import android.support.annotation.StringDef

class SubjectProgress {

    /**
     * subject_id : 183891
     * eps : [{"id":730511,"status":{"id":2,"css_name":"Watched","url_name":"watched","cn_name":"看过"}},{"id":730512,"status":{"id":2,"css_name":"Watched","url_name":"watched","cn_name":"看过"}},{"id":730513,"status":{"id":2,"css_name":"Watched","url_name":"watched","cn_name":"看过"}},{"id":730514,"status":{"id":2,"css_name":"Watched","url_name":"watched","cn_name":"看过"}},{"id":730515,"status":{"id":2,"css_name":"Watched","url_name":"watched","cn_name":"看过"}},{"id":730516,"status":{"id":2,"css_name":"Watched","url_name":"watched","cn_name":"看过"}},{"id":730517,"status":{"id":2,"css_name":"Watched","url_name":"watched","cn_name":"看过"}},{"id":730518,"status":{"id":2,"css_name":"Watched","url_name":"watched","cn_name":"看过"}},{"id":730519,"status":{"id":2,"css_name":"Watched","url_name":"watched","cn_name":"看过"}},{"id":730520,"status":{"id":2,"css_name":"Watched","url_name":"watched","cn_name":"看过"}},{"id":782756,"status":{"id":2,"css_name":"Watched","url_name":"watched","cn_name":"看过"}},{"id":782757,"status":{"id":2,"css_name":"Watched","url_name":"watched","cn_name":"看过"}},{"id":782758,"status":{"id":2,"css_name":"Watched","url_name":"watched","cn_name":"看过"}},{"id":782759,"status":{"id":2,"css_name":"Watched","url_name":"watched","cn_name":"看过"}},{"id":782760,"status":{"id":2,"css_name":"Watched","url_name":"watched","cn_name":"看过"}},{"id":782761,"status":{"id":2,"css_name":"Watched","url_name":"watched","cn_name":"看过"}},{"id":782762,"status":{"id":2,"css_name":"Watched","url_name":"watched","cn_name":"看过"}},{"id":782763,"status":{"id":2,"css_name":"Watched","url_name":"watched","cn_name":"看过"}}]
     */

    var subject_id: Int = 0
    var eps: List<EpisodeProgress>? = null

    class EpisodeProgress {
        /**
         * id : 730511
         * status : {"id":2,"css_name":"Watched","url_name":"watched","cn_name":"看过"}
         */

        var id: Int = 0
        var status: EpisodeStatus? = null

        class EpisodeStatus {
            /**
             * id : 2
             * css_name : Watched
             * url_name : watched
             * cn_name : 看过
             */

            var id: Int = 0
            var css_name: String? = null
            var url_name: String? = null
            var cn_name: String? = null

            companion object {
                const val WATCH_ID = 2
                const val QUEUE_ID = 1
                const val DROP_ID = 3
                @IntDef(WATCH_ID, QUEUE_ID, DROP_ID)
                annotation class EpStatusId

                const val WATCH = "watched"
                const val QUEUE = "queue"
                const val DROP = "drop"
                const val REMOVE = "remove"
                @StringDef(WATCH, QUEUE, DROP, REMOVE)
                annotation class EpStatusType
                val types = arrayOf(WATCH, QUEUE, DROP, REMOVE)
            }
        }
    }
}
