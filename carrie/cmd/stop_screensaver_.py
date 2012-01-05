#!/usr/bin/env python

"""Stop screensaver from kicking in by nudging the mouse every few minutes.
There does not appear to be a reliable, neat, cross-desktop solution.
"""

def main():
	"""Entry point to `stop-screensaver` command line tool."""

	parser = argparse.ArgumentParser(
		description='Disable X screensavers by periodically sending small mouse movements')
	parser.add_argument('--stop-xscreensaver',
						action='store_true')
	parser.add_argument('--delay',
						type=int,
						help=('Number of seconds between mouse nudges if --stop-xscreensaver is '
							  'used'))
	args = parser.parse_args()
		# Spawn a separate thread for the screensaver stopper
	# if args.stop_xscreensaver:
	# 	stop_xscreensaver = xscreensaver.StopXScreensaver(180)
	# 	logging.debug('Starting thead')
	# 	stop_xscreensaver.deamon = True
	# 	stop_xscreensaver.start()
	# 	logging.debug('main')

if __name__ == '__main__':
	main()
