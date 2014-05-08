/*************************************************************************
 FILE :		  CSock5.java

 Author :	  Svetoslav Tchekanov  (swetoslav@iname.com)

 Description: CSock5 class definition.

			  CSock5.class is the implementation of Socks5 copmmands


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

///////////////////////////////////////////////

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.jdamico.socks.server.commons.Constants;
import org.jdamico.socks.server.commons.DebugLog;

///////////////////////////////////////////////

public class Socks5Impl extends Socks4Impl
{
	
	
	static	final	int		MaxAddrLen	= 255;
	
	static	final	byte	SC_UDP		= 0x03;

	protected	DatagramSocket	DGSocket= null;
	protected	DatagramPacket	DGPack	= null;
	
	private	InetAddress			UDP_IA		= null;
	private	int					UDP_port	= 0;
	
	//--- Reply Codes ---
	protected	byte	getSuccessCode()	{ return 00; }
	protected	byte	getFailCode()		{ return 04; }
	//-------------------
	
	
//	public			byte	SOCKS_Version;	// Version of SOCKS
//	public			byte	Command;		// Command code
	public			byte 	RSV;			// Reserved.Must be'00'
	public			byte	ATYP;			// Address Type
//	public			byte[]	DST_Addr;		// Destination Address
//	public			byte[]	DST_Port;		// Destination Port
											// in Network order
	
	static	final int	ADDR_Size[]={ -1, //'00' No such AType 
									   4, //'01' IP v4 - 4Bytes
									  -1, //'02' No such AType
									  -1, //'03' First Byte is Len
									  16  //'04' IP v6 - 16bytes
									};
	
	
	/////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	
	public	Socks5Impl( ProxyHandler Parent )	{
		
		super( Parent );
		
		DST_Addr = new byte[MaxAddrLen]; // 20 za vseki sluchay.
	}

	/////////////////////////////////////////////////////////////////	
	
	public	InetAddress	calcInetAddress( byte AType, byte[]	addr )	{
		InetAddress	IA = null;

		switch( AType )	{
			// Version IP 4
		case 0x01:	IA = super.calcInetAddress( addr );
					break;
			// Version IP DOMAIN NAME
		case 0x03:	if( addr[0] <= 0  )	{
						DebugLog.getInstance().error( "SOCKS 5 - calcInetAddress() : BAD IP in command - size : " + addr[0] );
						return null;	
					}
					String		sIA= "";
					for( int i = 1; i <= addr[0]; i++ )	{
						sIA += (char)addr[i];
					}
					try	{
						IA = InetAddress.getByName( sIA );
					}
					catch( UnknownHostException e )	{
						return null;
					}
					break;
		default:	return null;
		}
		return IA;
	} // calcInetAddress()
	
	/////////////////////////////////////////////////////////////////	
	
	public	boolean	Calculate_Address()	{
			
		m_ServerIP		= calcInetAddress( ATYP, DST_Addr );
		m_nServerPort	= calcPort( DST_Port[0], DST_Port[1] );
		
		m_ClientIP		= m_Parent.m_ClientSocket.getInetAddress();
		m_nClientPort	= m_Parent.m_ClientSocket.getPort();
		
		return ( (m_ServerIP != null) && (m_nServerPort >= 0) );
	}							
	/////////////////////////////////////////////////////////////////	
	
	public	void	Authenticate( byte SOCKS_Ver )	
		throws Exception	{
			
		super.Authenticate( SOCKS_Ver ); // Sets SOCKS Version...
		
		
		if( SOCKS_Version == Constants.SOCKS5_Version )	{	
			if( !CheckAuthentication() )	{// It reads whole Cli Request
				Refuse_Authentication("SOCKS 5 - Not Supported Authentication!");
				throw new Exception("SOCKS 5 - Not Supported Authentication.");
			}
			Accept_Authentication();
		}// if( SOCKS_Version...
		else	{
			Refuse_Authentication( "Incorrect SOCKS version : "+SOCKS_Version );
			throw new Exception( "Not Supported SOCKS Version -'"+
								 SOCKS_Version + "'");
		}
	} // Authenticate()
	/////////////////////////////////////////////////////////////
	
	public	void	Refuse_Authentication( String msg )	{

		DebugLog.getInstance().println( "SOCKS 5 - Refuse Authentication: '"+msg+"'" );
		m_Parent.sendToClient( Constants.SRE_Refuse );
	}
	/////////////////////////////////////////////////////////////
	
	public	void	Accept_Authentication()	{
		DebugLog.getInstance().println( "SOCKS 5 - Accepts Auth. method 'NO_AUTH'" );
		byte[] tSRE_Accept = Constants.SRE_Accept;
		tSRE_Accept[0] = SOCKS_Version;
		m_Parent.sendToClient( tSRE_Accept );
	}
	/////////////////////////////////////////////////////////////
	
	public	boolean	CheckAuthentication() 
		throws Exception	{
		//boolean	Have_NoAuthentication = false;
		byte	Methods_Num		= GetByte();
		String	Methods = "";
		
		for( int i=0; i<Methods_Num; i++ )	{
			Methods += ",-" + GetByte() + '-';
		}
	
		return	( (Methods.indexOf( "-0-"  ) != -1) || 
				  (Methods.indexOf( "-00-" ) != -1) );
	}
	/////////////////////////////////////////////////////////////
	
	public	void	GetClientCommand()	
	  throws Exception
	{
		int					Addr_Len;
			
		SOCKS_Version	= GetByte();
		Command			= GetByte();
		RSV				= GetByte();
		ATYP			= GetByte();
		
		Addr_Len = ADDR_Size[ATYP];
		DST_Addr[0] =  GetByte();
		if( ATYP==0x03 )	{
			Addr_Len = DST_Addr[0]+1;
		}
		
		for( int i=1; i<Addr_Len; i++ )	{
			DST_Addr[i]= GetByte();
		}
		DST_Port[0]	= GetByte();
		DST_Port[1]	= GetByte();
		
		if( SOCKS_Version != Constants.SOCKS5_Version )	{
			DebugLog.getInstance().println( "SOCKS 5 - Incorrect SOCKS Version of Command: "+
						 SOCKS_Version );
			Refuse_Command( (byte)0xFF );
			throw new Exception("Incorrect SOCKS Version of Command: "+ 
								  SOCKS_Version);
		}
		
		if( (Command < Constants.SC_CONNECT) || (Command > SC_UDP) )	{
			DebugLog.getInstance().error( "SOCKS 5 - GetClientCommand() - Unsupported Command : \"" + commName( Command )+"\"" );
			Refuse_Command( (byte)0x07 );
			throw new Exception("SOCKS 5 - Unsupported Command: \"" + Command +"\"" );
		}
		
		if( ATYP == 0x04 )	{
			DebugLog.getInstance().error( "SOCKS 5 - GetClientCommand() - Unsupported Address Type - IP v6" );
			Refuse_Command( (byte)0x08 );
			throw new Exception( "Unsupported Address Type - IP v6" );
		}
		
		if( (ATYP >= 0x04) || (ATYP <=0) )	{
			DebugLog.getInstance().error( "SOCKS 5 - GetClientCommand() - Unsupported Address Type: " + ATYP );
			Refuse_Command( (byte)0x08 );
			throw new Exception( "SOCKS 5 - Unsupported Address Type: " + ATYP );
		}
		
		if( !Calculate_Address() )	{  // Gets the IP Address 
			Refuse_Command( (byte)0x04 );// Host Not Exists...
			throw new Exception( "SOCKS 5 - Unknown Host/IP address '" + m_ServerIP.toString()+"'" );
		}
		
		DebugLog.getInstance().println( "SOCKS 5 - Accepted SOCKS5 Command: \""+commName(Command)+"\"" );
	}  // GetClientCommand()

	/////////////////////////////////////////////////////////////
	
	public	void	Reply_Command( byte ReplyCode )	{
		DebugLog.getInstance().println( "SOCKS 5 - Reply to Client \"" + ReplyName(ReplyCode)+"\"" );
		
		int	pt = 0;
		//String		DN = "0.0.0.0";
		//InetAddress	IA = null;
			
		byte[]	REPLY	= new byte[10];
		byte	IP[]	= new byte[4];
			
		if( m_Parent.m_ServerSocket != null )	{
			//IA = m_Parent.m_ServerSocket.getInetAddress();
			//DN = IA.toString();
			pt = m_Parent.m_ServerSocket.getLocalPort();
		}
		else	{
			IP[0]=0;
			IP[1]=0;
			IP[2]=0;
			IP[3]=0;
			pt = 0;
		}
			
		REPLY[0] = Constants.SOCKS5_Version;
		REPLY[1] = ReplyCode;	// Reply Code;
		REPLY[2] = 0x00;		// Reserved	'00'
		REPLY[3] = 0x01;		// DOMAIN NAME Type IP ver.4
		REPLY[4]= IP[0];
		REPLY[5]= IP[1];
		REPLY[6]= IP[2];
		REPLY[7]= IP[3];
		REPLY[8] = (byte)((pt & 0xFF00) >> 8);// Port High
		REPLY[9] = (byte) (pt & 0x00FF);	  // Port Low
			
		m_Parent.sendToClient( REPLY );// BND.PORT
	} // Reply_Command()
	/////////////////////////////////////////////////////////////
	
		/////////////////////////////////////////////////////////////
	
	public	void	BIND_Reply( byte ReplyCode, InetAddress IA, int PT )
	{
		byte	IP[] = {0,0,0,0};
		
		DebugLog.getInstance().println( "BIND Reply to Client \"" + ReplyName( ReplyCode )+"\"" );
		
		byte[]	REPLY = new byte[10];
		if( IA != null )	IP = IA.getAddress();
			
		REPLY[0] = Constants.SOCKS5_Version;
		REPLY[1] = (byte)((int)ReplyCode - 90);	// Reply Code;
		REPLY[2] = 0x00;		// Reserved	'00'
		REPLY[3] = 0x01;		// IP ver.4 Type
		REPLY[4] = IP[0];
		REPLY[5] = IP[1];
		REPLY[6] = IP[2];
		REPLY[7] = IP[3];
		REPLY[8] = (byte)((PT & 0xFF00) >> 8);
		REPLY[9] = (byte) (PT & 0x00FF);
			
		if( m_Parent.isActive() )	{
			m_Parent.sendToClient( REPLY );
		}
		else	{
			DebugLog.getInstance().println( "BIND - Closed Client Connection" );
		}
	} // BIND_Reply()

	/////////////////////////////////////////////////////////////
	
/*	protected	void	Bind() throws EXNetServer, IOException {

		super.Bind();
		
	} // BIND ...
*/	
	/////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////
	
	/////////////////////////////////////////////////////////////
	
	public	void	UDP_Reply( byte ReplyCode, InetAddress IA, int PT )
		throws	IOException	{
		
		DebugLog.getInstance().println( "Reply to Client \"" + ReplyName( ReplyCode )+"\"" );
		
		if( m_Parent.m_ClientSocket == null )	{
			DebugLog.getInstance().println( "Error in UDP_Reply() - Client socket is NULL" );	
		}
		byte[]	IP = IA.getAddress();
			 
		byte[]	REPLY = new byte[10];
			
		REPLY[0] = Constants.SOCKS5_Version;
		REPLY[1] = ReplyCode;	// Reply Code;
		REPLY[2] = 0x00;		// Reserved	'00'
		REPLY[3] = 0x01;		// Address Type	IP v4
		REPLY[4] = IP[0];
		REPLY[5] = IP[1];
		REPLY[6] = IP[2];
		REPLY[7] = IP[3];

		REPLY[8] = (byte)((PT & 0xFF00) >> 8);// Port High
		REPLY[9] = (byte) (PT & 0x00FF);		 // Port Low
			
		m_Parent.sendToClient( REPLY );// BND.PORT
	} // Reply_Command()
	
	
	/////////////////////////////////////////////////////////////
	
	
	public	void	UDP() throws IOException {
		
		//	Connect to the Remote Host
		
		try	{
			DGSocket  = new DatagramSocket();
			Init_UDP_InOut();
		}
		catch( IOException e )	{
			Refuse_Command( (byte)0x05 ); // Connection Refused
			throw new IOException( "Connection Refused - FAILED TO INITIALIZE UDP Association." );
		}

		InetAddress	MyIP   = m_Parent.m_ClientSocket.getLocalAddress();
		int			MyPort = DGSocket.getLocalPort();
		
		//	Return response to the Client   
		// Code '00' - Connection Succeeded,
		// IP/Port where Server will listen
		UDP_Reply( (byte)0, MyIP, MyPort );
		
		DebugLog.getInstance().println( "UDP Listen at: <"+MyIP.toString()+":"+MyPort+">" );

		while( m_Parent.checkClientData() >= 0 )
		{
			ProcessUDP();
			Thread.yield();
		}
		DebugLog.getInstance().println( "UDP - Closed TCP Master of UDP Association" );
	} // UDP ...
	/////////////////////////////////////////////////////////////
	
	private	void	Init_UDP_InOut()	throws IOException {
		
		DGSocket.setSoTimeout ( Constants.DEFAULT_PROXY_TIMEOUT );	
				
		m_Parent.m_Buffer = new byte[ Constants.DEFAULT_BUF_SIZE ];
		
		DGPack = new DatagramPacket( m_Parent.m_Buffer, Constants.DEFAULT_BUF_SIZE );
	}
	/////////////////////////////////////////////////////////////
	
	private	byte[]	AddDGPhead( byte[]	Buffer )	{
				
		//int		bl			= Buffer.length;
		byte	IABuf[]		= DGPack.getAddress().getAddress();
		int		DGport		= DGPack.getPort();
		int		HeaderLen	= 6 + IABuf.length;
		int		DataLen		= DGPack.getLength();
		int		NewPackLen	= HeaderLen + DataLen;
		
		byte	UB[] = new byte[ NewPackLen ];
		
		UB[0] = (byte)0x00;	// Reserved 0x00
		UB[1] = (byte)0x00;	// Reserved 0x00
		UB[2] = (byte)0x00;	// FRAG '00' - Standalone DataGram
		UB[3] = (byte)0x01;	// Address Type -->'01'-IP v4
		System.arraycopy( IABuf,0, UB,4, IABuf.length );
		UB[4+IABuf.length] = (byte)((DGport >> 8) & 0xFF);
		UB[5+IABuf.length] = (byte)((DGport     ) & 0xFF);
		System.arraycopy( Buffer,0, UB, 6+IABuf.length, DataLen );
		System.arraycopy( UB,0, Buffer,0, NewPackLen );
		
		return	UB;
		
	} // AddDGPhead()
	
	/////////////////////////////////////////////////////////////
	
	private	byte[]	ClearDGPhead( byte[] Buffer )	{
		int	IAlen = 0;
		//int	bl	= Buffer.length;
		int	p	= 4;	// First byte of IP Address
		
		byte	AType = Buffer[3];	// IP Address Type
		switch( AType )	{
		case	0x01:	IAlen = 4;   break;
		case	0x03:	IAlen = Buffer[p]+1; break; // One for Size Byte
		default		:	DebugLog.getInstance().println( "Error in ClearDGPhead() - Invalid Destination IP Addres type " + AType );
						return null;
		}

		byte	IABuf[] = new byte[IAlen];
		System.arraycopy( Buffer, p, IABuf, 0, IAlen );
		p += IAlen;
		
		UDP_IA   = calcInetAddress( AType , IABuf );
		UDP_port = calcPort( Buffer[p++], Buffer[p++] );
		
		if( UDP_IA == null )	{
			DebugLog.getInstance().println( "Error in ClearDGPHead() - Invalid UDP dest IP address: NULL" );
			return null;
		}
		
		int	DataLen = DGPack.getLength();
		DataLen -= p; // <p> is length of UDP Header
		
		byte	UB[] = new byte[ DataLen ];
		System.arraycopy( Buffer,p, UB,0, DataLen );
		System.arraycopy( UB,0, Buffer,0, DataLen );
		
		return UB;
		
	} // ClearDGPhead()
	
	/////////////////////////////////////////////////////////////

	protected	void	UDPSend( DatagramPacket	DGP )	{
	
		if( DGP == null )	return;
		
		String	LogString =	DGP.getAddress()+ ":" + 
							DGP.getPort()	+ "> : " + 
							DGP.getLength()	+ " bytes";
		try	{
			DGSocket.send( DGP );
		}
		catch( IOException e )	{
			DebugLog.getInstance().println( "Error in ProcessUDPClient() - Failed to Send DGP to "+ LogString );
			return;
		}
	}
	
	/////////////////////////////////////////////////////////////
	
	public	void	ProcessUDP()	{
		
		// Trying to Receive DataGram
		try	{
        	DGSocket.receive( DGPack );
		}
		catch( InterruptedIOException e )	{
			return;	// Time Out		
		}
		catch( IOException e )	{
			DebugLog.getInstance().println( "Error in ProcessUDP() - "+ e.toString() );
			return;
		}
		
		if( m_ClientIP.equals( DGPack.getAddress() ) )	{

			ProcessUDPClient();
		}
		else	{

			ProcessUDPRemote();
		}
		
		try	{
			Init_UDP_InOut();	// Clean DGPack & Buffer
		}
		catch( IOException e )	{
			DebugLog.getInstance().println( "IOError in Init_UDP_IO() - "+ e.toString() );
			m_Parent.close();
		}
	} // ProcessUDP()...
	
	/////////////////////////////////////////////////////////////

	/** Processing Client's datagram
	 * This Method must be called only from <ProcessUDP()>
	*/
	public	void	ProcessUDPClient()	{
		
		m_nClientPort = DGPack.getPort();

		// Also calculates UDP_IA & UDP_port ...
		byte[]	Buf = ClearDGPhead( DGPack.getData() );
		if( Buf == null )	return;
		
		if( Buf.length <= 0 )	return;				

		if( UDP_IA == null )	{
			DebugLog.getInstance().println( "Error in ProcessUDPClient() - Invalid Destination IP - NULL" );
			return;
		}
		if( UDP_port == 0 )	{
			DebugLog.getInstance().println( "Error in ProcessUDPClient() - Invalid Destination Port - 0" );
			return;
		}
		
		if( m_ServerIP != UDP_IA || m_nServerPort != UDP_port )	{
			m_ServerIP		= UDP_IA;
			m_nServerPort	= UDP_port;
		}
		
		DebugLog.getInstance().println( "Datagram : "+ Buf.length + " bytes : "+DebugLog.getInstance().getSocketInfo(	DGPack )+
					 " >> <" + DebugLog.getInstance().iP2Str( m_ServerIP )+":"+m_nServerPort+">" );
		
		DatagramPacket	DGPSend = new DatagramPacket( Buf, Buf.length,
													  UDP_IA, UDP_port );
		
		UDPSend( DGPSend );
	}		
	
	/////////////////////////////////////////////////////////////
	
	public	void	ProcessUDPRemote()	{

		DebugLog.getInstance().println( "Datagram : "+ DGPack.getLength()+" bytes : "+
					 "<"+DebugLog.getInstance().iP2Str( m_ClientIP )+":"+m_nClientPort+"> << " +
					 DebugLog.getInstance().getSocketInfo( DGPack ) );
		
		// This Method must be CALL only from <ProcessUDP()>
		// ProcessUDP() Reads a Datagram packet <DGPack>

		InetAddress	DGP_IP	= DGPack.getAddress();
		int			DGP_Port= DGPack.getPort();
		
		byte[]	Buf;
	
		Buf = AddDGPhead( m_Parent.m_Buffer );
		if( Buf == null )	return;
		
		// SendTo Client
		DatagramPacket	DGPSend = new DatagramPacket( Buf, Buf.length,
													  m_ClientIP, m_nClientPort );
		UDPSend( DGPSend );
		
		if( DGP_IP != UDP_IA || DGP_Port != UDP_port )	{
			m_ServerIP		= DGP_IP;
			m_nServerPort	= DGP_Port;
		}
	}		
	
	/////////////////////////////////////////////////////////////
}														 
/////////////////////////////////////////////////////////////////////