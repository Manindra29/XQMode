package quarkninja.mode.xqmode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;
import processing.app.*;
import processing.core.*;

/**
 * Syntax Checking Service for XQMode
 */
public class SyntaxCheckerService implements Runnable {

	static public String PATH = "E:/TestStuff/EarthQuake_Map.java";
	public static final int sleepTime = 2000;
	
	private ASTParser parser;
	public Editor editor;
	private boolean stopThread = false;
	

	public static void main(String[] args) {
		(new SyntaxCheckerService()).checkCode();
	}

	public SyntaxCheckerService() {
		parser = ASTParser.newParser(AST.JLS4);
	}

	public SyntaxCheckerService(String path) {
		PATH = path;
		parser = ASTParser.newParser(AST.JLS4);
	}

	/**
	 * Perform syntax check
	 * 
	 * @return true - only if no syntax errors found.
	 */
	public boolean checkCode() {

		String source = "";
		try {
			// if (editor != null)
			source = preprocessCode();
			// else
			// source = readFile(PATH);

			parser.setSource(source.toCharArray());
			parser.setKind(ASTParser.K_COMPILATION_UNIT);

			@SuppressWarnings("unchecked")
			Map<String, String> options = JavaCore.getOptions();

			JavaCore.setComplianceOptions(JavaCore.VERSION_1_6, options);
			parser.setCompilerOptions(options);
			CompilationUnit cu = (CompilationUnit) parser.createAST(null);

			IProblem[] problems = cu.getProblems();
			if (problems.length == 0)
				System.out.println("No syntax errors.");
			else {
				System.out.println("Syntax errors: ");
				for (int i = 0; i < problems.length; i++) {
					System.out.println(problems[i].getMessage()
							+ " | Line no. "
							+ (problems[i].getSourceLineNumber() - 1)); // Class
																		// offset
																		// is
																		// 1 for
																		// now
				}
			}
			return true;
		} catch (Exception e) {
			System.out.println("Oops! [SyntaxCheckerThreaded.checkCode]: " + e);
			// e.printStackTrace();
		}
		return false;
	}

	@Override
	public void run() {
		stopThread = false;
		while (!stopThread) {

			try {
				checkCode();
				// Check every 2 seconds
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			} catch (Exception e) {
				System.out.println("Oops! [SyntaxCheckerThreaded]: " + e);
				// e.printStackTrace();
			}

		}
	}

	public void stopThread() {
		stopThread = true;
	}

