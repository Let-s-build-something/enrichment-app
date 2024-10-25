package data

/** base URL for the brand storage */
private const val BASE_URL = "https://augmy.org/storage/"

sealed class Asset {

    protected open val appendix: String = ""
    open val url: String = ""

    sealed class Image(name: String): Asset() {

        final override val appendix: String = "img/"
        override val url = BASE_URL + appendix + name
        open val thumbnail = BASE_URL + appendix + "thumbnail/" + "tn_$name"


        data object SignIn: Image("i0_sign_up.webp")
        data object SignUp: Image("i1_sign_in.webp")

        data object NaturePalette: Image("7b270e82-4ceb-43dc-bbc1-b5d47ff1cfc0.jpg")
        data object EarHelp: Image("e66f02af-b274-4acc-b25a-7bbcdd8e0131.jpg")
        data object Cooperation: Image("e4d1017e-d7d1-442c-ab1f-8a588d67b5b0.jpg")
        data object CustomDesign: Image("5802c63e-45fc-4687-8c72-d52066ef45e8.jpg")
        data object DesignConstruction: Image("90c610ff-e2e1-48e5-82c9-3c9440b765c3.jpg")
        data object Experiment: Image("95b14a4e-5182-4375-b78f-d0f6fd504926.jpg")
    }

    class Peep(index: Int): Asset() {
        override val appendix: String = "peep/"
        override val url = BASE_URL + appendix + "peep-${index + 1}.svg"
    }
}