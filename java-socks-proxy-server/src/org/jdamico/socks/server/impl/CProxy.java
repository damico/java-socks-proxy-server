


package	org.jdamico.socks.server.impl;


import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.jdamico.socks.server.commons.Constants;
import org.jdamico.socks.server.commons.DebugLog;


public class CProxy	implements	Runnable
{
	protected	Object	m_lock;
	
	
	
	protected	Thread		m_TheThread	= null;

	public	Socket			m_ClientSocket	= null;
	public	Socket			m_ServerSocket	= null;
	
	public	byte[]			m_Buffer		= null;
	
	public	InputStream		m_ClientInput	= null;
	public	OutputStream	m_ClientOutput	= null;
	public	InputStream		m_ServerInput	= null;
	public	OutputStream	m_ServerOutput	= null;
	
	public	CProxy(Socket clientSocket) {	
		m_lock = this;
		m_ClientSocket = clientSocket;
		if( m_ClientSocket != null )	{
			try	{
				m_ClientSocket.setSoTimeout( Constants.DEFAULT_PROXY_TIMEOUT );
			}
			catch( SocketException e )	{
				DebugLog.Error( "Socket Exception during seting Timeout." );
			}
		}
		
		m_Buffer = new byte[ Constants.DEFAULT_BUF_SIZE ];
		
		DebugLog.Println( "Proxy Created." );
	}
		
	public	void setLock( Object lock ) {
		this.m_lock = lock;
	}
	
	public	void	start()
	{
		m_TheThread = new Thread( this );
		m_TheThread.start();
		DebugLog.Println( "Proxy Started." );
	}

	public	void	stop()	{
	
		try	{
			if( m_ClientSocket != null )	m_ClientSocket.close();
			if( m_ServerSocket  != null )	m_ServerSocket.close();
		}
		catch( IOException e )	{
		}
		
		m_ClientSocket = null;
		m_ServerSocket  = null;
		
		DebugLog.Println( "Proxy Stopped." );
		
		m_TheThread.interrupt();
	}
	 
	public	void	run()
	{
		setLock( this );
		
		if( !prepareClient() )	{
			DebugLog.Error( "Proxy - client socket is null !" );
			return;
		}

		processRelay();

		close();
	}
	
	public	void close()		{
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
		
		DebugLog.Println( "Proxy Closed." );
	}
	
	public	void sendToClient( byte[] buffer )	{
		sendToClient( buffer, buffer.length );
	}
	
	public	void sendToClient( byte[] buffer, int len )	{
		if( m_ClientOutput == null )		return;
		if( len <= 0 || len > buffer.length )	return;
		
		try	{
			m_ClientOutput.write( buffer, 0, len );
			m_ClientOutput.flush();
		}
		catch( IOException e )	{
			DebugLog.Error( "Sending data to client" );
		}
	}
	
	public void sendToServer( byte[] buffer )	{
		sendToServer( buffer, buffer.length );
	}
	
	public	void sendToServer( byte[] buffer, int len )	{
		if( m_ServerOutput == null )		return;
		if( len <= 0 || len > buffer.length )	return;
		
		try	{
			m_ServerOutput.write( buffer, 0, len );
			m_ServerOutput.flush();
		}
		catch( IOException e )	{
			DebugLog.Error( "Sending data to server" );
		}
	}
	
	
	public	boolean	isActive()	{
		return	(m_ClientSocket != null && m_ServerSocket != null);	
	}
	
	
	public	void connectToServer( String server, int port ) throws IOException, UnknownHostException {

		if( server.equals("") )	{
			close();
			DebugLog.Error( "Invalid Remote Host Name - Empty String !!!" );
			return;
		}
		
		m_ServerSocket = new Socket( server, port );
		m_ServerSocket.setSoTimeout( Constants.DEFAULT_PROXY_TIMEOUT );
		
		DebugLog.Println( "Connected to "+DebugLog.getSocketInfo( m_ServerSocket ) );
		prepareServer();
	}

	protected void prepareServer() throws IOException	{
		synchronized( m_lock )
		{
			m_ServerInput  = m_ServerSocket.getInputStream();
			m_ServerOutput = m_ServerSocket.getOutputStream();
		}
	}
	
