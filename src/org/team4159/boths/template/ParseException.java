package org.team4159.boths.template;

/**
 * Thrown if there is a syntax error in the template source or if some
 * other error occurs while parsing the template source. 
 */
public class ParseException extends TemplateException
{
	public ParseException ()
	{
		super ();
	}
	
	public ParseException (String msg)
	{
		super (msg);
	}

	public ParseException (String msg, String filename, int line, int column)
	{
		super (msg + " (" + filename +": line " + line + ", column " + column + ")");
	}
}
