package org.decisionGraph;

import java.util.ArrayList;

public class JoinOperation extends Operation{
	
	private ArrayList<TreeNode> nodes;
	private TreeNode joinedNode;
	
	/**
	 * Constructor
	 * @param nodes - the nodes that are joined together
	 * @param savings - the communication savings accomplished with the join operation
	 */
	public JoinOperation(ArrayList<TreeNode> nodes, double savings){
		this.nodes = nodes;
		this.savings = savings;
		this.joinedNode = null;
	}
	
	@Override
	public void perform() {
		joinNodes(this.nodes);	
	}
	
	@Override
	public void reverse() {
		reverseJoinNodes(this.nodes);
	}
	
	@Override
	public void updateLeaves(ArrayList<TreeNode> leaves){
		leaves.removeAll(this.nodes);
		leaves.add(this.joinedNode);
	}
	
	@Override
	public void getInfo(){ 
		System.out.println("Performing a Join of " + this.nodes.size() + " nodes");
		System.out.println("Savings are " + Math.round(this.savings) + " bits (rounded)");
	}
	
	/**
	 * Method to join together a list of nodes.
	 * @param nodes - list of nodes involved in the join
	 */
	public void joinNodes(ArrayList<TreeNode> nodes){
	
		// join the data rows
		Data remainingData = nodes.get(0).getRemainingData();		
		for(int i = 1; i < nodes.size(); i++){
			remainingData = remainingData.join(nodes.get(i).getRemainingData());
		}
		
		// create the new (joined) node and compute the class frequencies
		TreeNode newNode = new TreeNode(nodes.get(0), null, remainingData);
		newNode.computeClassFreq();
		this.joinedNode = newNode;
		
		// add the new node to the list of children of the parent nodes
		for(TreeNode v : nodes){
			v.addChild(newNode);
		}
	}
	
	/**
	 * Method to reverse a join operation.
	 * @param nodes - the nodes involved in the join that should be reversed
	 */
	public void reverseJoinNodes(ArrayList<TreeNode> nodes){
		for(TreeNode v : nodes){
			v.setChildren(null);
		}
	}
	
	public ArrayList<TreeNode> getNodes(){
		return this.nodes;
	}
	
	public TreeNode getJoinedNode(){
		return this.joinedNode;
	}
	
}
