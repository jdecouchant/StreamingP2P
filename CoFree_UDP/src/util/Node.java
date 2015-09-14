package util;

import java.net.InetAddress;

/**
 * Contains the information needed to be able to interact with a node.
 * Specifies the status of a node. 
 */
public class Node {
	public final short nodeId;
	public final InetAddress address; 
	public final int port; // Port number on which node listens
	
	public Node(short nodeId, InetAddress address, int port) {		
		this.nodeId = nodeId;
		this.address = address;
		this.port = port;
	}
}
