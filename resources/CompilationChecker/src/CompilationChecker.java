import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jface.text.Document;

public class CompilationChecker {
	/**
	 * ICompilationUnit implementation
	 */
	private class CompilationUnitImpl implements ICompilationUnit {

		private CompilationUnit unit;

		CompilationUnitImpl(CompilationUnit unit) {
			this.unit = unit;
		}

		public char[] getContents() {
			char[] contents = null;
			try {
				Document doc = new Document();
				if (readFromFile)
					doc.set(readFile());
				else
					doc.set(sourceText);
				// TextEdit edits = unit.rewrite(doc, null);
				// edits.apply(doc);
				String sourceCode = doc.get();
				if (sourceCode != null)
					contents = sourceCode.toCharArray();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return contents;
		}

		public char[] getMainTypeName() {
			TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);
			return classType.getName().getFullyQualifiedName().toCharArray();
		}

		public char[][] getPackageName() {
			String[] names = getSimpleNames(this.unit.getPackage().getName()
					.getFullyQualifiedName());
			char[][] packages = new char[names.length][];
			for (int i = 0; i < names.length; ++i)
				packages[i] = names[i].toCharArray();

			return packages;
		}

		public char[] getFileName() {
			TypeDeclaration classType = (TypeDeclaration) unit.types().get(0);
			String name = classType.getName().getFullyQualifiedName() + ".java";
			return name.toCharArray();
		}
	}

	/**
	 * ICompilerRequestor implementation
	 */
	private class CompileRequestorImpl implements ICompilerRequestor {

		private List<IProblem> problems;
		private List<ClassFile> classes;

		public CompileRequestorImpl() {
			this.problems = new ArrayList<IProblem>();
			this.classes = new ArrayList<ClassFile>();
		}

		public void acceptResult(CompilationResult result) {
			boolean errors = false;
			if (result.hasProblems()) {
				IProblem[] problems = result.getProblems();
				for (int i = 0; i < problems.length; i++) {
					if (problems[i].isError())
						errors = true;

					this.problems.add(problems[i]);
				}
			}
			if (!errors) {
				ClassFile[] classFiles = result.getClassFiles();
				for (int i = 0; i < classFiles.length; i++)
					this.classes.add(classFiles[i]);
			}
		}

		List<IProblem> getProblems() {
			return this.problems;
		}

		List<ClassFile> getResults() {
			return this.classes;
		}
	}

	/**
	 * INameEnvironment implementation
	 */
	private class NameEnvironmentImpl implements INameEnvironment {

		private ICompilationUnit unit;
		private String fullName;

		NameEnvironmentImpl(ICompilationUnit unit) {
			this.unit = unit;
			this.fullName = CharOperation.toString(this.unit.getPackageName())
					+ "." + new String(this.unit.getMainTypeName());
		}

		public NameEnvironmentAnswer findType(char[][] compoundTypeName) {
			return findType(CharOperation.toString(compoundTypeName));
		}

		public NameEnvironmentAnswer findType(char[] typeName,
				char[][] packageName) {
			String fullName = CharOperation.toString(packageName);
			if (typeName != null) {
				if (fullName.length() > 0)
					fullName += ".";

				fullName += new String(typeName);
			}
			return findType(fullName);
		}

		public boolean isPackage(char[][] parentPackageName, char[] packageName) {
			String fullName = CharOperation.toString(parentPackageName);
			if (packageName != null) {
				if (fullName.length() > 0)
					fullName += ".";

				fullName += new String(packageName);
			}
			if (findType(fullName) != null)
				return false;

			try {
				return (getClass().getClassLoader().loadClass(fullName) == null);
			} catch (ClassNotFoundException e) {
				return true;
			}
		}

		public void cleanup() {
		}

