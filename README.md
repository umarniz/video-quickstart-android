# Video Quickstart for Android

This application should give you a ready-made starting point for writing your
own video apps with Twilio Video. Before we begin, we need to collect
all the credentials we need to run the application:

## Gather Twilio account information

The first thing we need to do is grab all the necessary information from our
Twilio account. To set up our back-end for Video, we will need four 
values from our account:

Config Values  | Description
:-------------  |:-------------
Configuration Profile SID | A set of configuration values for webhooks and other options for Video. [Generate one in the console here](/user/account/video/profiles).
Account SID | Your primary Twilio account identifier - find this [in the console here](/user/account/video).
API Key | Used to authenticate - [generate one here](/user/account/video/dev-tools/api-keys).
API Secret | Used to authenticate - [just like the above, you'll get one here](/user/account/video/dev-tools/api-keys).

## Set up the server app

A Video application has two pieces - a client (our Android app) and a server.
You can learn more about what the server app does [in this guide](identity).
For now, let's just get a simple server running so we can use it to power our
Android application.

<a href="https://github.com/TwilioDevEd/video-quickstart-php/archive/master.zip" 
    class="button button-block call-to-action">
    Download server app for PHP
</a>

If you prefer, there are backend apps available for [other server-side languages](quickstart-js).

This (beta) tutorial assumes you are able to run a PHP application on your development machine. On a Mac, the `php` command line tool is pre-installed and should be available at the terminal. On Linux or Windows, you may need to install php before continuing. 

At the command line, navigate to the directory of the extracted php application that you downloaded above. Then run the following command:

~~~
cp config.example.php config.php
~~~

Open `config.php` in your editor and enter in your account credentials inside the single quotes for the appropriate variables. Now we're ready to start the server - again in your terminal, run:

~~~
php -S localhost:8000
~~~

To confirm everything is set up correctly, visit [http://localhost:8000](http://localhost:8000)
in a web browser. You should be assigned a random username, and be able to invite
other users to a video conversation.

Feel free to use this app up between a couple browser tabs. Next, you'll
also find this browser app useful when testing your Android app, giving you an
easy second screen to run a video call on. **Leave the php server app running** in the Terminal, 
so that your Android app running in the simulator can talk to it.

Now that our server is set up, let's get the starter Android app up and running.

## Configure the mobile app

Download and unzip this project now, and import it into Android Studio.

The application is built using [Gradle](http://gradle.org/getting-started-android/) and is [available on GitHub](https://github.com/twilio/video-quickstart-android).

Your dependencies should all be installed and ready to rock. Locate the following code snippet in the file ConversationsActivity.java:

~~~
// OPTION 2- Retrieve an access token from your own web app
// retrieveAccessTokenfromServer();
~~~

Uncomment the second line which calls `retrieveAccessToken()`, and comment out the entire code block constituting OPTION 1. 

### Running in the Emulator

*Note: In release 0.8.1, emulator support is currently limited to armeabi-v7a images. As such, we recommend using a physical device during development. This will change with the next release.*

If you load the app in the emulator, you won't be able to access the camera
feed since the Android emulator doesn't have access to a camera. The app will
still work and you'll be able to create Conversations and see events logged in the console, you just want see your local camera preview. Once you load the app on your device, you'll be able to access the local camera.

If you launch the app in the emulator, you should be able to video chat with the browser app we started earlier.

### Running on a Device {#device}

The mobile app will attempt to retrieve a token from the PHP server that we started running at `http://localhost:8000`. This URL will only be visible while running on the simulator on your machine. To test on a device, you'll need to replace the localhost URL in `ConversationsActivity` with a public URL for your token server. If you're not ready to deploy a public web server, you can use something like <a href="http://www.ngrok.com">ngrok</a> to expose your local server on the public Internet. For example:

~~~
    private void retrieveAccessTokenfromServer() {
        Ion.with(this)
                .load("http://http://e0e68fa.ngrok.com/token.php)
                .asJsonObject()
~~~

The app should launch on your device and fetch a token from the PHP server we set up earlier. From here, open the browser app that we started earlier. From the Android app, click the Call button and type in the identity assigned to you in the browser application. Press Call and you should be receive a Conversation Invite in the browser. The browser app will accept the Invite automatically, and a video Conversation will be created.

From here, you can start building your own application. For guidance on integrating Twilio Video's Conversations framework into your existing project, [head over to our install guide](/docs/api/video/sdks). If you'd like to learn more about how Video works, you might want to dive into our user [identity](identity) guide, which talks about the relationship between the mobile app and the server.

Good luck and have fun!

## License

MIT
