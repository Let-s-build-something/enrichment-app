package database

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter

/** Factory converter for Room database */
@ProvidedTypeConverter
class AppDatabaseConverter {

    /** Converts list of strings to string */
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }

    /** Converts string to list of strings */
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return value.split(',')
    }
}