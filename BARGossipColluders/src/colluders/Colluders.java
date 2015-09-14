package colluders;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import util.Node;

public class Colluders {

	// Parameters for the Colluders protocol

	// The following values must verify the relation (updatesLen * 8 * updatesNbrPerRound) = 300000 * roundDuration
	public static final int ROUND_DURATION = 1;
	public static final int UPD_LEN = 938; // in bytes
	public static final int UPD_NB_PER_ROUND = 40;
	// Number of rounds updates are released before they expire
	private final int RTE;

	// Simulations parameters
	private final int SESSION_DURATION;

	private Peer peer;
	private Server server;

	public Colluders (short selfId, int listeningPort, ArrayList<Node> nodesList, int GROUP_SIZE, int GROUP_NBR, int RTE, int BAL_SIZE, int PUSH_SIZE, 
			int PUSH_AGE, int FANOUT, int SESSION_DURATION) {
		this.RTE = RTE;
		this.SESSION_DURATION = SESSION_DURATION;
		if (selfId != 0) // The node is a peer
			this.peer = new Peer(selfId, listeningPort, nodesList, GROUP_SIZE, GROUP_NBR, RTE, BAL_SIZE, PUSH_SIZE, PUSH_AGE);
		else // The node is the broadcaster
			this.server = new Server(nodesList, FANOUT);	
	}

	public void sleepAtLeast(long millis) throws InterruptedException {
		long t0 = System.currentTimeMillis();
		long millisLeft = millis;
		while (millisLeft > 0) {
			Thread.sleep(millisLeft);
			long t1 = System.currentTimeMillis();
			millisLeft = millis - (t1 - t0);
		}
	}

	public void run(int selfId) {
		long initTime, lastRoundTime;

		if (selfId != 0) // The node is a peer
		{
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(selfId + "_nbDeletedUpdates.txt"));
				out.write(selfId + " " + RTE + " " + UPD_NB_PER_ROUND + "\n");

				long currentTimeMillis = System.currentTimeMillis();
				long nextMinuteMillis = (1 + currentTimeMillis/60000)*(60000);

				try {sleepAtLeast(nextMinuteMillis - currentTimeMillis);} catch (InterruptedException e) {}
				System.out.println("Session starts NOW");

				// Count new rounds until time limit
				initTime = System.currentTimeMillis()/1000;
				while (System.currentTimeMillis()/1000 - initTime < SESSION_DURATION) {

					// If the duration of one round passed, treat a new round
					lastRoundTime = System.currentTimeMillis()/1000;

					// Trigger periodical messages of peer
					peer.newRound();

					// Wait until the end of round
					while (System.currentTimeMillis()/1000 - lastRoundTime < ROUND_DURATION)
						peer.treatInMessage();

					// End of round: increase the round ID, update the log, and consume received updates
					// End of round: increase the round ID, update the log, and consume received updates
					int nbDeletedUpdates = peer.endOfRound();
					out.write(peer.getRoundId()+" 2 " + nbDeletedUpdates + "\n");
				}

				peer.mustStop = true;
				peer.sender.mustStop = true;

				out.close();
			} catch (IOException e) 
			{
				e.printStackTrace();
			}

		} else // The node is the broadcaster
		{
			long currentTimeMillis = System.currentTimeMillis();
			long nextMinuteMillis = (1 + currentTimeMillis/60000)*(60000);

			try {sleepAtLeast(nextMinuteMillis - currentTimeMillis);} catch (InterruptedException e) {}
			System.out.println("Session starts NOW");

			// Count new rounds until time limit
			initTime = System.currentTimeMillis()/1000;

			// If the duration of one round passed, treat a new round
			while (System.currentTimeMillis()/1000 - initTime < SESSION_DURATION) 	{ 	
				lastRoundTime = System.currentTimeMillis()/1000;

				// Trigger periodical messages of peer
				server.newRound();

				// Wait until the end of round
				while (System.currentTimeMillis()/1000 - lastRoundTime < ROUND_DURATION);
			}

			server.stop();

			int nbrSentUpdates = server.getNbrSentUpdates() - RTE; // the subtraction corresponds to the final rounds, in which updates do not have enough time to propagate

			try {
				BufferedWriter out = new BufferedWriter(new FileWriter("0_nbrSentUpdates.txt"));
				out.write(nbrSentUpdates + "\n");
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} 
	}

}
