package org.team4159.boths;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.team4159.boths.util.LimitingInputStream;
import org.team4159.boths.util.StringUtils;
import com.sun.squawk.io.BufferedReader;

/**
 * The {@link Request} class represents an HTTP request and is passed to
 * {@link Route}s and {@link View}s when handing an HTTP request.
 */
public class Request
{
	private static int maximumRequestSize = 4096;
	private static int maximumPostSize = 65536;
	
	/**
	 * Sets the maximum size of the HTTP message (request line and headers) of a request.
	 * Default is 4096 bytes.
	 * 
	 * @param sz	Maximum size of the HTTP message in bytes.
	 */
	public static void setMaximumRequestSize (int sz)
	{
		maximumRequestSize = sz;
	}
	
	/**
	 * Sets the maximum size of the POST body of a request.
	 * Default is 65536 bytes.
	 * 
	 * @param sz	Maximum size of the POST body in bytes.
	 */
	public static void setMaximumPostSize (int sz)
	{
		maximumPostSize = sz;
	}
	
	/**
	 * The HTTP method of the request, commonly {@code "GET"} or {@code "POST"}.
	 */
	public final String method;
	
	/**
	 * The full path of the request, including the query string.
	 * 
	 * {@code http://www.example.com/file.html?param=value => "/file.html?param=value"}
	 */
	public final String fullPath;
	
	/**
	 * The version of the HTTP request, commonly {@code "HTTP/1.0"} or {@code "HTTP/1.1"}.
	 */
	public final String version;
	
	/**
	 * The path of the request, excluding the query string.
	 * 
	 * {@code http://www.example.com/file.html?param=value => "/file.html"}
	 */
	public final String path;
	
	/**
	 * The query string of the request.
	 * 
	 * {@code http://www.example.com/file.html?param=value => "param=value"}
	 */
	public final String queryString;
	
	/**
	 * If {@code method == "POST"}, this is the raw POST data of the request as a byte array.
	 * If {@code method != "POST"}, this will be null.
	 */
	public final byte[] rawPostData;
	
	private final Hashtable headers = new Hashtable ();
	
	private final Hashtable singleParams = new Hashtable ();
	private final Hashtable multiParams = new Hashtable ();
	
	private final Hashtable singlePosts = new Hashtable ();
	private final Hashtable multiPosts = new Hashtable ();
	
	Request (InputStream is) throws RequestException
	{
		BufferedReader rr = new BufferedReader (
			new InputStreamReader (
				new LimitingInputStream (is, maximumRequestSize)
			), 1
		);
		
		// parse request line and headers
		{
			String firstLine;
			try {
				firstLine = rr.readLine ();
			} catch (IOException e) {
				e.printStackTrace ();
				throw new RequestException ("failed to read request line");
			}
			
			if (firstLine == null)
				throw new RequestException ("EOF at beginning of request");
			
			String[] firstLineElements = StringUtils.splitByWholeSeparator (firstLine, " ");
			if (firstLineElements.length != 3)
				throw new RequestException ("wrong number of elements in first line of HTTP request");
			
			method = firstLineElements[0];
			fullPath = firstLineElements[1];
			version = firstLineElements[2];
			
			if (method.length () == 0 || fullPath.length () == 0 || version.length () == 0)
				throw new RequestException ("bad HTTP first line");
			if (fullPath.charAt (0) != '/')
				throw new RequestException ("HTTP path does not begin with /");
			
			for (;;)
			{
				String headerLine;
				
				try {
					headerLine = rr.readLine ();
				} catch (IOException e) {
					e.printStackTrace ();
					throw new RequestException ("failed to read header line");
				}
				
				if (headerLine == null) // premature death
					throw new RequestException ("headers terminated prematurely");
				if (headerLine.length () == 0) // end of headers
					break;
				
				int separatorLocation = headerLine.indexOf (": ");
				if (separatorLocation == -1)
					throw new RequestException ("separator not found in header entry");
				
				String key = headerLine.substring (0, separatorLocation);
				String value = headerLine.substring (separatorLocation + 2);
				
				headers.put (key.toLowerCase (), value);
			}
		}
		
		// parse the path
		{
			int qsep = fullPath.indexOf ('?');
			if (qsep == -1)
			{
				path = fullPath;
				queryString = "";
			}
			else
			{
				path = fullPath.substring (0, qsep);
				queryString = fullPath.substring (qsep + 1);
			}
			
			if (path.indexOf ("/../") >= 0 || path.indexOf ("/./") >= 0)
				throw new RequestException (". or .. in path");
		}
		
		// parse the query string
		parseEncodedParams (queryString, singleParams, multiParams);
		
		// parse POST data
		if (method.equals ("POST"))
		{
			int contentLength;
			try {
				contentLength = Integer.parseInt (getHeader ("Content-Length"));
			} catch (NumberFormatException e) {
				throw new RequestException ("invalid Content-Length");
			}
			
			if (contentLength < 0)
				throw new RequestException ("negative Content-Length");
			if (contentLength > maximumPostSize)
				throw new RequestException ("Content-Length too large (" + contentLength +" > " + maximumPostSize + ")");
			
			rawPostData = new byte[contentLength];
			try {
				new DataInputStream (is).readFully (rawPostData);
			} catch (IOException e) {
				e.printStackTrace ();
				throw new RequestException ("failed to read POST data");
			}
			
			if (getHeader ("Content-Type").equals ("application/x-www-form-urlencoded"))
				parseEncodedParams (new String (rawPostData), singlePosts, multiPosts);
			// TODO: process multipart/form-data
		}
		else
		{
			rawPostData = null;
		}
	}
	
