#!/usr/bin/python

import os
import argparse
import glob
import tarfile
import shutil
from subprocess import call

parser = argparse.ArgumentParser()
parser.add_argument('-E', '--existing', action='store_true', help="Use existing unpacked distro instead of unpacking again (if it exists)")
parser.add_argument('-e', '--etc', metavar="ETC_DIR", help="Copy the given etc directory into the distro before starting")
args=parser.parse_args()


BASE=os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR=os.path.join(BASE, 'deployments/standalone-rest/target')
DIST_PATTERN='cartographer-standalone-rest-*.tar.gz'

UNPACKED_DIR = os.path.join(OUT_DIR, "cartographer")

print "Unpacked dist directory: %s" % UNPACKED_DIR
if not os.path.isdir(UNPACKED_DIR) or not args.existing:
	if ( os.path.isdir(UNPACKED_DIR)):
		print "Removing pre-existing dist directory"
		shutil.rmtree(UNPACKED_DIR)

	pathPattern = os.path.join(OUT_DIR, DIST_PATTERN)
	matches = glob.glob(pathPattern)
	if len(matches) < 1:
		print "Cannot find distribution at: %s. Maybe you haven't built it?" % pathPattern
		exit(1)

	dist = matches[0]
	print "Using dist tarball: %s" % dist

	with tarfile.open(dist) as tar:
		print "Extracting dist tarball"
		tar.extractall(OUT_DIR)

if args.etc:
	print "Replacing default etc directory with contents of: %s" % args.etc
	etcDir = os.path.join(UNPACKED_DIR, 'etc/cartographer')
	shutil.rmtree(etcDir)
	shutil.copytree(args.etc, etcDir)

print "Launching Cartographer"
call([os.path.join(UNPACKED_DIR, 'bin/start.sh')])
