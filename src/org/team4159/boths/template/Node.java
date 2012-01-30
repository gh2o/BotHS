package org.team4159.boths.template;

import java.util.Hashtable;

/**
 * A node representing a block of output in the rendering of a {@link Template} instance. 
 */
public abstract class Node
{
	/**
	 * Renders this node.
	 * 
	 * @param context	The context variables of the scope in which this node is located.
	 * @return			The rendered output.
	 */
	public abstract String render (Hashtable context);
}
