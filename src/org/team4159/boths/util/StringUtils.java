package org.team4159.boths.util;

public class StringUtils
{
	/**
	 * Represents a failed index search.
	 */
	public static final int INDEX_NOT_FOUND = -1;
	
	public static String htmlEscape (String input)
	{
		StringBuffer output = new StringBuffer ();
		
		int len = input.length ();
		for (int i = 0; i < len; i++)
		{
			char c = input.charAt (i);
			switch (c)
			{
				case '"':
					output.append ("&quot;");
					break;
				case '&':
					output.append ("&amp;");
					break;
				case '\'':
					output.append ("&apos;");
					break;
				case '<':
					output.append ("&lt;");
					break;
				case '>':
					output.append ("&gt;");
					break;
				default:
					output.append (c);
					break;
			}
		}
		
		return output.toString ();
	}
	
	public static void locate (String str, int pos, int[] out)
	{
		if (out.length != 2)
			throw new IllegalArgumentException ("out must have a length of 2");
		if (pos >= str.length ())
			pos = str.length () - 1;
		
		int line = 1;
		int col = 0;
		
		for (int i = 0; i < pos; i++)
		{
			char c = str.charAt (i + 1);
			if (c == '\n')
			{
				line++;
				col = 0;
			}
			else
			{
				col++;
			}
		}
		
		if (col == 0)
			col = 1;
		
		out[0] = line;
		out[1] = col;
	}
	
	/**
	 * <p>
	 * Checks if a String is empty ("") or null.
	 * </p>
	 * 
	 * <pre>
	 * StringUtils.isEmpty(null)      = true
	 * StringUtils.isEmpty("")        = true
	 * StringUtils.isEmpty(" ")       = false
	 * StringUtils.isEmpty("bob")     = false
	 * StringUtils.isEmpty("  bob  ") = false
	 * </pre>
	 * 
	 * <p>
	 * NOTE: This method changed in Lang version 2.0. It no longer trims the String. That
	 * functionality is available in isBlank().
	 * </p>
	 * 
	 * @param str
	 *           the String to check, may be null
	 * @return <code>true</code> if the String is empty or null
	 */
	public static boolean isEmpty (String str)
	{
		return str == null || str.length () == 0;
	}

	/**
	 * <p>
	 * Counts how many times the substring appears in the larger String.
	 * </p>
	 * 
	 * <p>
	 * A <code>null</code> or empty ("") String input returns <code>0</code>.
	 * </p>
	 * 
	 * <pre>
	 * StringUtils.countMatches(null, *)       = 0
	 * StringUtils.countMatches("", *)         = 0
	 * StringUtils.countMatches("abba", null)  = 0
	 * StringUtils.countMatches("abba", "")    = 0
	 * StringUtils.countMatches("abba", "a")   = 2
	 * StringUtils.countMatches("abba", "ab")  = 1
	 * StringUtils.countMatches("abba", "xxx") = 0
	 * </pre>
	 * 
	 * @param str
	 *           the String to check, may be null
	 * @param sub
	 *           the substring to count, may be null
	 * @return the number of occurrences, 0 if either String is <code>null</code>
	 */
	public static int countMatches (String str, String sub)
	{
		if (isEmpty (str) || isEmpty (sub))
		{
			return 0;
		}
		int count = 0;
		int idx = 0;
		while ((idx = str.indexOf (sub, idx)) != INDEX_NOT_FOUND)
		{
			count++;
			idx += sub.length ();
		}
		return count;
	}

	public static String[] splitByWholeSeparator (String str, String separator)
	{
		int separatorLength = separator.length ();
		
		String[] ret = new String[countMatches (str, separator) + 1];
		int i = 0;
		
		int start = 0;
		int end;
		
		for (;;)
		{
			end = str.indexOf (separator, start);
			if (end != -1)
			{
				ret[i++] = str.substring (start, end);
				start = end + separatorLength;
			}
			else
			{
				ret[i++] = str.substring (start);
				break;
			}
		}
		
		return ret;
	}
	
	private static boolean isHex (char x)
	{
		if (x >= '0' && x <= '9')
			return true;
		if (x >= 'a' && x <= 'f')
			return true;
		if (x >= 'A' && x <= 'F')
			return true;
		return false;
	}
	
	public static String urlUnquote (String input)
	{
		StringBuffer output = new StringBuffer ();
		int inputLength = input.length ();
		
		for (int i = 0; i < inputLength;)
		{
			char c = input.charAt (i++);
			boolean special = false;
			
			switch (c)
			{
				case '+':
					
					output.append (' ');
					special = true;
					break;
					
				case '%':
					
					if (i + 1 >= inputLength)
						break;
					char cHigh = input.charAt (i), cLow = input.charAt (i + 1);
					if (!(isHex (cHigh) && isHex (cLow)))
						break;
					
					output.append ((char) Integer.parseInt (cHigh + "" + cLow, 16));
					i += 2;
					special = true;
					
					break;
			}
			
			if (!special)
				output.append (c);
		}
		
		return output.toString ();
	}
}
