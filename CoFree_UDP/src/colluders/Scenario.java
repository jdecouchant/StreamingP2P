package colluders;

public class Scenario {

	private int scenarioId;
	private int nbNodes;
	//	private int period;

	public Scenario(int scenarioId, int nbNodes, int period) {
		this.scenarioId = scenarioId;
		this.nbNodes = nbNodes;
		//		this.period = period;
	}

	public boolean nodeMustQuit(int nodeId, int roundId) {
		int massiveDepartureRound = 500;
		if (scenarioId <= 4) // Perfect case and massive join
			return false;
		else if (5 <= scenarioId && scenarioId <= 7) {// Massive departure
			switch (scenarioId) {
			case 5: // 50% of the nodes leave
				return (roundId == massiveDepartureRound && nodeId % 2 == 0);
			case 6: // 60% of the nodes leave
				return (roundId == massiveDepartureRound && nodeId % 10 < 6);
			default: // 70% of the nodes leave
				return (roundId == massiveDepartureRound && nodeId % 10 < 7);
			}
		} else if (8 <= scenarioId && scenarioId <= 16) {
			return false;
		} else { 
			return false;
		}
	}

	// To be different from initial joins, a join event must happen after round 80
	// Could be improved by splitting the join among an epoch
	public boolean nodeMustJoin(int nodeId, int roundId) {
		int massiveJoinRound = 500;
		if (scenarioId == 0) {// Perfect case
			return (roundId < 20 && (nodeId + roundId) % 5 == 0);
		} else if (1 <= scenarioId && scenarioId <= 4) { // Massive join
			switch(scenarioId) {
			case 1: // 100% of join
				return ((2*nodeId > nbNodes && roundId == massiveJoinRound) || (2*nodeId <= nbNodes && roundId == 1));
			case 2: // 200% of join
				return ((3*nodeId > nbNodes && roundId == massiveJoinRound) || (3*nodeId <= nbNodes && roundId == 1));
			case 3: // 400% of join
				return ((5*nodeId > nbNodes && roundId == massiveJoinRound) || (5*nodeId <= nbNodes && roundId == 1));
			default: // 800% of join
				return ((9*nodeId > nbNodes && roundId == massiveJoinRound) || (9*nodeId <= nbNodes && roundId == 1));
			}
		} else if (scenarioId <= 7) {// Massive departure
			return (roundId < 20 && (nodeId + roundId) % 5 == 0);
		} else if (8 <= scenarioId && scenarioId <= 16) { // Colluders
			return (roundId < 20 && (nodeId + roundId) % 5 == 0);
		} else {
			return false;
		}
	}

	public boolean nodeIsColluder(int roundId, int nodeId) {
		int attackRound = 500;
		if (0 <= scenarioId && scenarioId <= 7)
			return false;
		else if (8 <= scenarioId  && scenarioId <= 16) {// Colluders
			switch (scenarioId) {
			case 8:
				return (roundId >= attackRound && 100*nodeId < 10*nbNodes); // 10% of colluders
			case 9:
				return (roundId >= attackRound && 100*nodeId < 20*nbNodes);
			case 10:
				return (roundId >= attackRound && 100*nodeId < 30*nbNodes);
			case 11:
				return (roundId >= attackRound && 100*nodeId < 40*nbNodes);
			case 12:
				return (roundId >= attackRound && 100*nodeId < 50*nbNodes);
			case 13:
				return (roundId >= attackRound && 100*nodeId < 60*nbNodes);
			case 14:
				return (roundId >= attackRound && 100*nodeId < 70*nbNodes);
			case 15:
				return (roundId >= attackRound && 100*nodeId < 80*nbNodes);
			default:
				return (roundId >= attackRound && 100*nodeId < 90*nbNodes);
			}
		} else
			return false;
	}
}
