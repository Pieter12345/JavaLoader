package io.github.pieter12345.graph;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.pieter12345.graph.Graph.ChildBeforeParentGraphIterator;
import io.github.pieter12345.graph.Graph.ParentBeforeChildGraphIterator;

class GraphTest {
	
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
		assertThat(result.toArray(new Integer[0])).isEqualTo(new Integer[] {});
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
		assertThat(result.toArray(new Integer[0])).isEqualTo(new Integer[] {});
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
	 *      ^    |    |    |    ^
	 *      |    v    v    v    |
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
		graph.addDirectedEdge(9, 10);
		graph.addDirectedEdge(9, 12);
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
	
}
