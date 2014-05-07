/*************************************************************************
 FILE :		  Tools.java

 Author :	  Svetoslav Tchekanov  (swetoslav@iname.com)

 Description: Tools class definition.

			  Tools.class contains general purpose tools


 Copyright notice:
	Written by Svetoslav Tchekanov (swetoslav@iname.com)
	Copyright(c) 2000

This code may be used in compiled form in any way you desire. This
file may be redistributed unmodified by any means PROVIDING it is 
not sold for profit without the authors written consent, and 
providing that this notice and the authors name is included. If 
the source code in this file is used in any commercial application 
then a simple email would be nice.

This file is provided "as is" with no expressed or implied warranty.
The author accepts no liability if it causes any damage to your
computer.

*************************************************************************/


package	org.jdamico.socks.server.commons;

/////////////////////////////////////////////////////////////////////


import	java.util.Properties;

/////////////////////////////////////////////////////////////////


/**
 */
public	class	Tools
{
	/////////////////////////////////////////////////////////////
	
	public	static	int	byte2int( byte b )	{
		int	res = b;
		if( res < 0 ) res = (int)( 0x100 + res );
		return	res;
	}
	//------------
	public	static	String	byte2intString( byte b )	{
		return	String.valueOf( byte2int( b ) );
	}
	/////////////////////////////////////////////////////////////////
	
	public	static	int	Str2Int( String val, int Default )	{
		
		int	i;
		try	{
			i = Integer.valueOf( val ).intValue();
		}
		catch( NumberFormatException e )	{
			return	Default;
		}
		return	i;
	}
	
	/////////////////////////////////////////////////////////////////
	
	public	static	String	Bytes2MBytes( long nBytes )	{
		long	KBytes = nBytes / 1024;
		long	MBytes = KBytes / 1024;
		String	MB = "0";
		String	KB = "";
		
		if( MBytes > 0 )	MB  = new Long(MBytes).toString();
		
		KBytes = (nBytes - MBytes*1024*1024) / 1024;
		if( KBytes > 0 )	{
			KB = new Long(KBytes).toString();
			while( KB.length() < 3 )	KB = "0" + KB;
			MB += "."+KB;
		}
		
		return	MB;
	}
	/////////////////////////////////////////////////////////////
	
	public	static	String	MBytes2Bytes( long nMBytes )	{
		long	Bytes  = nMBytes * 1024  * 1024;
		String	cBytes  = "0";
		
		if( Bytes > 0 )	cBytes  = new Long(Bytes).toString();
				
		return	cBytes;
	}
	/////////////////////////////////////////////////////////////
	
	public	static	String	Data2String( byte[] Buf )	{
		return	Data2String( Buf, 0, Buf.length );
	}
	public	static	String	Data2String( byte[] Buf, int len )	{
		return	Data2String( Buf, 0, len );
	}
	public	static	String	Data2String( byte[] Buf, int beg, int len )	{
	
		if( Buf == null )	return "";
		if( len <= 0    )	return "";
		if( beg < 0 || beg > Buf.length )	return "";
		if( beg+len-1 > Buf.length )		return "";
		
		String	s = "";
		byte	b;
		for( int i = 0; i < len; i++ )	{
			s += byte2intString( Buf[i] );
			if( i < len - 1 )	s += ",";
		}
		
		return	s;
	}
	
	/////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////
	
	//////////////////////////////////////////////////////////////////
	
	public	static	boolean	CheckBoolean( String v, boolean Default )	{
		v = v.toUpperCase();
		if( v.equals( "1" ) ||
			v.equals("TRUE") ||
			v.equals("YES") )
		{
			return	true;
		}
		
		if( v.equals( "0" ) ||
		    v.equals("FALSE") ||
			v.equals("NO") )
		{
			return	false;
		}
		
		return	Default;
	}
	
	//////////////////////////////////////////////////////////////////
	
	public	static	boolean	LoadBoolean( String cName, Properties p )	{
		
		return	LoadBoolean( cName, true, p );
	}
	//-------------------------------------
	
	public	static	boolean	LoadBoolean( String cName, boolean Default, Properties p )	{
		
		String	v = LoadString( cName, p );
		if( v == null )	return Default;
		
		return	CheckBoolean( v, Default );
	}
	
	//////////////////////////////////////////////////////////////////

	public	static	int LoadInt( String cName, Properties p ) 
	{
		return	LoadInt( cName, 0, p );
	}
	public	static	int	LoadInt( String cName, int Default, Properties p ) 
	{
		String	cValue = LoadString( cName, p );
		if( cValue == null )	return	Default;
		if( cValue.equals("") )	return	Default;

		int	value;
		try	{
			value = Integer.valueOf( cValue ).intValue();
		}
		catch( NumberFormatException e )	{
			Log.Println( "Number Format Error in LoadInt("+cValue+")" );
			value = Default;
		}
		return	value;
	}
	/////////////////////////////////////////////////////////////

	public	static	String	LoadString( String cName, Properties p )
	{
		return	LoadString( cName, "", p );
	}
	public	static	String	LoadString( String cName, String Default, Properties p )
	{
		if( p == null )	return	Default;

		String	cValue;
		cValue = p.getProperty( cName );
		if( cValue == null )	return	Default;

		return	cValue;
	}
	/////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////
}
/////////////////////////////////////////////////////////////////
