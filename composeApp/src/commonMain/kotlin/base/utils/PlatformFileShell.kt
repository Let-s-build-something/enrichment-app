package base.utils

import io.github.vinceglb.filekit.core.PlatformFile

/** Shell for informing outside sources about a [PlatformFile] content */
class PlatformFileShell(val content: PlatformFile) {
    override fun toString(): String {
        return content.name
    }
}