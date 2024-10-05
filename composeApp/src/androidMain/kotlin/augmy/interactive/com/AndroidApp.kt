package augmy.interactive.com

import android.app.Application
import koin.commonModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class AndroidApp: Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this

        startKoin {
            androidContext(applicationContext)
            androidLogger()
            modules(commonModule)
        }
    }

    companion object {
        lateinit var instance: AndroidApp
            private set
    }
}