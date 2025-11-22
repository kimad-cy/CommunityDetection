package org.example.lpalgo;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;

/**
 * Label Propagation Algorithm (LPA) for Community Detection
 * Using weighted Graph structure with adjacency map
 *
 * Algorithm Overview:
 * 1. Initialize: Each node gets a unique label (community ID)
 * 2. Iterate: Each node adopts the most frequent label among its neighbors
 * 3. Converge: Stop when labels stabilize or max iterations reached
 *
 * Time Complexity: O(m) per iteration (m = number of edges)
 * Space Complexity: O(n + m) (n = nodes, m = edges)
 */
public class LPAVisualizer extends Application {

    // Graph representation
    private Graph graph;
    private LabelPropagation lpa;

    // Visualization components
    private Canvas canvas;
    private GraphicsContext gc;
    private Label statsLabel;
    private Label iterationLabel;

    // Node positions for visualization
    private Map<Integer, Point> nodePositions;

    // Colors for communities
    private static final Color[] COLORS = {
            Color.web("#e6194b"), // Red
            Color.web("#3cb44b"), // Green
            Color.web("#ffe119"), // Yellow
            Color.web("#0082c8"), // Blue
            Color.web("#f58231"), // Orange
            Color.web("#911eb4"), // Purple
            Color.web("#46f0f0"), // Cyan
            Color.web("#f032e6"), // Magenta
            Color.web("#d2f53c"), // Lime
            Color.web("#fabebe"), // Pink
            Color.web("#008080"), // Teal
            Color.web("#e6beff"), // Lavender
            Color.web("#aa6e28"), // Brown
            Color.web("#fffac8"), // Cream
            Color.web("#800000"), // Maroon
            Color.web("#aaffc3"), // Mint
            Color.web("#808000"), // Olive
            Color.web("#ffd8b1")  // Peach
    };


    @Override
    public void start(Stage primaryStage) {
        // ROOT LAYOUT
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // 1. CREATE CANVAS FIRST
        canvas = new Canvas(800, 600);
        gc = canvas.getGraphicsContext2D();
        root.setCenter(canvas);

        // 2. INITIALIZE GRAPH
        initializeSampleGraph();

        // 3. CREATE CONTROL PANEL
        VBox controlPanel = createControlPanel();
        root.setRight(controlPanel);

        // 4. DRAW INITIAL GRAPH
        drawGraph();

        // 5. SHOW STAGE
        Scene scene = new Scene(root, 1000, 650);
        primaryStage.setTitle("Label Propagation Algorithm - Community Detection");
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    private VBox createControlPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(10));
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc;");
        panel.setPrefWidth(180);

        Label titleLabel = new Label("Controls");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        iterationLabel = new Label("Iteration: 0");
        iterationLabel.setStyle("-fx-font-size: 14px;");

        statsLabel = new Label("Communities: " + graph.nodes().size());
        statsLabel.setStyle("-fx-font-size: 12px;");
        statsLabel.setWrapText(true);

        Button stepButton = new Button("Run One Step");
        stepButton.setOnAction(e -> runOneStep());
        styleButton(stepButton);

        Button runButton = new Button("Run to Convergence");
        runButton.setOnAction(e -> runToConvergence());
        styleButton(runButton);

        Button resetButton = new Button("Reset Graph");
        resetButton.setOnAction(e -> resetGraph());
        styleButton(resetButton);

        Button newGraphButton = new Button("Generate New Graph");
        newGraphButton.setOnAction(e -> generateNewGraph());
        styleButton(newGraphButton);

        Label infoLabel = new Label("\nHow it works:\n" +
                "• Each node starts with unique label\n" +
                "• Nodes adopt most common neighbor label\n" +
                "• Colors show detected communities");
        infoLabel.setStyle("-fx-font-size: 10px;");
        infoLabel.setWrapText(true);

        panel.getChildren().addAll(
                titleLabel,
                iterationLabel,
                statsLabel,
                stepButton,
                runButton,
                resetButton,
                newGraphButton,
                infoLabel
        );

