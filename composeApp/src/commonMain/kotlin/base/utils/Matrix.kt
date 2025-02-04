package base.utils

object Matrix {
    private const val LOGIN = "m.login"

    const val LOGIN_DUMMY = "$LOGIN.dummy"
    const val LOGIN_EMAIL_IDENTITY = "$LOGIN.email.identity"
    const val LOGIN_RECAPTCHA = "$LOGIN.recaptcha"
    const val LOGIN_TERMS = "$LOGIN.terms"
    const val LOGIN_PASSWORD = "$LOGIN.password"

    object ErrorCode {
        const val FORBIDDEN = "M_FORBIDDEN"
        const val LIMIT_EXCEEDED = "M_LIMIT_EXCEEDED"
        const val UNKNOWN = "M_UNKNOWN"
        const val CREDENTIALS_IN_USE = "M_THREEPID_IN_USE"
        const val CREDENTIALS_DENIED = "M_THREEPID_DENIED"
    }

    object Id {
        const val USER = "m.id.user"
        const val THIRD_PARTY = "m.id.thirdparty"
    }

    object Medium {
        const val EMAIL = "email"
    }

    object Media {
        const val MATRIX_REPOSITORY_PREFIX = "mxc://"
    }
}