package com.bandtrack.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Convertisseurs pour stocker des types complexes dans Room (Map, List...)
 * Utilise Kotlinx Serialization pour convertir en String JSON
 */
class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringMap(value: String): Map<String, String> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun toStringMap(map: Map<String, String>): String {
        return json.encodeToString(map)
    }

    @TypeConverter
    fun fromIntMap(value: String): Map<String, Int> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun toIntMap(map: Map<String, Int>): String {
        return json.encodeToString(map)
    }

    @TypeConverter
    fun fromBooleanMap(value: String): Map<String, Boolean> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun toBooleanMap(map: Map<String, Boolean>): String {
        return json.encodeToString(map)
    }

    @TypeConverter
    fun fromStringList(value: String): List<String> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun toStringList(list: List<String>): String {
        return json.encodeToString(list)
    }
}
