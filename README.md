# Community Detection Algorithms Visualizer

A comprehensive JavaFX application for visualizing and comparing four fundamental community detection algorithms in network analysis. This educational tool provides both theoretical understanding and practical visualization of how different algorithms discover community structures in graphs.

## 📊 Algorithms Overview

### 1. Louvain Method
**Core Principle**: Hierarchical modularity optimization through local node movements and graph aggregation.

**Key Features**:
- **Two-phase process**: Local optimization followed by graph coarsening
- **Greedy optimization**: Nodes move to communities that maximize modularity gain
- **Hierarchical structure**: Naturally reveals communities at multiple scales
- **High efficiency**: O(n log n) complexity suitable for large networks

**Mathematical Foundation**:
```
Modularity Q = Σ [ (l_c/m) - (d_c/2m)² ]
where l_c = internal links, d_c = total degree, m = total links
```

### 2. Girvan-Newman Algorithm  
**Core Principle**: Divisive hierarchical clustering by iteratively removing high-betweenness edges.

**Key Features**:
- **Edge betweenness**: Identifies bridge edges connecting communities
- **Divisive approach**: Splits network by removing edges rather than grouping nodes
- **Hierarchical dendrogram**: Produces complete community hierarchy
- **Quality tracking**: Monitors modularity to find optimal partition

**Betweenness Calculation**:
- Counts shortest paths passing through each edge
- High betweenness indicates critical connector edges
- O(m²n) complexity limits to smaller networks

### 3. Label Propagation Algorithm (LPA)
**Core Principle**: Iterative diffusion where nodes adopt the most frequent label among neighbors.

**Key Features**:
- **Near-linear time**: O(m) complexity enables massive scale analysis
- **Simple mechanics**: No parameters or optimization criteria
- **Non-deterministic**: Random tie-breaking leads to multiple possible outcomes
- **Fast convergence**: Typically requires only 5-10 iterations

**Process**:
1. Each node starts with unique label
2. Nodes asynchronously adopt majority neighbor label
3. Process repeats until no labels change
4. Nodes with same label form communities

### 4. Clique Percolation Method (CPM)
**Core Principle**: Identifies overlapping communities through adjacent k-cliques.

**Key Features**:
- **Overlapping communities**: Nodes can belong to multiple communities
- **Structure-based**: Uses complete subgraphs (cliques) as building blocks
- **Parameter k**: Controls community granularity (k-clique size)
- **NP-hard complexity**: Suitable for dense, smaller networks

**Methodology**:
1. Find all maximal cliques (Bron-Kerbosch algorithm)
2. Connect k-cliques sharing k-1 nodes
3. Connected components form overlapping communities
4. k parameter controls community strictness

## 🎯 Algorithm Comparison

| Algorithm | Approach | Community Type | Complexity | Best Use Case |
|-----------|----------|----------------|------------|---------------|
| **Louvain** | Agglomerative | Non-overlapping | O(n log n) | Large networks, high quality |
| **Girvan-Newman** | Divisive | Non-overlapping | O(m²n) | Small networks, hierarchy |
| **Label Propagation** | Diffusion | Non-overlapping | O(m) | Massive networks, speed |
| **Clique Percolation** | Structural | Overlapping | NP-hard | Dense networks, overlaps |

## 🔍 Key Theoretical Concepts

### Modularity
Measures quality of community partition by comparing actual edge distribution to random null model. Higher modularity indicates better community structure.

### Overlap vs Non-overlapping
- **Non-overlapping**: Each node belongs to exactly one community (Louvain, Girvan-Newman, LPA)
- **Overlapping**: Nodes can belong to multiple communities (CPM)

### Resolution Limit
Algorithms may miss small communities in large networks. CPM's k parameter directly controls this.

## 🖥️ Visualization Features

The visualizer provides real-time execution of each algorithm with:
- Step-by-step algorithm progression
- Color-coded community assignments
- Interactive node inspection
- Performance metrics (time, modularity)
- CPM-specific clique highlighting and view modes

## 🎓 Educational Value

This project demonstrates:
- Algorithm trade-offs in community detection
- Practical implementation of complex graph algorithms
- Visual intuition for abstract network concepts
- Performance characteristics of different approaches

Perfect for courses in network science, graph theory, or algorithm design.

---

*Explore how different mathematical approaches reveal the hidden community structure in networks through this interactive educational tool.*
