package soko.ekibun.bangumi.api.bgmlist.bean

data class BgmItem(
        var titleCN: String? = null,
        var titleJP: String? = null,
        var titleEN: String? = null,
        var officalSite: String? = null,
        var weekDayJP: Int = 0,
        var weekDayCN: Int = 0,
        var timeJP: String? = null,
        var timeCN: String? = null,
        var isNewBgm: Boolean = false,
        var bgmId: Int = 0,
        var showDate: String? = null,
        var onAirSite: List<String>? = null
) {

    /**
     * titleCN : 海螺小姐
     * titleJP : サザエさん
     * titleEN :
     * officalSite : http://www.fujitv.co.jp/sazaesan/
     * weekDayJP : 0
     * weekDayCN : 0
     * timeJP : 1730
     * timeCN :
     * onAirSite : []
     * newBgm : false
     * bgmId : 32585
     * showDate : 1969-10-05
     */


}
