package deployment;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import colluders.Colluders;
import util.Node;

public class DeployBARGossip 
{
	public static void main(String[] args) 
	{
		// Id of the node in the list
		short selfId = (short) Integer.parseInt(args[0]);

		int balSize = Integer.parseInt(args[1]);
		int pushSize = Integer.parseInt(args[2]);
		int pushAge = Integer.parseInt(args[3]);
		int fanout = Integer.parseInt(args[4]);
		
		FileReader nodes_port_file;
		try 
		{
			nodes_port_file = new FileReader(args[5]);
			BufferedReader bufRead = new BufferedReader(nodes_port_file);

			ArrayList<Node> nodesList = new ArrayList<Node>(0); // server node is the first
			String nodeName = bufRead.readLine();
			int nodePort;
			short nodeId = 0;

			while (nodeName != null)
			{
				nodePort = Integer.parseInt(bufRead.readLine());
				nodeName.replaceAll("\n", "");
				nodesList.add(new Node(nodeId, InetAddress.getByName(nodeName), nodePort));

				nodeName = bufRead.readLine();
				++nodeId;
			};

			bufRead.close();

			// Create a node that will apply the Colluders protocol. Its role depends on selfId: 0 corresponds to the broadcaster, others are peers
			Colluders colluders = new Colluders(selfId, nodesList.get(selfId).getPort(), balSize, pushSize, pushAge, fanout, nodesList);
			
			// Starts the protocol
			colluders.run(selfId);

		} catch (IOException e1) 
		{
			e1.printStackTrace();
		} 	
	}

}