	// Preprocess Pde code into pure java
	private String preprocessCode() {

		// Super wicked regular expressions! (Used from Processing source)
		final String importRegexp = "(?:^|;)\\s*(import\\s+)((?:static\\s+)?\\S+)(\\s*;)";
		final Pattern FUNCTION_DECL = Pattern.compile(
				"(^|;)\\s*((public|private|protected|final|static)\\s+)*"
						+ "(void|int|float|double|String|char|byte)"
						+ "(\\s*\\[\\s*\\])?\\s+[a-zA-Z0-9]+\\s*\\(",
				Pattern.MULTILINE);
		String sourceAlt = "";

		// Handle code input from editor/java file
		try {
			if (editor == null) {
				System.out.println("Reading .java file: " + PATH);
			} else {
				StringBuffer bigCode = new StringBuffer();
				int bigCount = 0;
				for (SketchCode sc : editor.getSketch().getCode()) {
					if (sc.isExtension("pde")) {
						sc.setPreprocOffset(bigCount);

						try {

							if (editor.getSketch().getCurrentCode().equals(sc))
								bigCode.append(sc.getDocument().getText(0,
										sc.getDocument().getLength()));
							else {
								bigCode.append(sc.getProgram());

							}
							bigCode.append('\n');
						} catch (Exception e) {
							System.err
									.println("Exception in preprocessCode() - bigCode "
											+ e.toString());
						}
						bigCode.append('\n');
						bigCount += sc.getLineCount();
					}
				}

				sourceAlt = bigCode.toString();
				// System.out.println("Obtaining source from editor.");
			}
		} catch (Exception e) {

			System.out.println("Exception in preprocessCode()");

		}

		// Replace comments with whitespaces
		// sourceAlt = scrubComments(sourceAlt);

		// Find all int(*), replace with (int)(*)

		// \bint\s*\(\s*\b , i.e all exclusive "int("

		String dataTypeFunc[] = { "int", "char", "float", "boolean", "byte" };
		for (String dataType : dataTypeFunc) {
			String dataTypeRegexp = "\\b" + dataType + "\\s*\\(";
			Pattern pattern = Pattern.compile(dataTypeRegexp);
			Matcher matcher = pattern.matcher(sourceAlt);

			// while (matcher.find()) {
			// System.out.print("Start index: " + matcher.start());
			// System.out.println(" End index: " + matcher.end() + " ");
			// System.out.println("-->" + matcher.group() + "<--");
			// }
			sourceAlt = matcher.replaceAll("(" + dataType + ")(");

		}

		// Find all #[web color] and replace with 0xff[webcolor]
		// Should be 6 digits only.
		String webColorRegexp = "#{1}[A-F|a-f|0-9]{6}\\W";
		Pattern webPattern = Pattern.compile(webColorRegexp);
		Matcher webMatcher = webPattern.matcher(sourceAlt);
		while (webMatcher.find()) {
			// System.out.println("Found at: " + webMatcher.start());
			String found = sourceAlt.substring(webMatcher.start(),
					webMatcher.end());
			// System.out.println("-> " + found);
			sourceAlt = webMatcher.replaceFirst("0xff" + found.substring(1));
			webMatcher = webPattern.matcher(sourceAlt);
		}

		// Find all import statements and remove them, add them to import list
		ArrayList<String> programImports = new ArrayList<String>();
		//
		do {
			// System.out.println("-->\n" + sourceAlt + "\n<--");
			String[] pieces = PApplet.match(sourceAlt, importRegexp);

			// Stop the loop if we've removed all the import lines
			if (pieces == null)
				break;

			String piece = pieces[1] + pieces[2] + pieces[3];
			int len = piece.length(); // how much to trim out

			programImports.add(piece); // the package name
			// System.out.println("Import -> " + piece);

			// find index of this import in the program
			int idx = sourceAlt.indexOf(piece);

			// Remove the import from the main program
			String whiteSpace = "";
			for (int j = 0; j < piece.length(); j++) {
				whiteSpace += " ";
			}
			sourceAlt = sourceAlt.substring(0, idx) + whiteSpace
					+ sourceAlt.substring(idx + len);

		} while (true);

		String className = (editor == null) ? "DefaultClass" : editor
				.getSketch().getName();

		// Check whether the code is being written in BASIC mode(no function
		// declarations) - append class declaration and void setup() declaration
		Matcher matcher = FUNCTION_DECL.matcher(sourceAlt);
		if (!matcher.find()) {
			sourceAlt = "public class " + className + " extends PApplet {\n"
					+ "public void setup() {\n" + sourceAlt
					+ "\nnoLoop();\n}\n" + "\n}\n";

		} else
			sourceAlt = "public class " + className + " extends PApplet {\n"
					+ sourceAlt + "\n}\n";

		// Convert non ascii characters
		// sourceAlt = substituteUnicode(sourceAlt);

		// System.out.println("-->\n" + sourceAlt + "\n<--");
		System.out.println("PDE code processed - "
				+ editor.getSketch().getName());

		return sourceAlt;
	}

	public static String readFile(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(file)));
		try {
			StringBuilder ret = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				ret.append(line);
				ret.append("\n");
			}
			return ret.toString();
		} finally {
			reader.close();
		}
	}

	public static String readFile(String path) throws IOException {
		return readFile(new File(path));
	}

}
