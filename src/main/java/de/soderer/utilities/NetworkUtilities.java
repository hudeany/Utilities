package de.soderer.utilities;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;

public class NetworkUtilities {
	public static boolean ping (String ipOrHostname) {
		try {
            return InetAddress.getByName(getHostnameFromRequestString(ipOrHostname)).isReachable(5000);
        } catch (Exception e) {
            return false;
        }
	}
	
    public static byte[] getMacAddressBytes(String macAddress) throws IllegalArgumentException {
    	if (StringUtils.isEmpty(macAddress)) {
    		throw new IllegalArgumentException("Invalid MAC address.");
    	}

        String[] hexParts = macAddress.split("(\\:|\\-| )");
        if (hexParts.length != 6) {
            throw new IllegalArgumentException("Invalid MAC address.");
        }
        
        try {
            byte[] bytes = new byte[6];
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) Integer.parseInt(hexParts[i], 16);
            }
            return bytes;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex digit in MAC address.");
        }
    }
    
    public static boolean wakeOnLanPing(String macAddress) {
    	DatagramSocket socket = null;
        try {
            byte[] macBytes = getMacAddressBytes(macAddress);
            byte[] bytes = new byte[6 + 16 * macBytes.length];
            Arrays.fill(bytes, 0, 6, (byte) 0xFF);
            for (int i = 6; i < bytes.length; i += macBytes.length) {
                System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
            }
            
            socket = new DatagramSocket();
            socket.send(new DatagramPacket(bytes, bytes.length, InetAddress.getByName("255.255.255.255"), 9));
            socket.close();
            
            return true;
        }
        catch (Exception e) {
        	return false;
        } finally {
        	if (socket != null) {
        		socket.close();
        	}
        }
    }
    
    public static boolean checkForNetworkConnection() {
    	try {
			for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				if (networkInterface.isUp() && !networkInterface.isLoopback()) {
					return true;
				}
			}
			return false;
		} catch (SocketException e) {
			e.printStackTrace();
			return false;
		}
    }
    
    public static String getHostnameFromRequestString(String requestString) {
    	if (requestString == null || !requestString.contains("/")) {
    		return requestString;
    	} else {
    		if (requestString.toLowerCase().startsWith("http")) {
    			requestString = requestString.substring(requestString.indexOf("//") + 2);
    			
    			if (!requestString.contains("/")) {
    				return requestString;
    			}
    		}
    		
    		return requestString.substring(0, requestString.indexOf("/"));
    	}
    }
}
