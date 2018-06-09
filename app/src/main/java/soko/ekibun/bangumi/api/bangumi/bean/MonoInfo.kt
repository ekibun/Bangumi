package soko.ekibun.bangumi.api.bangumi.bean

data class MonoInfo(
        var name_cn: String? = null,
        var alias: Any? = null,
        var gender: String? = null,
        var birth: String? = null,
        var bloodtype: String? = null,
        var height: Any? = null,
        var weight: Any? = null
) {
    /**
     * name_cn : 江户川柯南
     * alias : {"0":"バーロー","jp":"江戸川コナン","kana":"えどがわこなん","romaji":"Edogawa Conan","nick":"柯南"}
     * gender : 男
     * birth : 5月4日
     * bloodtype : A型
     * height : 121CM
     * weight : 18kg
     */
    /*
    class AliasBean (
            var jp: String? = null,
            var kana: String? = null,
            var romaji: String? = null,
            var nick: String? = null
    ){
        /**
         * 0 : バーロー
         * jp : 江戸川コナン
         * kana : えどがわこなん
         * romaji : Edogawa Conan
         * nick : 柯南
         */
    }
    */
}