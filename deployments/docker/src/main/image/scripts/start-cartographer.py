#!/usr/bin/python
#
# Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


from __future__ import absolute_import
from __future__ import print_function
import os
import sys

# import tarfile
import shutil
import fnmatch

try:
    # Python 2
    from urllib2 import urlopen
except ImportError:
    # Python 3
    from urllib.request import urlopen

def run(cmd, fail_message='Error running command', fail=True):
  cmd += " 2>&1"
  print(cmd)
  ret = os.system(cmd)
  if fail is True and ret != 0:
    print("%s (failed with code: %s)" % (fail_message, ret))
    sys.exit(ret)



def runIn(cmd, workdir, fail_message='Error running command', fail=True):
  cmd += " 2>&1"
  olddir = os.getcwd()
  os.chdir(workdir)

  print("In: %s, executing: %s" % (workdir, cmd))

  ret = os.system(cmd)
  if fail is True and ret != 0:
    print("%s (failed with code: %s)" % (fail_message, ret))
    sys.exit(ret)
  
  os.chdir(olddir)



def move_and_link(src, target, replaceIfExists=False):
  srcParent = os.path.dirname(src)
  if not os.path.isdir(srcParent):
    print("mkdir -p %s" % srcParent)
    os.makedirs(srcParent)
  
  if not os.path.isdir(target):
    print("mkdir -p %s" % target)
    os.makedirs(target)
  
  if os.path.isdir(src):
    for f in os.listdir(src):
      targetFile = os.path.join(target, f)
      srcFile = os.path.join(src, f)
      print("%s => %s" % (srcFile, targetFile))
      if os.path.exists(targetFile):
        if not replaceIfExists:
          print("Target dir exists: %s. NOT replacing." % targetFile)
          continue
        else:
          print("Target dir exists: %s. Replacing." % targetFile)

        if os.path.isdir(targetFile):
          print("rm -r %s" % targetFile)
          shutil.rmtree(targetFile)
        else:
          print("rm %s" % targetFile)
          os.remove(targetFile)
      
      if os.path.isdir(srcFile):
        print("cp -r %s %s" % (srcFile, targetFile))
        shutil.copytree(srcFile, targetFile)
      else:
        print("cp %s %s" % (srcFile, targetFile))
        shutil.copy(srcFile, targetFile)

    print("rm -r %s" % src)
    shutil.rmtree(src)

  print("ln -s %s %s" % (target, src))
  os.symlink(target, src)




SSH_CONFIG_VOL = '/tmp/ssh-config'

ETC_URL_ENVAR = 'ETC_URL'
ETC_BRANCH_ENVAR = 'ETC_BRANCH'
ETC_SUBPATH_ENVAR = 'ETC_SUBPATH'

OPTS_ENVAR = 'OPTS'


# locations for expanded indy binary
DIR = '/opt/cartographer'
BOOT_PROPS = 'boot.properties'
BIN = os.path.join(DIR, 'bin')
ETC = os.path.join(DIR, 'etc/cartographer')
CACHE = os.path.join(DIR, 'var/lib/cartographer/cache')
WORK = os.path.join(DIR, 'var/lib/cartographer/work')
DATA = os.path.join(DIR, 'var/lib/cartographer/data')
LOGS = os.path.join(DIR, 'var/log/cartographer')


# locations on global fs
ETC_CARTO = '/etc/cartographer'
VAR_CARTO = '/var/lib/cartographer'
VAR_CACHE = os.path.join(VAR_CARTO, 'cache')
VAR_WORK = os.path.join(VAR_CARTO, 'work')
VAR_DATA = os.path.join(VAR_CARTO, 'data')
LOGS = '/var/log/cartographer'


# Git location supplying /opt/cartographer/etc/cartographer
etcUrl = os.environ.get(ETC_URL_ENVAR)
etcBranch = os.environ.get(ETC_BRANCH_ENVAR) or 'master'
etcSubpath = os.environ.get(ETC_SUBPATH_ENVAR)

# command-line options for indy
opts = os.environ.get(OPTS_ENVAR) or ''

print("Read environment:\n  etc Git URL: %s\n  CLI opts: %s" % (etcUrl, opts))

if os.path.isdir(SSH_CONFIG_VOL) and len(os.listdir(SSH_CONFIG_VOL)) > 0:
  print("Importing SSH configurations from volume: %s" % SSH_CONFIG_VOL)
  run("cp -vrf %s /root/.ssh" % SSH_CONFIG_VOL)
  run("chmod -v 700 /root/.ssh", fail=False)
  run("chmod -v 600 /root/.ssh/*", fail=False)


if os.path.isdir(DIR) is False:
  print("Cannot start, %s does not exist!" % DIR)
  exit(1)

if etcUrl is not None:
  if os.path.isdir(ETC):
    print("clearing pre-existing etc directory")
    shutil.rmtree(ETC)

  print("Cloning: %s" % etcUrl)
  run("git clone --branch %s --verbose --progress %s %s 2>&1" % (etcBranch, etcUrl, ETC), "Failed to checkout %s branch of etc from: %s" % (etcBranch, etcUrl))
  if etcSubpath is not None and etcSubpath != '.':
    runIn("git read-tree -um --aggressive `git write-tree`: HEAD:%s" % etcSubpath, ETC, "Failed to relocate %s subpath to %s", (etcSubpath, ETC))
  
move_and_link(ETC, ETC_CARTO, replaceIfExists=True)
move_and_link(CACHE, VAR_CACHE)
move_and_link(WORK, VAR_WORK)
move_and_link(DATA, VAR_DATA)
move_and_link(LOGS, LOGS)


run("%s %s" % (os.path.join(DIR, 'bin', 'start.sh'), opts), fail=False)
