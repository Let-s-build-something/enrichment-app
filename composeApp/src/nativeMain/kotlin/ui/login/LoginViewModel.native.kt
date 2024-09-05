package ui.login

import data.io.identity_platform.IdentityRefreshToken
import data.io.identity_platform.IdentityUserResponse
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import org.koin.dsl.module

/** module providing platform-specific sign in options */
actual fun signInServiceModule() = module {
    single { UserOperationServiceImpl() }
}

internal class UserOperationServiceImpl: UserOperationService {
    override val availableOptions: List<SingInServiceOption> = listOf(
        SingInServiceOption.GOOGLE,
        SingInServiceOption.APPLE
    )

    override suspend fun requestGoogleSignIn(
        filterAuthorizedAccounts: Boolean,
        webClientId: String
    ): LoginResultType {
        Firebase.auth.signInWithCredential(
            GoogleAuthProvider.credential(
                Firebase.app.options.applicationId,
                webClientId
            )
        )

        return LoginResultType.FAILURE
    }

    override suspend fun requestAppleSignIn(webClientId: String): LoginResultType {
        return LoginResultType.FAILURE
    }

    override suspend fun signUpWithPassword(email: String, password: String): IdentityUserResponse? = null
    override suspend fun signInWithPassword(email: String, password: String): IdentityUserResponse? = null
    override suspend fun refreshToken(refreshToken: String): IdentityRefreshToken? = null
}