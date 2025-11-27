package ru.zolotuhin.ModelInfoProcess.Lab4;

import java.util.*;
import java.util.stream.Collectors;

public class ReachabilityTree {
    private PetriNet petriNet;
    private Map<String, Node> nodes = new HashMap<>();
    private Node root;

    public ReachabilityTree(PetriNet petriNet) {
        this.petriNet = petriNet;
        buildTree();
    }

    private void buildTree() {
        root = new Node(petriNet.getState(), "Initial");
        nodes.put(root.getStateHash(), root);
        buildTreeRecursive(root, new HashSet<>());
    }

    private void buildTreeRecursive(Node currentNode, Set<String> visited) {
        String currentHash = currentNode.getStateHash();
        if (visited.contains(currentHash)) {
            currentNode.setTerminal(true);
            return;
        }

        visited.add(currentHash);

        // Проверяем все возможные переходы
        for (String transition : Arrays.asList("StartRead", "EndRead", "StartWrite", "EndWrite")) {
            // Создаем копию сети для симуляции
            PetriNet tempNet = new PetriNet(petriNet.maxReaders);
            tempNet.places.putAll(currentNode.getState());

            if (tempNet.fireTransition(transition)) {
                Node nextNode = new Node(tempNet.getState(), transition);
                String nextHash = nextNode.getStateHash();

                if (!nodes.containsKey(nextHash)) {
                    nodes.put(nextHash, nextNode);
                    currentNode.addChild(nextNode, transition);
                    buildTreeRecursive(nextNode, new HashSet<>(visited));
                } else {
                    currentNode.addChild(nodes.get(nextHash), transition);
                }
            }
        }

        if (currentNode.getChildren().isEmpty()) {
            currentNode.setTerminal(true);
        }
    }

    public void printTree() {
        System.out.println("ДЕРЕВО ДОСТИЖИМОСТИ:");
        printNode(root, 0);
    }

    private void printNode(Node node, int level) {
        String indent = "  ".repeat(level);
        System.out.println(indent + "Состояние: " + node.getState());
        System.out.println(indent + "Переход: " + node.getTransition());

        if (node.isTerminal()) {
            System.out.println(indent + "ТЕРМИНАЛЬНОЕ СОСТОЯНИЕ");
        }

        for (Map.Entry<String, Node> entry : node.getChildren().entrySet()) {
            System.out.println(indent + "-> " + entry.getKey());
            printNode(entry.getValue(), level + 1);
        }
    }

    // Вложенный класс для узла дерева
    private static class Node {
        private Map<String, Integer> state;
        private String transition;
        private Map<String, Node> children = new HashMap<>();
        private boolean isTerminal = false;

        public Node(Map<String, Integer> state, String transition) {
            this.state = new HashMap<>(state);
            this.transition = transition;
        }

        public String getStateHash() {
            return state.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("|"));
        }

        public void addChild(Node child, String transition) {
            children.put(transition, child);
        }

        // Геттеры
        public Map<String, Integer> getState() { return state; }
        public String getTransition() { return transition; }
        public Map<String, Node> getChildren() { return children; }
        public boolean isTerminal() { return isTerminal; }
        public void setTerminal(boolean terminal) { isTerminal = terminal; }
    }
}
