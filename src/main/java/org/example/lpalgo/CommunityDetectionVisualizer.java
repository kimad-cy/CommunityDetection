package org.example.lpalgo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;

/**
 * Unified Community Detection Visualizer
 * Supports: Louvain, Girvan-Newman, Label Propagation, and Clique Percolation
 */
public class CommunityDetectionVisualizer extends Application {

    private Graph graph;
    private Canvas canvas;
    private GraphicsContext gc;

    // Algorithm instances
    private LouvainAlgorithm louvainAlgo;
    private GirvanNewmanAlgorithm girvanNewmanAlgo;
    private LabelPropagation lpaAlgo;
    private CliquePercolation cpmAlgo;

    // UI Components
    private ComboBox<String> algorithmSelector;
    private Label statsLabel;
    private Label metricLabel;
    private Slider kSlider;
    private Label kValueLabel;
    private VBox kPanel;
    private Button showBestButton;

    private Map<Integer, Point> nodePositions;
    private boolean isRunning = false;
    private boolean showBest = false;
    private String currentAlgorithm = "Louvain";
    private int currentK = 3;

    private static final Color[] COLORS = {
            Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.PURPLE,
            Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.PINK, Color.BROWN,
            Color.web("#e6194b"), Color.web("#3cb44b"), Color.web("#ffe119")
    };

    @Override
    public void start(Stage primaryStage) {
        canvas = new Canvas(800, 600);
        gc = canvas.getGraphicsContext2D();

        initializeSampleGraph();
        calculateNodePositions();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setCenter(canvas);

        VBox controlPanel = createControlPanel();
        root.setRight(controlPanel);

        drawGraph();

        Scene scene = new Scene(root, 1000, 650);
        primaryStage.setTitle("Community Detection Visualizer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(10));
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc;");
        panel.setPrefWidth(200);

        Label titleLabel = new Label("Algorithm Selector");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        algorithmSelector = new ComboBox<>();
        algorithmSelector.getItems().addAll("Louvain", "Girvan-Newman", "Label Propagation", "Clique Percolation");
        algorithmSelector.setValue("Louvain");
        algorithmSelector.setOnAction(e -> switchAlgorithm());
        algorithmSelector.setPrefWidth(180);

        statsLabel = new Label("Ready to start");
        statsLabel.setStyle("-fx-font-size: 11px;");
        statsLabel.setWrapText(true);
        statsLabel.setMinHeight(80);

        metricLabel = new Label("Modularity: 0.0000");
        metricLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        // K-value slider for CPM
        kPanel = new VBox(5);
        Label kLabel = new Label("Clique Size (k):");
        kLabel.setStyle("-fx-font-size: 11px;");

        kSlider = new Slider(3, 5, 3);
        kSlider.setMajorTickUnit(1);
        kSlider.setMinorTickCount(0);
        kSlider.setSnapToTicks(true);
        kSlider.setShowTickLabels(true);
        kSlider.setShowTickMarks(true);
        kSlider.setPrefWidth(180);

        kValueLabel = new Label("k = 3");
        kValueLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        kSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentK = newVal.intValue();
            kValueLabel.setText("k = " + currentK);
        });

        kPanel.getChildren().addAll(kLabel, kSlider, kValueLabel);
        kPanel.setVisible(false);

        Button stepButton = new Button("Next Step");
        stepButton.setOnAction(e -> performStep());
        styleButton(stepButton);

        Button runButton = new Button("Run All");
        runButton.setOnAction(e -> runAll());
        styleButton(runButton);

        showBestButton = new Button("Show Best Partition");
        showBestButton.setOnAction(e -> toggleBestPartition());
        showBestButton.setVisible(false);
        styleButton(showBestButton);

        Button resetButton = new Button("Reset");
        resetButton.setOnAction(e -> reset());
        styleButton(resetButton);

        Button newGraphButton = new Button("New Graph");
        newGraphButton.setOnAction(e -> generateNewGraph());
        styleButton(newGraphButton);

        Label infoLabel = new Label();
        infoLabel.setStyle("-fx-font-size: 9px;");
        infoLabel.setWrapText(true);
        infoLabel.setText(getAlgorithmInfo("Louvain"));

