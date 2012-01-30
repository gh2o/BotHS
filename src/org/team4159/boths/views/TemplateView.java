package org.team4159.boths.views;

import java.util.Hashtable;
import org.team4159.boths.Request;
import org.team4159.boths.Response;
import org.team4159.boths.Route;
import org.team4159.boths.View;
import org.team4159.boths.template.Template;

/**
 * This view renders a template for each request.  
 */
public class TemplateView extends View
{
	private Template template;
	
	/**
	 * Creates a new {@link TemplateView}.
	 * 
	 * @param filename
	 * The path to the template file relative to this class
	 * or relative to the root package if it starts with "/".
	 * @see Template#load(Object, String)
	 */
	public TemplateView (String filename)
	{
		template = Template.load (this, filename);
	}

	public Response getResponse (Request req, Route route)
	{
		return template.renderToResponse (getContext (req));
	}
	
	/**
	 * Gets the context variables for rendering the template.
	 * Subclasses should override this.
	 * 
	 * @param req The request to process.
	 * @return A {@link Hashtable} of context variables.
	 */
	public Hashtable getContext (Request req)
	{
		return null;
	}
}