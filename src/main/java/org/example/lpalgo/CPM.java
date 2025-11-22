package org.example.lpalgo;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;

/**
 * Clique Percolation Method (CPM) for Community Detection
 *
 * Algorithm Overview:
 * 1. Find all k-cliques (fully connected subgraphs of size k)
 * 2. Build clique graph: connect k-cliques sharing (k-1) nodes
 * 3. Communities = connected components in clique graph
 * 4. Key advantage: Detects OVERLAPPING communities
 *
 * Time Complexity: Exponential worst case (NP-hard)
 * Practical: Fast on sparse real-world networks
 *
 * Difference from LPA:
 * - LPA: Fast, non-overlapping, fuzzy boundaries
 * - CPM: Slower, overlapping allowed, rigid structure (complete subgraphs)
 */
public class CPM extends Application {

    private Graph graph;
    private CliquePercolation cpm;

    private Canvas canvas;
    private GraphicsContext gc;
    private Label statsLabel;
    private Label kValueLabel;
    private Slider kSlider;

    private Map<Integer, Point> nodePositions;

    // Colors for communities (using transparency for overlaps)
    private static final Color[] COLORS = {
            Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.PURPLE,
            Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.PINK, Color.BROWN
    };

    private int currentK = 3;

    @Override
    public void start(Stage primaryStage) {
        // Create the canvas first
        canvas = new Canvas(800, 600);
        gc = canvas.getGraphicsContext2D();

        // Initialize the graph (no need to calculate positions here)
        initializeSampleGraph();

        // Calculate node positions now that canvas exists
        calculateNodePositions();

        //  Create UI layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setCenter(canvas);

        VBox controlPanel = createControlPanel();
        root.setRight(controlPanel);

        drawGraph();  // initial draw

        Scene scene = new Scene(root, 1000, 650);
        primaryStage.setTitle("Clique Percolation Method - Overlapping Community Detection");
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

        // K-value slider
        Label kLabel = new Label("Clique Size (k):");
        kLabel.setStyle("-fx-font-size: 12px;");

        kSlider = new Slider(3, 5, 3);
        kSlider.setMajorTickUnit(1);
        kSlider.setMinorTickCount(0);
        kSlider.setSnapToTicks(true);
        kSlider.setShowTickLabels(true);
        kSlider.setShowTickMarks(true);

        kValueLabel = new Label("k = 3");
        kValueLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        kSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentK = newVal.intValue();
            kValueLabel.setText("k = " + currentK);
        });

        statsLabel = new Label();
        statsLabel.setStyle("-fx-font-size: 11px;");
        statsLabel.setWrapText(true);

        Button runButton = new Button("Find Communities");
        runButton.setOnAction(e -> runCPM());
        styleButton(runButton);

        Button showCliquesButton = new Button("Show All Cliques");
        showCliquesButton.setOnAction(e -> showCliques());
        styleButton(showCliquesButton);

        Button resetButton = new Button("Reset");
        resetButton.setOnAction(e -> resetGraph());
        styleButton(resetButton);

        Button newGraphButton = new Button("New Graph");
        newGraphButton.setOnAction(e -> generateNewGraph());
        styleButton(newGraphButton);

        Label infoLabel = new Label("\nCPM Features:\n" +
                "• Finds k-cliques (complete subgraphs)\n" +
                "• Nodes can be in multiple communities\n" +
                "• Rigid structure: all members connected\n" +
                "• Higher k = smaller, denser communities");
        infoLabel.setStyle("-fx-font-size: 9px;");
        infoLabel.setWrapText(true);

        panel.getChildren().addAll(
                titleLabel,
                kLabel,
                kSlider,
                kValueLabel,
                statsLabel,
                runButton,
                showCliquesButton,
                resetButton,
                newGraphButton,
                infoLabel
        );

        return panel;
    }

    private void styleButton(Button button) {
        button.setStyle("-fx-font-size: 11px;");
        button.setPrefWidth(160);
    }

    private void initializeSampleGraph() {
        graph = new Graph();

        // Create a graph with overlapping community structure
        // Community 1: 0,1,2,3,4 (triangle-based)
        graph.addEdge(0, 1);
        graph.addEdge(0, 2);
        graph.addEdge(1, 2);
        graph.addEdge(1, 3);
        graph.addEdge(2, 3);
        graph.addEdge(3, 4);
        graph.addEdge(2, 4);

        // Community 2: 3,4,5,6,7 (overlaps with Community 1 via nodes 3,4)
        graph.addEdge(3, 5);
        graph.addEdge(4, 5);
        graph.addEdge(5, 6);
        graph.addEdge(5, 7);
        graph.addEdge(6, 7);

        // Community 3: 7,8,9,10
        graph.addEdge(7, 8);
        graph.addEdge(7, 9);
        graph.addEdge(8, 9);
        graph.addEdge(8, 10);
        graph.addEdge(9, 10);

        // Add more triangles for richer structure
        graph.addEdge(0, 4);
        graph.addEdge(1, 4);

        cpm = new CliquePercolation(graph);
        calculateNodePositions();
    }

    private void calculateNodePositions() {
        nodePositions = new HashMap<>();
        List<Integer> nodes = new ArrayList<>(graph.nodes());
        Collections.sort(nodes);

        int n = nodes.size();
        double centerX = canvas.getWidth() / 2;
        double centerY = canvas.getHeight() / 2;
        double radius = Math.min(centerX, centerY) - 60;

        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n - Math.PI / 2;
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
        gc.setLineWidth(2);
        for (int node : graph.nodes()) {
            Point p1 = nodePositions.get(node);
            for (int neighbor : graph.neighbors(node)) {
                if (node < neighbor) {
                    Point p2 = nodePositions.get(neighbor);
                    gc.strokeLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }

        // Draw community memberships (if computed)
        if (cpm.hasResults()) {
            drawCommunities();
        }

        // Draw nodes
        for (int node : graph.nodes()) {
            Point p = nodePositions.get(node);

            // Node circle
            gc.setFill(Color.WHITE);
            gc.fillOval(p.x - 18, p.y - 18, 36, 36);

            // Node border
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2);
            gc.strokeOval(p.x - 18, p.y - 18, 36, 36);

            // Node ID
            gc.setFill(Color.BLACK);
            gc.setFont(javafx.scene.text.Font.font("Arial", 14));
            String text = String.valueOf(node);
            double textWidth = text.length() * 7;
            gc.fillText(text, p.x - textWidth / 2, p.y + 5);
        }
    }

    private void drawCommunities() {
        Map<Integer, Set<Integer>> communities = cpm.getCommunities();

        int commIndex = 0;
        for (Set<Integer> community : communities.values()) {
            if (community.size() < 2) continue;

            Color color = COLORS[commIndex % COLORS.length];

            // Draw semi-transparent polygon around community
            List<Point> points = new ArrayList<>();
            for (int node : community) {
                points.add(nodePositions.get(node));
            }

            // Calculate convex hull for cleaner visualization
            List<Point> hull = convexHull(points);

            // Draw filled polygon with transparency
            gc.setFill(new Color(color.getRed(), color.getGreen(),
                    color.getBlue(), 0.15));
            double[] xPoints = new double[hull.size()];
            double[] yPoints = new double[hull.size()];

            for (int i = 0; i < hull.size(); i++) {
                // Expand outward from center
                Point center = getCenter(hull);
                Point p = hull.get(i);
                double dx = p.x - center.x;
                double dy = p.y - center.y;
                double len = Math.sqrt(dx * dx + dy * dy);
                xPoints[i] = p.x + (dx / len) * 25;
                yPoints[i] = p.y + (dy / len) * 25;
            }

            gc.fillPolygon(xPoints, yPoints, hull.size());

            // Draw border
            gc.setStroke(color);
            gc.setLineWidth(3);
            gc.strokePolygon(xPoints, yPoints, hull.size());

            commIndex++;
        }
    }

    private Point getCenter(List<Point> points) {
        double sumX = 0, sumY = 0;
        for (Point p : points) {
            sumX += p.x;
            sumY += p.y;
        }
        return new Point(sumX / points.size(), sumY / points.size());
    }

    private List<Point> convexHull(List<Point> points) {
        if (points.size() < 3) return new ArrayList<>(points);

        List<Point> sorted = new ArrayList<>(points);
        sorted.sort((a, b) -> {
            int cmp = Double.compare(a.x, b.x);
            return cmp != 0 ? cmp : Double.compare(a.y, b.y);
        });

        List<Point> lower = new ArrayList<>();
        for (Point p : sorted) {
            while (lower.size() >= 2 &&
                    cross(lower.get(lower.size() - 2),
                            lower.get(lower.size() - 1), p) <= 0) {
                lower.remove(lower.size() - 1);
            }
            lower.add(p);
        }

        List<Point> upper = new ArrayList<>();
        for (int i = sorted.size() - 1; i >= 0; i--) {
            Point p = sorted.get(i);
            while (upper.size() >= 2 &&
                    cross(upper.get(upper.size() - 2),
                            upper.get(upper.size() - 1), p) <= 0) {
                upper.remove(upper.size() - 1);
            }
            upper.add(p);
        }

        lower.remove(lower.size() - 1);
        upper.remove(upper.size() - 1);
        lower.addAll(upper);
        return lower;
    }

    private double cross(Point o, Point a, Point b) {
        return (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x);
    }

    private void runCPM() {
        cpm.findCommunities(currentK);
        drawGraph();
        updateStats();
    }

    private void showCliques() {
        List<Set<Integer>> cliques = cpm.findAllCliques(currentK);
        drawGraph();

        StringBuilder stats = new StringBuilder();
        stats.append("Found ").append(cliques.size())
                .append(" cliques of size ≥ ").append(currentK).append("\n\n");

        for (int i = 0; i < Math.min(cliques.size(), 10); i++) {
            stats.append("Clique ").append(i + 1).append(": ")
                    .append(cliques.get(i)).append("\n");
        }

        if (cliques.size() > 10) {
            stats.append("... and ").append(cliques.size() - 10).append(" more");
        }

        statsLabel.setText(stats.toString());
    }

    private void resetGraph() {
        cpm = new CliquePercolation(graph);
        drawGraph();
        statsLabel.setText("");
    }

    private void generateNewGraph() {
        graph = new Graph();
        Random rand = new Random();

        int numNodes = 10 + rand.nextInt(8);

        // Create 2-3 overlapping communities
        int comm1Size = 4 + rand.nextInt(3);
        int comm2Size = 4 + rand.nextInt(3);

        // Community 1
        for (int i = 0; i < comm1Size; i++) {
            for (int j = i + 1; j < comm1Size; j++) {
                if (rand.nextDouble() < 0.7) {
                    graph.addEdge(i, j);
                }
            }
        }

        // Community 2 (overlaps with community 1)
        int overlap = 2;
        int start = comm1Size - overlap;
        for (int i = start; i < start + comm2Size; i++) {
            for (int j = i + 1; j < start + comm2Size; j++) {
                if (rand.nextDouble() < 0.7) {
                    graph.addEdge(i, j);
                }
            }
        }

        // Add isolated nodes
        for (int i = start + comm2Size; i < numNodes; i++) {
            graph.addNode(i);
            if (rand.nextDouble() < 0.5 && i > 0) {
                graph.addEdge(i, rand.nextInt(i));
            }
        }

        cpm = new CliquePercolation(graph);
        calculateNodePositions();
        drawGraph();
        statsLabel.setText("");
    }

    private void updateStats() {
        Map<Integer, Set<Integer>> communities = cpm.getCommunities();

        StringBuilder stats = new StringBuilder();
        stats.append("k = ").append(currentK).append("\n");
        stats.append("Communities: ").append(communities.size()).append("\n\n");

        // Check for overlapping nodes
        Map<Integer, Integer> nodeMembership = new HashMap<>();
        for (Set<Integer> comm : communities.values()) {
            for (int node : comm) {
                nodeMembership.put(node, nodeMembership.getOrDefault(node, 0) + 1);
            }
        }

        long overlapping = nodeMembership.values().stream()
                .filter(count -> count > 1).count();

        stats.append("Overlapping nodes: ").append(overlapping).append("\n\n");

        stats.append("Community Sizes:\n");
        int idx = 1;
        for (Set<Integer> comm : communities.values()) {
            stats.append("C").append(idx++).append(": ")
                    .append(comm.size()).append(" nodes\n");
        }

        statsLabel.setText(stats.toString());
    }

    public static void main(String[] args) {
        launch(args);
    }
}

/**
 * Graph class - same as before
 */


/**
 * Clique Percolation Method Implementation
 * Uses Bron-Kerbosch algorithm to find all maximal cliques
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

        for (int v : toProcess) {
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

    public Map<Integer, Set<Integer>> getCommunities() {
        return communities;
    }

    public boolean hasResults() {
        return resultsComputed;
    }
}

