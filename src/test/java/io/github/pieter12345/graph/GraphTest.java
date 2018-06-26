package io.github.pieter12345.graph;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.pieter12345.graph.Graph.ChildBeforeParentGraphIterator;
import io.github.pieter12345.graph.Graph.ParentBeforeChildGraphIterator;

/**
 * Tests the {@link Graph} class.
 * @author P.J.S. Kools
 */
class GraphTest {
	
	@Test
	void testNullConstructor() {
		Graph<Integer> graph = new Graph<Integer>(null);
		assertThat(graph.size()).isEqualTo(0);
		assertThat(graph.getNodes()).isEmpty();
	}
	
	/**
	 * Tests that adding a node actually adds the node.
	 */
	@Test
	void testAddNode() {
		Graph<Integer> graph = new Graph<Integer>();
		assertThat(graph.getNodes()).isEmpty();
		graph.addNode(5);
		assertThat(graph.getNodes()).containsExactlyInAnyOrder(5);
		assertThat(graph.size()).isEqualTo(1);
		graph.addNode(7);
		assertThat(graph.getNodes()).containsExactlyInAnyOrder(5, 7);
		assertThat(graph.size()).isEqualTo(2);
	}
	
	/**
	 * Tests that adding a node that is already in the graph will be ignored.
	 */
	@Test
	void testAddNodeTwice() {
		Graph<Integer> graph = new Graph<Integer>();
		assertThat(graph.getNodes()).isEmpty();
		graph.addNode(5);
		assertThat(graph.getNodes()).containsExactlyInAnyOrder(5);
		graph.addNode(5);
		assertThat(graph.getNodes()).containsExactlyInAnyOrder(5);
	}
	
