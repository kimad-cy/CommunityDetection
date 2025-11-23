package org.example.lpalgo;

import java.util.*;


/**
 * Louvain Algorithm for Community Detection
 *
 * Algorithm Overview:
 * Phase 1: Iteratively move nodes to neighboring communities to maximize modularity
 * Phase 2: Aggregate communities into super-nodes and repeat
 * Continue until modularity cannot be improved
 *
 * Key Features:
 * - Fast: O(n log n) on typical networks
 * - High modularity results
 * - Hierarchical community structure
 * - Non-overlapping communities
 */
class LouvainAlgorithm {
    private Graph graph;
    private Graph originalGraph;  // Keep reference to original
    private Map<Integer, Integer> nodeToCommunity;
    private Map<Integer, Integer> nodeToOriginalNode;  // Track original nodes
    private Map<Integer, Double> communityWeights;
    private Map<Integer, Double> communityDegrees;
    private int phase;
    private double totalWeight;

    public LouvainAlgorithm(Graph graph) {
        this.graph = graph;
        this.originalGraph = graph;  // Save original graph
        this.phase = 1;
        this.totalWeight = graph.totalWeight();

        // Initialize: each node in its own community
        this.nodeToCommunity = new HashMap<>();
        this.nodeToOriginalNode = new HashMap<>();
        for (int node : graph.nodes()) {
            nodeToCommunity.put(node, node);
            nodeToOriginalNode.put(node, node);  // Initially maps to itself
        }

        computeCommunityStats();
    }

    /**
     * Compute internal weights and degrees for each community
     */
    private void computeCommunityStats() {
        communityWeights = new HashMap<>();
        communityDegrees = new HashMap<>();

        // Initialize
        for (int comm : new HashSet<>(nodeToCommunity.values())) {
            communityWeights.put(comm, 0.0);
            communityDegrees.put(comm, 0.0);
        }

        // Calculate internal weights
        for (int node : graph.nodes()) {
            int comm = nodeToCommunity.get(node);
            communityDegrees.put(comm, communityDegrees.get(comm) + graph.weightedDegree(node));

            for (int neighbor : graph.neighbors(node)) {
                if (nodeToCommunity.get(neighbor).equals(comm)) {
                    double weight = graph.getWeight(node, neighbor);
                    communityWeights.put(comm, communityWeights.get(comm) + weight / 2.0);
                }
            }
        }
    }

    /**
     * Perform one pass of the algorithm
     * Returns true if any node changed community
     */
    public boolean performOnePass() {
        boolean improved = false;
        List<Integer> nodes = new ArrayList<>(graph.nodes());
        Collections.shuffle(nodes);

        for (int node : nodes) {
            int oldCommunity = nodeToCommunity.get(node);
            int bestCommunity = oldCommunity;
            double bestGain = 0.0;

            // Remove node from its current community
            removeNodeFromCommunity(node, oldCommunity);

            // Try all neighboring communities
            Set<Integer> neighborCommunities = new HashSet<>();
            for (int neighbor : graph.neighbors(node)) {
                neighborCommunities.add(nodeToCommunity.get(neighbor));
            }

            for (int newCommunity : neighborCommunities) {
                double gain = modularityGain(node, newCommunity);
                if (gain > bestGain) {
                    bestGain = gain;
                    bestCommunity = newCommunity;
                }
            }

            // Insert node into best community
            insertNodeToCommunity(node, bestCommunity);

            if (bestCommunity != oldCommunity) {
                improved = true;
            }
        }

        if (!improved) {
            computeCommunityStats();
        }

        return improved;
    }

    /**
     * Calculate the modularity gain of moving a node to a new community
     */
    private double modularityGain(int node, int community) {
        // Weight of edges from node to nodes in community
        double kiIn = 0.0;
        for (int neighbor : graph.neighbors(node)) {
            if (nodeToCommunity.get(neighbor).equals(community)) {
                kiIn += graph.getWeight(node, neighbor);
            }
        }

        double ki = graph.weightedDegree(node);
        double sigmaTotal = communityDegrees.getOrDefault(community, 0.0);
        double m2 = 2.0 * totalWeight;

        // Modularity gain formula
        return (kiIn / totalWeight) - (sigmaTotal * ki) / (m2 * m2);
    }

    /**
     * Remove node from its community (update stats)
     */
    private void removeNodeFromCommunity(int node, int community) {
        nodeToCommunity.put(node, -1);

        double nodeDegree = graph.weightedDegree(node);
        communityDegrees.put(community, communityDegrees.get(community) - nodeDegree);

        // Update internal weights
        for (int neighbor : graph.neighbors(node)) {
            if (nodeToCommunity.get(neighbor).equals(community)) {
                double weight = graph.getWeight(node, neighbor);
                communityWeights.put(community, communityWeights.get(community) - weight);
            }
        }
    }

    /**
     * Insert node into a community (update stats)
     */
    private void insertNodeToCommunity(int node, int community) {
        nodeToCommunity.put(node, community);

        double nodeDegree = graph.weightedDegree(node);
        communityDegrees.put(community, communityDegrees.getOrDefault(community, 0.0) + nodeDegree);

        // Update internal weights
        for (int neighbor : graph.neighbors(node)) {
            if (nodeToCommunity.get(neighbor).equals(community)) {
                double weight = graph.getWeight(node, neighbor);
                communityWeights.put(community, communityWeights.getOrDefault(community, 0.0) + weight);
            }
        }
    }

