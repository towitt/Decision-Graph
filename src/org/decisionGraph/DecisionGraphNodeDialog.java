package org.decisionGraph;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.knime.core.data.NominalValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;

public class DecisionGraphNodeDialog extends DefaultNodeSettingsPane {

    @SuppressWarnings({ "unchecked"})
	protected DecisionGraphNodeDialog() {
    super();
        
    	// GENERAL SETTINGS
    	createNewGroup("General");
    	
    	// class attribute
    	addDialogComponent(new DialogComponentColumnNameSelection(
    			DecisionGraphNodeModel.m_class, 
    			DecisionGraphNodeModel.CFGKEY_CLASS,
    			0, true, NominalValue.class));
        
    	// alpha parameter of the Beta prior distribution over unknown class probabilities
        addDialogComponent(new DialogComponentNumberEdit(
                DecisionGraphNodeModel.m_alpha, 
                DecisionGraphNodeModel.CFGKEY_ALPHA));
        
        // allow joins (Decision Graph or Decision Tree)
        addDialogComponent(new DialogComponentBoolean(
        		DecisionGraphNodeModel.m_joins, 
        		DecisionGraphNodeModel.CFGKEY_JOINS));
        
        // JOIN SETTINGS
        createNewGroup("Join Settings");
        
        // always prefer joins over splits?
        addDialogComponent(new DialogComponentBoolean(
        		DecisionGraphNodeModel.m_prefjoins, 
        		DecisionGraphNodeModel.CFGKEY_PREFJOINS));
        
        // restrict the number of nodes which form a join?
        addDialogComponent(new DialogComponentBoolean(
        		DecisionGraphNodeModel.m_restrictjoinnodes, 
        		DecisionGraphNodeModel.CFGKEY_RESTRICTJOINNODES));
        
        // if number of nodes in join is restricted, specify maximum number
        addDialogComponent(new DialogComponentNumberEdit(
                DecisionGraphNodeModel.m_maxjoinnodes, ""));                      
        
        // add change listeners so that join settings are only accessible if joins are allowed
       DecisionGraphNodeModel.m_joins.addChangeListener(
        		new ChangeListener() {
        			@Override
        			public void stateChanged(ChangeEvent arg0) {        			
        				DecisionGraphNodeModel.m_prefjoins.
        				setEnabled(DecisionGraphNodeModel.m_joins.getBooleanValue());		
        			}
        		}
        );
        
        DecisionGraphNodeModel.m_joins.addChangeListener(
        		new ChangeListener() {
        			@Override
        			public void stateChanged(ChangeEvent arg0) {        			
        				DecisionGraphNodeModel.m_restrictjoinnodes.
        				setEnabled(DecisionGraphNodeModel.m_joins.getBooleanValue());		
        			}
        		}
        );
        
        DecisionGraphNodeModel.m_joins.addChangeListener(
        		new ChangeListener() {
        			@Override
        			public void stateChanged(ChangeEvent arg0) {        			
        				DecisionGraphNodeModel.m_maxjoinnodes.
        				setEnabled(DecisionGraphNodeModel.m_joins.getBooleanValue() &&
        						   DecisionGraphNodeModel.m_restrictjoinnodes.getBooleanValue());		
        			}
        		}
        );          
        
        DecisionGraphNodeModel.m_restrictjoinnodes.addChangeListener(
        		new ChangeListener() {
        			@Override
        			public void stateChanged(ChangeEvent arg0) {        		
        				DecisionGraphNodeModel.m_maxjoinnodes.
        				setEnabled(DecisionGraphNodeModel.m_restrictjoinnodes.getBooleanValue());		
        			}
        		}
        );  
    }

}

