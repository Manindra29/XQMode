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
 * Syntax Checking Service for XQMode.<br>
 * 
 * Fetches code from editor every few seconds, converts it into pure java and
 * runs it through the Eclipse AST parser. Parser detects the syntax errors.
 * Errors are passed on to Error Window to be displayed to the user.
 * 
 * All this happens in a separate thread, so that PDE keeps running without any
 * hiccups.
 * 
 * @author Manindra Moharana
 */
public class SyntaxCheckerService implements Runnable {

	static public String PATH = "E:/TestStuff/hw1.java";
	public static final int sleepTime = 1000;

	private ASTParser parser;
	public Editor editor;
	private boolean stopThread = false;
	public ErrorWindow errorWindow;
	public ErrorBar errorBar;
	private IProblem[] problems;

	/**
	 * How many lines are present till the initial class declaration? In basic
	 * mode, this would include imports, class declaration and setup
	 * declaration. In nomral mode, this would include imports, class
	 * declaration only. It's fate is decided inside preprocessCode() }:)
	 */
	public int mainClassOffset;
	public boolean basicMode = false;

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

	public SyntaxCheckerService(ErrorWindow erw, ErrorBar erb) {
		parser = ASTParser.newParser(AST.JLS4);
		this.errorWindow = erw;
		this.errorBar = erb;
	}

	private String[] slashAnimation = { "|", "/", "--", "\\", "|", "/", "--",
			"\\" };
	private int slashAnimationIndex = 0;

