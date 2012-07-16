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
/**
	 * Error markers displayed on the Error Bar.
	 * 
	 * @author Manindra Moharana &lt;mkmoharana29@gmail.com&gt;
	 * 
	 */
	public class ErrorMarker {
		public int y;
		public int type = -1;
		public static final int Error = 1;
		public static final int Warning = 2;
		public Problem problem;

		public ErrorMarker(Problem problem, int y, int type) {
			this.problem = problem;
			this.y = y;
			this.type = type;
		}
	}