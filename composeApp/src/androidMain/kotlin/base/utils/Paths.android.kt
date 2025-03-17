package base.utils

import android.content.Context
import okio.Path.Companion.toOkioPath
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformPathsModule(): Module = module {
    single<RootPath> {
        val context = get<Context>()
        RootPath(context.filesDir.toOkioPath())
    }
}
