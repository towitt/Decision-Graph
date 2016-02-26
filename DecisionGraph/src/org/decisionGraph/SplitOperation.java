package org.decisionGraph;

import java.util.ArrayList;

public class SplitOperation extends Operation{

	private TreeNode node;
	public String splitAttribute;
	private Boolean continuousSplit;
	private Double cutValue;	
	
	/**
	 * Constructor 
	 * @param node - the node at which the operation on the tree should be performed
	 * @param splitAttribute - the attribute used to split the node 
	 * @param savings - the savings in message length accomplished with the operation
	 */
	public SplitOperation(TreeNode node, String splitAttribute, double savings) {		
		this.node = node;
		this.splitAttribute = splitAttribute;		
		this.continuousSplit = false;
		this.cutValue = null;
		this.savings = savings;	
	}
	
	@Override
	public void perform() {
		
		// split on a continuous attribute
		if(this.continuousSplit) this.getNode().splitByAttribute(this.getSplitAttribute(), this.cutValue);		
		
		// split on a categorical attribute
		else this.getNode().splitByAttribute(this.getSplitAttribute());
	}
	
	@Override
	public void reverse(){ 
		this.node.reverseSplit();
	}
	
	@Override
	public void updateLeaves(ArrayList<TreeNode> leaves){
		leaves.remove(this.node);
		leaves.addAll(this.node.getChildren());
	}
	
	@Override
	public void getInfo(){ 
		System.out.println("Performing a split on attribute " + this.splitAttribute);
		if(this.continuousSplit) System.out.println("The cut value is " + this.cutValue);
		System.out.println("Savings are " + Math.round(this.savings) + " bits (rounded)");
	}

	public void setNode(TreeNode node) {
		this.node = node;
	}
	
	public TreeNode getNode() {
		return node;
	}

	public void setSplitAttribute(String attribute) {
		this.splitAttribute = attribute;
	}

	public String getSplitAttribute() {
		return splitAttribute;
	}
	
	public void setContinuousSplit(){
		this.continuousSplit = true;
	}
	
	public Boolean getContinuousSplit(){
		return this.continuousSplit;
	}
	
	public void setCutValue(double cutValue){
		this.cutValue = cutValue;
	}
	
	public double getCutValue(){
		return this.cutValue;
	}
	
}
