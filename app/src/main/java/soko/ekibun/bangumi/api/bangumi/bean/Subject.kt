package soko.ekibun.bangumi.api.bangumi.bean

import com.google.gson.annotations.SerializedName


data class Subject(
        var id: Int = 0,
        var url: String? = null,
        var type: Int = 0,
        var name: String? = null,
        var name_cn: String? = null,
        var summary: String? = null,
        var eps_count: Int = 0,
        var air_date: String? = null,
        var air_weekday: Int = 0,
        var rating: RatingBean? = null,
        var rank: Int = 0,
        var images: Images? = null,
        var collection: CollectionBean? = null,
        var eps: Any? = null,
        var crt: List<Character>? = null,
        var staff: List<Person>? = null
){
    /**
     * id : 899
     * url : http://bgm.tv/subject/899
     * type : 2
     * name : 名探偵コナン
     * name_cn : 名侦探柯南
     * summary : 主角工藤新一原本是一位颇具名声的高中生侦探，在目击黑暗组织的地下交易后，正准备追踪时却被突袭击昏，并被灌下代号为“APTX4869”（アポトキシン4869）的不明药物。后来虽然幸免于死，但身体就此缩小为小学时期的模样。之后他化名为江户川柯南，在邻居阿笠博士的建议下，寄住在女友毛利兰的父亲—侦探毛利小五郎家中，继续秘密从事追查黑暗组织的工作，并私下探寻获得解药的管道，希望能够恢复原来新一的样貌。与此同时，柯南凭着自己的推理天份，配合阿笠博士为他发明的道具，帮助毛利小五郎成为出名的大侦探。故事内容当中穿插许多爱情、友情、犯罪、背叛、复仇等情节。
     * air_date : 1996-01-08
     * air_weekday : 6
     * rating : {"total":3069,"count":{"10":331,"9":335,"8":907,"7":977,"6":391,"5":97,"4":19,"3":3,"2":2,"1":7},"score":7.6}
     * rank : 570
     * images : {"large":"http://lain.bgm.tv/pic/cover/l/01/88/899_Q3F3X.jpg","common":"http://lain.bgm.tv/pic/cover/c/01/88/899_Q3F3X.jpg","medium":"http://lain.bgm.tv/pic/cover/m/01/88/899_Q3F3X.jpg","small":"http://lain.bgm.tv/pic/cover/s/01/88/899_Q3F3X.jpg","grid":"http://lain.bgm.tv/pic/cover/g/01/88/899_Q3F3X.jpg"}
     * collection : {"wish":171,"collect":2011,"doing":1880,"on_hold":941,"dropped":273}
     * crt : [{"id":3453,"url":"http://bgm.tv/character/3453","name":"江戸川コナン","name_cn":"江户川柯南","role_name":"主角","images":{"large":"http://lain.bgm.tv/pic/crt/l/e7/f9/3453_crt_836v3.jpg?r=1444795097","medium":"http://lain.bgm.tv/pic/crt/m/e7/f9/3453_crt_836v3.jpg?r=1444795097","small":"http://lain.bgm.tv/pic/crt/s/e7/f9/3453_crt_836v3.jpg?r=1444795097","grid":"http://lain.bgm.tv/pic/crt/g/e7/f9/3453_crt_836v3.jpg?r=1444795097"},"comment":29,"collects":96,"info":{"name_cn":"江户川柯南","alias":{"0":"バーロー","jp":"江戸川コナン","kana":"えどがわこなん","romaji":"Edogawa Conan","nick":"柯南"},"gender":"男","birth":"5月4日","bloodtype":"A型","height":"121CM","weight":"18kg"},"actors":[{"id":3933,"url":"http://bgm.tv/person/3933","name":"高山みなみ","images":{"large":"http://lain.bgm.tv/pic/crt/l/29/8f/3933_seiyu_anidb.jpg","medium":"http://lain.bgm.tv/pic/crt/m/29/8f/3933_seiyu_anidb.jpg","small":"http://lain.bgm.tv/pic/crt/s/29/8f/3933_seiyu_anidb.jpg","grid":"http://lain.bgm.tv/pic/crt/g/29/8f/3933_seiyu_anidb.jpg"}}]},{"id":3455,"url":"http://bgm.tv/character/3455","name":"工藤新一","name_cn":"工藤新一","role_name":"主角","images":{"large":"http://lain.bgm.tv/pic/crt/l/2f/ef/3455_crt_2V5Q1.jpg?r=1444804644","medium":"http://lain.bgm.tv/pic/crt/m/2f/ef/3455_crt_2V5Q1.jpg?r=1444804644","small":"http://lain.bgm.tv/pic/crt/s/2f/ef/3455_crt_2V5Q1.jpg?r=1444804644","grid":"http://lain.bgm.tv/pic/crt/g/2f/ef/3455_crt_2V5Q1.jpg?r=1444804644"},"comment":16,"collects":88,"info":{"name_cn":"工藤新一","alias":{"en":"Jimmy Kudo","kana":"くどう しんいち","romaji":"Kudou Shinichi","nick":"滚筒洗衣机"},"gender":"男","birth":"5月4日","bloodtype":"A型","height":"174cm","source":"anidb.net"},"actors":[{"id":3933,"url":"http://bgm.tv/person/3933","name":"高山みなみ","images":{"large":"http://lain.bgm.tv/pic/crt/l/29/8f/3933_seiyu_anidb.jpg","medium":"http://lain.bgm.tv/pic/crt/m/29/8f/3933_seiyu_anidb.jpg","small":"http://lain.bgm.tv/pic/crt/s/29/8f/3933_seiyu_anidb.jpg","grid":"http://lain.bgm.tv/pic/crt/g/29/8f/3933_seiyu_anidb.jpg"}},{"id":3900,"url":"http://bgm.tv/person/3900","name":"山口勝平","images":{"large":"http://lain.bgm.tv/pic/crt/l/46/c8/3900_prsn_RB1me.jpg?r=1494885663","medium":"http://lain.bgm.tv/pic/crt/m/46/c8/3900_prsn_RB1me.jpg?r=1494885663","small":"http://lain.bgm.tv/pic/crt/s/46/c8/3900_prsn_RB1me.jpg?r=1494885663","grid":"http://lain.bgm.tv/pic/crt/g/46/c8/3900_prsn_RB1me.jpg?r=1494885663"}},{"id":7713,"url":"http://bgm.tv/person/7713","name":"劉傑","images":{"large":"http://lain.bgm.tv/pic/crt/l/c8/e7/7713_prsn_Bb8xl.jpg","medium":"http://lain.bgm.tv/pic/crt/m/c8/e7/7713_prsn_Bb8xl.jpg","small":"http://lain.bgm.tv/pic/crt/s/c8/e7/7713_prsn_Bb8xl.jpg","grid":"http://lain.bgm.tv/pic/crt/g/c8/e7/7713_prsn_Bb8xl.jpg"}}]},{"id":3499,"url":"http://bgm.tv/character/3499","name":"毛利蘭","name_cn":"毛利兰","role_name":"主角","images":{"large":"http://lain.bgm.tv/pic/crt/l/0b/51/3499_crt_4kknM.jpg?r=1444795840","medium":"http://lain.bgm.tv/pic/crt/m/0b/51/3499_crt_4kknM.jpg?r=1444795840","small":"http://lain.bgm.tv/pic/crt/s/0b/51/3499_crt_4kknM.jpg?r=1444795840","grid":"http://lain.bgm.tv/pic/crt/g/0b/51/3499_crt_4kknM.jpg?r=1444795840"},"comment":17,"collects":70,"info":{"name_cn":"毛利兰","alias":{"en":"Rachel Moore","jp":"毛利蘭","kana":"もうり らん","romaji":"Mouri Ran"},"gender":"女","height":"160","source":"anidb.net"},"actors":[{"id":4559,"url":"http://bgm.tv/person/4559","name":"山崎和佳奈","images":{"large":"http://lain.bgm.tv/pic/crt/l/a5/b9/4559_seiyu_anidb.jpg","medium":"http://lain.bgm.tv/pic/crt/m/a5/b9/4559_seiyu_anidb.jpg","small":"http://lain.bgm.tv/pic/crt/s/a5/b9/4559_seiyu_anidb.jpg","grid":"http://lain.bgm.tv/pic/crt/g/a5/b9/4559_seiyu_anidb.jpg"}}]},{"id":7041,"url":"http://bgm.tv/character/7041","name":"毛利小五郎","name_cn":"毛利小五郎","role_name":"主角","images":{"large":"http://lain.bgm.tv/pic/crt/l/e8/09/7041_crt_brb2e.jpg?r=1444806201","medium":"http://lain.bgm.tv/pic/crt/m/e8/09/7041_crt_brb2e.jpg?r=1444806201","small":"http://lain.bgm.tv/pic/crt/s/e8/09/7041_crt_brb2e.jpg?r=1444806201","grid":"http://lain.bgm.tv/pic/crt/g/e8/09/7041_crt_brb2e.jpg?r=1444806201"},"comment":16,"collects":26,"info":{"name_cn":"毛利小五郎","alias":{"en":"Richard Moore","jp":"毛利小五郎","romaji":"Mouri Kogoro"},"gender":"男","source":"anidb.net"},"actors":[{"id":4202,"url":"http://bgm.tv/person/4202","name":"神谷明","images":{"large":"http://lain.bgm.tv/pic/crt/l/7d/26/4202_seiyu_anidb.jpg","medium":"http://lain.bgm.tv/pic/crt/m/7d/26/4202_seiyu_anidb.jpg","small":"http://lain.bgm.tv/pic/crt/s/7d/26/4202_seiyu_anidb.jpg","grid":"http://lain.bgm.tv/pic/crt/g/7d/26/4202_seiyu_anidb.jpg"}},{"id":4130,"url":"http://bgm.tv/person/4130","name":"小山力也","images":{"large":"http://lain.bgm.tv/pic/crt/l/c9/29/4130_seiyu_anidb.jpg","medium":"http://lain.bgm.tv/pic/crt/m/c9/29/4130_seiyu_anidb.jpg","small":"http://lain.bgm.tv/pic/crt/s/c9/29/4130_seiyu_anidb.jpg","grid":"http://lain.bgm.tv/pic/crt/g/c9/29/4130_seiyu_anidb.jpg"}}]},{"id":7043,"url":"http://bgm.tv/character/7043","name":"灰原哀","name_cn":"灰原哀","role_name":"主角","images":{"large":"http://lain.bgm.tv/pic/crt/l/ee/85/7043_crt_RHkz2.jpg?r=1444824403","medium":"http://lain.bgm.tv/pic/crt/m/ee/85/7043_crt_RHkz2.jpg?r=1444824403","small":"http://lain.bgm.tv/pic/crt/s/ee/85/7043_crt_RHkz2.jpg?r=1444824403","grid":"http://lain.bgm.tv/pic/crt/g/ee/85/7043_crt_RHkz2.jpg?r=1444824403"},"comment":19,"collects":235,"info":{"name_cn":"灰原哀","alias":{"jp":"灰原哀","kana":"はいばら あい","romaji":"Haibara Ai"},"gender":"女","source":"anidb.net"},"actors":[{"id":3919,"url":"http://bgm.tv/person/3919","name":"林原めぐみ","images":{"large":"http://lain.bgm.tv/pic/crt/l/5f/c7/3919_seiyu_anidb.jpg","medium":"http://lain.bgm.tv/pic/crt/m/5f/c7/3919_seiyu_anidb.jpg","small":"http://lain.bgm.tv/pic/crt/s/5f/c7/3919_seiyu_anidb.jpg","grid":"http://lain.bgm.tv/pic/crt/g/5f/c7/3919_seiyu_anidb.jpg"}}]},{"id":36145,"url":"http://bgm.tv/character/36145","name":"阿笠博士","name_cn":"阿笠博士","role_name":"主角","images":{"large":"http://lain.bgm.tv/pic/crt/l/0c/a9/36145_crt_X8VT1.jpg?r=1444807336","medium":"http://lain.bgm.tv/pic/crt/m/0c/a9/36145_crt_X8VT1.jpg?r=1444807336","small":"http://lain.bgm.tv/pic/crt/s/0c/a9/36145_crt_X8VT1.jpg?r=1444807336","grid":"http://lain.bgm.tv/pic/crt/g/0c/a9/36145_crt_X8VT1.jpg?r=1444807336"},"comment":1,"collects":3,"info":{"name_cn":"阿笠博士","alias":{"0":"ハーシェル・アガサ","en":"Hershel Agasa","kana":"あがさ ひろし","romaji":"Agasa Hiroshi","nick":"阿笠博士"},"gender":"男","source":"anidb.net"},"actors":[{"id":4009,"url":"http://bgm.tv/person/4009","name":"緒方賢一","images":{"large":"http://lain.bgm.tv/pic/crt/l/f4/56/4009_prsn_2ziEV.jpg","medium":"http://lain.bgm.tv/pic/crt/m/f4/56/4009_prsn_2ziEV.jpg","small":"http://lain.bgm.tv/pic/crt/s/f4/56/4009_prsn_2ziEV.jpg","grid":"http://lain.bgm.tv/pic/crt/g/f4/56/4009_prsn_2ziEV.jpg"}},{"id":4324,"url":"http://bgm.tv/person/4324","name":"田中一成","images":{"large":"http://lain.bgm.tv/pic/crt/l/24/ee/4324_prsn_2X7e7.jpg?r=1491623412","medium":"http://lain.bgm.tv/pic/crt/m/24/ee/4324_prsn_2X7e7.jpg?r=1491623412","small":"http://lain.bgm.tv/pic/crt/s/24/ee/4324_prsn_2X7e7.jpg?r=1491623412","grid":"http://lain.bgm.tv/pic/crt/g/24/ee/4324_prsn_2X7e7.jpg?r=1491623412"}}]},{"id":7056,"url":"http://bgm.tv/character/7056","name":"服部平次","name_cn":"服部平次","role_name":"主角","images":{"large":"http://lain.bgm.tv/pic/crt/l/eb/1d/7056_crt_EfGeG.jpg?r=1444807883","medium":"http://lain.bgm.tv/pic/crt/m/eb/1d/7056_crt_EfGeG.jpg?r=1444807883","small":"http://lain.bgm.tv/pic/crt/s/eb/1d/7056_crt_EfGeG.jpg?r=1444807883","grid":"http://lain.bgm.tv/pic/crt/g/eb/1d/7056_crt_EfGeG.jpg?r=1444807883"},"comment":5,"collects":44,"info":{"name_cn":"服部平次","alias":{"en":"Harley Hartwell","jp":"服部平次","kana":"はっとり へいじ","romaji":"Hattori Heiji"},"gender":"男","source":"anidb.net"},"actors":[{"id":4135,"url":"http://bgm.tv/person/4135","name":"堀川りょう","images":{"large":"http://lain.bgm.tv/pic/crt/l/46/fc/4135_seiyu_anidb.jpg?r=1459625015","medium":"http://lain.bgm.tv/pic/crt/m/46/fc/4135_seiyu_anidb.jpg?r=1459625015","small":"http://lain.bgm.tv/pic/crt/s/46/fc/4135_seiyu_anidb.jpg?r=1459625015","grid":"http://lain.bgm.tv/pic/crt/g/46/fc/4135_seiyu_anidb.jpg?r=1459625015"}}]},{"id":7095,"url":"http://bgm.tv/character/7095","name":"怪盗キッド","name_cn":"怪盗基德","role_name":"主角","images":{"large":"http://lain.bgm.tv/pic/crt/l/60/a7/7095_crt_YkUoT.jpg?r=1473585503","medium":"http://lain.bgm.tv/pic/crt/m/60/a7/7095_crt_YkUoT.jpg?r=1473585503","small":"http://lain.bgm.tv/pic/crt/s/60/a7/7095_crt_YkUoT.jpg?r=1473585503","grid":"http://lain.bgm.tv/pic/crt/g/60/a7/7095_crt_YkUoT.jpg?r=1473585503"},"comment":10,"collects":73,"info":{"name_cn":"怪盗基德","alias":{"0":"怪盗1412号","1":"基德","2":"1412","3":"KID","en":"Kid the phantom thief","jp":"怪盗キッド","kana":"かいとうキッド","romaji":"Kaitou Kid"},"gender":"男","source":"anidb.net"},"actors":[{"id":3900,"url":"http://bgm.tv/person/3900","name":"山口勝平","images":{"large":"http://lain.bgm.tv/pic/crt/l/46/c8/3900_prsn_RB1me.jpg?r=1494885663","medium":"http://lain.bgm.tv/pic/crt/m/46/c8/3900_prsn_RB1me.jpg?r=1494885663","small":"http://lain.bgm.tv/pic/crt/s/46/c8/3900_prsn_RB1me.jpg?r=1494885663","grid":"http://lain.bgm.tv/pic/crt/g/46/c8/3900_prsn_RB1me.jpg?r=1494885663"}}]},{"id":7045,"url":"http://bgm.tv/character/7045","name":"吉田歩美","name_cn":"吉田步美","role_name":"配角","images":{"large":"http://lain.bgm.tv/pic/crt/l/bc/8d/7045_crt_7cHj7.jpg?r=1444821809","medium":"http://lain.bgm.tv/pic/crt/m/bc/8d/7045_crt_7cHj7.jpg?r=1444821809","small":"http://lain.bgm.tv/pic/crt/s/bc/8d/7045_crt_7cHj7.jpg?r=1444821809","grid":"http://lain.bgm.tv/pic/crt/g/bc/8d/7045_crt_7cHj7.jpg?r=1444821809"},"comment":1,"collects":11,"info":{"name_cn":"吉田步美","alias":{"kana":"よしだ あゆみ","romaji":"Yoshida Ayumi"},"gender":"女","weight":"15","source":"anidb.net"},"actors":[{"id":4163,"url":"http://bgm.tv/person/4163","name":"岩居由希子","images":{"large":"http://lain.bgm.tv/pic/crt/l/60/49/4163_seiyu_anidb.jpg","medium":"http://lain.bgm.tv/pic/crt/m/60/49/4163_seiyu_anidb.jpg","small":"http://lain.bgm.tv/pic/crt/s/60/49/4163_seiyu_anidb.jpg","grid":"http://lain.bgm.tv/pic/crt/g/60/49/4163_seiyu_anidb.jpg"}}]}]
     * staff : [{"id":681,"url":"http://bgm.tv/person/681","name":"青山剛昌","name_cn":"青山刚昌","role_name":"","images":{"large":"http://lain.bgm.tv/pic/crt/l/15/95/681_prsn_anidb.jpg","medium":"http://lain.bgm.tv/pic/crt/m/15/95/681_prsn_anidb.jpg","small":"http://lain.bgm.tv/pic/crt/s/15/95/681_prsn_anidb.jpg","grid":"http://lain.bgm.tv/pic/crt/g/15/95/681_prsn_anidb.jpg"},"comment":13,"collects":0,"info":{"name_cn":"青山刚昌","alias":{"jp":"青山剛昌","kana":"あおやま　ごうしょう","romaji":"Aoyama Goushou"},"gender":"男","birth":"1963年6月21日"},"jobs":["原作"]},{"id":682,"url":"http://bgm.tv/person/682","name":"山本泰一郎","name_cn":"山本泰一郎","role_name":"","images":{"large":"http://lain.bgm.tv/pic/crt/l/08/d9/682_prsn_anidb.jpg","medium":"http://lain.bgm.tv/pic/crt/m/08/d9/682_prsn_anidb.jpg","small":"http://lain.bgm.tv/pic/crt/s/08/d9/682_prsn_anidb.jpg","grid":"http://lain.bgm.tv/pic/crt/g/08/d9/682_prsn_anidb.jpg"},"comment":5,"collects":0,"info":{"name_cn":"山本泰一郎","alias":{"kana":"やまもと やすいちろう","romaji":"Yamamoto Yasuichirou"},"gender":"男","birth":"1961年"},"jobs":["导演"]},{"id":1400,"url":"http://bgm.tv/person/1400","name":"こだま兼嗣","name_cn":"儿玉兼嗣","role_name":"","images":{"large":"http://lain.bgm.tv/pic/crt/l/f0/dd/1400_prsn_anidb.jpg","medium":"http://lain.bgm.tv/pic/crt/m/f0/dd/1400_prsn_anidb.jpg","small":"http://lain.bgm.tv/pic/crt/s/f0/dd/1400_prsn_anidb.jpg","grid":"http://lain.bgm.tv/pic/crt/g/f0/dd/1400_prsn_anidb.jpg"},"comment":12,"collects":0,"info":{"name_cn":"儿玉兼嗣","alias":{"jp":"児玉兼嗣","kana":"こだま けんじ","romaji":"Kodama Kenji"},"gender":"男","birth":"1949-12-13"},"jobs":["导演"]},{"id":3196,"url":"http://bgm.tv/person/3196","name":"於地紘仁","name_cn":"于地纮仁","role_name":"","images":{"large":"http://lain.bgm.tv/pic/crt/l/08/d3/3196_prsn_pHb8S.jpg","medium":"http://lain.bgm.tv/pic/crt/m/08/d3/3196_prsn_pHb8S.jpg","small":"http://lain.bgm.tv/pic/crt/s/08/d3/3196_prsn_pHb8S.jpg","grid":"http://lain.bgm.tv/pic/crt/g/08/d3/3196_prsn_pHb8S.jpg"},"comment":0,"collects":0,"info":{"name_cn":"于地纮仁","alias":{"0":"Ochi Hirohito","jp":"越智浩仁（おちひろひと）","kana":"おちこうじん","romaji":"Ochi Koujin"},"gender":"男"},"jobs":["导演"]},{"id":1570,"url":"http://bgm.tv/person/1570","name":"佐藤真人","name_cn":"佐藤真人","role_name":"","images":null,"comment":0,"collects":0,"info":{"name_cn":"佐藤真人","alias":{"0":"佐藤正人","kana":"さとうまさと","romaji":"Satou Masato"},"gender":"男"},"jobs":["导演"]},{"id":1027,"url":"http://bgm.tv/person/1027","name":"島田満","name_cn":"岛田满","role_name":"","images":{"large":"http://lain.bgm.tv/pic/crt/l/88/3e/1027_prsn_anidb.jpg","medium":"http://lain.bgm.tv/pic/crt/m/88/3e/1027_prsn_anidb.jpg","small":"http://lain.bgm.tv/pic/crt/s/88/3e/1027_prsn_anidb.jpg","grid":"http://lain.bgm.tv/pic/crt/g/88/3e/1027_prsn_anidb.jpg"},"comment":34,"collects":0,"info":{"name_cn":"岛田满","alias":{"kana":"しまだ みちる","romaji":"Shimada Michiru"},"gender":"女","birth":"1959年5月19日","bloodtype":"O型","卒日":"2017年12月15日"},"jobs":["脚本"]},{"id":1179,"url":"http://bgm.tv/person/1179","name":"飯岡順一","name_cn":"饭冈顺一","role_name":"","images":null,"comment":0,"collects":0,"info":{"name_cn":"饭冈顺一","alias":{"jp":"飯岡順一","kana":"いいおか じゅんいち","romaji":"Iioka Jun`ichi"},"gender":"男","birth":"1945年9月6日"},"jobs":["脚本"]},{"id":1460,"url":"http://bgm.tv/person/1460","name":"桜井正明","name_cn":"樱井正明","role_name":"","images":null,"comment":0,"collects":0,"info":{"name_cn":"樱井正明","alias":{"kana":"さくらい まさあき","romaji":"Sakurai Masaaki"},"gender":"男","birth":"1948年12月10日","卒日":"2010年2月20日"},"jobs":["脚本"]},{"id":1420,"url":"http://bgm.tv/person/1420","name":"辻真先","name_cn":"辻真先","role_name":"","images":{"large":"http://lain.bgm.tv/pic/crt/l/90/db/1420_prsn_anidb.jpg","medium":"http://lain.bgm.tv/pic/crt/m/90/db/1420_prsn_anidb.jpg","small":"http://lain.bgm.tv/pic/crt/s/90/db/1420_prsn_anidb.jpg","grid":"http://lain.bgm.tv/pic/crt/g/90/db/1420_prsn_anidb.jpg"},"comment":1,"collects":0,"info":{"name_cn":"辻真先","alias":{"0":"牧薩次","1":"浦川しのぶ","2":"桂真佐喜","jp":"辻真先","kana":"つじまさき","romaji":"Tsuji Masaki"},"gender":"男","birth":"1932-03-23"},"jobs":["脚本"]}]
     */

    data class RatingBean (
            var total: Int = 0,
            var count: CountBean? = null,
            var score: Double = 0.toDouble()
    ){
        /**
         * total : 3069
         * count : {"10":331,"9":335,"8":907,"7":977,"6":391,"5":97,"4":19,"3":3,"2":2,"1":7}
         * score : 7.6
         */

        data class CountBean(
                @SerializedName("10")
                var `_$10`: Int = 0,
                @SerializedName("9")
                var `_$9`: Int = 0,
                @SerializedName("8")
                var `_$8`: Int = 0,
                @SerializedName("7")
                var `_$7`: Int = 0,
                @SerializedName("6")
                var `_$6`: Int = 0,
                @SerializedName("5")
                var `_$5`: Int = 0,
                @SerializedName("4")
                var `_$4`: Int = 0,
                @SerializedName("3")
                var `_$3`: Int = 0,
                @SerializedName("2")
                var `_$2`: Int = 0,
                @SerializedName("1")
                var `_$1`: Int = 0
        ) {
            /**
             * 10 : 331
             * 9 : 335
             * 8 : 907
             * 7 : 977
             * 6 : 391
             * 5 : 97
             * 4 : 19
             * 3 : 3
             * 2 : 2
             * 1 : 7
             */
        }
    }

    data class CollectionBean (
            var wish: Int = 0,
            var collect: Int = 0,
            var doing: Int = 0,
            var on_hold: Int = 0,
            var dropped: Int = 0
    ){
        /**
         * wish : 171
         * collect : 2011
         * doing : 1880
         * on_hold : 941
         * dropped : 273
         */
    }
}