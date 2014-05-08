package org.jdamico.socks.server;


import org.jdamico.socks.server.commons.Constants;
import org.jdamico.socks.server.impl.ProxyServerInitiator;


public class StartProxy {
	
	public	static	boolean	enableLog = false;
	public static void main(String[] args) {
		
		
		enableLog = true;
		
		new ProxyServerInitiator(Constants.LISTEN_PORT, Constants.PROXY_HOST, Constants.PROXY_PORT ).start();
	}
	
}

