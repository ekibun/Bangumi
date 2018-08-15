package soko.ekibun.bangumi.api.smms.bean

data class Response(
        var code: String? = null,
        var data: DataBean? = null,
        var msg: String? = null
) {

    /**
     * code : success
     * data : {"width":1157,"height":680,"filename":"image_2015-08-26_10-54-48.png","storename":"56249afa4e48b.png","size":69525,"path":"/2015/10/19/56249afa4e48b.png","hash":"nLbCw63NheaiJp1","timestamp":1445239546,"url":"https://ooo.0o0.ooo/2015/10/19/56249afa4e48b.png","delete":"https://sm.ms/api/delete/nLbCw63NheaiJp1"}
     */

    data class DataBean(
            var width: Int = 0,
            var height: Int = 0,
            var filename: String? = null,
            var storename: String? = null,
            var size: Int = 0,
            var path: String? = null,
            var hash: String? = null,
            var timestamp: Int = 0,
            var url: String? = null,
            var delete: String? = null
    ) {
        /**
         * width : 1157
         * height : 680
         * filename : image_2015-08-26_10-54-48.png
         * storename : 56249afa4e48b.png
         * size : 69525
         * path : /2015/10/19/56249afa4e48b.png
         * hash : nLbCw63NheaiJp1
         * timestamp : 1445239546
         * url : https://ooo.0o0.ooo/2015/10/19/56249afa4e48b.png
         * delete : https://sm.ms/api/delete/nLbCw63NheaiJp1
         */
    }
}