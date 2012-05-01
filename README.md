========================================================================


									XQMode


========================================================================


It's coming. Stay tuned.


Contributors:
* Daniel Shiffman
* Manindra Moharana

Instructions:

* You need Processing 2.0a5 or later, we built trunk from latest SVN.
* You need Apache Ant to build.

* Set properties in build.xml    
    * Set the path to your processing jars (core.jar, pde.jar)
    * If you want to automatically install your mode (target: install ), set the path to your modes folder (typically a folder named "modes" inside your sketchbook folder)
    * If you want to run processing after building (target: run), set the path to your processing.exe's directory.

* Ant Targets:
    * build: builds your mode, creates a folder containing the mode in "dist". This can be put into the modes folder inside your sketchbook.
    * install: builds your mode and copies it to your modes folder.
    * run: builds and installs your mode, then runs processing.

Processing Mode Template by Martin Leopold.
======