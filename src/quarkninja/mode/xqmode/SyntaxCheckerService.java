package quarkninja.mode.xqmode;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.table.DefaultTableModel;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import processing.app.Base;
import processing.app.Editor;
import processing.app.SketchCode;
import processing.core.PApplet;

/**
 * Syntax Checking Service for XQMode
 */
public class SyntaxCheckerService implements Runnable {

	static public String PATH = "E:/TestStuff/hw1.java";
	public static final int sleepTime = 1000;

	private ASTParser parser;
	public Editor editor;
	private boolean stopThread = false;
	public ErrorWindow errorWindow;
	private IProblem[] problems;

	public static void main(String[] args) {
		(new SyntaxCheckerService()).checkCode();
	}

	public SyntaxCheckerService() {
		parser = ASTParser.newParser(AST.JLS4);
		initializeErrorWindow();
	}

	public SyntaxCheckerService(String path) {
		PATH = path;
		parser = ASTParser.newParser(AST.JLS4);
		initializeErrorWindow();
	}

	public SyntaxCheckerService(ErrorWindow erw) {
		parser = ASTParser.newParser(AST.JLS4);
		this.errorWindow = erw;
	}

	private String[] slashAnimation = { "|", "/", "--", "\\", "|", "/", "--", "\\" };
	private int slashAnimationIndex = 0;

	private void initializeErrorWindow() {
		if (errorWindow == null) {
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					try {

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			try {
				errorWindow = new ErrorWindow(editor);
				errorWindow.setVisible(true);
				if (editor != null) {
					errorWindow.setTitle("Problems - " + editor.getSketch().getName() + " "
							+ slashAnimation[slashAnimationIndex]);
				}
				// One thread sleeps while other continues running - the
				// troubles of multithreading. Sigh, no more NPEs.
				Thread.sleep(500);
			} catch (InterruptedException e1) {

				e1.printStackTrace();
			}
			if (editor == null) {
				errorWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			} else {
				errorWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			}
		}
	}

	/**
	 * Perform syntax check
	 * 
	 * @return true - only if no syntax errors found.
	 */
	public boolean checkCode() {

		String source = "";
		try {
			if (editor != null)
				source = preprocessCode();
			else
				source = readFile(PATH);

			parser.setSource(source.toCharArray());
			parser.setKind(ASTParser.K_COMPILATION_UNIT);

			@SuppressWarnings("unchecked")
			Map<String, String> options = JavaCore.getOptions();

			// Ben has decided to move on to 1.6. Yay!
			JavaCore.setComplianceOptions(JavaCore.VERSION_1_6, options);
			parser.setCompilerOptions(options);
			CompilationUnit cu = (CompilationUnit) parser.createAST(null);

			// TODO: Two sets of same data, is this the best approach?
			problems = cu.getProblems();
			if (errorWindow != null) {
				errorWindow.problemList = cu.getProblems();
				// for (int i = 0; i < problems.length; i++) {
				// errorWindow.problemList[i].setSourceStart(xyToOffset(
				// problems[i].getSourceLineNumber()-1, 0));
				// }
			}

			// if (problems.length == 0)
			// System.out.println("No syntax errors.");
			// else {
			// System.out.println("Syntax errors: ");
			// for (int i = 0; i < problems.length; i++) {
			// System.out.println(problems[i].getMessage()
			// + " | Line no. "
			// + (problems[i].getSourceLineNumber() - 1));
			// // Class offset is line 1 for now
			// }
			//
			//
			// }

			setErrorTable();
			return true;
		} catch (Exception e) {
			System.out.println("Oops! [SyntaxCheckerThreaded.checkCode]: " + e);
			e.printStackTrace();
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
			} catch (Exception e) {
				System.out.println("Oops! [SyntaxCheckerThreaded]: " + e);
				// e.printStackTrace();
			}

		}
	}

	public void stopThread() {
		stopThread = true;
		System.out.println("Syntax Checker Service stopped.");
	}

