package org.jdamico.socks.server;


import org.jdamico.socks.server.commons.Constants;
import org.jdamico.socks.server.commons.DebugLog;
import org.jdamico.socks.server.impl.ProxyServerInitiator;


public class StartProxy {
	

	public static void main(String[] args) {
		
		
		DebugLog.EnableLog = true;
		
		new ProxyServerInitiator(Constants.LISTEN_PORT, Constants.PROXY_HOST, Constants.PROXY_PORT ).start();
	}
	
}

