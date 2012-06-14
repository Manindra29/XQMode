package quarkninja.mode.xqmode;

import org.eclipse.jdt.core.compiler.IProblem;

/**
 * Wrapper class for IProblem
 * 
 * @author QuarkNinja
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
}
