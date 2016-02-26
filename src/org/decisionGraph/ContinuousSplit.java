package org.decisionGraph;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;

public class ContinuousSplit {
	
	private Data data;
	private String attribute;
	private MessageLength messageLength;
	private Multimap<Double, String> sortedMap;
	private int uniqueValues; 	
	
	public ContinuousSplit(Data data, String attribute, MessageLength messageLength){
		this.data = data;
		this.attribute = attribute;
		this.messageLength = messageLength;
		this.generateSortedMap();
	}
	
	/**
	 * Select the best cut value to split the data on the continuous attribute. 
	 * The quality of a split is evaluated using the minimum message length principle.
	 * @return - the cut value producing the split with the lowest message length.	
	 */
	protected Double selectBestCutValue() {
			
		// get all eligible split values, i.e., points where the class value changes
        TreeSet<Double> cutValues = getSortedCutValues(this.sortedMap);       
        
        // Take a random subset if there are too many possible cut values
        if(cutValues.size() > 500){        
        	List<Double> list = new LinkedList<Double>(cutValues);
        	Collections.shuffle(list);
        	cutValues = new TreeSet<Double>(list.subList(0, 500));
        }
        
        // initialize variables		
		double bestCutValue = cutValues.first();		
		double length = evalCutValue(cutValues.first());
		
		// search for the split that produces the smallest message length		
		for(Double cut : cutValues){			
			double ml = evalCutValue(cut);
			if(ml < length){
				length = ml;
				bestCutValue = cut;				
			}		
		}
		
		// return the cut value 
		return bestCutValue;
	}
	
	/**
	 * Generate a map in which the entries are sorted by key.
	 * The key is the value of the numerical attribute, the value is the class attribute.
	 */
	private void generateSortedMap(){		
		
		// get column index for attribute and class
		int attributeColIndex = this.data.getColIndex(this.attribute);
		int classColIndex = this.data.getColIndex(data.getClassAttribute());
				
		// loop over the data and put (attribute, class) pairs in a tree multimap to
		// sort the entries by the value of the numerical attribute	
		this.sortedMap = TreeMultimap.create();
		for(DataRow r : this.data.getData()){						
			double value = ((DoubleValue)r.getCell(attributeColIndex)).getDoubleValue();			
			String classValue = r.getCell(classColIndex).toString();
			this.sortedMap.put(value, classValue);					
		}		
		
		// get the number of unique values of the attribute
		this.uniqueValues = this.sortedMap.asMap().size();				
	}	
	
	/**
	 * Identify all eligible cut values, i.e., those points where the class variable changes
	 * @param sortedMap - a map in which the entries are sorted by key
	 * @return set with all eligible cut values
	 */
	private TreeSet<Double> getSortedCutValues(Multimap<Double, String> sortedMap){	
		
		// generate sorted set to save the split points		
		TreeSet<Double> cutValues = Sets.newTreeSet();
		
		// get the first entry of the map
		Entry<Double, String> prevEntry = sortedMap.entries().iterator().next();
		
		// loop over map entries to find possible cut points
		for(Entry<Double, String> entry : sortedMap.entries()){			
			if(!(entry.getValue().equals(prevEntry.getValue()))){
				double cv = (entry.getKey() + prevEntry.getKey()) / 2;
				cutValues.add(cv);
				prevEntry = entry;
			}
		}		
		return cutValues;		
	}
	
	/**
	 * Calculate the number of bits needed to encode the category messages for the two subsets
	 * resulting from a split at the cut value. 
	 * @param cutValue - the value used to split the data
	 * @return the overall message length for the resulting subsets
	 */
	private double evalCutValue(Double cutValue) {	
		Data[] subsets = this.data.partition(this.attribute, cutValue);
		double ml = messageLength.encodeSubset(subsets[0]) + messageLength.encodeSubset(subsets[1]);		
		return ml;
	}
	
	public int getUniqueValues(){
		return this.uniqueValues;		
	}
	
	public Multimap<Double, String> getSortedMap(){
		return this.sortedMap;
	}
	
}
