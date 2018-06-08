package soko.ekibun.bangumi.api.bangumi.bean

object StatusCode{
    val code = mapOf(
            200 to "OK",
            202 to "Accepted",
            304 to "Not Modified",
            30401 to "Not Modified: Collection already exists",
            400 to "Bad Request",
            40001 to "Error: Nothing found with that ID",
            401 to "Unauthorized",
            40101 to "Error: Auth failed over 5 times",
            40102 to "Error: Username is not an Email address",
            405 to "Method Not Allowed",
            404 to "Not Found")
}