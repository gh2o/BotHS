package org.team4159.boths.template;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import org.team4159.boths.Response;
import org.team4159.boths.template.Lexer.Token;
import org.team4159.boths.template.Lexer.TokenTypes;

/**
 * The {@link Template} class represents an HTML template page.
 * 
 * The syntax for specifying variables is loosely based off of Django's templating system.
 * However, at the current moment, only variables are supported; tags and filters are not yet supported.
 * 
 * <p>
 * If text in the form of {@code {{var}}} appears in the template source,
 * they will be replaced by context variables passed to the {@link #render()} method.
 * </p>
 * 
 * <p>
 * Note that because this template is designed to render HTML, all variables will
 * automatically be escaped (&lt; becomes &amp;lt;, &gt; becomes &amp;gt;, etc.).
 * To avoid automatic escaping, add an exclamation point (!) after the variable name: {@code {{var!}}}
 * </p>
 * 
 * @see <a href="https://www.djangoproject.com/">Django Website</a>
 */
public class Template
{
	private static final Hashtable EMPTY_CONTEXT = new Hashtable ();
	
	private Node rootNode;
	
	/**
	 * Creates a new template.
	 * 
	 * @param tmplString The source of the template as a string.
	 * @throws ParseException if the template file cannot be parsed.
	 */
	public Template (String tmplString)
	{
		this (tmplString, null);
	}
	
	/**
	 * Creates a new template, specifying a filename for debugging purposes.
	 * 
	 * @param tmplString	The source of the template as a string.
	 * @param filename	The filename from which the source was originated.
	 * @throws ParseException if the template file cannot be parsed.
	 */
	public Template (String tmplString, String filename)
	{
		rootNode = parse (tmplString, filename);
	}
	
	/**
	 * Renders the template.
	 * 
	 * @param context	A {@link Hashtable} of context variables.
	 * @return			The rendered output as a {@link String}.
	 */
	public String render (Hashtable context)
	{
		if (context == null)
			context = EMPTY_CONTEXT;
		return rootNode.render (context);
	}
	
	/**
	 * Renders the template with no context variables.
	 * 
	 * @return The rendered output as a {@link String}.
	 */
	public String render ()
	{
		return render (null);
	}
	
	/**
	 * Renders the template to a {@link Response} object.
	 * 
	 * @param context	A {@link Hashtable} of context variables.
	 * @return			The {@link Response} object with the rendered template content.
	 */
	public Response renderToResponse (Hashtable context)
	{
		return new Response (render (context));
	}
	
	/**
	 * Renders the template to a {@link Response} object with no context variables.
	 * 
	 * @return			The {@link Response} object with the rendered template content.
	 */
	public Response renderToResponse ()
	{
		return renderToResponse (null);
	}
	
	private Node parse (String tmpl, String filename)
	{
		TreeNode node = new TreeNode ();
		Lexer lexer = new Lexer (tmpl, filename);
		
		while (lexer.hasMoreTokens ())
		{
			Token token = lexer.nextToken ();
			switch (token.type)
			{
				
				case TokenTypes.TEXT:
					node.addChild (new TextNode (token.text));
					break;
					
				case TokenTypes.START_VARIABLE:
					
					Token varToken = lexer.expectToken (TokenTypes.VARIABLE);
					
					boolean safe;
					if (safe = (lexer.peekToken ().type == TokenTypes.VARIABLE_SAFE))
						lexer.nextToken (); // consume safety token
					
					lexer.expectToken (TokenTypes.END_VARIABLE);
					
					node.addChild (new VariableNode (varToken.text, safe));
					break;
					
				default:
					throw new ParseException ("unrecognized token type");
			}
		}
		
		return node;
	}
	
	/**
	 * Loads a template from the specified {@link InputStream}.
	 * 
	 * @param is			The {@link InputStream} from which to load the template.
	 * @param filename	Filename to use for debugging purposes.
	 * @return				A newly-created {@link Template} object.
	 * @throws ParseException if the template file cannot be parsed.
	 */
	public static Template load (InputStream is, String filename) throws IOException
	{
		if (is == null)
			throw new NullPointerException ("input stream must not be null");
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream ();
		
		int k;
		while ((k = is.read ()) != -1)
			baos.write (k);
		baos.flush ();
		
		return new Template (new String (baos.toByteArray ()), filename);
	}
	
	/**
	 * Loads a template from the specified filename, relative to the package
	 * in which the given class is located.
	 * 
	 * If the filename starts with "/", locate the template file relative
	 * to the root package. In other words, if the template is located in 
	 * {@code index.html} in your {@code resources} directory, the filename
	 * should be {@code "index.html"}.
	 * 
	 * @param cls 			A class in the package in which the template file will be located.
	 * @param filename	The path to the template file.
	 * @return				A newly-created {@link Template} object.
	 * @throws TemplateException if the template file is not found or failed to load.
	 * @throws ParseException if parsing failed
	 */
	public static Template load (Class cls, String filename)
	{
		InputStream is = cls.getResourceAsStream (filename);
		if (is == null)
			throw new TemplateException ("resource of name " + filename + " for class " + cls.getName () + " not found");
		try {
			return load (is, filename);
		} catch (IOException e) {
			throw new TemplateException ("failed to load " + filename + ": " + e);
		}
	}
	
	public static Template load (Object obj, String filename)
	{
		return load (obj.getClass (), filename);
	}
}