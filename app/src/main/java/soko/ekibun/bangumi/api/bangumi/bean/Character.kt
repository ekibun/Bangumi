package soko.ekibun.bangumi.api.bangumi.bean

import soko.ekibun.bangumi.api.bangumi.Bangumi

/**
 * 虚拟人物类
 * @property id 人物id
 * @property role_name 出演角色
 * @property comment 吐槽数
 * @property collects 收藏数
 * @property actors 演员（CV）
 * @constructor
 */
class Character(
        val id: Int = 0,
        name: String? = null,
        name_cn: String? = null,
        images: Images? = null,
        val role_name: String? = null,
        val comment: Int = 0,
        val collects: Int = 0,
        val actors: List<Person>? = null
) : MonoInfo(name, name_cn, images, "", "${Bangumi.SERVER}/character/$id")