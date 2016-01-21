package com.mycompany.myplugin;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


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
		
		String server = env.getHost().getAddress();
		InetAddress address;
		try
		{
			address = InetAddress.getByName(server);
			
			InetAddress ip = InetAddress.getByName(address.getHostAddress());
			String ipName = ip.getHostName();

			
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
				
				int holdingit = Integer.parseInt(env.getConfigString(CONFIG_TIME));
				long startTime = 0;
				long endTime = 0;
				boolean reachable = false;
				startTime = System.currentTimeMillis();
				reachable = address.isReachable(holdingit);
				endTime = System.currentTimeMillis();
				String Timers = new Long(endTime - startTime).toString();
				

				tcpRoundTripTime = Integer.parseInt(Timers);
				if(reachable)
				{
					TCPConnectStatus = true;
				}
				else
				{
					TCPConnectStatus = false;
					tcpConnectViolation++;
					log.severe("Not reachable address: " + address );
				}
				
			}
			
			DNSResolve = true;
			
		}
		catch (UnknownHostException uex)
    		{
			log.severe("UnknownHostException. Error occurred: Server " + env.getHost().getAddress() + " was not resolvable. " + uex.getMessage());
			result = new Status(Status.StatusCode.PartialSuccess);
			result.setMessage("Error: Server unreachable or unresolvable");
			DNSResolve = false;
			dnsResolutionViolation++;
			
    		}
		catch (Exception ex)
		{
			log.severe("Error occurred: Server " + env.getHost().getAddress() + " was not resolvable. " + ex.getMessage());
			result = new Status(Status.StatusCode.PartialSuccess);
			result.setMessage("Error: Server unreachable or unresolvable");
			DNSResolve = false;
			dnsResolutionViolation++;
		
		}

		
		
		/* If PING is allowed in the Data Center. And if */
		if(doPingTest && DNSResolve)
		{
			String pingArgs = " -w " + pingMaxWaitTime + " " + server;
			try {
			
	            Process proc = new ProcessBuilder("ping", pingArgs).start();
			
								
	            int exitValue = proc.waitFor();
	        
	            if(exitValue == 0)
	            {
	                Ping = true;
	            }
	            else
	            {
	            	Ping = false;
	            	pingViolation++;
	            }
	        } catch (IOException e) {
	        	Ping = false;
	        	pingViolation++;
	            log.severe("IOException: Cannot PING Args: " + pingArgs + ". Message: " + e.getMessage());
	            e.printStackTrace();
	        } catch (InterruptedException e) {
	        	Ping = false;
	        	pingViolation++;
	        	log.severe("InterruptedException: Cannot PING Args: " + pingArgs + ". Message: " + e.getMessage());
	            e.printStackTrace();
	        }

			if(Ping == false)
			{
				log.severe("Pinging " + env.getHost().getAddress() + " Failure : ping Args: " + pingArgs );
				result = new Status(Status.StatusCode.PartialSuccess);
			}
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
		
		/*	
		log.severe("getAvail reverseDNSViolation: " + reverseDNSViolation + " dnsResolutionViolation: " + dnsResolutionViolation + 
		" pingViolation:" + pingViolation + " tcpConnectViolation: " + tcpConnectViolation);
		*/
				
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