	// Preprocess Pde code into pure java
	private String preprocessCode() {

		String sourceAlt = "";

		if (editor == null) {
			try {
				sourceAlt = readFile(PATH);
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println(sourceAlt);
			System.out.println("-------------------------------------");
			return sourceAlt;
		}
		// Super wicked regular expressions! (Used from Processing source)
		final String importRegexp = "(?:^|;)\\s*(import\\s+)((?:static\\s+)?\\S+)(\\s*;)";
		final Pattern FUNCTION_DECL = Pattern.compile(
				"(^|;)\\s*((public|private|protected|final|static)\\s+)*"
						+ "(void|int|float|double|String|char|byte)"
						+ "(\\s*\\[\\s*\\])?\\s+[a-zA-Z0-9]+\\s*\\(", Pattern.MULTILINE);

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
								bigCode.append(sc.getDocument().getText(0, sc.getDocument().getLength()));
							else {
								bigCode.append(sc.getProgram());

							}
							bigCode.append('\n');
						} catch (Exception e) {
							System.err
									.println("Exception in preprocessCode() - bigCode " + e.toString());
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
			String found = sourceAlt.substring(webMatcher.start(), webMatcher.end());
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
			sourceAlt = sourceAlt.substring(0, idx) + whiteSpace + sourceAlt.substring(idx + len);

		} while (true);

		String className = (editor == null) ? "DefaultClass" : editor.getSketch().getName();

		// Check whether the code is being written in BASIC mode(no function
		// declarations) - append class declaration and void setup() declaration
		Matcher matcher = FUNCTION_DECL.matcher(sourceAlt);
		if (!matcher.find()) {
			sourceAlt = "public class " + className + " extends PApplet {\n" + "public void setup() {\n"
					+ sourceAlt + "\nnoLoop();\n}\n" + "\n}\n";

		} else
			sourceAlt = "public class " + className + " extends PApplet {\n" + sourceAlt + "\n}\n";

		// Convert non ascii characters
		// sourceAlt = substituteUnicode(sourceAlt);

		// System.out.println("-->\n" + sourceAlt + "\n<--");
		// System.out.println("PDE code processed - "
		// + editor.getSketch().getName());

		return sourceAlt;
	}

	private void setErrorTable() {
		String[][] errorData = new String[problems.length][2];
		for (int i = 0; i < problems.length; i++) {
			errorData[i][0] = problems[i].getMessage();
			errorData[i][1] = (problems[i].getSourceLineNumber() - 1) + "";
		}
		DefaultTableModel tm = new DefaultTableModel(errorData, ErrorWindow.columnNames);

		try {
			initializeErrorWindow();

			errorWindow.updateTable(tm);
		} catch (Exception e) {
			System.out.println("Exception at setErrorTable() " + e);
			e.printStackTrace();
			stopThread();
		}

		// A nifty rotating slash animation on the title bar to show that syntax
		// checker thread is running
		slashAnimationIndex++;
		if (slashAnimationIndex == slashAnimation.length)
			slashAnimationIndex = 0;
		if (editor != null) {
			errorWindow.setTitle("Problems - " + editor.getSketch().getName() + " "
					+ slashAnimation[slashAnimationIndex]);
		}
	}

	/**
	 * Converts a row no, column no. representation of cursor location to offset
	 * representation. Editor uses JTextArea internally which deals only with
	 * caret offset, not row no. and column no.
	 * 
	 * @param x
	 *            - row no.
	 * @param y
	 *            - column no.
	 * 
	 * @return int - Offset
	 */
	public static int xyToOffset(int x, int y, Editor editor) {

		String[] lines = {};// = PApplet.split(sourceString, '\n');
		int offset = 0;

		int codeIndex = 0;
		int bigCount = 0;
		for (SketchCode sc : editor.getSketch().getCode()) {
			if (sc.isExtension("pde")) {
				sc.setPreprocOffset(bigCount);

				try {
					int len = 0;
					if (editor.getSketch().getCurrentCode().equals(sc)) {
						lines = PApplet.split(sc.getDocument().getText(0, sc.getDocument().getLength()),
								'\n');
						// System.out.println("Getting from document "
						// + sc.getLineCount() + "," + lines.length);
						len = Base.countLines(sc.getDocument().getText(0, sc.getDocument().getLength())) + 1;
					} else {
						lines = PApplet.split(sc.getProgram(), '\n');
						len = Base.countLines(sc.getProgram()) + 1;
					}

					// Adding + 1 to len because \n gets appended for each
					// sketchcode extracted during processPDECode()
					if (x >= len) {
						x -= len;
					} else {
						// System.out.println(" x = " +
						// x +
						// "in tab: " +
						// editor.getSketch().getCode(codeIndex).getPrettyName());
						// if(errorWindow.hasFocus())
						editor.getSketch().setCurrentCode(codeIndex);
						break;
					}
					codeIndex++;

				} catch (Exception e) {
					System.out.println("Document Exception in xyToOffset");

					e.printStackTrace();

				}
				bigCount += sc.getLineCount();
			}

		}

		//

		// Count chars till the end of previous line(x-1), keeping in mind x
		// starts from 1
		// System.out.println(" offset x: " + x);
		for (int i = 0; i < x - 1; i++) {
			offset += lines[i].length() + 1;
		}
		// Line Columns start from 1
		offset += y == 0 ? 0 : y - 1;
				
		return offset;
	}

	public static String readFile(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
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
