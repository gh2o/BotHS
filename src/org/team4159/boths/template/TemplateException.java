package org.team4159.boths.template;

/**
 * Thrown if there a template file fails to load or if some other
 * template-related error occurs.
 */
public class TemplateException extends RuntimeException
{
	public TemplateException ()
	{
		super ();
	}
	
	public TemplateException (String msg)
	{
		super (msg);
	}
}
