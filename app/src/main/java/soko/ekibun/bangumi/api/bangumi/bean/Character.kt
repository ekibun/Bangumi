package soko.ekibun.bangumi.api.bangumi.bean

import soko.ekibun.bangumi.api.bangumi.Bangumi

/**
 * 虚拟人物类
 * @property id Int
 * @property role_name String?
 * @property comment Int
 * @property collects Int
 * @property actors List<Person>?
 * @constructor
 */
class Character(
    val id: Int = 0,
    name: String? = null,
    name_cn: String? = null,
    image: String? = null,
    val role_name: String? = null,
    val comment: Int = 0,
    val collects: Int = 0,
    val actors: List<Person>? = null
) : MonoInfo(name, name_cn, image, "", "${Bangumi.SERVER}/character/$id")