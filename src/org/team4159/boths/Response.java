package org.team4159.boths;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Hashtable;
import org.team4159.boths.template.Template;
import org.team4159.boths.util.FlushingOutputStreamWriter;

/**
 * The {@link Response} class is returned by {@link View}s containing the headers and content to be
 * sent back to the client.
 * 
 * <p>
 * Since this class is a subclass of {@link ByteArrayOutputStream}, one may to use this {@link Response}
 * as if it were an {@link OutputStream}. However, streaming is not supported; all writes to an instance
 * to this class will be cached in memory before being sent to the client.
 * </p>
 * 
 * @author Team 4159
 */
public class Response extends ByteArrayOutputStream
{
	private static final String DEFAULT_CONTENT_TYPE = "text/html; charset=utf-8";
	
	static final Hashtable HTTP_STATUS_MESSAGES = new Hashtable ();
	private static void addSM (int code, String msg) { HTTP_STATUS_MESSAGES.put (new Integer (code), msg); }
	static {
		addSM (100, "Continue");
		addSM (101, "Switching Protocols");
		addSM (200, "OK");
		addSM (201, "Created");
		addSM (202, "Accepted");
		addSM (203, "Non-Authoritative Information");
		addSM (204, "No Content");
		addSM (205, "Reset Content");
		addSM (206, "Partial Content");
		addSM (300, "Multiple Choices");
		addSM (301, "Moved Permanently");
		addSM (302, "Found");
		addSM (303, "See Other");
		addSM (304, "Not Modified");
		addSM (305, "Use Proxy");
		addSM (307, "Temporary Redirect");
		addSM (400, "Bad Request");
		addSM (401, "Unauthorized");
		addSM (402, "Payment Required");
		addSM (403, "Forbidden");
		addSM (404, "Not Found");
		addSM (405, "Method Not Allowed");
		addSM (406, "Not Acceptable");
		addSM (407, "Proxy Authentication Required");
		addSM (408, "Request Timeout");
		addSM (409, "Conflict");
		addSM (410, "Gone");
		addSM (411, "Length Required");
		addSM (412, "Precondition Failed");
		addSM (413, "Request Entity Too Large");
		addSM (414, "Request-URI Too Long");
		addSM (415, "Unsupported Media Type");
		addSM (416, "Requested Range Not Satisfiable");
		addSM (500, "Internal Server Error");
		addSM (501, "Not Implemented");
		addSM (502, "Bad Gateway");
		addSM (503, "Service Unavailable");
		addSM (504, "Gateway Timeout");
		addSM (505, "HTTP Version Not Supported");
	}
	
	private final Hashtable headers = new Hashtable ();
	private final Hashtable headersRealKeys = new Hashtable ();
	
	private int statusCode = 200;
	
	/**
	 * A Writer that allows character-level writing to this Response.
	 */
	public final Writer writer;
	
	/**
	 * Creates an empty response and sets the content type to text/html.
	 */
	public Response ()
	{
		this (null);
	}

	/**
	 * Creates a response with a HTML string.
	 * 
	 * @param content
	 *           The HTML to be returned to the client.
	 */
	public Response (String content)
	{
		this (content, null);
	}

	/**
	 * Creates a response with the specified content and content type.
	 * 
	 * @param content
	 *           The HTML to be returned to the client.
	 * 
	 * @param content_type
	 *           The MIME type of the returned content.
	 */
	public Response (String content, String content_type)
	{
		this.writer = new FlushingOutputStreamWriter (this);
		
		if (content != null)
			try {
				writer.write (content);
				writer.flush ();
			} catch (IOException e) {
				e.printStackTrace ();
			}
		
		if (content_type == null)
			content_type = DEFAULT_CONTENT_TYPE;
		setHeader ("Content-Type", content_type);
	}
	
	/**
	 * Sets an HTTP header on this response, overwriting it
	 * if it already exists.
	 * 
	 * @param key
	 * The key of the HTTP header.
	 * @param value
	 * The value of the HTTP header.
	 */
	public void setHeader (String key, String value)
	{
		String low = key.toLowerCase ();
		headers.put (low, value);
		headersRealKeys.put (low, key);
	}
	
	/**
	 * Gets an HTTP header from this request.
	 * 
	 * @param key
	 * The key of the HTTP header.
	 * 
	 * @return The value of the HTTP header, or null if it does not exist.
	 */
	public String getHeader (String key)
	{
		return (String) headers.get (key.toLowerCase ());
	}
	