        return panel;
    }

    private void styleButton(Button button) {
        button.setStyle("-fx-font-size: 12px;");
        button.setPrefWidth(160);
    }

    private void initializeSampleGraph() {
        // Create a graph with clear community structure
        graph = new Graph();

        // Community 1: nodes 0-4 (densely connected)
        graph.addEdge(0, 1);
        graph.addEdge(0, 2);
        graph.addEdge(1, 2);
        graph.addEdge(1, 3);
        graph.addEdge(2, 3);
        graph.addEdge(3, 4);
        graph.addEdge(2, 4);

        // Community 2: nodes 5-9 (densely connected)
        graph.addEdge(5, 6);
        graph.addEdge(5, 7);
        graph.addEdge(6, 7);
        graph.addEdge(6, 8);
        graph.addEdge(7, 8);
        graph.addEdge(8, 9);
        graph.addEdge(7, 9);

        // Community 3: nodes 10-14 (densely connected)
        graph.addEdge(10, 11);
        graph.addEdge(10, 12);
        graph.addEdge(11, 12);
        graph.addEdge(11, 13);
        graph.addEdge(12, 13);
        graph.addEdge(13, 14);
        graph.addEdge(12, 14);

        // Bridge edges between communities (weak connections)
        graph.addEdge(4, 5);
        graph.addEdge(9, 10);

        lpa = new LabelPropagation(graph);
        calculateNodePositions();
    }

    private void calculateNodePositions() {
        nodePositions = new HashMap<>();
        List<Integer> nodes = new ArrayList<>(graph.nodes());
        Collections.sort(nodes);

        int n = nodes.size();
        double centerX = canvas.getWidth() / 2;
        double centerY = canvas.getHeight() / 2;
        double radius = Math.min(centerX, centerY) - 50;

        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n;
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            nodePositions.put(nodes.get(i), new Point(x, y));
        }
    }

    private void drawGraph() {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Draw edges
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(1);
        for (int node : graph.nodes()) {
            Point p1 = nodePositions.get(node);
            for (int neighbor : graph.neighbors(node)) {
                if (node < neighbor) { // Draw each edge once
                    Point p2 = nodePositions.get(neighbor);
                    gc.strokeLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }

        // Draw nodes
        for (int node : graph.nodes()) {
            Point p = nodePositions.get(node);
            int label = lpa.getLabel(node);

            // Node circle
            gc.setFill(COLORS[label % COLORS.length]);
            gc.fillOval(p.x - 20, p.y - 20, 40, 40);

            // Node border
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2);
            gc.strokeOval(p.x - 20, p.y - 20, 40, 40);

            // Node ID
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", 14));
            String text = String.valueOf(node);
            gc.fillText(text, p.x - 5, p.y + 5);
        }
    }

    private void runOneStep() {
        boolean changed = lpa.iterate();
        drawGraph();
        updateStats();

        if (!changed) {
            statsLabel.setText(statsLabel.getText() + "\n\nConverged!");
        }
    }

    private void runToConvergence() {
        int maxIterations = 100;
        for (int i = 0; i < maxIterations; i++) {
            boolean changed = lpa.iterate();
            if (!changed) {
                break;
            }
        }
        drawGraph();
        updateStats();
    }

    private void resetGraph() {
        lpa.reset();
        drawGraph();
        updateStats();
    }

    private void generateNewGraph() {
        graph = new Graph();
        Random rand = new Random();

        int numCommunities = 3 + rand.nextInt(2); // 3 or 4 communities
        int nodesPerCommunity = 4 + rand.nextInt(3); // 4-6 nodes per community

        // Create communities with dense internal connections
        for (int c = 0; c < numCommunities; c++) {
            int start = c * nodesPerCommunity;
            int end = start + nodesPerCommunity;

            // Dense connections within community
            for (int i = start; i < end; i++) {
                for (int j = i + 1; j < end; j++) {
                    if (rand.nextDouble() < 0.7) { // 70% edge probability
                        graph.addEdge(i, j);
                    }
                }
            }
        }

        // Add sparse inter-community edges
        for (int c = 0; c < numCommunities - 1; c++) {
            int node1 = c * nodesPerCommunity + rand.nextInt(nodesPerCommunity);
            int node2 = (c + 1) * nodesPerCommunity + rand.nextInt(nodesPerCommunity);
            graph.addEdge(node1, node2);
        }

        lpa = new LabelPropagation(graph);
        calculateNodePositions();
        drawGraph();
        updateStats();
    }

    private void updateStats() {
        iterationLabel.setText("Iteration: " + lpa.getIteration());

        Map<Integer, Integer> communitySizes = new HashMap<>();
        for (int node : graph.nodes()) {
            int label = lpa.getLabel(node);
            communitySizes.put(label, communitySizes.getOrDefault(label, 0) + 1);
        }

        StringBuilder stats = new StringBuilder();
        stats.append("Communities: ").append(communitySizes.size()).append("\n\n");
        stats.append("Community Sizes:\n");

        List<Map.Entry<Integer, Integer>> sortedCommunities =
                new ArrayList<>(communitySizes.entrySet());
        sortedCommunities.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (Map.Entry<Integer, Integer> entry : sortedCommunities) {
            stats.append("  C").append(entry.getKey())
                    .append(": ").append(entry.getValue()).append(" nodes\n");
        }

        statsLabel.setText(stats.toString());
    }

    public static void main(String[] args) {
        launch(args);
    }
}

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