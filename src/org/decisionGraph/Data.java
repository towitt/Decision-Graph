package org.decisionGraph;

import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.node.BufferedDataTable;

import com.google.common.collect.Maps;

import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;

public class Data {	
	private ArrayList<DataRow> data; // the data saved as an array list of data rows
	private long nrow;  // the number of rows in the data (number of elements in the array list)
	private String classAttr; // the class variable in the data object
	private String[] colNames; // the names of the columns (attributes and class variable); does not change when taking subsets
	private Map<String, Set<DataCell>> colValues; // mapping of column names to set of values of the variable in the column
	private Map<String, Integer> colIndex; // mapping of column names to positions of columns in the data
	private Set<String> attributes; // names of the (remaining) attributes; changes when taking subsets
	private HashMap<String, String> colTypes; // types of the columns (numeric, string, ...)
	
	/**
	 * CONSTRUCTOR 1 (for class BufferedDataTable)
	 * @param indata - object of class BufferedDataTable
	 */
	public Data(BufferedDataTable indata, String classAttr){		
		this.nrow = indata.size(); 	
		this.colNames = indata.getDataTableSpec().getColumnNames(); 
		this.classAttr = classAttr;		
		this.setData(indata);		
		this.setColTypes(indata);
		this.setMaps(indata);
		this.setAttributesFromColnames();
	}
		
	/**
	 * CONSTRUCTOR 2 (for class Data)
	 * @param data - subset of data rows from the initial Data object
	 * @param initData - original data (object of class Data)
	 */
	public Data(ArrayList<DataRow> data, Data initData){		
		this.data = data;
		this.nrow = data.size();
		this.colIndex = initData.getColIndex();
		this.colValues = initData.getColValues();	
		this.colNames = initData.getColNames();	
		this.classAttr = initData.getClassAttribute();
		this.colTypes = initData.getColTypes();
		this.setAttributes(initData.getAttributes());
	}
	
	/**
	 * Sets the actual data by looping over the data rows of the BufferedDataTable.
	 * Used in the first constructor for the class. 
	 * @param indata - object of class BufferedDataTable
	 */
	private void setData(BufferedDataTable indata) {	
		this.data = new ArrayList<DataRow>();
		for(DataRow r : indata){
			this.data.add(r);		
		}		
	}
		
	/**
	 * Sets two different maps:
	 * (1) Map of column names to the possible column values
	 * (2) Map of column names to to the corresponding column index
	 * Used in the first constructor for the class.
	 * @param indata - object of class BufferedDataTable
	 */
	private void setMaps(BufferedDataTable indata){			
		this.colValues = new HashMap<String, Set<DataCell>>();
		this.colIndex = new HashMap<String, Integer>();			
		for(String s : this.colNames){			
			this.colIndex.put(s, indata.getDataTableSpec().findColumnIndex(s));					
			if(this.colTypes.get(s).equals("StringCell")){
				Set<DataCell> values = indata.getDataTableSpec().getColumnSpec(s).getDomain().getValues();
				//if(values == null) values = createColValues(s);
				this.colValues.put(s, values);
			}			
		}
	}			
	
	/**
	 * Method to partition the data according to the values of an nominal attribute. 
	 * @param condAttr - the attribute used to partition the data
	 * @return a map including the subsets of the data
	 */
	protected HashMap<DataCell, Data> partition(String condAttr) {							
			
		// get the index of the attribute used to partition the data into subsets
		int condAttrIndex = this.colIndex.get(condAttr);	
		
		// create the map and fill it with empty data objects
		HashMap<DataCell, Data> subsetMap = Maps.newHashMap();						
		for(DataCell condValue : this.getColValues(condAttr)){					
			
			// create the (empty) data object
			Data subset = new Data(new ArrayList<DataRow>(), this);					
			
			// remove the attribute that was used to partition the data
			subset.setAttributes(RemoveAttribute.rm(subset.getAttributes(), condAttr));
			
			subsetMap.put(condValue, subset);
		}
		
		// fill the data objects by adding all elements which have the specified value (condValue) 
		// on the attribute used to partition the data		
		for(DataRow r : this.data) subsetMap.get(r.getCell(condAttrIndex)).add(r);				
		
		return subsetMap;
	}
		
