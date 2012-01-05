#!/usr/bin/env python

# Carrie Remote Control
# Copyright (C) 2012 Mike Elson

# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.

# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.

# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

"""Web server for carrie process.
Supplies a web site index page and dispatches media commands.
"""

import os
import socket
import logging

import flask
import werkzeug

from carrie import xorg
from carrie import fifo

app = flask.Flask(__name__)


@app.route('/')
def index():
	"""Send main index page template to browser."""
	return flask.render_template('index.html')


@app.route('/favicon.ico')
def favicon():
	"""Send static/favicon.png as a favicon"""
	# url_for('static', filename='favicon.png')
	return flask.send_file(os.path.join(os.path.dirname(__file__), 'static', 'favicon.png'),
						   mimetype='image/png')


@app.route('/carrie/hello')
def hello():
	"""Response to a ping from the Android application with the host name
	(the client may have been configured with IP address only)."""
	return socket.gethostname()


@app.route('/android/application')
def android_application():
	"""Return the Android application"""
	filename = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'android', 'bin', 'carrie.apk')
	if os.path.exists(filename):
		return flask.send_file(filename,
							   attachment_filename='carrie.apk',
							   as_attachment=True,
							   mimetype='application/vnd.android.package-archive')
	else:
		return "not configured"


@app.route('/android')
def android():
	"""Send the browser a page with a link to download the Android application."""

	return flask.render_template('android.html',
								 name='carrie.apk',
								 url=flask.url_for('android_application'))


@app.route('/pause')
def pause():
	"""Try to play/pause an youtube or iplayer widget. If not found, send play/pause
	to the mplayer fifo.
	"""
	if xorg.autopause() is None:
		return "ok (flash)"

	else:
		res = fifo.send("pause")
		if res == 'ok':
			return 'ok'

		else:
			return 'no media player found'


@app.route('/forward/<int:seconds>')
def forward(seconds):
	"""Send a skip forwards to mplayer only."""
	logging.info('forward ' + str(seconds) + 's')
	return fifo.send("seek " + str(seconds))


@app.route('/backward/<int:seconds>')
def backward(seconds):
	"""Send a skip backwards to mplayer only."""
	logging.info('backward ' + str(seconds) + 's')
	return fifo.send("seek -" + str(seconds))


@app.route('/volup')
def volup():
	"""On volume up request we:
	- Tell the window manager to increase the volume and
	- Tell mplayer to increase the volume
	"""
	xorg.shell('xdotool', 'key', 'XF86AudioRaiseVolume')
	fifo.send("volume 20")
	return 'ok'


@app.route('/voldown')
def voldown():
	"""On volume up request we:
	- Tell the window manager to decrease the volume and
	- Tell mplayer to increase the volume
	"""
	xorg.shell('xdotool', 'key', 'XF86AudioLowerVolume')
	fifo.send("volume -20")
	return 'ok'


@app.route('/fullscreen')
def fullscreen():
	"""Toggle fullscreen mode in flash control or mplayer."""
	if xorg.autofullscreen() is None:
		return "ok (flash)"

	else:
		return fifo.send("vo_fullscreen")


@app.route('/mute')
def mute():
	"""On mute toggle request we:
	- Tell mplayer to toggle the volume
	- Tell the window manager to toggle the volume and
	"""
	if fifo.send("mute") == 'ok':
		return 'ok'

	else:
		xorg.shell('xdotool', 'key', 'XF86AudioMute')

	return 'ok'


@app.route('/osdon')
def osdon():
	"""Tell mplayer to switch on on screen display."""
	return fifo.send("osd 3")


@app.route('/osdoff')
def osdoff():
	"""Tell mplayer to switch off on screen display."""
	return fifo.send("osd 1")


@app.route('/sub')
def sub():
	"""Tell mplayer to toggle subtitle visibility."""
	logging.debug('sending subvisibility')
	return fifo.send("sub_visibility")


@app.route('/sublang')
def sublang():
	"""Tell mplayer to switch between subtitle languages."""
	return fifo.send("sub_select")


@app.route('/audlang')
def audlang():
	"""Tell mplayer to switch between audio languages."""
	return fifo.send("switch_audio")


def serve_forever(port, debug):
	"""Start the web server on `port` with optional Flask `debug` mode
	(not safe in any public environment as it allows connections to run code
	in the program's environment.
	"""

	# print 'static ', os.path.join(os.path.dirname(__file__), 'static')
	app.wsgi_app = werkzeug.SharedDataMiddleware(app.wsgi_app, {
			'/static': os.path.join(os.path.dirname(__file__), 'static')})

	app.run(host='0.0.0.0', port=port, debug=debug)
