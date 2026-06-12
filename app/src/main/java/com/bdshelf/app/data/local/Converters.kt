package com.bdshelf.app.data.local

import androidx.room.TypeConverter
import com.bdshelf.app.data.local.entities.ReadStatus
import com.bdshelf.app.data.local.entities.SeriesStatus

/** Conversion des enums Room <-> TEXT (stockage par nom). */
class Converters {
    @TypeConverter
    fun fromSeriesStatus(value: SeriesStatus): String = value.name

    @TypeConverter
    fun toSeriesStatus(value: String): SeriesStatus = SeriesStatus.valueOf(value)

    @TypeConverter
    fun fromReadStatus(value: ReadStatus): String = value.name

    @TypeConverter
    fun toReadStatus(value: String): ReadStatus = ReadStatus.valueOf(value)
}
