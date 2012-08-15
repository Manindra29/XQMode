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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.compiler.IProblem;

/**
 * Makes IProblem error messages jargon free. 
 * 
 * @author Manindra Moharana &lt;mkmoharana29@gmail.com&gt;
 * 
 */
public class ProblemsFilter {

	static Pattern pattern;
	static Matcher matcher;

	static final String tokenRegExp = "\\b token\\b";

	public static String process(IProblem problem) {
		return process(problem.getMessage());
	}

	/**
	 * Processes error messages and attempts to make them a bit more english like. 
	 * Currently performs:
	 * <li>Remove all instances of token. "Syntax error on token 'blah', delete this token"
	 * becomes "Syntax error on 'blah', delete this"
	 * @param message - The message to be processed
	 * @return String - The processed message
	 */
	public static String process(String message) {
		// Remove all instances of token
		// "Syntax error on token 'blah', delete this token"

		pattern = Pattern.compile(tokenRegExp);
		matcher = pattern.matcher(message);
		message = matcher.replaceAll("");

		// Split camel case words into separate words. 
		// "VaraibleDeclaration" becomes "Variable Declaration"
		// But sadly "PApplet" become "P Applet" and so on.

		// StringTokenizer st = new StringTokenizer(message);
		// String newMessage = "";
		// while (st.hasMoreTokens()) {
		// String word = st.nextToken();
		// newMessage += splitCamelCaseWord(word) + " ";
		// }
		// message = new String(newMessage);

		return message;
	}

	public static String splitCamelCaseWord(String word) {
		String newWord = "";
		for (int i = 1; i < word.length(); i++) {
			if (Character.isUpperCase(word.charAt(i))) {
				// System.out.println(word.substring(0, i) + " "
				// + word.substring(i));
				newWord += word.substring(0, i) + " ";
				word = word.substring(i);
				i = 1;
			}
		}
		newWord += word;
		// System.out.println(newWord);
		return newWord.trim();
	}

	public static void main(String[] args) {
		System.out
				.println(process("Syntax error on token 'blah', delete this token IfStatement"));
		// splitWord("Ifassdsdf");

	}

}
