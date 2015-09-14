package colluders;

import util.Node;

public class Partner 
{

    private Node node;
    private int period;
    private int nbrRoundLeft;
    
    Partner(Node node, int period, int rte)
    {
    	this.node = node;
    	this.period = period;
    	this.nbrRoundLeft = period-1;
    }
    
	public short getPartnerId() {
	    return this.node.getId();
	}

	public int getPartnerRoundLeft()
	{
		return this.nbrRoundLeft;
	}
	
	public boolean isNewPartner()
	{
		return (this.nbrRoundLeft == period - 1);
	}
	
	public boolean partnershipHasEnded()
	{
		return (this.nbrRoundLeft == 0);
	}
	
	public void newRound()
	{
		--(this.nbrRoundLeft);
	}
}
