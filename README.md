This is a Kotlin Multiplatform project targeting Android, iOS, Desktop.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.

* `/iosApp` contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform, 
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.


Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…


**SETUP**

**iOS** side uses CocoaPods for dependency management, so in order to get iOS running, you need to setup CocoaPods on your computer, see how to set it up here: https://kotlinlang.org/docs/native-cocoapods.html#add-and-configure-kotlin-cocoapods-gradle-plugin. In order for the build to run, the file Pods within iosApp module has to be filled with the dependencies. List of dependencies is defined in Podsfile (also within iosApp module). So the only thing needed to be done is to run command "pod install" (which installs the dependencies) on the iosApp module (folder), if there is no "Pod" folder, you should run the "pod init" before the "pod install". When opening the project in Xcode, make sure you're opening the "iosApp.xcworkspace" file under the iosApp module.


The simulator you're using to run the app can be configured in Android Studio (meaning, that in theory you don't need Xcode, from my experience, there is an issue of dependencies not being found when run from Android Studio). The configuration can be done with the help of Android Studio plugin "Kotlin Multiplatform" by JetBrains (current version 0.8.3). See following image for the specific configuration that can be used.

![Screenshot 2024-09-10 at 10 20 50](https://github.com/user-attachments/assets/e9c60e44-3b20-4c96-8e70-4ce341bdb268)

---------------------------------


**Jvm** requires Java, as it's run on it. OpenJDK 20.0.2 is confirmed to be working both when you build the app from macOS, and Windows. The easiest way to download it is to head to Android Studio > Settings > Gradle > Gradle JDK > Download JDK, and choose Oracle OpenJDK 22.0.2. The app is built by running the cradle script [run], which can be found under Gradle scripts > composeApp > Tasks > compose desktop > run.


---------------------------------


**Android** similarly to Jvm, Android requires in order to build into an app. Open JDK 22.0.2 is proven to work here as well. Specific setup for Android that is required is to setup your own key_store.jks file (see here for how-to: https://www.ibm.com/docs/en/rational-change/5.3.0?topic=https-generating-keystore-files), which can be stored locally anywhere in your computer. Its path has to be defines within keystore.properties file, together with password to the key store, key password, and Web API Key (which can be found in Cloud Console in our project > Credentials). All of the information within keystore.properties is local only, as the file is in .gitignore - thus, stays only on your device.
In order to run the app, you also need to pair Firebase with your specific signature (which is dictated by the key store - those are the keys to sign the app). In order to do so, after completing the previous step, run the script "gradle signingReport", which will show you your signatures specific to the key store used on your device. Copy the SHA-1 to list of signatures within Firebase and update the google-services.json file, which will add you to authorized people to sign the app. 
