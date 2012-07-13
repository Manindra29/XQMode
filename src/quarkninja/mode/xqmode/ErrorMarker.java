package quarkninja.mode.xqmode;
/**
	 * Error markers displayed on the Error Bar.
	 * 
	 * @author Manindra Moharana
	 * 
	 */
	public class ErrorMarker {
		public int y;
		public int type = -1;
		public static final int Error = 1;
		public static final int Warning = 2;
		public Problem problem;

		public ErrorMarker(Problem problem, int y, int type) {
			this.problem = problem;
			this.y = y;
			this.type = type;
		}
	}