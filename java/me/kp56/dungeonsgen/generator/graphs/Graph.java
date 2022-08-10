package me.kp56.dungeonsgen.generator.graphs;

import me.kp56.dungeonsgen.utils.Utils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class Graph {
    private List<List<Integer>> adjacencyList;

    public Graph(int nodes) {
        adjacencyList = new ArrayList<>();

        for (int i = 0; i < nodes; i++)
            adjacencyList.add(new ArrayList<>());
    }

    public Graph(List<List<Integer>> adjacencyList) {
        this.adjacencyList = adjacencyList;
    }

    public void createNewNode() {
        adjacencyList.add(new ArrayList<>());
    }

    public List<Integer> getConnected(int node) {
        return adjacencyList.get(node);
    }

    public void connectNode(int node1, int node2) {
        adjacencyList.get(node1).add(node2);
    }

    public int size() {
        return adjacencyList.size();
    }

    public void connectNodeBidirectionally(int node1, int node2) {
        connectNode(node1, node2);
        connectNode(node2, node1);
    }

    public List<List<Integer>> toAdjacencyList() {
        return adjacencyList;
    }

    public static Graph fullyConnectedFromTwoDimIntArray(int[][] arr) {
        Map<Integer, List<Coordinates>> nodeIDs = getNodesFromTwoDimIntArray(arr);

        Graph graph = new Graph(nodeIDs.size());

        for (Map.Entry<Integer, List<Coordinates>> entry : nodeIDs.entrySet()) {
            List<Coordinates> relCoords = Utils.getRelCoords();

            for (Coordinates coords : entry.getValue()) {
                for (Coordinates rel : relCoords) {
                    Coordinates coordinates = new Coordinates(coords.x + rel.x, coords.y + rel.y);

                    if (Utils.isInBounds(coordinates.x, coordinates.y, arr)) {
                        if (arr[coordinates.x][coordinates.y] != arr[coords.x][coords.y]) {
                            if (!graph.getConnected(arr[coordinates.x][coordinates.y]).contains(arr[coords.x][coords.y])) {
                                graph.connectNodeBidirectionally(arr[coordinates.x][coordinates.y], arr[coords.x][coords.y]);
                            }
                        }
                    }
                }
            }
        }

        return graph;
    }

    public static Graph fromTwoDimIntArray(int[][] arr) {
        Map<Integer, List<Coordinates>> nodeIDs = getNodesFromTwoDimIntArray(arr);

        return new Graph(nodeIDs.size());
    }

    private static Map<Integer, List<Coordinates>> getNodesFromTwoDimIntArray(int[][] arr) {
        Map<Integer, List<Coordinates>> nodeIDs = new TreeMap<>();

        for (int x = 0; x < arr.length; x++) {
            for (int y = 0; y < arr[0].length; y++) {
                if (!nodeIDs.containsKey(arr[x][y])) {
                    List<Coordinates> list = new ArrayList<>();
                    list.add(new Coordinates(x, y));

                    nodeIDs.put(arr[x][y], list);
                } else {
                    nodeIDs.get(arr[x][y]).add(new Coordinates(x, y));
                }
            }
        }

        return nodeIDs;
    }

    public static List<Pair<Integer, Integer>> constructPath(List<Integer> path) {
        List<Pair<Integer, Integer>> connections = new ArrayList<>();

        int prev = -1;
        for (int i : path) {
            if (prev != -1) {
                connections.add(Pair.of(prev, i));
            }

            prev = i;
        }

        return connections;
    }

    @Override
    public String toString() {
        return "Graph{" +
                "adjacencyList=" + adjacencyList +
                '}';
    }
}
