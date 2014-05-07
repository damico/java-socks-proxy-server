package org.jdamico.socks.server;


import org.jdamico.socks.server.commons.Constants;
import org.jdamico.socks.server.commons.Log;
import org.jdamico.socks.server.impl.CServer;


public class StartProxy {
	

	public static void main(String[] args) {
		
		
		Log.EnableLog = true;
		
		new CServer(Constants.LISTEN_PORT, Constants.PROXY_HOST, Constants.PROXY_PORT ).start();
	}
	
}

