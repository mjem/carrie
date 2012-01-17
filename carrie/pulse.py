#!/usr/bin/env python2.7

"""Volume control of a pulseaudio server.
"""

from __future__ import division

import re
import logging

from carrie.shell import shell

def paste(i):
	it = iter(i)
	try:
		while True:
			yield it.next(), it.next()

	except StopIteration:
		pass

def get_volume(target="Sink #0"):
	#         Volume: 0:  60% 1:  16%

	trigger = False
	matcher = re.compile(r'.*Volume: (?P<vals>.*)')
	for line in shell('pactl', 'list'):
		# print 'LINE ', line
		if trigger:
			# print line
			match = matcher.match(line)
			if match is not None:
				# print 'MATCH ', match.group('vals'), 'x', match.group('vals').split(), 'x'
				#res = {}
				acc = 0.0
				cc = 0
				for k, v in paste(match.group('vals').split()):
					# assume the value is like "40%"
					# res[k] = int(v.replace('%', '')) / 100
					acc += int(v.replace('%', '')) / 100
					cc += 1

				res = acc / cc
				logging.debug('Read volume as {vol}'.format(vol=res))
				return res

		if line.startswith(target):
			# print 'TRIGGER ', line
			trigger = True

	return None

def set_volume(val, sink=0):
	logging.debug('Setting volume to {vol}'.format(vol=val))
	shell('pactl', 'set-sink-volume', str(sink), str(int(val*65535)))

def volup():
	set_volume(get_volume() + 0.05)

def voldown():
	set_volume(get_volume() - 0.05)

def main():
	from carrie import log
	log.init_log()
	import argparse
	parser = argparse.ArgumentParser()
	parser.add_argument('--show',
						action='store_true')
	parser.add_argument('--volup',
						action='store_true')
	parser.add_argument('--voldown',
						action='store_true')
	args = parser.parse_args()

	if args.show:
		print get_volume()
		parser.exit()

	elif args.volup:
		volup()
		parser.exit()

	elif args.voldown:
		voldown()
		parser.exit()

	parser.error('No actions specified')

if __name__ == '__main__':
	main()
