package org.decisionGraph;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "DecisionGraph" Node.
 * 
 *
 * @author Tobias Witt
 */
public class DecisionGraphNodeFactory 
        extends NodeFactory<DecisionGraphNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public DecisionGraphNodeModel createNodeModel() {
        return new DecisionGraphNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<DecisionGraphNodeModel> createNodeView(final int viewIndex,
            final DecisionGraphNodeModel nodeModel) {
        return new DecisionGraphNodeView(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new DecisionGraphNodeDialog();
    }

}

