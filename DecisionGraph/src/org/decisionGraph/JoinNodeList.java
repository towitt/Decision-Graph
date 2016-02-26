package org.decisionGraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class JoinNodeList {
	
	private Set<TreeNode> newNodes; // (join) nodes added in current iteration
	private Set<TreeNode> oldNodes; // (join) nodes added in previous iteration
	private Set<TreeNode> pendingNodes; // nodes not involved in join in current iteration
	private Set<TreeNode> joinNodes; // nodes involved in join in current iteration
	private ArrayList<Set<TreeNode>> joinGroups; // the groups of nodes which are joined together
	
	/**
	 * Constructor
	 */
	public JoinNodeList(){
		this.newNodes = new HashSet<TreeNode>();
		this.oldNodes = new HashSet<TreeNode>();
		this.joinNodes = new HashSet<TreeNode>();
		this.pendingNodes = new HashSet<TreeNode>();
		this.joinGroups = new ArrayList<Set<TreeNode>>();		
	}
	
	/**
	 * Method to update the join node list
	 * The aim is to detect the join pattern among joining nodes in current iteration. A joining pattern consists 
	 * of at least two nodes which form a new common child. Thus, nodes that have a common child are joining 
	 * nodes in this iteration, the remaining nodes will be declared as pending nodes.
	 */
	protected void updateList(){

		// get all nodes currently in the join node list
		HashSet<TreeNode> nodeSet = Sets.newHashSet();
		nodeSet.addAll(this.newNodes);
		nodeSet.addAll(this.oldNodes);		
		
		// create a multi-map in which the parent/child combinations are saved
		Multimap<TreeNode, TreeNode> joinPatternMap = ArrayListMultimap.create();
		
		// loop over the join nodes in this iteration
		for(TreeNode node : nodeSet){
			
			// get the single child of the join node			
			TreeNode child = node.getChildren().get(0);					
			
			// put the parent/child pair in the map			
			joinPatternMap.put(child, node);					
		}		
		
		/*
		 * now, detect the nodes that actually participate in a join in this iteration. Add the join group to 
		 * the list of all join groups and declare the other nodes as pending nodes.
		 */		 
		for(TreeNode child : joinPatternMap.keySet()){			
			HashSet<TreeNode> joinGroup = Sets.newHashSet(joinPatternMap.get(child));
			
			if(joinGroup.size() > 1){
				this.joinGroups.add(joinGroup);
				this.joinNodes.addAll(joinGroup);
			}
			else{
				this.pendingNodes.addAll(joinGroup);
			}
		}				
	}	
	
	/**
	 * Method to tell the list that a new iteration started. All new nodes not involved in a join in the
	 * previous iteration become old nodes in the new iteration. The nodes involved in a join in the previous
	 * are removed from the list. 
	 */
	public void newIteration(){
		this.oldNodes.addAll(newNodes);		
		this.oldNodes.removeAll(this.joinNodes);
		this.newNodes.clear();	
		this.joinGroups.clear();
		this.pendingNodes.clear();
		this.joinNodes.clear();
	}
	
	public void add(TreeNode node){
		this.newNodes.add(node);		
	}
	
	public Set<TreeNode> getPendingNodes() {
		return pendingNodes;
	}

	public Set<TreeNode> getJoinNodes() {
		return joinNodes;
	}

	public ArrayList<Set<TreeNode>> getJoinGroups() {
		return joinGroups;
	}	
	
	public Set<TreeNode> getNewNodes() {
		return newNodes;
	}
	
	public Set<TreeNode> getOldNodes() {
		return oldNodes;
	}	
	
}
