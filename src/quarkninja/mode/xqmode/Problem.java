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

import org.eclipse.jdt.core.compiler.IProblem;

/**
 * Wrapper class for IProblem.
 * 
 * Stores the tabIndex and line number according to its tab, including the
 * original IProblem object
 * 
 * @author Manindra Moharana &lt;mkmoharana29@gmail.com&gt;
 * 
 */
public class Problem {
	/**
	 * The IProblem which is being wrapped
	 */
	private IProblem iProblem;
	/**
	 * The tab number to which the error belongs to
	 */
	public int tabIndex; 
	/**
	 * Line number(pde code) of the error
	 */
	public int lineNumber;
	
	/**
	 * Error Message. Processed form of IProblem.getMessage()
	 */
	public String message;
	
	public int type;

	public static final int ERROR = 1, WARNING = 2;
	
	public Problem(IProblem iProblem, int tabIndex, int lineNumber) {
		this.iProblem = iProblem;
		if(iProblem.isError())
			type = ERROR;
		if(iProblem.isWarning())
			type = WARNING;
		this.tabIndex = tabIndex;
		this.lineNumber = lineNumber;
		this.message = ProblemsFilter.process(iProblem);
	}
	
	public String display() {
		return new String("Tab" + tabIndex + ",LN" + lineNumber + "PROB:"
				+ message);
	}
	
	public boolean isError(){
		return type == ERROR;
	}
	
	public boolean isWarning(){
		return type == WARNING;
	}
	
	public String getMessage(){
		return message;
	}
	
	public IProblem getIProblem(){
		return iProblem;
	}
	
	public void setType(int ProblemType){
		if(ProblemType == ERROR)
			type = ERROR;
		else if(ProblemType == WARNING)
			type = WARNING;
		else throw new IllegalArgumentException("Illegal Problem type passed to Problem.setType(int)");
	}
	
}
