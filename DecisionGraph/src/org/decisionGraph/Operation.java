package org.decisionGraph;

import java.util.ArrayList;

/**
 * Abstract class for graph/tree operations. 
 * Extended by the SplitOperation and JoinOperation classes.
 */
public abstract class Operation {
	
	// the communication savings produced by the operation
	public double savings;
	
	// method to perform the operation 
	public abstract void perform();
	
	// method to reverse the operation (only works when no other join or split operation is performed meanwhile)
	public abstract void reverse(); 
	
	// update the leaves of the graph after the operation was performed
	public abstract void updateLeaves(ArrayList<TreeNode> leaves);
	
	// method to print information on the operation
	public abstract void getInfo();
	
	public void setSavings(double savings) {
		this.savings = savings;
	}
	
	public double getSavings() {
		return savings;
	}	
}
