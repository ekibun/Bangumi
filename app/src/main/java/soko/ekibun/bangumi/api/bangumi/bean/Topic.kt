package soko.ekibun.bangumi.api.bangumi.bean

data class Topic(
        val group: String,
        val title: String,
        val replies: List<TopicPost>,
        val post: String,
        val formhash: String?,
        val lastview: String?,
        val links: Map<String, String>
)