package de.soderer.utilities;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

public class NetworkUtilities {
	public static boolean ping (String ipOrHostname) {
		try {
            return InetAddress.getByName(ipOrHostname).isReachable(5000);
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
    	return wakeOnLanPing(macAddress, false);
    }
    
    public static boolean wakeOnLanPing(String macAddress, boolean checkAfterWakeup) {
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
}
