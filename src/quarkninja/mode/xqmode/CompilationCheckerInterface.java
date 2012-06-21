package quarkninja.mode.xqmode;

import org.eclipse.jdt.core.compiler.IProblem;

public interface CompilationCheckerInterface {
	public IProblem[] getErrors(String sourceName, String source);
	public void display();
}
