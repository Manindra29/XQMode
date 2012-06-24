package quarkninja.mode.xqmode;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
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
import processing.app.Library;
import processing.app.SketchCode;
import processing.app.SketchException;
import processing.core.PApplet;
import processing.mode.java.preproc.PdePreprocessor;
import processing.mode.java.preproc.PreprocessorResult;
import antlr.RecognitionException;
import antlr.TokenStreamException;

/**
 * Error Checking Service for XQMode.<br>
 * 
 * Fetches code from editor every few seconds, converts it into pure java and
 * runs it through the Eclipse AST parser. Parser detects the syntax errors.
 * Errors are passed on to Error Window to be displayed to the user. If no
 * syntax errors are detected, code is further processed into compilable form by
 * the P5 preprocessor. Contributed libraries' jars are added to classpath and
 * Compiler is loaded by URLCLassLoader putting the extra jars in its classpath.
 * Compilation is done are errors are passed on to Error Window to be displayed
 * to the user.
 * 
 * All this happens in a separate thread, so that PDE keeps running without any
 * hiccups.
 * 
 * @author Manindra Moharana
 */
public class ErrorCheckerService implements Runnable {

	static public String PATH = "E:/TestStuff/HelloPeasy.java";
	public static final int sleepTime = 1000;

	private ASTParser parser;
	public Editor editor;
	public boolean stopThread = false;
	public ErrorWindow errorWindow;
	public ErrorBar errorBar;
	private IProblem[] problems;
	public String className, sourceCode;
	public URL[] classpath;
	public boolean compileCheck;
	private StringBuffer rawCode;
	private int scPreProcOffset = 0;

	/**
	 * How many lines are present till the initial class declaration? In basic
	 * mode, this would include imports, class declaration and setup
	 * declaration. In nomral mode, this would include imports, class
	 * declaration only. It's fate is decided inside preprocessCode() }:)
	 */
	public int mainClassOffset;
	public boolean basicMode = false;

	// public static final String[] defaultImports = {
	// "import processing.core.*;", "import processing.xml.*;",
	// "import java.applet.*;", "import java.awt.Dimension;",
	// "import java.awt.Frame; ", "import java.awt.event.MouseEvent;",
	// "import java.awt.event.KeyEvent;",
	// "import java.awt.event.FocusEvent;", "import java.awt.Image;",
	// "import java.io.*;", "import java.net.*;", "import java.text.*;",
	// "import java.util.*;", "import java.util.zip.*;",
	// "import java.util.regex.*;", };
	public ArrayList<URL> classpathJars;

