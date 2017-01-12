#!/bin/bash
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


test -f /etc/profile && source /etc/profile
test -f $HOME/.bash_profile &&source $HOME/.bash_profile

THIS=$(cd ${0%/*} && echo $PWD/${0##*/})
# THIS=`realpath ${0}`
BASEDIR=`dirname ${THIS}`
BASEDIR=`dirname ${BASEDIR}`

echo "Cartographer basedir: ${BASEDIR}"

CARTO_LOCALLIB_DIR=${CARTO_LOCALLIB_DIR:-${BASEDIR}/lib/local}
CARTO_LOGCONF_DIR=${CARTO_LOGCONF_DIR:-${BASEDIR}/etc/cartographer/logging}

echo "Loading logging config from: ${CARTO_LOGCONF_DIR}"

CP="${CARTO_LOCALLIB_DIR}:${CARTO_LOGCONF_DIR}"
for f in $(find $BASEDIR/lib/*.jar -type f)
do
  CP=${CP}:${f}
done

for f in $(find $BASEDIR/lib/thirdparty -type f)
do
  CP=${CP}:${f}
done

# echo "Classpath: ${CP}"

JAVA=`which java`
$JAVA -version 2>&1 > /dev/null
if [ $? != 0 ]; then
  PATH=${JAVA_HOME}/bin:${PATH}
  JAVA=${JAVA_HOME}/bin/java
fi

CARTO_ENV=${CARTO_ENV:-${BASEDIR}/etc/cartographer/env.sh}
test -f ${CARTO_ENV} && source ${CARTO_ENV}

#JAVA_DEBUG_OPTS="-Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"
JAVA_OPTS="$JAVA_OPTS $JAVA_DEBUG_OPTS"

MAIN_CLASS=org.commonjava.cartographer.boot.Main

"$JAVA" ${JAVA_OPTS} -cp "${CP}" -Dcarto.home="${BASEDIR}" ${MAIN_CLASS} "$@"
ret=$?
if [ $ret == 0 -o $ret == 130 ]; then
  exit 0
else
  exit $ret
fi