        panel.getChildren().addAll(
                titleLabel,
                algorithmSelector,
                new Separator(),
                metricLabel,
                statsLabel,
                kPanel,
                stepButton,
                runButton,
                showBestButton,
                resetButton,
                newGraphButton,
                new Separator(),
                infoLabel
        );

        // Store info label reference for updates
        panel.setUserData(infoLabel);

        return panel;
    }

    private void styleButton(Button button) {
        button.setStyle("-fx-font-size: 11px;");
        button.setPrefWidth(180);
    }

    private String getAlgorithmInfo(String algorithm) {
        switch (algorithm) {
            case "Louvain":
                return "\nLouvain:\n• Fast modularity optimization\n• Hierarchical communities\n• Non-overlapping\n• O(n log n) complexity";
            case "Girvan-Newman":
                return "\nGirvan-Newman:\n• Removes high-betweenness edges\n• Divisive hierarchical\n• Tracks max modularity\n• O(m²n) complexity";
            case "Label Propagation":
                return "\nLabel Propagation:\n• Nodes adopt neighbor labels\n• Very fast O(m)\n• Non-deterministic\n• Non-overlapping";
            case "Clique Percolation":
                return "\nClique Percolation:\n• Finds k-cliques\n• Overlapping communities\n• Rigid structure\n• NP-hard complexity";
            default:
                return "";
        }
    }

    private void switchAlgorithm() {
        if (isRunning) return;

        currentAlgorithm = algorithmSelector.getValue();
        showBest = false;

        // Update UI visibility
        kPanel.setVisible(currentAlgorithm.equals("Clique Percolation"));
        showBestButton.setVisible(currentAlgorithm.equals("Girvan-Newman"));

        // Update info label
        VBox panel = (VBox) algorithmSelector.getParent();
        Label infoLabel = (Label) panel.getUserData();
        infoLabel.setText(getAlgorithmInfo(currentAlgorithm));

        reset();
    }

    private void initializeSampleGraph() {
        graph = new Graph();

        // Create a graph with overlapping community structure (like standalone CPM)
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

        // Add some additional nodes
        graph.addNode(11);
        graph.addNode(12);
        graph.addEdge(11, 0);
        graph.addEdge(12, 10);

        initializeAlgorithms();
    }

    private void initializeAlgorithms() {
        louvainAlgo = new LouvainAlgorithm(graph);
        girvanNewmanAlgo = new GirvanNewmanAlgorithm(graph);
        lpaAlgo = new LabelPropagation(graph);
        cpmAlgo = new CliquePercolation(graph);
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

    private void performStep() {
        if (isRunning) return;

        switch (currentAlgorithm) {
            case "Louvain":
                performLouvainStep();
                break;
            case "Girvan-Newman":
                performGirvanNewmanStep();
                break;
            case "Label Propagation":
                performLPAStep();
                break;
            case "Clique Percolation":
                performCPMStep();
                break;
        }

        drawGraph();
    }

    private void performLouvainStep() {
        boolean changed = louvainAlgo.performOnePass();
        if (changed) {
            statsLabel.setText("Phase " + louvainAlgo.getPhase() + "\nCommunities: " +
                    louvainAlgo.getNumCommunities());
        } else {
            statsLabel.setText("Converged!\nCommunities: " + louvainAlgo.getNumCommunities());
        }
        metricLabel.setText(String.format("Modularity: %.4f", louvainAlgo.computeModularity()));
    }

    private void performGirvanNewmanStep() {
        showBest = false;
        boolean hasMore = girvanNewmanAlgo.performOneStep();

        if (hasMore) {
            statsLabel.setText("Step: " + girvanNewmanAlgo.getCurrentStep() +
                    "\nEdges: " + girvanNewmanAlgo.getRemainingEdges() +
                    "\nCommunities: " + girvanNewmanAlgo.getCurrentCommunities().size());
        } else {
            statsLabel.setText("Complete!\nBest: " + girvanNewmanAlgo.getBestCommunities().size() +
                    " communities");
        }
        metricLabel.setText(String.format("Q: %.4f\nMax: %.4f",
                girvanNewmanAlgo.getCurrentModularity(), girvanNewmanAlgo.getMaxModularity()));
    }

    private void performLPAStep() {
        boolean changed = lpaAlgo.iterate();
        statsLabel.setText("Iteration: " + lpaAlgo.getIteration() +
                "\nCommunities: " + lpaAlgo.getCommunities().size() +
                (changed ? "" : "\nConverged!"));
        metricLabel.setText("LPA (No modularity)");
    }

    private void performCPMStep() {
        // For CPM, just run the algorithm completely in one step
        cpmAlgo.findCommunities(currentK);

        Map<Integer, Set<Integer>> communities = cpmAlgo.getCommunities();

        // Count overlapping nodes
        Map<Integer, Integer> nodeMembership = new HashMap<>();
        for (Set<Integer> comm : communities.values()) {
            for (int node : comm) {
                nodeMembership.put(node, nodeMembership.getOrDefault(node, 0) + 1);
            }
        }
        long overlapping = nodeMembership.values().stream().filter(c -> c > 1).count();

        // Count total unique nodes in communities
        Set<Integer> nodesInCommunities = new HashSet<>();
        for (Set<Integer> comm : communities.values()) {
            nodesInCommunities.addAll(comm);
        }

        statsLabel.setText("✓ Complete!\nk = " + currentK +
                "\nCommunities: " + communities.size() +
                "\nNodes in communities: " + nodesInCommunities.size() +
                "\nOverlapping nodes: " + overlapping);

        metricLabel.setText("CPM - Final");
    }

    private void runAll() {
        if (isRunning) return;
        isRunning = true;

        new Thread(() -> {
            if (currentAlgorithm.equals("Louvain")) {
                runLouvainAll();
            } else if (currentAlgorithm.equals("Girvan-Newman")) {
                runGirvanNewmanAll();
            } else if (currentAlgorithm.equals("Label Propagation")) {
                runLPAAll();
            } else if (currentAlgorithm.equals("Clique Percolation")) {
                runCPMAll();
            }
        }).start();
    }

    private void runLouvainAll() {
        while (louvainAlgo.performOnePass()) {
            Platform.runLater(() -> {
                statsLabel.setText("Running...\nCommunities: " + louvainAlgo.getNumCommunities());
                metricLabel.setText(String.format("Modularity: %.4f", louvainAlgo.computeModularity()));
                drawGraph();
            });
            try { Thread.sleep(500); } catch (InterruptedException e) {}
        }

        Platform.runLater(() -> {
            statsLabel.setText("Converged!\nCommunities: " + louvainAlgo.getNumCommunities());
            metricLabel.setText(String.format("Final Q: %.4f", louvainAlgo.computeModularity()));
            drawGraph();
            isRunning = false;
        });
    }

    private void runGirvanNewmanAll() {
        while (girvanNewmanAlgo.performOneStep()) {
            Platform.runLater(() -> {
                statsLabel.setText("Step: " + girvanNewmanAlgo.getCurrentStep() +
                        "\nCommunities: " + girvanNewmanAlgo.getCurrentCommunities().size());
                metricLabel.setText(String.format("Q: %.4f\nMax: %.4f",
                        girvanNewmanAlgo.getCurrentModularity(), girvanNewmanAlgo.getMaxModularity()));
                drawGraph();
            });
            try { Thread.sleep(300); } catch (InterruptedException e) {}
        }

        Platform.runLater(() -> {
            showBest = true;
            statsLabel.setText("Complete!\nShowing best partition");
            metricLabel.setText(String.format("Max Q: %.4f", girvanNewmanAlgo.getMaxModularity()));
            drawGraph();
            isRunning = false;
        });
    }

    private void runLPAAll() {
        int maxIter = 100;
        for (int i = 0; i < maxIter; i++) {
            boolean changed = lpaAlgo.iterate();
            int currentIter = lpaAlgo.getIteration();

            Platform.runLater(() -> {
                statsLabel.setText("Iteration: " + currentIter +
                        "\nCommunities: " + lpaAlgo.getCommunities().size());
                drawGraph();
            });

            try { Thread.sleep(300); } catch (InterruptedException e) {}

            if (!changed) break;
        }

        Platform.runLater(() -> {
            statsLabel.setText("Converged!\nCommunities: " + lpaAlgo.getCommunities().size());
            isRunning = false;
        });
    }

    private void runCPMAll() {
        // For CPM, just run it once since it's not step-by-step
        Platform.runLater(() -> {
            cpmAlgo.findCommunities(currentK);
            Map<Integer, Set<Integer>> communities = cpmAlgo.getCommunities();

            Map<Integer, Integer> nodeMembership = new HashMap<>();
            for (Set<Integer> comm : communities.values()) {
                for (int node : comm) {
                    nodeMembership.put(node, nodeMembership.getOrDefault(node, 0) + 1);
                }
            }
            long overlapping = nodeMembership.values().stream().filter(c -> c > 1).count();

            // Count total unique nodes in communities
            Set<Integer> nodesInCommunities = new HashSet<>();
            for (Set<Integer> comm : communities.values()) {
                nodesInCommunities.addAll(comm);
            }

            statsLabel.setText("✓ Complete!\nk = " + currentK +
                    "\nCommunities: " + communities.size() +
                    "\nNodes in communities: " + nodesInCommunities.size() +
                    "\nOverlapping nodes: " + overlapping);
            metricLabel.setText("CPM - Final");
            drawGraph();
            isRunning = false;
        });
    }

    private void toggleBestPartition() {
        showBest = !showBest;
        drawGraph();

        if (showBest) {
            statsLabel.setText("Showing BEST partition\n(max modularity)");
        } else {
            statsLabel.setText("Showing CURRENT state");
        }
    }

    private void reset() {
        if (isRunning) return;

        initializeAlgorithms();
        showBest = false;
        statsLabel.setText("Ready to start");

        switch (currentAlgorithm) {
            case "Louvain":
            case "Girvan-Newman":
                metricLabel.setText("Modularity: 0.0000");
                break;
            case "Label Propagation":
                metricLabel.setText("LPA (No modularity)");
                break;
            case "Clique Percolation":
                metricLabel.setText("CPM (Overlapping)");
                break;
        }

        drawGraph();
    }

    private void generateNewGraph() {
        if (isRunning) return;

        graph = new Graph();
        Random rand = new Random();

        int numNodes = 12 + rand.nextInt(6);
        int numCommunities = 3;
        int nodesPerCommunity = numNodes / numCommunities;

        for (int c = 0; c < numCommunities; c++) {
            int start = c * nodesPerCommunity;
            int end = (c == numCommunities - 1) ? numNodes : (c + 1) * nodesPerCommunity;

            for (int i = start; i < end; i++) {
                for (int j = i + 1; j < end; j++) {
                    if (rand.nextDouble() < 0.7) {
                        graph.addEdge(i, j);
                    }
                }
            }
        }

        for (int c = 0; c < numCommunities - 1; c++) {
            int node1 = c * nodesPerCommunity + rand.nextInt(nodesPerCommunity);
            int node2 = (c + 1) * nodesPerCommunity + rand.nextInt(nodesPerCommunity);
            graph.addEdge(node1, node2);
        }

        calculateNodePositions();
        reset();
    }

    private void drawGraph() {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Get appropriate graph for display
        Graph displayGraph = currentAlgorithm.equals("Girvan-Newman") ?
                girvanNewmanAlgo.getOriginalGraph() : graph;

        // Draw edges
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(2);
        for (int node : displayGraph.nodes()) {
            Point p1 = nodePositions.get(node);
            if (p1 == null) continue;

            for (int neighbor : displayGraph.neighbors(node)) {
                if (node < neighbor) {
                    Point p2 = nodePositions.get(neighbor);
                    if (p2 != null) {
                        gc.strokeLine(p1.x, p1.y, p2.x, p2.y);
                    }
                }
            }
        }

        // Draw communities
        drawCommunities();

        // Draw nodes - exactly like standalone version
        for (int node : displayGraph.nodes()) {
            Point p = nodePositions.get(node);
            if (p == null) continue;

            // Node circle - white fill like standalone
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

    private Map<Integer, Integer> getNodeToCommunityMap() {
        Map<Integer, Integer> map = new HashMap<>();

        switch (currentAlgorithm) {
            case "Louvain":
                return louvainAlgo.getCommunityMap();

            case "Girvan-Newman":
                Map<Integer, Set<Integer>> gnComm = showBest ?
                        girvanNewmanAlgo.getBestCommunities() : girvanNewmanAlgo.getCurrentCommunities();
                for (Map.Entry<Integer, Set<Integer>> entry : gnComm.entrySet()) {
                    for (int node : entry.getValue()) {
                        map.put(node, entry.getKey());
                    }
                }
                return map;

            case "Label Propagation":
                for (int node : graph.nodes()) {
                    map.put(node, lpaAlgo.getLabel(node));
                }
                return map;

            case "Clique Percolation":
                Map<Integer, Set<Integer>> cpmComm = cpmAlgo.getCommunities();
                // For overlapping communities, assign node to its first community for coloring
                for (Map.Entry<Integer, Set<Integer>> entry : cpmComm.entrySet()) {
                    for (int node : entry.getValue()) {
                        if (!map.containsKey(node)) {
                            map.put(node, entry.getKey());
                        }
                    }
                }
                return map;
        }

        return map;
    }

    private void drawCommunities() {
        Map<Integer, Set<Integer>> communitySets = new HashMap<>();

        switch (currentAlgorithm) {
            case "Louvain":
                Map<Integer, Integer> louvainMap = louvainAlgo.getCommunityMap();
                for (Map.Entry<Integer, Integer> entry : louvainMap.entrySet()) {
                    communitySets.computeIfAbsent(entry.getValue(), k -> new HashSet<>()).add(entry.getKey());
                }
                break;

            case "Girvan-Newman":
                communitySets = showBest ? girvanNewmanAlgo.getBestCommunities() :
                        girvanNewmanAlgo.getCurrentCommunities();
                break;

            case "Label Propagation":
                communitySets = lpaAlgo.getCommunities();
                break;

            case "Clique Percolation":
                communitySets = cpmAlgo.getCommunities();
                break;
        }

        for (Map.Entry<Integer, Set<Integer>> entry : communitySets.entrySet()) {
            if (entry.getValue().size() < 2) continue;

            Color color = COLORS[Math.abs(entry.getKey()) % COLORS.length];
            List<Point> points = new ArrayList<>();

            for (int node : entry.getValue()) {
                Point p = nodePositions.get(node);
                if (p != null) points.add(p);
            }

            if (points.size() < 2) continue;

            List<Point> hull = convexHull(points);
            if (hull.isEmpty()) continue;

            Point center = getCenter(hull);
            double[] xPoints = new double[hull.size()];
            double[] yPoints = new double[hull.size()];

            for (int i = 0; i < hull.size(); i++) {
                Point p = hull.get(i);
                double dx = p.x - center.x;
                double dy = p.y - center.y;
                double len = Math.sqrt(dx * dx + dy * dy);
                if (len > 0) {
                    xPoints[i] = p.x + (dx / len) * 25;  // Same expansion as standalone
                    yPoints[i] = p.y + (dy / len) * 25;
                } else {
                    xPoints[i] = p.x;
                    yPoints[i] = p.y;
                }
            }

            // Same transparency as standalone version (0.15)
            gc.setFill(new Color(color.getRed(), color.getGreen(), color.getBlue(), 0.15));
            gc.fillPolygon(xPoints, yPoints, hull.size());

            gc.setStroke(color);
            gc.setLineWidth(3);
            gc.strokePolygon(xPoints, yPoints, hull.size());
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
            while (lower.size() >= 2 && cross(lower.get(lower.size() - 2), lower.get(lower.size() - 1), p) <= 0) {
                lower.remove(lower.size() - 1);
            }
            lower.add(p);
        }

        List<Point> upper = new ArrayList<>();
        for (int i = sorted.size() - 1; i >= 0; i--) {
            Point p = sorted.get(i);
            while (upper.size() >= 2 && cross(upper.get(upper.size() - 2), upper.get(upper.size() - 1), p) <= 0) {
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

    public static void main(String[] args) {
        launch(args);
    }
}