	/**
	 * Tests that removing a node actually removes the node.
	 */
	@Test
	void testRemoveNode() {
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4));
		assertThat(graph.getNodes()).containsExactlyInAnyOrder(1, 2, 3, 4);
		graph.removeNode(3);
		assertThat(graph.getNodes()).containsExactlyInAnyOrder(1, 2, 4);
		graph.removeNode(4);
		assertThat(graph.getNodes()).containsExactlyInAnyOrder(1, 2);
	}
	
	/**
	 * Tests that removing a node that was already removed will be ignored.
	 */
	@Test
	void testRemoveNodeTwice() {
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3));
		assertThat(graph.getNodes()).containsExactlyInAnyOrder(1, 2, 3);
		graph.removeNode(3);
		assertThat(graph.getNodes()).containsExactlyInAnyOrder(1, 2);
		graph.removeNode(3);
		assertThat(graph.getNodes()).containsExactlyInAnyOrder(1, 2);
	}
	
	/**
	 * Tests that adding a directed edge will actually add the edge.
	 */
	@Test
	void testAddDirectedEdge() {
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2));
		assertThat(graph.hasDirectedEdge(1, 2)).isFalse();
		assertThat(graph.hasDirectedEdge(2, 1)).isFalse();
		assertThat(graph.addDirectedEdge(1, 2)).isTrue(); // Add edge.
		assertThat(graph.hasDirectedEdge(1, 2)).isTrue();
		assertThat(graph.hasDirectedEdge(2, 1)).isFalse();
	}
	
	/**
	 * Tests that adding a directed edge that already exists will be ignored.
	 */
	@Test
	void testAddDirectedEdgeTwice() {
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2));
		assertThat(graph.hasDirectedEdge(1, 2)).isFalse();
		assertThat(graph.hasDirectedEdge(2, 1)).isFalse();
		assertThat(graph.addDirectedEdge(1, 2)).isTrue(); // Add edge.
		assertThat(graph.hasDirectedEdge(1, 2)).isTrue();
		assertThat(graph.hasDirectedEdge(2, 1)).isFalse();
		assertThat(graph.addDirectedEdge(1, 2)).isFalse(); // Add edge again, should be ignored.
		assertThat(graph.hasDirectedEdge(1, 2)).isTrue();
		assertThat(graph.hasDirectedEdge(2, 1)).isFalse();
	}
	
	/**
	 * Tests that adding a directed edge from/to an unexisting node will be ignored.
	 */
	@ParameterizedTest
	@CsvSource(value = {
			"1, 5",
			"5, 1",
			"5, 6"
		})
	void testAddDirectedEdgeUnexistingNode(int from, int to) {
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2));
		assertThat(graph.hasDirectedEdge(1, 2)).isFalse();
		assertThat(graph.hasDirectedEdge(2, 1)).isFalse();
		assertThat(graph.addDirectedEdge(from, to)).isFalse(); // Add edge.
		assertThat(graph.hasDirectedEdge(1, 2)).isFalse();
		assertThat(graph.hasDirectedEdge(2, 1)).isFalse();
	}
	
	/**
	 * Tests that adding a directed edge from/to an unexisting node will be ignored.
	 */
	@ParameterizedTest
	@CsvSource(value = {
			"1, 5",
			"5, 1",
			"5, 6"
		})
	void testHasDirectedEdgeUnexistingNode(int from, int to) {
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2));
		
		// Add all possible directed edges since it makes it more likely that a mistake in checking edge
		// existence returns true.
		graph.addDirectedEdge(1, 1);
		graph.addDirectedEdge(1, 2);
		graph.addDirectedEdge(2, 1);
		graph.addDirectedEdge(2, 2);
		
		// Assert that the graph has no edge from/to an unexisting node.
		assertThat(graph.hasDirectedEdge(from, to)).isFalse();
	}
	
	/**
	 * Tests that removing a directed edge will actually remove the edge.
	 */
	@Test
	void testRemoveDirectedEdge() {
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2));
		
		// Add all possible directed edges since it makes it more likely that a mistake in edge removal will remove
		// additional/other edges.
		graph.addDirectedEdge(1, 1);
		graph.addDirectedEdge(1, 2);
		graph.addDirectedEdge(2, 1);
		graph.addDirectedEdge(2, 2);
		
		// Assert that all edges exist.
		assertThat(graph.hasDirectedEdge(1, 1)).isTrue();
		assertThat(graph.hasDirectedEdge(1, 2)).isTrue();
		assertThat(graph.hasDirectedEdge(2, 1)).isTrue();
		assertThat(graph.hasDirectedEdge(2, 2)).isTrue();
		
		// Perform the removal.
		assertThat(graph.removeDirectedEdge(1, 2)).isTrue();
		
		// Assert that edge 1 -> 2 was removed and that the others still exist.
		assertThat(graph.hasDirectedEdge(1, 1)).isTrue();
		assertThat(graph.hasDirectedEdge(1, 2)).isFalse();
		assertThat(graph.hasDirectedEdge(2, 1)).isTrue();
		assertThat(graph.hasDirectedEdge(2, 2)).isTrue();
	}
	
	/**
	 * Tests that removing a directed edge which was already removed will be ignored.
	 */
	@Test
	void testRemoveDirectedEdgeTwice() {
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2));
		
		// Add all possible directed edges since it makes it more likely that a mistake in edge removal will remove
		// additional/other edges.
		graph.addDirectedEdge(1, 1);
		graph.addDirectedEdge(1, 2);
		graph.addDirectedEdge(2, 1);
		graph.addDirectedEdge(2, 2);
		
		// Assert that all edges exist.
		assertThat(graph.hasDirectedEdge(1, 1)).isTrue();
		assertThat(graph.hasDirectedEdge(1, 2)).isTrue();
		assertThat(graph.hasDirectedEdge(2, 1)).isTrue();
		assertThat(graph.hasDirectedEdge(2, 2)).isTrue();
		
		// Perform the removal.
		assertThat(graph.removeDirectedEdge(1, 2)).isTrue();
		
		// Assert that edge 1 -> 2 was removed and that the others still exist.
		assertThat(graph.hasDirectedEdge(1, 1)).isTrue();
		assertThat(graph.hasDirectedEdge(1, 2)).isFalse();
		assertThat(graph.hasDirectedEdge(2, 1)).isTrue();
		assertThat(graph.hasDirectedEdge(2, 2)).isTrue();
		
		// Perform the second removal, which should be ignored.
		assertThat(graph.removeDirectedEdge(1, 2)).isFalse();
		
		// Assert that the edges remain unchanged.
		assertThat(graph.hasDirectedEdge(1, 1)).isTrue();
		assertThat(graph.hasDirectedEdge(1, 2)).isFalse();
		assertThat(graph.hasDirectedEdge(2, 1)).isTrue();
		assertThat(graph.hasDirectedEdge(2, 2)).isTrue();
	}
	
	/**
	 * Tests that removing a directed edge from/to an unexisting node will be ignored.
	 */
	@ParameterizedTest
	@CsvSource(value = {
			"1, 5",
			"5, 1",
			"5, 6"
		})
	void testRemoveDirectedEdgeUnexistingNode(int from, int to) {
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2));
		
		// Add all possible directed edges since it makes it more likely that a mistake in edge removal will remove
		// additional/other edges.
		graph.addDirectedEdge(1, 1);
		graph.addDirectedEdge(1, 2);
		graph.addDirectedEdge(2, 1);
		graph.addDirectedEdge(2, 2);
		
		// Assert that all edges exist.
		assertThat(graph.hasDirectedEdge(1, 1)).isTrue();
		assertThat(graph.hasDirectedEdge(1, 2)).isTrue();
		assertThat(graph.hasDirectedEdge(2, 1)).isTrue();
		assertThat(graph.hasDirectedEdge(2, 2)).isTrue();
		
		// Perform the removal.
		assertThat(graph.removeDirectedEdge(from, to)).isFalse();
		
		// Assert that all edges still exist.
		assertThat(graph.hasDirectedEdge(1, 1)).isTrue();
		assertThat(graph.hasDirectedEdge(1, 2)).isTrue();
		assertThat(graph.hasDirectedEdge(2, 1)).isTrue();
		assertThat(graph.hasDirectedEdge(2, 2)).isTrue();
	}
	
	/**
	 * Tests the parent-before-child iteration order over graph: 1 -> 2 -> 3 -> 4.
	 */
	@Test
	void testPBCIterationOrder() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4));
		graph.addDirectedEdge(1, 2);
		graph.addDirectedEdge(2, 3);
		graph.addDirectedEdge(3, 4);
		ParentBeforeChildGraphIterator<Integer> it = graph.parentBeforeChildIterator();
		
		// Get the iteration result.
		List<Integer> result = new ArrayList<Integer>();
		while(it.hasNext()) {
			result.add(it.next());
		}
		
		// Assert the iteration result with the expected answer.
		assertThat(result.toArray(new Integer[0])).isEqualTo(new Integer[] {1, 2, 3, 4});
	}
	
	/**
	 * Tests the parent-before-child iteration order over graph:
	 * <pre>
	 * 1 -> 2 -> 3 -> 4
	 *      |
	 *       --> 5 -> 6
	 * </pre>
	 */
	@Test
	void testPBCIterationOrder2() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6));
		graph.addDirectedEdge(1, 2);
		graph.addDirectedEdge(2, 3);
		graph.addDirectedEdge(3, 4);
		graph.addDirectedEdge(2, 5);
		graph.addDirectedEdge(5, 6);
		ParentBeforeChildGraphIterator<Integer> it = graph.parentBeforeChildIterator();
		
		// Get the iteration result.
		List<Integer> result = new ArrayList<Integer>();
		while(it.hasNext()) {
			result.add(it.next());
		}
		
		// Assert the iteration result with the expected answer.
		assertThat(result.toArray(new Integer[0])).isIn(
				new Integer[] {1, 2, 3, 5, 4, 6},
				new Integer[] {1, 2, 5, 3, 6, 4});
	}
	
	/**
	 * Tests the parent-before-child iteration order over graph:
	 * <pre>
	 * 1 -> 2 -> 3 -> 4
	 *      |    |
	 *      |    v
	 *       --> 5 -> 6
	 * </pre>
	 */
	@Test
	void testPBCIterationOrder3() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6));
		graph.addDirectedEdge(1, 2);
		graph.addDirectedEdge(2, 3);
		graph.addDirectedEdge(3, 4);
		graph.addDirectedEdge(3, 5);
		graph.addDirectedEdge(2, 5);
		graph.addDirectedEdge(5, 6);
		ParentBeforeChildGraphIterator<Integer> it = graph.parentBeforeChildIterator();
		
		// Get the iteration result.
		List<Integer> result = new ArrayList<Integer>();
		while(it.hasNext()) {
			result.add(it.next());
		}
		
		// Assert the iteration result with the expected answer.
		assertThat(result.toArray(new Integer[0])).isIn(
				new Integer[] {1, 2, 3, 4, 5, 6},
				new Integer[] {1, 2, 3, 5, 4, 6});
	}
	
	/**
	 * Tests the parent-before-child iteration order over graph:
	 * <pre>
	 * 1 -> 2 -> 3 -> 4
	 *      |    ^
	 *      |    |
	 *       --> 5 -> 6
	 * </pre>
	 */
	@Test
	void testPBCIterationOrder4() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6));
		graph.addDirectedEdge(1, 2);
		graph.addDirectedEdge(2, 3);
		graph.addDirectedEdge(3, 4);
		graph.addDirectedEdge(5, 3);
		graph.addDirectedEdge(2, 5);
		graph.addDirectedEdge(5, 6);
		ParentBeforeChildGraphIterator<Integer> it = graph.parentBeforeChildIterator();
		
		// Get the iteration result.
		List<Integer> result = new ArrayList<Integer>();
		while(it.hasNext()) {
			result.add(it.next());
		}
		
		// Assert the iteration result with the expected answer.
		assertThat(result.toArray(new Integer[0])).isIn(
				new Integer[] {1, 2, 5, 3, 6, 4},
				new Integer[] {1, 2, 5, 6, 3, 4});
	}
	
	/**
	 * Tests the parent-before-child iteration order over graph:
	 * <pre>
	 *      5    6
	 *      v    v
	 * 1 -> 2 -> 3 -> 4
	 * </pre>
	 */
	@Test
	void testPBCIterationOrder5() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6));
		graph.addDirectedEdge(1, 2);
		graph.addDirectedEdge(2, 3);
		graph.addDirectedEdge(3, 4);
		graph.addDirectedEdge(5, 2);
		graph.addDirectedEdge(6, 4);
		ParentBeforeChildGraphIterator<Integer> it = graph.parentBeforeChildIterator();
		
		// Get the iteration result.
		List<Integer> result = new ArrayList<Integer>();
		while(it.hasNext()) {
			result.add(it.next());
		}
		
		// Assert the iteration result with the expected answer.
		assertThat(result.toArray(new Integer[0])).isIn(
				new Integer[] {1, 5, 6, 2, 3, 4},
				new Integer[] {1, 6, 5, 2, 3, 4},
				new Integer[] {5, 1, 6, 2, 3, 4},
				new Integer[] {5, 6, 1, 2, 3, 4},
				new Integer[] {6, 1, 5, 2, 3, 4},
				new Integer[] {6, 5, 1, 2, 3, 4});
	}
	
	/**
	 * Tests the parent-before-child iteration order over graph:
	 * <pre>
	 * 1 -> 2 -> 3
	 * |    ^    |
	 * v    |    v
	 * 4 -> 5 -> 6
	 * </pre>
	 */
	@Test
	void testPBCIterationOrder6() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6));
		graph.addDirectedEdge(1, 2);
		graph.addDirectedEdge(1, 4);
		graph.addDirectedEdge(2, 3);
		graph.addDirectedEdge(3, 6);
		graph.addDirectedEdge(4, 5);
		graph.addDirectedEdge(5, 2);
		graph.addDirectedEdge(5, 6);
		ParentBeforeChildGraphIterator<Integer> it = graph.parentBeforeChildIterator();
		
		// Get the iteration result.
		List<Integer> result = new ArrayList<Integer>();
		while(it.hasNext()) {
			result.add(it.next());
		}
		
		// Assert the iteration result with the expected answer.
		assertThat(result.toArray(new Integer[0])).isEqualTo(new Integer[] {1, 4, 5, 2, 3, 6});
	}
	
	/**
	 * Tests the parent-before-child iteration order over graph:
	 * <pre>
	 * 1 -> 2 -> 5
	 * ^    |
	 * |    v
	 * 4 <- 3
	 * </pre>
	 */
	@Test
	void testPBCIterationOrderCycle() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5));
		graph.addDirectedEdge(1, 2);
		graph.addDirectedEdge(2, 3);
		graph.addDirectedEdge(2, 5);
		graph.addDirectedEdge(3, 4);
		graph.addDirectedEdge(4, 1);
		ParentBeforeChildGraphIterator<Integer> it = graph.parentBeforeChildIterator();
		
		// Get the iteration result.
		List<Integer> result = new ArrayList<Integer>();
		while(it.hasNext()) {
			result.add(it.next());
		}
		
		// Assert the iteration result with the expected answer.
		assertThat(result).isEmpty();
	}
	
	/**
	 * Tests the parent-before-child iteration order over graph:
	 * <pre>
	 * 1 -> 2 -> 3 -> 6
	 *      ^    |
	 *      |    v
	 *      5 <- 4
	 * </pre>
	 */
	@Test
	void testPBCIterationOrderCycle2() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6));
		graph.addDirectedEdge(1, 2);
		graph.addDirectedEdge(2, 3);
		graph.addDirectedEdge(3, 4);
		graph.addDirectedEdge(3, 6);
		graph.addDirectedEdge(4, 5);
		graph.addDirectedEdge(5, 2);
		ParentBeforeChildGraphIterator<Integer> it = graph.parentBeforeChildIterator();
		
		// Get the iteration result.
		List<Integer> result = new ArrayList<Integer>();
		while(it.hasNext()) {
			result.add(it.next());
		}
		
		// Assert the iteration result with the expected answer.
		assertThat(result.toArray(new Integer[0])).isEqualTo(new Integer[] {1});
	}
	
	/**
	 * Tests that the next() method from the parent-before-child iterator throws an exception when no more elements
	 * are available using graph: 1 -> 2.
	 */
	@Test
	void testPBCIterationNextNoMoreElements() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2));
		graph.addDirectedEdge(1, 2);
		ParentBeforeChildGraphIterator<Integer> it = graph.parentBeforeChildIterator();

		// Assert that the iterator has a next element and skip one element.
		assertThat(it.hasNext()).isTrue();
		it.next();
		
		// Assert that the iterator has a next element and skip one element.
		assertThat(it.hasNext()).isTrue();
		it.next();
		
		// Assert that the iterator has no more elements and that a call to next() throws an exception.
		assertThat(it.hasNext()).isFalse();
		assertThrows(NoSuchElementException.class, () -> it.next());
	}
	
	/**
	 * Tests the child-before-parent iteration order over graph: 1 <- 2 <- 3 <- 4.
	 */
	@Test
	void testCBPIterationOrder() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4));
		graph.addDirectedEdge(2, 1);
		graph.addDirectedEdge(3, 2);
		graph.addDirectedEdge(4, 3);
		ChildBeforeParentGraphIterator<Integer> it = graph.childBeforeParentIterator();
		
		// Get the iteration result.
		List<Integer> result = new ArrayList<Integer>();
		while(it.hasNext()) {
			result.add(it.next());
		}
		
		// Assert the iteration result with the expected answer.
		assertThat(result.toArray(new Integer[0])).isEqualTo(new Integer[] {1, 2, 3, 4});
	}
	
	/**
	 * Tests the child-before-parent iteration order over graph:
	 * <pre>
	 * 1 <- 2 <- 3 <- 4
	 *      ^
	 *       --- 5 <- 6
	 * </pre>
	 */
	@Test
	void testCBPIterationOrder2() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6));
		graph.addDirectedEdge(2, 1);
		graph.addDirectedEdge(3, 2);
		graph.addDirectedEdge(4, 3);
		graph.addDirectedEdge(5, 2);
		graph.addDirectedEdge(6, 5);
		ChildBeforeParentGraphIterator<Integer> it = graph.childBeforeParentIterator();
		
		// Get the iteration result.
		List<Integer> result = new ArrayList<Integer>();
		while(it.hasNext()) {
			result.add(it.next());
		}
		
		// Assert the iteration result with the expected answer.
		assertThat(result.toArray(new Integer[0])).isIn(
				new Integer[] {1, 2, 3, 5, 4, 6},
				new Integer[] {1, 2, 5, 3, 6, 4});
	}
	
	/**
	 * Tests the child-before-parent iteration order over graph:
	 * <pre>
	 * 1 <- 2 <- 3 <- 4
	 *      ^    ^
	 *      |    |
	 *       --- 5 <- 6
	 * </pre>
	 */
	@Test
	void testCBPIterationOrder3() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6));
		graph.addDirectedEdge(2, 1);
		graph.addDirectedEdge(3, 2);
		graph.addDirectedEdge(4, 3);
		graph.addDirectedEdge(5, 3);
		graph.addDirectedEdge(5, 2);
		graph.addDirectedEdge(6, 5);
		ChildBeforeParentGraphIterator<Integer> it = graph.childBeforeParentIterator();
		
		// Get the iteration result.
		List<Integer> result = new ArrayList<Integer>();
		while(it.hasNext()) {
			result.add(it.next());
		}
		
		// Assert the iteration result with the expected answer.
		assertThat(result.toArray(new Integer[0])).isIn(
				new Integer[] {1, 2, 3, 4, 5, 6},
				new Integer[] {1, 2, 3, 5, 4, 6});
	}
	
	/**
	 * Tests the child-before-parent iteration order over graph:
	 * <pre>
	 * 1 <- 2 <- 3 <- 4
	 *      ^    |
	 *      |    v
	 *       --- 5 <- 6
	 * </pre>
	 */
	@Test
	void testCBPIterationOrder4() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6));
		graph.addDirectedEdge(2, 1);
		graph.addDirectedEdge(3, 2);
		graph.addDirectedEdge(4, 3);
		graph.addDirectedEdge(3, 5);
		graph.addDirectedEdge(5, 2);
		graph.addDirectedEdge(6, 5);
		ChildBeforeParentGraphIterator<Integer> it = graph.childBeforeParentIterator();
		
		// Get the iteration result.
		List<Integer> result = new ArrayList<Integer>();
		while(it.hasNext()) {
			result.add(it.next());
		}
		
		// Assert the iteration result with the expected answer.
		assertThat(result.toArray(new Integer[0])).isIn(
				new Integer[] {1, 2, 5, 3, 6, 4},
				new Integer[] {1, 2, 5, 6, 3, 4});
	}
	
	/**
	 * Tests the child-before-parent iteration order over graph:
	 * <pre>
	 *      5    6
	 *      ^    ^
	 * 1 <- 2 <- 3 <- 4
	 * </pre>
	 */
	@Test
	void testCBPIterationOrder5() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6));
		graph.addDirectedEdge(2, 1);
		graph.addDirectedEdge(3, 2);
		graph.addDirectedEdge(4, 3);
		graph.addDirectedEdge(2, 5);
		graph.addDirectedEdge(4, 6);
		ChildBeforeParentGraphIterator<Integer> it = graph.childBeforeParentIterator();
		
		// Get the iteration result.
		List<Integer> result = new ArrayList<Integer>();
		while(it.hasNext()) {
			result.add(it.next());
		}
		
		// Assert the iteration result with the expected answer.
		assertThat(result.toArray(new Integer[0])).isIn(
				new Integer[] {1, 5, 6, 2, 3, 4},
				new Integer[] {1, 6, 5, 2, 3, 4},
				new Integer[] {5, 1, 6, 2, 3, 4},
				new Integer[] {5, 6, 1, 2, 3, 4},
				new Integer[] {6, 1, 5, 2, 3, 4},
				new Integer[] {6, 5, 1, 2, 3, 4});
	}
	
	/**
	 * Tests the child-before-parent iteration order over graph:
	 * <pre>
	 * 1 <- 2 <- 3
	 * ^    |    ^
	 * |    v    |
	 * 4 <- 5 <- 6
	 * </pre>
	 */
	@Test
	void testCBPIterationOrder6() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6));
		graph.addDirectedEdge(2, 1);
		graph.addDirectedEdge(4, 1);
		graph.addDirectedEdge(3, 2);
		graph.addDirectedEdge(6, 3);
		graph.addDirectedEdge(5, 4);
		graph.addDirectedEdge(2, 5);
		graph.addDirectedEdge(6, 5);
		ChildBeforeParentGraphIterator<Integer> it = graph.childBeforeParentIterator();
		
		// Get the iteration result.
		List<Integer> result = new ArrayList<Integer>();
		while(it.hasNext()) {
			result.add(it.next());
		}
		
		// Assert the iteration result with the expected answer.
		assertThat(result.toArray(new Integer[0])).isEqualTo(new Integer[] {1, 4, 5, 2, 3, 6});
	}
	
	/**
	 * Tests the child-before-parent iteration order over graph:
	 * <pre>
	 * 1 <- 2 <- 5
	 * |    ^
	 * v    |
	 * 4 -> 3
	 * </pre>
	 */
	@Test
	void testCBPIterationOrderCycle() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5));
		graph.addDirectedEdge(2, 1);
		graph.addDirectedEdge(3, 2);
		graph.addDirectedEdge(5, 2);
		graph.addDirectedEdge(4, 3);
		graph.addDirectedEdge(1, 4);
		ChildBeforeParentGraphIterator<Integer> it = graph.childBeforeParentIterator();
		
		// Get the iteration result.
		List<Integer> result = new ArrayList<Integer>();
		while(it.hasNext()) {
			result.add(it.next());
		}
		
		// Assert the iteration result with the expected answer.
		assertThat(result).isEmpty();
	}
	
	/**
	 * Tests the child-before-parent iteration order over graph:
	 * <pre>
	 * 1 <- 2 <- 3 <- 6
	 *      |    ^
	 *      v    |
	 *      5 -> 4
	 * </pre>
	 */
	@Test
	void testCBPIterationOrderCycle2() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6));
		graph.addDirectedEdge(2, 1);
		graph.addDirectedEdge(3, 2);
		graph.addDirectedEdge(4, 3);
		graph.addDirectedEdge(6, 3);
		graph.addDirectedEdge(5, 4);
		graph.addDirectedEdge(2, 5);
		ChildBeforeParentGraphIterator<Integer> it = graph.childBeforeParentIterator();
		
		// Get the iteration result.
		List<Integer> result = new ArrayList<Integer>();
		while(it.hasNext()) {
			result.add(it.next());
		}
		
		// Assert the iteration result with the expected answer.
		assertThat(result.toArray(new Integer[0])).isEqualTo(new Integer[] {1});
	}
	
	/**
	 * Tests that the next() method from the child-before-parent iterator throws an exception when no more elements
	 * are available using graph: 1 -> 2.
	 */
	@Test
	void testCBPIterationNextNoMoreElements() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2));
		graph.addDirectedEdge(1, 2);
		ChildBeforeParentGraphIterator<Integer> it = graph.childBeforeParentIterator();

		// Assert that the iterator has a next element and skip one element.
		assertThat(it.hasNext()).isTrue();
		it.next();
		
		// Assert that the iterator has a next element and skip one element.
		assertThat(it.hasNext()).isTrue();
		it.next();
		
		// Assert that the iterator has no more elements and that a call to next() throws an exception.
		assertThat(it.hasNext()).isFalse();
		assertThrows(NoSuchElementException.class, () -> it.next());
	}
	
	/**
	 * Tests {@link Graph#getStronglyConnectedComponents()} for graph:
	 * <pre>
	 * 1 -> 2 -> 3 -> 6
	 *      ^    |
	 *      |    v
	 *      5 <- 4
	 * </pre>
	 */
	@Test
	void testGetStronglyConnectedComponents() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6));
		graph.addDirectedEdge(1, 2);
		graph.addDirectedEdge(2, 3);
		graph.addDirectedEdge(3, 4);
		graph.addDirectedEdge(3, 6);
		graph.addDirectedEdge(4, 5);
		graph.addDirectedEdge(5, 2);
		
		// Get the strongly connected components.
		Set<Set<Integer>> sccs = graph.getStronglyConnectedComponents();
		
		// Assert the result with the expected answer.
		assertThat(sccs).containsOnly(
				new HashSet<Integer>(Arrays.asList(1)),
				new HashSet<Integer>(Arrays.asList(2, 3, 4, 5)),
				new HashSet<Integer>(Arrays.asList(6)));
	}
	
	/**
	 * Tests {@link Graph#getStronglyConnectedComponents()} for graph:
	 * <pre>
	 * 1 -> 2 -> 3 -> 6   11 <-10    13
	 *      ^    |    |    ^  / ^
	 *      |    v    v    v /  |
	 *      5 <- 4 <- 7 -> 8 -> 9 -> 12
	 * </pre>
	 */
	@Test
	void testGetStronglyConnectedComponents2() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13));
		graph.addDirectedEdge(1, 2);
		graph.addDirectedEdge(2, 3);
		graph.addDirectedEdge(3, 4);
		graph.addDirectedEdge(3, 6);
		graph.addDirectedEdge(4, 5);
		graph.addDirectedEdge(5, 2);
		graph.addDirectedEdge(6, 7);
		graph.addDirectedEdge(7, 4);
		graph.addDirectedEdge(7, 8);
		graph.addDirectedEdge(8, 9);
		graph.addDirectedEdge(8, 10);
		graph.addDirectedEdge(8, 11);
		graph.addDirectedEdge(9, 10);
		graph.addDirectedEdge(9, 12);
		graph.addDirectedEdge(10, 8);
		graph.addDirectedEdge(10, 11);
		graph.addDirectedEdge(11, 8);
		
		// Get the strongly connected components.
		Set<Set<Integer>> sccs = graph.getStronglyConnectedComponents();
		
		// Assert the result with the expected answer.
		assertThat(sccs).containsOnly(
				new HashSet<Integer>(Arrays.asList(1)),
				new HashSet<Integer>(Arrays.asList(2, 3, 4, 5, 6, 7)),
				new HashSet<Integer>(Arrays.asList(8, 9, 10, 11)),
				new HashSet<Integer>(Arrays.asList(12)),
				new HashSet<Integer>(Arrays.asList(13)));
	}
	
	/**
	 * Tests the parent-before-child iteration order from a given start node over graph: 1 -> 2 -> 3 -> 4.
	 */
	@Test
	void testPBCIterationOrderCustomStartNode() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4));
		graph.addDirectedEdge(1, 2);
		graph.addDirectedEdge(2, 3);
		graph.addDirectedEdge(3, 4);
		
		// Get the iterator, starting at node 2.
		ParentBeforeChildGraphIterator<Integer> it = graph.parentBeforeChildIterator(2);
		
		// Get the iteration result.
		List<Integer> result = new ArrayList<Integer>();
		while(it.hasNext()) {
			result.add(it.next());
		}
		
		// Assert the iteration result with the expected answer.
		assertThat(result.toArray(new Integer[0])).isEqualTo(new Integer[] {2, 3, 4});
	}
	
	/**
	 * Tests the parent-before-child iteration order from an unexisting start node over graph: 1 -> 2 -> 3 -> 4.
	 */
	@Test
	void testPBCIterationOrderUnexistingStartNode() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4));
		graph.addDirectedEdge(1, 2);
		graph.addDirectedEdge(2, 3);
		graph.addDirectedEdge(3, 4);
		
		// Assert that an exception is thrown when creating an iterator from an unexisting node.
		assertThrows(IllegalArgumentException.class, () -> graph.parentBeforeChildIterator(100));
	}
	
	/**
	 * Tests the child-before-parent iteration order from a given start node over graph: 1 -> 2 -> 3 -> 4.
	 */
	@Test
	void testCBPIterationOrderCustomStartNode() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4));
		graph.addDirectedEdge(1, 2);
		graph.addDirectedEdge(2, 3);
		graph.addDirectedEdge(3, 4);
		
		// Get the iterator, starting at node 2.
		ChildBeforeParentGraphIterator<Integer> it = graph.childBeforeParentIterator(2);
		
		// Get the iteration result.
		List<Integer> result = new ArrayList<Integer>();
		while(it.hasNext()) {
			result.add(it.next());
		}
		
		// Assert the iteration result with the expected answer.
		assertThat(result.toArray(new Integer[0])).isEqualTo(new Integer[] {2, 1});
	}
	
	/**
	 * Tests the child-before-parent iteration order from an unexisting start node over graph: 1 -> 2 -> 3 -> 4.
	 */
	@Test
	void testCBPIterationOrderUnexistingStartNode() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4));
		graph.addDirectedEdge(1, 2);
		graph.addDirectedEdge(2, 3);
		graph.addDirectedEdge(3, 4);
		
		// Assert that an exception is thrown when creating an iterator from an unexisting node.
		assertThrows(IllegalArgumentException.class, () -> graph.childBeforeParentIterator(100));
	}
	
	/**
	 * Tests the getAncestors method using graph:
	 * <pre>
	 * 1 <- 2 <- 3 <- 4
	 *      |    ^
	 *      v    |
	 * 5 <- 6    7 <- 8
	 * </pre>
	 */
	@Test
	void testGetAncestors() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
		graph.addDirectedEdge(2, 1);
		graph.addDirectedEdge(2, 6);
		graph.addDirectedEdge(3, 2);
		graph.addDirectedEdge(4, 3);
		graph.addDirectedEdge(6, 5);
		graph.addDirectedEdge(7, 3);
		graph.addDirectedEdge(8, 7);
		
		// Get the ancestors of node 3.
		Set<Integer> ancestors = graph.getAncestors(3);
		
		// Assert the result with the expected result.
		assertThat(ancestors).containsExactlyInAnyOrder(3, 4, 7, 8);
	}
	
	/**
	 * Tests that the getAncestors method throws an exception when supplying an unexisting node value using graph:
	 * <pre>
	 * 1 <- 2 <- 3 <- 4
	 *      |    ^
	 *      v    |
	 * 5 <- 6    7 <- 8
	 * </pre>
	 */
	@Test
	void testGetAncestorsUnexistingNode() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
		graph.addDirectedEdge(2, 1);
		graph.addDirectedEdge(2, 6);
		graph.addDirectedEdge(3, 2);
		graph.addDirectedEdge(4, 3);
		graph.addDirectedEdge(6, 5);
		graph.addDirectedEdge(7, 3);
		graph.addDirectedEdge(8, 7);
		
		// Assert that an exception is thrown when getting the ancestors of an unexisting node.
		assertThrows(IllegalArgumentException.class, () -> graph.getAncestors(100));
	}
	
	/**
	 * Tests the getAncestors method using graph:
	 * <pre>
	 * 1 <- 2 <- 3 <- 4
	 *      |    ^
	 *      v    |
	 * 5 <- 6 -> 7 <- 8
	 * </pre>
	 */
	@Test
	void testGetAncestorsCycle() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
		graph.addDirectedEdge(2, 1);
		graph.addDirectedEdge(2, 6);
		graph.addDirectedEdge(3, 2);
		graph.addDirectedEdge(4, 3);
		graph.addDirectedEdge(6, 5);
		graph.addDirectedEdge(6, 7);
		graph.addDirectedEdge(7, 3);
		graph.addDirectedEdge(8, 7);
		
		// Get the ancestors of node 3.
		Set<Integer> ancestors = graph.getAncestors(3);
		
		// Assert the result with the expected result.
		assertThat(ancestors).containsExactlyInAnyOrder(2, 3, 4, 6, 7, 8);
	}
	
	/**
	 * Tests the getDescendents method using graph:
	 * <pre>
	 * 1 <- 2 <- 3 <- 4
	 *      |    ^
	 *      v    |
	 * 5 <- 6    7 <- 8
	 * </pre>
	 */
	@Test
	void testGetDescendents() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
		graph.addDirectedEdge(2, 1);
		graph.addDirectedEdge(2, 6);
		graph.addDirectedEdge(3, 2);
		graph.addDirectedEdge(4, 3);
		graph.addDirectedEdge(6, 5);
		graph.addDirectedEdge(7, 3);
		graph.addDirectedEdge(8, 7);
		
		// Get the ancestors of node 3.
		Set<Integer> ancestors = graph.getDescendents(3);
		
		// Assert the result with the expected result.
		assertThat(ancestors).containsExactlyInAnyOrder(1, 2, 3, 5, 6);
	}
	
	/**
	 * Tests that the getDescendents method throws an exception when supplying an unexisting node value using graph:
	 * <pre>
	 * 1 <- 2 <- 3 <- 4
	 *      |    ^
	 *      v    |
	 * 5 <- 6    7 <- 8
	 * </pre>
	 */
	@Test
	void testGetDescendentsUnexistingNode() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
		graph.addDirectedEdge(2, 1);
		graph.addDirectedEdge(2, 6);
		graph.addDirectedEdge(3, 2);
		graph.addDirectedEdge(4, 3);
		graph.addDirectedEdge(6, 5);
		graph.addDirectedEdge(7, 3);
		graph.addDirectedEdge(8, 7);
		
		// Assert that an exception is thrown when getting the ancestors of an unexisting node.
		assertThrows(IllegalArgumentException.class, () -> graph.getDescendents(100));
	}
	
	/**
	 * Tests the getDescendents method using graph:
	 * <pre>
	 * 1 <- 2 <- 3 <- 4
	 *      |    ^
	 *      v    |
	 * 5 <- 6 -> 7 <- 8
	 * </pre>
	 */
	@Test
	void testGetDescendentsCycle() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
		graph.addDirectedEdge(2, 1);
		graph.addDirectedEdge(2, 6);
		graph.addDirectedEdge(3, 2);
		graph.addDirectedEdge(4, 3);
		graph.addDirectedEdge(6, 5);
		graph.addDirectedEdge(6, 7);
		graph.addDirectedEdge(7, 3);
		graph.addDirectedEdge(8, 7);
		
		// Get the ancestors of node 3.
		Set<Integer> ancestors = graph.getDescendents(3);
		
		// Assert the result with the expected result.
		assertThat(ancestors).containsExactlyInAnyOrder(1, 2, 3, 5, 6, 7);
	}
	
	/**
	 * Tests the parent-before-child iterator removeDescendents method using graph:
	 * <pre>
	 * 1 <- 2 <- 3 <- 4
	 *      |    ^
	 *      v    |
	 * 5 <- 6    7 <- 8
	 * </pre>
	 */
	@Test
	void testPBCRemoveDescendents() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
		graph.addDirectedEdge(2, 1);
		graph.addDirectedEdge(2, 6);
		graph.addDirectedEdge(3, 2);
		graph.addDirectedEdge(4, 3);
		graph.addDirectedEdge(6, 5);
		graph.addDirectedEdge(7, 3);
		graph.addDirectedEdge(8, 7);
		
		// Remove the descendents of node 3.
		ParentBeforeChildGraphIterator<Integer> it = graph.parentBeforeChildIterator(3);
		assertThat(it.removeDescendents()).isNull();
		assertThat(it.next()).isEqualTo(3);
		List<Integer> descendents = it.removeDescendents();
		
		// Assert the result with the expected result.
		assertThat(descendents.toArray(new Integer[0])).isIn(
				new Integer[] {3, 2, 1, 6, 5},
				new Integer[] {3, 2, 6, 1, 5});
		
		// Assert that the nodes were removed from the graph.
		assertThat(graph.getNodes()).containsExactlyInAnyOrder(4, 7, 8);
	}
	
	/**
	 * Tests the parent-before-child iterator removeDescendents method using graph:
	 * <pre>
	 * 1 <- 2 <- 3 <- 4
	 *      |    ^
	 *      v    |
	 * 5 <- 6 -> 7 <- 8
	 *        |  |
	 *         --
	 * </pre>
	 */
	@Test
	void testPBCRemoveDescendentsCycle() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
		graph.addDirectedEdge(2, 1);
		graph.addDirectedEdge(2, 6);
		graph.addDirectedEdge(3, 2);
		graph.addDirectedEdge(4, 3);
		graph.addDirectedEdge(6, 5);
		graph.addDirectedEdge(6, 7);
		graph.addDirectedEdge(7, 3);
		graph.addDirectedEdge(7, 7);
		graph.addDirectedEdge(8, 7);
		
		// Remove the descendents of node 3.
		ParentBeforeChildGraphIterator<Integer> it = graph.parentBeforeChildIterator(3);
		assertThat(it.removeDescendents()).isNull();
		assertThat(it.next()).isEqualTo(3);
		List<Integer> descendents = it.removeDescendents();
		
		// Assert the result with the expected result.
		assertThat(descendents.toArray(new Integer[0])).isIn(
				new Integer[] {3, 2, 1, 6, 5, 7},
				new Integer[] {3, 2, 1, 6, 7, 5},
				new Integer[] {3, 2, 6, 1, 5, 7},
				new Integer[] {3, 2, 6, 1, 7, 5});
		
		// Assert that the nodes were removed from the graph.
		assertThat(graph.getNodes()).containsExactlyInAnyOrder(4, 8);
	}
	
	/**
	 * Tests the child-before-parent iterator removeAncestors method using graph:
	 * <pre>
	 * 1 <- 2 <- 3 <- 4
	 *      |    ^
	 *      v    |
	 * 5 <- 6    7 <- 8
	 * </pre>
	 */
	@Test
	void testPBCRemoveAncestors() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
		graph.addDirectedEdge(2, 1);
		graph.addDirectedEdge(2, 6);
		graph.addDirectedEdge(3, 2);
		graph.addDirectedEdge(4, 3);
		graph.addDirectedEdge(6, 5);
		graph.addDirectedEdge(7, 3);
		graph.addDirectedEdge(8, 7);
		
		// Remove the ancestors of node 3.
		ChildBeforeParentGraphIterator<Integer> it = graph.childBeforeParentIterator(3);
		assertThat(it.removeAncestors()).isNull();
		assertThat(it.next()).isEqualTo(3);
		List<Integer> ancestors = it.removeAncestors();
		
		// Assert the result with the expected result.
		assertThat(ancestors.toArray(new Integer[0])).isIn(
				new Integer[] {3, 4, 7, 8},
				new Integer[] {3, 7, 4, 8});
		
		// Assert that the nodes were removed from the graph.
		assertThat(graph.getNodes()).containsExactlyInAnyOrder(1, 2, 5, 6);
	}
	
	/**
	 * Tests the child-before-parent iterator removeAncestors method using graph:
	 * <pre>
	 * 1 <- 2 <- 3 <- 4
	 *      |    ^
	 *      v    |
	 * 5 <- 6 -> 7 <- 8
	 *        |  |
	 *         --
	 * </pre>
	 */
	@Test
	void testPBCRemoveAncestorsCycle() {
		
		// Construct the graph.
		Graph<Integer> graph = new Graph<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
		graph.addDirectedEdge(2, 1);
		graph.addDirectedEdge(2, 6);
		graph.addDirectedEdge(3, 2);
		graph.addDirectedEdge(4, 3);
		graph.addDirectedEdge(6, 5);
		graph.addDirectedEdge(6, 7);
		graph.addDirectedEdge(7, 3);
		graph.addDirectedEdge(7, 7);
		graph.addDirectedEdge(8, 7);
		
		// Remove the ancestors of node 3.
		ChildBeforeParentGraphIterator<Integer> it = graph.childBeforeParentIterator(3);
		assertThat(it.removeAncestors()).isNull();
		assertThat(it.next()).isEqualTo(3);
		List<Integer> ancestors = it.removeAncestors();
		
		// Assert the result with the expected result.
		assertThat(ancestors.toArray(new Integer[0])).isIn(
				new Integer[] {3, 4, 7, 6, 8, 2},
				new Integer[] {3, 4, 7, 8, 6, 2},
				new Integer[] {3, 7, 4, 6, 8, 2},
				new Integer[] {3, 7, 4, 8, 6, 2});
		
		// Assert that the nodes were removed from the graph.
		assertThat(graph.getNodes()).containsExactlyInAnyOrder(1, 5);
	}
	
}
