package colluders;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import util.Node;

public class Colluders 
{

    // Parameters for the Colluders protocol

    // The following values must verify the relation (updatesLen * 8 * updatesNbrPerRound) = 300000 * roundDuration
    private static final int ROUND_DURATION = 1; // Duration of a round in seconds
    private static final int UPDATE_LEN = 938; // Length of one update in bytes
    private static final int UPDATES_PER_ROUND = 40; // Number of updates the source releases at each round

    private final int RTE; // Number of rounds updates are released before being consumed by media applications

    // Simulations parameters
    private int SESSION_DURATION;

    private Peer peer;
    private Server server;

    private static final boolean OUTPUT = true;

    public Colluders (short selfId, int listeningPort, int RTE, int FANOUT_CLI, int FANOUT_SVR, int PERIOD, int EPOCH, ArrayList<Node> nodesList, 
            int scenarioId, int sessionDuration) {
        this.RTE = RTE;
        this.SESSION_DURATION= sessionDuration; 
        if (selfId != 0) // The node is a peer
            this.peer = new Peer(selfId, listeningPort, nodesList, FANOUT_CLI, PERIOD, RTE, UPDATES_PER_ROUND, UPDATE_LEN, scenarioId, EPOCH);
        else // The node is the broadcaster
            this.server = new Server(listeningPort, nodesList, FANOUT_SVR, UPDATES_PER_ROUND, UPDATE_LEN, EPOCH);   
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
        BufferedWriter out = null;

        if (selfId != 0) // The node is a peer
        {
            try {
                if (OUTPUT) {
                    out = new BufferedWriter(new FileWriter(selfId + "_nbDeletedUpdates.txt"));
                    out.write(peer.selfId + " " + RTE + " " + UPDATES_PER_ROUND + "\n");
                }

                long currentTimeMillis = System.currentTimeMillis();
                long nextMinuteMillis = (1 + currentTimeMillis/60000)*(60000);

                try {
                    sleepAtLeast(nextMinuteMillis - currentTimeMillis);
                } catch (InterruptedException e) {}

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
                    int [] results = peer.endOfRound();
                    if (OUTPUT)
                        out.write(peer.getRoundId() + " " + results[0] + " " + results[1] + "\n"); // roundId, nodeStatus, nbDeletedUpdates

                }

                if (OUTPUT)
                    out.close();

                peer.sender.mustStop = true;
                peer.receiver.mustStop = true;
                if (OUTPUT) {
                    try {
                        peer.outLog.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }

            } catch (IOException e)
            {
                e.printStackTrace();
            }

        } else // The node is the broadcaster
        {
            long currentTimeMillis = System.currentTimeMillis();
            long nextMinuteMillis = (1 + currentTimeMillis/60000)*(60000);

            try {
                sleepAtLeast(nextMinuteMillis - currentTimeMillis);
            } catch (InterruptedException e) {}

            System.out.println("Session starts NOW");

            // Count new rounds until time limit
            initTime = System.currentTimeMillis()/1000;

            // If the duration of one round passed, treat a new round
            while (System.currentTimeMillis()/1000 - initTime < SESSION_DURATION) 
            {   
                lastRoundTime = System.currentTimeMillis()/1000;

                // Trigger periodical messages of peer
                server.newRound();

                // Wait until the end of round
                while (System.currentTimeMillis()/1000 - lastRoundTime < ROUND_DURATION)
                    server.treatInMessage();
            }

            server.stop();

            int nbrServeRounds = server.getNbServeRounds() - RTE; // The subtraction is related to the final rounds, for which updates cannot expire
            if (OUTPUT) {
                try {
                    out = new BufferedWriter(new FileWriter("0_nbrSentUpdates.txt"));
                    out.write(nbrServeRounds + "\n");
                    out.close();
                } catch (IOException e) 
                {
                    e.printStackTrace();
                }
            }
        } 
    }

}
