package org.team4159.boths;

import javax.microedition.io.StreamConnection;
import java.io.*;
import java.util.Vector;

class ConnectionHandler
{
	Vector routes;

	ConnectionHandler (Server server)
	{
		this.routes = server.routes;
	}
	
	void handleConnection (StreamConnection sc)
	{
		InputStream is = null;
		OutputStream os = null;
		
		try {
			is = sc.openInputStream ();
			os = sc.openOutputStream ();
			handleConnection (is, os);
		} catch (IOException e) {
			e.printStackTrace ();
			return;
		} finally {
			try {
				is.close ();
				os.close ();
			} catch (IOException e) {}
		}
	}
	
	void handleConnection (InputStream is, OutputStream os)
	{
		Request req;
		View view;
		Response res;
		
		try {
			req = new Request (is);
		} catch (RequestException e) {
			e.printStackTrace ();
			sendError (500, os);
			return;
		}
		
		int nroutes = routes.size ();
		Route route = null;
		
		for (int i = 0; i < nroutes; i++)
		{
			Route r = (Route) routes.elementAt (i);
			if (r.matches (req.path))
			{
				route = r;
				break;
			}
		}
		
		if (route == null)
		{
			sendError (404, os);
			return;
		}
		
		view = route.getView (req);
		
		try {
			res = view.getResponse (req, route);
		} catch (Throwable e) {
			System.err.println ("error while processing view");
			e.printStackTrace ();
			sendError (500, os);
			return;
		}
		
		send (res, os);
		
		try {
			view.postResponse (req, res, is, os);
		} catch (IOException e) {
			e.printStackTrace ();
		}
	}

	void send (Response res, OutputStream os)
	{
		try {
			res.writeResponseToOutputStream (os);
		} catch (Throwable e) {
			System.err.println ("failed to send response to client");
			e.printStackTrace ();
			return;
		}
	}

	void sendError (int code, OutputStream os)
	{
		send (Response.createErrorResponse (code), os);
	}
}