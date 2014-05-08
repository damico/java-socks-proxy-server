package org.jdamico.socks.server;


import org.jdamico.socks.server.commons.Constants;
import org.jdamico.socks.server.impl.ProxyServerInitiator;


public class StartProxy {
	
	public static	boolean	enableDebugLog = false;
	public static void main(String[] args) {
		
		/*
		 * enableDebugLog
		 * listenPort
		 */
		
		int port = Constants.LISTEN_PORT;
		
		String noParamsMsg = "Unable to parse command-line parameters, using default configuration.";
		
		if(args.length == 2){
			try {
				String prePort = args[0].trim();
				port = Integer.parseInt(prePort);
				String preDebug = args[1].trim();
				enableDebugLog = Boolean.parseBoolean(preDebug);
			} catch (Exception e) {
				System.out.println(noParamsMsg);
			}
		}else System.out.println(noParamsMsg);
		
		new ProxyServerInitiator(port).start();
	}
	
}

