package org.team4159.boths.views;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import org.team4159.boths.Request;
import org.team4159.boths.Response;
import org.team4159.boths.Route;
import org.team4159.boths.View;

/**
 * This view serves files from a resource directory.
 * 
 * <p>
 * If you have a folder in your {@code resources} folder named "www"
 * and have a file in it named "abc.html",
 * you can define a {@link Route} with "/files/" as the path prefix;
 * that file can now be accessed at http://address/files/abc.html.
 * </p>
 */
public class DirectoryView extends View
{
	private static final String[] MIME_TYPES_ARRAY = {
		"text/html", "htm", "html",
		"text/css", "css",
		"application/javascript", "js",
		"image/jpeg", "jpe", "jpg", "jpeg",
		"image/png", "png",
		"image/gif", "gif"
	};
	
	private static final Hashtable MIME_TYPES = new Hashtable ();
	
	static {
		String currentMimeType = null;
		for (int i = 0; i < MIME_TYPES_ARRAY.length; i++)
		{
			String value = MIME_TYPES_ARRAY[i];
			if (value.indexOf ('/') >= 0) // value = mime
				currentMimeType = value;
			else // value = extension
				MIME_TYPES.put (value, currentMimeType);
		}
	}
	
	private String directory;
	
	/**
	 * Creates a new {@link DirectoryView}.
	 * 
	 * @param dirPath The path to serve the files from.
	 */
	public DirectoryView (String dirPath)
	{
		directory = dirPath;
		if (!directory.endsWith ("/"))
			directory += "/";
	}

	public Response getResponse (Request req, Route route)
	{
		if (!req.path.startsWith (route.pathPrefix))
			throw new IllegalArgumentException ("bad request path for view");
		
		String subpath = req.path.substring (route.pathPrefix.length ());
		if (subpath.startsWith ("/"))
			subpath = subpath.substring (1);
		
		String mimeType = "application/octet-stream";
		int dotPosition = subpath.lastIndexOf ('.');
		if (dotPosition >= 0)
		{
			String ext = subpath.substring (dotPosition + 1);
			String newMimeType = (String) MIME_TYPES.get (ext);
			if (newMimeType != null)
				mimeType = newMimeType;
		}
		
		String path = directory + subpath;
		InputStream is = getClass ().getResourceAsStream (path);
		System.out.println (is);
		if (is == null)
			return Response.createErrorResponse (404);
		
		Response res = new Response (null, mimeType);
		
		try {
			int k;
			while ((k = is.read ()) != -1)
				res.write (k);
		} catch (IOException e) {
			e.printStackTrace ();
			return Response.createErrorResponse (500);
		} finally {
			try {
				is.close ();
				res.close ();
			} catch (IOException e) {}
		}
		
		return res;
	}
}