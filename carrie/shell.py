#!/usr/bin/env python2.7

import subprocess

def shell(*command):
	"""Run a shell command, returning stdout as an array of strings.

	>>> shell("echo", "hello")
	['hello\n']

	"""

	try:
		p = subprocess.Popen(command,
							 stdout=subprocess.PIPE,
							 stderr=subprocess.PIPE)
	except OSError as e:
		if e.errno == 2:  # No such file or directory
			raise IOError('Cannot run ' + ' '.join(command))

	raw, _ = p.communicate()
	res = []
	for line in raw.split('\n'):
		if len(line) > 0:
			# yield line
			res.append(line)

	return res
