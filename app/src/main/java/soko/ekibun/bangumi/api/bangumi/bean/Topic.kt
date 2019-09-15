package soko.ekibun.bangumi.api.bangumi.bean

data class Topic(
        val group: String,
        val title: String,
        val images: Images,
        val replies: List<TopicPost>,
        val post: String,
        val lastview: String?,
        val links: Map<String, String>,
        val error: String?,
        val errorLink: String?
)