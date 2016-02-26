package org.decisionGraph;

public class SelectBestOperation {
	
	private MessageLength ml;
	private boolean allowJoins;
	private int maxJoinNodes;

	public SelectBestOperation(MessageLength messageLength, boolean allowJoins, int maxJoinNodes){		
		this.allowJoins = allowJoins;
		this.ml = messageLength;
		this.maxJoinNodes = maxJoinNodes;
	}
	
	protected Operation select(DecisionGraph tree) {			
		
		SplitOperation split = SelectBestSplit.select(tree.getLeaves(), this.ml);
		if(!allowJoins) return split;
		
		JoinOperation join = SelectBestJoin.select(tree.getLeaves(), tree.getRoot(), tree.getML(), this.ml,
				this.maxJoinNodes);
		
		if(split.getSavings() < join.getSavings()) return join;
		return split;
	}
}
