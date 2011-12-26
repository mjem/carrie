<!-- -*- mode: markdown -*- -->

Carrie Remote Control
=====================

Introduction
------------

This project is a remote control tool to give some basic control of certain media types (listed below) running on a Linux computer. The user can send commands using either a web browser on another machine, or an Android application.

The players supported are:

- Youtube on Firefox or Chromium browser.
- BBC iPlayer on Firefox or Chromium.
- mplayer

This project is not a media center. There is no functionality for starting media playback or changing the file being played. The only functions available are:

- Play/pause
- Toggle full screen
- Volume up annd down

For mplayer clients only these functions are also available:

- Skip forwards/backwards
- Toggle fullscreen, OSD, subtitle visibility
- Change subtitle language.

How does it work?
-----------------

`carrie` is a process running on the machine doing the media playback. It opens a port (5505 by default) and supplied a simple web interface to any browsers which connect. Alternatively the `carrie` Android application can send commands to the same port.

When a command is received the server will attempt to send it to a web browser running a Flash video. If one is not found then the server will try to send the same command to mplayer via a FIFO object - this uses the 'slave mode' feature of `mplayer`. `mplayer` must be configured in advance to listen on the FIFO.

Limitations
-----------

There is no proper API for communicating with Flash video. The server process send fake mouse events for control. This has to be configured for each combination of browser and video site.

Why not just use x/y/z remote control instead of this?
------------------------------------------------------

There are other far better remote control tools than this around, with features such a full media center or providing full remote control of another machine. `carrie` is for users who already have another method they use to start media playback, and just want to add a basic remote function without changing anything else. And for me, it is a way to learn some new techniques and libraries.

Other remote controls I would recommend are:

- sshmote: A media center for Android. Does not need any special server software except a Unix compatible OS except an SSH server.
- teamviewer: Take control of a Windows or Linux machine from a web browser or phone. Has good features for operating over the Internet.

Requirements
------------

To run the server:

- Python 2.7+ (programming language)
- Flask (lightweight Python web server framework)
- xdotool (command line tool to fake mouse and keyboard events)
- xwininfo (command line tool to read window information)

To control the server:

- Any modern web browser

To use the mobile phone applet:

- Mobile phone running Android Froyo or later

Server installation
-------------------

First install the server:

    > git clone xxx
    > cd xxx
    > python setup.py build
    > sudo python setup.py install

And optionally configure mplayer by editing $HOME/.mplayer/config and adding the line:

    input=file=/tmp/mplayer.fifo

Any new instances of mplayer will connect to the FIFO and listen to commands. This doesn't affect normal mplayer usage.

Android application installation
--------------------------------

This project includes the Android application source in the `android` directory and a precompiled `carrie.apk` file for quick installation. It requires internet access permission to make connections to the server.

Start the server
----------------

Run:

    > carrie

This will start up the server, running in the foreground, listening on port 5505, using a FIFO on /tmp/mplayer.fif to communicate with mplayer. The FIFO will be created if it doesn't already exist.

Run:

    > carrie -h

to see available options.

Control via browser
-------------------

Open a web browser and visit: "http://<server>:5505"

Control from Android device
---------------------------

Install the Android application:

It requires internet access permission to connect to the server.

Press the MENU button and specify the server name or IP address and port before using it.

Development - server
--------------------

May wish to install:

- python-pip
- python-distribute

Development - Android application
---------------------------------

On Debian install: android sdk (manually), ant, openjdk-6-jdk

Compiling and installing the code should be the same as a standard Android application - either use Eclipse, or command line tools.

Directory layout
----------------

- android/
  Android controller application
- carrie/
  Python HTTP server code
- bin/
  Startup script
- README.md
  This file
