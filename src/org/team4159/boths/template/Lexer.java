package org.team4159.boths.template;

import org.team4159.boths.util.Queue;
import org.team4159.boths.util.StringUtils;

class Lexer
{
	public static class TokenTypes
	{
		public static final int TEXT = 1;
		
		public static final int START_VARIABLE = 50;
		public static final int END_VARIABLE = 51;
		public static final int VARIABLE = 52;
		public static final int VARIABLE_SAFE = 53;
	}
	
	public static class Token
	{
		public final int type;
		public final String text;
		
		private Token (int type, String text)
		{
			this.type = type;
			this.text = text;
		}
	}
	
	private final String tmpl;
	private final int tmplLength;
	private final String filename;
	
	private int pos = 0;
	private int lastTokenType = TokenTypes.TEXT;
	private Queue queue = new Queue ();
	
	public Lexer (String tmpl, String filename)
	{
		this.tmpl = tmpl;
		this.tmplLength = tmpl.length ();
		
		if (filename == null)
			filename = "<string>";
		this.filename = filename;
	}
	
	private void populateQueue ()
	{
		if (pos == tmplLength)
			return;
		
		try {
			switch (lastTokenType)
			{
				case TokenTypes.START_VARIABLE:
					
					eatWhitespace ();
					
					int endpos = pos;
					
					for (;;)
					{
						char c = tmpl.charAt (endpos);
						if (
							c == '_' ||
							(c >= '0' && c <= '9') ||
							(c >= 'A' && c <= 'Z') ||
							(c >= 'a' && c <= 'z')
						)
							endpos++;
						else
							break;
					}
					
					addToken (TokenTypes.VARIABLE, tmpl.substring (pos, endpos));
					pos = endpos;
					
					break;
				
				case TokenTypes.VARIABLE:
					
					// optionally expect safe symbol
					eatWhitespace ();
					if (tmpl.charAt (pos) == '!')
					{
						addToken (TokenTypes.VARIABLE_SAFE, "!");
						pos += 1;
					}
					
					// expect end token
					eatWhitespace ();
					if (pos + 2 < tmplLength && tmpl.substring (pos, pos + 2).equals ("}}"))
					{
						addToken (TokenTypes.END_VARIABLE, "}}");
						pos += 2;
					}
					else
						throw buildParsingException ("missing \"}}\" in template");
					
					break;
				
				case TokenTypes.END_VARIABLE:
				case TokenTypes.TEXT:
					
					int nextSymPos = tmplLength;
					nextSymPos = Math.min (nextSymPos, tmplIndexOf ("{{", pos));
					
					if (nextSymPos == tmplLength) // no symbols, end of string
					{
						addToken (TokenTypes.TEXT, tmpl.substring (pos, nextSymPos));
						pos = nextSymPos;
					}
					else // symbol found
					{
						addToken (TokenTypes.TEXT, tmpl.substring (pos, nextSymPos));
						pos = nextSymPos;
						
						String symStarter = tmpl.substring (pos, pos + 2);
						if (symStarter.equals ("{{"))
						{
							addToken (TokenTypes.START_VARIABLE, "{{");
							pos += 2;
						}
						else
							throw new IllegalStateException ("bad symbol starter");
					}
					
					break;
				
				default:
					
					throw new IllegalStateException ("bad last token type");
			}
		} catch (IndexOutOfBoundsException e) {
			throw buildParsingException ("symbol expected");
		}
	}
	
	public Token peekToken ()
	{
		if (queue.size () > 0)
			return (Token) queue.element ();
		
		populateQueue ();
		
		if (queue.size () > 0)
			return (Token) queue.element ();
		
		return null;
	}
	
	public Token nextToken ()
	{
		Token ret = peekToken ();
		queue.poll ();
		return ret;
	}
	
	public Token expectToken (int type)
	{
		Token token = nextToken ();
		if (token == null)
			throw buildParsingException ("expecting token, got null");
		if (token.type == type)
			return token;
		throw buildParsingException ("wrong token type");
	}
	
	public Token expectToken (int[] types)
	{
		Token token = nextToken ();
		if (token == null)
			throw buildParsingException ("expecting token, got null");
		for (int i = 0; i < types.length; i++)
			if (token.type == types[i])
				return token;
		throw buildParsingException ("wrong token type");
	}
	
	public boolean hasMoreTokens ()
	{
		return peekToken () != null;
	}
	
	private void addToken (int type, String text)
	{
		queue.add (new Token (type, text));
		lastTokenType = type;
	}
	
	private void eatWhitespace ()
	{
		for (;;)
		{
			char c = tmpl.charAt (pos);
			if (
				c == 0x09 ||
				c == 0x0a ||
				c == 0x0b ||
				c == 0x0c ||
				c == 0x0d ||
				c == 0x20
			)
				pos++;
			else
				break;
		}
	}
	
	private int tmplIndexOf (String sub, int start)
	{
		int ret = tmpl.indexOf (sub, start);
		return (ret >= 0) ? ret : tmplLength;
	}
	
	private ParseException buildParsingException (String msg)
	{
		int[] lc = new int[2];
		StringUtils.locate (msg, pos, lc);
		return new ParseException (msg, filename, lc[0], lc[1]);
	}
}
