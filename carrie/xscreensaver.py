#!/usr/bin/env python

import threading

from carrie import xorg

class StopXScreensaver(threading.Thread):
	"""Called in a separate process. Moves the mouse pointer a pixel left or right
	every 3 minutes to stop the screensaver activating.
	Seems to double fire for some reason.
	"""

	def __init__(self, delay):
		super(StopXScreensaver, self).__init__()
		self.delay = delay

	def run(self):
		xdir = 1
		while True:
			mouse_pos = xorg.get_mouse_pos()
			logging.debug('mouse shift from ' + str(mouse_pos))
			if mouse_pos is not None:
				x, y = mouse_pos
				x = int(x)
				# if x > 0:
					# x -= 1
				# else:
					# x += 1
				x += xdir
				xdir = -xdir

				xorg.shell('xdotool', 'mousemove', str(x), y)

			time.sleep(self.delay)

if __name__ == '__main__':
	import argparse
	parser = argparse.ArgumentParser()
	parser.add_argument('--delay',
						type=int,
						help='Number of seconds between mouse nudges')
	args = parser.parse_args()
	stop_xscreensaver = StopXScreensaver(args.delay)
	logging.debug('Starting thead')
	# stop_xscreensaver.deamon = True
	stop_xscreensaver.run()
	# stop_xscreensaver.start()
	# logging.debug('main')