    /**
     * Compute the modularity of the current partition
     */
    public double computeModularity() {
        double q = 0.0;
        double m2 = 2.0 * totalWeight;

        Set<Integer> communities = new HashSet<>(nodeToCommunity.values());

        for (int comm : communities) {
            if (comm == -1) continue;

            double lc = communityWeights.getOrDefault(comm, 0.0);
            double dc = communityDegrees.getOrDefault(comm, 0.0);

            q += (lc / totalWeight) - (dc / m2) * (dc / m2);
        }

        return q;
    }

    /**
     * Aggregate the graph (Phase 2): create super-nodes from communities
     */
    public void aggregateGraph() {
        phase = 2;

        // Map old communities to new super-node IDs
        Map<Integer, Integer> communityToSuperNode = new HashMap<>();
        Set<Integer> communities = new HashSet<>(nodeToCommunity.values());
        int superNodeId = 0;
        for (int comm : communities) {
            if (comm != -1) {
                communityToSuperNode.put(comm, superNodeId++);
            }
        }

        // Update nodeToOriginalNode mapping for super-nodes
        Map<Integer, Integer> newNodeToOriginal = new HashMap<>();
        Map<Integer, Set<Integer>> superNodeToOriginals = new HashMap<>();

        for (Map.Entry<Integer, Integer> entry : nodeToCommunity.entrySet()) {
            int node = entry.getKey();
            int comm = entry.getValue();
            int superNode = communityToSuperNode.get(comm);

            // Track which original nodes belong to each super-node
            superNodeToOriginals.computeIfAbsent(superNode, k -> new HashSet<>())
                    .add(nodeToOriginalNode.get(node));
        }

        // Create new graph with super-nodes
        Graph newGraph = new Graph();

        // Add edges between super-nodes
        Map<String, Double> superEdges = new HashMap<>();
        for (int node : graph.nodes()) {
            int comm1 = nodeToCommunity.get(node);
            int superNode1 = communityToSuperNode.get(comm1);

            for (int neighbor : graph.neighbors(node)) {
                int comm2 = nodeToCommunity.get(neighbor);
                int superNode2 = communityToSuperNode.get(comm2);

                String key = Math.min(superNode1, superNode2) + "-" + Math.max(superNode1, superNode2);
                double weight = graph.getWeight(node, neighbor);
                superEdges.put(key, superEdges.getOrDefault(key, 0.0) + weight);
            }
        }

        // Add aggregated edges to new graph
        for (Map.Entry<String, Double> entry : superEdges.entrySet()) {
            String[] nodes = entry.getKey().split("-");
            int u = Integer.parseInt(nodes[0]);
            int v = Integer.parseInt(nodes[1]);
            newGraph.addEdge(u, v, entry.getValue());
        }

        // Update internal state
        this.graph = newGraph;
        this.totalWeight = newGraph.totalWeight();

        // Reset communities for super-nodes
        this.nodeToCommunity = new HashMap<>();
        for (int node : newGraph.nodes()) {
            nodeToCommunity.put(node, node);
        }

        // Preserve original node mapping
        this.nodeToOriginalNode = new HashMap<>();
        for (Map.Entry<Integer, Set<Integer>> entry : superNodeToOriginals.entrySet()) {
            // For super-nodes, just pick the first original (for display)
            int originalRep = entry.getValue().iterator().next();
            nodeToOriginalNode.put(entry.getKey(), originalRep);
        }

        computeCommunityStats();
    }

    /**
     * Get the community assignment for each ORIGINAL node
     */
    public Map<Integer, Integer> getCommunityMap() {
        Map<Integer, Integer> result = new HashMap<>();

        if (phase == 1) {
            // Phase 1: direct mapping
            return new HashMap<>(nodeToCommunity);
        } else {
            // Phase 2+: map back to original nodes through super-nodes
            // All original nodes are in the same community as their super-node
            for (int originalNode : originalGraph.nodes()) {
                // Find which community this original node belongs to
                result.put(originalNode, findCommunityForOriginalNode(originalNode));
            }
        }

        return result;
    }

    /**
     * Find community ID for an original node (works across phases)
     */
    private int findCommunityForOriginalNode(int originalNode) {
        // For now, return a consistent community ID based on the node's community
        // In phase 1, direct lookup; in phase 2, nodes keep their phase 1 communities
        for (Map.Entry<Integer, Integer> entry : nodeToCommunity.entrySet()) {
            if (nodeToOriginalNode.containsKey(entry.getKey()) &&
                    nodeToOriginalNode.get(entry.getKey()) == originalNode) {
                return entry.getValue();
            }
        }
        return originalNode; // fallback
    }

    /**
     * Get current phase
     */
    public int getPhase() {
        return phase;
    }

    /**
     * Get number of communities
     */
    public int getNumCommunities() {
        return new HashSet<>(nodeToCommunity.values()).size();
    }
}