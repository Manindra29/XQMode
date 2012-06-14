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
	public int tabIndex;
	
	public Problem(IProblem iProblem, int tanIndex) {
		this.iProblem = iProblem;
		this.tabIndex = tanIndex;
	}
}
