Carrie Remote Control
=====================

Introduction
------------

This is a remote control tool to control media playback via network commands. The media player must be a Linux machine and the controller can be a web browser or Android device.

The players supported are:

- Youtube on Firefox or Chromium browser.
- BBC iPlayer on Firefox or Chromium.
- mplayer

This project is not a media center. There is no function to start media playback or change the file being played. `carrie` is used only to control a video which is already playing. The functions available are:

- Play/pause
- Toggle full screen
- Volume up annd down
- Mute

If the client player is `mplayer` then these additional functions are available:

- Skip forwards/backwards
- Toggle fullscreen, OSD, subtitle visibility
- Change audio and subtitle languages.

Screenshots
-----------

See the `doc` directory.

How does it work?
-----------------

`carrie` is a process running on the machine doing the media playback. It opens a port (5505 by default) and supplies a simple web interface. Alternatively the `carrie` Android application can send commands to the same port from an Android phone or tablet..

When a command is received the server will attempt to control a web browser showing Flash video. If one is not found then the server will try to control mplayer via a FIFO object - this uses the 'slave mode' feature of `mplayer`. `mplayer` must be configured in advance to listen on the FIFO.

Limitations
-----------

Since there is no API for communicating with Flash video, the server process sends fake mouse events for control. This has to be configured for each combination of browser and video site.

License
-------

The server and application are licensed under the GPLv3.

Why use this and not another remote control?
--------------------------------------------

There are other remote controls for Android devices with more functions. `carrie` is for users who already have a method they use to start media playback, and just want to add some remote functions.

Other remote controls I would recommend are:

- sshmote: A media center for Android. Does not need any special server software except a Unix compatible OS running an SSH server.
- teamviewer: Take control of a Windows or Linux machine from a web browser or phone.

Requirements
------------

To run the server:

- Python 2.7+ (programming language)
- Flask (lightweight Python web server framework)
- xdotool (command line tool to fake mouse and keyboard events)
- xwininfo (command line tool to read window information)

To control the server:

- A web browser

To use the mobile phone applet:

- Android device running Android Froyo (2.1) or later

Server installation
-------------------

First install the server software::

    > sudo pip install carrie

or from source::

    > python setup.py build
    > sudo python setup.py install

or without root access::

    (download and uncompress tarball from http://pypi.python.org/pypi/carrie)
    > python setup.py build
    > virtualenv $HOME/.carrie-env
    > . $HOME/.carrie-env/bin/activate
    > python setup.py install

To configure `mplayer` to accept commands over a FIFO, edit `$HOME/.mplayer/config` and add this line::

    input=file=/tmp/mplayer.fifo

Any new instances of mplayer will connect to the FIFO and listen to commands. This doesn't affect normal mplayer usage. The FIFO will be created by `carrie` on startup, or maually with::

    > mkfifo /tmp/mplayer.fifo

Start the server
----------------

Run::

    > carrie

This will start the server and listen on port 5505, using a FIFO on /tmp/mplayer.fifo to communicate with mplayer. The FIFO will be created if it doesn't already exist.

Run::

    > carrie -h

to see available options.

To run in the background detached from the terminal use::

    > nohup carrie 2>&1 > /dev/null &

Control via browser
-------------------

Open a web browser and visit::

    http://<server>:5505

For controlling Flash video this should be done from a different window, or different computer, from the screen showing the video. It is not possible to control videos on one tab from a different tab.

Android application installation
--------------------------------

Either download the full project from GitHub and compile the Android application, or search for Carrie on the Android marketplace.

Changes and news
----------------

0.3 (2012-01-06)
~~~~~~~~~~~~~~~~

 * Added 'About' server page, release application to Android marketplace.

0.2 (2012-01-05)
~~~~~~~~~~~~~~~~

 * Initial pypi release