		private NameEnvironmentAnswer findType(String fullName) {
			if (this.fullName.equals(fullName))
				return new NameEnvironmentAnswer(unit, null);

			try {
				InputStream is = getClass().getClassLoader()
						.getResourceAsStream(
								fullName.replace('.', '/') + ".class");
				if (is != null) {
					byte[] buffer = new byte[8192];
					int bytes = 0;
					ByteArrayOutputStream os = new ByteArrayOutputStream(
							buffer.length);
					while ((bytes = is.read(buffer, 0, buffer.length)) > 0)
						os.write(buffer, 0, bytes);

					os.flush();
					ClassFileReader classFileReader = new ClassFileReader(
							os.toByteArray(), fullName.toCharArray(), true);
					return new NameEnvironmentAnswer(classFileReader, null);
				}
				return null;
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (ClassFormatException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * ClassLoader implementation
	 */
	private class CustomClassLoader extends ClassLoader {

		private Map classMap;

		CustomClassLoader(ClassLoader parent, List classesList) {
			this.classMap = new HashMap();
			for (int i = 0; i < classesList.size(); i++) {
				ClassFile classFile = (ClassFile) classesList.get(i);
				String className = CharOperation.toString(classFile
						.getCompoundName());
				this.classMap.put(className, classFile.getBytes());
			}
		}

		public Class findClass(String name) throws ClassNotFoundException {
			byte[] bytes = (byte[]) this.classMap.get(name);
			if (bytes != null)
				return defineClass(name, bytes, 0, bytes.length);

			return super.findClass(name);
		}
	};

	private ICompilationUnit generateCompilationUnit() {
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		try {
			parser.setSource("".toCharArray());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Map<String, String> options = JavaCore.getOptions();

		// Ben has decided to move on to 1.6. Yay!
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_6, options);
		parser.setCompilerOptions(options);
		CompilationUnit unit = (CompilationUnit) parser.createAST(null);
		unit.recordModifications();

		AST ast = unit.getAST();

		// Package statement
		// package astexplorer;

		PackageDeclaration packageDeclaration = ast.newPackageDeclaration();
		unit.setPackage(packageDeclaration);
		// unit.se
		packageDeclaration.setName(ast.newSimpleName(fileName));
		// System.out.println("Filename: " + fileName);
		// class declaration
		// public class SampleComposite extends Composite {

		TypeDeclaration classType = ast.newTypeDeclaration();
		classType.setInterface(false);
		// classType.s
		classType.setName(ast.newSimpleName(fileName));
		unit.types().add(classType);
		// classType.setSuperclass(ast.newSimpleName("Composite"));
		return new CompilationUnitImpl(unit);
	}

	public static String fileName = "HelloPeasy";

	public static String readFile() {
		BufferedReader reader = null;
		System.out.println(fileName);
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File("D:/TestStuff/" + fileName
							+ ".java"))));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			StringBuilder ret = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				ret.append(line);
				ret.append("\n");
			}
			return ("package " + fileName + ";\n" + ret.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return null;
	}

