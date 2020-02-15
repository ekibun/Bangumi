package soko.ekibun.bangumi.api.bangumi.bean

import soko.ekibun.bangumi.api.bangumi.Bangumi

/**
 * 现实人物
 * @property id Int
 * @constructor
 */
class Person(
    val id: Int = 0,
    name: String? = null,
    name_cn: String? = null,
    image: String? = null
) : MonoInfo(name, name_cn, image, "", "${Bangumi.SERVER}/person/$id")