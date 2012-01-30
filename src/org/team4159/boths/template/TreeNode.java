package org.team4159.boths.template;

import java.util.Hashtable;
import java.util.Vector;

class TreeNode extends Node
{
	private Vector nodes = new Vector ();

	public void addChild (Node node)
	{
		nodes.addElement (node);
	}

	public String render (Hashtable context)
	{
		StringBuffer ret = new StringBuffer ();
		
		int len = nodes.size ();
		for (int i = 0; i < len; i++)
			ret.append (((Node) nodes.elementAt (i)).render (context));
		
		return ret.toString ();
	}

}
