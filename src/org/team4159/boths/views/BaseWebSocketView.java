package org.team4159.boths.views;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.codec.binary.Base64;
import org.team4159.boths.Request;
import org.team4159.boths.Response;
import org.team4159.boths.Route;
import org.team4159.boths.View;
import org.team4159.boths.util.StringUtils;
import ch.ethz.ssh2.crypto.digest.SHA1;

public abstract class BaseWebSocketView extends View
{
	public static final int OPCODE_CONTINUE = 0x0;
	public static final int OPCODE_TEXT = 0x1;
	public static final int OPCODE_BINARY = 0x2;
	public static final int OPCODE_CLOSE = 0x8;
	public static final int OPCODE_PING = 0x9;
	public static final int OPCODE_PONG = 0xa;
	
	static class FragmentProcessor
	{
		final static byte[] EMPTY_MASKING_KEY = { 0, 0, 0, 0 }; 
		
		final DataInputStream dis;
		
		boolean fin, rsv1, rsv2, rsv3;
		int opcode;
		boolean mask;
		long payloadLength;
		final byte[] maskingKey = new byte[4];
		
		FragmentProcessor (InputStream is)
		{
			dis = new DataInputStream (is);
		}
		
		void readHeader () throws IOException
		{
			int firstByte = dis.readUnsignedByte ();
			fin = (firstByte & (1 << 7)) != 0;
			rsv1 = (firstByte & (1 << 6)) != 0;
			rsv2 = (firstByte & (1 << 5)) != 0;
			rsv3 = (firstByte & (1 << 4)) != 0;
			opcode = firstByte & 0xf;
			
			int secondByte = dis.readUnsignedByte ();
			mask = (secondByte & (1 << 7)) != 0;
			int preLength = secondByte & 0x7f;
			
			if (preLength == 126)
				payloadLength = dis.readUnsignedShort ();
			else if (preLength == 127)
				payloadLength = dis.readLong ();
			else
				payloadLength = preLength;
			
			if (payloadLength < 0)
				throw new IOException ("negative payload length");
			
			if (mask)
				dis.readFully (maskingKey);
			else
				System.arraycopy (EMPTY_MASKING_KEY, 0, maskingKey, 0, 4);
		}

		void readPayload (OutputStream os) throws IOException
		{
			for (int i = 0; i < payloadLength; i++)
			{
				int k = dis.read ();
				if (k < 0)
					throw new EOFException ("unexpected end of payload");
				os.write (k ^ (maskingKey[i % 4] & 0xff));
			}
		}
	}
	
	/**
	 * A WebSocket message.
	 */
	public static class Message
	{
		/**
		 * The opcode of the message.
		 */
		public final int opcode;
		
		/**
		 * The data payload of the message.
		 */
		public final byte[] data;

		/**
		 * Constructs a new message.
		 * 
		 * @param opcode	The opcode of the message.
		 * @param data		The data payload of the message.
		 */
		public Message (int opcode, byte[] data)
		{
			this.opcode = opcode;
			this.data = data;
		}
	}
	
	/**
	 * Maximum payload size per fragment.
	 */
	protected int maximumPayloadSize = 0x10000;
	
	/**
	 * Maximum message size.
	 */
	protected int maximumMessageSize = 0x10000;
	
	/**
	 * Sets the maximum payload size per fragment.
	 * @param sz	Maximum size in bytes.
	 * @see #maximumPayloadSize
	 */
	public void setMaximumPayloadSize (int sz) { maximumPayloadSize = sz; }
	
	/**
	 * Sets the maximum message size.
	 * @param sz	Maximum size in bytes.
	 * @see #maximumMessageSize
	 */
	public void setMaximumMessageSize (int sz) { maximumMessageSize = sz; }
	
