#!/usr/bin/env python

import os

if 'PYTHONDONTWRITEBYTECODE' in os.environ:
	del os.environ['PYTHONDONTWRITEBYTECODE']

import distribute_setup
distribute_setup.use_setuptools()

#try:
#from setuptools import setup, find_packages
#from setuptools.command.build import build
#except ImportError:
#    from ez_setup import use_setuptools
#    use_setuptools()
from setuptools import setup, find_packages

# tests_require = [
    # 'flask',
# ]

# Utility function to read the README file.
# Used for the long_description.  It's nice, because now 1) we have a top level
# README file and 2) it's easier to type in the README file than to put a raw
# string in below ...
def read(fname):
    return open(os.path.join(os.path.dirname(__file__), fname)).read()

setup(
    name='carrie',
    version='0.2',
    author='Mike Elson',
    author_email='mike.elson@gmail.com',
    url='http://github.com/mjem/carrie',
    description='Remote control of media players via web or Android phone',
	license='GPL',
	# license="GPLv3",
	keywords="mplayer youtube iplayer android",
	packages=['carrie'],
	#scripts=['bin/carrie'],
	entry_points={
		'console_scripts': [
			'carrie=carrie.cmd:carrie_main',
			'stop-screensaver=carrie.cmd:stop_screensaver_main',
			]},
    # packages=find_packages(exclude="example_project"),
    # zip_safe=False,
    install_requires=[
        'flask>=0.6.1-1',
    ],
    # dependency_links=[
    # 'https://github.com/disqus/django-haystack/tarball/master#egg=django-haystack',
	# ],
	# tests_require=tests_require,
    # extras_require={'test': tests_require},
	# test_suite='sentry.runtests.runtests',
	#include_package_data=True,
	package_dir={'carrie': 'carrie'},
	# this doesn't seem to work, use MANIFEST.in instead
	# Files go into MANIFEST.in to get them in the distribution archives,
	# package_data to get them installed
	package_data={'carrie': ['static/*.js',
							 'static/*.css',
							 'templates/*.html',
							 'static/jquery-ui/js/*.js',
							 'static/jquery-ui/css/smoothness/*.css',
							 'static/jquery-ui/css/smoothness/images/*.png']},
	#data_files=[('carrie/static', ['index.js'])],
	long_description=read('README.md'),
	# 			  entry_points = {
    #     'console_scripts': [
    #         'foo = my_package.some_module:main_func',
    #         'bar = other_module:some_func',
    #     ],
    #     'gui_scripts': [
    #         'baz = my_package_gui.start_func',
    #     ]
    # }
    classifiers=[
        'Framework :: Django',
        'Topic :: Software Development',
		'License :: OSI Approved :: GNU General Public License (GPL)',
		"Intended Audience :: End Users/Desktop",
		"Operating System :: POSIX :: Linux",
		"Programming Language :: Python :: 2.7",
		"Topic :: Internet :: WWW/HTTP",
		"Topic :: Multimedia :: Sound/Audio",
    ],
)
