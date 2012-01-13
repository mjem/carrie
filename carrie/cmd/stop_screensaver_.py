#!/usr/bin/env python

"""Stop screensaver from kicking in by nudging the mouse every few minutes.
There does not appear to be a reliable, neat, cross-desktop solution.
"""

import argparse

from carrie import xscreensaver

def main():
	"""Entry point to `stop-screensaver` command line tool."""

	parser = argparse.ArgumentParser(
		description='Disable X screensavers by periodically sending small mouse movements')
	parser.add_argument('--delay',
						type=int,
						default=180,
						help='Number of seconds between mouse nudges')
	args = parser.parse_args()

	stop_xscreensaver = xscreensaver.StopXScreensaver(args.delay)
	stop_xscreensaver.run()  # run in own thread; we don't use the threading option

if __name__ == '__main__':
	main()
