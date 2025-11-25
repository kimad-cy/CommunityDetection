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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Compact Enhanced Community Detection Visualizer
 * All features in a space-efficient design
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
    private Label timeLabel;
    private Label complexityLabel;
    private Slider kSlider;
    private Label kValueLabel;
    private VBox kPanel;
    private Button showBestButton;
    private ListView<String> cliqueListView;
    private ToggleGroup cpmViewToggle;
    private ToggleButton showCliquesButton;
    private ToggleButton showCommunitiesButton;

    private Map<Integer, Point> nodePositions;
    private boolean isRunning = false;
    private boolean showBest = false;
    private String currentAlgorithm = "Louvain";
    private int currentK = 3;

    // Enhanced visualization state
    private Set<Integer> highlightedClique = new HashSet<>();
    private int hoveredNode = -1;
    private List<Set<Integer>> discoveredCliques = new ArrayList<>();
    private Map<Integer, Double> nodeAnimations = new HashMap<>();
    private double animationTime = 0;

    // CPM visualization mode
    private boolean showCliquesMode = true;

    // Execution time tracking
    private long startTime;
    private long executionTime;

    // Visualization settings
    private boolean showCliqueOverlays = true;
    private boolean showCommunityHulls = true;
    private boolean animateTransitions = true;

    private static final Color[] COLORS = {
            Color.rgb(231, 76, 60), Color.rgb(52, 152, 219), Color.rgb(46, 204, 113),
            Color.rgb(241, 196, 15), Color.rgb(155, 89, 182), Color.rgb(26, 188, 156),
            Color.rgb(230, 126, 34), Color.rgb(52, 73, 94), Color.rgb(255, 107, 129),
            Color.rgb(95, 39, 205), Color.rgb(0, 184, 148), Color.rgb(253, 203, 110)
    };

    @Override
    public void start(Stage primaryStage) {
        canvas = new Canvas(900, 650);
        gc = canvas.getGraphicsContext2D();

        initializeSampleGraph();
        calculateNodePositions();

        // Add mouse interaction
        canvas.setOnMouseMoved(this::handleMouseMove);
        canvas.setOnMouseExited(e -> {
            hoveredNode = -1;
            drawGraph();
        });
        canvas.setOnMouseClicked(this::handleMouseClick);

        // Animation timer for smooth transitions
        javafx.animation.AnimationTimer timer = new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                animationTime += 0.016;
                if (animateTransitions) {
                    updateAnimations();
                    drawGraph();
                }
            }
        };
        timer.start();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(8));
        root.setCenter(canvas);

        VBox controlPanel = createControlPanel();
        root.setRight(controlPanel);

        VBox bottomPanel = createBottomPanel();
        root.setBottom(bottomPanel);

        drawGraph();

        Scene scene = new Scene(root, 1200, 750);
        primaryStage.setTitle("Community Detection Visualizer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(10));
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setStyle("-fx-background-color: #2c3e50; -fx-border-radius: 5;");
        panel.setPrefWidth(250);

        Label titleLabel = new Label("Algorithm Control");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        algorithmSelector = new ComboBox<>();
        algorithmSelector.getItems().addAll("Louvain", "Girvan-Newman", "Label Propagation", "Clique Percolation");
        algorithmSelector.setValue("Louvain");
        algorithmSelector.setOnAction(e -> switchAlgorithm());
        algorithmSelector.setStyle("-fx-font-size: 12px;");
        algorithmSelector.setPrefWidth(230);

        // Compact info section
        VBox infoBox = new VBox(3);
        infoBox.setStyle("-fx-background-color: #34495e; -fx-padding: 8; -fx-background-radius: 3;");

        timeLabel = new Label("Time: --");
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ecf0f1;");

        complexityLabel = new Label("Complexity: O(n log n)");
        complexityLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #bdc3c7;");

        metricLabel = new Label("Modularity: 0.0000");
        metricLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #3498db;");

        infoBox.getChildren().addAll(timeLabel, complexityLabel, metricLabel);

        statsLabel = new Label("Ready to start");
        statsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ecf0f1; -fx-font-family: 'Consolas';");
        statsLabel.setWrapText(true);
        statsLabel.setPrefHeight(60);

        // Enhanced K-value panel for CPM
        kPanel = new VBox(5);
        kPanel.setStyle("-fx-background-color: #34495e; -fx-padding: 8; -fx-background-radius: 3;");
        Label kLabel = new Label("Clique Size (k):");
        kLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ecf0f1;");

        kSlider = new Slider(3, 6, 3);
        kSlider.setMajorTickUnit(1);
        kSlider.setMinorTickCount(0);
        kSlider.setSnapToTicks(true);
        kSlider.setShowTickLabels(true);
        kSlider.setShowTickMarks(true);
        kSlider.setPrefWidth(210);

        kValueLabel = new Label("k = 3");
        kValueLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3498db;");

        kSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentK = newVal.intValue();
            kValueLabel.setText("k = " + currentK);
            if (currentAlgorithm.equals("Clique Percolation") && cpmAlgo.hasResults()) {
                performCPMStep();
            }
        });

        // Compact CPM View Toggle
        HBox toggleBox = new HBox(3);
        cpmViewToggle = new ToggleGroup();

        showCliquesButton = new ToggleButton("Cliques");
        showCliquesButton.setToggleGroup(cpmViewToggle);
        showCliquesButton.setSelected(true);
        showCliquesButton.setStyle("-fx-font-size: 10px; -fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 3 8 3 8;");
        showCliquesButton.setOnAction(e -> {
            showCliquesMode = true;
            drawGraph();
        });

        showCommunitiesButton = new ToggleButton("Communities");
        showCommunitiesButton.setToggleGroup(cpmViewToggle);
        showCommunitiesButton.setStyle("-fx-font-size: 10px; -fx-background-color: #34495e; -fx-text-fill: white; -fx-padding: 3 8 3 8;");
        showCommunitiesButton.setOnAction(e -> {
            showCliquesMode = false;
            drawGraph();
        });

        toggleBox.getChildren().addAll(showCliquesButton, showCommunitiesButton);
        toggleBox.setVisible(false);

        kPanel.getChildren().addAll(kLabel, kSlider, kValueLabel, toggleBox);
        kPanel.setVisible(false);

        // Compact action buttons
        Button stepButton = createCompactButton("Next Step", "#3498db", e -> performStep());
        Button runButton = createCompactButton("Run All", "#2ecc71", e -> runAll());

        showBestButton = createCompactButton("Best Partition", "#9b59b6", e -> toggleBestPartition());
        showBestButton.setVisible(false);

        HBox buttonRow1 = new HBox(5, stepButton, runButton);
        buttonRow1.setAlignment(Pos.CENTER);

        Button resetButton = createCompactButton("Reset", "#e74c3c", e -> reset());
        Button newGraphButton = createCompactButton("New Graph", "#f39c12", e -> generateNewGraph());

        HBox buttonRow2 = new HBox(5, resetButton, newGraphButton);
        buttonRow2.setAlignment(Pos.CENTER);

        panel.getChildren().addAll(
                titleLabel,
                algorithmSelector,
                new Separator(),
                infoBox,
                statsLabel,
                kPanel,
                buttonRow1,
                buttonRow2,
                showBestButton
        );

        return panel;
    }

    private VBox createBottomPanel() {
        VBox panel = new VBox(3);
        panel.setPadding(new Insets(5));
        panel.setStyle("-fx-background-color: #ecf0f1;");
        panel.setPrefHeight(120);

        Label listLabel = new Label("Cliques (click to highlight):");
        listLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        cliqueListView = new ListView<>();
        cliqueListView.setPrefHeight(80);
        cliqueListView.setStyle("-fx-font-size: 10px; -fx-font-family: 'Consolas';");
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

    private Button createCompactButton(String text, String color, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button button = new Button(text);
        button.setOnAction(handler);
        button.setStyle(
                "-fx-font-size: 11px; " +
                        "-fx-background-color: " + color + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 5 10 5 10; " +
                        "-fx-background-radius: 3;"
        );
        button.setPrefWidth(110);
        return button;
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

    private void handleMouseClick(MouseEvent e) {
        if (hoveredNode != -1 && currentAlgorithm.equals("Clique Percolation")) {
            highlightCliquesContainingNode(hoveredNode);
        }
    }

    private void highlightCliquesContainingNode(int node) {
        if (!cpmAlgo.hasResults()) return;

        List<Set<Integer>> cliques = cpmAlgo.findAllCliques(currentK);
        List<Set<Integer>> containingCliques = cliques.stream()
                .filter(clique -> clique.contains(node))
                .collect(Collectors.toList());

        if (!containingCliques.isEmpty()) {
            highlightedClique = containingCliques.get(0);
            drawGraph();
        }
    }

    private void highlightCliqueFromString(String cliqueStr) {
        highlightedClique.clear();
        try {
            int startIdx = cliqueStr.indexOf('[');
            int endIdx = cliqueStr.indexOf(']');
            if (startIdx != -1 && endIdx != -1) {
                String nodesStr = cliqueStr.substring(startIdx + 1, endIdx);
                String[] nodes = nodesStr.split(",");
                for (String node : nodes) {
                    highlightedClique.add(Integer.parseInt(node.trim()));
                }
                for (int node : highlightedClique) {
                    nodeAnimations.put(node, animationTime);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing clique: " + e.getMessage());
        }
        drawGraph();
    }

    private String getTimeComplexity(String algorithm) {
        switch (algorithm) {
            case "Louvain": return "O(n log n)";
            case "Girvan-Newman": return "O(m²n)";
            case "Label Propagation": return "O(m)";
            case "Clique Percolation": return "NP-hard";
            default: return "Unknown";
        }
    }

    private void switchAlgorithm() {
        if (isRunning) return;

        currentAlgorithm = algorithmSelector.getValue();
        showBest = false;
        highlightedClique.clear();
        nodeAnimations.clear();

        boolean isCPM = currentAlgorithm.equals("Clique Percolation");
        kPanel.setVisible(isCPM);
        cliqueListView.setVisible(isCPM);
        showBestButton.setVisible(currentAlgorithm.equals("Girvan-Newman"));

        // Show/hide CPM view toggle
        HBox toggleBox = (HBox) kPanel.getChildren().get(3);
        toggleBox.setVisible(isCPM);

        complexityLabel.setText("Complexity: " + getTimeComplexity(currentAlgorithm));
        reset();
    }

    private void initializeSampleGraph() {
        graph = new Graph();
        // Community 1
        graph.addEdge(0, 1); graph.addEdge(0, 2); graph.addEdge(1, 2);
        graph.addEdge(1, 3); graph.addEdge(2, 3); graph.addEdge(0, 3);
        // Overlap region
        graph.addEdge(3, 4); graph.addEdge(3, 5); graph.addEdge(4, 5);
        // Community 2
        graph.addEdge(4, 6); graph.addEdge(5, 6); graph.addEdge(5, 7);
        graph.addEdge(6, 7); graph.addEdge(4, 7);
        // Community 3
        graph.addEdge(7, 8); graph.addEdge(7, 9); graph.addEdge(8, 9);
        graph.addEdge(8, 10); graph.addEdge(9, 10);
        // Additional edges
        graph.addEdge(0, 4); graph.addEdge(6, 10);

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

    private void updateAnimations() {
        List<Integer> toRemove = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : nodeAnimations.entrySet()) {
            if (animationTime - entry.getValue() > 3.0) {
                toRemove.add(entry.getKey());
            }
        }
        toRemove.forEach(nodeAnimations::remove);
    }

    private void performStep() {
        if (isRunning) return;
        startTime = System.currentTimeMillis();

        switch (currentAlgorithm) {
            case "Louvain": performLouvainStep(); break;
            case "Girvan-Newman": performGirvanNewmanStep(); break;
            case "Label Propagation": performLPAStep(); break;
            case "Clique Percolation": performCPMStep(); break;
        }

        executionTime = System.currentTimeMillis() - startTime;
        updateTimeLabel();
        drawGraph();
    }

    private void performLouvainStep() {
        boolean changed = louvainAlgo.performOnePass();
        if (changed) {
            statsLabel.setText("Phase " + louvainAlgo.getPhase() + "\nCommunities: " + louvainAlgo.getNumCommunities());
        } else {
            statsLabel.setText("Converged!\nCommunities: " + louvainAlgo.getNumCommunities());
        }
        metricLabel.setText(String.format("Q: %.4f", louvainAlgo.computeModularity()));
    }

    private void performGirvanNewmanStep() {
        showBest = false;
        boolean hasMore = girvanNewmanAlgo.performOneStep();
        if (hasMore) {
            statsLabel.setText("Step: " + girvanNewmanAlgo.getCurrentStep() +
                    "\nEdges: " + girvanNewmanAlgo.getRemainingEdges() +
                    "\nCommunities: " + girvanNewmanAlgo.getCurrentCommunities().size());
        } else {
            statsLabel.setText("Complete!\nBest: " + girvanNewmanAlgo.getBestCommunities().size() + " communities");
        }
        metricLabel.setText(String.format("Q: %.4f", girvanNewmanAlgo.getCurrentModularity()));
    }

    private void performLPAStep() {
        boolean changed = lpaAlgo.iterate();
        statsLabel.setText("Iteration: " + lpaAlgo.getIteration() +
                "\nCommunities: " + lpaAlgo.getCommunities().size() +
                (changed ? "" : "\nConverged!"));
        metricLabel.setText("LPA Running");
    }

    private void performCPMStep() {
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

        statsLabel.setText("✓ Complete!\nk=" + currentK +
                "\nCommunities: " + communities.size() +
                "\nNodes: " + nodesInCommunities.size() +
                "\nOverlap: " + overlapping);
        metricLabel.setText("CPM Final");

        updateCliquesDisplay();
        animateCliqueDiscovery();
    }

    private void animateCliqueDiscovery() {
        List<Set<Integer>> cliques = cpmAlgo.findAllCliques(currentK);
        for (Set<Integer> clique : cliques) {
            for (int node : clique) {
                nodeAnimations.put(node, animationTime);
            }
        }
    }

    private void updateCliquesDisplay() {
        List<Set<Integer>> allCliques = cpmAlgo.findAllCliques(currentK);
        discoveredCliques = allCliques;

        allCliques.sort((a, b) -> Integer.compare(b.size(), a.size()));

        cliqueListView.getItems().clear();
        for (int i = 0; i < allCliques.size(); i++) {
            List<Integer> cliqueList = new ArrayList<>(allCliques.get(i));
            Collections.sort(cliqueList);
            cliqueListView.getItems().add(String.format("C%d (size %d): %s",
                    i + 1, cliqueList.size(), cliqueList));
        }
    }

    private void updateTimeLabel() {
        if (executionTime > 0) {
            String timeText = executionTime < 1000 ?
                    String.format("Time: %d ms", executionTime) :
                    String.format("Time: %.2f s", executionTime / 1000.0);
            timeLabel.setText(timeText);
        }
    }

    private void runAll() {
        if (isRunning) return;
        isRunning = true;
        startTime = System.currentTimeMillis();

        new Thread(() -> {
            switch (currentAlgorithm) {
                case "Louvain": runLouvainAll(); break;
                case "Girvan-Newman": runGirvanNewmanAll(); break;
                case "Label Propagation": runLPAAll(); break;
                case "Clique Percolation": runCPMAll(); break;
            }
        }).start();
    }

    private void runLouvainAll() {
        while (louvainAlgo.performOnePass()) {
            Platform.runLater(() -> {
                statsLabel.setText("Running...\nCommunities: " + louvainAlgo.getNumCommunities());
                metricLabel.setText(String.format("Q: %.4f", louvainAlgo.computeModularity()));
                drawGraph();
            });
            try { Thread.sleep(400); } catch (InterruptedException e) {}
        }
        executionTime = System.currentTimeMillis() - startTime;
        Platform.runLater(() -> {
            statsLabel.setText("Converged!\nCommunities: " + louvainAlgo.getNumCommunities());
            updateTimeLabel();
            drawGraph();
            isRunning = false;
        });
    }

    private void runGirvanNewmanAll() {
        while (girvanNewmanAlgo.performOneStep()) {
            Platform.runLater(() -> {
                statsLabel.setText("Step: " + girvanNewmanAlgo.getCurrentStep() +
                        "\nCommunities: " + girvanNewmanAlgo.getCurrentCommunities().size());
                drawGraph();
            });
            try { Thread.sleep(250); } catch (InterruptedException e) {}
        }
        executionTime = System.currentTimeMillis() - startTime;
        Platform.runLater(() -> {
            showBest = true;
            statsLabel.setText("Complete! Showing best partition");
            updateTimeLabel();
            drawGraph();
            isRunning = false;
        });
    }

    private void runLPAAll() {
        int maxIter = 100;
        for (int i = 0; i < maxIter; i++) {
            boolean changed = lpaAlgo.iterate();
            Platform.runLater(() -> {
                statsLabel.setText("Iteration: " + lpaAlgo.getIteration() +
                        "\nCommunities: " + lpaAlgo.getCommunities().size());
                drawGraph();
            });
            try { Thread.sleep(200); } catch (InterruptedException e) {}
            if (!changed) break;
        }
        executionTime = System.currentTimeMillis() - startTime;
        Platform.runLater(() -> {
            statsLabel.setText("Converged!\nCommunities: " + lpaAlgo.getCommunities().size());
            updateTimeLabel();
            isRunning = false;
        });
    }

    private void runCPMAll() {
        Platform.runLater(() -> {
            cpmAlgo.findCommunities(currentK);
            executionTime = System.currentTimeMillis() - startTime;
            updateTimeLabel();
            updateCliquesDisplay();
            animateCliqueDiscovery();
            drawGraph();
            isRunning = false;
        });
    }

    private void toggleBestPartition() {
        showBest = !showBest;
        drawGraph();
        statsLabel.setText(showBest ? "Showing BEST partition" : "Showing CURRENT state");
    }

    private void reset() {
        if (isRunning) return;

        initializeAlgorithms();
        showBest = false;
        highlightedClique.clear();
        nodeAnimations.clear();
        statsLabel.setText("Ready to start");
        cliqueListView.getItems().clear();
        executionTime = 0;
        timeLabel.setText("Time: --");

        showCliquesMode = true;
        if (showCliquesButton != null) showCliquesButton.setSelected(true);
        if (showCommunitiesButton != null) showCommunitiesButton.setSelected(false);

        switch (currentAlgorithm) {
            case "Louvain":
            case "Girvan-Newman":
                metricLabel.setText("Modularity: 0.0000");
                break;
            case "Label Propagation":
                metricLabel.setText("LPA Ready");
                break;
            case "Clique Percolation":
                metricLabel.setText("CPM Ready");
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
            int start = c * nodesPerComm;
            int end = start + nodesPerComm;
            for (int i = start; i < end; i++) {
                for (int j = i + 1; j < end; j++) {
                    if (rand.nextDouble() < 0.7) graph.addEdge(i, j);
                }
            }
            if (c < numCommunities - 1) {
                graph.addEdge(end - 1, end);
                graph.addEdge(end - 2, end + 1);
            }
        }
        calculateNodePositions();
        reset();
    }

    private void drawGraph() {
        gc.setFill(Color.rgb(248, 249, 250));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        Graph displayGraph = currentAlgorithm.equals("Girvan-Newman") ?
                girvanNewmanAlgo.getOriginalGraph() : graph;

        // Draw communities
        if (showCommunityHulls && !(currentAlgorithm.equals("Clique Percolation") && showCliquesMode)) {
            drawCommunities();
        }

        // Draw CPM visualizations
        if (currentAlgorithm.equals("Clique Percolation") && cpmAlgo.hasResults() && showCliqueOverlays) {
            if (showCliquesMode) {
                drawEnhancedCliques();
            } else {
                drawCPMCommunities();
            }
        }

        // Draw highlighted clique
        if (!highlightedClique.isEmpty()) {
            drawAnimatedHighlightedClique(displayGraph);
        }

        // Draw edges
        drawEdges(displayGraph);

        // Draw nodes
        drawNodes(displayGraph);

        // Draw info for hovered node
        if (hoveredNode != -1) {
            drawEnhancedNodeInfo(displayGraph);
        }

        // Draw overlays
        drawAlgorithmOverlays();
    }

    // ... (Keep all the drawing methods from previous version, but make them more compact)
    // drawEnhancedCliques(), drawCPMCommunities(), drawCliqueConnections(), drawCliqueHull(),
    // drawAnimatedHighlightedClique(), drawEdges(), drawNodes(), drawEnhancedNodeInfo(),
    // drawAlgorithmOverlays(), drawCommunities(), getCurrentCommunities(), getNodeToCommunityMap(),
    // getCenter(), convexHull(), cross() methods remain the same but are more compact

    private void drawEnhancedCliques() {
        List<Set<Integer>> cliques = cpmAlgo.findAllCliques(currentK);
        for (int i = 0; i < cliques.size(); i++) {
            Set<Integer> clique = cliques.get(i);
            Color color = COLORS[i % COLORS.length];
            Point center = getCliqueCenter(clique);
            double radius = getCliqueRadius(clique, center);

            if (clique.size() >= currentK) {
                double pulse = 0.7 + 0.3 * Math.sin(animationTime * 3 + i);
                Color pulseColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0.1 * pulse);
                gc.setFill(pulseColor);
                gc.fillOval(center.x - radius, center.y - radius, radius * 2, radius * 2);
                drawCliqueConnections(clique, color);
            }

            if (clique.size() > 3) {
                drawCliqueHull(clique, color);
            }
        }
    }

    private void drawCPMCommunities() {
        Map<Integer, Set<Integer>> communities = cpmAlgo.getCommunities();
        int idx = 0;
        for (Set<Integer> community : communities.values()) {
            if (community.size() < 2) continue;
            Color color = COLORS[idx % COLORS.length];
            List<Point> points = new ArrayList<>();
            for (int node : community) {
                Point p = nodePositions.get(node);
                if (p != null) points.add(p);
            }
            if (points.size() >= 3) {
                List<Point> hull = convexHull(points);
                double animation = 0.7 + 0.3 * Math.sin(animationTime * 2 + idx);
                Color hullColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0.2 * animation);
                gc.setFill(hullColor);
                double[] xPoints = new double[hull.size()];
                double[] yPoints = new double[hull.size()];
                Point center = getCenter(hull);
                for (int i = 0; i < hull.size(); i++) {
                    Point p = hull.get(i);
                    double dx = p.x - center.x;
                    double dy = p.y - center.y;
                    double len = Math.sqrt(dx * dx + dy * dy);
                    xPoints[i] = p.x + (dx / len) * 35;
                    yPoints[i] = p.y + (dy / len) * 35;
                }
                gc.fillPolygon(xPoints, yPoints, hull.size());
                gc.setStroke(new Color(color.getRed(), color.getGreen(), color.getBlue(), 0.7));
                gc.setLineWidth(2);
                gc.setLineDashes(8, 4);
                gc.strokePolygon(xPoints, yPoints, hull.size());
                gc.setLineDashes(0);
            }
            idx++;
        }
    }

    private void drawCliqueConnections(Set<Integer> clique, Color color) {
        gc.setStroke(new Color(color.getRed(), color.getGreen(), color.getBlue(), 0.6));
        gc.setLineWidth(2);
        List<Integer> nodes = new ArrayList<>(clique);
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                Point p1 = nodePositions.get(nodes.get(i));
                Point p2 = nodePositions.get(nodes.get(j));
                gc.strokeLine(p1.x, p1.y, p2.x, p2.y);
            }
        }
    }

    private void drawCliqueHull(Set<Integer> clique, Color color) {
        List<Point> points = new ArrayList<>();
        for (int node : clique) points.add(nodePositions.get(node));
        if (points.size() >= 3) {
            List<Point> hull = convexHull(points);
            double animation = 0.7 + 0.3 * Math.sin(animationTime * 2);
            Color hullColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0.25 * animation);
            gc.setFill(hullColor);
            double[] xPoints = new double[hull.size()];
            double[] yPoints = new double[hull.size()];
            for (int i = 0; i < hull.size(); i++) {
                Point pt = hull.get(i);
                xPoints[i] = pt.x;
                yPoints[i] = pt.y;
            }
            gc.fillPolygon(xPoints, yPoints, hull.size());
        }
    }

    private void drawAnimatedHighlightedClique(Graph displayGraph) {
        double pulse = 0.5 + 0.5 * Math.sin(animationTime * 5);
        gc.setStroke(Color.rgb(241, 196, 15, pulse));
        gc.setLineWidth(4);
        List<Integer> nodes = new ArrayList<>(highlightedClique);
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                int u = nodes.get(i), v = nodes.get(j);
                if (displayGraph.neighbors(u).contains(v)) {
                    Point p1 = nodePositions.get(u), p2 = nodePositions.get(v);
                    gc.strokeLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }
    }

    private void drawEdges(Graph displayGraph) {
        gc.setStroke(Color.rgb(189, 195, 199, 0.6));
        gc.setLineWidth(1);
        for (int node : displayGraph.nodes()) {
            Point p1 = nodePositions.get(node);
            for (int neighbor : displayGraph.neighbors(node)) {
                if (node < neighbor) {
                    Point p2 = nodePositions.get(neighbor);
                    gc.strokeLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }
    }

    private void drawNodes(Graph displayGraph) {
        Map<Integer, Integer> nodeToCommunity = getNodeToCommunityMap();
        for (int node : displayGraph.nodes()) {
            Point p = nodePositions.get(node);
            boolean isHighlighted = highlightedClique.contains(node);
            boolean isHovered = (node == hoveredNode);
            boolean isAnimated = nodeAnimations.containsKey(node);
            int community = nodeToCommunity.getOrDefault(node, -1);

            double size = 30;
            Color fillColor = Color.WHITE;
            if (isHighlighted) fillColor = Color.rgb(241, 196, 15);
            else if (isHovered) fillColor = Color.rgb(52, 152, 219);
            else if (isAnimated) {
                double pulse = 0.7 + 0.3 * Math.sin(animationTime * 8);
                if (community != -1) {
                    Color communityColor = COLORS[Math.abs(community) % COLORS.length];
                    fillColor = new Color(communityColor.getRed(), communityColor.getGreen(), communityColor.getBlue(), 0.8 + 0.2 * pulse);
                }
            } else if (community != -1) {
                fillColor = COLORS[Math.abs(community) % COLORS.length];
            }

            gc.setFill(fillColor);
            gc.fillOval(p.x - size/2, p.y - size/2, size, size);
            gc.setStroke(Color.rgb(52, 73, 94));
            gc.setLineWidth(isHighlighted ? 2.5 : (isHovered ? 2 : 1.5));
            gc.strokeOval(p.x - size/2, p.y - size/2, size, size);

            gc.setFill(Color.rgb(44, 62, 80));
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 12));
            String text = String.valueOf(node);
            double textWidth = text.length() * 6;
            gc.fillText(text, p.x - textWidth / 2, p.y + 4);
        }
    }

    private void drawEnhancedNodeInfo(Graph displayGraph) {
        Point p = nodePositions.get(hoveredNode);
        StringBuilder info = new StringBuilder();
        info.append("Node ").append(hoveredNode).append("\n");
        info.append("Degree: ").append(displayGraph.degree(hoveredNode));
        if (currentAlgorithm.equals("Clique Percolation") && cpmAlgo.hasResults()) {
            List<Set<Integer>> cliques = cpmAlgo.findAllCliques(currentK);
            int cliqueCount = (int) cliques.stream().filter(clique -> clique.contains(hoveredNode)).count();
            info.append("\nCliques: ").append(cliqueCount);
        }
        gc.setFill(Color.rgb(44, 62, 80, 0.9));
        gc.fillRect(p.x + 25, p.y - 25, 90, 45);
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", 10));
        String[] lines = info.toString().split("\n");
        for (int i = 0; i < lines.length; i++) {
            gc.fillText(lines[i], p.x + 30, p.y - 10 + i * 12);
        }
    }

    private void drawAlgorithmOverlays() {
        gc.setFill(Color.rgb(52, 73, 94, 0.8));
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 14));
        gc.fillText(currentAlgorithm, 15, 25);
        if (executionTime > 0) {
            String timeText = executionTime < 1000 ?
                    String.format("Time: %d ms", executionTime) :
                    String.format("Time: %.1f s", executionTime / 1000.0);
            gc.fillText(timeText, 15, 45);
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
                gc.setFill(new Color(color.getRed(), color.getGreen(), color.getBlue(), 0.1));
                double[] xPoints = new double[hull.size()];
                double[] yPoints = new double[hull.size()];
                Point center = getCenter(hull);
                for (int i = 0; i < hull.size(); i++) {
                    Point p = hull.get(i);
                    double dx = p.x - center.x;
                    double dy = p.y - center.y;
                    double len = Math.sqrt(dx * dx + dy * dy);
                    xPoints[i] = p.x + (dx / len) * 35;
                    yPoints[i] = p.y + (dy / len) * 35;
                }
                gc.fillPolygon(xPoints, yPoints, hull.size());
            }
            idx++;
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
            default: return new HashMap<>();
        }
    }

    private Map<Integer, Integer> getNodeToCommunityMap() {
        Map<Integer, Integer> map = new HashMap<>();
        switch (currentAlgorithm) {
            case "Louvain": return louvainAlgo.getCommunityMap();
            case "Girvan-Newman":
                Map<Integer, Set<Integer>> gnComm = showBest ? girvanNewmanAlgo.getBestCommunities() : girvanNewmanAlgo.getCurrentCommunities();
                for (Map.Entry<Integer, Set<Integer>> entry : gnComm.entrySet()) {
                    for (int node : entry.getValue()) map.put(node, entry.getKey());
                }
                return map;
            case "Label Propagation":
                for (int node : graph.nodes()) map.put(node, lpaAlgo.getLabel(node));
                return map;
            case "Clique Percolation":
                Map<Integer, Set<Integer>> cpmComm = cpmAlgo.getCommunities();
                for (Map.Entry<Integer, Set<Integer>> entry : cpmComm.entrySet()) {
                    for (int node : entry.getValue()) {
                        if (!map.containsKey(node)) map.put(node, entry.getKey());
                    }
                }
                return map;
        }
        return map;
    }

    private Point getCenter(List<Point> points) {
        double sumX = 0, sumY = 0;
        for (Point p : points) { sumX += p.x; sumY += p.y; }
        return new Point(sumX / points.size(), sumY / points.size());
    }

    private List<Point> convexHull(List<Point> points) {
        if (points.size() < 3) return new ArrayList<>(points);
        List<Point> sorted = new ArrayList<>(points);
        sorted.sort((a, b) -> Double.compare(a.x, b.x) != 0 ? Double.compare(a.x, b.x) : Double.compare(a.y, b.y));
        List<Point> lower = new ArrayList<>();
        for (Point p : sorted) {
            while (lower.size() >= 2 && cross(lower.get(lower.size()-2), lower.get(lower.size()-1), p) <= 0) {
                lower.remove(lower.size()-1);
            }
            lower.add(p);
        }
        List<Point> upper = new ArrayList<>();
        for (int i = sorted.size()-1; i >= 0; i--) {
            Point p = sorted.get(i);
            while (upper.size() >= 2 && cross(upper.get(upper.size()-2), upper.get(upper.size()-1), p) <= 0) {
                upper.remove(upper.size()-1);
            }
            upper.add(p);
        }
        lower.remove(lower.size()-1);
        upper.remove(upper.size()-1);
        lower.addAll(upper);
        return lower;
    }

    private double cross(Point o, Point a, Point b) {
        return (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x);
    }

    private Point getCliqueCenter(Set<Integer> clique) {
        double sumX = 0, sumY = 0;
        for (int node : clique) {
            Point p = nodePositions.get(node);
            sumX += p.x; sumY += p.y;
        }
        return new Point(sumX / clique.size(), sumY / clique.size());
    }

    private double getCliqueRadius(Set<Integer> clique, Point center) {
        double maxDist = 0;
        for (int node : clique) {
            Point p = nodePositions.get(node);
            double dist = Math.sqrt(Math.pow(p.x - center.x, 2) + Math.pow(p.y - center.y, 2));
            maxDist = Math.max(maxDist, dist);
        }
        return maxDist + 25;
    }

    public static void main(String[] args) {
        launch(args);
    }
}