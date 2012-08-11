#!/bin/bash

# Processing Modifier for XQMode - Linux
# By - Manindra Moharana
# 02-07-2012
# http://github.com/Manindra29/XQMode

echo "Processing Modifier for XQMode (Linux)"
SCRIPT_PATH="${BASH_SOURCE[0]}";
if ([ -h "${SCRIPT_PATH}" ]) then
  while([ -h "${SCRIPT_PATH}" ]) do cd `dirname "$SCRIPT_PATH"`; SCRIPT_PATH=`readlink "${SCRIPT_PATH}"`; done
fi
cd `dirname ${SCRIPT_PATH}` > /dev/null
echo ${SCRIPT_PATH}
cd ..
echo "Copying required jar files"
#echo "Copying JDT-Core files to Processing.app\Contents\Resources\Java" 
cp "xqmodeData"/lib/*.jar lib
echo Done.
