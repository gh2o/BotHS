package org.team4159.boths.views;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.team4159.boths.Request;
import org.team4159.boths.Response;

/**
 * This class allows for individual processing of every WebSocket connection
 * received in this view.
 */
public class BidirectionalWebSocketView extends BaseWebSocketView
{
	/**
	 * Handles bidirectional WebSocket sessions.
	 */
	public static interface Handler
	{
		/**
		 * The main loop of the WebSocket handler. Must return when {@code sock.isOpen()}
		 * starts returning false.
		 * 
		 * @param sock The {@link BidirectionalWebSocket} used to communicate with the client.
		 */
		public void handleBidirectionalWebSocket (BidirectionalWebSocket sock);
	}
	
	/**
	 * This class represents a socket which should be used to communicate with a WebSocket client.
	 */
	public static class BidirectionalWebSocket
	{
		private final Request request;
		private final InputStream is;
		private final OutputStream os;
		private final BidirectionalWebSocketView view;
		private final ByteArrayOutputStream ibaos;
		private final DataOutputStream dos;
		private final FragmentProcessor fp;
		
		private boolean open = true;
		
		BidirectionalWebSocket (BidirectionalWebSocketView view, Request req, InputStream is, OutputStream os)
		{
			this.view = view;
			this.request = req;
			this.is = is;
			this.os = os;
			this.ibaos = new ByteArrayOutputStream ();
			this.dos = new DataOutputStream (os);
			this.fp = new FragmentProcessor (is);
		}
		
		/**
		 * Checks whether the WebSocket connection is still open. The handler
		 * must return when this method returns false.
		 * 
		 * @return true if the WebSocket connection is still open.
		 */
		public boolean isOpen () { return open; }
		
		/**
		 * Checks whether there is a message available in the buffer or
		 * if the client is in the process of sending another message.
		 * 
		 * @return true if a message is available for reading or will be
		 * available for reading shortly.
		 */
		public boolean messageAvailable ()
		{
			if (!open)
				return false;
			
			try {
				return is.available () > 0;
			} catch (IOException e) {
				e.printStackTrace ();
				throw new RuntimeException (e.toString ());
			}
		}
		
		/**
		 * Returns the next message the client sends. If {@link #messageAvailable()}
		 * returns false, this method may block for a substantial amount of time.
		 * 
		 * This method will automatically respond to PING packets and will ignore
		 * PONG packets.
		 * 
		 * @return The next message.
		 */
		public Message nextMessage ()
		{
			Message msg = null;
			
			while (msg == null)
			{
				try {
					msg = view.receiveMessageFromSocket (fp, ibaos);
				} catch (IOException e) {
					e.printStackTrace ();
					throw new RuntimeException (e.toString ());
				}
				
				switch (msg.opcode)
				{
					case OPCODE_CLOSE:
						open = false;
						break;
					case OPCODE_PING:
						sendMessage (new Message (0xa, null));
						msg = null;
						break;
					case OPCODE_PONG:
						msg = null;
						break;
				}
			}
			
			return msg;
		}
		
		/**
		 * Sends a message with binary data.
		 * 
		 * @param data		The binary data to send.
		 */
		public void sendMessage (byte[] data)
		{
			sendMessage (new Message (OPCODE_BINARY, data));
		}
		
		/**
		 * Sends a message with text data.
		 * 
		 * @param str		The text to send.
		 */
		public void sendMessage (String str)
		{
			sendMessage (new Message (OPCODE_TEXT, str.getBytes ()));
		}
		
		/**
		 * Sends a message.
		 * 
		 * @param msg 		The {@link BaseWebSocketView.Message} to send.
		 */
		public void sendMessage (BaseWebSocketView.Message msg)
		{
			try {
				view.sendMessageToSocket (msg, dos);
			} catch (IOException e) {
				e.printStackTrace ();
				throw new RuntimeException (e.toString ());
			}
		}
		
		/**
		 * Closes the WebSocket connection.
		 */
		public void close ()
		{
			sendMessage (new Message (OPCODE_CLOSE, null));
			open = false;
		}
	}
	
	private Handler handler;
	
	/**
	 * Creates a {@link BidirectionalWebSocketView}. Overwrite {@link #handleBidirectionalWebSocket(BidirectionalWebSocket)}
	 * or use {@link #BidirectionalWebSocketView(Handler)} to implement custom behavior.
	 */
	public BidirectionalWebSocketView ()
	{
		this (null);
	}
	
	/**
	 * Creates a {@link BidirectionalWebSocketView}. {@code handler} will be used to handle each connection.
	 * 
	 * @param handler		Connection handler.
	 */
	public BidirectionalWebSocketView (Handler handler)
	{
		this.handler = handler;
	}
	
	public void postResponse (Request req, Response res, InputStream is, OutputStream os) throws IOException
	{
		if (res.getStatusCode () != 101)
			return;
		
		BidirectionalWebSocket sock = new BidirectionalWebSocket (this, req, is, os);
		handleBidirectionalWebSocket (sock);
	}

	/**
	 * The main loop of the WebSocket handler. Must return when {@code sock.isOpen()}
	 * starts returning false.
	 * 
	 * The default implementation calls {@link Handler#handleBidirectionalWebSocket(BidirectionalWebSocket)}
	 * on the {@link Handler} passed to the constructor. 
	 * 
	 * @param sock The {@link BidirectionalWebSocket} used to communicate with the client.
	 */
	public void handleBidirectionalWebSocket (BidirectionalWebSocket sock)
	{
		if (handler != null)
			handler.handleBidirectionalWebSocket (sock);
	}
}
