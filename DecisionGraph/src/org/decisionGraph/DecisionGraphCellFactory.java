package org.decisionGraph;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;

public class DecisionGraphCellFactory extends SingleCellFactory{
	
	private DecisionGraph tree;

	public DecisionGraphCellFactory(DataColumnSpec newColSpec, DecisionGraph tree) {
		super(newColSpec);
		this.tree = tree;
	}

	@Override
	public DataCell getCell(DataRow row) {	
		return new StringCell(tree.classify(row));
	}

}
