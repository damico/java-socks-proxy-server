/*************************************************************************
 FILE :		  Log.java

 Author :	  Svetoslav Tchekanov  (swetoslav@iname.com)

 Description: Log class definition.

			  Log.class is the logging system of the SSH Proxy


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

///////////////////////////////////////////////

import	java.net.Socket;
import	java.net.DatagramPacket;
import	java.net.InetAddress;

///////////////////////////////////////////////

public class DebugLog
{

	public	static	final	String	EOL	= "\r\n";
	
	public	static	boolean	EnableLog = true;

	/////////////////////////////////////////////////
	
	public	static	void	Println( String txt )	{
		if( EnableLog )	Print( txt + EOL );
	}
	
	/////////////////////////////////////////////////
	
	public	static	void	Print( String txt )	{
		if( !EnableLog )	return;
		if( txt == null )	return;
		System.out.print( txt );	
	}
	
	/////////////////////////////////////////////////
	
	public	static	void	Error( String txt )	{
		if( EnableLog )	Println( "Error : " + txt );
	}
	
	/////////////////////////////////////////////////
	
	public	static	void	Error( Exception e )	{
		if( !EnableLog )	return;
		Println( "ERROR : " + e.toString() );
		e.printStackTrace();
	}
	
	/////////////////////////////////////////////////
	
	public	static	String	IP2Str( InetAddress IP )	{
		if( IP == null )	return "NA/NA";
		
		return	IP.getHostName()+"/"+IP.getHostAddress();
	}
	
	/////////////////////////////////////////////////
	
	public	static	String	getSocketInfo( Socket sock )	{
	
		if( sock == null )	return "<NA/NA:0>";
		
		return	"<"+IP2Str( sock.getInetAddress() )+":"+
				sock.getPort() + ">";
	}
	
	/////////////////////////////////////////////////
	
	public	static	String	getSocketInfo( DatagramPacket DGP )	{
	
		if( DGP == null )	return "<NA/NA:0>";
		
		return	"<"+IP2Str( DGP.getAddress() )+":"+
				DGP.getPort() + ">";
	}
	
	/////////////////////////////////////////////////
}
/////////////////////////////////////////////////////