#!/usr/bin/env python

# Carrie Remote Control
# Copyright (C) 2011 Mike Elson

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
import stat
import socket
import logging
import argparse

import flask
import werkzeug

from carrie import log
from carrie import xorg
# from carrie import daemon
from carrie import xscreensaver

DEFAULT_FIFO = '/tmp/mplayer.fifo'
DEFAULT_PORT = 5505
# DEFAULT_PID = '/tmp/carrie.pid'

# from pkg_resources import Requirement, resource_filename
# filename = resource_filename(Requirement.parse("MyProject"),"sample.conf")


class SlaveDriver(object):
	"""Wrapper around FIFO object.
	"""

	class NoClient(Exception):
		"""No-one is listening on the end of the FIFO."""
		pass

	class NoFifo(Exception):
		"""The FIFO does not exist."""
		pass

	def __init__(self, fifo_filename):
		self.fifo_filename = fifo_filename
		self.fifo = None

	def send(self, message):
		"""Send text command `message` over the FIFO. Raises an exception if the FIFO
		does not exist or there is no client on the other end."""

		if self.fifo is None:
			logging.debug('Opening FIFO ' + self.fifo_filename)
			try:
				# fifo_fd = os.open(self.fifo_filename, os.O_RDWR|os.O_NONBLOCK)
				fifo_fd = os.open(self.fifo_filename, os.O_WRONLY | os.O_NONBLOCK)
			except OSError as e:
				if e.errno == 6:  # no listener on the other end of the fifo
					logging.debug('No fifo client')
					raise self.NoClient()

				if e.errno == 2:  # fifo not found
					logging.debug('No fifo')
					raise self.NoFifo()

				raise

			logging.debug('fd ' + str(fifo_fd))
			# self.fifo = os.fdopen(fifo_fd, 'r+', 0)
			self.fifo = os.fdopen(fifo_fd, 'w', 0)

		try:
			self.fifo.write(message + '\n')
			# self.fifo.flush()
		except IOError as e:
			if e.errno == 32:  # broken pipe
				logging.debug('Closing fifo')
				self.fifo = None
				raise self.NoClient()


class Console(object):
	"""Little command line dispatcher. Line `cmd` module but very primitive.
	For experimenting with mplayer slave mode.
	"""

	def run(self):
		"""Loop around dispatching user inputs to FIFO."""
		while True:
			command = raw_input('> ')
			#print
			driver.send(command)

app = flask.Flask(__name__)


def send(command):
	"""Use the wrapper class to send a string command to mplayer."""
	try:
		driver.send(command)
	except driver.NoClient:
		return 'mplayer client not found'
	except driver.NoFifo:
		return 'fifo not found'

	return 'ok'


@app.route('/')
def index():
	"""Send main index page template to browser."""
	return flask.render_template('index.html')


@app.route('/favicon.ico')
def favicon():
	"""Send static/favicon.png as a favicon"""
	return flask.send_file(os.path.join(os.path.dirname(__file__), 'static', 'favicon.png'),
						   mimetype='image/png')


@app.route('/carrie/hello')
def hello():
	"""Response to a ping from the Android application with the host name
	(the client may have been configured with IP address only)."""
	return socket.gethostname()

@app.route('/pause')
def pause():
	"""Try to play/pause an youtube or iplayer widget. If not found, send play/pause
	to the mplayer fifo.
	"""
	if xorg.autopause() is None:
		return "ok (flash)"

	else:
		res = send("pause")
		if res == 'ok':
			return 'ok'

		else:
			return 'no media player found'


@app.route('/forward/<int:seconds>')
def forward(seconds):
	"""Send a skip forwards to mplayer only."""
	logging.info('forward ' + str(seconds) + 's')
	return send("seek " + str(seconds))


@app.route('/backward/<int:seconds>')
def backward(seconds):
	"""Send a skip backwards to mplayer only."""
	logging.info('backward ' + str(seconds) + 's')
	return send("seek -" + str(seconds))


@app.route('/volup')
def volup():
	"""On volume up request we:
	- Tell the window manager to increase the volume and
	- Tell mplayer to increase the volume
	"""
	xorg.shell('xdotool', 'key', 'XF86AudioRaiseVolume')
	send("volume 20")
	return 'ok'


@app.route('/voldown')
def voldown():
	"""On volume up request we:
	- Tell the window manager to decrease the volume and
	- Tell mplayer to increase the volume
	"""
	xorg.shell('xdotool', 'key', 'XF86AudioLowerVolume')
	send("volume -20")
	return 'ok'


