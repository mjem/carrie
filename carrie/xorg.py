#!/usr/bin/env python

"""Interact with the X server.
"""

import re
import logging
import subprocess

from carrie import fifo


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

# def find_top_windows(search):
# 	"""Yield all 'top level' X window ids."""

# 	for p in shell('xdotool', 'search', '-name', search):
# 		# print 'x ', p
# 		yield p

# def find_focus_window():
# 	"""Return id of window with focus."""

# 	for p in shell('xdotool', 'getwindowfocus'):
# 		return p


def raw_list(print_line=False, print_match=False):
	res = []
	line_matcher = re.compile(
		r'(?P<indent> +)(?P<id>0x[0-9a-f]+) (?P<name>.*) '
		'(?P<w>\d+)x(?P<h>\d+)\+(?P<rel_x>[0-9-]+)\+(?P<rel_y>[0-9-]+) +\+(?P<abs_x>[0-9-]+)\+(?P<abs_y>[0-9-]+)')
		# '(?P<n1>\d+)x(?P<n2>\d+)\+(?P<n3>\d+)\+(?P<n4>\d+) \+(?P<n5>\d+)\+(?P<n7>\d+)')
	for line in shell('xwininfo', '-root', '-tree'):
		if print_line:
			print 'LINE ', line

		line_match = line_matcher.match(line)
		if line_match is not None:
			indent = (len(line_match.group('indent'))-5)//3
			name = line_match.group('name')
			w = int(line_match.group('w'))
			h = int(line_match.group('h'))
			x = int(line_match.group('abs_x'))
			y = int(line_match.group('abs_y'))
			# print 'MATCH indent ', indent, ' name ', line_match.group('name'),\
			# 	' n1 ', line_match.group('n1'),\
			# 	' n2 ', line_match.group('n2'),\
			# 	' n3 ', line_match.group('n3'),\
			# 	' n4 ', line_match.group('n4'),\
			# 	' n5 ', line_match.group('n5'),\
			# 	' n6 ', line_match.group('n6')
			if print_match:
				print 'MATCH indent ', indent, ' name ', line_match.group('name'),\
					' x ', y, ' y ' , y , ' w ' , w , ' h ' , h

			# acc = res
			# for _ in xrange(indent):
				# acc = res[-1]
			# acc.append({'name': name, 'w': w, 'h': h, 'x': x, 'y': y})
			res.append({'indent': indent, 'name': name, 'w': w, 'h': h, 'x': x, 'y': y})

	return res

def fs_match(stack):
	"""Matches either YouTube or iPlayer full screen window"""
	# firefox fullscreen
	if len(stack) == 2:
		if '(has no name): ()' in stack[-2]['name']:
			if '"<unknown>": ("<unknown>" "<unknown>")' in stack[-1]['name']:
				stack[-1]['type'] = 'fs'
				return stack[-1]

	# chromium fullscreen
	if len(stack) == 2:
		if '"exe": ("exe" "Exe")' in stack[-1]['name']:
			stack[-1]['type'] = 'fs'
			return stack[-1]

	return None

def youtube_match(stack):
	if len(stack) == 5:
		# print 'Stack is five'
		# look for a Firefox Youtube (stack of 5 items, YouTube in last but one)
		if 'YouTube' in stack[-4]['name']:
			# print 'and YouTube is there'
			stack[-1]['type'] = 'youtube'
			return stack[-1]

		# look for a Chromium Youtube (stack of 5 items, YouTube in last)
	if len(stack) == 4:
		if '"exe": ("exe" "Exe")' in stack[-1]['name']:
			if 'YouTube' in stack[-4]['name']:
				# print 'and YouTube is theree'
				stack[-1]['type'] = 'youtube'
				return stack[-1]

def iplayer_match(stack):
	if len(stack) >= 5:
		if 'BBC iPlayer' in stack[-4]['name']:
			stack[-1]['type'] = 'iplayer'
			return stack[-1]

