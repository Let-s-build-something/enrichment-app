package data

object HostedData {
    /** base URL for  */
    private const val baseUrl = "https://cubeit.cz/"

    sealed class Image(private val appendix: String) {

        /** full url to image */
        val url: String = baseUrl + folder + appendix

        companion object {
            private const val folder = "img/"
        }

        data object SignIn: Image("i0_sign_up.webp")
        data object SignUp: Image("i1_sign_in.webp")
    }
}