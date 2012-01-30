package org.team4159.boths;

public class Route
{
	/**
	 * The prefix to which to match the start of the path to if {@link #exactPathMatch} is false,
	 * or the full path (excluding the query string) to match if {@link #exactPathMatch} is true.
	 */
	public final String pathPrefix;
	
	/**
	 * If true, the entire path (rather than just the beginning of the path) of every request
	 * will be compared to {@link #pathPrefix}.
	 * 
	 * <p>
	 * For example, the {@link #pathPrefix} {@code "/abc/"} would match {@code "/abc/def/"}
	 * if and only if {@link #exactPathMatch} is false.
	 * </p>
	 */
	public final boolean exactPathMatch;
	
	/**
	 * The view to use if the request path matches this route. This object will be reused
	 * for each and every request.
	 */
	protected final View view;
	
	/**
	 * The {@link Class} object representing the view to use if the request path matches this route.
	 * A new instance of this class will be created for each and every request.
	 */
	protected final Class viewClass;
	
	/**
	 * Initializes this route with an exact match for the given path.
	 * 
	 * @param path	The path that this {@link Route} will match.
	 */
	protected Route (String path)
	{
		this (path, true);
	}
	
	/**
	 * Initializes this route with an exact or prefix match for the given path.
	 * 
	 * @param pathPrefix			The path or prefix that this {@link Route} will match.
	 * @param exactPathMatch	Whether to match the path exactly or by prefix.
	 */
	protected Route (String pathPrefix, boolean exactPathMatch)
	{
		this.pathPrefix = pathPrefix;
		this.view = null;
		this.viewClass = null;
		this.exactPathMatch = exactPathMatch;
	}
	
	/**
	 * Initializes this route with an exact match for the given path.
	 * 
	 * See {@link #Route(String, View, boolean)} for caveats.
	 * 
	 * @param path	The path that this {@link Route} will match.
	 * @param view	The view to use when this route matches the path.
	 * @see #Route(String, View, boolean)
	 */
	public Route (String path, View view)
	{
		this (path, view, true);
	}
	
	/**
	 * Initializes this route with an exact or prefix match for the given path.
	 * 
	 * The specified view will be re-used for every request from different
	 * threads.
	 * 
	 * <p>
	 * If your view class is not thread-safe, consider using {@link #Route(String, Class)} instead.
	 * </p>
	 * 
	 * @param pathPrefix			The path or prefix that this {@link Route} will match.
	 * @param view					The view to use when this route matches the path.
	 * @param exactPathMatch	Whether to match the path exactly or by prefix.
	 */
	public Route (String pathPrefix, View view, boolean exactPathMatch)
	{
		this.pathPrefix = pathPrefix;
		this.view = view;
		this.viewClass = null;
		this.exactPathMatch = exactPathMatch;
	}
	
	/**
	 * Initializes this route with an exact match for the given path.
	 * 
	 * A new instance of the specified class will be created for each request.
	 * 
	 * @param path			The path that this {@link Route} will match.
	 * @param viewClass	The class of the view to use when this route matches the path.
	 */
	public Route (String path, Class viewClass)
	{
		this (path, viewClass, true);
	}
	
	/**
	 * Initializes this route with an exact or prefix match for the given path.
	 * 
	 * A new instance of the specified class will be created for each request.
	 * 
	 * @param pathPrefix			The path or prefix that this {@link Route} will match.
	 * @param viewClass			The class of the view to use when this route matches the path.
	 * @param exactPathMatch	Whether to match the path exactly or by prefix.
	 */
	public Route (String pathPrefix, Class viewClass, boolean exactPathMatch)
	{
		if (!View.class.isAssignableFrom (viewClass))
			throw new IllegalArgumentException ("class must be a subclass of View");
		
		this.pathPrefix = pathPrefix;
		this.view = null;
		this.viewClass = viewClass;
		this.exactPathMatch = exactPathMatch;
	}
	
	/**
	 * Matches a path to this route. This method may be overridden for custom path matching.
	 * 
	 * @param path	The path to match.
	 * @return		true if this route matches the given path, false otherwise.		
	 */
	public boolean matches (String path)
	{
		if (pathPrefix == null)
			return false;
		
		if (exactPathMatch)
			return path.equals (pathPrefix);
		else
			return path.startsWith (pathPrefix);
	}

	/**
	 * Retrieves the view for handling the given request.
	 * 
	 * <p>The default implementation does the following:</p>
	 * 
	 * <ol>
	 * <li>If {@link #view} is defined, return that.</li>
	 * <li>If {@link #viewClass} is defined, create a new instance of that and return it.</li>
	 * <li>If neither {@link #view} nor {@link #viewClass} are defined, return null.</li>
	 * </ol>
	 * 
	 * @param req	The request object, guaranteed to match this route.
	 * @return		The view for processing the given request.
	 */
	public View getView (Request req)
	{
		if (view != null)
			return view;
		
		if (viewClass != null)
		{
			try {
				return (View) viewClass.newInstance ();
			} catch (InstantiationException e) {
				System.err.println ("instantiation of " + viewClass +
					" failed, ensure that the class has a constructor that takes zero parameters");
				e.printStackTrace();
			} catch (IllegalAccessException e)
			{
				System.err.println ("failed to access " + viewClass +
					", ensure that the class is public");
				e.printStackTrace();
			}
		}
		
		return null;
	}
}
