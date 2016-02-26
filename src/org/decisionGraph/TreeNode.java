package org.decisionGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.def.DoubleCell;

public class TreeNode {
	private String splitAttribute;
	private TreeNode parent;
	private DataCell parentSplitValue;
	private ArrayList<TreeNode> children;
	private Data remainingData;
	private Set<String> remainingAttributes;	
	private Map<DataCell, Integer> classFreq;
	private ArrayList<DataCell> classVec;
	private Boolean continuousSplit;
	private SplitOperation bestSplit;
	private Set<String> splitSavings;
	
	/**
	 * Constructor for a node 	
	 * @param parent - the parent of this node
	 * @param parentSplitValue - the value of the parent attribute leading to this node
	 * @param remainingData - the partition of the original data created by traversing the tree up to this node
	 * @param remainingAttributes - the attributes not yet used for splitting
	 * @param classVec - vector of class values for the remaining elements
	 */
	public TreeNode(TreeNode parent, 
			DataCell parentSplitValue, 			
			Data remainingData){
		this.parent = parent;
		this.parentSplitValue = parentSplitValue;
		this.children = null; 
		this.splitAttribute = null;							
		this.remainingData = remainingData;
		this.remainingAttributes = remainingData.getAttributes();
		this.classVec = remainingData.getClassVec();
		this.classFreq = null;	
		this.continuousSplit = false;
		this.bestSplit = null;
		this.splitSavings = new HashSet<String>();
	}
	
	/**
	 * Method to split the node
	 * @param splitAttribute - the attribute used for splitting
	 */	
	public void splitByAttribute(String splitAttribute) {		
		
		// get the (remaining) data in the node that should be partitioned
		Data data = this.getRemainingData();		
		data.setAttributes(this.getRemainingAttributes());
		
		// create a new ArrayList to save the children
		this.children = new ArrayList<TreeNode>();
		
		// get the subsets
		HashMap<DataCell, Data> subsetMap = data.partition(splitAttribute);	
		
		// loop over the subsets and create new leaf nodes
		for(DataCell condValue : subsetMap.keySet()){					
			TreeNode u = new TreeNode(this, condValue, subsetMap.get(condValue));	
			u.computeClassFreq();
			this.addChild(u);					
		}		
		
		// save the splitting attribute in the node
		this.setSplitAttribute(splitAttribute);		
	}
	
	/**
	 * Method to split the node by a continuous attribute
	 * @param splitAttribute - the continuous attribute used for splitting
	 */
	public void splitByAttribute(String splitAttribute, Double cutValue) {
				
		// get the (remaining) data in the node that should be partitioned
		Data data = this.getRemainingData();		
		data.setAttributes(this.getRemainingAttributes());
		
		// create a new ArrayList to save the children
		this.children = new ArrayList<TreeNode>();
			
		// split the data by the cut-off value
		Data[] subsets = data.partition(splitAttribute, cutValue);					
		
		// create the two new leaf nodes
		TreeNode u1 = new TreeNode(this, new DoubleCell(cutValue), subsets[0]);	
		u1.computeClassFreq();
		this.addChild(u1);			
		
		TreeNode u2 = new TreeNode(this, new DoubleCell(cutValue), subsets[1]);	
		u2.computeClassFreq();
		this.addChild(u2);		
		
		// save the splitting attribute in the node
		this.setSplitAttribute(splitAttribute);
		this.setContinuousSplit();		
	}		
	
	/**
	 * Method to reverse the split at a node
	 */
	public void reverseSplit(){
		this.setChildren(null); 
		this.setSplitAttribute(null);
		this.setContinuousSplit(false);
	}
	