@app.route('/fullscreen')
def fullscreen():
	"""Toggle fullscreen mode in flash control or mplayer."""
	if xorg.autofullscreen() is None:
		return "ok (flash)"

	else:
		return send("vo_fullscreen")


@app.route('/mute')
def mute():
	"""On mute toggle request we:
	- Tell mplayer to toggle the volume
	- Tell the window manager to toggle the volume and
	"""
	if send("mute") == 'ok':
		return 'ok'

	else:
		xorg.shell('xdotool', 'key', 'XF86AudioMute')

	return 'ok'


@app.route('/osdon')
def osdon():
	"""Tell mplayer to switch on on screen display."""
	return send("osd 3")


@app.route('/osdoff')
def osdoff():
	"""Tell mplayer to switch off on screen display."""
	return send("osd 1")


@app.route('/sub')
def sub():
	"""Tell mplayer to toggle subtitle visibility."""
	logging.debug('sending subvisibility')
	return send("sub_visibility")


@app.route('/sublang')
def sublang():
	"""Tell mplayer to switch between subtitle languages."""
	return send("sub_select")


@app.route('/audlang')
def audlang():
	"""Tell mplayer to switch between audio languages."""
	return send("switch_audio")


def is_fifo(filename):
	"""Is `filename` a FIFO?"""
	st_mode = os.stat(filename)[0]
	return stat.S_ISFIFO(st_mode)


def create_fifo(filename):
	"""Create `filename` as a FIFO if it does not already exist."""
	if os.path.exists(filename) and not is_fifo(filename):
		parser.error(filename + ' already exists but is not a fifo')

	if not os.path.exists(filename):
		logging.info('Creating fifo ' + filename)
		os.mkfifo(filename)


def serve_forever(port, debug):
	"""Start the web server on `port` with optional Flask `debug` mode
	(not safe in any public environment as it allows connections to run code
	in the program's environment.
	"""

	# print 'static ', os.path.join(os.path.dirname(__file__), 'static')
	app.wsgi_app = werkzeug.SharedDataMiddleware(app.wsgi_app, {
			'/static': os.path.join(os.path.dirname(__file__), 'static')
			})

	app.run(host='0.0.0.0', port=port, debug=debug)


if __name__ == '__main__':
	# <program>  Copyright (C) <year>  <name of author>
    # This program comes with ABSOLUTELY NO WARRANTY; for details type `show w'.
    # This is free software, and you are welcome to redistribute it
    # under certain conditions; type `carrie --license' for details.
	parser = argparse.ArgumentParser()
	parser.add_argument('--fifo', '-f',
						default=DEFAULT_FIFO,
						help='Name of mplayer fifo')
	parser.add_argument('--console',
						action='store_true',
						help='Open a debug console connection instead of normal operations')
	parser.add_argument('--port', '-p',
						type=int,
						default=DEFAULT_PORT,
						help='HTTP port to listen on')
	# parser.add_argument('--create-fifo', '-c',
						# action='store_true',
						# help='Create FIFO if it does not already exist')
	parser.add_argument('--stop-xscreensaver',
						action='store_true')
	parser.add_argument('--delay',
						type=int,
						help=('Number of seconds between mouse nudges if --stop-xscreensaver is '
							  'used'))
	parser.add_argument('--logfile',
						metavar='LOG',
						help='Log to LOG instead of stdout')
	parser.add_argument('--debug',
						action='store_true',
						help='Use flask debug mode')
	# parser.add_argument('--pid',
						# default=DEFAULT_PID,
						# help='PID file if daemonise option is used')
	# parser.add_argument('--daemonise', '--daemonize', '-d',
						# action='store_true',
						# help='Detach from terminal')
	# parser.add_argument('--stop')
	# parser.add_argument('--reload')
	args = parser.parse_args()

	# set up logging
	log.init_log(args.logfile)

	# create the FIFO object to communicate with mplayer
	create_fifo(args.fifo)

	# Wrapper to send commands to the FIFO
	driver = SlaveDriver(args.fifo)

	# Handle --console option
	if args.console:
		Console().run()
		parser.exit()

	# Spawn a separate thread for the screensaver stopper
	if args.stop_xscreensaver:
		stop_xscreensaver = xscreensaver.StopXScreensaver(180)
		logging.debug('Starting thead')
		stop_xscreensaver.deamon = True
		stop_xscreensaver.start()
		logging.debug('main')

	# Listen for HTTP connections
	args.debug = True
	serve_forever(args.port, args.debug)
