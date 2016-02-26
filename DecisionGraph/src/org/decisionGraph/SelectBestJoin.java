package org.decisionGraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Maps;

public class SelectBestJoin {
		
	/**
	 * Method to select the best join operation for the graph. 
	 * @param nodes - list of nodes that could be joined (i.e., the leaf nodes of the graph)
	 * @param root - the root of the graph (needed to calculate message length savings)
	 * @param currentML - the current message length for the graph
	 * @param ml - object that provides methods to calculate the message length
	 * @param maxJoinNodes - the maximum number of nodes involved in a join (set by user)
	 * @return the best join operation as measured by communication savings
	 */
	protected static JoinOperation select(ArrayList<TreeNode> nodes, TreeNode root, double currentML, 
			MessageLength ml, int maxJoinNodes){
		
		// initialize best join operation object
		JoinOperation bestJoin = new JoinOperation(null, 0.0);		
		
		// initialize a set which saves all tested join combinations
		Set<int[]> testedComb = new HashSet<int[]>();		
		
		// create a map which saves for each attribute the nodes with split savings on the attribute
		HashMap<String, ArrayList<TreeNode>> savingsMap = createSavingsMap(nodes);
				
		// loop over entries of the savings map and test all join combinations to search for the best join
		for(String attribute : savingsMap.keySet()){
			
			// test the join combination
			JoinOperation join = testJoinCombinations(savingsMap.get(attribute), root, currentML, ml, 
					maxJoinNodes, testedComb);
			
			// replace the best join if the savings of the tested join are larger
			if(join.getSavings() > bestJoin.getSavings()){
				bestJoin = join;
			}
		}		
		return bestJoin;
	}
	
	/**
	 * Method to create a map that lists for the remaining attributes the nodes for which a split on the attribute
	 * produces communication savings. The map is useful because only leaves with communication savings when 
	 * splitting on an identical attribute can form a join that has a possible message length savings.
	 * @param nodes - a list of nodes that could be joined
	 * @return communication savings map
	 */
	protected static HashMap<String, ArrayList<TreeNode>> createSavingsMap(ArrayList<TreeNode> nodes){
		
		// create a map which saves for each attribute the nodes with split savings on the attribute				
		HashMap<String, ArrayList<TreeNode>> savingsMap = Maps.newHashMap();
		
		// loop over the leaf nodes to fill the map
		for(TreeNode node : nodes){
			
			// check if leaf itself results from a join (then it can't be involved in a join in this iteration)
			if(node.getParent() == null || !(node.getParent().isJoin())){	
				
				// loop over the node's split saving attributes
				for(String attribute : node.getSplitSavings()){
				
					// attribute already included in map: just add the tree node
					if(savingsMap.containsKey(attribute)){
						savingsMap.get(attribute).add(node);
					}
				
					// otherwise: put new attribute/node pair in the map
					else{
						savingsMap.put(attribute, new ArrayList<TreeNode>(Arrays.asList(node)));
					}
				}
			}
		}
		return savingsMap;		
	}
	
	/**
	 * Method to test all join combinations for a given list of nodes. For each combination the communication 
	 * savings are calculated and the combination with the largest savings is returned. 
	 * @param nodes - a list of nodes that could be joined
	 * @param currentML - the current message length for the tree
	 * @param ml - object providing methods to calculate the message length	
	 * @param maxJoinNodes - the maximum number of nodes involved in a join (set by user)
	 * @param testedComb - the join combinations already tested
	 * @return the join operation with the highest communication savings
	 */
	protected static JoinOperation testJoinCombinations(ArrayList<TreeNode> nodes, TreeNode root, 
			double currentML, MessageLength ml, int maxJoinNodes, Set<int[]> testedComb){				
		
		// the number of nodes possibly involved in a join		
		int N = nodes.size();		
		
		// consider restriction on the max. number of nodes in a join (as set in the node dialog)
		if(maxJoinNodes <= N){
			N = maxJoinNodes;
		}
		
		// initialize best join operation object
		JoinOperation bestJoin = new JoinOperation(null, 0.0);
		
		// if there is only one node, no join is possible
		if(N == 1) return bestJoin;
			
		// test all combinations -> binomial(N, K)
		for(int K = 2; K < N; K++){
							
			// get the combination by index        
			int combination[] = new int[K];
         
			// position of current index
			int r = 0;      
			int index = 0;
		
			while(r >= 0){
				if(index <= (N + (r - K))){
					combination[r] = index;                    
                
					// if we are at the last position print and increase the index
					if(r == K-1){
                	
						// at this point we obtained one combination pattern of the desired size												
						// check if combination was already tested
						if(!testedComb.contains(combination)){														
						
							// add nodes included in this combination to new node list
							ArrayList<TreeNode> joinNodes = new ArrayList<TreeNode>();          
							for(int c : combination){
								joinNodes.add(nodes.get(c));                		
							}														
							
							// if all nodes have the same parent and this parent has no other children
							// the set of nodes cannot form a join (this is checked here)
							if(checkForCommonParent(joinNodes)){
							
								// create a join operation
								JoinOperation join = new JoinOperation(joinNodes, 0.0);
                								
								// calculate the ML savings obtained with the join
								double savings = calculateMLsavings(join, root, currentML, ml);
								join.setSavings(savings);
				
								// if the savings are greater than for all other joins so far,
								// set the join as the best join operation                
								if(join.getSavings() > bestJoin.getSavings()){
									bestJoin = join;
								}	
								
								// add combination to set of tested combinations
								testedComb.add(combination);
							}
						}
                	
						// increase index
						index++;                
					}
					else{
						// select index for next position
						index = combination[r]+1;                    
						r++;                                        
					}
				}
				else{
					r--;
					if(r > 0)
						index = combination[r]+1;
					else
						index = combination[0]+1;   
				}				
			}			
        } 
		
		return bestJoin;		
	}
	
	/**
	 * Method to calculate the communication savings for a join operation.
	 * @param join - a join operation
	 * @param root - the root of the decision graph
	 * @param currentML - the current message length for the graph
	 * @param ml - object providing methods to calculate the message length
	 * @return communication savings of join operation
	 */
	protected static double calculateMLsavings(JoinOperation join, TreeNode root, double currentML, 
			MessageLength ml){	
		
		// perform the join
		join.perform();		
		
		// calculate the new message length of the graph		
		double newML = ml.graphLength(root);
		
		// calculate the savings obtained with the join
		double savings = currentML - newML;
		
		// reverse the join
		join.reverse();
		
		return savings;
	}	
		
	/**
	 * Method to check if all nodes have the same parent and this parent has no other children. 
	 * In this case the nodes cannot form a join.
	 * @param joinNodes - nodes potentially involved in a join
	 * @return true if join is eligible, otherwise false
	 */
	private static boolean checkForCommonParent(ArrayList<TreeNode> joinNodes){
		Set<TreeNode> parents = new HashSet<TreeNode>();
		for(TreeNode v : joinNodes) parents.add(v);
		if(parents.size() > 1) return true;
		else{
			TreeNode parent = parents.iterator().next();
			if(parent.getChildren().containsAll(joinNodes)) return false;		
		}
		return true;		
	}
	
}
