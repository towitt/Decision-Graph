# Decision Graph Node

Decision graphs ([Oliver, Dowe and Wallace, 1992](http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.52.1476&rep=rep1&type=pdf)) are directed acyclic graphs used for classification. In contrast to decision trees, the graph representation permits two ore more nodes to have a common child, thereby allowing for a more efficient representation of disjunctive concepts (less fragmentation and no need to duplicate subtrees). Decision graphs can be inferred from a set of training examples using the *minimum message length principle*.

This repository contains the implementation of a decision graph classifier for the data analytics platform KNIME. Further information are provided in this [paper](DecisionGraphs.pdf).
