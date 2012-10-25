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

import java.awt.Color;
import java.io.File;

import processing.app.Base;
import processing.app.Editor;
import processing.app.EditorState;
import processing.app.Mode;
import processing.app.RunnerListener;
import processing.app.Sketch;
import processing.app.SketchException;
import processing.mode.java.JavaMode;
import processing.mode.java.runner.Runner;

/**
 * Teh XQMode
 * 
 * @author Manindra Moharana &lt;mkmoharana29@gmail.com&gt;
 */
public class XQMode extends JavaMode {

	public XQMode(Base base, File folder) {
		super(base, folder);
		
		// Needed so that core libraries like opengl, etc. are loaded.
		for (Mode m : base.getModeList()) {
			if (m.getClass() == JavaMode.class) {
				JavaMode jMode = (JavaMode) m;
				librariesFolder = jMode.getLibrariesFolder();
				rebuildLibraryList(); 
				break;
			}
		}

		// Fetch examples and reference from java mode
		examplesFolder = Base.getContentFile("modes/java/examples");
		referenceFolder = Base.getContentFile("modes/java/reference");

		System.out.println("XQMode initialized.");
	}

	public Editor createEditor(Base base, String path, EditorState state) {
		return new XQEditor(base, path, state, this);
	}

	/**
	 * Called by PDE
	 */
	@Override
	public String getTitle() {
		return "XQMode";
	}

	public Runner handleRun(Sketch sketch, RunnerListener listener)
			throws SketchException {
		XQJavaBuild build = new XQJavaBuild(sketch);
		String appletClassName = build.build(false);
		if (appletClassName != null) {
			final Runner runtime = new Runner(build, listener);
			new Thread(new Runnable() {
				public void run() {
					runtime.launch(false); // this blocks until finished
				}
			}).start();
			return runtime;
		}
		return null;
	}
	
	/**
     * Load a String value from theme.txt
     *
     * @param attribute the attribute key to load
     * @param defaultValue the default value
     * @return the attributes value, or the default value if the attribute
     * couldn't be loaded
     */
    public String loadStringFromTheme(String attribute, String defaultValue) {
        String newString = theme.get(attribute);
        if (newString != null) {
            return newString;
        }
        System.out.println("Error loading String from theme.txt: " + attribute);
        return defaultValue;
    }

    /**
     * Load a Color value from theme.txt
     *
     * @param attribute the attribute key to load
     * @param defaultValue the default value
     * @return the attributes value, or the default value if the attribute
     * couldn't be loaded
     */
    public Color loadColorFromTheme(String attribute, Color defaultValue) {
        Color newColor = theme.getColor(attribute);
        if (newColor != null) {
            return newColor;
        }
        System.out.println("error loading color theme.txt: " + attribute);
        return defaultValue;
    }

}
