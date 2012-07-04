#!/bin/sh

# Processing Modifier for XQMode - Mac OS X.
# By - Manindra Moharana
# 02-07-2012
# http://github.com/Manindra29/XQMode

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo $DIR
cd $DIR
cd ..
echo "Processing Modifier for XQMode (Mac OS X)"
echo "Copying required jar files to Processing.app\Contents\Resources\Java" 
cp xqmodeData/lib/*.jar Processing.app/Contents/Resources/Java
echo "Updating Processing.app\Contents\Info.plist. Original file retained as Processing.app\Contents\Info_Original.plist"
mv Processing.app/Contents/Info.plist Processing.app/Contents/Info_Original.plist
cp xqmodeData/Info.plist Processing.app/Contents/
echo Done.