	/**
	 * Method to compute a map which holds the frequency of each class value in the node
	 */
	public void computeClassFreq(){
		
		// if no observation is left, we use the frequency table of the parent
		if(this.classVec.size() == 0){
			this.setClassFreq(this.getParent().getClassFreq());
			return;
		}
		
		// loop over the instances in the node and fill the map
		Map<DataCell, Integer> classFreq = new HashMap<DataCell, Integer>();		
		for(DataCell c : this.getClassVec()){			
			
			// put class/frequency pair in the map
			if(!classFreq.containsKey(c)){
				classFreq.put(c, 0);
			}			
			
			// update class frequency
			classFreq.put(c, classFreq.get(c) + 1);					
		} 
		
		// set the class frequency table of the leaf node
		this.setClassFreq(classFreq);		
	}
	
	/**
	 * Method to obtain the most frequent class value in a node
	 * @return the most frequent class (object of class DataCell)
	 */
	public DataCell getMostFreqClass(){
		Map.Entry<DataCell, Integer> maxEntry = null;
		for(Map.Entry<DataCell, Integer> entry : this.getClassFreq().entrySet()){
			if(maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0){
				maxEntry = entry;
			}
		}
		return maxEntry.getKey();
	}
	
	/**
	 * Check if all elements in a node have the same class (node is pure)
	 * @return true, if all elements have the same class
	 */
	public boolean isPure(){
		DataCell c1 = this.getClassVec().get(0);
		for(DataCell c : this.getClassVec()){
			if(!c.equals(c1)) return false;
		}
		return true;
	}
		
	/**
	 * Check if the node is a leaf
	 * @return true, if node is a leaf
	 */
	public boolean isLeaf(){
		if(this.children == null) return true; 
		return false;				
	}
	
	/**
	 * Check if the node is a join node
	 * @return true, if node is a join node
	 */
	public boolean isJoin(){
		if(this.children != null && this.children.size() == 1) return true;
		return false;
	}
	
	/**
	 * Check if the node is empty, i.e, no elements in the training data have this combination of attribute values
	 * @return true, if node is empty
	 */
	public boolean isEmpty(){
		if(this.classVec.size() == 0) return true;
		return false;
	}
			
	public void addChild(TreeNode child) {
		if(this.children == null) this.children = new ArrayList<TreeNode>();
		this.children.add(child);
	}
	
	public Set<String> getSplitSavings() {
		return splitSavings;
	}

	public void setSplitSavings(Set<String> positiveSplitSavings) {
		this.splitSavings = positiveSplitSavings;
	}
	
	public void addSplitSavings(String attribute) {
		if(this.splitSavings == null) this.splitSavings = new HashSet<String>();
		this.splitSavings.add(attribute);
	}
	
	public void setClassFreq(Map<DataCell, Integer> classFreq){
		this.classFreq = classFreq;
	}
	
	public  Map<DataCell, Integer> getClassFreq(){
		return this.classFreq;
	}
		
	public ArrayList<DataCell> getClassVec(){
		return this.classVec;
	}
	
	public void setSplitAttribute(String attribute) {
		this.splitAttribute = attribute;
	}
	
	public String getSplitAttribute() {
		return this.splitAttribute;
	}
	
	public void setParentSplitValue(DataCell value) {
		this.parentSplitValue = value;
	}
	
	public DataCell getParentSplitValue() {
		return this.parentSplitValue;
	}
		
	public void setParent(TreeNode parent) {
		this.parent = parent;
	}

	public TreeNode getParent() {
		return parent;
	}

	public void setChildren(ArrayList<TreeNode> children) {
		this.children = children;
	}
	
	public ArrayList<TreeNode> getChildren() {
		return this.children;
	}
	
	public Set<String> getRemainingAttributes(){
		return this.remainingAttributes;
	}
	
	public Data getRemainingData(){
		return this.remainingData;
	}
	
	public void setContinuousSplit(){
		this.continuousSplit = true;
	}
	
	public void setContinuousSplit(Boolean b){
		this.continuousSplit = b;
	}
	
	public Boolean getContinuousSplit(){
		return this.continuousSplit;
	}
		
	public void setBestSplit(SplitOperation split){
		this.bestSplit = split;
	}
	
	public SplitOperation getBestSplit(){
		return this.bestSplit;
	}
	
}
