# Twilio Video Quickstart for Android

Use this project to get started with Twilio Video's Android SDK by [following along with the quickstart guide here](https://www.twilio.com/docs/api/video/guide/quickstart-android).

 [ ![Download](https://api.bintray.com/packages/twilio/releases/conversations-android/images/download.svg) ](https://bintray.com/twilio/releases/conversations-android/_latestVersion)

## Up and Running

1) Create a Twilio Video [Configuration Profile](https://www.twilio.com/user/account/video/profiles). If you haven't used Twilio before, welcome! You'll need to [Sign up for a Twilio account](https://www.twilio.com/try-twilio).

2) Download this project and open it in Android Studio.

3) Get an access token [Generate an Access Token](https://www.twilio.com/user/account/video/dev-tools/testing-tools). Pick your identity (such as Bob). Leave this web page open, because you'll use it as the other side of the video chat.

4) Paste the access token into ConversationActivity.java

5) Run your app on an Android device or the Android emulator

6) On the same web page where you generated the token, scroll down the bottom, put in the username that you generated the access token for, and click Create Conversation. Your video conversation should start immediately! 

## What is this project?

This quick start will help you get video chat integrated directly into your Android applications using Twilio's Video Conversations SDK. 

Inside this quick start project, you will find an Activity that contains all of the functionality necessary to show two video streams on one screen - one video stream for your phone's video camera, and one for a remote video stream.

You'll see how to set up key classes like TwilioAccessManager, ConversationsClient, LocalMedia, Conversation, and CameraCapturer. All of these Twilio classes have related listeners with implementations in ConversationActivity.java.

You can also see how Android's runtime permissions are implemented for access to the camera and the microphone on Android devices running version 6.0 (Marshmallow) or higher inside the onCreate() method in the ConversationActivity class. It's important to note that you shouldn't initialize the Twilio client until the user grants access to the camera and microphone.

## Prerequisites

This project is a native Android application written in Java. You will need an up-to-date version of Android Studio (at the time of this writing, 2.1.0), to build the application. You will also need version 23.0.2 of the Android build tools, which you can download through Android Studio if you do not have them installed.

The minimum supported Android version is 4.0.3 for devices or Android emulators. You can run the quick start on an x86 Android emulator - either 32-bit or 64-bit.

## Access Tokens and Servers

Using Twilio's Video client within your applications requires an access token. These access tokens can come from one of two places:

1) You can create a one-time use access token for testing on Twilio.com. This access token can be hard-coded directly into your mobile app, and you won't need to run your own server.

2) You can run your own server that provides access tokens, based on your Twilio credentials. This server can either run locally on your development machine, or it can be installed on a server. If you run the server on your local machine, you should use the ngrok utility to give the server an externally accessible web address. That way, you can run the quick start app on an Android device.

### Generating an Access Token

The first step is to [Generate an Access Token](https://www.twilio.com/user/account/video/dev-tools/testing-tools) from the Twilio developer console. Use whatever clever username you would like for the identity. You will get an access token that you can copy and paste into ConversationActivity.java

Once you have that access token in place, scroll down to the bottom of the page and you will get a web-based video chat window in the Twilio developer console that you can use to communicate with your Android app! Just invite that identity you just named above!

### Setting up a Video Chat Web Server

If you want to be a little closer to a real environment, you can download one of the video quickstart applications - for instance, [Video Quickstart: PHP](https://github.com/TwilioDevEd/video-quickstart-php) and either run it locally, or install it on a server.

 You'll need to gather a couple of configuration options from your Twilio developer console before running it, so read the directions on the quickstart. You'll copy the config.example.php file to a config.php file, and then add in these credentials:
 
 Credential | Description
---------- | -----------
Twilio Account SID | Your main Twilio account identifier - [find it on your dashboard](https://www.twilio.com/user/account/video).
Twilio Video Configuration SID | Adds video capability to the access token - [generate one here](https://www.twilio.com/user/account/video/profiles)
API Key | Used to authenticate - [generate one here](https://www.twilio.com/user/account/messaging/dev-tools/api-keys).
API Secret | Used to authenticate - [just like the above, you'll get one here](https://www.twilio.com/user/account/messaging/dev-tools/api-keys).

#### A Note on API Keys

When you generate an API key pair at the URLs above, your API Secret will only
be shown once - make sure to save this in a secure location.

#### Running the Video Quickstart with ngrok

If you run your video chat application on an Android device, you'll need to provide an externally accessible URL for the app. The [ngrok](https://ngrok.com/) tool creates a publicly accessible URL that you can use to send HTTP/HTTPS traffic to a server running on your localhost. Use HTTPS to make web connections that retrieve a Twilio access token.

When you get a URL from ngrok, go ahead and update ConversationActivity.java with the new URL. At the very bottom is the retrieveAccessTokenfromServer() method.  If you go down this path, be sure to follow the directions in the comments in the initializeTwilioSdk() method at the top of the source file - you will need to comment out everything under "OPTION 1" and uncomment the one line under "OPTION 2". You will also need to update the code if your ngrok URL changes.

## Have fun!

This is an introduction to Twilio's Video Conversations SDK on Android. From here, you can start building applications that use video chat across the web, iOS, and Android platforms.

## License

MIT
