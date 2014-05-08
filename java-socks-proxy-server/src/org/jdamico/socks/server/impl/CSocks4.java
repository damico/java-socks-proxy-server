/*************************************************************************
 FILE :		  CSock4.java

 Author :	  Svetoslav Tchekanov  (swetoslav@iname.com)

 Description: CSock4 class definition.

			  CSock4.class is the implementation of Socks4 copmmands


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

package	org.jdamico.socks.server.impl;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.jdamico.socks.server.commons.Constants;
import org.jdamico.socks.server.commons.Log;

public class CSocks4
{
	public	byte	SOCKS_Version = 0;

	public	CProxy	m_Parent	= null;
	
	public	byte	Command;
	public	byte	DST_Port[]	= null;
	public	byte	DST_Addr[]	= null;
	public	byte	UserID[]	= null;
	
	public	String	UID = "";
	

	
	//--- Reply Codes ---
	protected	byte	getSuccessCode()	{ return 90; }
	protected	byte	getFailCode()		{ return 91; }
	//-------------------
	
	protected	InetAddress		m_ServerIP	  = null;
	protected	int				m_nServerPort = 0;
	
	protected	InetAddress		m_ClientIP	 = null;
	protected	int				m_nClientPort = 0;
	
	public	InetAddress	getClientAddress()	{ return m_ClientIP;	}
	public	InetAddress	getServerAddress()	{ return m_ServerIP;	}
	public	int			getClientPort()		{ return m_nClientPort;	}
	public	int			getServerPort()		{ return m_nServerPort;	}
	
	public	InetAddress	m_ExtLocalIP	= null;
	
	public	String	commName( byte code )	{
	
		switch( code )	{
			case 0x01: return "CONNECT";
			case 0x02: return "BIND";
			case 0x03: return "UDP Association";
			default:	return "Unknown Command";
		}
	
	}
	/////////////////////////////////////////////////////////////////
	
	public	String	ReplyName( byte code )	{
	
		switch( code )	{
		case 0: return "SUCCESS";
		case 1: return "General SOCKS Server failure";
		case 2: return "Connection not allowed by ruleset";
		case 3: return "Network Unreachable";
		case 4: return "HOST Unreachable";
		case 5: return "Connection Refused";
		case 6: return "TTL Expired";
		case 7: return "Command not supported";
		case 8: return "Address Type not Supported";
		case 9: return "to 0xFF UnAssigned";
		
		case 90: return "Request GRANTED";
		case 91: return "Request REJECTED or FAILED";
		case 92: return "Request REJECTED - SOCKS server can't connect to Identd on the client";
		case 93: return "Request REJECTED - Client and Identd report diff user-ID";		 
				   
		default:	return "Unknown Command";
		}
	}
	/////////////////////////////////////////////////////////////////

	public	CSocks4( CProxy Parent )	{

		m_Parent = Parent;
		
		DST_Addr = new byte[4];
		DST_Port = new byte[2];
	}
	
	/////////////////////////////////////////////////////////////////

	public	void	Calculate_UserID()	{
	
		String	s = UID + " ";
		UserID = s.getBytes();
		UserID[UserID.length-1] = 0x00;
	}
	
	/////////////////////////////////////////////////////////////////	
	
	public	int	byte2int( byte b )	{
		int	res = b;
		if( res < 0 ) res = (int)( 0x100 + res );
		return	res;
	}
	/////////////////////////////////////////////////////////////////
	
	public	int	calcPort( byte Hi, byte Lo )	{
		
		return ( (byte2int( Hi ) << 8) | byte2int( Lo ) );	
	}

	/////////////////////////////////////////////////////////////////
	
	public	InetAddress	calcInetAddress( byte[] addr )	{
		InetAddress	IA  = null;
		String		sIA = "";		
		
		if( addr.length < 4 )	{
			Log.Error( "calcInetAddress() - Invalid length of IP v4 - "+addr.length+" bytes" );	
			return null;
		}
		
		// IP v4 Address Type
		for( int i=0; i<4; i++ )	{
			sIA += byte2int( addr[i] );
			if( i<3 )	sIA += ".";
		}
		
		try	{
			IA = InetAddress.getByName( sIA );
		}
		catch( UnknownHostException e )	{
			return null;
		}
		
		return	IA; // IP Address
	}
	/////////////////////////////////////////////////////////////////	
	
	public	boolean	Calculate_Address()	{
			
		// IP v4 Address Type
		m_ServerIP		= calcInetAddress( DST_Addr );
		m_nServerPort	= calcPort( DST_Port[0], DST_Port[1] );
		
		m_ClientIP		= m_Parent.m_ClientSocket.getInetAddress();
		m_nClientPort	= m_Parent.m_ClientSocket.getPort();
		
		return ( (m_ServerIP != null) && (m_nServerPort >= 0) );
	}							
	/////////////////////////////////////////////////////////////////	
	
	protected	byte	GetByte()
	{
		byte	b;
		try	{
			b = m_Parent.getByteFromClient();
		}
		catch( Exception e )	{
			b = 0;
		}
		return	b;
	}
	/////////////////////////////////////////////////////////////

	public	void	Authenticate( byte SOCKS_Ver )
		throws	Exception	{
	
		SOCKS_Version = SOCKS_Ver;
	}
	
	/////////////////////////////////////////////////////////////

	public void	GetClientCommand()
		throws Exception
	{
		byte	b;
				
		// Version was get in method Authenticate()
		Command		= GetByte();

		DST_Port[0]	= GetByte();
		DST_Port[1]	= GetByte();
		
		for( int i=0; i<4; i++ )	{
			DST_Addr[i] = GetByte();
		}
		
		while( (b=GetByte()) != 0x00 )	{
			UID += (char)b;
		}
		Calculate_UserID();
		
		if( (Command < Constants.SC_CONNECT) || (Command > Constants.SC_BIND) )	{
			Refuse_Command( (byte)91 );
			throw	new Exception( "Socks 4 - Unsupported Command : "+commName( Command ) );
		}
		
		if( !Calculate_Address() )	{  // Gets the IP Address 
			Refuse_Command( (byte)92 );	// Host Not Exists...
			throw new Exception( "Socks 4 - Unknown Host/IP address '"+m_ServerIP.toString() );
		}
									
		Log.Println( "Accepted SOCKS 4 Command: \""+ commName( Command )+"\"" );
	}  // GetClientCommand()
	/////////////////////////////////////////////////////////////

	public	void	Reply_Command( byte ReplyCode )
	{
		Log.Println( "Socks 4 reply: \""+ReplyName( ReplyCode)+"\"" );
		
		byte[] REPLY = new byte[8];
		REPLY[0]= 0;
		REPLY[1]= ReplyCode;
		REPLY[2]= DST_Port[0];
		REPLY[3]= DST_Port[1];
		REPLY[4]= DST_Addr[0];
		REPLY[5]= DST_Addr[1];
		REPLY[6]= DST_Addr[2];
		REPLY[7]= DST_Addr[3];
			
		m_Parent.sendToClient( REPLY );
	} // Reply_Command()
	/////////////////////////////////////////////////////////////
			
	protected	void	Refuse_Command( byte ErrorCode )	{
		Log.Println( "Socks 4 - Refuse Command: \""+ReplyName(ErrorCode)+"\"" );
		Reply_Command( ErrorCode );
	}	// Refuse_Command()

	/////////////////////////////////////////////////////////////

	protected	void	Connect() throws Exception {

		Log.Println( "Connecting..." );
	//	Connect to the Remote Host
		try	{
			m_Parent.connectToServer( m_ServerIP.getHostAddress(), m_nServerPort );
		}
		catch( IOException e )	{
			Refuse_Command( getFailCode() ); // Connection Refused
			throw new Exception("Socks 4 - Can't connect to " +
			Log.getSocketInfo( m_Parent.m_ServerSocket ) );
		}
		
		Log.Println( "Connected to "+Log.getSocketInfo( m_Parent.m_ServerSocket ) );
		Reply_Command( getSuccessCode() );
	}	// Connect()
	
	/////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////

	/////////////////////////////////////////////////////////////
	
	public	void	BIND_Reply( byte ReplyCode, InetAddress IA, int PT )
		throws	IOException
	{
		byte	IP[] = {0,0,0,0};
		
		Log.Println( "Reply to Client : \""+ReplyName( ReplyCode )+"\"" );
		
		byte[] REPLY = new byte[8];
		if( IA != null )	IP = IA.getAddress();
						
		REPLY[0]= 0;
		REPLY[1]= ReplyCode;
		REPLY[2]= (byte)((PT & 0xFF00) >> 8);
		REPLY[3]= (byte) (PT & 0x00FF);
		REPLY[4]= IP[0];
		REPLY[5]= IP[1];
		REPLY[6]= IP[2];
		REPLY[7]= IP[3];
			
		if( m_Parent.isActive() )	{
			m_Parent.sendToClient( REPLY );
		}
		else	{
			Log.Println( "Closed BIND Client Connection" );
		}
	} // Reply_Command()
	
	/////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////
	//	It is IMPOSSIBLE to resolve normally the External
	//	IP address of yout machine )-: !!!
	/////////////////////////////////////////////////////////////
	public	InetAddress	ResolveExternalLocalIP()	{
		
		InetAddress	IP		=	null;
		
		if( m_ExtLocalIP != null )	{
			Socket	sct = null;
			try	{
				sct = new Socket( m_ExtLocalIP, m_Parent.getSocksServer().getPort() );
				IP = sct.getLocalAddress();
				sct.close();
				return m_ExtLocalIP;
			}
			catch( IOException e )	{
				Log.Println( "WARNING !!! THE LOCAL IP ADDRESS WAS CHANGED !" );
			}
		}
		
		String[]	hosts = {"www.sun.com","www.microsoft.com",
							 "www.aol.com","www.altavista.com",
							 "www.mirabilis.com","www.yahoo.com"};
		
		for( int i=0;i<hosts.length;i++ )	{
			try	{
				Socket	sct = new Socket( InetAddress.getByName(hosts[i]),80 );
				IP = sct.getLocalAddress();
				sct.close();
				break;
    		}
			catch( Exception e )	{  // IP == null
				Log.Println( "Error in BIND() - BIND reip Failed at "+i );
			}
		}
		
		m_ExtLocalIP = IP;
		return	IP;
	}
	
	/////////////////////////////////////////////////////////////
	
	protected	void	Bind()	throws IOException
	{
		ServerSocket	ssock	= null;
		InetAddress		MyIP	= null;
		int				MyPort	= 0;
		
		Log.Println( "Binding..." );
		// Resolve External IP
		MyIP = ResolveExternalLocalIP();

// It have not matter ... ask me for more...		
/*		if( MyIP == null )	{
			Log.Error(this, "BIND()", "Can't resolve local IP");	
			BIND_Reply( (byte)91, MyIP,MyPort );
			return;
		}
*/	
		Log.Println( "Local IP : " + MyIP.toString() );
		
		
		try	{	
			ssock = new ServerSocket( 0 );
			ssock.setSoTimeout( Constants.DEFAULT_PROXY_TIMEOUT );
			MyPort	= ssock.getLocalPort();
		}
		catch( IOException e )	{  // MyIP == null
			Log.Println( "Error in BIND() - Can't BIND at any Port" );
			BIND_Reply( (byte)92, MyIP,MyPort );
			ssock.close();
			return;
		}

		Log.Println( "BIND at : <"+MyIP.toString()+":"+MyPort+">" );
		BIND_Reply( (byte)90, MyIP, MyPort );
									 
		Socket	socket = null;

		while( socket == null )
		{
			if( m_Parent.checkClientData() >= 0 ) {
				Log.Println( "BIND - Client connection closed" );
				ssock.close();
				return;
			}

			try {
				socket = ssock.accept();
				socket.setSoTimeout( Constants.DEFAULT_PROXY_TIMEOUT );
			}
			catch( InterruptedIOException e ) {
				socket.close();
			}
			Thread.yield();
		}
		
		
/*		if( socket.getInetAddress() != m_m_ServerIP )	{
			BIND_Reply( (byte)91,	socket.getInetAddress(), 
									socket.getPort() );
			Log.Warning( m_Server, "BIND Accepts different IP/P" );
			m_Server.Close();
			return;
		}
*/		
		
		m_ServerIP	= socket.getInetAddress();
		m_nServerPort	= socket.getPort();
		
		BIND_Reply( (byte)90,	socket.getInetAddress(), 
								socket.getPort() );
		
		m_Parent.m_ServerSocket = socket;
		m_Parent.prepareServer();
		
		Log.Println( "BIND Connection from "+Log.getSocketInfo( m_Parent.m_ServerSocket ) );
		ssock.close();
		
		
	}// BIND...
	/////////////////////////////////////////////////////////////

	public	void	UDP() throws IOException
	{
		Log.Println( "Error - Socks 4 don't support UDP Association!" );
		Log.Println( "Check your Software please..." );
		Refuse_Command( (byte)91 );	// SOCKS4 don't support UDP
	}
	/////////////////////////////////////////////////////////////
}
/////////////////////////////////////////////////////////////////