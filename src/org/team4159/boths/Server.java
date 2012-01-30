package org.team4159.boths;

import java.io.IOException;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.ServerSocketConnection;
import javax.microedition.io.StreamConnection;

/**
 * <p>Base class of the BotHS framework.</p>
 * 
 * <p>
 * <strong>NOTE:</strong> The FRC cRIO already has a web service running
 * on port 80. Using port 80 will result in an error as the port has
 * already been allocated. 
 * </p>
 * 
 * @author Team 4159
 */
public class Server implements Runnable
{
	private class Worker extends Thread
	{
		public void run ()
		{
			while (keepGoing)
			{
				StreamConnection sc;
				
				// wait for something
				synchronized (workerLock)
				{
					try {
						workerLock.wait ();
					} catch (InterruptedException e) {}
					
					// stop server if stopping
					if (!keepGoing)
						return;
					
					if (workerConnection == null)
					{
						// someone else took it already!
						continue;
					}
					else
					{
						// grab it and run
						sc = workerConnection;
						workerConnection = null;
					}
				}

				try {
					connectionHandler.handleConnection (sc);
				} finally {
					try {
						sc.close ();
					} catch (IOException e) {}
				}
			}
		}
	}
	
	private final ConnectionHandler connectionHandler;
	
	private final int numberOfWorkerThreads; 
	private final Worker[] workers;
	private final Object workerLock = new Object ();
	private StreamConnection workerConnection;
	
	private Thread thread;
	private boolean keepGoing;
	private int port;
	
	/**
	 * URL routes to match paths to while dealing with requests.
	 */
	protected final Vector routes = new Vector ();
	
	/**
	 * Initializes an {@link Server} instance on port 8080.
	 */
	public Server ()
	{
		this (8080);
	}
	
	/**
	 * Initializes an {@link Server} instance on a given port.
	 * 
	 * @param port
	 * The TCP port the server will run on.
	 */
	public Server (int port)
	{
		this (port, 4);
	}
	
	/**
	 * Initializes a {@link Server} instance on a given port
	 * with a set number of worker threads. Increasing the number
	 * of worker threads will increase concurrency at the expense of system resources.
	 * 
	 * @param port
	 * The TCP port the server will run on.
	 * 
	 * @param numberOfWorkerThreads
	 * The number of worker threads to create.
	 */
	public Server (int port, int numberOfWorkerThreads)
	{
		this.numberOfWorkerThreads = numberOfWorkerThreads;
		this.workers = new Worker[numberOfWorkerThreads];
		this.connectionHandler = new ConnectionHandler (this); // this needs routes, initialize it here
		setPort (port);
	}
	
	/**
	 * Sets the number of the TCP port the server will run on.
	 * 
	 * <p>Do not call this method while the server is running.</p>
	 * 
	 * @param port
	 * The TCP port the server will run on.
	 */
	public void setPort (int port)
	{
		if (thread != null)
			throw new IllegalStateException ("setPort must not be called while the server is running");
		if (port < 1 || port > 65535)
			throw new IllegalArgumentException ("port must be between 1-65535 inclusive");
		this.port = port;
	}
	
	/**
	 * Gets the number of the TCP port the server will run on.
	 * 
	 * @return The TCP port the server will run on.
	 */
	public int getPort ()
	{
		return port;
	}
	
	/**
	 * Adds a route to the route list.
	 * 
	 * @param route
	 * The route to add.
	 */
	public Route addRoute (Route route)
	{
		if (!routes.contains (route))
			routes.addElement (route);
		return route;
	}
	
	public Route addRoute (String pathPrefix, View view)
	{
		return addRoute (new Route (pathPrefix, view));
	}
	
	public Route addRoute (String pathPrefix, View view, boolean exactPathMatch)
	{
		return addRoute (new Route (pathPrefix, view, exactPathMatch));
	}
	
	/**
	 * Removes a route from the route list.
	 * 
	 * @param route
	 * The route to remove.
	 */
	public void removeRoute (Route route)
	{
		routes.removeElement (route);
	}
	
	/**
	 * Checks if the server is currently running. 
	 * 
	 * @return true if the server is currently running, false otherwise.
	 */
	public synchronized boolean isRunning ()
	{
		return (thread != null) && thread.isAlive ();
	}
	
	/**
	 * Starts the server.
	 */
	public synchronized void start ()
	{
		if (isRunning ())
			throw new IllegalStateException ("server already started");
		keepGoing = true;
		(thread = new Thread (this)).start ();
	}
	
	/**
	 * Stops the server.
	 */
	public synchronized void stop ()
	{
		if (!isRunning ())
			throw new IllegalStateException ("server already stopped");
		
		keepGoing = false;
		try {
			Connector.open ("socket://127.0.0.1:" + port).close ();
		} catch (IOException e) {}
		try {
			thread.join ();
		} catch (InterruptedException e) {}
		thread = null;
	}
	
	/**
	 * Implementation of {@link Thread#run()} for request handler.
	 * 
	 * <p>Do not call this method directly. Instead, call {@link #start()},
	 * which will start the request handling loop in the background.</p>
	 */
	public void run ()
	{
		try {
			
			// start worker threads
			for (int i = 0; i < numberOfWorkerThreads; i++)
				(workers[i] = new Worker ()).start ();
			
			// open connection
			ServerSocketConnection server;
			try {
				server = (ServerSocketConnection) Connector.open ("socket://:" + port);
			} catch (Throwable e) {
				System.err.println ("failed to start HTTP server!");
				e.printStackTrace();
				return;
			}
			
			// run!
			try {
				_run (server);
			} catch (Throwable e) {
				System.err.println ("exception in main loop of HTTP server!");
				e.printStackTrace ();
				return;
			}
			
		} finally {
			
			keepGoing = false;
			
			synchronized (workerLock) {
				workerLock.notifyAll ();
			}
			
			for (int i = 0; i < numberOfWorkerThreads; i++)
				if (workers[i] != null)
					for (;;) {
						try {
							workers[i].join ();
							break;
						} catch (InterruptedException e) {}
					}
		}
	}
	
	private void _run (ServerSocketConnection server) throws Throwable
	{
		while (keepGoing)
		{
			StreamConnection sc = server.acceptAndOpen ();
			
			// stop server if stopping
			if (!keepGoing)
			{
				sc.close ();
				return;
			}
			
			// send it for processing
			synchronized (workerLock) {
				// wait for someone to take it
				while (workerConnection != null)
					workerLock.wait (2);
				workerConnection = sc;
				workerLock.notify ();
			}
		}
	}
}