def mplayer_match(stack):
	if len(stack) >= 2:
		if '"MPlayer": ("xv" "MPlayer")' in stack[-1]['name']:
			stack[-1]['type'] = 'mplayer'
			return stack[-1]

def player_list(print_line=False, print_match=False, print_player=False):
	stack = []
	res = []
	for win in raw_list(print_line=print_line, print_match=print_match):
		# print 'new win ' , win
		stack = stack[:win['indent']]
		# print '  trim stack to ', stack
		stack.append(win)

		# print '  Stack:'
		# for s in stack:
		# 	print '    ', s

		for matcher in (fs_match, youtube_match, iplayer_match, mplayer_match):
			match = matcher(stack)
			if match is not None:
				res.append(match)
				if print_player:
					print 'PLAYER ', match

	return res

def top_player(print_line=False, print_match=False, print_player=False):
	players = player_list(print_line=print_line, print_match=print_match, print_player=print_player)

	if len(players) == 0:
		logging.error('No players found')
		return None

	elif players[0]['type'] == 'fs':
		if len(players) < 2:
			logging.error('Found fs window but no other player')
			return None

		elif players[1]['type'] == 'youtube':
			players[0]['type'] = 'youtube-fs'
			return players[0]

		elif players[1]['type'] == 'iplayer':
			players[0]['type'] = 'iplayer-fs'
			return players[0]

		else:
			logging.error('Found fs window but did not recognise previous player')
			return None

	else:
		return players[0]

def click(player, x_frac, y_frac):
	logging.info('Click x {x} y {y} w {w} h {h} x_frac {xf} y_frac {yf}'.format(
			x=player['x'], y=player['y'], w=player['w'], h=player['h'], xf=x_frac, yf=y_frac))
	x_abs = player['x'] + player['w'] * x_frac
	y_abs = player['y'] + player['h'] * (1-y_frac)
	shell('xdotool', 'mousemove', str(x_abs), str(y_abs), 'click', '1')
	return {'ok': True, 'message': 'done'}

def keypress(key):
	logging.info('Key ' + key)
	shell('xdotool', 'key', key)
	return {'ok': True, 'message': 'done'}


def command_to_player(command, player):
	if player['type'] == 'youtube':
		if command == 'pause':
			return click(player, 0.01, 0.01)

		elif command == 'fullscreen':
			return click(player, 0.99, 0.01)

		else:
			return {'ok': False, 'message': 'command not supported'}

	elif player['type'] == 'youtube-fs':
		if command == 'pause':
			return click(player, 0.5, 0.5)

		elif command == 'fullscreen':
			return keypress('Escape')

		else:
			return {'ok': False, 'message': 'command not supported'}

	elif player['type'] == 'iplayer-fs':
		if command == 'pause':
			return click(player, 0.01, 0.01)

		elif command == 'fullscreen':
			return keypress('Escape')

		else:
			return {'ok': False, 'message': 'command not supported'}

	elif player['type'] == 'iplayer':
		if command == 'pause':
			return click(player, 0.01, 0.01)

		elif command == 'fullscreen':
			return click(player, 0.42, 0.08)

		else:
			return {'ok': False, 'message': 'command not supported'}

	elif player['type'] == 'mplayer':
		if command == 'pause':
			fifo.send('pause')
			return {'ok': True, 'message': 'done'}

		elif command == 'fullscreen':
			shell('xdotool', 'search', '--class', 'mplayer', 'windowactivate')
			fifo.send("vo_fullscreen")
			return {'ok': True, 'message': 'done'}

		else:
			return {'ok': False, 'message': 'command not supported'}

	else:
		logging.error('Type ' + player['type'])
		return {'ok': False, 'message': 'bad player detection'}


def auto_command(command):
	player = top_player()
	if player is not None:
		return command_to_player(command, player)

	else:
		return None

# def win_info(win_id):
# 	"""For window (int) `win_id` return a tuple of:

# 	`name` :: Window name
# 	`x`, `y` :: Window position
# 	`flash_x`, `flash_y` :: Position within window of youtube or iplayer Flash control
# 	`flash_w`, `flash_h` :: dimensions of Flash control.

