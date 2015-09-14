package deployment;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import colluders.Colluders;
import util.Node;

public class DeployBARGossipColluders {
	public static void main(String[] args) {
		// Id of the node in the list
		short selfId = (short) Integer.parseInt(args[0]);
		
		int GROUP_SIZE = Integer.parseInt(args[1]);
		int GROUP_NBR = Integer.parseInt(args[2]);
		int RTE = Integer.parseInt(args[3]);
		
		int BAL_SIZE = Integer.parseInt(args[4]);
		int PUSH_SIZE = Integer.parseInt(args[5]);
		int PUSH_AGE = Integer.parseInt(args[6]);
		int FANOUT = Integer.parseInt(args[7]);
		int SESSION_DURATION = Integer.parseInt(args[8]);
		
		FileReader nodes_port_file;
		try {
			nodes_port_file = new FileReader(args[9]);
			BufferedReader bufRead = new BufferedReader(nodes_port_file);

			ArrayList<Node> nodesList = new ArrayList<Node>(0); // server node is the first
			String nodeName = bufRead.readLine();
			int nodePort;
			short nodeId = 0;

			while (nodeName != null) {
				nodePort = Integer.parseInt(bufRead.readLine());
				nodeName.replaceAll("\n", "");
				nodesList.add(new Node(nodeId, InetAddress.getByName(nodeName), nodePort));

				nodeName = bufRead.readLine();
				++nodeId;
			};

			bufRead.close();

			// Create a node that will apply the Colluders protocol. Its role depends on selfId: 0 corresponds to the broadcaster, others are peers
			Colluders colluders = new Colluders(selfId, nodesList.get(selfId).getPort(), nodesList, GROUP_SIZE, GROUP_NBR, RTE, BAL_SIZE, PUSH_SIZE, PUSH_AGE, FANOUT, SESSION_DURATION);
			// Starts the protocol
			colluders.run(selfId);

		} catch (IOException e1) {
			e1.printStackTrace();
		} 	
	}

}
