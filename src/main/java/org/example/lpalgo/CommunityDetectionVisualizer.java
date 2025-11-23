package org.example.lpalgo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;

/**
 * Enhanced Unified Community Detection Visualizer
 * Supports: Louvain, Girvan-Newman, Label Propagation, and Clique Percolation
 * With advanced visualization features matching the standalone CPM
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
    private Label cliquesLabel;
    private ListView<String> cliqueListView;

    private Map<Integer, Point> nodePositions;
    private boolean isRunning = false;
    private boolean showBest = false;
    private String currentAlgorithm = "Louvain";
    private int currentK = 3;

    // Enhanced visualization state
    private Set<Integer> highlightedClique = new HashSet<>();
    private int hoveredNode = -1;
    private List<Set<Integer>> discoveredCliques = new ArrayList<>();

    private static final Color[] COLORS = {
            Color.rgb(231, 76, 60), Color.rgb(52, 152, 219), Color.rgb(46, 204, 113),
            Color.rgb(241, 196, 15), Color.rgb(155, 89, 182), Color.rgb(26, 188, 156),
            Color.rgb(230, 126, 34), Color.rgb(52, 73, 94), Color.rgb(255, 107, 129),
            Color.rgb(95, 39, 205), Color.rgb(0, 184, 148), Color.rgb(253, 203, 110)
    };

    @Override
    public void start(Stage primaryStage) {
        canvas = new Canvas(800, 600);
        gc = canvas.getGraphicsContext2D();

        initializeSampleGraph();
        calculateNodePositions();

        // Add mouse interaction
        canvas.setOnMouseMoved(this::handleMouseMove);
        canvas.setOnMouseExited(e -> {
            hoveredNode = -1;
            drawGraph();
        });

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setCenter(canvas);

        VBox controlPanel = createControlPanel();
        root.setRight(controlPanel);

        VBox bottomPanel = createBottomPanel();
        root.setBottom(bottomPanel);

        drawGraph();

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("Enhanced Community Detection Visualizer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(10));
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setStyle("-fx-background-color: #2c3e50; -fx-border-radius: 5;");
        panel.setPrefWidth(220);

        Label titleLabel = new Label("Algorithm Selector");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        algorithmSelector = new ComboBox<>();
        algorithmSelector.getItems().addAll("Louvain", "Girvan-Newman", "Label Propagation", "Clique Percolation");
        algorithmSelector.setValue("Louvain");
        algorithmSelector.setOnAction(e -> switchAlgorithm());
        algorithmSelector.setStyle("-fx-font-size: 12px;");
        algorithmSelector.setPrefWidth(200);

        statsLabel = new Label("Ready to start");
        statsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ecf0f1;");
        statsLabel.setWrapText(true);
        statsLabel.setPrefHeight(80);

        metricLabel = new Label("Modularity: 0.0000");
        metricLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3498db;");

        // K-value slider for CPM
        kPanel = new VBox(5);
        Label kLabel = new Label("Clique Size (k):");
        kLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ecf0f1;");

        kSlider = new Slider(3, 5, 3);
        kSlider.setMajorTickUnit(1);
        kSlider.setMinorTickCount(0);
        kSlider.setSnapToTicks(true);
        kSlider.setShowTickLabels(true);
        kSlider.setShowTickMarks(true);
        kSlider.setPrefWidth(200);

        kValueLabel = new Label("k = 3");
        kValueLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #3498db;");

        kSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentK = newVal.intValue();
            kValueLabel.setText("k = " + currentK);
        });

        kPanel.getChildren().addAll(kLabel, kSlider, kValueLabel);
        kPanel.setVisible(false);

        // Cliques display area
        Label cliquesTitleLabel = new Label("Cliques Found:");
        cliquesTitleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");

        cliquesLabel = new Label();
        cliquesLabel.setStyle("-fx-font-size: 10px; -fx-font-family: monospace; -fx-text-fill: #ecf0f1;");
        cliquesLabel.setWrapText(true);
        cliquesLabel.setPrefHeight(80);
        cliquesLabel.setVisible(false);

        // Action buttons
        Button stepButton = new Button("▶ Next Step");
        stepButton.setOnAction(e -> performStep());
        styleButton(stepButton, "#3498db");

        Button runButton = new Button("⚡ Run All");
        runButton.setOnAction(e -> runAll());
        styleButton(runButton, "#2ecc71");

        showBestButton = new Button("🏆 Show Best");
        showBestButton.setOnAction(e -> toggleBestPartition());
        showBestButton.setVisible(false);
        styleButton(showBestButton, "#9b59b6");

        Button resetButton = new Button("🔄 Reset");
        resetButton.setOnAction(e -> reset());
        styleButton(resetButton, "#e74c3c");

        Button newGraphButton = new Button("✨ New Graph");
        newGraphButton.setOnAction(e -> generateNewGraph());
        styleButton(newGraphButton, "#f39c12");

        Label infoLabel = new Label();
        infoLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #bdc3c7;");
        infoLabel.setWrapText(true);
        infoLabel.setText(getAlgorithmInfo("Louvain"));

        panel.getChildren().addAll(
                titleLabel,
                algorithmSelector,
                new Separator(),
                metricLabel,
                statsLabel,
                kPanel,
                cliquesTitleLabel,
                cliquesLabel,
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

    private VBox createBottomPanel() {
        VBox panel = new VBox(5);
        panel.setPadding(new Insets(5));
        panel.setStyle("-fx-background-color: #ecf0f1;");

        Label listLabel = new Label("Discovered Cliques (click to highlight):");
        listLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        cliqueListView = new ListView<>();
        cliqueListView.setPrefHeight(100);
        cliqueListView.setStyle("-fx-font-size: 11px;");
        cliqueListView.setVisible(false);

        cliqueListView.setOnMouseClicked(e -> {
            String selected = cliqueListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                highlightCliqueFromString(selected);
            }
        });

        panel.getChildren().addAll(listLabel, cliqueListView);
        return panel;
    }

    private void styleButton(Button button, String color) {
        button.setStyle(
                "-fx-font-size: 11px; " +
                        "-fx-background-color: " + color + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 8 15 8 15; " +
                        "-fx-background-radius: 5;"
        );
        button.setPrefWidth(200);
    }

    private void handleMouseMove(MouseEvent e) {
        double mouseX = e.getX();
        double mouseY = e.getY();

        hoveredNode = -1;
        for (Map.Entry<Integer, Point> entry : nodePositions.entrySet()) {
            Point p = entry.getValue();
            double dist = Math.sqrt(Math.pow(mouseX - p.x, 2) + Math.pow(mouseY - p.y, 2));
            if (dist < 20) {
                hoveredNode = entry.getKey();
                break;
            }
        }

        drawGraph();
    }

    private void highlightCliqueFromString(String cliqueStr) {
        highlightedClique.clear();

        // Parse "Clique X: [a, b, c]"
        int startIdx = cliqueStr.indexOf('[');
        int endIdx = cliqueStr.indexOf(']');
        if (startIdx != -1 && endIdx != -1) {
            String nodesStr = cliqueStr.substring(startIdx + 1, endIdx);
            String[] nodes = nodesStr.split(",");
            for (String node : nodes) {
                highlightedClique.add(Integer.parseInt(node.trim()));
            }
        }

        drawGraph();
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
        highlightedClique.clear();

        // Update UI visibility
        boolean isCPM = currentAlgorithm.equals("Clique Percolation");
        kPanel.setVisible(isCPM);
        cliquesLabel.setVisible(isCPM);
        cliqueListView.setVisible(isCPM);
        showBestButton.setVisible(currentAlgorithm.equals("Girvan-Newman"));

        // Update info label
        VBox panel = (VBox) algorithmSelector.getParent();
        Label infoLabel = (Label) panel.getUserData();
        infoLabel.setText(getAlgorithmInfo(currentAlgorithm));

        reset();
    }

    private void initializeSampleGraph() {
        graph = new Graph();

        // Create overlapping triangular structures (like enhanced CPM)
        // Community 1
        graph.addEdge(0, 1);
        graph.addEdge(0, 2);
        graph.addEdge(1, 2);
        graph.addEdge(1, 3);
        graph.addEdge(2, 3);
        graph.addEdge(0, 3);

        // Overlap region
        graph.addEdge(3, 4);
        graph.addEdge(3, 5);
        graph.addEdge(4, 5);

        // Community 2
        graph.addEdge(4, 6);
        graph.addEdge(5, 6);
        graph.addEdge(5, 7);
        graph.addEdge(6, 7);
        graph.addEdge(4, 7);

        // Community 3
        graph.addEdge(7, 8);
        graph.addEdge(7, 9);
        graph.addEdge(8, 9);
        graph.addEdge(8, 10);
        graph.addEdge(9, 10);

        // Additional edges
        graph.addEdge(0, 4);
        graph.addEdge(6, 10);

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
        double radius = Math.min(centerX, centerY) - 80;

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
        // For CPM, run the algorithm completely in one step
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

        // Update cliques display
        updateCliquesDisplay();
    }

    private void updateCliquesDisplay() {
        // Find all cliques of size >= currentK
        List<Set<Integer>> allCliques = cpmAlgo.findAllCliques(currentK);

        StringBuilder cliquesText = new StringBuilder();
        cliquesText.append("Found ").append(allCliques.size())
                .append(" cliques of size ≥ ").append(currentK).append("\n\n");

        for (int i = 0; i < allCliques.size(); i++) {
            List<Integer> cliqueList = new ArrayList<>(allCliques.get(i));
            Collections.sort(cliqueList);
            cliquesText.append("Clique ").append(i + 1).append(": ")
                    .append(cliqueList).append("\n");
        }

        cliquesLabel.setText(cliquesText.toString());

        // Update list view
        cliqueListView.getItems().clear();
        for (int i = 0; i < allCliques.size(); i++) {
            List<Integer> cliqueList = new ArrayList<>(allCliques.get(i));
            Collections.sort(cliqueList);
            cliqueListView.getItems().add("Clique " + (i + 1) + ": " + cliqueList);
        }
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

            Set<Integer> nodesInCommunities = new HashSet<>();
            for (Set<Integer> comm : communities.values()) {
                nodesInCommunities.addAll(comm);
            }

            statsLabel.setText("✓ Complete!\nk = " + currentK +
                    "\nCommunities: " + communities.size() +
                    "\nNodes in communities: " + nodesInCommunities.size() +
                    "\nOverlapping nodes: " + overlapping);
            metricLabel.setText("CPM - Final");

            updateCliquesDisplay();
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
        highlightedClique.clear();
        statsLabel.setText("Ready to start");
        cliquesLabel.setText("");
        cliqueListView.getItems().clear();

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

        int numCommunities = 2 + rand.nextInt(2);
        int nodesPerComm = 4 + rand.nextInt(3);

        for (int c = 0; c < numCommunities; c++) {
            int start = c * (nodesPerComm - 1);
            int end = start + nodesPerComm;

            for (int i = start; i < end; i++) {
                for (int j = i + 1; j < end; j++) {
                    if (rand.nextDouble() < 0.75) {
                        graph.addEdge(i, j);
                    }
                }
            }

            if (c < numCommunities - 1) {
                int overlap = 2;
                for (int i = 0; i < overlap; i++) {
                    int u = end - overlap + i;
                    int v = end + i;
                    graph.addEdge(u, v);
                }
            }
        }

        calculateNodePositions();
        reset();
    }

    private void drawGraph() {
        // Enhanced background
        gc.setFill(Color.rgb(236, 240, 241));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Get appropriate graph for display
        Graph displayGraph = currentAlgorithm.equals("Girvan-Newman") ?
                girvanNewmanAlgo.getOriginalGraph() : graph;

        // Draw communities for all algorithms
        drawCommunities();

        // Draw cliques for CPM
        if (currentAlgorithm.equals("Clique Percolation") && cpmAlgo.hasResults()) {
            drawCliques();
        }

        // Draw highlighted clique edges
        if (!highlightedClique.isEmpty()) {
            drawHighlightedCliqueEdges(displayGraph);
        }

        // Draw all edges
        gc.setStroke(Color.rgb(189, 195, 199));
        gc.setLineWidth(2);
        for (int node : displayGraph.nodes()) {
            Point p1 = nodePositions.get(node);
            for (int neighbor : displayGraph.neighbors(node)) {
                if (node < neighbor) {
                    Point p2 = nodePositions.get(neighbor);
                    gc.strokeLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }

        // Draw nodes with enhanced styling
        Map<Integer, Integer> nodeToCommunity = getNodeToCommunityMap();
        for (int node : displayGraph.nodes()) {
            Point p = nodePositions.get(node);

            boolean isHighlighted = highlightedClique.contains(node);
            boolean isHovered = (node == hoveredNode);
            int community = nodeToCommunity.getOrDefault(node, -1);

            // Node circle with community-based coloring
            if (isHighlighted) {
                gc.setFill(Color.rgb(241, 196, 15));
                gc.fillOval(p.x - 25, p.y - 25, 50, 50);
            } else if (isHovered) {
                gc.setFill(Color.rgb(52, 152, 219));
                gc.fillOval(p.x - 23, p.y - 23, 46, 46);
            } else if (community != -1) {
                Color communityColor = COLORS[Math.abs(community) % COLORS.length];
                gc.setFill(communityColor);
                gc.fillOval(p.x - 20, p.y - 20, 40, 40);
            } else {
                gc.setFill(Color.WHITE);
                gc.fillOval(p.x - 20, p.y - 20, 40, 40);
            }

            // Node border
            gc.setStroke(Color.rgb(52, 73, 94));
            gc.setLineWidth(isHighlighted ? 4 : (isHovered ? 3 : 2));
            gc.strokeOval(p.x - 20, p.y - 20, 40, 40);

            // Node ID
            gc.setFill(Color.rgb(44, 62, 80));
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 16));
            String text = String.valueOf(node);
            double textWidth = text.length() * 8;
            gc.fillText(text, p.x - textWidth / 2, p.y + 6);
        }

        // Draw info for hovered node
        if (hoveredNode != -1) {
            drawNodeInfo(displayGraph);
        }
    }

    private void drawHighlightedCliqueEdges(Graph displayGraph) {
        gc.setStroke(Color.rgb(241, 196, 15));
        gc.setLineWidth(5);

        List<Integer> nodes = new ArrayList<>(highlightedClique);
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                int u = nodes.get(i);
                int v = nodes.get(j);
                if (displayGraph.neighbors(u).contains(v)) {
                    Point p1 = nodePositions.get(u);
                    Point p2 = nodePositions.get(v);
                    gc.strokeLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }
    }

    private void drawCliques() {
        List<Set<Integer>> cliques = cpmAlgo.findAllCliques(currentK);

        for (int i = 0; i < cliques.size(); i++) {
            Set<Integer> clique = cliques.get(i);
            Color color = COLORS[i % COLORS.length];

            // Draw colored polygon for each clique
            List<Point> points = new ArrayList<>();
            for (int node : clique) {
                points.add(nodePositions.get(node));
            }

            if (points.size() >= 3) {
                List<Point> hull = convexHull(points);

                gc.setFill(new Color(color.getRed(), color.getGreen(),
                        color.getBlue(), 0.2));
                double[] xPoints = new double[hull.size()];
                double[] yPoints = new double[hull.size()];

                Point center = getCenter(hull);
                for (int j = 0; j < hull.size(); j++) {
                    Point pt = hull.get(j);
                    double dx = pt.x - center.x;
                    double dy = pt.y - center.y;
                    double len = Math.sqrt(dx * dx + dy * dy);
                    xPoints[j] = pt.x + (dx / len) * 35;
                    yPoints[j] = pt.y + (dy / len) * 35;
                }

                gc.fillPolygon(xPoints, yPoints, hull.size());
                gc.setStroke(color);
                gc.setLineWidth(2);
                gc.strokePolygon(xPoints, yPoints, hull.size());
            }
        }
    }

    private void drawCommunities() {
        Map<Integer, Set<Integer>> communitySets = getCurrentCommunities();

        int idx = 0;
        for (Set<Integer> community : communitySets.values()) {
            if (community.size() < 2) continue;

            Color color = COLORS[idx % COLORS.length];

            List<Point> points = new ArrayList<>();
            for (int node : community) {
                Point p = nodePositions.get(node);
                if (p != null) points.add(p);
            }

            if (points.size() >= 3) {
                List<Point> hull = convexHull(points);

                gc.setFill(new Color(color.getRed(), color.getGreen(),
                        color.getBlue(), 0.15));
                double[] xPoints = new double[hull.size()];
                double[] yPoints = new double[hull.size()];

                Point center = getCenter(hull);
                for (int i = 0; i < hull.size(); i++) {
                    Point p = hull.get(i);
                    double dx = p.x - center.x;
                    double dy = p.y - center.y;
                    double len = Math.sqrt(dx * dx + dy * dy);
                    xPoints[i] = p.x + (dx / len) * 40;
                    yPoints[i] = p.y + (dy / len) * 40;
                }

                gc.fillPolygon(xPoints, yPoints, hull.size());

                // Dashed border for communities
                gc.setStroke(color);
                gc.setLineWidth(3);
                gc.setLineDashes(10, 5);
                gc.strokePolygon(xPoints, yPoints, hull.size());
                gc.setLineDashes(0);
            }

            idx++;
        }
    }

    private void drawNodeInfo(Graph displayGraph) {
        Point p = nodePositions.get(hoveredNode);

        String info = "Node " + hoveredNode + "\nDegree: " + displayGraph.degree(hoveredNode);

        // Add clique info for CPM
        if (currentAlgorithm.equals("Clique Percolation") && cpmAlgo.hasResults()) {
            List<Set<Integer>> cliques = cpmAlgo.findAllCliques(currentK);
            int cliqueCount = 0;
            for (Set<Integer> clique : cliques) {
                if (clique.contains(hoveredNode)) {
                    cliqueCount++;
                }
            }
            info += "\nCliques: " + cliqueCount;
        }

        gc.setFill(Color.rgb(44, 62, 80, 0.9));
        gc.fillRect(p.x + 25, p.y - 30, 100, 60);

        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", 11));
        String[] lines = info.split("\n");
        for (int i = 0; i < lines.length; i++) {
            gc.fillText(lines[i], p.x + 30, p.y - 15 + i * 15);
        }
    }

    private Map<Integer, Set<Integer>> getCurrentCommunities() {
        switch (currentAlgorithm) {
            case "Louvain":
                Map<Integer, Integer> louvainMap = louvainAlgo.getCommunityMap();
                Map<Integer, Set<Integer>> louvainComm = new HashMap<>();
                for (Map.Entry<Integer, Integer> entry : louvainMap.entrySet()) {
                    louvainComm.computeIfAbsent(entry.getValue(), k -> new HashSet<>()).add(entry.getKey());
                }
                return louvainComm;

            case "Girvan-Newman":
                return showBest ? girvanNewmanAlgo.getBestCommunities() : girvanNewmanAlgo.getCurrentCommunities();

            case "Label Propagation":
                return lpaAlgo.getCommunities();

            case "Clique Percolation":
                return cpmAlgo.getCommunities();

            default:
                return new HashMap<>();
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