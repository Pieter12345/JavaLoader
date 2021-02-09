package io.github.pieter12345.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This is a directed graph implementation that stores children and parents in sets for every node object. It comes
 * with a guarenteed return-parent-before-child iterator (which does not support cycles for that reason).
 * @author P.J.S. Kools
 * @param <T>
 */
public class Graph<T> implements Iterable<T> {
	
	private final Map<T, Node<T>> nodeMap = new HashMap<T, Node<T>>();
	
	/**
	 * Creates an empty graph.
	 */
	public Graph() {
	}
	
	/**
	 * Creates a graph, containing the given nodes with no edges between them.
	 * @param nodeValues - The initial nodes.
	 */
	public Graph(Iterable<T> nodeValues) {
		
		// Add the node values to the node map.
		if(nodeValues != null) {
			for(T nodeVal : nodeValues) {
				this.nodeMap.put(nodeVal, new Node<T>(nodeVal));
			}
		}
	}
	
	/**
	 * Adds a node with the given value to the graph.
	 * Does nothing if a node with an equal value was already added to the graph.
	 * @param nodeVal - The value of the node to add.
	 * @return True if the node was added, false if a node with the given value already exists in the graph.
	 */
	public boolean addNode(T nodeVal) {
		if(!this.nodeMap.containsKey(nodeVal)) {
			this.nodeMap.put(nodeVal, new Node<T>(nodeVal));
			return true;
		}
		return false;
	}
	
