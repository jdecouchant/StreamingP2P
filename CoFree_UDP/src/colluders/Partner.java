package colluders;

import util.Node;

public class Partner 
{

    private Node node;
    private int period;
    private int nbrRoundLeft;
    private boolean initiatedByNode;
    
    Partner(Node node, int period, int rte, boolean initiatedByNode) {
    	this.node = node;
    	this.period = period;
    	this.nbrRoundLeft = period-1;
    	this.initiatedByNode = initiatedByNode;
    }
    
	public short getPartnerId() {
	    return this.node.nodeId;
	}

	public int getPartnerRoundLeft() {
		return this.nbrRoundLeft;
	}
	
	public boolean isNewPartner() {
		return (this.nbrRoundLeft == period - 1);
	}
	
	public boolean partnershipHasEnded() {
		return (this.nbrRoundLeft == 0);
	}
	
	public void newRound() {
		--(this.nbrRoundLeft);
	}
	
	public boolean getInitiatedByNode() {
		return this.initiatedByNode;
	}
}