	public Response getResponse (Request req, Route route)
	{
		if (!verifyRequest (req))
		{
			Response res = Response.createErrorResponse (400);
			res.setHeader ("Sec-WebSocket-Version", "8");
			return res;
		}
		
		String webSocketKey = req.getHeader ("Sec-WebSocket-Key");
		String webSocketKeyConstant = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
		String hashInput = webSocketKey + webSocketKeyConstant;
		
		SHA1 hasher = new SHA1 ();
		hasher.update (hashInput.getBytes ());
		byte[] hasherOutput = new byte[20];
		hasher.digest (hasherOutput);
		
		Response res = new Response ();
		res.setStatusCode (101);
		res.deleteHeader ("Content-Type");
		res.setHeader ("Upgrade", "websocket");
		res.setHeader ("Connection", "Upgrade");
		res.setHeader ("Sec-WebSocket-Accept", new String (Base64.encodeBase64 (hasherOutput)));
		return res;
	}
	
	private boolean verifyRequest (Request req)
	{
		if (!req.method.equals ("GET"))
			return false;
		
		String upgrade = req.getHeader ("Upgrade");
		if (upgrade == null || !upgrade.toLowerCase ().equals ("websocket"))
			return false;
		
		String connection = req.getHeader ("Connection");
		if (connection == null)
			return false;
		
		String[] connectionTokens = StringUtils.splitByWholeSeparator (connection, ",");
		boolean correctConnection = false;
		for (int i = 0; i < connectionTokens.length; i++)
		{
			if (connectionTokens[i].trim ().toLowerCase ().equals ("upgrade"))
			{
				correctConnection = true;
				break;
			}
		}
		
		if (!correctConnection)
			return false;
		
		String webSocketKey = req.getHeader ("Sec-WebSocket-Key");
		if (webSocketKey == null)
			return false;
		if (webSocketKey.length () != 24)
			return false;
		
		String webSocketVersionString = req.getHeader ("Sec-WebSocket-Version");
		if (webSocketVersionString == null)
			return false;
		
		int webSocketVersion;
		try {
			webSocketVersion = Integer.parseInt (webSocketVersionString);
		} catch (NumberFormatException e) {
			return false;
		}
		if (webSocketVersion < 6 || webSocketVersion > 13)
			return false;
		
		return true;
	}

	Message receiveMessageFromSocket (FragmentProcessor fp, ByteArrayOutputStream baos) throws IOException
	{
		baos.reset ();
		
		int currentOpcode = 0;
		
		do {
			fp.readHeader ();
			
			if (!fp.mask)
				throw new IOException ("unmasked fragment");
			
			if (currentOpcode == 0)
			{
				if (fp.opcode != 0)
					currentOpcode = fp.opcode;
				else
					throw new IOException ("first fragment of message is continuation fragment");
				if (isControlOpcode (fp.opcode) && !fp.fin)
					throw new IOException ("fragmented control packet");
			}
			else
			{
				if (fp.opcode != 0)
					throw new IOException ("fragment with opcode after first fragment");
			}
			
			if (fp.payloadLength > maximumPayloadSize)
				throw new IOException ("payload too large (" +
					fp.payloadLength + " > " + maximumPayloadSize +
				")");
			if (baos.size () + fp.payloadLength > maximumMessageSize)
				throw new IOException ("message too large");
			
			fp.readPayload (baos);
		} while (!fp.fin);
		
		return new Message (currentOpcode, baos.toByteArray ());
	}

	void sendMessageToSocket (Message msg, DataOutputStream dos) throws IOException
	{
		int len = msg.data != null ? msg.data.length : 0;
		
		if (isControlOpcode (msg.opcode) && len > 125)
			throw new IOException ("payload for control packet too large");
		if (msg.opcode <= 0 || msg.opcode > 0xf)
			throw new IOException ("opcode out of range 1-15 inclusive");
		
		dos.write (0x80 | msg.opcode);
		
		if (len <= 125)
		{
			dos.write (len);
		}
		else if (len <= 0xffff)
		{
			dos.write (126);
			dos.writeShort (len);
		}
		else
		{
			dos.write (127);
			dos.writeLong (len);
		}
		
		if (msg.data != null)
			dos.write (msg.data);
		dos.flush ();
	}
	
	private static boolean isControlOpcode (int opcode)
	{
		return (opcode & (1 << 3)) != 0;
	}
}