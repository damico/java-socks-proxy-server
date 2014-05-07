package org.jdamico.socks.server;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.jdamico.socks.server.commons.Log;
import org.jdamico.socks.server.commons.Tools;
import org.jdamico.socks.server.impl.CServer;


public class StartProxy
{
	public	static	final	int	DEFAULT_PORT = 8888;
	
	public	static	int		s_nPort = DEFAULT_PORT;
	
	public	static	boolean	s_bUseSHttpProxy = false;
	
	public	static	String	s_ProxyHost = null;
	public	static	int		s_ProxyPort = 80;
	
	public	static	boolean	s_EnableLog	= true;
	
	public	static	Properties	s_Prop = null;
	

	public	static boolean LoadProperties()	{
	
		String	ErrorMsg = "";
		
		FileInputStream	fis;
		s_Prop = new Properties();
		String	UseSHTTP = "NO";
		String	pHost = "";
		String	pPort = "";
		String	sPort = "";
		try	{
			fis = new FileInputStream( "/home/jdamico/workspace/java-socks-proxy-2/config.txt" );
			s_Prop.load( fis );
		}
		catch( FileNotFoundException  e )	{
			ErrorMsg = "File not found \"config.txt\"";
		}
		catch( IOException e )	{
			ErrorMsg = "IO Error loading \"config.txt\"";
		}
		
		s_nPort = Tools.LoadInt( "SOCKSPort", DEFAULT_PORT, s_Prop );
		s_bUseSHttpProxy = Tools.LoadBoolean( "UseSHttpProxy", false, s_Prop );
		s_ProxyPort = Tools.LoadInt   ( "SHttpProxyPort",  0, s_Prop );
		s_ProxyHost = Tools.LoadString( "SHttpProxyHost", "", s_Prop );
		
		if( !s_bUseSHttpProxy )	{
			Log.Println( "Use of SHTTP Proxy Disabled." );
		}
		else	{
			Log.Println( "USE of SHTTP Proxy Enabled." );
			Log.Println( "SHTTP Proxy Host : " + s_ProxyHost );
			Log.Println( "SHTTP Proxy Port : " + s_ProxyPort );
		}
		Log.Println( "---------------------------------------" );
		
		if( s_ProxyPort <= 0 || s_ProxyHost == null ||
			s_ProxyHost.length() <= 0 )
		{
			ErrorMsg = "Invalid settings for SHttpProxy !  Use of SHTTP Proxy disabled !";
			s_bUseSHttpProxy = false;
		}
		
		s_EnableLog = Tools.LoadBoolean( "EnableLog", true, s_Prop );
		
		if( s_EnableLog )	{
			Log.Println( "Logging : On" );
		}
		else	{
			Log.Println( "Logging : Off" );
		}
		
		if( !ErrorMsg.equals("") )	{
			Log.Error( ErrorMsg );
			return	false;
		}
		
		return true;
	}
	

	public static void main(String[] args)
	{
		
		if( ! LoadProperties() )	return;
		
		Log.EnableLog = s_EnableLog;
		
		new CServer( s_nPort,	s_ProxyHost, s_ProxyPort ).start();
	}
	
}

