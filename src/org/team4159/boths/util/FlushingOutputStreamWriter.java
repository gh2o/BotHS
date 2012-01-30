package org.team4159.boths.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class FlushingOutputStreamWriter extends OutputStreamWriter
{
	public FlushingOutputStreamWriter (OutputStream out)
	{
		super (out);
	}
	
	public void write (int c) throws IOException
	{
		super.write (c);
		super.flush ();
	}
	
	public void write (char[] cbuf) throws IOException
	{
		super.write (cbuf);
		super.flush ();
	}
	
	public void write (char[] cbuf, int off, int len) throws IOException
	{
		super.write (cbuf, off, len);
		super.flush ();
	}
	
	public void write (String str) throws IOException
	{
		super.write (str);
		super.flush ();
	}
	
	public void write (String str, int off, int len) throws IOException
	{
		super.write (str, off, len);
		super.flush ();
	}
}
