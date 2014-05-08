

package	org.jdamico.socks.server.impl;



import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.jdamico.socks.server.commons.Constants;
import org.jdamico.socks.server.commons.Log;



public class CServer	implements	Runnable
{
	
	
	protected	Object	m_lock;
	
	protected	Thread			m_TheThread		= null;

	protected	ServerSocket	m_ListenSocket	= null;
	
	protected	int				m_nPort			= 0;
	
	public	int		getPort()		{	return	m_nPort;		}

	public	CServer(int listenPort, String proxyHost, int proxyPort) {
		
		m_lock = this;	
		m_nPort			= listenPort;
		Log.Println( "SOCKS Server Created." );
	}
	
	public	void setLock( Object lock ) {
		this.m_lock = lock;
	}
	

	public	void start() {
		m_TheThread = new Thread( this );
		m_TheThread.start();
		Log.Println( "SOCKS Server Started." );
	}

	public	void	stop()	{
		
		Log.Println( "SOCKS Server Stopped." );
		m_TheThread.interrupt();
	}
	
	public	void	run()
	{
		setLock( this );
		listen();
		close();
	}

	public	void close() {
		
		if( m_ListenSocket != null )	{
			try	{
				m_ListenSocket.close();
			}
			catch( IOException e )	{
			}
		}
		m_ListenSocket = null;
		
		Log.Println( "SOCKS Server Closed." );
	}
	
	public	boolean	isActive()	{
		return	(m_ListenSocket != null);	
	}
	
	
	private	void prepareToListen()	throws java.net.BindException, IOException {
		synchronized( m_lock )
		{
			m_ListenSocket = new ServerSocket( m_nPort );
			m_ListenSocket.setSoTimeout( Constants.LISTEN_TIMEOUT );
	
			if( m_nPort == 0 )	{
				m_nPort = m_ListenSocket.getLocalPort();
			}
			Log.Println( "SOCKS Server Listen at Port : " + m_nPort );
		}
	}
	
	protected	void listen() {
	
		try
		{
			prepareToListen();
		}
		catch( java.net.BindException e )	{
			Log.Error( "The Port "+m_nPort+" is in use !" );
			Log.Error( e );
			return;
		}
		catch( IOException e )	{
			Log.Error( "IO Error Binding at port : "+m_nPort );
			return;
		}

		while( isActive() )	{
			checkClientConnection();
			Thread.yield();
		}
	}
	
	public	void checkClientConnection()	{
		synchronized( m_lock )
		{
		//	Close() method was probably called.
			if( m_ListenSocket == null )	return;
	
			try
			{
				Socket clientSocket = m_ListenSocket.accept();
				clientSocket.setSoTimeout( Constants.DEFAULT_SERVER_TIMEOUT );
				Log.Println( "Connection from : " + Log.getSocketInfo( clientSocket ) );
				CProxy proxy = new CProxy(clientSocket );
				proxy.start();
			}
			catch( InterruptedIOException e )		{
			//	This exception is thrown when accept timeout is expired
			}
			catch( Exception e )	{
				Log.Error( e );
			}
		}	// synchronized
	}
}

