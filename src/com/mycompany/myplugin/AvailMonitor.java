package com.mycompany.myplugin;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.IllegalBlockingModeException;
import java.util.logging.Logger;

import com.dynatrace.diagnostics.pdk.PluginEnvironment;
import com.dynatrace.diagnostics.pdk.Status;

public class AvailMonitor {
	
	private static final Logger log = Logger.getLogger(AvailMonitor.class.getName());
	
	//plug-in configuration settings specified by User
	boolean doPingTest = false;
	boolean doTCPCheck = false;
	boolean doReverseDNSTest = false;
	
	int tcpRoundTripTime = 0;
	int pingMaxWaitTime = 0;


	//Variables for local status tracking
	boolean TCPConnectStatus = false;
	boolean ReverseDNS = false;
	boolean DNSResolve = false;
	
	boolean Ping = true;
	
	//Measures reported by this plug-in
	int availability = 0;
	int availabilityViolation = 0;
	int reverseDNSViolation = 0;
	int pingViolation = 0;
	int tcpConnectViolation = 0;
	int dnsResolutionViolation = 0;
	int port =  0;
	
	
	
	private static final String CONFIG_TIME = "ReturnTime";
	
	protected Status setup(PluginEnvironment env) throws Exception {
		this.doTCPCheck = env.getConfigBoolean("doTCPCheck");//doTCPCheck
		this.doPingTest = env.getConfigBoolean("doPingTest");
		
		return new Status(Status.StatusCode.Success);
	}
	
	protected Status execute(PluginEnvironment env) throws Exception {
	
		Status result = new Status(Status.StatusCode.Success);

		doTCPCheck = env.getConfigBoolean("doTCPCheck");//doTCPCheck
		doPingTest = env.getConfigBoolean("doPingTest");
		pingMaxWaitTime = Integer.parseInt(env.getConfigString("pingWaitTime"));
		doReverseDNSTest = env.getConfigBoolean("doReverseDNSTest");
		port = Integer.parseInt(env.getConfigString("port"));
		
		String server = env.getHost().getAddress();
		InetAddress address;
		address = InetAddress.getByName(server);
		
		InetAddress ip = InetAddress.getByName(address.getHostAddress());
		String ipName = ip.getHostName();
		
		try
		{			
			if (this.doReverseDNSTest) {
				if(ipName.equalsIgnoreCase(address.getHostName()))
				{
					ReverseDNS = true;
				}
				else
				{
					ReverseDNS = false;
					reverseDNSViolation++;
					log.severe("Reverse DNS Failed. ipName = " + ipName + ". Address.getHostName() = " + address.getHostName() + 
					" server: " + server + " ip: " + ip);
				}
			}			

			if(this.doTCPCheck)
			{
				SocketAddress socketAddress = new InetSocketAddress(address, port);
				Socket socket = new Socket();
				boolean available = true;
				
				try {
					socket.connect(socketAddress);
				} catch (IOException e) {
					available = false;
					log.severe("[IOException] - doTCPCheck - Not reachable address: " + address + "\n" + e.getMessage());
				}catch (IllegalArgumentException e) {
					available = false;
					log.severe("[IllegalArgumentException] - doTCPCheck - Not reachable address: " + address + "\n" + e.getMessage());
				}catch (IllegalBlockingModeException e) {
					available = false;
					log.severe("[IllegalBlockingModeException] - doTCPCheck - Not reachable address: " + address + "\n" + e.getMessage());
				}
				socket.close();
				
				if(available)
				{
					TCPConnectStatus = true;
				}
				else
				{
					TCPConnectStatus = false;
					tcpConnectViolation++;
					log.severe("Not reachable address: " + address + " Port: " + port);
				}
				
			}
			
			DNSResolve = true;
			
		}
		catch (UnknownHostException uex){
			log.severe("UnknownHostException. Error occurred: Server " + env.getHost().getAddress() + " was not resolvable. " + uex.getMessage());
			result = new Status(Status.StatusCode.PartialSuccess);
			result.setMessage("Error: Server unreachable or unresolvable");
			DNSResolve = false;
			dnsResolutionViolation++;			
    		}
		catch (Exception ex){
			log.severe("Error occurred: Server " + env.getHost().getAddress() + " was not resolvable. " + ex.getMessage());
			result = new Status(Status.StatusCode.PartialSuccess);
			result.setMessage("Error: Server unreachable or unresolvable");
			DNSResolve = false;
			dnsResolutionViolation++;		
		}		
		
		/* If PING is allowed in the Data Center. And if */
		if(doPingTest && DNSResolve)
		{
			try {
				if(address.isReachable(pingMaxWaitTime))					
					log.severe("Pinging: ");
				else{
					Ping = false;
					pingViolation++;
				}					
			}  catch (IOException e) {
				Ping = false;
				pingViolation++;
				log.severe("[IOException] - doPingTest - Not reachable address: " + address + "\n" + e.getMessage());
			}catch (IllegalArgumentException e) {
				Ping = false;
				pingViolation++;
				log.severe("[IllegalArgumentException] - doPingTest - Not reachable address: " + address + "\n" + e.getMessage());
			}
		}else{
			log.severe("NO Pinging: ");
			pingViolation++;
		}
		
		return result;
			
	}

	protected int getReverseDNSConnectViolation() {
		return reverseDNSViolation;
	}

	protected int getPingViolation() {
		return pingViolation;
	}


	protected int getDNSResolutionViolation() {
		return dnsResolutionViolation;
	}

	protected int returnTripTime() {
		return tcpRoundTripTime;		
	}

	protected void teardown(PluginEnvironment env) throws Exception {
	}
	
	/*
	 * @todo: Add instance variable for Availability and Violation count.
	 */
	protected int getAvailability() {
		
			
		log.severe("getAvail reverseDNSViolation: " + reverseDNSViolation + " dnsResolutionViolation: " + dnsResolutionViolation + 
		" pingViolation:" + pingViolation + " tcpConnectViolation: " + tcpConnectViolation);
		
				
		if((reverseDNSViolation == 0) && (pingViolation == 0) && (tcpConnectViolation == 0) && (dnsResolutionViolation == 0)) {
			this.availability = 100;
			this.availabilityViolation = 0;
		} else {
			this.availability = 0;
			this.availabilityViolation = 1;			
		} 
		
		return this.availability;
	}
	
	/*
	 * 
	 */
	protected int getAvailabilityViolation() {
				
		return this.availabilityViolation;
	}

	protected int getTCPConnectViolation() {
				
		return this.tcpConnectViolation ;
	}
	
}