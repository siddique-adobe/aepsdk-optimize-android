# Optimize Tutorial: Fetch and track Target Offers

## Getting Started

Follow the steps below to download the Optimize Tutorial Starter App from the [Optimize GitHub repository](https://github.com/adobe/aepsdk-optimize-android).

1. Navigate to the GitHub repository using the URL https://github.com/adobe/aepsdk-optimize-android/tree/optimize-target-tutorial.

2. Click on **Code (1)** then select **Download ZIP (2)** from the pop-up dialog.

| ![Optimize Extension Code](../../assets/optimize-github-code.png?raw=true) |
| :---: |
| **Optimize GitHub Code** |

3. Copy the `aepsdk-optimize-android-optimize-target-tutorial.zip` file from Downloads directory to another appropriate location. For example, your home directory

**Command-line command**
```text
mv ~/Downloads/aepsdk-optimize-android-optimize-target-tutorial.zip ~/
```

4. Unzip the file in the target location.

**Command-line command**
```text
cd ~/
unzip aepsdk-optimize-android-optimize-target-tutorial.zip
```

5. Change directory to the `OptimizeTutorialStarterApp`

**Command-line command**
```text
cd aepsdk-optimize-android-optimize-target-tutorial/docs/tutorials/OptimizeTutotialStarterApp
```

6. Open `build.gradle` file in Android Studio.

**Command-line command**
```text
open build.gradle
```

Select `Trust Project`, if promoted while opening the gradle file.

| ![Trust Android project](../../assets/android-trust-project.png?raw=true) |
| :---: |
| **Trust Android project** |

## Install AEPOptimize SDK Extension in your mobile application

Follow the steps in [Install SDK Extensions guide](https://opensource.adobe.com/aepsdk-optimize-android/#/tutorials/mobile-app/install-sdk-extensions) to install AEPOptimize SDK extension and its dependencies in your mobile application.

For this tutorial, the `OptimizeTutorialStarterApp` uses [Maven Central](https://search.maven.org/) for dependency management. The dependencies are already added in the app's `build.gradle` file. Verify the file to inspect the dependency versions.

## Initialize the mobile SDK

Follow the steps in [Initialize SDK guide](https://opensource.adobe.com/aepsdk-optimize-android/#/tutorials/mobile-app/init-sdk) to initialize the Experience Platform mobile SDK by registering the SDK extensions with `Mobile Core`.

## Enable Optimize API implementation code

Follow the steps below to enable the SDK implementation code:

1. In Android Studio, expand `app` project. You will see all the `.kt` source files in `java` -> `com.adobe.marketing.optimizetutorial` folder (**1**). Select `MainApplication.kt` file (**2**) and provide your `DATA_COLLECTION_ENVIRONMENT_FILE_ID` value (**3**). For more details, see [Getting the Environment File ID guide](http://localhost:3000/#/tutorials/setup/create-tag-property?id=getting-the-environment-file-id).

| ![AppDelegate - Configure Data Collection Environment File ID](../../assets/mobile-app-application.png?raw=true) |
| :---: |
| **AppDelegate - Configure Data Collection Environment File ID** |

2. Select`Edit` -> `Find` -> `Find in Files` (`Cmd+Shift+F` on Macbook keyboard) to search all files in the project (**1**).

| ![Mobile App - Find in Files](../../assets/mobile-app-find-in-files.png?raw=true) |
| :---: |
| **Mobile App - Find in Files** |


Enter text `Optimize Tutorial: CODE SECTION` in the search bar (2). It will list all the code implementation sections for this tutorial. The code sections follow the below format:

```text
/* Optimize Tutorial: CODE SECTION n/m BEGINS
...
Code Implementation
...
// Optimize Tutorial: CODE SECTION n ENDS */
```
where n = Current section number, m = Total number of sections in the mobile app

| ![Mobile App - Code Implementation Search](../../assets/mobile-app-code-section-search.png?raw=true) |
| :---: |
| **Mobile App - Code Implementation Search** |


3. Enable all the code sections sequentially, simply by adding a forward slash (/) at the beginning of every `/* Optimize Tutorial: CODE SECTION n/m BEGINS` statement:

```text
//* Optimize Tutorial: CODE SECTION n/m BEGINS
```

Enabling the code sections for initialization of the Experience Platform SDKs is shown in the image below.

| ![Mobile App - Code Implementation Enable](../../assets/mobile-app-code-section-enable.png?raw=true) |
| :---: |
| **Mobile App - Code Implementation Enable** |

## Run the mobile application

Follow the steps below to run the `OptimizeTutorialStarterApp` app:

1. Select the mobile app target **app (1)** and the destination device e.g. Nexus 4 API 30 emulator (2). Click on Play icon (3).

| ![Run Mobile App](../../assets/mobile-app-run.png?raw=true) |
| :---: |
| **Run Mobile App** |

2. You should see the mobile app running on your emulator device.

|![Offers View](../../assets/mobile-app-offers-view.png?raw=true) | ![Settings View](../../assets/mobile-app-settings-view.png?raw=true) |
| :---------: | :------------: |
| **Offers View** |  **Settings View** |

