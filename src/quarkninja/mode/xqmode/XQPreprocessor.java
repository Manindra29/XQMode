package quarkninja.mode.xqmode;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

public class XQPreprocessor {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

	private ASTRewrite rewrite = null;

	public String doYourThing(String source) {

		Document doc = new Document(source);

		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setSource(doc.get().toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		@SuppressWarnings("unchecked")
		Map<String, String> options = JavaCore.getOptions();

		// Ben has decided to move on to 1.6. Yay!
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_6, options);
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_6);
		parser.setCompilerOptions(options);
		CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		cu.recordModifications();
		rewrite = ASTRewrite.create(cu.getAST());
		cu.accept(new XQASTVisitor());
		System.out.println("------------XQPreProc-----------------");
		TextEdit edits = cu.rewrite(doc, null);
		try {
			edits.apply(doc);
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		System.out.println(doc.get());
		System.out.println("------------XQPreProc End-----------------");
		return doc.get();
	}

	private class XQASTVisitor extends ASTVisitor {
		public boolean visit(MethodDeclaration node) {
			if (node.modifiers().size() == 1)
				System.out.println(node.modifiers().get(0).getClass()
						.getCanonicalName());
			if (node.modifiers().size() == 0 && !node.isConstructor()) {
				// rewrite.set(node, node.getModifiersProperty(),
				// Modifier.PUBLIC,
				// null);
				// rewrite.getListRewrite(node,
				// node.getModifiersProperty()).insertLast(Modifier., null)
				List newMod = rewrite.getAST().newModifiers(Modifier.PUBLIC);
				node.modifiers().add(newMod.get(0));
			}
			return true;
		}

		public boolean visit(NumberLiteral node) {
			if (!node.getToken().endsWith("f")
					&& !node.getToken().endsWith("d")) {
				for (int i = 0; i < node.getToken().length(); i++) {
					if (node.getToken().charAt(i) == '.') {

						String s = node.getToken() + "f";
						node.setToken(s);
						break;
					}
				}
			}
			return true;
		}

		public boolean visit(FieldDeclaration node) {
			if (node.getType().toString().equals("color"))
				node.setType(rewrite.getAST().newPrimitiveType(
						PrimitiveType.INT));
			return true;
		}

		public boolean visit(VariableDeclarationStatement node) {
			if (node.getType().toString().equals("color"))
				node.setType(rewrite.getAST().newPrimitiveType(
						PrimitiveType.INT));
			return true;
		}
	}

}
