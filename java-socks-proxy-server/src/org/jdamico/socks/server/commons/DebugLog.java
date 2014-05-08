

package	org.jdamico.socks.server.commons;



import	java.net.Socket;
import	java.net.DatagramPacket;
import	java.net.InetAddress;

import org.jdamico.socks.server.StartProxy;



public class DebugLog
{
	
	private static DebugLog INSTANCE = null;
	private DebugLog() {}
	
	public static DebugLog getInstance(){
		if(INSTANCE == null) INSTANCE = new DebugLog();
		return INSTANCE;
	}

	
	public	void	println( String txt )	{
		if( StartProxy.enableLog )	print( txt + Constants.EOL );
	}

	
	public	void	print( String txt )	{
		if( !StartProxy.enableLog )	return;
		if( txt == null )	return;
		System.out.print( txt );	
	}
	
	/////////////////////////////////////////////////
	
	public	void	error( String txt )	{
		if( StartProxy.enableLog )	println( "Error : " + txt );
	}
	
	/////////////////////////////////////////////////
	
	public	void	error( Exception e )	{
		if( !StartProxy.enableLog )	return;
		println( "ERROR : " + e.toString() );
		e.printStackTrace();
	}
	
	/////////////////////////////////////////////////
	
	public	String	iP2Str( InetAddress IP )	{
		if( IP == null )	return "NA/NA";
		
		return	IP.getHostName()+"/"+IP.getHostAddress();
	}
	
	/////////////////////////////////////////////////
	
	public	String	getSocketInfo( Socket sock )	{
	
		if( sock == null )	return "<NA/NA:0>";
		
		return	"<"+iP2Str( sock.getInetAddress() )+":"+
				sock.getPort() + ">";
	}
	
	
	
	public	String	getSocketInfo( DatagramPacket DGP )	{
	
		if( DGP == null )	return "<NA/NA:0>";
		
		return	"<"+iP2Str( DGP.getAddress() )+":"+
				DGP.getPort() + ">";
	}
	
	
}