	private static void parseEncodedParams (String str, Hashtable single, Hashtable multi)
	{
		String[] fragments = StringUtils.splitByWholeSeparator (str, "&");
		for (int i = 0; i < fragments.length; i++)
		{
			String fragment = fragments[i];
			if (fragment.length () == 0)
				continue;
			
			int eq = fragment.indexOf ('=');
			String key, value;
			
			if (eq != -1)
			{
				key = fragment.substring (0, eq);
				value = fragment.substring (eq + 1);
			}
			else
			{
				key = fragment;
				value = "";
			}
			
			key = StringUtils.urlUnquote (key);
			value = StringUtils.urlUnquote (value);
			
			single.put (key, value);
			
			Vector vec = (Vector) multi.get (key);
			if (vec == null)
				multi.put (key, vec = new Vector ());
			vec.addElement (value);
		}
		
		Enumeration keys = multi.keys ();
		while (keys.hasMoreElements ())
		{
			Object key = keys.nextElement ();
			Vector vec = (Vector) multi.get (key);
			int len = vec.size ();
			String[] arr = new String[len];
			multi.put (key, arr);
			
			for (int i = 0; i < len; i++)
				arr[i] = (String) vec.elementAt (i);
		}
	}
	
	/**
	 * Gets a header from the HTTP request given a case-insensitive key.
	 *  
	 * @param key	The name of the value to retrieve.
	 * @return		The value, or null if the value does not exist.
	 */
	public String getHeader (String key)
	{
		return (String) headers.get (key.toLowerCase ());
	}
	
	/**
	 * Gets the value of a given parameter from the query string.
	 * 
	 * @param key	The URL-decoded key of the parameter. 
	 * @return		The URL-decoded value of the parameter, or null if the value does not exist.
	 */
	public String getParam (String key)
	{
		return (String) singleParams.get (key);
	}
	
	/**
	 * Gets a list of values for a given parameter from the query string.
	 * Useful if HTML checkboxes are used.
	 * 
	 * @param key	The URL-decoded key of the parameter. 
	 * @return		The URL-decoded value of the parameter, or null if the value does not exist.
	 */
	public String[] getParamMulti (String key)
	{
		return (String[]) multiParams.get (key);
	}
	
	/**
	 * Gets the value of a given parameter from the POST data the request.
	 *  
	 * @param key	The URL-decoded key of the parameter. 
	 * @return		The URL-decoded value of the parameter, or null if the value does not exist.
	 */
	public String getPost (String key)
	{
		return (String) singlePosts.get (key);
	}
	
	/**
	 * Gets a list of values for a given parameter from the POST data the request.
	 * Useful if HTML checkboxes are used.
	 * 
	 * @param key	The URL-decoded key of the parameter. 
	 * @return		The URL-decoded value of the parameter, or null if the value does not exist.
	 */
	public String[] getPostMulti (String key)
	{
		return (String[]) multiPosts.get (key);
	}
}
