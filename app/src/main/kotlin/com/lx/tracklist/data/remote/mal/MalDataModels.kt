package com.lx.tracklist.data.remote.mal

import com.google.gson.annotations.SerializedName

data class MalTokenResponse(
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Long,
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String
)

data class MalUserProfile(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("picture") val picture: String? = null
)

data class MalPicture(
    @SerializedName("medium") val medium: String? = null,
    @SerializedName("large") val large: String? = null
)

data class MalAlternativeTitles(
    @SerializedName("synonyms") val synonyms: List<String>? = null,
    @SerializedName("en") val en: String? = null,
    @SerializedName("ja") val ja: String? = null
)

data class MalGenre(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class MalAnimeNode(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("main_picture") val mainPicture: MalPicture? = null,
    @SerializedName("num_episodes") val numEpisodes: Int = 0,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null,
    @SerializedName("synopsis") val synopsis: String? = null,
    @SerializedName("mean") val mean: Float? = null,
    @SerializedName("alternative_titles") val alternativeTitles: MalAlternativeTitles? = null,
    @SerializedName("genres") val genres: List<MalGenre>? = null
)

data class MalAnimeListStatus(
    @SerializedName("status") val status: String? = null,
    @SerializedName("score") val score: Int? = null,
    @SerializedName("num_episodes_watched") val numEpisodesWatched: Int = 0,
    @SerializedName("is_rewatching") val isRewatching: Boolean = false,
    @SerializedName("num_times_rewatched") val numTimesRewatched: Int = 0,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class MalAnimeListItem(
    @SerializedName("node") val node: MalAnimeNode,
    @SerializedName("list_status") val listStatus: MalAnimeListStatus? = null
)

data class MalPaging(
    @SerializedName("previous") val previous: String? = null,
    @SerializedName("next") val next: String? = null
)

data class MalAnimeListResponse(
    @SerializedName("data") val data: List<MalAnimeListItem>? = null,
    @SerializedName("paging") val paging: MalPaging? = null
)

data class MalMangaNode(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("main_picture") val mainPicture: MalPicture? = null,
    @SerializedName("num_volumes") val numVolumes: Int = 0,
    @SerializedName("num_chapters") val numChapters: Int = 0,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null,
    @SerializedName("synopsis") val synopsis: String? = null,
    @SerializedName("mean") val mean: Float? = null,
    @SerializedName("alternative_titles") val alternativeTitles: MalAlternativeTitles? = null,
    @SerializedName("genres") val genres: List<MalGenre>? = null
)

data class MalMangaListStatus(
    @SerializedName("status") val status: String? = null,
    @SerializedName("score") val score: Int? = null,
    @SerializedName("num_volumes_read") val numVolumesRead: Int = 0,
    @SerializedName("num_chapters_read") val numChaptersRead: Int = 0,
    @SerializedName("is_rereading") val isRereading: Boolean = false,
    @SerializedName("num_times_reread") val numTimesReread: Int = 0,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class MalMangaListItem(
    @SerializedName("node") val node: MalMangaNode,
    @SerializedName("list_status") val listStatus: MalMangaListStatus? = null
)

data class MalMangaListResponse(
    @SerializedName("data") val data: List<MalMangaListItem>? = null,
    @SerializedName("paging") val paging: MalPaging? = null
)

