package org.decisionGraph;

import java.util.ArrayList;

public class SelectBestSplit {
	
	/**
	 * Select the best split among all leaves
	 * @param leaves - the leaf nodes in the decision tree/graph
	 * @return the split operation producing the greatest savings in message length	
	 */
	protected static SplitOperation select(ArrayList<TreeNode> leaves, MessageLength ml) {
		
		// initialize variables (savings for best split should be greater than 0)
		SplitOperation bestSplit = new SplitOperation(null, null, 0.0);
		SplitOperation split;		
		
		// loop over all leaves to find the best split operation
		for(TreeNode v: leaves){
			
			// exclude empty and pure leafs (nothing can be done there)
			if(!(v.isEmpty()) && !(v.isPure())){	
				
				// was the best split already computed for the leaf? If yes, use it
				if(v.getBestSplit() != null) split = (SplitOperation) v.getBestSplit();
				
				// if no, select the best split
				else split = selectBestSplitNode(v, ml);
				
				// if the savings are greater than for all other splits so far, set the split as the best split
				if(split.getSavings() > bestSplit.getSavings()){
						bestSplit = split;
				}		
			}
		}							
		return bestSplit;
	}
	
	/**
	 * Select the best split operation for a specific node
	 * @param node - the node that should be split
	 * @param remainingAttributes - the attributes still available for splitting
	 * @return the split operation producing the greatest savings in message length
	 */
	private static SplitOperation selectBestSplitNode(TreeNode node, MessageLength ml) {
						
		// any attributes left for splitting?		
		if(node.getRemainingAttributes().size() == 0){		
			return new SplitOperation(null, null, 0.0);
		}
				
		// for all remaining attributes test the savings in message length if we split by the attribute 
		// savings should be greater than 0
		SplitOperation bestSplit = new SplitOperation(null, null, 0.0);
				
		// current local message length
		double currentML = ml.graphLength(node);
		
		// loop over all remaining attributes and perform tentative splits
		for(String s: node.getRemainingAttributes()){		
			
			// create split operation
			SplitOperation split = new SplitOperation(node, s, 0.0);
			
			// mark if split is on a continuous attribute			
			if(node.getRemainingData().getColTypes().get(s).equals("DoubleCell") ||
			   node.getRemainingData().getColTypes().get(s).equals("IntCell")){					
				split.setContinuousSplit();				
			}
			
			// calculate ML savings for the split on attribute s			
			split.setSavings(calculateMLsavings(split, currentML, ml));	
			
			// remember for which attributes the split produces positive savings
			if(split.getSavings() > 0.0) split.getNode().addSplitSavings(s);
			
			// if the savings are greater than any split tested before, set bestSplit to current attribute
			if(split.getSavings() > bestSplit.getSavings()){				
				bestSplit = split;
			} 
		}				
		
		// save the best split in the node for future iterations
		node.setBestSplit(bestSplit);
		
		return bestSplit;
	}		
	
	/**
	 * Calculate the savings in message length for a split operation
	 * @param split - the split for which the savings should be computed	 
	 * @param currentML - the message length of the leaf node before splitting
	 * @return the savings in message length for the split operation	
	 */
	private static double calculateMLsavings(SplitOperation split, double currentML, MessageLength ml) {
		
		double newML = 0.0;		
		
		// if attribute is categorical
		if(!split.getContinuousSplit()){
			
			// perform a tentative split
			split.perform();
			
			// new local message length
			newML += ml.treeLength(split.getNode());
		}
		
		// if attribute is numeric (integer or double values)
		else{				
			
			// create object that deals with splitting a continuous attribute
			ContinuousSplit cs = new ContinuousSplit(split.getNode().getRemainingData(), 
					split.getSplitAttribute(), ml);
			
			// if number the number of unique values of the attribute is <= 1 a split makes no sense
			if(cs.getUniqueValues() < 2) newML = currentML;
			
			else{
				// select best split value
				double cutValue = cs.selectBestCutValue();
				
				// set the cut value for the split				
				split.setCutValue(cutValue);
							
				// perform a tentative split at the cut-off value
				split.perform();
			
				// new local message length
				newML += ml.treeLength(split.getNode());
			
				// add bits to specify the cut-off value				
				newML += (Math.log(cs.getUniqueValues() - 1)) / Math.log(2);					
			}				
		}	
		
		// calculate the savings accomplished with the split		
		double MLsavings = currentML - newML;		
		
		// reverse the split
		split.reverse();
		
		return MLsavings;
	}	

}
