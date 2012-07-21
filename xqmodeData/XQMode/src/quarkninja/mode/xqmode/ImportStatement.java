package quarkninja.mode.xqmode;
public class ImportStatement {
		String importName;
		int tab, lineNumber;

		public ImportStatement(String importName, int tab, int lineNumber) {
			this.importName = importName;
			this.tab = tab;
			this.lineNumber = lineNumber;
		}
	}