package org.decisionGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;

import com.google.common.collect.Sets;
import com.google.common.math.IntMath;

public class MessageLength {
		
	private final double alpha; 
	private final double C;
	private Set<TreeNode> treeRootSet;
	private JoinNodeList joinList;
	
	/**
	 * Constructor 
	 * @param alpha - parameter of the Beta prior distribution over the unknown class probabilities
	 * alpha = 1: uniform prior, i.e., all class distributions are equally likely
	 * alpha = 0: singular prior, i.e., all weight is placed on the C possible single class distributions,
	 * where C is the number of distinct class values	 
	 */
	public MessageLength(double alpha, double C){
		this.alpha = alpha;
		this.C = C;		
	}
	
	/**
	 * Method to compute the message length for a Decision Graph (or a part of it)
	 * @param node - usually the root of the graph 
	 * @return the number of bits needed to transmit the decision graph (a.k.a. message length)
	 */
	protected double graphLength(TreeNode root){	
		
		// add the root of the graph to the set of roots of trees within the graph
		this.treeRootSet = Sets.newHashSet(root);
		
		// create this object to keep track of join nodes and joining patterns
		this.joinList = new JoinNodeList();
			
		/* 
		 * We now iteratively calculate the number of bits needed to transmit the graph, i.e., the costs to 
		 * communicate all trees within the graph and the joining patterns.
		 * In each subtree, there might again be joins so that new nodes are added to the root set. Thus, we
		 * need to iterate until no element is left in the root set.		
		 */		
		double ml = 0.0; // initialize message length
		int nroots = this.treeRootSet.size();
		while(nroots > 0){
			
			// tell the list that a new iteration is started
			this.joinList.newIteration();			
			
			// add the bits needed to communicate all subtrees
			Set<TreeNode> tested = Sets.newHashSet(this.treeRootSet);
			for(TreeNode v: tested){			
				ml += treeLength(v);			
			}
			this.treeRootSet.removeAll(tested);
			
			// check if join nodes were added to the list
			if(this.joinList.getNewNodes().size() > 0){
			
				// update the join node list (potentially, join nodes were added)
				this.joinList.updateList();		
				
				// add bits needed to transmit join pattern 
				if(joinList.getNewNodes().size() > 1){
					ml += encodeJoinPattern();				
				}
			}			
			nroots = this.treeRootSet.size();
		}
		return ml;
	}	
	
	/**
	 * Method to recursively calculate the number of bits needed to transmit a tree within the graph. 
	 * For this purpose, any potential join nodes in the graph are treated as leaf nodes.
	 * @param node - the root of the subtree.
	 * @return the number of bits needed to transmit the tree (a.k.a. message length)
	 */
	protected double treeLength(TreeNode node){
		
		// initialize variables
		double ml = 0.0; // message length
		double p; // probability that node is no leaf.
		
		/*
		 * Check out the parent of the node to distinguish two cases:
		 * (1) Node is the root of a subtree (i.e. actual root or node resulting from join)
		 * (2) Node is no root of a subtree
		 */
		TreeNode u = node.getParent();		
	
		// Case (1) 
		if(u == null|| u.isJoin()){		
			
			// probability that the root is NOT a leaf is 1 - (1/(number of attributes))			
			p = 1 - (1/(double) node.getRemainingAttributes().size());
		}
		
		// Case (2)
		else{
			
			// obtain the number of children of the parent node
			double b = (double) u.getChildren().size();	
			
			// probability that the node is NOT a leaf is	
			p = 1/b;			
		}
				
		/*
		 * Distinguish three cases to calculate the bits needed to transmit the node
		 * (1) node is a leaf -> add bits needed to transmit structure and category message
		 * (2) node is a join -> add bits needed to transmit structure message
		 * (3) node is a decision node -> add bits needed to transmit structure message and recurse over subtrees
		 */
		
		// Case (1)
		if(node.isLeaf()){	
			
			// add bits to transmit the leaf node
			ml += encodeLeafNode(node, p);
			
			// if no instances are left in the node, we use the class frequencies of the parent
			if(node.isEmpty()){							
				node.setClassFreq(node.getParent().getClassFreq());
			}						
		}
		
		// Case (2)
		else if(node.isJoin()){
			
			// add bits needed in the structure message
			if(p != 0) ml += (Math.log(1/(1 - p)) / Math.log(2));	
			
			// add join node to open list		
			this.joinList.add(node);			
			
			// add child of join node to set of new roots
			this.treeRootSet.add(node.getChildren().get(0));
			
		}
		
		// Case (3)
		else{					
			// add bits needed to encode inner node 
			ml += encodeInnerNode(node, p);
		
			// recurse over all subtrees
			for(TreeNode v: node.getChildren()){				
				ml += treeLength(v);
			}
		}	
		
		return ml;
	}	
	
