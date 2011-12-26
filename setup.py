#!/usr/bin/env python

import os

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
    version='0.2dev',
    author='Mike Elson',
    author_email='mike.elson@gmail.com',
    url='http://github.com/mjem/carrie',
    description='Remote control of media players via web or Android phone',
	license='GPLv3',
	# license="GPLv3",
	keywords="mplayer youtube iplayer android",
	packages=['carrie'],
	scripts=['bin/carrie'],
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
	package_data={'carrie': ['static/*.html', '*.css', '*.js', '*.png']},
	#data_files=...
	long_description=read('README'),
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
		"License :: OSI Approved :: GPLv3 License",
		"Intended Audience :: End Users/Desktop",
		"Operating System :: POSIX :: Linux",
		"Programming Language :: Python :: 2.7",
		"Topic :: Internet :: WWW/HTTP",
		"Topic :: Multimedia :: Sound/Audio",
    ],
)
