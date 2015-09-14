package deployment;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import colluders.Colluders;
import util.Node;

public class DeployColluders {

    public static void main(String[] args) {
        // Id of the node in the list
        short selfId = Short.parseShort(args[0]);
        int RTE = Integer.parseInt(args[1]);
        int FANOUT_CLI = Integer.parseInt(args[2]);
        int FANOUT_SVR = Integer.parseInt(args[3]);
        int PERIOD = Integer.parseInt(args[4]);
        int EPOCH = Integer.parseInt(args[5]);
        int scenarioId = Integer.parseInt(args[6]);
        int sessionDuration = Integer.parseInt(args[7]);

        try {
            FileReader nodes_port_file = new FileReader(args[8]); // this file must contain the list of the hosts and the associated port number
            BufferedReader bufRead = new BufferedReader(nodes_port_file);

            ArrayList<Node> nodesList = new ArrayList<Node>(0); // server node is the first
            String nodeName = bufRead.readLine();

            short nodeId = 0;
            while (nodeName != null) {
                int nodePort = Integer.parseInt(bufRead.readLine());
                nodeName.replaceAll("\n", "");
                nodesList.add(new Node(nodeId, InetAddress.getByName(nodeName), nodePort));

                nodeName = bufRead.readLine();
                ++nodeId;
            };

            bufRead.close();

            // Create a node that will apply the Colluders protocol. Its role depends on selfId: 0 corresponds to the broadcaster, others are peers
            Colluders colluders = new Colluders(selfId, nodesList.get(selfId).port, RTE, FANOUT_CLI, FANOUT_SVR, PERIOD, EPOCH, nodesList, 
                    scenarioId, sessionDuration);

            // Starts the protocol
            colluders.run(selfId);

        } catch (IOException e1) {
            e1.printStackTrace();
        }   
    }

}
