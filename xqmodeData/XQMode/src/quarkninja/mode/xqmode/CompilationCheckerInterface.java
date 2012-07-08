package quarkninja.mode.xqmode;

import org.eclipse.jdt.core.compiler.IProblem;
/**
 * Interface for Compiler Class
 * 
 * @author Manindra Moharana
 *
 */
public interface CompilationCheckerInterface {
	public IProblem[] getErrors(String sourceName, String source);
	public void display();
}
