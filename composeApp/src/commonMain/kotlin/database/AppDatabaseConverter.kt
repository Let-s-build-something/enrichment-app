package database

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import org.koin.mp.KoinPlatform

/** Factory converter for Room database */
@ProvidedTypeConverter
class AppDatabaseConverter {

    private val json: Json by KoinPlatform.getKoin().inject()

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
/*
    *//** Converts object to string *//*
    @TypeConverter
    fun fromPagingMetaIO(value: PagingMetaIO): String {
        return json.encodeToString(value)
    }

    *//** Converts string to an object *//*
    @TypeConverter
    fun toPagingMetaIO(value: String): PagingMetaIO {
        return json.decodeFromString(value)
    }*/
}
