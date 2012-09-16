/*
  Part of the XQMode project - https://github.com/Manindra29/XQMode
  
  Under Google Summer of Code 2012 - 
  http://www.google-melange.com/gsoc/homepage/google/gsoc2012
  
  Copyright (C) 2012 Manindra Moharana
	
  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package quarkninja.mode.xqmode;

import java.io.File;

import processing.app.Sketch;
import processing.app.SketchException;
import processing.mode.java.JavaBuild;

/**
 * This class extends JavaBuild so that XQCompiler is used instead of
 * processing.mode.java.Compiler.
 * 
 * @author Manindra Moharana &lt;mkmoharana29@gmail.com&gt;
 */
public class XQJavaBuild extends JavaBuild {

	public XQJavaBuild(Sketch sketch) {
		super(sketch);
	}

	public String build(File srcFolder, File binFolder, boolean sizeWarning)
			throws SketchException {
		this.srcFolder = srcFolder;
		this.binFolder = binFolder;

		// Base.openFolder(srcFolder);
		// Base.openFolder(binFolder);

		// run the preprocessor
		String classNameFound = preprocess(srcFolder, sizeWarning);

		// compile the program. errors will happen as a RunnerException
		// that will bubble up to whomever called build().
		// Compiler compiler = new Compiler(this);
		// String bootClasses = System.getProperty("sun.boot.class.path");
		// if (compiler.compile(this, srcFolder, binFolder, primaryClassName,
		// getClassPath(), bootClasses)) {
		if (XQCompiler.compile(this)) {
			sketchClassName = classNameFound;
			return classNameFound;
		}
		return null;
	}
}
