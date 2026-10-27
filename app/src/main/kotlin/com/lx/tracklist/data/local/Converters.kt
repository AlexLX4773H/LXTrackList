package com.lx.tracklist.data.local

import androidx.room.TypeConverter
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.Priority
import com.lx.tracklist.data.model.TrackingUnit

class Converters {
    @TypeConverter
    fun fromMediaType(value: MediaType?): String? = value?.name

    @TypeConverter
    fun toMediaType(value: String?): MediaType? = value?.let { MediaType.fromString(it) }

    @TypeConverter
    fun fromMediaStatus(value: MediaStatus?): String? = value?.name

    @TypeConverter
    fun toMediaStatus(value: String?): MediaStatus? = value?.let { MediaStatus.fromString(it) }

    @TypeConverter
    fun fromTrackingUnit(value: TrackingUnit?): String? = value?.name

    @TypeConverter
    fun toTrackingUnit(value: String?): TrackingUnit? = value?.let { TrackingUnit.fromString(it) }

    @TypeConverter
    fun fromPriority(value: Priority?): String? = value?.name

    @TypeConverter
    fun toPriority(value: String?): Priority? = value?.let { Priority.fromString(it) }
}
