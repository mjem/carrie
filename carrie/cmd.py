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

"""Entry points for command line tools `carrie` and `stop-screensaver`
"""

import argparse

from carrie import log
from carrie import fifo
from carrie import server

DEFAULT_FIFO = '/tmp/mplayer.fifo'
DEFAULT_PORT = 5505

# from pkg_resources import Requirement, resource_filename
# filename = resource_filename(Requirement.parse("MyProject"),"sample.conf")


if __name__ == '__main__':
	carrie_main()


def carrie_main():
	"""Entry point to `carrie` command line tool."""

	# <program>  Copyright (C) <year>  <name of author>
    # This program comes with ABSOLUTELY NO WARRANTY; for details type `show w'.
    # This is free software, and you are welcome to redistribute it
    # under certain conditions; type `carrie --license' for details.
	parser = argparse.ArgumentParser(description='Carrie remote control server')
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
	args = parser.parse_args()

	# set up logging
	log.init_log(args.logfile)

	fifo.init_fifo(args.fifo)
	# Handle --console option
	if args.console:
		fifo.Console().run()
		parser.exit()

	# Listen for HTTP connections
	server.serve_forever(args.port, args.debug)


def stop_screensaver_main():
	"""Entry point to `stop-screensaver` command line tool."""

	parser = argparse.ArgumentParser(
		description='Disable X screensavers by periodically sending small mouse movements')
	args = parser.parse_args()
		# Spawn a separate thread for the screensaver stopper
	# if args.stop_xscreensaver:
	# 	stop_xscreensaver = xscreensaver.StopXScreensaver(180)
	# 	logging.debug('Starting thead')
	# 	stop_xscreensaver.deamon = True
	# 	stop_xscreensaver.start()
	# 	logging.debug('main')
