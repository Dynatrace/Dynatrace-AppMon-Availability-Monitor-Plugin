package com.mycompany.myplugin;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.InetAddress;
import java.net.UnknownHostException;



import com.dynatrace.diagnostics.pdk.PluginEnvironment;
import com.dynatrace.diagnostics.pdk.Status;

public class AvailMonitor {
	
	private static final Logger log = Logger.getLogger(AvailMonitor.class.getName());
	boolean DNSResolve = false;
	boolean TCPconnect = false;
	boolean ReverseDNS = false;
	boolean Ping = true;
	int RoundTrip = 0;
	private static final String CONFIG_TIME = "ReturnTime";
	protected Status setup(PluginEnvironment env) throws Exception {
		return new Status(Status.StatusCode.Success);
	}
	protected Status execute(PluginEnvironment env) throws Exception {
		Status result = new Status(Status.StatusCode.Success);
		boolean doTCPCheck = env.getConfigBoolean("doTCPCheck");
		String server = env.getHost().getAddress();
		InetAddress address;
		try
		{
			address = InetAddress.getByName(server);
			DNSResolve = true;
			InetAddress ip = InetAddress.getByName(address.getHostAddress());
			String ipName = ip.getHostName();
			if(ipName.equalsIgnoreCase(address.getHostName()))
			{
				ReverseDNS = true;
			}
			else
			{
				ReverseDNS = false;
			}
			if(doTCPCheck)
			{
				int holdingit = Integer.parseInt(env.getConfigString(CONFIG_TIME));
				long startTime = 0;
				long endTime = 0;
				boolean reachable = false;
				startTime = System.currentTimeMillis();
				reachable = address.isReachable(holdingit);
				endTime = System.currentTimeMillis();
				String Timers = new Long(endTime - startTime).toString();
				RoundTrip = Integer.parseInt(Timers);
				if(reachable)
				{
					TCPconnect = true;
				}
				else
				{
					TCPconnect = false;
				}
			}
			
		}
		catch (UnknownHostException e)
    	{
			log.log(Level.WARNING,"Error occurred: Server " + " " + env.getHost().getAddress() + " was not resolvable");
			result = new Status(Status.StatusCode.PartialSuccess);
			result.setMessage("Error: Server unreachable or unresolvable");
			DNSResolve = false;
			Ping = false;
			ReverseDNS = false;
			
    	}
		catch (IOException e)
		{
			log.log(Level.WARNING,"Error occurred: Server " + " " + env.getHost().getAddress() + " was not resolvable");
			result = new Status(Status.StatusCode.PartialSuccess);
			result.setMessage("Error: Server unreachable or unresolvable");
			DNSResolve = false;
			Ping = false;
			ReverseDNS = false;
		}
		
		if(DNSResolve)
		{
	        try {
	            Process proc = new ProcessBuilder("ping", server).start();

	            int exitValue = proc.waitFor();
	            System.out.println("Exit Value:" + exitValue);
	            if(exitValue == 0)
	            {
	                Ping = true;
	            }
	            else
	            {
	            	Ping = false;
	            }
	        } catch (IOException e1) {
	            System.out.println(e1.getMessage());
	            e1.printStackTrace();
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }
		}
		if(Ping == false)
		{
			log.warning("Pinging " + env.getHost().getAddress() + " Failure");
			result = new Status(Status.StatusCode.PartialSuccess);
		}
		return result;
		
		
}
	protected int returnPing() {
		if(Ping)
		{
			return 1;
		}
		else
		{
			return 0;
		}
	}
	protected int returnTCPConnect() {
		if(TCPconnect)
		{
			return 1;
		}
		else
		{
			return 0;
		}
	}
	protected int returnDNSConnect() {
		if(DNSResolve)
		{
			return 1;
		}
		else
		{
			return 0;
		}
	}
	protected int returnReverseDNSConnect() {
		if(ReverseDNS)
		{
			return 1;
		}
		else
		{
			return 0;
		}
	}
	protected int returnTrip() {
		if(TCPconnect)
		{
			return RoundTrip;
		}
		else
		{
			return 0;
		}
	}
	protected void teardown(PluginEnvironment env) throws Exception {
	}
	
}