package org.decisionGraph;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

public class DecisionGraphNodeModel extends NodeModel {
    
	// SETTINGS 	
	// class attribute
	static final String CFGKEY_CLASS = "Select class attribute";
	public static final SettingsModelString m_class = new SettingsModelString(CFGKEY_CLASS, "");
	
	// alpha parameter of the Beta prior distribution over unknown class probabilities
	static final String CFGKEY_ALPHA = "Parameter of Beta prior";
	static final double DEFAULT_ALPHA = 0.5;
	public static final SettingsModelDoubleBounded m_alpha = 
			new SettingsModelDoubleBounded(CFGKEY_ALPHA, DEFAULT_ALPHA, 0, 1);
	
	// allow joins (Decision Graph or Decision Tree)
	static final String CFGKEY_JOINS = "Allow joins (Decision Graph)";
	static final boolean DEFAULT_JOINS = true;
	public static final SettingsModelBoolean m_joins = 
			new SettingsModelBoolean(CFGKEY_JOINS, DEFAULT_JOINS);
	
	// restrict the number of nodes which form a join?
	static final String CFGKEY_RESTRICTJOINNODES = "Restrict max. number of nodes in a join";
	static final boolean DEFAULT_RESTRICTJOINNODES = false;
	public static final SettingsModelBoolean m_restrictjoinnodes = 
    		new SettingsModelBoolean(CFGKEY_RESTRICTJOINNODES, DEFAULT_RESTRICTJOINNODES);
	
	// if number of nodes in join is restricted, specify maximum number
	static final String CFGKEY_MAXJOINNODES = "Maximum Number of Nodes in Join";
	static final int DEFAULT_MAXJOINNODES = 20;
	public static final SettingsModelIntegerBounded m_maxjoinnodes = 
    		new SettingsModelIntegerBounded(CFGKEY_MAXJOINNODES, DEFAULT_MAXJOINNODES, 2, Integer.MAX_VALUE);
     
    /**
     * Constructor for the node model with two input ports
     * (1) training data
     * (2) test data for class prediction
     */
    protected DecisionGraphNodeModel() {
        super(2, 1);
    }
  
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
  
        // get data and set class attribute
        Data trainingData = new Data(inData[0], m_class.getStringValue());        
              
        // check if domain information for the class variable is available
        Set<DataCell> classValues = trainingData.getColValues(trainingData.getClassAttribute());
        if (classValues == null) { throw new InvalidSettingsException(
           "The class attribute has too many distinct values.");
        }    
        
        // obtain the number of distinct class values
        double C = (double) trainingData.getColValues(trainingData.getClassAttribute()).size();
               
        // create the Message Length object
        MessageLength ml = new MessageLength(m_alpha.getDoubleValue(), C);
               
        // create the graph object     
        if(!m_restrictjoinnodes.getBooleanValue()) m_maxjoinnodes.setIntValue(Integer.MAX_VALUE);     
		DecisionGraph graph = new DecisionGraph(trainingData, ml, m_joins.getBooleanValue(),
				m_maxjoinnodes.getIntValue());				
		
		// learn the decision graph
		graph.learnGraph();	
		
		// make prediction for test data set
		BufferedDataTable testData = inData[1];
        CellFactory cellFactory = 
        		new DecisionGraphCellFactory(createOutputColumnSpec(testData.getDataTableSpec()), graph);
        
        // create the column re-arranger
        ColumnRearranger outputTable = new ColumnRearranger(testData.getDataTableSpec());
        
        // append the new column
        outputTable.append(cellFactory);     
         
        // create the actual output table
        BufferedDataTable bufferedOutput = exec.createColumnRearrangeTable(testData, outputTable, exec);
                
        return new BufferedDataTable[]{bufferedOutput};	        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO Code executed on reset.
        // Models build during execute are cleared here.
        // Also data handled in load/saveInternals will be erased here.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
    	
    	 // check spec with selected column
    	DataColumnSpec columnSpec = inSpecs[0].getColumnSpec(m_class.getStringValue());
    	if (columnSpec == null || !columnSpec.getType().isCompatible(NominalValue.class)) {
    		// if no useful column is selected guess one
    		// get the first useful one starting at the end of the table
    		for (int i = inSpecs[0].getNumColumns() - 1; i >= 0; i--) {
    			
    			if (inSpecs[0].getColumnSpec(i).getType().isCompatible(NominalValue.class)) {
    				m_class.setStringValue(inSpecs[0].getColumnSpec(i).getName());
    				break;
    			}
    			throw new InvalidSettingsException("Table contains no nominal" + " attribute for classification.");
        }
    }
    	
    	DataTableSpec inputSpec = inSpecs[0];
    	
    	// check if input data includes class column
    	if(inputSpec.containsName(m_class.getStringValue())){
    		
    		// create output table specification
    		DataTableSpec outputSpec = createOutputTableSpec(inputSpec);    	
    		return new DataTableSpec[]{outputSpec};
    	}
    		    	
    	// throw exception of input data does not contain the specified class column
    	else{
    		throw new InvalidSettingsException("Class column " + m_class.getStringValue() + " not found");    		
    	}	    	    	
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_class.saveSettingsTo(settings);
        m_alpha.saveSettingsTo(settings);
        m_joins.saveSettingsTo(settings); 
        m_restrictjoinnodes.saveSettingsTo(settings);      
        m_maxjoinnodes.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {                                  
        m_class.loadSettingsFrom(settings);
        m_alpha.loadSettingsFrom(settings);
        m_joins.loadSettingsFrom(settings);
        m_restrictjoinnodes.loadSettingsFrom(settings);      
        m_maxjoinnodes.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_class.validateSettings(settings);
        m_alpha.validateSettings(settings);
        m_joins.validateSettings(settings);
        m_restrictjoinnodes.validateSettings(settings);      
        m_maxjoinnodes.validateSettings(settings);
        
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        
        // TODO load internal data. 
        // Everything handed to output ports is loaded automatically (data
        // returned by the execute method, models loaded in loadModelContent,
        // and user settings set through loadSettingsFrom - is all taken care 
        // of). Load here only the other internals that need to be restored
        // (e.g. data used by the views).

    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
       
        // TODO save internal models. 
        // Everything written to output ports is saved automatically (data
        // returned by the execute method, models saved in the saveModelContent,
        // and user settings saved through saveSettingsTo - is all taken care 
        // of). Save here only the other internals that need to be preserved
        // (e.g. data used by the views).

    }
    
    private DataColumnSpec createOutputColumnSpec(DataTableSpec inSpec) {
    	    	
    	// creator for the predicted class column
    	DataColumnSpecCreator colSpecCreator = new DataColumnSpecCreator("Prediction (" +  
    			m_class.getStringValue() + ")", StringCell.TYPE);
    	 
    	// prediction column will get same domain as class column
    	colSpecCreator.setDomain(inSpec.getColumnSpec(m_class.getStringValue()).getDomain());
    	
    	// create the specification for the new column
    	DataColumnSpec newColumnSpec = colSpecCreator.createSpec();
    	    	 
    	return newColumnSpec;
    }
    
    private DataTableSpec createOutputTableSpec(DataTableSpec inSpec) {
    	    	   	
    	// create the specification for the new column
    	DataColumnSpec newColumnSpec = createOutputColumnSpec(inSpec);
    	DataTableSpec newSpec = new DataTableSpec(newColumnSpec);
    	
    	// create the specification for the whole output data
    	DataTableSpec outputSpec = new DataTableSpec(inSpec, newSpec);    	 
    	return outputSpec;
    }
    
}

