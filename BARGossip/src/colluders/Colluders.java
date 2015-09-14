package colluders;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import util.Node;

public class Colluders {

	// Parameters for the Colluders protocol
	
	// The following values must verify the relation (updatesLen * 8 * updatesNbrPerRound) = 300000 * roundDuration
	private int roundDuration = 1;
	private int updatesLen = 938; // in bytes
	private int updatesNbrPerRound = 40;
	// Number of rounds updates are released before they expire
	private int rte = 10;

	// Simulations parameters
	private int startTime = 5;
	private int sessionDuration = 400;
	
	private Peer peer;
	private Server server;

	public Colluders (short selfId, int listeningPort, int balSize, int pushSize, int pushAge, int fanout, ArrayList<Node> nodesList)  
	{
		if (selfId != 0) // The node is a peer
			this.peer = new Peer(selfId, listeningPort, nodesList, this.rte, this.updatesNbrPerRound, this.updatesLen, balSize, pushSize, pushAge);
		else // The node is the broadcaster
			this.server = new Server(nodesList, this.updatesNbrPerRound, this.updatesLen, fanout);	
	}
	
	public void run(int selfId) 
	{
		long initTime, lastRoundTime;
		
		if (selfId != 0) // The node is a peer
		{
			// Wait, doing nothing, until the beginning of the session
			initTime = System.currentTimeMillis()/1000;
			System.out.println("Waiting to start (" + startTime + " seconds)");
			while (System.currentTimeMillis()/1000 - initTime < startTime);
			System.out.println("Session starts NOW");

			// Count new rounds until time limit
			initTime = System.currentTimeMillis()/1000;
			while (System.currentTimeMillis()/1000 - initTime < this.sessionDuration) {

				// If the duration of one round passed, treat a new round
				lastRoundTime = System.currentTimeMillis()/1000;

				// Trigger periodical messages of peer
				peer.newRound();

				// Wait until the end of round
				while (System.currentTimeMillis()/1000 - lastRoundTime < this.roundDuration)
					peer.treatInMessage();

				// End of round: increase the round ID, update the log, and consume received updates
				peer.endOfRound();
			}

			peer.mustStop = true;
			peer.sender.mustStop = true;

			long nbrReceivedUpdates = peer.getNbrReceivedUpdates();

			try 
			{
				BufferedWriter out = new BufferedWriter(new FileWriter(selfId + "_nbrReceivedUpdates.txt"));
				out.write(nbrReceivedUpdates + "\n");
				out.close();
			} catch (IOException e) 
			{
				e.printStackTrace();
			}
			
		} else // The node is the broadcaster
		{
			// Wait, doing nothing, until the beginning of the session
			initTime = System.currentTimeMillis()/1000;
			System.out.println("Waiting to start (" + startTime + " seconds)");
			while (System.currentTimeMillis()/1000 - initTime < startTime);
			System.out.println("Session starts NOW");

			// Count new rounds until time limit
			initTime = System.currentTimeMillis()/1000;
			
			// If the duration of one round passed, treat a new round
			while (System.currentTimeMillis()/1000 - initTime < this.sessionDuration) 
			{ 	
				lastRoundTime = System.currentTimeMillis()/1000;

				// Trigger periodical messages of peer
				server.newRound();

				// Wait until the end of round
				while (System.currentTimeMillis()/1000 - lastRoundTime < this.roundDuration);
			}

			server.mustStop = true;
			server.sender.mustStop = true;

			int nbrSentUpdates = server.getNbrSentUpdates() - rte; // the subtraction corresponds to the final rounds, in which updates do not have enough time to propagate

			try 
			{
				BufferedWriter out = new BufferedWriter(new FileWriter("0_nbrSentUpdates.txt"));
				out.write(nbrSentUpdates + "\n");
				out.close();
			} catch (IOException e) 
			{
				e.printStackTrace();
			}
		} 
	}

}
