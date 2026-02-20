package com.speekez.data.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.speekez.data.entity.ModelTier
import com.speekez.data.entity.RefinementLevel

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromRefinementLevel(value: RefinementLevel): String {
        return value.name
    }

    @TypeConverter
    fun toRefinementLevel(value: String): RefinementLevel {
        return try {
            RefinementLevel.valueOf(value)
        } catch (e: Exception) {
            RefinementLevel.NONE
        }
    }

    @TypeConverter
    fun fromModelTier(value: ModelTier): String {
        return value.name
    }

    @TypeConverter
    fun toModelTier(value: String): ModelTier {
        return try {
            ModelTier.valueOf(value)
        } catch (e: Exception) {
            ModelTier.CHEAP
        }
    }
}
