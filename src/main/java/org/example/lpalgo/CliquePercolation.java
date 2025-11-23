package org.example.lpalgo;

import java.util.*;

/**
 * Clique Percolation Method Implementation
 * Uses Bron-Kerbosch algorithm to find all maximal cliques
 * Compatible with both standalone and unified visualizer
 */
class CliquePercolation {
    private Graph graph;
    private List<Set<Integer>> allCliques;
    private Map<Integer, Set<Integer>> communities;
    private boolean resultsComputed = false;

    public CliquePercolation(Graph graph) {
        this.graph = graph;
        this.allCliques = new ArrayList<>();
        this.communities = new HashMap<>();
    }

    /**
     * Find communities using CPM with given k value
     */
    public void findCommunities(int k) {
        // Step 1: Find all maximal cliques
        findAllMaximalCliques();

        // Step 2: Filter cliques of size >= k
        List<Set<Integer>> kCliques = new ArrayList<>();
        for (Set<Integer> clique : allCliques) {
            if (clique.size() >= k) {
                kCliques.add(clique);
            }
        }

        // Step 3: Build clique adjacency (two k-cliques are adjacent if they share k-1 nodes)
        Map<Integer, Set<Integer>> cliqueGraph = buildCliqueGraph(kCliques, k);

        // Step 4: Find connected components in clique graph
        communities = findConnectedComponents(cliqueGraph, kCliques);

        resultsComputed = true;
    }

    /**
     * Bron-Kerbosch algorithm to find all maximal cliques
     * This is a recursive backtracking algorithm
     */
    private void findAllMaximalCliques() {
        allCliques.clear();
        Set<Integer> R = new HashSet<>();  // Current clique being built
        Set<Integer> P = new HashSet<>(graph.nodes());  // Candidates
        Set<Integer> X = new HashSet<>();  // Already processed

        bronKerbosch(R, P, X);
    }

    /**
     * Bron-Kerbosch recursive algorithm
     * R: current clique
     * P: candidate nodes
     * X: excluded nodes
     */
    private void bronKerbosch(Set<Integer> R, Set<Integer> P, Set<Integer> X) {
        if (P.isEmpty() && X.isEmpty()) {
            // Found maximal clique
            if (R.size() >= 2) {  // Only consider cliques of size >= 2
                allCliques.add(new HashSet<>(R));
            }
            return;
        }

        // Choose pivot to reduce branching
        Set<Integer> pivotCandidates = new HashSet<>(P);
        pivotCandidates.addAll(X);

        int pivot = -1;
        int maxNeighbors = -1;
        for (int v : pivotCandidates) {
            int neighborCount = graph.neighbors(v).size();
            if (neighborCount > maxNeighbors) {
                maxNeighbors = neighborCount;
                pivot = v;
            }
        }

        // Process vertices not connected to pivot
        Set<Integer> toProcess = new HashSet<>(P);
        if (pivot != -1) {
            toProcess.removeAll(graph.neighbors(pivot));
        }

        // Create a copy to avoid concurrent modification
        List<Integer> processList = new ArrayList<>(toProcess);

        for (int v : processList) {
            Set<Integer> newR = new HashSet<>(R);
            newR.add(v);

            Set<Integer> neighbors = graph.neighbors(v);
            Set<Integer> newP = new HashSet<>();
            Set<Integer> newX = new HashSet<>();

            for (int u : P) {
                if (neighbors.contains(u)) newP.add(u);
            }
            for (int u : X) {
                if (neighbors.contains(u)) newX.add(u);
            }

            bronKerbosch(newR, newP, newX);

            P.remove(v);
            X.add(v);
        }
    }

    /**
     * Build clique adjacency graph
     * Two k-cliques are adjacent if they share exactly k-1 nodes
     */
    private Map<Integer, Set<Integer>> buildCliqueGraph(List<Set<Integer>> kCliques, int k) {
        Map<Integer, Set<Integer>> cliqueGraph = new HashMap<>();

        for (int i = 0; i < kCliques.size(); i++) {
            cliqueGraph.put(i, new HashSet<>());

            for (int j = i + 1; j < kCliques.size(); j++) {
                Set<Integer> intersection = new HashSet<>(kCliques.get(i));
                intersection.retainAll(kCliques.get(j));

                // Two k-cliques are adjacent if they share k-1 nodes
                if (intersection.size() >= k - 1) {
                    cliqueGraph.get(i).add(j);
                    cliqueGraph.putIfAbsent(j, new HashSet<>());
                    cliqueGraph.get(j).add(i);
                }
            }
        }

        return cliqueGraph;
    }

    /**
     * Find connected components using BFS
     */
    private Map<Integer, Set<Integer>> findConnectedComponents(
            Map<Integer, Set<Integer>> cliqueGraph,
            List<Set<Integer>> kCliques) {

        Map<Integer, Set<Integer>> result = new HashMap<>();
        Set<Integer> visited = new HashSet<>();
        int communityId = 0;

        for (int startClique : cliqueGraph.keySet()) {
            if (visited.contains(startClique)) continue;

            // BFS to find connected component
            Set<Integer> component = new HashSet<>();
            Queue<Integer> queue = new LinkedList<>();
            queue.add(startClique);
            visited.add(startClique);

            while (!queue.isEmpty()) {
                int current = queue.poll();
                component.addAll(kCliques.get(current));

                for (int neighbor : cliqueGraph.get(current)) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }

            if (!component.isEmpty()) {
                result.put(communityId++, component);
            }
        }

        return result;
    }

    /**
     * Find all cliques of size >= k (for visualization)
     */
    public List<Set<Integer>> findAllCliques(int k) {
        findAllMaximalCliques();
        List<Set<Integer>> result = new ArrayList<>();
        for (Set<Integer> clique : allCliques) {
            if (clique.size() >= k) {
                result.add(clique);
            }
        }
        return result;
    }

    /**
     * Get detected communities
     */
    public Map<Integer, Set<Integer>> getCommunities() {
        return communities;
    }

    /**
     * Check if results have been computed
     */
    public boolean hasResults() {
        return resultsComputed;
    }
}