REM Processing Modifier for XQMode - Windows.
REM By - Manindra Moharana
REM 02-07-2012
REM http://github.com/Manindra29/XQMode

cd /d %~dp0
cd ..
echo Processing Modifier for XQMode (Windows)
echo Copying required jar files
copy xqmodeData\lib\*.jar lib
echo Replacing processing.exe with modified one. Original file retained as processing_original.exe.
ren processing.exe processing_original.exe
copy xqmodeData\processing.exe
echo Done.
pause