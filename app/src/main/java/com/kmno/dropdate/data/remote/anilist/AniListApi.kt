package com.kmno.dropdate.data.remote.anilist

import retrofit2.http.Body
import retrofit2.http.POST

fun aniListScheduleQuery(
    weekStartUnix: Int,
    weekEndUnix: Int,
) = AniListRequest(
    query =
        """
        query (${'$'}weekStart: Int, ${'$'}weekEnd: Int) {
          Page(page: 1, perPage: 100) {
            airingSchedules(
              airingAt_greater: ${'$'}weekStart
              airingAt_lesser: ${'$'}weekEnd
              sort: TIME
            ) {
              id
              airingAt
              episode
              media {
                id
                title { romaji english }
                coverImage { large }
                averageScore
                description(asHtml: false)
                genres
                externalLinks { site }
              }
            }
          }
        }
        """.trimIndent(),
    variables = mapOf("weekStart" to weekStartUnix, "weekEnd" to weekEndUnix),
)

interface AniListApi {
    // Base URL ends with "/"; "." resolves back to root → https://graphql.anilist.co/
    @POST(".")
    suspend fun getAnimeSchedule(
        @Body request: AniListRequest,
    ): AniListResponse
}