	/**
	 * Method to calculate the number of bits needed to announce a "theory" about the distribution 
	 * of classes within a category (i.e., within a leaf node).
	 * To encode the message an incremental code is used (see Wallace & Patrick 1993)
	 * @param leaf - the leaf that should be encoded	
	 * @return the length of the encoded message
	 */
	private double encodeCategory(TreeNode leaf){
		
		// initialize message length
		double ml = 0.0;
				
		// create map to save the frequency of each class value 
		Map<DataCell, Integer> classFreq = new HashMap<DataCell, Integer>();	
		
		// loop over all elements in the class vector to encode the category
		int j = 0;
		for(DataCell c : leaf.getClassVec()){	
			
			// create entry for class value
			if(!classFreq.containsKey(c)){
				classFreq.put(c, 0);
			}
			
			// the count i_m (number of elements with class m, so far)
			double i = (double) classFreq.get(c);
			
			// calculate q (the expectation of the updated probability that instance j belongs to class m)
			double q = (i + this.alpha) / (j + (this.C * this.alpha));					
			if(q != 0) ml += (Math.log(1/q)) / (Math.log(2));
			
			// update frequency map
			classFreq.put(c, classFreq.get(c) + 1);		
			j++;
		} 				
		return ml;
	}
		
	/**
	 * Method to calculate the bits needed to communicate a leaf node.
	 * @param leaf - the leaf node
	 * @param p - the probability that the node is NOT a leaf
	 * @return the length of the encoded message
	 */
	private double encodeLeafNode(TreeNode leaf, double p){
		
		// add bits needed to express distribution of classes within leaf (category)
		double ml = encodeCategory(leaf);	
		
		// add bits to encode leaf node in tree structure message
		if(p != 0) ml += (Math.log(1/(1 - p)) / Math.log(2));	
				
		return ml;
	}
		
	/**
	 * Method to calculate the bits needed to communicate a decision node.
	 * @param node - the decision node
	 * @param p - the probability that the node is NOT a leaf	 
	 * @return the length of the encoded message
	 */
	private double encodeInnerNode(TreeNode node, double p){
		
		// initialize message length
		double ml = 0.0;
		
		// obtain the number of remaining attributes in this node
		double nattr = (double) node.getRemainingAttributes().size();
		
		// add bits needed to encode inner node in the structure message
		if(p != 0) ml += (Math.log(1/p) / Math.log(2));			
			
		// add bits needed to encode the name of the splitting attribute			
		if(nattr> 0) ml += (Math.log(nattr) / Math.log(2));			
		
		return ml;
	}
	
	/**
	 * Method to calculate the number of bits needed to announce a "theory" about the distribution 
	 * of classes within a subset of the data.
	 * To encode the message an incremental code is used (see Wallace & Patrick 1993)
	 * @param data - the subset
	 * @return the length of the encoded message
	 */
	protected double encodeSubset(Data data){
		
		// initialize variables
		double length = 0.0;				
		
		// create map to save the frequency of each class value 
		Map<DataCell, Integer> classFreq = new HashMap<DataCell, Integer>();	
		
		// loop over all elements in the class vector to encode the category
		int j = 0;
		for(DataCell c : data.getClassVec()){	
			
			// create entry for class value
			if(!classFreq.containsKey(c)){
				classFreq.put(c, 0);
			}
			
			// the count i_m (number of elements with class m, so far)
			double i = (double) classFreq.get(c);
			
			// calculate q (the expectation of the updated probability that instance j belongs to class m)
			double q = (i + this.alpha) / (j + (this.C * this.alpha));					
			if(q != 0) length += (Math.log(1/q)) / (Math.log(2));
			classFreq.put(c, classFreq.get(c) + 1);		
			j++;
		} 
		
		return length;
	}
	
