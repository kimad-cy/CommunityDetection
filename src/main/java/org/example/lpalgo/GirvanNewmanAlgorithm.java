package org.example.lpalgo;

import java.util.*;

/**
 * Girvan-Newman Algorithm for Community Detection
 *
 * Algorithm Overview:
 * 1. Calculate edge betweenness for all edges (using BFS from all nodes)
 * 2. Remove edge with highest betweenness
 * 3. Recalculate betweenness and repeat
 * 4. Track modularity at each step
 * 5. Return partition with maximum modularity
 *
 * Key Features:
 * - Divisive hierarchical clustering
 * - Finds communities by removing "bridge" edges
 * - Time complexity: O(m²n) for m edges, n nodes
 * - Can detect hierarchical structure
 */
class GirvanNewmanAlgorithm {
    private Graph originalGraph;
    private Graph workingGraph;
    private List<GraphPartition> partitionHistory;
    private GraphPartition bestPartition;
    private double maxModularity;
    private int currentStep;

    public GirvanNewmanAlgorithm(Graph graph) {
        this.originalGraph = graph;
        this.workingGraph = graph.copy();
        this.partitionHistory = new ArrayList<>();
        this.currentStep = 0;
        this.maxModularity = -1.0;

        // Initial partition (all nodes in one component)
        GraphPartition initial = new GraphPartition(findConnectedComponents(workingGraph), 0.0);
        partitionHistory.add(initial);
        bestPartition = initial;
    }

    /**
     * Perform one step: remove one edge with highest betweenness
     * Returns false if no edges left
     */
    public boolean performOneStep() {
        if (workingGraph.nodes().isEmpty() || !hasEdges(workingGraph)) {
            return false;
        }

        // Step 1: Calculate edge betweenness for all edges
        Map<Edge, Double> betweenness = calculateEdgeBetweenness();

        if (betweenness.isEmpty()) {
            return false;
        }

        // Step 2: Find and remove edge with highest betweenness
        Edge maxEdge = null;
        double maxBetweenness = -1;
        for (Map.Entry<Edge, Double> entry : betweenness.entrySet()) {
            if (entry.getValue() > maxBetweenness) {
                maxBetweenness = entry.getValue();
                maxEdge = entry.getKey();
            }
        }

        if (maxEdge != null) {
            workingGraph.removeEdge(maxEdge.u, maxEdge.v);
            currentStep++;
        }

        // Step 3: Find current communities (connected components)
        Map<Integer, Set<Integer>> communities = findConnectedComponents(workingGraph);

        // Step 4: Calculate modularity
        double modularity = calculateModularity(communities);

        // Step 5: Save partition
        GraphPartition partition = new GraphPartition(communities, modularity);
        partitionHistory.add(partition);

        // Step 6: Update best partition if modularity improved
        if (modularity > maxModularity) {
            maxModularity = modularity;
            bestPartition = partition;
        }

        return true;
    }

    /**
     * Calculate edge betweenness using BFS from all nodes
     * Edge betweenness = number of shortest paths passing through the edge
     */
    private Map<Edge, Double> calculateEdgeBetweenness() {
        Map<Edge, Double> betweenness = new HashMap<>();

        // Initialize all edges with 0
        for (int u : workingGraph.nodes()) {
            for (int v : workingGraph.neighbors(u)) {
                if (u < v) {
                    betweenness.put(new Edge(u, v), 0.0);
                }
            }
        }

        // Run BFS from each node
        for (int source : workingGraph.nodes()) {
            Map<Edge, Double> pathCounts = bfsEdgeBetweenness(source);

            // Add to total betweenness
            for (Map.Entry<Edge, Double> entry : pathCounts.entrySet()) {
                Edge edge = entry.getKey();
                betweenness.put(edge, betweenness.getOrDefault(edge, 0.0) + entry.getValue());
            }
        }

        return betweenness;
    }