# 	If `win_id` does not contain a Flash playing media, `flash_*` are all set to None.

# 	"""

# 	x = None
# 	y = None
# 	width = None
# 	height = None
# 	# flash_x = None
# 	# flash_y = None
# 	# flash_w = None
# 	# flash_h = None
# 	# name_matcher = re.compile(r'xwininfo: Window id: [0-9a-fx]+ "(?P<name>.*)"')
# 	x_matcher = re.compile(r'.*Absolute upper-left X: +(?P<x>[0-9-]+)')
# 	y_matcher = re.compile(r'.*Absolute upper-left Y: +(?P<y>[0-9-]+)')
# 	width_matcher = re.compile(r'.*Width: +(?P<width>[0-9-]+)')
# 	height_matcher = re.compile(r'.*Height: +(?P<height>[0-9-]+)')
# 	if win_id == 'root':
# 		command = ['xwininfo', '-root']
# 	else:
# 		command = ['xwininfo', '-id', str(win_id)]

# 	for line in shell(*command):
# 		x_match = x_matcher.match(line)
# 		if x_match is not None:
# 			x = int(x_match.group('x'))

# 		y_match = y_matcher.match(line)
# 		if y_match is not None:
# 			y = int(y_match.group('y'))

# 		width_match = width_matcher.match(line)
# 		if width_match is not None:
# 			width = int(width_match.group('width'))

# 		height_match = height_matcher.match(line)
# 		if height_match is not None:
# 			height = int(height_match.group('height'))

# 	return {'x': x,
# 			'y': y,
# 			'w': width,
# 			'h': height}
# 	flash_matcher = re.compile(r'.*\"<unknown>"\) +'
# 							   '(?P<flash_w>\d+)x(?P<flash_h>\d+)'
# 							   '\+0\+0 +'
# 							   '(?P<flash_x>[+-]\d+)(?P<flash_y>[+-]\d+)')
# 	for line in shell('xwininfo', '-id', str(winid), '-tree'):
# 		flash_match = flash_matcher.match(line)
# 		if flash_match is not None:
# 			flash_w = int(flash_match.group('flash_w'))
# 			flash_h = int(flash_match.group('flash_h'))
# 			flash_x = int(flash_match.group('flash_x'))
# 			flash_y = int(flash_match.group('flash_y'))

# 	return name, x, y, flash_x, flash_y, flash_w, flash_h

# def find_fullscreen_windows():
# 	"""Class as fullscreen if:
# 	xwininfo "Relative upper-left X:  0" and
# 	xwininfo "Relative upper-left Y:  0" and
# 	(xwininfo name is "MPlayer" or
# 	 xwininfo name contains "Firefox" but is not equal to "Firefox")
# 	 """

# 	x_str = "Relative upper-left X:  0"
# 	y_str = "Relative upper-left Y:  0"
# 	name_matcher = re.compile(r'xwininfo: Window id: [0-9a-fx]+ "(?P<name>.*)"')

# 	for winid in shell('xdotool', 'search', '-name', '.'):
# 		print 'scan ', winid
# 		got_x = False
# 		got_y = False
# 		name = None
# 		for line in shell('xwininfo', '-id', winid):
# 			# print 'LINE ', line
# 			if x_str in line:
# 				# print '  got_x'
# 				got_x = True

# 			elif y_str in line:
# 				# print '  got_y'
# 				got_y = True

# 			name_match = name_matcher.match(line)
# 			if name_match is not None:
# 				name = name_match.group('name')
# 				# print '  name ', name
# 				if 'MPlayer' not in name and 'Firefox' not in name:
# 					name = None
# 					break

# 				if 'Firefox' in name and name == 'Firefox':
# 					name = None
# 					break

# 		if name is not None and got_x is True and got_y is True:
# 			print 'Potential, ', win_info(winid)


