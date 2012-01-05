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

"""Handle interactions with the FIFO.
"""

import os
import stat
import logging

driver = None


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


def send(command):
	"""Use the wrapper class to send a string command to mplayer."""
	try:
		driver.send(command)
	except driver.NoClient:
		return 'mplayer client not found'
	except driver.NoFifo:
		return 'fifo not found'

	return 'ok'


def is_fifo(filename):
	"""Is `filename` a FIFO?"""
	st_mode = os.stat(filename)[0]
	return stat.S_ISFIFO(st_mode)


def create_fifo(filename):
	"""Create `filename` as a FIFO if it does not already exist."""
	if os.path.exists(filename) and not is_fifo(filename):
		raise IOError(filename + ' already exists but is not a fifo')

	if not os.path.exists(filename):
		logging.info('Creating fifo ' + filename)
		os.mkfifo(filename)


def init_fifo(filename):
	"""Create fifo object if needed and create a module SlaveDriver object.
	"""

	global driver

	# create the FIFO object to communicate with mplayer
	create_fifo(filename)

	# Wrapper to send commands to the FIFO
	driver = SlaveDriver(filename)


class Console(object):
	"""Little command line dispatcher to the FIFO. Like `cmd` module but very primitive.
	For experimenting with mplayer slave mode.
	"""

	def run(self):
		"""Loop around dispatching user inputs to FIFO."""
		while True:
			command = raw_input('> ')
			#print
			driver.send(command)
