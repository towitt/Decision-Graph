package org.decisionGraph;

public class SelectBestOperation {
	
	private MessageLength ml;
	private boolean allowJoins;
	private boolean prefJoins;
	private int maxJoinNodes;

	public SelectBestOperation(MessageLength messageLength, boolean allowJoins, 
			boolean prefJoins, int maxJoinNodes){		
		this.allowJoins = allowJoins;
		this.prefJoins = prefJoins;
		this.ml = messageLength;
		this.maxJoinNodes = maxJoinNodes;
	}
	
	protected Operation select(DecisionGraph tree) {			
		
		SplitOperation split = SelectBestSplit.select(tree.getLeaves(), this.ml);
		if(!allowJoins) return split;
		
		JoinOperation join = SelectBestJoin.select(tree.getLeaves(), tree.getRoot(), tree.getML(), this.ml,
				this.maxJoinNodes);		
		
		if(this.prefJoins){		
			if(join.getSavings() > 0) return join;
			return split;
			}
			if(split.getSavings() < join.getSavings()) return join;
		return split;
	}
}