# def flash_info(win_id):
# 	"""Examine `win_id` to see if it looks like a browser containing Flash video.
# 	Returns a tuple of x, y, width, height, win_id if found.
# 	"""

# 	flash_matchers = (
# 		re.compile(r'.*"<unknown>"\) +'
# 			   '(?P<flash_w>\d+)x(?P<flash_h>\d+)'
# 			   '\+0\+0 +'
# 			   '(?P<flash_x>[+-]\d+)(?P<flash_y>[+-]\d+)'),
# 		re.compile(r'.*"Plugin-container"\) +'
# 			   '(?P<flash_w>\d+)x(?P<flash_h>\d+)'
# 			   '\+0\+0 +'
# 			   '(?P<flash_x>[+-]\d+)(?P<flash_y>[+-]\d+)'),
# 		re.compile(r'.*"Exe"\) +'
# 			   '(?P<flash_w>\d+)x(?P<flash_h>\d+)'
# 			   '\+0\+0 +'
# 			   '(?P<flash_x>[+-]\d+)(?P<flash_y>[+-]\d+)'),
# 		)
# 	flash_w = None
# 	flash_h = None
# 	flash_x = None
# 	flash_y = None
# 	for line in shell('xwininfo', '-id', str(win_id), '-tree'):
# 		for f in flash_matchers:
# 			m = f.match(line)
# 			if m is not None:
# 				flash_w = int(m.group('flash_w'))
# 				flash_h = int(m.group('flash_h'))
# 				flash_x = int(m.group('flash_x'))
# 				flash_y = int(m.group('flash_y'))
# 				break

# 	return {'id': win_id,
# 		'x': flash_x,
# 		'y': flash_y,
# 		'w': flash_w,
# 		'h': flash_h}

# Information about configured flash players

# x offsets are upwards from the bottom of the window,
# unless negative in which case they are downwards from the top
# y offsets are rightwards from the left of the window,
# unless negative in which case they are leftwards from the right
# flash_types = {'youtube': {'name': 'YouTube',
# 						   'pause': {'unit': 'px', 'x': 10, 'y': 10},
# 						   'fullscreen': {'unit': 'px', 'x': -10, 'y': 10},
# 						   'priority': 1},
# 			   'iplayer': {'name': 'BBC iPlayer',
# 						   'pause': {'unit': 'px', 'x': 10, 'y': 10},
# 						   'fullscreen': {'unit': 'px', 'x': 270, 'y': 40},
# 						   'priority': 1},
# 			   'chromium_fs': {'name': 'Chromium fullscreen',
# 							  'pause': {'unit': 'px', 'x': 10, 'y': 10},
# 							  'fullscreen': {'keypress': 'Escape'},
# 							  # 'fullscreen': {'unit': '%', 'x': 47, 'y': 6},
# 							  'priority': 2},
# 			   'firefox_fs': {'name': 'Firefox fullscreen',
# 							  'pause': {'unit': 'px', 'x': 10, 'y': 10},
# 							  'fullscreen': {'keypress': 'Escape'},
# 							  # 'fullscreen': {'unit': '%', 'x': 47, 'y': 6},
# 							  'priority': 2},
# 			   }


# def find_flash_windows():
# 	"""Find windows where xwininfo name contains "Firefox" but is not equal to "Firefox"
# 	and return win_info().
# 	Returns:
# 	- name
# 	- x
# 	- y
# 	- width
# 	- height
# 	- win_id
# 	- type
# 	 """

# 	name_matcher = re.compile(r'xwininfo: Window id: [0-9a-fx]+ "(?P<name>.*)"')

# 	root = win_info('root')

# 	res = []
# 	for win_id in shell('xdotool', 'search', '-name', '.'):
# 		for line in shell('xwininfo', '-id', win_id):
# 			name_match = name_matcher.match(line)
# 			if name_match is not None:
# 				name = name_match.group('name')
# 				# logging.debug('Id {wid} name {name}'.format(wid=win_id, name=name))
# 				# if 'Firefox' not in name or name == 'Firefox':
# 					# break

