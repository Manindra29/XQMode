XQMode v0.3 alpha for Processing - ReadMe
----------------------------------------------------------------------------------------------------------------------------------

XQMode requires Processing 2.0a8 or later. Java version 1.6 Update 33 and upwards is recommended. At this moment, XQMode 
will need some modifications to your Processing app in order to run. It’s suggested that you make a backup 
of your Processing app before proceeding further.

----------------------------------------------------------------------------------------------------------------------------------
Installation steps:

Windows

1. Create a backup/copy of your Processing folder,i.e, the folder where processing.exe is located.
2. Copy the XQMode folder to <Your Processing Sketchbook>/modes folder. Create a folder named 'modes' if there isn’t one.
3. Copy xqmodeData into the Processing folder,i.e, the folder where processing.exe is located.
4. Open the xqmodeData folder. Double-click on Modify_Processing_Windows.bat.
If you get access denied errors, rename your default processing.exe to processing_original.exe and then copy the 
processing.exe located inside xqmodeData to the Processing folder. The script tries to do this automatically, 
but the processing folder is usually read only.
5. Run Processing. Switch to XQMode.

----------------------------------------------------------------------------------------------------------------------------------

Mac OS X

Note: By default, OS X Mountain Lion (10.8 and above) cannot launch 3rd party non-Mac App Store apps. So, in order to run the script,
check 'Allow From Anywhere' in your System Preferences > Security settings. You can change it back to default after running the
script.

1. Create a backup/copy of your Processing.app
2. Copy xqmodeData into Processing.app's parent folder, i.e, the folder inside which Processing.app is located.
3. Open the xqmodeData folder. Double-click on Modify_Processing_MacOSX.command.
4. Copy the XQMode folder to <Your Processing Sketchbook>/modes folder. Create a folder named 'modes' if there isn’t one.
5. Run Processing. Switch to XQMode.

----------------------------------------------------------------------------------------------------------------------------------

Linux

1. Create a backup/copy of your Processing folder
2. Copy all files inside xqmodeData/lib folder into processing-2.0a8/lib folder.
3. Move ecj.jar from processing-2.0a8/lib folder into processing-2.0a8 folder.
4. Now copy the XQMode to <Your Processing Sketchbook>/modes folder. Create a folder named 'modes' if there isn’t one.
5. Run Processing. Switch to XQMode.

----------------------------------------------------------------------------------------------------------------------------------

What these scripts do is copy the additional jar files into the lib folder and then update the classpath of processing
app. On Windows, this requires building a new .exe so you need to replace your processing.exe with the custom built one. 

On Mac OS X, editing the classpath involves editing the Info.plist file of the app. The original Info.plist file is 
renamed and is replaced by a modified Info.plist file. Additional jar files are copied to Processing.app/Contents/Resources/Java

Linux systems don't need any extra changes other than copying and moving the jar files.

----------------------------------------------------------------------------------------------------------------------------------

If you've any questions, bugs or problems, please post it as an Issue at XQMode's github repo or get in touch with us!

XQMode's git repo: www.github.com/Manindra29/XQMode

Manindra Moharana (www.mkmoharana.com)
Daniel Shiffman (www.shiffman.net)

Last updated: 16th August, 2012
