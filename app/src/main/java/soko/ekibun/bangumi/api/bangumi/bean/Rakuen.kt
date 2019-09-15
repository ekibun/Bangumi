package soko.ekibun.bangumi.api.bangumi.bean

/**
 * 超展开列表
 * @property images 头像
 * @property topic 标题
 * @property group 小组
 * @property time 时间
 * @property reply 回复数
 * @property url 链接
 * @property groupUrl 小组链接
 * @constructor
 */
data class Rakuen(
        val images: Images,
        val topic: String,
        val group: String?,
        val time: String,
        val reply: Int = 0,
        val url: String,
        val groupUrl: String?
)