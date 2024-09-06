import koin.commonModule
import org.koin.core.context.startKoin

/** initializes koin */
fun initKoin() {
    startKoin {
        modules(commonModule)
    }
}