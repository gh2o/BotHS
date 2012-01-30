package org.team4159.boths.util;

import java.io.IOException;
import java.io.InputStream;

public class LimitingInputStream extends InputStream
{
	private final InputStream is;
	private final int limit;
	private int pos = 0;

	public LimitingInputStream (InputStream is, int limit)
	{
		this.is = is;
		this.limit = limit;
	}
	
	public int read () throws IOException
	{
		if (pos >= limit)
			throw new IOException ("stream overflow");
		
		pos++;
		return is.read ();
	}
	
	public int read (byte[] buf, int off, int len) throws IOException
	{
		if (pos >= limit)
			throw new IOException ("stream overflow");
		
		int n = is.read (buf, off, Math.min (len, limit - pos));
		pos += n;
		return n;
	}
}
