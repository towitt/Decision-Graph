package org.decisionGraph;

import java.util.ArrayList;
import java.util.Set;

import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;

public class DecisionGraph{
	
	private TreeNode root;
	public Set<String> attributes;
	public Data trainingData;
	private ArrayList<TreeNode> leaves = new ArrayList<TreeNode>();	
	private double messageLength;
	private MessageLength ml;
	private boolean allowJoins;
	private boolean prefJoins;
	private int maxJoinNodes;
	
	/**
	 * Constructor 
	 * @param trainingData - the data used to train the classifier
	 * @param alpha - the parameter of the symmetric Beta prior distribution
	 * @param allowJoins - if joins are allowed a decision graph instead of a tree is constructed
	 * @param maxJoinNodes - the maximum number of nodes involved in a join
	 */
	public DecisionGraph(Data trainingData, MessageLength ml, boolean allowJoins, 
			boolean prefJoins, int maxJoinNodes){
		this.root = null;
		this.trainingData = trainingData;	
		this.attributes = trainingData.getAttributes();
		this.ml = ml;		
		this.allowJoins = allowJoins;	
		this.prefJoins = prefJoins;
		this.maxJoinNodes = maxJoinNodes;		
		this.learnGraph();
	}	
		
	/**
	 * Method to learn the Decision Graph 
	 */
	public void learnGraph(){										
						
		// create object to select the best operation
		SelectBestOperation selectOp = new SelectBestOperation(this.ml, this.allowJoins, this.prefJoins, 
				this.maxJoinNodes);		

		// create root node		
		this.createRootNode();						
		boolean grow = true;	
		while(grow){
			
			// select the best operation (split or join) as measured by ML to further grow the graph			
			Operation bestOp = selectOp.select(this);						
			
			// if nothing can be done to reduce message length, end the while-loop	
			if(bestOp.getSavings() <= 0) grow = false;								
		
			// if message length can be reduced, perform operation on the graph
			else{						
				bestOp.perform();
				bestOp.updateLeaves(this.leaves);				
				this.updateMessageLength(bestOp.getSavings());								
			}				
		}			
	}	
	
	/**
	 * Method to start the classification of a data record. 
	 * @param row - the data record that should be classified	
	 * @return the predicted class value for the data record
	 */
	protected String classify(DataRow row){			
		return classify(row, this.root);
	}
	
	/**
	 * Private method to classify a data record. 
	 * @param row - the data record that should be classified	
	 * @param node - node in the graph
	 * @return the predicted class value for the data record
	 */
	private String classify(DataRow row, TreeNode node){		
		
		// if the node is a leaf, return the most frequent class in the node
		if(node.isLeaf()){					
			return node.getMostFreqClass().toString();
		}
		
		// if the node is a join node, just skip it and follow the (single) branch
		else if(node.isJoin()){			
			return classify(row, node.getChildren().get(0));
		}
		
		// if the node is an inner node, check which branch to follow
		else{			
			
			// get the splitting attribute in the node and check which column contains it
			String attr = node.getSplitAttribute();					
			int col = this.trainingData.getColIndex(attr);		
			
			// Distinguish between a split on a (1) categorical and a (2) numerical/continuous attribute		
			// Case (1)
			if(node.getContinuousSplit()){				
															
				// there can only be two children - get them
				TreeNode v1 = node.getChildren().get(0);
				TreeNode v2 = node.getChildren().get(1);
				
				// convert the cut-value and the observed value to doubles 
				double cutValue = ((DoubleValue) v1.getParentSplitValue()).getDoubleValue();
				double value = ((DoubleValue) row.getCell(col)).getDoubleValue();				
				
				// if value is smaller or equal than the cut-value, follow the left branch
				if(value <= cutValue) return classify(row, v1);
				
				// if value is larger than the cut-value, follow the right branch
				else return classify(row, v2);														
			}
			
			// Case (2)
			else{											
				// looper over all children
				for(TreeNode v : node.getChildren()){	
					
					// follow the branch that corresponds to the value of the data record on the splitting attribute 
					if(v.getParentSplitValue().equals(row.getCell(col))){					
						return classify(row, v);
					}			
				}
			}
		}		
		return null;
	}
	
	/**
	 * Method to create the root node of the graph
	 */
	private void createRootNode(){
		TreeNode u = new TreeNode(null, null, this.trainingData); 
		this.root = u;		
		u.computeClassFreq();
		this.addLeaf(u);		
		this.updateMessageLength();
	}
	
	public void setMessageLength(double messageLength){
		this.messageLength = messageLength;
	}
	
	public double getML(){
		return this.messageLength;
	}
	
	public void updateMessageLength(){
		this.messageLength = this.ml.graphLength(this.root);
	}
	
	public void updateMessageLength(double savings){
		this.messageLength = this.messageLength - savings;
	}
	
	public Set<String> getAttributes(){
		return this.attributes;
	}
	
	public ArrayList<TreeNode> getLeaves(){
		return this.leaves;
	}
	
	public void addLeaf(TreeNode leaf){
		this.leaves.add(leaf);
	}
	
	public TreeNode getRoot(){
		return this.root;
	}

}