    /**
     * BFS-based edge betweenness from a single source
     * Returns contribution of shortest paths from this source
     */
    private Map<Edge, Double> bfsEdgeBetweenness(int source) {
        Map<Edge, Double> edgeCount = new HashMap<>();
        Map<Integer, Integer> distance = new HashMap<>();
        Map<Integer, Double> pathCount = new HashMap<>();
        Map<Integer, List<Integer>> predecessors = new HashMap<>();

        // Initialize
        for (int node : workingGraph.nodes()) {
            distance.put(node, Integer.MAX_VALUE);
            pathCount.put(node, 0.0);
            predecessors.put(node, new ArrayList<>());
        }

        distance.put(source, 0);
        pathCount.put(source, 1.0);

        // BFS
        Queue<Integer> queue = new LinkedList<>();
        Stack<Integer> stack = new Stack<>();
        queue.add(source);

        while (!queue.isEmpty()) {
            int v = queue.poll();
            stack.push(v);

            for (int w : workingGraph.neighbors(v)) {
                // First time we see w?
                if (distance.get(w) == Integer.MAX_VALUE) {
                    distance.put(w, distance.get(v) + 1);
                    queue.add(w);
                }

                // Shortest path to w via v?
                if (distance.get(w) == distance.get(v) + 1) {
                    pathCount.put(w, pathCount.get(w) + pathCount.get(v));
                    predecessors.get(w).add(v);
                }
            }
        }

        // Back-propagation
        Map<Integer, Double> dependency = new HashMap<>();
        for (int node : workingGraph.nodes()) {
            dependency.put(node, 0.0);
        }

        while (!stack.isEmpty()) {
            int w = stack.pop();

            for (int v : predecessors.get(w)) {
                double credit = (pathCount.get(v) / pathCount.get(w)) * (1.0 + dependency.get(w));

                Edge edge = new Edge(Math.min(v, w), Math.max(v, w));
                edgeCount.put(edge, edgeCount.getOrDefault(edge, 0.0) + credit);

                dependency.put(v, dependency.get(v) + credit);
            }
        }

        return edgeCount;
    }

    /**
     * Find connected components using DFS
     */
    private Map<Integer, Set<Integer>> findConnectedComponents(Graph g) {
        Map<Integer, Set<Integer>> components = new HashMap<>();
        Set<Integer> visited = new HashSet<>();
        int componentId = 0;

        for (int node : g.nodes()) {
            if (!visited.contains(node)) {
                Set<Integer> component = new HashSet<>();
                dfs(g, node, visited, component);
                components.put(componentId++, component);
            }
        }

        return components;
    }

    /**
     * DFS helper for finding connected components
     */
    private void dfs(Graph g, int node, Set<Integer> visited, Set<Integer> component) {
        visited.add(node);
        component.add(node);

        for (int neighbor : g.neighbors(node)) {
            if (!visited.contains(neighbor)) {
                dfs(g, neighbor, visited, component);
            }
        }
    }

    /**
     * Calculate modularity for a given partition
     */
    private double calculateModularity(Map<Integer, Set<Integer>> communities) {
        double q = 0.0;
        double m = originalGraph.totalWeight();

        if (m == 0) return 0.0;

        for (Set<Integer> community : communities.values()) {
            double lc = 0.0;  // Internal edges
            double dc = 0.0;  // Total degree

            for (int i : community) {
                for (int j : community) {
                    if (i < j && originalGraph.neighbors(i).contains(j)) {
                        lc += originalGraph.getWeight(i, j);
                    }
                }
                dc += originalGraph.weightedDegree(i);
            }

            q += (lc / m) - Math.pow(dc / (2.0 * m), 2);
        }

        return q;
    }

    /**
     * Check if graph has any edges
     */
    private boolean hasEdges(Graph g) {
        for (int node : g.nodes()) {
            if (!g.neighbors(node).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get current communities
     */
    public Map<Integer, Set<Integer>> getCurrentCommunities() {
        if (partitionHistory.isEmpty()) {
            return new HashMap<>();
        }
        return partitionHistory.get(partitionHistory.size() - 1).communities;
    }

    /**
     * Get best communities (max modularity)
     */
    public Map<Integer, Set<Integer>> getBestCommunities() {
        return bestPartition != null ? bestPartition.communities : new HashMap<>();
    }

    /**
     * Get current modularity
     */
    public double getCurrentModularity() {
        if (partitionHistory.isEmpty()) {
            return 0.0;
        }
        return partitionHistory.get(partitionHistory.size() - 1).modularity;
    }

    /**
     * Get maximum modularity found so far
     */
    public double getMaxModularity() {
        return maxModularity;
    }

    /**
     * Get current step number
     */
    public int getCurrentStep() {
        return currentStep;
    }

    /**
     * Get number of edges remaining
     */
    public int getRemainingEdges() {
        int count = 0;
        for (int node : workingGraph.nodes()) {
            count += workingGraph.neighbors(node).size();
        }
        return count / 2;
    }

    /**
     * Get original graph (for visualization)
     */
    public Graph getOriginalGraph() {
        return originalGraph;
    }
}

/**
 * Helper class to represent a graph partition
 */
class GraphPartition {
    Map<Integer, Set<Integer>> communities;
    double modularity;

    GraphPartition(Map<Integer, Set<Integer>> communities, double modularity) {
        this.communities = new HashMap<>(communities);
        this.modularity = modularity;
    }
}

/**
 * Helper class to represent an edge (for betweenness calculation)
 */
class Edge {
    int u, v;

    Edge(int u, int v) {
        this.u = Math.min(u, v);
        this.v = Math.max(u, v);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return u == edge.u && v == edge.v;
    }

    @Override
    public int hashCode() {
        return Objects.hash(u, v);
    }

    @Override
    public String toString() {
        return "(" + u + "," + v + ")";
    }
}