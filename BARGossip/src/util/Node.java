package util;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * Contains the information any node needs to know to
 * be able to interact with the node it represents.
 */
public class Node implements Serializable {
	static final long serialVersionUID = 45L;
	
	private short selfId; // node id
	private InetAddress address; // node IP
	private int port; // port to connect to the node

	
	public Node(short id, InetAddress address, int port) {
		this.selfId = id;
		this.address = address;
		this.port = port;
	}

	public short getId() {
		return selfId;
	}

	public InetAddress getAddress() {
		return address;
	}

	public int getPort() {
		return port;
	}
	
}
