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

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.table.DefaultTableModel;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

import processing.app.Base;
import processing.app.Editor;
import processing.app.Library;
import processing.app.SketchCode;
import processing.core.PApplet;

/**
 * Error Checking Service for XQMode.<br>
 * <br>
 * 
 * Program Flow:<br>
 * <li>Fetches code from editor every few seconds, converts it into pure java
 * and runs it through the Eclipse AST parser. <li>Parser detects the syntax
 * errors. Errors are passed on to Error Window, ErrorBar, etc. to be displayed
 * to the user. <li>
 * If no syntax errors are detected, code is further processed into compilable
 * form by the XQPreprocessor. <li>Contributed libraries' jars are added to
 * classpath and Compiler is loaded by URLCLassLoader with the extra jars in its
 * classpath. <li>The code is compiled and finally, ErrorWindow, ErrorBar, etc
 * are updated with new errors. <br>
 * All this happens in a separate thread, so that PDE keeps running without any
 * hiccups.
 * 
 * @author Manindra Moharana &lt;mkmoharana29@gmail.com&gt;
 */
public class ErrorCheckerService implements Runnable {

	static public String PATH = "E:/TestStuff/HelloPeasy.java";
	/**
	 * Error check happens every sleepTime milliseconds
	 */
	public static final int sleepTime = 1000;

	/**
	 * The amazing eclipse ast parser
	 */
	private ASTParser parser;
	public XQEditor editor;
	/**
	 * Used to indirectly stop the Error Checker Thread
	 */
	public boolean stopThread = false;

	/**
	 * Used to indirectly pause the Error Checking. Calls to checkCode() become
	 * useless.
	 */
	public boolean pauseThread = false;

	public ErrorWindow errorWindow;
	public ErrorBar errorBar;
	/**
	 * IProblem[] returned by parser stored in here
	 */
	private IProblem[] problems;
	public String className, sourceCode;
	/**
	 * URLs of extra imports jar files stored here.
	 */
	public URL[] classpath;

	/**
	 * Code preprocessed by the custom preprocessor
	 */
	private StringBuffer rawCode;

	private int scPreProcOffset = 0;

	/**
	 * Stores all Problems in the sketch
	 */
	public ArrayList<Problem> problemsList;

	/**
	 * How many lines are present till the initial class declaration? In basic
	 * mode, this would include imports, class declaration and setup
	 * declaration. In nomral mode, this would include imports, class
	 * declaration only. It's fate is decided inside preprocessCode() }:)
	 */
	public int mainClassOffset;

	/**
	 * Is the sketch running in basic mode or active mode?
	 */
	public boolean basicMode = false;
	private CompilationUnit cu;

	/**
	 * If true, compilation checker will be reloaded with updated classpath
	 * items.
	 */
	public boolean loadCompClass = true;

	/**
	 * Compiler Checker class
	 */
	Class<?> checkerClass;

	/**
	 * Compiler Checker used by compCheck class
	 */
	CompilationCheckerInterface compCheck;

	/**
	 * List of jar files to be present in compilation checker's classpath
	 */
	public ArrayList<URL> classpathJars;

	/**
	 * Timestamp - for measuring total overhead
	 */
	long lastTimeStamp = System.currentTimeMillis();

	private String[] slashAnimation = { "|", "/", "--", "\\", "|", "/", "--",
			"\\" };
	private int slashAnimationIndex = 0;

	/**
	 * This is needed so that if ErrorWindow is closed using te close button on
	 * the window itself, the menu item needs to be unchecked.
	 */
	public JCheckBoxMenuItem problemWindowMenuCB;

	/**
	 * This is used to detect if the current tab index has changed and thus
	 * repaint the textarea.
	 */
	public int currentTab = 0;
	public int lastTab = 0;

	/**
	 * Stores the current import statements in the program. Used to compare for
	 * changed import statements and update classpath if needed.
	 */
	ArrayList<ImportStatement> programImports;

	/**
	 * List of imports when sketch was last checked. Used for checking for
	 * changed imports
	 */
	ArrayList<ImportStatement> previousImports = new ArrayList<ImportStatement>();

