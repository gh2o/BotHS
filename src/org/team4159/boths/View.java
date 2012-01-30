package org.team4159.boths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The {@link View} class handles requests after being routed by the server.
 */
public abstract class View
{
	/**
	 * Handles the request and returns a {@link Response} object accordingly.
	 * Custom behavior should be implemented by overriding this method.
	 * 
	 * @param req		The {@link Request} object.
	 * @param route	The {@link Route} that routed the request to this {@link View}.
	 * @return			The {@link Response} to be sent back to the client.
	 */
	public abstract Response getResponse (Request req, Route route);
	
	/**
	 * Optionally continues handling the request after a response from {@link #getResponse(Request, Route)}
	 * has been sent to the client.
	 * 
	 * The default implementation does nothing.
	 * 
	 * @param req	The {@link Request} object of the initial request.
	 * @param res	The {@link Response} object as returned by {@link #getResponse(Request, Route)}.
	 * @param is	The raw {@link InputStream} of the socket.
	 * @param os	The raw {@link OutputStream} of the socket.
	 * @throws IOException
	 */
	public void postResponse (Request req, Response res, InputStream is, OutputStream os) throws IOException
	{
	}
}