	/**
	 * Removes the node with the given value from the graph. All edges from and to this node will be removed as well.
	 * @param nodeVal - The value of the node to remove.
	 * @return True if the node was removed, false if a node with the given value did not exist in the graph.
	 */
	public boolean removeNode(T nodeVal) {
		Node<T> node = this.nodeMap.get(nodeVal);
		if(node != null) {
			for(Node<T> child : node.getChildren()) {
				child.getParents().remove(node);
			}
			for(Node<T> parent : node.getParents()) {
				parent.getChildren().remove(node);
			}
			this.nodeMap.remove(nodeVal);
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the value of all nodes in the graph.
	 * @return The value of all nodes in the graph.
	 */
	public Set<T> getNodes() {
		return new HashSet<T>(this.nodeMap.keySet());
	}
	
	/**
	 * Returns the number of nodes in this graph.
	 * @return The number of nodes in this graph.
	 */
	public int size() {
		return this.nodeMap.size();
	}
	
	/**
	 * Adds a directed edge from the given node to the given node.
	 * @param from - The from node value.
	 * @param to - The to node value.
	 * @return True if the edge was added, false if at least one node did not exist or if the edge already exists.
	 */
	public boolean addDirectedEdge(T from, T to) {
		Node<T> fromNode = nodeMap.get(from);
		Node<T> toNode = nodeMap.get(to);
		if(fromNode != null && toNode != null && !this.hasDirectedEdge(from, to)) {
			fromNode.getChildren().add(toNode);
			toNode.getParents().add(fromNode);
			return true;
		}
		return false;
	}
	
	/**
	 * Removes a directed edge from the given node to the given node.
	 * @param from - The from node value.
	 * @param to - The to node value.
	 * @return True if the edge was removed, false if the edge did not exist.
	 */
	public boolean removeDirectedEdge(T from, T to) {
		Node<T> fromNode = nodeMap.get(from);
		Node<T> toNode = nodeMap.get(to);
		if(fromNode != null && toNode != null) {
			return fromNode.getChildren().remove(toNode) && toNode.getParents().remove(fromNode);
		}
		return false;
	}
	
	/**
	 * Returns whether a directed edge from the given node to the given node exists.
	 * @param from - The from node value.
	 * @param to - The to node value.
	 * @return True if the edge exists, false otherwise.
	 */
	public boolean hasDirectedEdge(T from, T to) {
		Node<T> fromNode = nodeMap.get(from);
		Node<T> toNode = nodeMap.get(to);
		return fromNode != null && toNode != null
				&& fromNode.getChildren().contains(toNode) && toNode.getParents().contains(fromNode);
	}
	
	@Override
	public ParentBeforeChildGraphIterator<T> iterator() {
		return new ParentBeforeChildGraphIterator<T>(this);
	}
	
	public ParentBeforeChildGraphIterator<T> parentBeforeChildIterator() {
		return new ParentBeforeChildGraphIterator<T>(this);
	}
	
	public ParentBeforeChildGraphIterator<T> parentBeforeChildIterator(T startNode) {
		Node<T> node = this.nodeMap.get(startNode);
		if(node == null) {
			throw new IllegalArgumentException("Root has to be part of the graph.");
		}
		return new ParentBeforeChildGraphIterator<T>(this, node);
	}
	
	public ChildBeforeParentGraphIterator<T> childBeforeParentIterator() {
		return new ChildBeforeParentGraphIterator<T>(this);
	}
	
	public ChildBeforeParentGraphIterator<T> childBeforeParentIterator(T startNode) {
		Node<T> node = this.nodeMap.get(startNode);
		if(node == null) {
			throw new IllegalArgumentException("Root has to be part of the graph.");
		}
		return new ChildBeforeParentGraphIterator<T>(this, node);
	}
	
	/**
	 * Returns a set of all strongly connected components. A strongly connected component is a connected component in
	 * which all nodes can reach eachother through their children. A single node with no connections is also considered
	 * a strongly connected component.
	 * @return A set of all strongly connected components.
	 */
	public Set<Set<T>> getStronglyConnectedComponents() {
		
		// Perform depth-first iteration, adding completely handled nodes to a result stack.
		Stack<Node<T>> resultStack = new Stack<Node<T>>();
		{
			// Create a stack and add all nodes to it.
			Stack<Node<T>> stack = new Stack<Node<T>>();
			stack.addAll(this.nodeMap.values());
			
			// Create a stack to detect when a node its children have been handled.
			Stack<Node<T>> handleStack = new Stack<Node<T>>();
			
			// Perform the depth-first iteration.
			Set<Node<T>> visited = new HashSet<Node<T>>();
			while(!stack.empty()) {
				
				// Get the next node.
				Node<T> node = stack.peek();
				
				// Push the node to the resultStack if all its children have been handled.
				if(!handleStack.empty() && handleStack.peek() == node) {
					handleStack.pop();
					stack.pop();
					resultStack.push(node);
					continue;
				}
				
				// Mark the next node as visited or skip it if it has been visited already.
				if(!visited.add(node)) {
					stack.pop();
					continue;
				}
				
				// Add the node to the handleStack to be able to push it to the resultStack once its children
				// have been handled.
				handleStack.push(node);
				
				// Add all children if they have not yet been visited.
				for(Node<T> child : node.getChildren()) {
					if(!visited.contains(child)) {
						stack.push(child);
					}
				}
			}
		}
		
		// Perform depth-first iteration on the transpose graph, in order of the above received stack, to
		// receive the strongly connected components.
		Set<Set<T>> sccsSet = new HashSet<Set<T>>();
		{
			// Perform the depth-first iteration, starting at every node in the resultStack.
			Set<Node<T>> visited = new HashSet<Node<T>>();
			while(!resultStack.empty()) {
				
				// Get the next start node.
				Node<T> startNode = resultStack.pop();
				
				// Skip the node if it has been visited already.
				if(visited.contains(startNode)) {
					continue;
				}
				
				// Create a set for all nodes values in this strongly connected component.
				Set<T> sccSet = new HashSet<T>();
				
				// Perform the depth-first iteration from the startNode, using the 'global' visited set.
				Stack<Node<T>> stack = new Stack<Node<T>>();
				stack.push(startNode);
				while(!stack.empty()) {
					
					// Get the next node.
					Node<T> node = stack.pop();
					
					// Mark the next node as visited or skip it if it has been visited already.
					if(!visited.add(node)) {
						continue;
					}
					
					// Add the node value to the strongly connected component set.
					sccSet.add(node.get());
					
					// Add all parents if they have not yet been visited.
					for(Node<T> parent : node.getParents()) {
						if(!visited.contains(parent)) {
							stack.push(parent);
						}
					}
				}
				
				// Add the result to the strongly connected components set.
				sccsSet.add(sccSet);
			}
		}
		
		// Return the strongly connected components.
		return sccsSet;
	}
	
	public Set<T> getAncestors(T forNode) {
		
		// Get the actual node object.
		Node<T> node = this.nodeMap.get(forNode);
		if(node == null) {
			throw new IllegalArgumentException("Node has to be part of the graph.");
		}
		
		// Get the ancestors using depth-first iteration.
		Set<Node<T>> ancestorNodes = new HashSet<Node<T>>();
		Stack<Node<T>> stack = new Stack<Node<T>>();
		stack.push(node);
		while(!stack.empty()) {
			node = stack.pop();
			ancestorNodes.add(node);
			for(Node<T> parent : node.getParents()) {
				if(!ancestorNodes.contains(parent)) {
					stack.push(parent);
				}
			}
		}
		
		// Return the nodes converted to values.
		Set<T> ancestors = new HashSet<T>();
		for(Node<T> ancestorNode : ancestorNodes) {
			ancestors.add(ancestorNode.get());
		}
		return ancestors;
	}
	
	public Set<T> getDescendents(T forNode) {
		
		// Get the actual node object.
		Node<T> node = this.nodeMap.get(forNode);
		if(node == null) {
			throw new IllegalArgumentException("Node has to be part of the graph.");
		}
		
		// Get the descendents using depth-first iteration.
		Set<Node<T>> descendentNodes = new HashSet<Node<T>>();
		Stack<Node<T>> stack = new Stack<Node<T>>();
		stack.push(node);
		while(!stack.empty()) {
			node = stack.pop();
			descendentNodes.add(node);
			for(Node<T> child : node.getChildren()) {
				if(!descendentNodes.contains(child)) {
					stack.push(child);
				}
			}
		}
		
		// Return the nodes converted to values.
		Set<T> descendents = new HashSet<T>();
		for(Node<T> descendentNode : descendentNodes) {
			descendents.add(descendentNode.get());
		}
		return descendents;
	}
	
	/**
	 * Represents a node in a graph. This node contains a parent and child set for direct references to parent
	 * and child nodes.
	 * @author P.J.S. Kools
	 * @param <T>
	 */
	private static class Node<T> {
		private final T value;
		private Set<Node<T>> parents = new HashSet<Node<T>>();
		private Set<Node<T>> children = new HashSet<Node<T>>();
		public Node(T value) {
			this.value = value;
		}
		public T get() {
			return this.value;
		}
		public Set<Node<T>> getParents() {
			return this.parents;
		}
		public Set<Node<T>> getChildren() {
			return this.children;
		}
	}
	
	/**
	 * An Iterator implementation that returns the elements in a graph in a 'breadth-first-like', but slightly
	 * different order:
	 * <ul>
	 * <li>All nodes without parents are added to the queue and are guarenteed to be returned first.</li>
	 * <li>When a node is returned, its children are added to the queue if and only if all their parents have been
	 *   handled.</li>
	 * </ul>
	 * This implementation guarentees that parent nodes are returned before their children, even when multiple
	 * root nodes exist.
	 * If the graph contains a cycle, all nodes within the cycle and their descendents are skipped without notice.
	 * @author P.J.S. Kools
	 * @param <T>
	 */
	public static class ParentBeforeChildGraphIterator<T> implements Iterator<T> {
		
		private final Graph<T> graph;
		private Queue<Node<T>> nodeQueue = new LinkedBlockingQueue<Node<T>>();
		private Set<Node<T>> visited = new HashSet<Node<T>>();
		private Node<T> last = null;
		
		public ParentBeforeChildGraphIterator(Graph<T> graph) {
			this.graph = graph;
			for(Node<T> node : this.graph.nodeMap.values()) {
				if(node.getParents().isEmpty()) {
					this.nodeQueue.offer(node);
				}
			}
		}
		
		/**
		 * Creates a new ParentBeforeChildGraphIterator that starts at the given rootNode.
		 * @param graph - The graph to (partially) iterate over.
		 * @param rootNode - The root node to start iteration at.
		 */
		private ParentBeforeChildGraphIterator(Graph<T> graph, Node<T> rootNode) {
			this.graph = graph;
			this.nodeQueue.offer(rootNode);
		}
		
		@Override
		public boolean hasNext() {
			return !this.nodeQueue.isEmpty();
		}
		
		@Override
		public T next() {
			
			// Get and remove the next node from the queue. Throw an exception if this is not available.
			Node<T> nextNode = this.nodeQueue.poll();
			if(nextNode == null) {
				throw new NoSuchElementException("Iterator has no more elements.");
			}
			
			// Mark the node as visited.
			this.visited.add(nextNode);
			
			// Add all children to the queue if they have not been handled and have no unhandled parents left.
			for(Node<T> child : nextNode.getChildren()) {
				if(!this.visited.contains(child)) {
					if(this.visited.containsAll(child.getParents())) {
						this.nodeQueue.offer(child);
					}
				}
			}
			
			// Store and return the next node.
			this.last = nextNode;
			return nextNode.get();
		}
		
		/**
		 * Removes all descendents of the last returned node from the graph. This is the last returned node and all its
		 * direct and indirect children. The values of the removed nodes will be returned in breath-first iteration
		 * order. If this method is called before the next() method has been called, null will be returned.
		 * @return The removed nodes in breadth-first iteration order or null if no nodes were removed.
		 */
		public List<T> removeDescendents() {
			
			// Return null if the 'next()' method hasn't been called yet.
			if(this.last == null) {
				return null;
			}
			
			// Remove the 'last' node from the 'visited' set since it will be removed.
			this.visited.remove(this.last);
			
			// Remove all children of 'last' from the queue if they were added, we won't have to visit them anymore.
			this.nodeQueue.removeAll(this.last.getChildren());
			
			// Perform breadth-first iteration, adding all removed node values to a list to return.
			Queue<Node<T>> queue = new LinkedBlockingQueue<Node<T>>();
			queue.offer(this.last);
			List<T> removed = new ArrayList<T>();
			while(!queue.isEmpty()) {
				
				// Get the next node from the queue.
				Node<T> node = queue.poll();
				
				// Add all its children to the queue.
				for(Node<T> child : node.getChildren()) {
					if(node != child) { // This is the only possible cycle because all visited nodes are removed.
						queue.offer(child);
					}
				}
				
				// Remove the node from the graph and add it to the removal list.
				this.graph.removeNode(node.get());
				removed.add(node.get());
			}
			
			// Return the removed values.
			return removed;
		}
		
	}
	
	/**
	 * An Iterator implementation that returns the elements in a graph in a reverse 'breadth-first-like', but slightly
	 * different order:
	 * <ul>
	 * <li>All nodes without children are added to the queue and are guarenteed to be returned first.</li>
	 * <li>When a node is returned, its parents are added to the queue if and only if all their children have been
	 *   handled.</li>
	 * </ul>
	 * This implementation guarentees that child nodes are returned before their parents, even when multiple root nodes
	 * exist.
	 * If the graph contains a cycle, all nodes within the cycle and their ancestors are skipped without notice.
	 * @author P.J.S. Kools
	 * @param <T>
	 */
	public static class ChildBeforeParentGraphIterator<T> implements Iterator<T> {
		
		private final Graph<T> graph;
		private Queue<Node<T>> nodeQueue = new LinkedBlockingQueue<Node<T>>();
		private Set<Node<T>> visited = new HashSet<Node<T>>();
		private Node<T> last = null;
		
		public ChildBeforeParentGraphIterator(Graph<T> graph) {
			this.graph = graph;
			for(Node<T> node : this.graph.nodeMap.values()) {
				if(node.getChildren().isEmpty()) {
					this.nodeQueue.offer(node);
				}
			}
		}
		
		/**
		 * Creates a new ChildBeforeParentGraphIterator that starts at the given rootNode.
		 * @param graph - The graph to (partially) iterate over.
		 * @param rootNode - The root node to start iteration at.
		 */
		private ChildBeforeParentGraphIterator(Graph<T> graph, Node<T> rootNode) {
			this.graph = graph;
			this.nodeQueue.offer(rootNode);
		}
		
		@Override
		public boolean hasNext() {
			return !this.nodeQueue.isEmpty();
		}
		
		@Override
		public T next() {
			
			// Get and remove the next node from the queue. Throw an exception if this is not available.
			Node<T> nextNode = this.nodeQueue.poll();
			if(nextNode == null) {
				throw new NoSuchElementException("Iterator has no more elements.");
			}
			
			// Mark the node as visited.
			this.visited.add(nextNode);
			
			// Add all parents to the queue if they have not been handled and have no unhandled children left.
			for(Node<T> parent : nextNode.getParents()) {
				if(!this.visited.contains(parent)) {
					if(this.visited.containsAll(parent.getChildren())) {
						this.nodeQueue.offer(parent);
					}
				}
			}
			
			// Store and return the next node.
			this.last = nextNode;
			return nextNode.get();
		}
		
		/**
		 * Removes all ancestors of the last returned node from the graph. This is the last returned node and all its
		 * direct and indirect parents. The values of the removed nodes will be returned in breath-first iteration
		 * order. If this method is called before the next() method has been called, null will be returned.
		 * @return The removed nodes in breadth-first iteration order or null if no nodes were removed.
		 */
		public List<T> removeAncestors() {
			
			// Return null if the 'next()' method hasn't been called yet.
			if(this.last == null) {
				return null;
			}
			
			// Remove the 'last' node from the 'visited' set since it will be removed.
			this.visited.remove(this.last);
			
			// Remove all parents of 'last' from the queue if they were added, we won't have to visit them anymore.
			this.nodeQueue.removeAll(this.last.getParents());
			
			// Perform breadth-first iteration, adding all removed node values to a list to return.
			Queue<Node<T>> queue = new LinkedBlockingQueue<Node<T>>();
			queue.offer(this.last);
			List<T> removed = new ArrayList<T>();
			while(!queue.isEmpty()) {
				
				// Get the next node from the queue.
				Node<T> node = queue.poll();
				
				// Add all its parents to the queue.
				for(Node<T> parent : node.getParents()) {
					if(node != parent) { // This is the only possible cycle because all visited nodes are removed.
						queue.offer(parent);
					}
				}
				
				// Remove the node from the graph and add it to the removal list.
				this.graph.removeNode(node.get());
				removed.add(node.get());
			}
			
			// Return the removed values.
			return removed;
		}
		
	}
}