	/**
	 * Method to calculate the number of bits needed to transmit a joining pattern.
	 * This procedure is described in Tan & Dowe (2003): MML Inference of Decision Graphs with Multi-way Joins
	 * @return the number of bits needed to transmit the joining pattern.
	 */
	private double encodeJoinPattern(){
		
		// obtain the number of new nodes
		int N = joinList.getNewNodes().size();
		
		// obtain the number of old nodes
		int Q = joinList.getOldNodes().size();
				
		// obtain number of children of join nodes (equals size of join groups)
		int M = joinList.getJoinGroups().size();
		
		// number of pending nodes
		int P = joinList.getPendingNodes().size();
		
		// number of nodes in each joining group
		ArrayList<Integer> J = new ArrayList<Integer>();
		
		// number of new nodes in each group of joining leaf nodes
		ArrayList<Integer> X = new ArrayList<Integer>();	
		for(Set<TreeNode> nodes : joinList.getJoinGroups()){
			J.add(nodes.size());
			int x = 0;
			for(TreeNode v : nodes) if(joinList.getNewNodes().contains(v)) x += 1;
			X.add(x);
		}
				
		// number of new nodes among pending nodes
		int Y = 0;
		for(TreeNode newNode : joinList.getNewNodes()){
			if(joinList.getPendingNodes().contains(newNode)) Y += 1;
		}
		
		// calculate the length of the joining pattern message
		double ml1 = encodeJoinPattern1(N, Q);
		double ml2 = encodeJoinPattern2(N, Q, M);		
		double ml3 = encodeJoinPattern3(N, Q, Y, P, M, J, X);			
	
		return ml1 + ml2 + ml3;
	}
		
	// calculate the bits needed to transmit the number of nodes which are children of joins 
	private double encodeJoinPattern1(int N, int Q){				
		double invp = Math.min((double) N, ((double) N + (double) Q)/2 );
		return (Math.log(invp) / Math.log(2));
	}
	
	/*
	 * Calculate the bits needed to communicate the number of pending nodes, P, which are not involved in 
	 * any join and the numbers of nodes (J1, J2, . . . , JM ) in each group of joining leaf nodes 
	 * Applying the stars and bars theorem to find the number of different solutions 
	 * for P + J_1 + ... + J_M = N + Q where 
	 * - P >= 0 is the number of nodes not involved in any join 
	 * - M is the number of joining groups (i.e., number of children of join nodes)
	 * - J_1,...,J_M >= 2 are the number of joining nodes in each group
	 */
	private double encodeJoinPattern2(int N, int Q, int M){		
		// shift the right-hand side of the equation to account for J_i >= 2
		int rhs = N + Q - (2 * M);		
		
		// using stars and bars theorem to find number of possible solutions	
		int solutions = IntMath.binomial(rhs + M, M);
		
		// assuming that each solution is a priori equally likely 
		// the cost of transmitting is -log(1/solutions) = log(solutions)
		return ( Math.log((double) solutions) / Math.log(2) );
	}
	
	
	/*
	 * Calculate the bits needed to transmit the number of permutations of nodes.
	 * See Tan & Dowe (2003): MML Inference of Decision Graphs with Multi-way Joins
	 */
	private double encodeJoinPattern3(int N, int Q, int Y, int P, int M, ArrayList<Integer> J, 			
			ArrayList<Integer> X){	
		double enumerator = IntMath.factorial(N) * IntMath.factorial(Q);
		int denominator = IntMath.factorial(Y) * IntMath.factorial(P - Y); 
		for(int i = 0; i < M; i++){
			int Xi = X.get(i); int Ji = J.get(i);
			denominator *= IntMath.factorial(Xi);
			denominator *= IntMath.factorial(Ji - Xi);
		}
		return ( Math.log(enumerator / denominator) / Math.log(2) );
	}

}
		