	/**
	 * Teh Preprocessor
	 */
	public XQPreprocessor xqpreproc;

	/**
	 * Regexp for import statements. (Used from Processing source)
	 */
	final String importRegexp = "(?:^|;)\\s*(import\\s+)((?:static\\s+)?\\S+)(\\s*;)";

	public static void main(String[] args) {

		try {
			ErrorCheckerService syncheck = new ErrorCheckerService();
			ErrorCheckerService.showClassPath();
			// syncheck.checkCode();
			// syncheck.preprocessCode();
			// File f = new File(
			// "E:/WorkSpaces/Eclipse Workspace 2/AST Test 2/bin");
			File f = new File("resources/CompilationCheckerClasses");

			File f2 = new File(
					"C:/Users/QuarkNinja/Documents/Processing/libraries/peasycam/library/peasycam.jar");

			URL[] classpath;
			classpath = new URL[] { f2.toURI().toURL(), f.toURI().toURL() };
			URLClassLoader classLoader = new URLClassLoader(classpath);
			// quarkninja.mode.xqmode.CompilationChecker
			Class<?> checkerClass = Class.forName("CompilationChecker", true,
					classLoader);
			CompilationCheckerInterface compCheck = (CompilationCheckerInterface) checkerClass
					.newInstance();
			System.out.println(compCheck.getErrors("HelloPeasy",
					syncheck.preprocessCode())[0]);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ErrorCheckerService() {
		initParser();
		initializeErrorWindow();

	}

	public ErrorCheckerService(String path) {
		PATH = path;
		initParser();
		initializeErrorWindow();
	}

	public ErrorCheckerService(XQEditor editor, ErrorBar erb) {
		initParser();
		this.editor = editor;
		this.errorBar = erb;
	}

	/**
	 * Initializes ASTParser
	 */
	private void initParser() {
		try {
			parser = ASTParser.newParser(AST.JLS4);
		} catch (Exception e) {
			System.err
					.println("XQMode initialization failed. Are you running the right version of Processing? "
							+ "Think about it.");
			pauseThread = true;
		}
	}

	/**
	 * Initialiazes the Error Window
	 */
	public void initializeErrorWindow() {
		if (editor == null)
			return;
		if (errorWindow != null)
			return;

		final ErrorCheckerService thisService = this;
		final Editor thisEditor = editor;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					errorWindow = new ErrorWindow(thisEditor, thisService);
					errorWindow.problemWindowMenuCB = problemWindowMenuCB;
					//errorWindow.setVisible(true);
					editor.toFront();
					errorWindow.errorTable.setFocusable(false);
					editor.setSelection(0, 0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Prints out classpath elements per line. Used for debugging only.
	 */
	public static void showClassPath() {
		System.out.println("------Classpath------");
		String cps[] = PApplet.split(System.getProperty("java.class.path"),
				Base.isWindows() ? ';' : ':');
		for (int i = 0; i < cps.length; i++) {
			System.out.println(cps[i]);
		}
		System.out.println("---------------------");
	}

	/**
	 * Perform error check
	 * 
	 * @return true - if checking was completed succesfully.
	 */
	public boolean checkCode() {
		// Reset stuff here, maybe make reset()?
		sourceCode = "";
		lastTimeStamp = System.currentTimeMillis();

		try {
			if (editor != null)
				sourceCode = preprocessCode();
			else {
				sourceCode = readFile(PATH);
			}

			// Begin Stage 1!
			syntaxCheck();

			// No syntax errors, proceed for compilation check, Stage 2.
			if (problems.length == 0 && editor.compilationCheckEnabled) {
				sourceCode = xqpreproc.doYourThing(sourceCode, programImports);
				prepareCompilerClasspath();
				mainClassOffset = xqpreproc.mainClassOffset;
				if (basicMode)
					mainClassOffset++;
				// System.out.println("--------------------------");
				// System.out.println(sourceCode);
				// System.out.println("--------------------------");
				compileCheck();
			}

			// Notify user of the mess he's done.
			if (editor != null) {
				updateErrorTable();
				errorBar.updateErrorPoints(problemsList);
				updateTextAreaPainter();
				updateEditorStatus();
				return true;
			}

		} catch (Exception e) {
			System.out.println("Oops! [ErrorCheckerService.checkCode]: " + e);
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Updates editor status bar, depending on whether the caret is on an error
	 * line or not
	 */
	public void updateEditorStatus() {
		// editor.statusNotice("Position: " +
		// editor.getTextArea().getCaretLine());
		boolean notFound = true;
		for (ErrorMarker emarker : errorBar.errorPoints) {
			if (emarker.problem.lineNumber == editor.getTextArea()
					.getCaretLine() + 1) {
				if (emarker.type == ErrorMarker.Warning)
					editor.statusNotice(emarker.problem.message);
				else
					editor.statusError(emarker.problem.message);
				return;
			}
		}
		if (notFound)
			editor.statusEmpty();
	}

	/**
	 * Performs syntax check.
	 */
	private void syntaxCheck() {
		parser.setSource(sourceCode.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		@SuppressWarnings("unchecked")
		Map<String, String> options = JavaCore.getOptions();

		// Ben has decided to move on to 1.6. Yay!
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_6, options);
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_6);
		parser.setCompilerOptions(options);
		cu = (CompilationUnit) parser.createAST(null);

		// Store errors returned by the ast parser
		problems = cu.getProblems();
		// if (problems.length > 0)
		// System.out.println("Syntax error count: " + problems.length
		// + "---" + (System.currentTimeMillis() - lastTimeStamp)
		// + "ms || ");

		// Populate the probList
		problemsList = new ArrayList<Problem>();
		for (int i = 0; i < problems.length; i++) {
			int a[] = calculateTabIndexAndLineNumber(problems[i]);
			problemsList.add(new Problem(problems[i], a[0], a[1]));
		}

		// if (editor == null) {
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
		// }
		// }
	}

	/**
	 * The name cannot get any simpler, can it?
	 */
	private void compileCheck() {
		try {

			// NOTE TO SELF: If classpath contains null Strings
			// URLClassLoader gets angry. Drops NPE bombs.

			// File f = new File(
			// "resources/CompilationCheckerClasses");
			// System.out.println(1);
			// File f = new File(
			// "E:/WorkSpaces/Eclipse Workspace 2/AST Test 2/bin");

			// If imports have changed, reload classes with new classpath.
			if (loadCompClass) {
				if (classpathJars.size() > 0)
					System.out
							.println("XQMode: Loading contributed libraries referenced by import statements.");
				File f = new File(editor.getBase().getSketchbookFolder()
						.getAbsolutePath()
						+ File.separator
						+ "modes"
						+ File.separator
						+ "XQMode"
						+ File.separator + "CompilationCheckerClasses");
				classpath = new URL[classpathJars.size() + 1];
				for (int i = 0; i < classpathJars.size(); i++) {
					classpath[i] = classpathJars.get(i);
					// System.out.println(classpathJars.get(i));
				}
				// System.out.println("---------" + classpath.length);
				classpath[classpath.length - 1] = f.toURI().toURL();

				// System.out.println("-- " + classpath.length);
				// System.out.println(2);
				URLClassLoader classLoader = new URLClassLoader(classpath);
				// System.out.println(3);

				checkerClass = Class.forName("CompilationChecker", true,
						classLoader);

				compCheck = (CompilationCheckerInterface) checkerClass
						.newInstance();

				loadCompClass = false;
			}

			if (compilerSettings == null)
				prepareCompilerSetting();

			// The one line that does it all! All heavylifting happens in
			// CompilationChecker.java!
			IProblem[] prob = compCheck.getErrors(className, sourceCode,
					compilerSettings);

			if (problems == null || problems.length == 0) {
				problems = new IProblem[prob.length];
			}

			// int errorCount = 0, warningCount = 0;
			for (int i = 0, k = 0; i < prob.length; i++) {
				IProblem problem = prob[i];
				if (problem == null) {
					System.out.println(i + " is null.");
					continue;
				}
//				if (problem.getID() == IProblem.UnusedImport
//						|| problem.getID() == IProblem.MissingSerialVersion)
//					continue;
				problems[k++] = problem;
				if(problems[i].isWarning() && !warningsEnabled)
					continue;
				int a[] = calculateTabIndexAndLineNumber(problem);
				problemsList.add(new Problem(problem, a[0], a[1]));

				// StringBuffer buffer = new StringBuffer();
				// buffer.append(problem.getMessage());
				// buffer.append(" | line: ");
				// buffer.append(problem.getSourceLineNumber());
				// String msg = buffer.toString();
				// if (problem.isError()) {
				// msg = "Error: " + msg;
				// errorCount++;
				// } else if (problem.isWarning()) {
				// msg = "Warning: " + msg;
				// warningCount++;
				// }
				// System.out.println(msg);
			}

			// System.out.println("Total warnings: " + warningCount
			// + ", Total errors: " + errorCount + " , Len: "
			// + prob.length);
		} catch (InstantiationException e) {
			System.err
					.println(e
							+ " compileCheck() problem. Somebody tried to mess with XQMode files.");
		} catch (IllegalAccessException e) {
			System.err.println(e + " compileCheck() problem.");
		} catch (ClassNotFoundException e) {
			System.err.println("Compiltation Checker files couldn't be found! "
					+ e + " compileCheck() problem.");
		} catch (MalformedURLException e) {
			System.err.println("Compiltation Checker files couldn't be found! "
					+ e + " compileCheck() problem.");
		} catch (Exception e) {
			System.err.println("compileCheck() problem." + e);
			e.printStackTrace();
		}

		// System.out.println("Compilecheck, Done.");
	}

	@SuppressWarnings("rawtypes")
	private Map compilerSettings;
	public boolean warningsEnabled = true;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void prepareCompilerSetting() {
		compilerSettings = new HashMap();

		compilerSettings.put(CompilerOptions.OPTION_LineNumberAttribute,
				CompilerOptions.GENERATE);
		compilerSettings.put(CompilerOptions.OPTION_SourceFileAttribute,
				CompilerOptions.GENERATE);
		compilerSettings.put(CompilerOptions.OPTION_Source,
				CompilerOptions.VERSION_1_6);
		compilerSettings.put(CompilerOptions.OPTION_ReportUnusedImport,
				CompilerOptions.IGNORE);
		compilerSettings.put(CompilerOptions.OPTION_ReportMissingSerialVersion,
				CompilerOptions.IGNORE);
		compilerSettings.put(CompilerOptions.OPTION_ReportRawTypeReference,
				CompilerOptions.IGNORE);
		compilerSettings.put(
				CompilerOptions.OPTION_ReportUncheckedTypeOperation,
				CompilerOptions.IGNORE);

	}

	/**
	 * Calculates the tab number and line number of the error in that particular
	 * tab.
	 * 
	 * @param problem
	 *            - IProblem
	 * @return int[0] - tab number, int[1] - line number
	 */
	public int[] calculateTabIndexAndLineNumber(IProblem problem) {
		// String[] lines = {};// = PApplet.split(sourceString, '\n');
		int codeIndex = 0;
		int bigCount = 0;

		int x = problem.getSourceLineNumber() - mainClassOffset;
		if (x < 0) {
			// System.out.println("Negative line number "
			// + problem.getSourceLineNumber() + " , offset "
			// + mainClassOffset);
			x = problem.getSourceLineNumber() - 2; // Another -1 for 0 index
			if (x < programImports.size() && x >= 0) {
				ImportStatement is = programImports.get(x);
				// System.out.println(is.importName + ", " + is.tab + ", "
				// + is.lineNumber);
				return new int[] { is.tab, is.lineNumber };
			} else {

				// Some seriously badass stray error, just can't find the source
				// line! Simply return first line for first tab.
				return new int[] { 0, 1 };
			}

		}

		try {
			for (SketchCode sc : editor.getSketch().getCode()) {
				if (sc.isExtension("pde")) {
					sc.setPreprocOffset(bigCount);
					int len = 0;
					if (editor.getSketch().getCurrentCode().equals(sc)) {
						len = Base.countLines(sc.getDocument().getText(0,
								sc.getDocument().getLength())) + 1;
					} else {
						len = Base.countLines(sc.getProgram()) + 1;
					}

					// System.out.println("x,len, CI: " + x + "," + len + ","
					// + codeIndex);

					if (x >= len) {

						// We're in the last tab and the line count is greater
						// than the no.
						// of lines in the tab,
						if (codeIndex >= editor.getSketch().getCodeCount() - 1) {
							// System.out.println("Exceeds lc " + x + "," + len
							// + problem.toString());
							// x = len
							x = editor.getSketch().getCode(codeIndex)
									.getLineCount();
							// TODO: Obtain line having last non-white space
							// character in the code.
							break;
						} else {
							x -= len;
							codeIndex++;
						}
					} else {

						if (codeIndex >= editor.getSketch().getCodeCount())
							codeIndex = editor.getSketch().getCodeCount() - 1;
						break;
					}

				}
				bigCount += sc.getLineCount();
			}
		} catch (Exception e) {
			System.err
					.println("Things got messed up in ErrorCheckerService.calculateTabIndexAndLineNumber()");
		}

		return new int[] { codeIndex, x };
	}

	int runCount = 0;

	/**
	 * Starts the Syntax Checker Service thread
	 */
	@Override
	public void run() {
		lastTab = editor.getSketch().getCodeIndex(
				editor.getSketch().getCurrentCode());
		initializeErrorWindow();
		xqpreproc = new XQPreprocessor();
		checkCode();
		editor.getTextArea().repaint();
		stopThread = false;

		while (!stopThread) {
			try {
				// Take a nap.
				Thread.sleep(sleepTime);
			} catch (Exception e) {
				System.out.println("Oops! [ErrorCheckerThreaded]: " + e);
				// e.printStackTrace();
			}

			if (pauseThread)
				continue;

			// Check every x seconds
			checkCode();
			if (runCount < 5) {
				runCount++;
			}

			if (runCount == 3)
				System.out.println("XQMode v0.2 alpha");
		}
	}

	/**
	 * Stops the Error Checker Service thread
	 */
	public void stopThread() {
		stopThread = true;
		System.out.println(editor.getSketch().getName()
				+ " - Error Checker stopped.");
	}

	/**
	 * Fetches code from the editor tabs and pre-processes it into parsable pure
	 * java source. And there's a difference between parsable and compilable.
	 * XQPrerocessor.java makes this code compilable. <br>
	 * Handles: <li>Removal of import statements <li>Conversion of int(),
	 * char(), etc to (int)(), (char)(), etc. <li>Replacing '#' with 0xff for
	 * color representation <li>Appends class declaration statement after
	 * determining the mode the sketch is in - ACTIVE or BASIC
	 * 
	 * @return String - Pure java representation of PDE code. Note that this
	 *         code is not yet compile ready.
	 */
	private String preprocessCode() {

		String sourceAlt = "";
		programImports = new ArrayList<ImportStatement>();
		if (editor == null) {
			try {
				sourceAlt = readFile(PATH);
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println(sourceAlt);
			System.out.println("-----------PreProcessed----------");
			return sourceAlt;
		}
		// Super wicked regular expressions! (Used from Processing source)
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
				rawCode = new StringBuffer();

				for (SketchCode sc : editor.getSketch().getCode()) {
					if (sc.isExtension("pde")) {
						sc.setPreprocOffset(scPreProcOffset);

						try {

							if (editor.getSketch().getCurrentCode().equals(sc)) {

								// rawCode.append(sc.getDocument().getText(0,
								// sc.getDocument().getLength()));
								rawCode.append(scrapImportStatements(
										sc.getDocument().getText(0,
												sc.getDocument().getLength()),
										editor.getSketch().getCodeIndex(sc)));
							} else {

								// rawCode.append(sc.getProgram());
								rawCode.append(scrapImportStatements(sc
										.getProgram(), editor.getSketch()
										.getCodeIndex(sc)));

							}
							rawCode.append('\n');
						} catch (Exception e) {
							System.err
									.println("Exception in preprocessCode() - bigCode "
											+ e.toString());
						}
						rawCode.append('\n');
						scPreProcOffset += sc.getLineCount();
					}
				}

				sourceAlt = rawCode.toString();
				// System.out.println("Obtaining source from editor.");
			}
		} catch (Exception e) {

			System.out.println("Exception in preprocessCode()");

		}

		// Replace comments with whitespaces
		// sourceAlt = scrubComments(sourceAlt);

		// Find all int(*), replace with PApplet.parseInt(*)

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
			sourceAlt = matcher.replaceAll("PApplet.parse"
					+ Character.toUpperCase(dataType.charAt(0))
					+ dataType.substring(1) + "(");

		}

		// Find all #[web color] and replace with 0xff[webcolor]
		// Should be 6 digits only.
		final String webColorRegexp = "#{1}[A-F|a-f|0-9]{6}\\W";
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

		// TODO: Experimental.
		// Replace all color data types with int
		// Regex, Y U SO powerful?
		final String colorTypeRegex = "color(?![a-zA-Z0-9_])(?=\\[*)(?!(\\s*\\())";
		Pattern colorPattern = Pattern.compile(colorTypeRegex);
		Matcher colorMatcher = colorPattern.matcher(sourceAlt);
		sourceAlt = colorMatcher.replaceAll("int");

		checkForChangedImports();

		className = (editor == null) ? "DefaultClass" : editor.getSketch()
				.getName();

		// Check whether the code is being written in STATIC mode(no function
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

		// Handle unicode characters
		sourceAlt = substituteUnicode(sourceAlt);

		// System.out.println("-->\n" + sourceAlt + "\n<--");
		// System.out.println("PDE code processed - "
		// + editor.getSketch().getName());
		sourceCode = sourceAlt;
		return sourceAlt;
	}

	/**
	 * Removes import statements from tabSource, replaces each with a blank line
	 * and adds the import to the list of program imports
	 * 
	 * @param tabSource
	 * @param tabNumber
	 */
	private String scrapImportStatements(String tabProgram, int tabNumber) {

		String tabSource = new String(tabProgram);
		do {
			// System.out.println("-->\n" + sourceAlt + "\n<--");
			String[] pieces = PApplet.match(tabSource, importRegexp);

			// Stop the loop if we've removed all the import lines
			if (pieces == null)
				break;

			String piece = pieces[1] + pieces[2] + pieces[3];
			int len = piece.length(); // how much to trim out

			// programImports.add(piece); // the package name

			// find index of this import in the program
			int idx = tabSource.indexOf(piece);
			// System.out.print("Import -> " + piece);
			// System.out.println(" - "
			// + Base.countLines(tabSource.substring(0, idx)) + " tab "
			// + tabNumber);
			programImports.add(new ImportStatement(piece, tabNumber, Base
					.countLines(tabSource.substring(0, idx))));
			// Remove the import from the main program
			String whiteSpace = "";
			for (int j = 0; j < piece.length(); j++) {
				whiteSpace += " ";
			}
			tabSource = tabSource.substring(0, idx) + whiteSpace
					+ tabSource.substring(idx + len);

		} while (true);
		// System.out.println(tabSource);
		return tabSource;
	}

	/**
	 * Checks if import statements in the sketch have changed. If they have,
	 * compiler classpath needs to be updated.
	 */
	private void checkForChangedImports() {
		// System.out.println("Imports: " + programImports.size() +
		// " Prev Imp: "
		// + previousImports.size());
		if (programImports.size() != previousImports.size()) {
			// System.out.println(1);
			loadCompClass = true;
			previousImports = programImports;
		} else {
			for (int i = 0; i < programImports.size(); i++) {
				if (!programImports.get(i).importName.equals(previousImports
						.get(i).importName)) {
					// System.out.println(2);
					loadCompClass = true;
					previousImports = programImports;
					break;
				}
			}
		}
		// System.out.println("load..? " + loadCompClass);
	}

	/**
	 * Processes import statements to obtain classpaths of contributed
	 * libraries. This would be needed for compilation check. Also, adds
	 * stuff(jar files, class files, candy) from the code folder. And I've
	 * messed it up horribly.
	 * 
	 * @param programImports
	 */
	private void prepareCompilerClasspath() {
		if (!loadCompClass)
			return;
		// System.out.println("1..");
		classpathJars = new ArrayList<URL>();
		String entry = "";
		boolean codeFolderChecked = false;
		for (ImportStatement impstat : programImports) {
			String item = impstat.importName;
			int dot = item.lastIndexOf('.');
			entry = (dot == -1) ? item : item.substring(0, dot);

			entry = entry.substring(6).trim();
			// System.out.println("Entry--" + entry);
			if (ignorableImport(entry)) {
				// System.out.println("Ignoring: " + entry);
				continue;
			}
			Library library = null;
			try {
				library = editor.getMode().getLibrary(entry);
				// System.out.println("lib->" + library.getClassPath() + "<-");
				String libraryPath[] = PApplet.split(library.getClassPath()
						.substring(1).trim(), File.pathSeparatorChar);
				for (int i = 0; i < libraryPath.length; i++) {
					// System.out.println(entry + " ::"
					// + new File(libraryPath[i]).toURI().toURL());
					classpathJars.add(new File(libraryPath[i]).toURI().toURL());
				}
				// System.out.println("-- ");
				// classpath[count] = (new File(library.getClassPath()
				// .substring(1))).toURI().toURL();
				// System.out.println("  found ");
				// System.out.println(library.getClassPath().substring(1));
			} catch (Exception e) {
				if (library == null && !codeFolderChecked) {
					// System.out.println(1);
					// Look around in the code folde
					if (editor.getSketch().hasCodeFolder()) {
						File codeFolder = editor.getSketch().getCodeFolder();

						// get a list of .jar files in the "code" folder
						// (class files in subfolders should also be picked up)
						String codeFolderClassPath = Base
								.contentsToClassPath(codeFolder);
						codeFolderChecked = true;
						if (codeFolderClassPath.equalsIgnoreCase("")) {
							System.err.println("XQMODE: Yikes! Can't find \""
									+ entry
									+ "\" library! Line: "
									+ impstat.lineNumber
									+ " in tab: "
									+ editor.getSketch().getCode(impstat.tab)
											.getPrettyName());
							System.out
									.println("Please make sure that the library is present in <sketchbook "
											+ "folder>/libraries folder or in the code folder of your sketch");

						}
						String codeFolderPath[] = PApplet.split(
								codeFolderClassPath.substring(1).trim(),
								File.pathSeparatorChar);
						try {
							for (int i = 0; i < codeFolderPath.length; i++) {
								classpathJars.add(new File(codeFolderPath[i])
										.toURI().toURL());
							}

						} catch (Exception e2) {
							System.out
									.println("Yikes! codefolder, prepareImports(): "
											+ e2);
						}
					} else {
						System.err.println("XQMODE: Yikes! Can't find \""
								+ entry
								+ "\" library! Line: "
								+ impstat.lineNumber
								+ " in tab: "
								+ editor.getSketch().getCode(impstat.tab)
										.getPrettyName());
						System.out
								.println("Please make sure that the library is present in <sketchbook "
										+ "folder>/libraries folder or in the code folder of your sketch");
					}

				} else {
					System.err
							.println("Yikes! There was some problem in prepareImports(): "
									+ e);
					System.err.println("I was processing: " + entry);

					// e.printStackTrace();
				}
			}

		}

	}

	/**
	 * Ignore processing packages, java.*.*. etc.
	 * 
	 * @param packageName
	 * @return boolean
	 */
	private boolean ignorableImport(String packageName) {
		// packageName.startsWith("processing.")
		// ||
		if (packageName.startsWith("java.") || packageName.startsWith("javax.")) {
			return true;
		}
		return false;
	}

	/**
	 * Updates the error table in the Error Window.
	 */
	synchronized public void updateErrorTable() {

		try {
			String[][] errorData = new String[problemsList.size()][3];
			for (int i = 0; i < problemsList.size(); i++) {
				errorData[i][0] = problemsList.get(i).message;
				errorData[i][1] = editor.getSketch()
						.getCode(problemsList.get(i).tabIndex).getPrettyName();
				errorData[i][2] = problemsList.get(i).lineNumber + "";
			}

			// initializeErrorWindow();

			if (errorWindow != null) {
				DefaultTableModel tm = new DefaultTableModel(errorData,
						XQErrorTable.columnNames);
				if (errorWindow.isVisible())
					errorWindow.updateTable(tm);
				((XQEditor) editor).updateTable(tm);
				// A nifty rotating slash animation on the title bar to show
				// that error checker thread is running

				slashAnimationIndex++;
				if (slashAnimationIndex == slashAnimation.length)
					slashAnimationIndex = 0;
				if (editor != null) {
					String info = slashAnimation[slashAnimationIndex] + " T:"
							+ (System.currentTimeMillis() - lastTimeStamp)
							+ "ms";
					errorWindow.setTitle("Problems - "
							+ editor.getSketch().getName() + " " + info);
				}
			}

		} catch (Exception e) {
			System.out.println("Exception at updateErrorTable() " + e);
			e.printStackTrace();
			stopThread();
		}

	}

	/**
	 * Repaints the textarea if required
	 */
	public void updateTextAreaPainter() {
		currentTab = editor.getSketch().getCodeIndex(
				editor.getSketch().getCurrentCode());
		if (currentTab != lastTab) {
			lastTab = currentTab;
			editor.getTextArea().repaint();
			// System.out.println("Repaint");
		}

		if (!errorBar.errorPointsOld.equals(errorBar.errorPoints)) {
			editor.getTextArea().repaint();
		}
	}

	/**
	 * Scrolls to the error source in code. Used by ErrorWindow and ErrorBar
	 * 
	 * @param errorIndex
	 */
	public void scrollToErrorLine(int errorIndex) {
		if (editor == null)
			return;
		if (errorIndex < problemsList.size() && errorIndex >= 0) {
			Problem p = problemsList.get(errorIndex);
			try {
				editor.toFront();
				editor.getSketch().setCurrentCode(p.tabIndex);
				editor.getTextArea().scrollTo(p.lineNumber - 1, 0);
				editor.setSelection(editor.getTextArea()
						.getLineStartNonWhiteSpaceOffset(p.lineNumber - 1)
						+ editor.getTextArea().getLineText(p.lineNumber - 1)
								.trim().length(), editor.getTextArea()
						.getLineStartNonWhiteSpaceOffset(p.lineNumber - 1));
				editor.repaint();
			} catch (Exception e) {
				System.err
						.println(e
								+ " : Error while selecting text in scrollToErrorLine()");
				// e.printStackTrace();
			}
			// System.out.println("---");

		}
	}

	/**
	 * Replaces non-ascii characters with their unicode escape sequences and
	 * stuff. Used as it is from
	 * processing.src.processing.mode.java.preproc.PdePreprocessor
	 * 
	 * @param program
	 *            - Input String containing non ascii characters
	 * @return String - Converted String
	 */
	public static String substituteUnicode(String program) {
		// check for non-ascii chars (these will be/must be in unicode format)
		char p[] = program.toCharArray();
		int unicodeCount = 0;
		for (int i = 0; i < p.length; i++) {
			if (p[i] > 127)
				unicodeCount++;
		}
		if (unicodeCount == 0)
			return program;
		// if non-ascii chars are in there, convert to unicode escapes
		// add unicodeCount * 5.. replacing each unicode char
		// with six digit uXXXX sequence (xxxx is in hex)
		// (except for nbsp chars which will be a replaced with a space)
		int index = 0;
		char p2[] = new char[p.length + unicodeCount * 5];
		for (int i = 0; i < p.length; i++) {
			if (p[i] < 128) {
				p2[index++] = p[i];
			} else if (p[i] == 160) { // unicode for non-breaking space
				p2[index++] = ' ';
			} else {
				int c = p[i];
				p2[index++] = '\\';
				p2[index++] = 'u';
				char str[] = Integer.toHexString(c).toCharArray();
				// add leading zeros, so that the length is 4
				// for (int i = 0; i < 4 - str.length; i++) p2[index++] = '0';
				for (int m = 0; m < 4 - str.length; m++)
					p2[index++] = '0';
				System.arraycopy(str, 0, p2, index, str.length);
				index += str.length;
			}
		}
		return new String(p2, 0, index);
	}

	public static String readFile(File file) throws IOException {
		System.out.println("File: " + file.getAbsolutePath());
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