	/**
	 * Method to partition the data into two subsets using a numerical attribute and a cut-off value.
	 * @param condAttr - the attribute used to partition the data
	 * @param cutValue - the value of the numerical attribute at which the split is done
	 * @return the lower (<= cut) and the upper (> cut) subset
	 */
	protected Data[] partition(String condAttr, Double cutValue) {
		
		// get the index of the attribute used to partition the data into subsets
		int condAttrIndex = this.colIndex.get(condAttr);	
		
		// loop over data rows and add respective rows to the subset
		Data subsetUpp= new Data(new ArrayList<DataRow>(), this);
		Data subsetLow= new Data(new ArrayList<DataRow>(), this);		
		for(DataRow r : this.data){		
						
			// get the double value of the cell
			Double cellValue = ((DoubleValue)r.getCell(condAttrIndex)).getDoubleValue();		
			
			// take subset by adding all elements which have a greater/smaller value than the cutValue 
			if(cellValue.compareTo(cutValue) > 0) subsetUpp.add(r);				
			else subsetLow.add(r);									
		}		
				
		// Remark: do not remove attribute that was used to partition the data!
		
		return new Data[]{subsetLow, subsetUpp};
	}
	
	/**
	 * 'Join' two data objects by appending the data rows of another Data object 
	 * @param data 
	 * @return the 'joined' data (class Data)
	 */
	protected Data join(Data otherData){
		
		// create new ArrayList to save the data rows of both data objects
		ArrayList<DataRow> newData = new ArrayList<DataRow>();	
		newData.addAll(this.getData());
		newData.addAll(otherData.getData());
		
		// create new Data object
		Data joinedData = new Data(newData, this);
		
		// set the remaining attributes (intersection of both attribute sets)
		joinedData.attributes.retainAll(otherData.getAttributes());
		
		return joinedData;
	}
			
	/**
	 * Obtain the frequency table for an attribute
	 * @param attribute - the (nominal) attribute that should be tabled
	 * @return the frequency table for the attribute
	 */
	protected Map<DataCell, Integer> getFreqMap(String attribute){
		int col = this.getColIndex(attribute);
		Map<DataCell, Integer> counter = new HashMap<DataCell, Integer>();	
		for(DataRow r : this.getData()){	
			DataCell c = r.getCell(col);
			if(!counter.containsKey(c)){
				counter.put(c, 0);
			}
			counter.put(c, counter.get(c) + 1);				
		}
		return counter;
	}
	
	/**
	 * Obtain the vector of class values, i.e., the class column
	 * @return vector of class values 
	 */
	protected ArrayList<DataCell> getClassVec(){
		ArrayList<DataCell> classArray = new ArrayList<DataCell>();
		int col = this.getColIndex(this.classAttr);
		for(DataRow r: this.getData()){
			classArray.add(r.getCell(col));
		}
		return classArray;
	}
	
	/**
	 * Create a set of attributes using the column names
	 * (basically, the class variable is removed from the column names)	 
	 */
	private void setAttributesFromColnames(){		
		Set<String> attributes = new HashSet<String>(Arrays.asList(this.colNames)); 
		attributes.remove(this.getClassAttribute());	
		this.attributes = attributes;
	}
	
	/**
	 * Create a map of column types
	 * @param indata - object of class BufferedDataTable 
	 */
	private void setColTypes(BufferedDataTable indata){
		this.colTypes = new HashMap<String, String>();
		for(String s : this.colNames){
			String type = indata.getDataTableSpec().getColumnSpec(s).getType().getCellClass().getSimpleName();
			this.colTypes.put(s, type);
		}		
	}
	
	public void add(DataRow row){
		this.data.add(row);
		this.nrow += 1;
	}
	
	public HashMap<String, String> getColTypes(){
		return this.colTypes;
	}
	
	public ArrayList<DataRow> getData(){
		return this.data;
	}
		
	public long getNrow(){
		return this.nrow;
	}
	
	public void setClassAttribute(String attribute){
		this.classAttr = attribute;
	}
	
	public String getClassAttribute(){
		return this.classAttr;
	}
	
	public String[] getColNames(){
		return this.colNames;
	}
	
	public void setAttributes(Set<String> attributes){
		this.attributes = attributes;
	}
	
	public Set<String> getAttributes(){		
		return this.attributes;
	}
	
	public int getColIndex(String attr){
		return this.colIndex.get(attr);
	}
	
	public Map<String, Integer> getColIndex(){
		return this.colIndex;
	}
	
	public Set<DataCell> getColValues(String attr){
		return this.colValues.get(attr);
	}
	
	public Map<String, Set<DataCell>> getColValues(){
		return this.colValues;
	}
	
}
