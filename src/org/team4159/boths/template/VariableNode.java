package org.team4159.boths.template;

import java.util.Hashtable;
import org.team4159.boths.util.StringUtils;

class VariableNode extends Node
{
	private static final Hashtable DEFAULT_CONTEXT = new Hashtable ();
	static {
		DEFAULT_CONTEXT.put ("CURLY_OPEN", "{{");
		DEFAULT_CONTEXT.put ("CURLY_CLOSE", "}}");
	}
	
	private final String var;
	private final boolean safe;

	public VariableNode (String var)
	{
		this (var, false);
	}

	public VariableNode (String var, boolean safe)
	{
		this.var = var;
		this.safe = safe;
	}

	public String render (Hashtable context)
	{
		Object obj = context.get (var);
		if (obj == null)
			obj = DEFAULT_CONTEXT.get (var);
		
		if (obj == null)
			return "";
		else if (safe)
			return obj.toString ();
		else
			return StringUtils.htmlEscape (obj.toString ());
	}

}