	private void compileAndRun(ICompilationUnit unit, boolean runIt) {

		Map settings = new HashMap();
		settings.put(CompilerOptions.OPTION_LineNumberAttribute,
				CompilerOptions.GENERATE);
		settings.put(CompilerOptions.OPTION_SourceFileAttribute,
				CompilerOptions.GENERATE);

		CompileRequestorImpl requestor = new CompileRequestorImpl();
		Compiler compiler = new Compiler(new NameEnvironmentImpl(unit),
				DefaultErrorHandlingPolicies.proceedWithAllProblems(),
				settings, requestor, new DefaultProblemFactory(
						Locale.getDefault()));

		compiler.compile(new ICompilationUnit[] { unit });
		// System.out.println(unit.getContents());
		List problems = requestor.getProblems();
		boolean error = false;
		for (Iterator it = problems.iterator(); it.hasNext();) {
			IProblem problem = (IProblem) it.next();
			StringBuffer buffer = new StringBuffer();
			buffer.append(problem.getMessage());
			buffer.append(" line: ");
			buffer.append(problem.getSourceLineNumber());
			String msg = buffer.toString();
			if (problem.isError()) {
				error = true;
				msg = "Error:\n" + msg + " " + problem.toString();
			} else if (problem.isWarning())
				msg = "Warning:\n" + msg;

			System.out.println(msg);
		}

		if (!error && runIt) {
			try {
				ClassLoader loader = new CustomClassLoader(getClass()
						.getClassLoader(), requestor.getResults());
				String className = CharOperation
						.toString(unit.getPackageName())
						+ "."
						+ new String(unit.getMainTypeName());
				Class clazz = loader.loadClass(className);
				Method m = clazz.getMethod("main",
						new Class[] { String[].class });
				m.invoke(clazz, new Object[] { new String[0] });
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
	private void compileMeQuitely(ICompilationUnit unit, Map compilerSettings) {

		Map settings;
		if (compilerSettings == null) {
			settings = new HashMap();

			settings.put(CompilerOptions.OPTION_LineNumberAttribute,
					CompilerOptions.GENERATE);
			settings.put(CompilerOptions.OPTION_SourceFileAttribute,
					CompilerOptions.GENERATE);
			settings.put(CompilerOptions.OPTION_Source,
					CompilerOptions.VERSION_1_6);
			settings.put(CompilerOptions.OPTION_SuppressWarnings,
					CompilerOptions.DISABLED);
			// settings.put(CompilerOptions.OPTION_ReportUnusedImport,
			// CompilerOptions.IGNORE);
			// settings.put(CompilerOptions.OPTION_ReportMissingSerialVersion,
			// CompilerOptions.IGNORE);
			// settings.put(CompilerOptions.OPTION_ReportRawTypeReference,
			// CompilerOptions.IGNORE);
			// settings.put(CompilerOptions.OPTION_ReportUncheckedTypeOperation,
			// CompilerOptions.IGNORE);
		} else {
			settings = compilerSettings;
		}

		CompileRequestorImpl requestor = new CompileRequestorImpl();
		Compiler compiler = new Compiler(new NameEnvironmentImpl(unit),
				DefaultErrorHandlingPolicies.proceedWithAllProblems(),
				settings, requestor, new DefaultProblemFactory(
						Locale.getDefault()));
		compiler.compile(new ICompilationUnit[] { unit });

		List problems = requestor.getProblems();
		prob = new IProblem[problems.size()];
		int count = 0;
		for (Iterator it = problems.iterator(); it.hasNext();) {
			IProblem problem = (IProblem) it.next();
			prob[count++] = problem;
		}

	}

	private void compileMeQuitely(ICompilationUnit unit) {
		compileMeQuitely(unit, null);
	}

	static private String[] getSimpleNames(String qualifiedName) {
		StringTokenizer st = new StringTokenizer(qualifiedName, ".");
		ArrayList list = new ArrayList();
		while (st.hasMoreTokens()) {
			String name = st.nextToken().trim();
			if (!name.equals("*"))
				list.add(name);
		}
		return (String[]) list.toArray(new String[list.size()]);
	}

	public static void main(String[] args) {
		CompilationChecker cc = new CompilationChecker();
		cc.getErrors("DisplayFrame");
		cc.display();
	}

	public void display() {
		boolean error = false;
		int errorCount = 0, warningCount = 0, count = 0;
		for (int i = 0; i < prob.length; i++) {
			IProblem problem = prob[i];
			if (problem == null)
				continue;
			StringBuffer buffer = new StringBuffer();
			buffer.append(problem.getMessage());
			buffer.append(" | line: ");
			buffer.append(problem.getSourceLineNumber());
			String msg = buffer.toString();
			if (problem.isError()) {
				error = true;
				msg = "Error: " + msg;
				errorCount++;
			} else if (problem.isWarning()) {
				msg = "Warning: " + msg;
				warningCount++;
			}
			System.out.println(msg);
			prob[count++] = problem;
		}

		if (!error) {
			System.out.println("====================================");
			System.out.println("    Compiled without any errors.    ");
			System.out.println("====================================");
		} else {
			System.out.println("====================================");
			System.out.println(" Compilation failed. You erred man! ");
			System.out.println("====================================");

		}
		System.out.print("Total warnings: " + warningCount);
		System.out.println(", Total errors: " + errorCount);
	}

	IProblem[] prob;

	public IProblem[] getErrors(String name) {
		fileName = name;
		compileMeQuitely(generateCompilationUnit());
		// System.out.println("getErrors()");

		return prob;
	}

	public Object[][] getErrorsAsObjArr(String sourceName, String source,
			Map settings) {
		fileName = sourceName;
		readFromFile = false;
		sourceText = "package " + fileName + ";\n" + source;

		compileMeQuitely(generateCompilationUnit(), settings);
		// System.out.println("getErrors(), Done.");

		if (prob.length > 0) {
			Object[][] data = new Object[prob.length][10];
			for (int i = 0; i < data.length; i++) {
				IProblem p = prob[i];
				// data[i] = new
				// Object[]{p.getMessage(),p.getSourceLineNumber(),p.isError(),p};
				data[i] = new Object[] { p.getOriginatingFileName(),
						p.getMessage(), p.getID(), p.getArguments(), 0,
						p.getSourceStart(), p.getSourceEnd(),
						p.getSourceLineNumber(), p.isError(), p.isWarning() };

			}

			return data;
		}
		return null;
	}

	private boolean readFromFile = true;
	String sourceText = "";

	public IProblem[] getErrors(String sourceName, String source) {
		return getErrors(sourceName, source, null);
	}

	@SuppressWarnings("rawtypes")
	public IProblem[] getErrors(String sourceName, String source, Map settings) {
		fileName = sourceName;
		readFromFile = false;
		sourceText = "package " + fileName + ";\n" + source;

		compileMeQuitely(generateCompilationUnit(), settings);
		// System.out.println("getErrors(), Done.");
		return prob;
	}

	public CompilationChecker() {
		// System.out.println("Compilation Checker initialized.");
	}
}