	public	boolean	prepareClient()	{
		if( m_ClientSocket == null )	return false;

		try	{
			m_ClientInput = m_ClientSocket.getInputStream();
			m_ClientOutput= m_ClientSocket.getOutputStream();
		}
		catch( IOException e )	{
			DebugLog.Error( "Proxy - can't get I/O streams!" );
			DebugLog.Error( e );
			return	false;
		}
		return	true;
	}
	
	

	CSocks4	comm = null;
	
	public	void processRelay()	{
		
		try	{
			byte SOCKS_Version	= getByteFromClient();
			
			switch( SOCKS_Version )	{
			case Constants.SOCKS4_Version:	comm = new CSocks4( this );
											break;
			case Constants.SOCKS5_Version:	comm = new CSocks5( this );	
											break;
			default:	DebugLog.Error( "Invalid SOKCS version : "+SOCKS_Version );
						return;
			}
			DebugLog.Println( "Accepted SOCKS "+SOCKS_Version+" Request." );
						
			comm.Authenticate( SOCKS_Version );
			comm.GetClientCommand();
			
			switch ( comm.Command )	{
			case Constants.SC_CONNECT	:	comm.Connect();
										relay();
										break;
			
			case Constants.SC_BIND	:	comm.Bind();
										relay();
										break;
			
			case Constants.SC_UDP		:	comm.UDP();
										break;
			}
		}
		catch( Exception   e )	{
			DebugLog.Error( e );
		}
	} 

	public	byte getByteFromClient() throws Exception {
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
	
	public	void relay()	{
	
		boolean	isActive = true;
		int		dlen = 0;

		while( isActive )	{
			
		//---> Check for client data <---
			
			dlen = checkClientData();
			
			if( dlen < 0 )	isActive = false;
			if( dlen > 0 )	{
				logClientData( dlen );
				sendToServer( m_Buffer, dlen );
			}
			
			//---> Check for Server data <---
			dlen = checkServerData();
			
			if( dlen < 0 )	isActive = false;
			if( dlen > 0 )	{
				logServerData( dlen );
				sendToClient( m_Buffer, dlen );
			}
			
			Thread.currentThread();
			Thread.yield();
		}	// while
	}
	
	

	public	int	 checkClientData()	{
		synchronized( m_lock )
		{
		//	The client side is not opened.
			if( m_ClientInput == null )	return -1;
	
			int	dlen = 0;
	
			try
			{
				dlen = m_ClientInput.read( m_Buffer, 0, Constants.DEFAULT_BUF_SIZE );
			}
			catch( InterruptedIOException e )		{
				return	0;
			}
			catch( IOException e )		{
				DebugLog.Println( "Client connection Closed!" );
				close();	//	Close the server on this exception
				return -1;
			}
	
			if( dlen < 0 )	close();
	
			return	dlen;
		}
	}
	
	public	int	checkServerData()	{
		synchronized( m_lock )
		{
		//	The client side is not opened.
			if( m_ServerInput == null )	return -1;
	
			int	dlen = 0;
	
			try
			{
				dlen = m_ServerInput.read( m_Buffer, 0, Constants.DEFAULT_BUF_SIZE );
			}
			catch( InterruptedIOException e )		{
				return	0;
			}
			catch( IOException e )		{
				DebugLog.Println( "Server connection Closed!" );
				close();	//	Close the server on this exception
				return -1;
			}
	
			if( dlen < 0 )	close();
	
			return	dlen;
		}
	}

	public	void	logServerData( int traffic )	{
		DebugLog.Println("Srv data : "+
					DebugLog.getSocketInfo( m_ClientSocket ) +
					" << <"+
					comm.m_ServerIP.getHostName()+"/"+
					comm.m_ServerIP.getHostAddress()+":"+
					comm.m_nServerPort+"> : " + 
					traffic +" bytes." );
	}
	

	public	void	logClientData( int traffic )	{
		DebugLog.Println("Cli data : "+
					DebugLog.getSocketInfo( m_ClientSocket ) +
					" >> <"+
					comm.m_ServerIP.getHostName()+"/"+
					comm.m_ServerIP.getHostAddress()+":"+
					comm.m_nServerPort+"> : " + 
					traffic +" bytes." );
	}
	public Socket getSocksServer() {
		return m_ServerSocket;
	}
	
	
}
