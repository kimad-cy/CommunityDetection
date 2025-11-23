package org.example.lpalgo;

import java.util.*;

/**
 * Graph class using weighted adjacency map representation
 * Supports both weighted and unweighted edges
 */
class Graph {
    // Each node -> map(neighbor -> weight)
    private final Map<Integer, Map<Integer, Double>> adj = new HashMap<>();

    // ------------------------
    // Add a node
    // ------------------------
    public void addNode(int v) {
        adj.putIfAbsent(v, new HashMap<>());
    }

    // ------------------------
    // Add an undirected edge
    // ------------------------
    public void addEdge(int u, int v) {
        addEdge(u, v, 1.0);
    }

    public void addEdge(int u, int v, double weight) {
        addNode(u);
        addNode(v);
        adj.get(u).put(v, weight);
        adj.get(v).put(u, weight);
    }

    // ------------------------
    // Remove edge (needed for Girvan–Newman)
    // ------------------------
    public void removeEdge(int u, int v) {
        if (adj.containsKey(u)) adj.get(u).remove(v);
        if (adj.containsKey(v)) adj.get(v).remove(u);
    }

    // ------------------------
    // Get neighbors of a node
    // ------------------------
    public Set<Integer> neighbors(int v) {
        return adj.containsKey(v)
                ? adj.get(v).keySet()
                : Collections.emptySet();
    }

    // ------------------------
    // Get weight(u, v)
    // ------------------------
    public double getWeight(int u, int v) {
        return adj.getOrDefault(u, Collections.emptyMap())
                .getOrDefault(v, 0.0);
    }

    // ------------------------
    // Return all nodes
    // ------------------------
    public Set<Integer> nodes() {
        return adj.keySet();
    }

    // ------------------------
    // Degree of a node
    // ------------------------
    public int degree(int v) {
        return adj.getOrDefault(v, Collections.emptyMap()).size();
    }

    // Weighted degree (useful for Louvain)
    public double weightedDegree(int v) {
        return adj.getOrDefault(v, Collections.emptyMap())
                .values()
                .stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    // ------------------------
    // Total graph weight (needed for Louvain)
    // ------------------------
    public double totalWeight() {
        double sum = 0;
        for (int u : adj.keySet()) {
            for (double w : adj.get(u).values()) sum += w;
        }
        return sum / 2.0; // since edges are counted twice
    }

    // ------------------------
    // Clone graph (useful for Girvan-Newman)
    // ------------------------
    public Graph copy() {
        Graph g = new Graph();
        for (int u : adj.keySet()) {
            for (Map.Entry<Integer, Double> e : adj.get(u).entrySet()) {
                int v = e.getKey();
                double w = e.getValue();
                if (u < v) g.addEdge(u, v, w);
            }
        }
        return g;
    }
}

/**
 * Label Propagation Algorithm implementation
 *
 * Core Principle: Nodes iteratively adopt the most frequent label
 * among their neighbors until convergence.
 */
class LabelPropagation {
    private Graph graph;
    private Map<Integer, Integer> labels;
    private int iteration;

    public LabelPropagation(Graph graph) {
        this.graph = graph;
        this.iteration = 0;
        initializeLabels();
    }

    /**
     * Initialize labels: each node gets unique label (its ID)
     */
    private void initializeLabels() {
        labels = new HashMap<>();
        for (int node : graph.nodes()) {
            labels.put(node, node); // Each node starts with unique label
        }
    }

    /**
     * Perform one iteration of label propagation
     *
     * Algorithm:
     * 1. Process nodes in random order (prevents bias)
     * 2. Each node adopts most frequent neighbor label
     * 3. Break ties randomly
     *
     * @return true if any label changed, false if converged
     */
    public boolean iterate() {
        boolean changed = false;
        Map<Integer, Integer> newLabels = new HashMap<>();

        // Shuffle nodes for random processing order
        List<Integer> nodes = new ArrayList<>(graph.nodes());
        Collections.shuffle(nodes);

        for (int node : nodes) {
            int newLabel = getMostFrequentLabel(node);
            newLabels.put(node, newLabel);

            if (newLabel != labels.get(node)) {
                changed = true;
            }
        }

        labels = newLabels;
        iteration++;
        return changed;
    }

    /**
     * Find the most frequent label among neighbors
     *
     * For weighted graphs, we could weight votes by edge weights.
     * Current implementation: simple majority voting (unweighted)
     *
     * @param node The node whose label we're updating
     * @return The most frequent neighbor label (random tie-breaking)
     */
    private int getMostFrequentLabel(int node) {
        Map<Integer, Integer> labelCounts = new HashMap<>();

        // Count neighbor labels
        for (int neighbor : graph.neighbors(node)) {
            int label = labels.get(neighbor);
            labelCounts.put(label, labelCounts.getOrDefault(label, 0) + 1);
        }

        // If no neighbors, keep current label
        if (labelCounts.isEmpty()) {
            return labels.get(node);
        }

        // Find maximum count
        int maxCount = Collections.max(labelCounts.values());

        // Collect all labels with maximum count
        List<Integer> maxLabels = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : labelCounts.entrySet()) {
            if (entry.getValue() == maxCount) {
                maxLabels.add(entry.getKey());
            }
        }

        // Random tie breaking (important for non-determinism)
        return maxLabels.get(new Random().nextInt(maxLabels.size()));
    }

    /**
     * Weighted version of label propagation (optional extension)
     * Uses edge weights to weight neighbor votes
     */
    private int getMostFrequentLabelWeighted(int node) {
        Map<Integer, Double> labelWeights = new HashMap<>();

        // Weight neighbor labels by edge weight
        for (int neighbor : graph.neighbors(node)) {
            int label = labels.get(neighbor);
            double weight = graph.getWeight(node, neighbor);
            labelWeights.put(label, labelWeights.getOrDefault(label, 0.0) + weight);
        }

        if (labelWeights.isEmpty()) {
            return labels.get(node);
        }

        // Find maximum weight
        double maxWeight = Collections.max(labelWeights.values());

        // Collect all labels with maximum weight
        List<Integer> maxLabels = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : labelWeights.entrySet()) {
            if (Math.abs(entry.getValue() - maxWeight) < 1e-9) {
                maxLabels.add(entry.getKey());
            }
        }

        return maxLabels.get(new Random().nextInt(maxLabels.size()));
    }

    public int getLabel(int node) {
        return labels.get(node);
    }

    public int getIteration() {
        return iteration;
    }

    public void reset() {
        iteration = 0;
        initializeLabels();
    }

    /**
     * Get communities as a map: label -> set of nodes
     */
    public Map<Integer, Set<Integer>> getCommunities() {
        Map<Integer, Set<Integer>> communities = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : labels.entrySet()) {
            int node = entry.getKey();
            int label = entry.getValue();
            communities.computeIfAbsent(label, k -> new HashSet<>()).add(node);
        }
        return communities;
    }
}

/**
 * Simple Point class for node positions in visualization
 */

/**
 * Point class for 2D coordinates
 */
class Point {
    double x, y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Point)) return false;
        Point point = (Point) o;
        return Double.compare(point.x, x) == 0 &&
                Double.compare(point.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}