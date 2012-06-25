package quarkninja.mode.xqmode;

import org.eclipse.jdt.core.compiler.IProblem;

/**
 * Wrapper class for IProblem.
 * 
 * Stores the tabIndex and line number according to its tab, including the
 * original IProblem object
 * 
 * @author Manindra Moharana
 * 
 */
public class Problem {
	public IProblem iProblem;
	public int tabIndex, lineNumber;

	public Problem(IProblem iProblem, int tabIndex, int lineNumber) {
		this.iProblem = iProblem;
		this.tabIndex = tabIndex;
		this.lineNumber = lineNumber;
	}

	public String display() {
		return new String("Tab" + tabIndex + ",LN" + lineNumber + "PROB:"
				+ iProblem.getMessage());
	}
}