	/**
	 * Deletes an HTTP headers from this request.
	 * 
	 * If this header does not already exist, nothing will happen.
	 * 
	 * @param key
	 * The key of the HTTP header to delete.
	 */
	public void deleteHeader (String key)
	{
		String low = key.toLowerCase ();
		headers.remove (low);
		headersRealKeys.remove (low);
	}
	
	/**
	 * Checks if an HTTP header is present in this request.
	 * 
	 * @param key
	 * The key of the HTTP header to check.
	 */
	public boolean hasHeader (String key)
	{
		return headers.containsKey (key.toLowerCase ());
	}
	
	/**
	 * Sets the HTTP status code of this request.
	 * 
	 * The code must be a number between 100-999 inclusive.
	 * 
	 * @param code
	 * The HTTP status code of the response.
	 */
	public void setStatusCode (int code)
	{
		if (code < 100 || code > 999)
			throw new IllegalArgumentException ("status code must be between 100-999 inclusive");
		this.statusCode = code;
	}
	
	/**
	 * Gets the HTTP status code of this request.
	 * 
	 * @return The HTTP status code of the response.
	 */
	public int getStatusCode ()
	{
		return statusCode;
	}
	
	/**
	 * Gets the HTTP status message of this request.
	 * 
	 * @return The HTTP status message of the response.
	 */
	public String getStatusMessage ()
	{
		return getStatusMessageForStatusCode (statusCode);
	}
	
	/**
	 * Returns a string representation of this response instance.
	 */
	public String toString ()
	{
		return getClass ().getName () + "@" + Integer.toHexString (hashCode ());
	}
	
	/**
	 * Prepares the response for output by adding various necessary headers.
	 */
	public void prepare ()
	{
		if (!hasHeader ("Connection"))
			setHeader ("Connection", "close");
	}
	
	/**
	 * Writes the entire HTTP response (including the headers) of this response to
	 * an {@link OutputStream}, calling {@link #prepare()} if it has not been called
	 * already.
	 * 
	 * @param os		The {@link OutputStream} to which the response shall be written.
	 * @throws IOException
	 */
	public void writeResponseToOutputStream (OutputStream os) throws IOException
	{
		writeResponseToOutputStream (os, true);
	}
	
	/**
	 * Writes the entire HTTP response (including the headers) of this response to
	 * an {@link OutputStream}.
	 * 
	 * @param os		The {@link OutputStream} to which the response shall be written.
	 * @param prepare	Whether {@link #prepare()} should be called before writing.
	 * @throws IOException
	 */
	public void writeResponseToOutputStream (OutputStream os, boolean prepare) throws IOException
	{
		if (prepare)
			prepare ();
		
		Writer writer = new OutputStreamWriter (os);
		writer.write ("HTTP/1.1" + " " + getStatusCode () + " " + getStatusMessage () + "\r\n");
		
		Enumeration keys = headers.keys ();
		while (keys.hasMoreElements ())
		{
			String key = (String) keys.nextElement ();
			writer.write (headersRealKeys.get (key) + ": " + headers.get (key) + "\r\n");
		}
		
		writer.write ("\r\n");
		writer.flush ();
		
		writeBodyToOutputStream (os);
		os.flush ();
	}
	
	/**
	 * Writes the main body of this response to an {@link OutputStream}.
	 * 
	 * @param os		The {@link OutputStream} to which the body shall be written.
	 * @throws IOException
	 */
	public void writeBodyToOutputStream (OutputStream os) throws IOException
	{
		os.write (toByteArray ());
	}
	
	/**
	 * Gets the generic HTTP status message for a given HTTP status code. 
	 * 
	 * @param code		A HTTP status code.
	 * @return			The generic HTTP status message for the code.
	 */
	public static String getStatusMessageForStatusCode (int code)
	{
		String msg = (String) HTTP_STATUS_MESSAGES.get (new Integer (code));
		if (msg == null)
			return "Unknown Error";
		else
			return msg;
	}

	/**
	 * Creates and returns a generic {@link Response} for errors.
	 * 
	 * @param code		The HTTP status code of the error.
	 * @return			A {@link Response}.
	 */
	public static Response createErrorResponse (int code)
	{
		Hashtable ht = new Hashtable ();
		ht.put ("status_code", new Integer (code));
		ht.put ("status_message", getStatusMessageForStatusCode (code));
		
		Response res = Template.load (Response.class, "error.html").renderToResponse (ht);
		res.setStatusCode (code);
		
		return res;
	}
}