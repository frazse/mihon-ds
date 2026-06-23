package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ALRecommendationsResult(
    val data: ALRecommendationsMedia,
)

@Serializable
data class ALRecommendationsMedia(
    @SerialName("Media")
    val media: ALRecommendationsNodes,
)

@Serializable
data class ALRecommendationsNodes(
    val recommendations: ALRecommendationsNodeList,
)

@Serializable
data class ALRecommendationsNodeList(
    val nodes: List<ALRecommendationsNode>,
)

@Serializable
data class ALRecommendationsNode(
    val mediaRecommendation: ALSearchItem?,
)
