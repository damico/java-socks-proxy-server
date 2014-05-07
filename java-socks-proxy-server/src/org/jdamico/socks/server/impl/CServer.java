/*************************************************************************
 FILE :		  CServer.java

 Author :	  Svetoslav Tchekanov  (swetoslav@iname.com)

 Description: CServer class definition.

			  CServer.class is the implementation of TCP server


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
import java.net.ServerSocket;
import java.net.Socket;

import org.jdamico.socks.server.commons.Constants;
import org.jdamico.socks.server.commons.Log;

///////////////////////////////////////////////

public class CServer	implements	Runnable
{
	
	
	protected	Object	m_lock;
	
	protected	Thread			m_TheThread		= null;

	protected	ServerSocket	m_ListenSocket	= null;
	
	protected	int				m_nPort			= 0;
	
	protected	String			m_cProxyHost = null;
	protected	int				m_nProxyPort= 0;
	
	public	int		getPort()		{	return	m_nPort;		}
	public	int		getProxyPort()	{	return	m_nProxyPort;	}
	public	String	getProxyHost()	{	return	m_cProxyHost; 	}
	
	
	public	CServer(int listenPort, String proxyHost, int proxyPort) {
		
		m_lock = this;	
		m_nPort			= listenPort;
		m_cProxyHost	= proxyHost;
		m_nProxyPort	= proxyPort;
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
		m_TheThread.stop();
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
				clientSocket.setSoTimeout( Constants.DEFAULT_TIMEOUT );
				Log.Println( "Connection from : " + Log.getSocketInfo( clientSocket ) );
				CProxy proxy = new CProxy( this, clientSocket );
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

