package ui.login

import cocoapods.GoogleSignIn.GIDSignIn
import data.io.identity_platform.IdentityRefreshToken
import data.io.identity_platform.IdentityUserResponse
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationScopeEmail
import platform.AuthenticationServices.ASAuthorizationScopeFullName
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** module providing platform-specific sign in options */
actual fun signInServiceModule() = module {
    factoryOf(::UserOperationService)
    single<UserOperationService> { UserOperationService() }
}

actual class UserOperationService {
    actual val availableOptions: List<SingInServiceOption> = listOf(
        SingInServiceOption.GOOGLE,
        //SingInServiceOption.APPLE
    )

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun requestGoogleSignIn(
        filterAuthorizedAccounts: Boolean,
        webClientId: String
    ): LoginResultType {
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        return if (rootViewController == null) {
            LoginResultType.NO_WINDOW
        } else {
            val signInResult = requestCredentials(rootViewController)

            signInResult.first?.user?.let { user ->
                val idToken = user.idToken?.tokenString
                val accessToken = user.accessToken.tokenString

                if(Firebase.auth.signInWithCredential(
                        GoogleAuthProvider.credential(idToken, accessToken)
                    ).user != null
                ) {
                    LoginResultType.SUCCESS
                } else {
                    LoginResultType.FAILURE
                }
            } ?: LoginResultType.FAILURE
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun requestCredentials(rootViewController: UIViewController) = suspendCoroutine { continuation ->
        GIDSignIn.sharedInstance.signInWithPresentingViewController(rootViewController) { result, error ->
            continuation.resume(result to error)
        }
    }

    private var currentNonce: String? = null

    @OptIn(ExperimentalUuidApi::class, ExperimentalForeignApi::class)
    actual suspend fun requestAppleSignIn(): LoginResultType {
        currentNonce = Uuid.random().toString()
        val appleIDProvider = ASAuthorizationAppleIDProvider()
        val request = appleIDProvider.createRequest().apply {
            requestedScopes = listOf(
                ASAuthorizationScopeFullName,
                ASAuthorizationScopeEmail
            )
            nonce = currentNonce
        }
        suspendCoroutine { continuation ->
            ASAuthorizationController(authorizationRequests = listOf(request)).run {
                delegate = object: NSObject(), ASAuthorizationControllerDelegateProtocol, ASAuthorizationControllerPresentationContextProvidingProtocol {

                    override fun authorizationController(
                        controller: ASAuthorizationController,
                        didCompleteWithAuthorization: ASAuthorization
                    ) {
                        continuation.resume(didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential)
                    }

                    override fun authorizationController(
                        controller: ASAuthorizationController,
                        didCompleteWithError: NSError
                    ) {
                        continuation.resume(null)
                    }

                    override fun presentationAnchorForAuthorizationController(
                        controller: ASAuthorizationController
                    ): UIWindow? = UIApplication.sharedApplication.keyWindow
                }
                performRequests()
            }
        }?.let { credential ->
            Firebase.auth.signInWithCredential(
                OAuthProvider.credential(
                    providerId = "apple.com",
                    rawNonce = currentNonce,
                    idToken = credential.identityToken?.string()
                )
            )
        }

        return LoginResultType.FAILURE
    }

    private fun NSData.string(): String? =
        NSString.create(data = this, encoding = NSUTF8StringEncoding)?.toString()

    actual suspend fun signUpWithPassword(email: String, password: String): IdentityUserResponse? = null
    actual suspend fun signInWithPassword(email: String, password: String): IdentityUserResponse? = null
    actual suspend fun refreshToken(refreshToken: String): IdentityRefreshToken? = null
}