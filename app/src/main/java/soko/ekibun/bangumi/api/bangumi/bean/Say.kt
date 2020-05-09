package soko.ekibun.bangumi.api.bangumi.bean

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.ReplaySubject
import okhttp3.FormBody
import org.jsoup.Jsoup
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.util.HttpUtil
import kotlin.collections.set

data class Say(
    val id: Int,
    var user: UserInfo,
    var message: String? = null,
    var time: String? = null,
    var replies: List<SayReply>? = null
) {
    val cacheKey = "say_$id"

    val url: String get() = "${Bangumi.SERVER}/user/${user.username}/timeline/status/$id"

    data class SayReply(
        val user: UserInfo,
        val message: String,
        val index: Int
    )

    companion object {
        /**
         * 从url解析吐槽
         * @param url String
         * @return Say?
         */
        fun parse(
            url: String,
            user: UserInfo? = null,
            message: String? = null,
            time: String? = null
        ): Say? {
            val result = Regex("""/user/([^/]+)/timeline/status/(\d+)""").find(url)?.groupValues ?: return null
            return Say(
                id = result.getOrNull(2)?.toIntOrNull() ?: return null,
                user = user ?: UserInfo(
                    id = result.getOrNull(1)?.toIntOrNull() ?: 0,
                    username = result.getOrNull(1) ?: return null
                ),
                message = message,
                time = time
            )
        }

        /**
         * 加载吐槽
         * @return Call<SayReply|List<SayReply>|Say>
         */
        fun getSaySax(say: Say): Observable<Any> {
            return ApiHelper.createHttpObservable(say.url).flatMap { rsp ->
                Observable.create<Any> { emitter ->
                    val avatarCache = HashMap<String, String>()
                    say.user.avatar?.let { avatarCache[say.user.username!!] = it }
                    say.replies?.forEach { reply ->
                        reply.user.avatar?.let { avatarCache[reply.user.username!!] = it }
                    }
                    val unresolvedReplies = HashMap<String, ArrayList<SayReply>>()

                    val replyPub = ReplaySubject.create<String>()
                    val observable = replyPub.flatMap { username ->
                        Observable.just(0).observeOn(Schedulers.computation()).takeWhile {
                            !emitter.isDisposed
                        }.map {
                            UserInfo.getApiUser(username)
                        }
                    }.subscribeOn(Schedulers.newThread())

                    observable.subscribe({ user ->
                        if (emitter.isDisposed) return@subscribe
                        if (!user.avatar.isNullOrEmpty()) synchronized(avatarCache) {
                            user.let { avatarCache[it.username!!] = it.avatar!! }
                        }
                        synchronized(unresolvedReplies) {
                            unresolvedReplies.remove(user.username)?.apply {
                                this.forEach {
                                    it.user.avatar = user.avatar
                                }
                            }
                        }?.let { if (!emitter.isDisposed) emitter.onNext(it) }
                    }, {
                        if (!emitter.isDisposed) emitter.onError(it)
                    })

                    var beforeData = ""
                    val replies = ArrayList<SayReply>()
                    val updateReply = { str: String ->
                        val doc = Jsoup.parse(str)
                        doc.outputSettings().prettyPrint(false)
                        if (beforeData.isEmpty()) {
                            beforeData = str
                            say.user = UserInfo.parse(
                                doc.selectFirst(".statusHeader .inner a"),
                                Bangumi.parseImageUrl(doc.selectFirst(".statusHeader .avatar img"))
                            )
                            say.message = doc.selectFirst(".statusContent .text")?.html()
                            say.time = doc.selectFirst(".statusContent .date")?.text()
                            if (!emitter.isDisposed) emitter.onNext(SayReply(say.user, say.message ?: "", 0))
                        } else {
                            val user = UserInfo.parse(doc.selectFirst("a.l"))
                            user.avatar = user.avatar ?: synchronized(avatarCache) {
                                avatarCache[user.username]
                            }
                            val sayReply = SayReply(
                                user = user,
                                message = doc.selectFirst(".reply_item").childNodes()?.let { it.subList(6, it.size) }
                                    ?.joinToString("") {
                                        it.outerHtml()
                                    }?.trim() ?: "",
                                index = replies.size + 1
                            )
                            replies += sayReply
                            if (user.avatar.isNullOrEmpty()) {
                                synchronized(unresolvedReplies) {
                                    if (!unresolvedReplies.containsKey(user.username)) replyPub.onNext(user.username!!)
                                    unresolvedReplies.getOrPut(user.username!!) { ArrayList() }.add(sayReply)
                                }
                            }
                        }
                    }

                    ApiHelper.parseSax(rsp) { element, str ->
                        if (emitter.isDisposed) return@parseSax ApiHelper.SaxEventType.END
                        when {
                            element.hasClass("reply_item") -> {
                                updateReply(str())
                                ApiHelper.SaxEventType.BEGIN
                            }
                            element.attr("id") == "footer" -> {
                                updateReply(str())
                                ApiHelper.SaxEventType.END
                            }
                            else -> ApiHelper.SaxEventType.NOTHING
                        }
                    }
                    replyPub.onComplete()
                    observable.blockingSubscribe()
                    say.replies = replies
                    if (!emitter.isDisposed) {
                        emitter.onNext(say)
                        emitter.onComplete()
                    }
                }
            }
        }

        fun reply(say: Say, content: String): Observable<Boolean> {
            return ApiHelper.createHttpObservable(
                "${Bangumi.SERVER}/timeline/${say.id}/new_reply?ajax=1",
                body = FormBody.Builder()
                    .add("content", content)
                    .add("formhash", HttpUtil.formhash)
                    .add("submit", "submit").build()
            ).map { rsp ->
                rsp.body?.string()?.contains("\"status\":\"ok\"") == true
            }
        }
    }
}