# 				if 'BBC iPlayer' in name or 'YouTube' in name:
# 					info = flash_info(win_id)
# 					logging.debug('  flash media info ' + str(info))
# 					# check that flash_info thinks it found something
# 					if info['x']:
# 						# look at the window title to guess player type
# 						if 'BBC iPlayer' in name:
# 							info['player'] = flash_types['iplayer']
# 						elif 'YouTube' in name:
# 							info['player'] = flash_types['youtube']

# 						# detected a flash subwindow
# 						res.append(info)

# 				elif name == 'exe':
# 					# Full screen player for chromium (iplayer and youtube)
# 					info = win_info(win_id)
# 					if info['w'] == root['w'] and info['h'] == root['h']:
# 						info['player'] = flash_types['chromium_fs']
# 						res.append(info)

# 				elif name == 'plugin-container':
# 					# Full screen player, firefox
# 					# print win_info(win_id)
# 					# print shell('xwininfo', '-id', str(win_id), '-tree')
# 					info = win_info(win_id)
# 					if info['w'] == root['w'] and info['h'] == root['h']:
# 						info['player'] = flash_types['firefox_fs']
# 						res.append(info)

# 	return res


# def find_top_flash_window():
# 	"""Return info about furthest forward window with flash video playing"""
# 	flashes = find_flash_windows()

# 	have_fs = False
# 	for f in flashes:
# 		if f['player']['priority'] == 2:
# 			have_fs = True

# 	if have_fs:
# 		flashes = [f for f in flashes if f['player']['priority'] == 2]

# 	if len(flashes) > 0:
# 		return flashes[-1]

# 	else:
# 		return None


# def autocommand(command):
# 	"""Click `command` on the first youtube/iplayer widget found.
# 	If successful return None, else an error string.
# 	"""

# 	f = find_top_flash_window()
# 	if f is None:
# 		return "No flash player found"

# 	if command not in f['player']:
# 		return 'Command not supported for player ' + f['player']['name']

# 	p = f['player'][command]  # player info for 'play' operation

# 	if 'keypress' in p:
# 		shell('xdotool', 'key', p['keypress'])
# 		return

# 	if p['unit'] == 'px':
# 		if p['x'] > 0:
# 			x = p['x']
# 		else:
# 			x = f['w'] + p['x']

# 		y = p['y']

# 	elif p['unit'] == '%':
# 		x = f['w'] * p['x'] / 100
# 		y = f['h'] * p['y'] / 100

# 	click_x = f['x'] + x
# 	click_y = f['y'] + f['h'] - y
# 	shell('xdotool', 'mousemove', str(click_x), str(click_y), 'click', '1')


# def autopause():
# 	"""Locate flash window and send a click to the pause button."""
# 	return autocommand('pause')


# def autofullscreen():
# 	"""Locate the topmost window with a flash control and send a click to the pause button."""
# 	return autocommand('fullscreen')


def get_mouse_pos():
	"""Return a tuple of mouse x,y position, as strings.
	"""

	matcher = re.compile(r'x:(?P<x>\d+) y:(?P<y>\d+) screen:')
	for line in shell('xdotool', 'getmouselocation'):
		match = matcher.match(line)
		if match is not None:
			return match.group('x'), match.group('y')

	return None

if __name__ == '__main__':
	from carrie import log
	log.init_log()
	import argparse
	parser = argparse.ArgumentParser()
	parser.add_argument('--autopause',
						action='store_true')
	parser.add_argument('--autofullscreen', '--autofs',
						action='store_true')
	parser.add_argument('--tree',
						action='store_true')
	parser.add_argument('--players',
						action='store_true')
	args = parser.parse_args()

	fifo.init_fifo()

	if args.tree:
		raw_list()
		parser.exit()

	if args.players:
		player_list(print_player=True, print_match=True)
		parser.exit()

	if args.autopause:
		print auto_command('pause')
		parser.exit()

	if args.autofullscreen:
		print auto_command('fullscreen')
		parser.exit()