	public static void main(String[] args) {

		try {
			ErrorCheckerService syncheck = new ErrorCheckerService();
			syncheck.showClassPath();
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
		parser = ASTParser.newParser(AST.JLS4);
		initializeErrorWindow();
	}

	public ErrorCheckerService(String path) {
		PATH = path;
		parser = ASTParser.newParser(AST.JLS4);
		initializeErrorWindow();
	}

	public ErrorCheckerService(ErrorWindow erw, ErrorBar erb) {
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
	public void initializeErrorWindow() {
		if (editor == null)
			return;
		final ErrorCheckerService thisService = this;
		if (errorWindow == null) {
			// EventQueue.invokeLater(new Runnable() {
			// public void run() {
			// try {
			errorWindow = new ErrorWindow(editor, thisService);
			errorWindow.setVisible(true);
			System.out.println("EW Init");
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
			// }
			// });
			try {

				// if (editor != null) {
				// errorWindow.setTitle("Problems - "
				// + editor.getSketch().getName() + " "
				// + slashAnimation[slashAnimationIndex]);
				// }
				// One thread sleeps while other continues running - the
				// troubles of multithreading. Sigh, no more NPEs.
				System.out.println("Sleep 1");
				// Thread.sleep(500);
				System.out.println("Sleep 2");
				errorBar.errorWindow = errorWindow;
			} catch (Exception e1) {
				System.out.println("Stuck Here.");
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

	public void showClassPath() {
		String cp = System.getProperty("java.class.path");
		System.out.println("------Classpath------");
		String cps[] = PApplet.split(cp, ';');
		for (int i = 0; i < cps.length; i++) {
			System.out.println(cps[i]);
		}
		System.out.println("---------------------");
		// System.out.println(cp);
	}

	/**
	 * Perform error check
	 * 
	 * @return true - if checking was completed succesfully.
	 */
	public boolean checkCode() {
		// Reset stuff here, maybe make reset()?
		String source = "";
		sourceCode = "";
		compileCheck = false;
		lastTimeStamp = System.currentTimeMillis();

		try {
			if (editor != null)
				source = preprocessCode();
			else {
				source = readFile(PATH);
				sourceCode = source;
			}

			parser.setSource(source.toCharArray());
			parser.setKind(ASTParser.K_COMPILATION_UNIT);

			@SuppressWarnings("unchecked")
			Map<String, String> options = JavaCore.getOptions();

			// Ben has decided to move on to 1.6. Yay!
			JavaCore.setComplianceOptions(JavaCore.VERSION_1_6, options);
			options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_6);
			parser.setCompilerOptions(options);
			CompilationUnit cu = (CompilationUnit) parser.createAST(null);

			// Store errors returned by the ast parser
			problems = cu.getProblems();

			// Populate the error list of error window
			if (errorWindow != null) {
				// errorWindow.problemList = cu.getProblems();
				errorWindow.problemList.clear();
				for (int i = 0; i < problems.length; i++) {
					int a[] = calculateTabIndexAndLineNumber(problems[i]);
					errorWindow.problemList.add(new Problem(problems[i], a[0],
							a[1]));
					// System.out.println(editor.getSketch()
					// .getCode(a)
					// .getPrettyName()
					// + "-> " + problems[i].getSourceLineNumber());

				}
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

			if (problems.length == 0) {
				// System.out
				// .println("No syntax errors. Let the compilation begin!");
				sourceCode = preProcessP5style();
				compileCheck();
				compileCheck = true;
			}
			// showClassPath();
			if (editor != null) {
				setErrorTable();
				errorBar.updateErrorPoints(errorWindow.problemList);
				return true;
			}
		} catch (Exception e) {
			System.out.println("Oops! [SyntaxCheckerThreaded.checkCode]: " + e);
			e.printStackTrace();
		}
		return false;
	}

	long lastTimeStamp = System.currentTimeMillis();

	/**
	 * Preprocess PDE code to pure Java, P5 style. This is used only after
	 * eclipse ast parser has okayed the source code. SyntaxError
	 */
	private String preProcessP5style() {
		if (problems.length != 0) {
			System.err
					.println("Resolve syntax errors before P5 preprocessor does its thing.");
			return null;
		}
		PdePreprocessor preprocessor = new PdePreprocessor(editor.getSketch()
				.getName());

		String[] codeFolderPackages = null;
		if (editor.getSketch().hasCodeFolder()) {
			File codeFolder = editor.getSketch().getCodeFolder();
			// javaLibraryPath = codeFolder.getAbsolutePath();

			// get a list of .jar files in the "code" folder
			// (class files in subfolders should also be picked up)
			String codeFolderClassPath = Base.contentsToClassPath(codeFolder);
			// append the jar files in the code folder to the class path
			// classPath += File.pathSeparator + codeFolderClassPath;
			// get list of packages found in those jars
			codeFolderPackages = Base
					.packageListFromClassPath(codeFolderClassPath);

		} else {
			// javaLibraryPath = "";
		}

		// String[] sizeInfo;
		// try {
		// sizeInfo = preprocessor.initSketchSize(editor.getSketch()
		// .getCode(0).getProgram(), false);
		// if (sizeInfo != null) {
		// String sketchRenderer = sizeInfo[3];
		// if (sketchRenderer != null) {
		// if (sketchRenderer.equals("P2D")
		// || sketchRenderer.equals("P3D")
		// || sketchRenderer.equals("OPENGL")) {
		// rawCode.insert(0, "import processing.opengl.*; ");
		// }
		// }
		// }
		//
		// } catch (SketchException e) {
		// System.err.println(e);
		// }
		// PdePreprocessor.parseSketchSize(sketch.getMainProgram(), false);
		StringWriter writer = new StringWriter();
		try {
			PreprocessorResult result = preprocessor.write(writer,
					rawCode.toString(), codeFolderPackages);
			className = result.className;
			prepareImports(result.extraImports);
			sourceCode = writer.getBuffer().toString();
			int position = sourceCode.indexOf("{");
			int lines = 0;
			for (int i = 0; i < position; i++) {
				if (sourceCode.charAt(i) == '\n')
					lines++;
			}
			lines += 3;
			// System.out.println("Lines: " + lines);
			mainClassOffset = lines;
			// System.out.println(writer.getBuffer().toString());
			// + "P5 preproc.\n--"
			// + (System.currentTimeMillis() - lastTimeStamp));
			// System.out.println("Result: " + result.extraImports);
		} catch (RecognitionException e) {
			System.err.println(e);
		} catch (TokenStreamException e) {
			System.err.println(e);
		} catch (SketchException e) {
			System.err.println(e);
		}
		return writer.getBuffer().toString();
	}

	private void compileCheck() {
		try {

			// NOTE TO SELF: If classpath contains null Strings
			// URLClassLoader gets angry. Drops NPE bombs.

			// File f = new File(
			// "resources/CompilationCheckerClasses");
			// System.out.println(1);
			// File f = new File(
			// "E:/WorkSpaces/Eclipse Workspace 2/AST Test 2/bin");
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
			Class<?> checkerClass = Class.forName("CompilationChecker", true,
					classLoader);
			// System.out.println(4);
			CompilationCheckerInterface compCheck = (CompilationCheckerInterface) checkerClass
					.newInstance();

			IProblem[] prob = compCheck.getErrors(className, sourceCode);
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
				if (problem.getID() == IProblem.UnusedImport
						|| problem.getID() == IProblem.MissingSerialVersion)
					continue;
				problems[k++] = problem;
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
				int a[] = calculateTabIndexAndLineNumber(problem);
				errorWindow.problemList.add(new Problem(problem, a[0], a[1]));
			}
			// System.out.println("Total warnings: " + warningCount
			// + ", Total errors: " + errorCount + " , Len: "
			// + prob.length);

		} catch (Exception e) {
			// e.printStackTrace();
			System.err.println("Compilecheck Problem. " + e);
		}
		// System.out.println("Compilecheck, Done.");
		// stopThread();
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
							x = len;
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
					.println("Things got messed up in SyntaxCheckerService.calculateTabIndexAndLineNumber()");
		}

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
			System.out.println("-----------PreProcessed----------");
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
				rawCode = new StringBuffer();

				for (SketchCode sc : editor.getSketch().getCode()) {
					if (sc.isExtension("pde")) {
						sc.setPreprocOffset(scPreProcOffset);

						try {

							if (editor.getSketch().getCurrentCode().equals(sc))
								rawCode.append(sc.getDocument().getText(0,
										sc.getDocument().getLength()));
							else {
								rawCode.append(sc.getProgram());

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

		// prepareImports(programImports);

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

		// Add imports back at the top
		// int importOffset = programImports.size() + defaultImports.length;
		// mainClassOffset += importOffset;
		// for (int i = programImports.size() - 1; i >= 0; i--) {
		// sourceAlt = programImports.get(i) + "\n" + sourceAlt;
		// }
		// for (int i = defaultImports.length - 1; i >= 0; i--) {
		// sourceAlt = defaultImports[i] + "\n" + sourceAlt;
		// }

		// System.out.println("-->\n" + sourceAlt + "\n<--");
		// System.out.println("PDE code processed - "
		// + editor.getSketch().getName());
		sourceCode = sourceAlt;
		return sourceAlt;
	}

	private void prepareImports(List<String> programImports) {
		classpathJars = new ArrayList<URL>();
		String entry = "";
		// System.out.println("Imports: " + programImports.size());
		for (String item : programImports) {
			int dot = item.lastIndexOf('.');
			entry = (dot == -1) ? item : item.substring(0, dot);

			// entry = entry.substring(6).trim();
			// System.out.println("Entry--" + entry);
			if (ignorableImport(entry)) {
				System.out.println("Ignoring: " + entry);
				continue;
			}
			Library library = null;
			try {
				library = editor.getMode().getLibrary(entry);
				System.out.println("lib->" + library.getClassPath() + "<-");
				String libraryPath[] = PApplet.split(library.getClassPath()
						.substring(1).trim(), (Base.isWindows() ? ';' : ':'));
				// TODO: Investigate the jar path added twice issue here
				for (int i = 0; i < libraryPath.length / 2; i++) {
					// System.out.println(entry+" ::"+new
					// File(libraryPath[i]).toURI().toURL());
					classpathJars.add(new File(libraryPath[i]).toURI().toURL());
				}
				// System.out.println("-- ");
				// classpath[count] = (new File(library.getClassPath()
				// .substring(1))).toURI().toURL();
				// System.out.println("  found ");
				// System.out.println(library.getClassPath().substring(1));
			} catch (Exception e) {
				if (library == null) {
					System.err.println("XQMODE: Yikes! Can't find \"" + entry
							+ "\" library!");
					return;
				}
				System.out.println("Yikes!" + e);
				System.out.println("Was processing: " + entry);
				// e.printStackTrace();
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
		if (packageName.startsWith("processing.")
				|| packageName.startsWith("java.")) {
			return true;
		}
		return false;
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
			String info = slashAnimation[slashAnimationIndex] + " T:"
					+ (System.currentTimeMillis() - lastTimeStamp) + "ms";
			errorWindow.setTitle("Problems - " + editor.getSketch().getName()
					+ " " + info);
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
		int len = 0; // No. of lines in each tab
		x -= mainClassOffset;
		try {
			for (SketchCode sc : editor.getSketch().getCode()) {
				if (sc.isExtension("pde")) {
					sc.setPreprocOffset(bigCount);

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

					// System.out.println("x,len, CI: " + x + "," + len + ","
					// + codeIndex);

					// Adding + 1 to len because \n gets appended for each
					// sketchcode extracted during processPDECode()
					if (x >= len) {

						// We're in the last tab and the line count is greater
						// than the no.
						// of lines in the tab,
						if (codeIndex >= editor.getSketch().getCodeCount() - 1) {
							editor.getSketch().setCurrentCode(
									editor.getSketch().getCodeCount() - 1);
							x = len;
							break;
						} else {
							x -= len;
							codeIndex++;
						}
					} else {

						if (codeIndex >= editor.getSketch().getCodeCount())
							codeIndex = editor.getSketch().getCodeCount() - 1;
						editor.getSketch().setCurrentCode(codeIndex);
						break;
					}

				}
				bigCount += sc.getLineCount();
			}

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
		// System.out.println("---");
		return offset;
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
