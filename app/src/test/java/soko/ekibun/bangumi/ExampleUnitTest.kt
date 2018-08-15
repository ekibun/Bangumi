package soko.ekibun.bangumi

import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import org.junit.Test

import org.junit.Assert.*
import soko.ekibun.bangumi.api.bangumi.bean.TopicPost
import soko.ekibun.bangumi.util.JsonUtil

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    class PostList: ArrayList<TopicPost>()
    @Test
    fun addition_isCorrect() {
        val gson = "{\"1372754\":[{\"pst_id\":\"1374499\",\"pst_mid\":\"346386\",\"pst_uid\":\"419012\",\"pst_content\":\"<div class=\\\"quote\\\"><q><span style=\\\"font-weight:bold;\\\">ekibun</span> 说: test</q></div>test\",\"username\":\"419012\",\"nickname\":\"ekibun\",\"sign\":\"\",\"avatar\":\"//lain.bgm.tv/pic/user/s/000/41/90/419012.jpg?r=1530032761\",\"dateline\":\"2018-8-14 16:29\",\"model\":\"group\",\"is_self\":true}]}"
        val obj = (JsonUtil.toEntity<Map<String, PostList>>(gson, object: TypeToken<Map<String, PostList>>(){}.type)?:HashMap())
        obj.forEach {
            it.value.forEach {
                print(it.toString())
            }
        }
        assertEquals(4, 2 + 2)
    }
}