	/**
	 * Initialiazes the Error Window from Syntax Checker Service
	 */
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
				errorWindow = new ErrorWindow(editor, this);
				errorWindow.setVisible(true);
				if (editor != null) {
					errorWindow.setTitle("Problems - "
							+ editor.getSketch().getName() + " "
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
				errorWindow
						.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
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

			// Store errors returned by the ast parser
			problems = cu.getProblems();

			// Populate the error list of error window
			if (errorWindow != null) {
				// errorWindow.problemList = cu.getProblems();
				errorWindow.problemList.clear();
				for (int i = 0; i < problems.length; i++) {
					int a[] = calculateTabIndex(problems[i]);
					errorWindow.problemList.add(new Problem(problems[i], a[0],
							a[1]));
					// System.out.println(editor.getSketch()
					// .getCode(a)
					// .getPrettyName()
					// + "-> " + problems[i].getSourceLineNumber());

				}
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
			errorBar.updateErrorPoints2(errorWindow.problemList);
			return true;
		} catch (Exception e) {
			System.out.println("Oops! [SyntaxCheckerThreaded.checkCode]: " + e);
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Calculates the tab number and line number of the error in that particular
	 * tab.
	 * 
	 * @param problem
	 *            - IProblem
	 * @return int[0] - tab number, int[1] - line number
	 */
	public int[] calculateTabIndex(IProblem problem) {
		// String[] lines = {};// = PApplet.split(sourceString, '\n');
		int codeIndex = 0;
		int bigCount = 0;

		int x = problem.getSourceLineNumber() - mainClassOffset;

		for (SketchCode sc : editor.getSketch().getCode()) {
			if (sc.isExtension("pde")) {
				sc.setPreprocOffset(bigCount);

				try {

					int len = 0;
					if (editor.getSketch().getCurrentCode().equals(sc)) {
						// lines = PApplet.split(
						// sc.getDocument().getText(0,
						// sc.getDocument().getLength()), '\n');
						// System.out.println("Getting from document "
						// + sc.getLineCount() + "," + lines.length);
						len = Base.countLines(sc.getDocument().getText(0,
								sc.getDocument().getLength())) + 1;
					} else {
						// lines = PApplet.split(sc.getProgram(), '\n');
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
						// editor.getSketch().setCurrentCode(codeIndex);
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

		// TODO: Dirty hack to stop the index out of bounds errors
		// Remove this check, and add a } at the end, in the last tab to
		// generate the error.
		if (codeIndex >= editor.getSketch().getCodeCount())
			codeIndex = editor.getSketch().getCodeCount() - 1;
		return new int[] { codeIndex, x };
	}

	/**
	 * Starts the Syntax Checker Service thread
	 */
	@Override
	public void run() {
		initializeErrorWindow();
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

	/**
	 * Stops the Syntax Checker Service thread
	 */
	public void stopThread() {
		stopThread = true;
		System.out.println("Syntax Checker Service stopped.");
	}

	/**
	 * Fetches code from the editor tabs and pre-processes it into parsable pure
	 * java source. <br>
	 * Handles: <li>Removal of import statements <li>Conversion of int(),
	 * char(), etc to (int)(), (char)(), etc. <li>Replacing '#' with 0xff for
	 * color representation <li>Appends class declaration statement
	 * 
	 * @return String - Pure java representation of PDE code
	 */
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
						+ "(\\s*\\[\\s*\\])?\\s+[a-zA-Z0-9]+\\s*\\(",
				Pattern.MULTILINE);

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
			basicMode = true;
			mainClassOffset = 2;

		} else {
			sourceAlt = "public class " + className + " extends PApplet {\n"
					+ sourceAlt + "\n}";
			basicMode = false;
			mainClassOffset = 1;
		}
		// Convert non ascii characters
		// sourceAlt = substituteUnicode(sourceAlt);

		// System.out.println("-->\n" + sourceAlt + "\n<--");
		// System.out.println("PDE code processed - "
		// + editor.getSketch().getName());

		return sourceAlt;
	}

	/**
	 * Sets the error table in the Error Window
	 */
	private void setErrorTable() {
		try {
			String[][] errorData = new String[problems.length][3];
			for (int i = 0; i < problems.length; i++) {
				// System.out
				// .print(errorWindow.problemList.get(i).tabIndex + ", ");
				errorData[i][0] = problems[i].getMessage(); // Make this message
															// more natural.
				errorData[i][1] = editor.getSketch()
						.getCode(errorWindow.problemList.get(i).tabIndex)
						.getPrettyName();
				errorData[i][2] = errorWindow.problemList.get(i).lineNumber
						+ "";

				// (problems[i].getSourceLineNumber() - mainClassOffset)
				// + "";
			}
			// System.out.println();
			DefaultTableModel tm = new DefaultTableModel(errorData,
					ErrorWindow.columnNames);
			errorWindow.updateTable(tm);

		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println(e + " at setErrorTable()");
			e.printStackTrace();
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
			errorWindow.setTitle("Problems - " + editor.getSketch().getName()
					+ " " + slashAnimation[slashAnimationIndex]);
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

	public int xyToOffset(int x, int y) {

		String[] lines = {};// = PApplet.split(sourceString, '\n');
		int offset = 0;
		int codeIndex = 0;
		int bigCount = 0;

		x -= mainClassOffset;
		try {
			for (SketchCode sc : editor.getSketch().getCode()) {
				if (sc.isExtension("pde")) {
					sc.setPreprocOffset(bigCount);

					int len = 0;
					if (editor.getSketch().getCurrentCode().equals(sc)) {
						lines = PApplet.split(
								sc.getDocument().getText(0,
										sc.getDocument().getLength()), '\n');
						// System.out.println("Getting from document "
						// + sc.getLineCount() + "," + lines.length);
						len = Base.countLines(sc.getDocument().getText(0,
								sc.getDocument().getLength())) + 1;
					} else {
						lines = PApplet.split(sc.getProgram(), '\n');
						len = Base.countLines(sc.getProgram()) + 1;
					}

					// Adding + 1 to len because \n gets appended for each
					// sketchcode extracted during processPDECode()
					if (x >= len) {
						// System.out.println("x,len" + x + "," + len);
						x -= len;
						codeIndex++;

					} else {
						if (codeIndex >= editor.getSketch().getCodeCount())
							codeIndex = editor.getSketch().getCodeCount() - 1;
						editor.getSketch().setCurrentCode(codeIndex);
						break;
					}

				}
				bigCount += sc.getLineCount();
			}

			// Using setCurrentCode again below. A dirty method. How does one
			// tackle error locations which are generated by the parser outside
			// the line number of the source visible in the pde? Like adding a }
			// at the end of the code, the extra } added by the pre processor is
			// the error source(detected by the parser). Tab switching has to be
			// done this way then. TODO

			if (codeIndex >= editor.getSketch().getCodeCount())
				codeIndex = editor.getSketch().getCodeCount() - 1;
			editor.getSketch().setCurrentCode(codeIndex);

			// Count chars till the end of previous line(x-1), keeping in mind x
			// starts from 1
			for (int i = 0; i < x - 1; i++) {
				if (i < lines.length)
					offset += lines[i].length() + 1;
			}
			// Line Columns start from 1
			offset += y == 0 ? 0 : y - 1;
		} catch (Exception e) {
			System.out.println("Exception in xyToOffset");
			e.printStackTrace();
		}
		return offset;
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
