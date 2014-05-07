/*************************************************************************
 FILE :		  CProxy.java

 Author :	  Svetoslav Tchekanov  (swetoslav@iname.com)

 Description: CProxy class definition.

			  CProxy.class is the implementation of TCP Proxy server


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

import	java.io.*;
import	java.net.*;

import org.jdamico.socks.server.commons.Log;

///////////////////////////////////////////////

public class CProxy	implements	Runnable
{
	protected	Object	m_lock;
	
	public static final int	DEFAULT_BUF_SIZE = 4096;
	
	protected	Thread		m_TheThread	= null;
	
	protected	CServer			m_SocksServer	= null;
	public	CServer	getSocksServer()	{	return m_SocksServer;	}
	
	public	Socket			m_ClientSocket	= null;
	public	Socket			m_ServerSocket	= null;
	
	public	int				m_BufLen		= DEFAULT_BUF_SIZE;
	public	byte[]			m_Buffer		= null;
	
	public	InputStream		m_ClientInput	= null;
	public	OutputStream	m_ClientOutput	= null;
	public	InputStream		m_ServerInput	= null;
	public	OutputStream	m_ServerOutput	= null;
	
	public static final int DEFAULT_TIMEOUT	= 10;
	

	protected	String	m_cProxyHost		= null;
	protected	int		m_nProxyPort		= 0;
	

	public	int		getProxyPort()	{	return	m_nProxyPort;	}
	public	String	getProxyHost()	{	return	m_cProxyHost; 	}

	///////////////////////////////////////////////
	
	public	CProxy( CServer SocksServer, Socket ClientSocket )
	{
		m_lock = this;
		
		m_SocksServer = SocksServer;
		if( m_SocksServer == null )	{
			Close();
			return;
		}
		
		m_ClientSocket = ClientSocket;
		if( m_ClientSocket != null )	{
			try	{
				m_ClientSocket.setSoTimeout( DEFAULT_TIMEOUT );
			}
			catch( SocketException e )	{
				Log.Error( "Socket Exception during seting Timeout." );
			}
		}

		m_cProxyHost	= m_SocksServer.m_cProxyHost;
		m_nProxyPort	= m_SocksServer.m_nProxyPort;
		
		m_Buffer = new byte[ m_BufLen ];
		
		Log.Println( "Proxy Created." );
	}
	
	///////////////////////////////////////////////
	
	public	void	SetLock( Object lock )
	{
		this.m_lock = lock;
	}
	
	
	///////////////////////////////////////////////

	public	void	start()
	{
		m_TheThread = new Thread( this );
		m_TheThread.start();
		Log.Println( "Proxy Started." );
	}
	/////////////////////////////////////////////////////////////

	public	void	stop()	{
	
		try	{
			if( m_ClientSocket != null )	m_ClientSocket.close();
			if( m_ServerSocket  != null )	m_ServerSocket.close();
		}
		catch( IOException e )	{
		}
		
		m_ClientSocket = null;
		m_ServerSocket  = null;
		
		Log.Println( "Proxy Stopped." );
		
		m_TheThread.stop();
	}
	
	/////////////////////////////////////////////////////////////
//	Common part of the server

	public	void	run()
	{
		SetLock( this );
		
		if( ! PrepareClient() )	{
			Log.Error( "Proxy - client socket is null !" );
			return;
		}

		ProcessRelay();

		Close();
	}
	///////////////////////////////////////////////
	///////////////////////////////////////////////

	public	void	Close()		{
		try	{
			if( m_ClientOutput != null )	{
				m_ClientOutput.flush();
				m_ClientOutput.close();
			}
		}
		catch( IOException e )	{
		}
		try	{
			if( m_ServerOutput != null )	{
				m_ServerOutput.flush();
				m_ServerOutput.close();
			}
		}
		catch( IOException e )	{
		}
		
		try	{
			if( m_ClientSocket != null )	{
					m_ClientSocket.close();
				}
			}
			catch( IOException e )	{
		}
		
		try	{
			if( m_ServerSocket != null )	{
					m_ServerSocket.close();
				}
			}
			catch( IOException e )	{
		}
		
		m_ServerSocket = null;
		m_ClientSocket = null;
		
		Log.Println( "Proxy Closed." );
	}
	///////////////////////////////////////////////
	///////////////////////////////////////////////
	
	public	void	SendToClient( byte[] Buf )	{
		
		SendToClient( Buf, Buf.length );
	}
	//--------------
	public	void	SendToClient( byte[] Buf, int Len )	{
		if( m_ClientOutput == null )		return;
		if( Len <= 0 || Len > Buf.length )	return;
		
		try	{
			m_ClientOutput.write( Buf, 0, Len );
			m_ClientOutput.flush();
		}
		catch( IOException e )	{
			Log.Error( "Sending data to client" );
		}
	}
	///////////////////////////////////////////////
	
	public	void	SendToServer( byte[] Buf )	{
		
		SendToServer( Buf, Buf.length );
	}
	//----------------
	public	void	SendToServer( byte[] Buf, int Len )	{
		if( m_ServerOutput == null )		return;
		if( Len <= 0 || Len > Buf.length )	return;
		
		try	{
			m_ServerOutput.write( Buf, 0, Len );
			m_ServerOutput.flush();
		}
		catch( IOException e )	{
			Log.Error( "Sending data to server" );
		}
	}
	
	///////////////////////////////////////////////
	
	public	boolean	isActive()	{
		return	(m_ClientSocket != null && m_ServerSocket != null);	
	}
	
	///////////////////////////////////////////////
	///////////////////////////////////////////////
	
	public	void	ConnectToServer( String Server, int port ) 
		throws IOException, UnknownHostException
	{
	//	Connect to the Remote Host
		
		if( Server.equals("") )	{
			Close();
			Log.Error( "Invalid Remote Host Name - Empty String !!!" );
			return;
		}
		

		
		m_ServerSocket = new Socket( Server, port );
		m_ServerSocket.setSoTimeout( DEFAULT_TIMEOUT );
		
		Log.Println( "Connected to "+Log.getSocketInfo( m_ServerSocket ) );
		PrepareServer();
	}
	/////////////////////////////////////////////////////////////
	protected	void	PrepareServer()	throws IOException	{
	synchronized( m_lock )
	{
		m_ServerInput  = m_ServerSocket.getInputStream();
		m_ServerOutput = m_ServerSocket.getOutputStream();
	}
	}
	/////////////////////////////////////////////////////////////
	
	public	boolean	PrepareClient()	{
		if( m_ClientSocket == null )	return false;

		try	{
			m_ClientInput = m_ClientSocket.getInputStream();
			m_ClientOutput= m_ClientSocket.getOutputStream();
		}
		catch( IOException e )	{
			Log.Error( "Proxy - can't get I/O streams!" );
			Log.Error( e );
			return	false;
		}
		return	true;
	}

	///////////////////////////////////////////////
	
	static	final	byte	SOCKS5_Version	= 0x05;
	static	final	byte	SOCKS4_Version	= 0x04;

	CSocks4	comm = null;
	
	public	void	ProcessRelay()	{
		
		try	{
			byte	SOCKS_Version	= GetByteFromClient();
			
			switch( SOCKS_Version )	{
			case SOCKS4_Version:	comm = new CSocks4( this );
									break;
			case SOCKS5_Version:	comm = new CSocks5( this );	
									break;
			default:	Log.Error( "Invalid SOKCS version : "+SOCKS_Version );
						return;
			}
			Log.Println( "Accepted SOCKS "+SOCKS_Version+" Request." );
						
			comm.Authenticate( SOCKS_Version );
			comm.GetClientCommand();
			
			switch ( comm.Command )	{
			case CSocks4.SC_CONNECT	:	comm.Connect();
										Relay();
										break;
			
			case CSocks4.SC_BIND	:	comm.Bind();
										Relay();
										break;
			
			case CSocks4.SC_UDP		:	comm.UDP();
										break;
			}
		}
		catch( Exception   e )	{
			Log.Error( e );
		}
	} 

	public	byte	GetByteFromClient()
		throws Exception
	{
		int	b;
		
		while( m_ClientSocket != null )		{
			
			try	{
				b = m_ClientInput.read();
			}
			catch( InterruptedIOException e )		{
				Thread.yield();
				continue;
			}
						
			return (byte)b; // return loaded byte
	
		} // while...
		throw	new Exception( "Interrupted Reading GetByteFromClient()");
	} // GetByteFromClient()...
	
	
	public	static	final	String	EOL = "\r\n";
	
	
	
	public	void	Relay()	{
	
		boolean	Active		= true;
		int		dlen		= 0;

		while( Active )	{
			
		//---> Check for client data <---
			
			dlen = CheckClientData();
			
			if( dlen < 0 )	Active = false;
			if( dlen > 0 )	{
				LogClientData( dlen );
				SendToServer( m_Buffer, dlen );
			}
			
			//---> Check for Server data <---
			dlen = CheckServerData();
			
			if( dlen < 0 )	Active = false;
			if( dlen > 0 )	{
				LogServerData( dlen );
				SendToClient( m_Buffer, dlen );
			}
			
			Thread.currentThread().yield();
		}	// while
	}
	
	/////////////////////////////////////////////////////////////

	public	int		CheckClientData()	{
	synchronized( m_lock )
	{
	//	The client side is not opened.
		if( m_ClientInput == null )	return -1;

		int	dlen = 0;

		try
		{
			dlen = m_ClientInput.read( m_Buffer, 0, m_BufLen );
		}
		catch( InterruptedIOException e )		{
			return	0;
		}
		catch( IOException e )		{
			Log.Println( "Client connection Closed!" );
			Close();	//	Close the server on this exception
			return -1;
		}

		if( dlen < 0 )	Close();

		return	dlen;
	}
	}
	///////////////////////////////////////////////
	/////////////////////////////////////////////////////////////

	public	int		CheckServerData()	{
	synchronized( m_lock )
	{
	//	The client side is not opened.
		if( m_ServerInput == null )	return -1;

		int	dlen = 0;

		try
		{
			dlen = m_ServerInput.read( m_Buffer, 0, m_BufLen );
		}
		catch( InterruptedIOException e )		{
			return	0;
		}
		catch( IOException e )		{
			Log.Println( "Server connection Closed!" );
			Close();	//	Close the server on this exception
			return -1;
		}

		if( dlen < 0 )	Close();

		return	dlen;
	}
	}
	///////////////////////////////////////////////

	///////////////////////////////////////////////
	
	public	void	LogServerData( int traffic )	{
		Log.Println("Srv data : "+
					Log.getSocketInfo( m_ClientSocket ) +
					" << <"+
					comm.m_ServerIP.getHostName()+"/"+
					comm.m_ServerIP.getHostAddress()+":"+
					comm.m_nServerPort+"> : " + 
					traffic +" bytes." );
	}
	
	///////////////////////////////////////////////
	
	public	void	LogClientData( int traffic )	{
		Log.Println("Cli data : "+
					Log.getSocketInfo( m_ClientSocket ) +
					" >> <"+
					comm.m_ServerIP.getHostName()+"/"+
					comm.m_ServerIP.getHostAddress()+":"+
					comm.m_nServerPort+"> : " + 
					traffic +" bytes." );
	}
	
	///////////////////////////////////////////////
}
///////////////////////////////////////////////////