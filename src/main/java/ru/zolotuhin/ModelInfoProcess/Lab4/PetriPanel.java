package ru.zolotuhin.ModelInfoProcess.Lab4;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class PetriPanel extends JPanel {
    enum Mode { SELECT, ADD_PLACE, ADD_TRANSITION, ADD_ARC, DELETE, MOVE }

    private Mode currentMode = Mode.SELECT;
    private ArrayList<Place> places = new ArrayList<>();
    private ArrayList<Transition> transitions = new ArrayList<>();
    private ArrayList<Arc> arcs = new ArrayList<>();
    private String networkName;

    private Place selectedPlace = null;
    private Transition selectedTransition = null;
    private Place movingPlace = null;
    private Transition movingTransition = null;
    private int dragOffsetX, dragOffsetY;

    public PetriPanel(String name) {
        this.networkName = name;
        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e.getX(), e.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseReleased(e.getX(), e.getY());
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleDoubleClick(e.getX(), e.getY());
                } else if (e.getClickCount() == 1) {
                    handleSingleClick(e.getX(), e.getY());
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDragged(e.getX(), e.getY());
            }
        });
    }

    private void handleMousePressed(int x, int y) {
        if (currentMode == Mode.SELECT || currentMode == Mode.MOVE) {
            for (Place p : places) {
                if (p.contains(x, y)) {
                    movingPlace = p;
                    dragOffsetX = x - p.x;
                    dragOffsetY = y - p.y;
                    currentMode = Mode.MOVE;
                    p.setActiveColor(new Color(30, 144, 255)); // Синий при перемещении
                    repaint();
                    return;
                }
            }

            for (Transition t : transitions) {
                if (t.contains(x, y)) {
                    movingTransition = t;
                    dragOffsetX = x - t.x;
                    dragOffsetY = y - t.y;
                    currentMode = Mode.MOVE;
                    t.setActiveColor(new Color(30, 144, 255)); // Синий при перемещении
                    repaint();
                    return;
                }
            }
        }
    }

    private void handleSingleClick(int x, int y) {
        switch (currentMode) {
            case ADD_PLACE:
                String name = JOptionPane.showInputDialog(this, "Введите имя позиции:");
                if (name != null && !name.trim().isEmpty()) {
                    boolean exists = false;
                    for (Place p : places) {
                        if (p.name.equals(name)) {
                            exists = true;
                            break;
                        }
                    }

                    if (exists) {
                        JOptionPane.showMessageDialog(this,
                                "Позиция с именем '" + name + "' уже существует!",
                                "Ошибка",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    Color randomColor = new Color(
                            150 + (int)(Math.random() * 105),
                            150 + (int)(Math.random() * 105),
                            150 + (int)(Math.random() * 105)
                    );

                    String tokensStr = JOptionPane.showInputDialog(this,
                            "Введите начальное количество меток:",
                            "0");
                    int tokens = 0;
                    if (tokensStr != null) {
                        try {
                            tokens = Integer.parseInt(tokensStr);
                        } catch (NumberFormatException e) {
                            JOptionPane.showMessageDialog(this,
                                    "Будет установлено 0 меток",
                                    "Информация",
                                    JOptionPane.INFORMATION_MESSAGE);
                        }
                    }

                    addPlace(name, x - 20, y - 20, tokens, randomColor);
                    currentMode = Mode.SELECT;
                    repaint();
                }
                break;

            case ADD_TRANSITION:
                String tName = JOptionPane.showInputDialog(this, "Введите имя перехода:");
                if (tName != null && !tName.trim().isEmpty()) {
                    boolean exists = false;
                    for (Transition t : transitions) {
                        if (t.name.equals(tName)) {
                            exists = true;
                            break;
                        }
                    }

                    if (exists) {
                        JOptionPane.showMessageDialog(this,
                                "Переход с именем '" + tName + "' уже существует!",
                                "Ошибка",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    Color randomColor = new Color(
                            100 + (int)(Math.random() * 155),
                            100 + (int)(Math.random() * 155),
                            100 + (int)(Math.random() * 155)
                    );

                    addTransition(tName, x - 15, y - 5, randomColor);
                    currentMode = Mode.SELECT;
                    repaint();
                }
                break;

            case ADD_ARC:
                if (selectedPlace != null) {
                    selectedPlace.resetColor();
                }
                if (selectedTransition != null) {
                    selectedTransition.resetColor();
                }

                for (Place p : places) {
                    if (p.contains(x, y)) {
                        if (selectedPlace == null && selectedTransition == null) {
                            selectedPlace = p;
                            p.setActiveColor(new Color(50, 205, 50)); // Зеленый
                            repaint();
                            return;
                        } else if (selectedTransition != null) {
                            addArc(selectedTransition.name, p.name);
                            selectedTransition.resetColor();
                            selectedTransition = null;
                            currentMode = Mode.SELECT;
                            repaint();
                            return;
                        }
                    }
                }

                for (Transition t : transitions) {
                    if (t.contains(x, y)) {
                        if (selectedPlace != null) {
                            addArc(selectedPlace.name, t.name);
                            selectedPlace.resetColor();
                            selectedPlace = null;
                            currentMode = Mode.SELECT;
                            repaint();
                            return;
                        } else if (selectedTransition == null) {
                            selectedTransition = t;
                            t.setActiveColor(new Color(50, 205, 50)); // Зеленый
                            repaint();
                            return;
                        } else {
                            JOptionPane.showMessageDialog(this,
                                    "Дуги между переходами не поддерживаются",
                                    "Ошибка",
                                    JOptionPane.ERROR_MESSAGE);
                            selectedTransition.resetColor();
                            selectedTransition = null;
                        }
                    }
                }

                if (selectedPlace != null) {
                    selectedPlace.resetColor();
                    selectedPlace = null;
                    repaint();
                }
                if (selectedTransition != null) {
                    selectedTransition.resetColor();
                    selectedTransition = null;
                    repaint();
                }
                break;

            case DELETE:
                for (Iterator<Place> iterator = places.iterator(); iterator.hasNext();) {
                    Place p = iterator.next();
                    if (p.contains(x, y)) {
                        int confirm = JOptionPane.showConfirmDialog(this,
                                "Удалить позицию '" + p.name + "' и все связанные дуги?",
                                "Подтверждение удаления",
                                JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            removeArcsWithElement(p.name);
                            iterator.remove();
                            repaint();
                        }
                        return;
                    }
                }

                for (Iterator<Transition> iterator = transitions.iterator(); iterator.hasNext();) {
                    Transition t = iterator.next();
                    if (t.contains(x, y)) {
                        int confirm = JOptionPane.showConfirmDialog(this,
                                "Удалить переход '" + t.name + "' и все связанные дуги?",
                                "Подтверждение удаления",
                                JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            removeArcsWithElement(t.name);
                            iterator.remove();
                            repaint();
                        }
                        return;
                    }
                }
                break;

            case SELECT:
                boolean found = false;

                if (selectedPlace != null) {
                    selectedPlace.resetColor();
                    selectedPlace = null;
                }
                if (selectedTransition != null) {
                    selectedTransition.resetColor();
                    selectedTransition = null;
                }

                for (Place p : places) {
                    if (p.contains(x, y)) {
                        selectedPlace = p;
                        p.setActiveColor(new Color(30, 144, 255)); // Синий для выделения
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    for (Transition t : transitions) {
                        if (t.contains(x, y)) {
                            selectedTransition = t;
                            t.setActiveColor(new Color(30, 144, 255)); // Синий для выделения
                            found = true;
                            break;
                        }
                    }
                }

                if (found) {
                    repaint();
                }
                break;
        }
    }

    private void handleMouseDragged(int x, int y) {
        if (currentMode == Mode.MOVE) {
            if (movingPlace != null) {
                movingPlace.x = x - dragOffsetX;
                movingPlace.y = y - dragOffsetY;
                repaint();
            } else if (movingTransition != null) {
                movingTransition.x = x - dragOffsetX;
                movingTransition.y = y - dragOffsetY;
                repaint();
            }
        }
    }

    private void handleMouseReleased(int x, int y) {
        if (currentMode == Mode.MOVE) {
            if (movingPlace != null) {
                movingPlace.resetColor();
                movingPlace = null;
            }
            if (movingTransition != null) {
                movingTransition.resetColor();
                movingTransition = null;
            }
            currentMode = Mode.SELECT;
            repaint();
        }
    }

    private void handleDoubleClick(int x, int y) {
        for (Place p : places) {
            if (p.contains(x, y)) {
                String input = JOptionPane.showInputDialog(this,
                        "Введите количество меток для позиции " + p.name + ":",
                        String.valueOf(p.tokens));
                if (input != null) {
                    try {
                        int newTokens = Integer.parseInt(input);
                        if (newTokens >= 0) {
                            p.tokens = newTokens;
                            repaint();
                        } else {
                            JOptionPane.showMessageDialog(this,
                                    "Количество меток не может быть отрицательным",
                                    "Ошибка",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(this,
                                "Введите корректное число",
                                "Ошибка",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
                return;
            }
        }

        for (Transition t : transitions) {
            if (t.contains(x, y)) {
                String newName = JOptionPane.showInputDialog(this,
                        "Введите новое имя для перехода " + t.name + ":",
                        t.name);
                if (newName != null && !newName.trim().isEmpty()) {
                    // Обновляем все связанные дуги
                    updateArcElementName(t.name, newName);
                    t.name = newName;
                    repaint();
                }
                return;
            }
        }

        for (Place p : places) {
            int centerX = p.x + 20;
            int centerY = p.y + 20;
            double distance = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
            if (distance <= 25) { // Клик по кругу
                String newName = JOptionPane.showInputDialog(this,
                        "Введите новое имя для позиции " + p.name + ":",
                        p.name);
                if (newName != null && !newName.trim().isEmpty()) {
                    updateArcElementName(p.name, newName);
                    p.name = newName;
                    repaint();
                }
                return;
            }
        }
    }

    private void updateArcElementName(String oldName, String newName) {
        for (Arc arc : arcs) {
            if (arc.from.equals(oldName)) {
                arc.from = newName;
            }
            if (arc.to.equals(oldName)) {
                arc.to = newName;
            }
        }
    }

    private void removeArcsWithElement(String elementName) {
        arcs.removeIf(arc -> arc.from.equals(elementName) || arc.to.equals(elementName));
    }

    public void addPlace(String name, int x, int y, int tokens, Color color) {
        places.add(new Place(name, x, y, tokens, color));
    }

    public void addTransition(String name, int x, int y, Color color) {
        transitions.add(new Transition(name, x, y, color));
    }

    public void addArc(String from, String to) {
        for (Arc arc : arcs) {
            if (arc.from.equals(from) && arc.to.equals(to)) {
                JOptionPane.showMessageDialog(this,
                        "Дуга уже существует!",
                        "Предупреждение",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        arcs.add(new Arc(from, to));
        repaint();
    }

    public void deleteSelectedElement() {
        if (selectedPlace != null) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Удалить позицию '" + selectedPlace.name + "' и все связанные дуги?",
                    "Подтверждение удаления",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                removeArcsWithElement(selectedPlace.name);
                places.remove(selectedPlace);
                selectedPlace = null;
                repaint();
            }
        } else if (selectedTransition != null) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Удалить переход '" + selectedTransition.name + "' и все связанные дуги?",
                    "Подтверждение удаления",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                removeArcsWithElement(selectedTransition.name);
                transitions.remove(selectedTransition);
                selectedTransition = null;
                repaint();
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Нет выбранного элемента для удаления",
                    "Информация",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void setMode(Mode mode) {
        this.currentMode = mode;
        if (selectedPlace != null) {
            selectedPlace.resetColor();
            selectedPlace = null;
        }
        if (selectedTransition != null) {
            selectedTransition.resetColor();
            selectedTransition = null;
        }
    }

    public Mode getCurrentMode() {
        return currentMode;
    }

    public String getCurrentMarking() {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        for (int i = 0; i < places.size(); i++) {
            Place p = places.get(i);
            sb.append(p.name).append("=").append(p.tokens);
            if (i < places.size() - 1) sb.append(", ");
        }
        sb.append(" }");
        return sb.toString();
    }

    public String getActiveTransitionsNames() {
        StringBuilder sb = new StringBuilder();
        ArrayList<Transition> enabled = getEnabledTransitions();
        for (int i = 0; i < enabled.size(); i++) {
            sb.append(enabled.get(i).name);
            if (i < enabled.size() - 1) sb.append(", ");
        }
        return sb.toString().isEmpty() ? "Нет активных" : sb.toString();
    }

    public ArrayList<Transition> getEnabledTransitions() {
        ArrayList<Transition> enabled = new ArrayList<>();
        for (Transition t : transitions) {
            if (isTransitionEnabled(t)) {
                enabled.add(t);
            }
        }
        return enabled;
    }

    private boolean isTransitionEnabled(Transition t) {
        for (Arc arc : arcs) {
            if (arc.to.equals(t.name)) {
                Place p = findPlace(arc.from);
                if (p == null || p.tokens < 1) return false;
            }
        }
        return true;
    }

    public void fireTransition(Transition t) {
        for (Arc arc : arcs) {
            if (arc.to.equals(t.name)) {
                Place p = findPlace(arc.from);
                if (p != null) p.tokens--;
            }
        }

        for (Arc arc : arcs) {
            if (arc.from.equals(t.name)) {
                Place p = findPlace(arc.to);
                if (p != null) p.tokens++;
            }
        }
    }

    public ArrayList<Place> getInputPlaces(String transitionName) {
        ArrayList<Place> inputPlaces = new ArrayList<>();
        for (Arc arc : arcs) {
            if (arc.to.equals(transitionName)) {
                Place p = findPlace(arc.from);
                if (p != null) inputPlaces.add(p);
            }
        }
        return inputPlaces;
    }

    public void resetAllColors() {
        for (Place p : places) {
            p.resetColor();
        }
        for (Transition t : transitions) {
            t.resetColor();
        }
    }

    private Place findPlace(String name) {
        for (Place p : places) {
            if (p.name.equals(name)) return p;
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawGrid(g2d);

        for (Arc arc : arcs) {
            drawArc(g2d, arc);
        }

        for (Transition t : transitions) {
            t.draw(g2d);
        }

        for (Place p : places) {
            p.draw(g2d);
        }

        g2d.setColor(new Color(70, 70, 70));
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString(networkName, 20, 30);

        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Позиций: " + places.size() + " | Переходов: " + transitions.size() +
                " | Дуг: " + arcs.size(), 20, 50);
        g2d.drawString("Режим: " + getModeName(), 20, 70);

        String instruction = getModeInstruction();
        g2d.setColor(new Color(100, 100, 100));
        g2d.drawString(instruction, 20, getHeight() - 30);

        if (selectedPlace != null) {
            g2d.setColor(new Color(30, 144, 255, 100));
            g2d.fillOval(selectedPlace.x - 5, selectedPlace.y - 5, 50, 50);
            g2d.setColor(new Color(30, 144, 255));
            g2d.drawString("Выбрано: " + selectedPlace.name, getWidth() - 200, 30);
        }
        if (selectedTransition != null) {
            g2d.setColor(new Color(30, 144, 255, 100));
            g2d.fillRect(selectedTransition.x - 5, selectedTransition.y - 5, 40, 20);
            g2d.setColor(new Color(30, 144, 255));
            g2d.drawString("Выбрано: " + selectedTransition.name, getWidth() - 200, 30);
        }
    }

    private String getModeInstruction() {
        switch (currentMode) {
            case SELECT:
                return "Инструкция: Клик для выделения, перетаскивание для перемещения";
            case ADD_PLACE:
                return "Инструкция: Кликните в любом месте для добавления позиции";
            case ADD_TRANSITION:
                return "Инструкция: Кликните в любом месте для добавления перехода";
            case ADD_ARC:
                if (selectedPlace != null) {
                    return "Инструкция: Выбрана позиция '" + selectedPlace.name + "'. Теперь выберите переход для создания дуги";
                } else if (selectedTransition != null) {
                    return "Инструкция: Выбран переход '" + selectedTransition.name + "'. Теперь выберите позицию для создания дуги";
                } else {
                    return "Инструкция: Выберите начальный элемент (позицию или переход), затем конечный элемент";
                }
            case DELETE:
                return "Инструкция: Кликните на элемент для удаления";
            case MOVE:
                return "Инструкция: Перетаскивайте элемент для перемещения";
            default:
                return "";
        }
    }

    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(new Color(240, 240, 240));
        int gridSize = 20;
        for (int x = 0; x < getWidth(); x += gridSize) {
            g2d.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += gridSize) {
            g2d.drawLine(0, y, getWidth(), y);
        }
    }

    private void drawArc(Graphics2D g2d, Arc arc) {
        Place fromPlace = findPlace(arc.from);
        Transition fromTrans = findTransition(arc.from);
        Place toPlace = findPlace(arc.to);
        Transition toTrans = findTransition(arc.to);

        if (fromPlace == null && fromTrans == null) return;
        if (toPlace == null && toTrans == null) return;

        int x1 = 0, y1 = 0, x2 = 0, y2 = 0;

        if (fromPlace != null) {
            x1 = fromPlace.x + 20;
            y1 = fromPlace.y + 20;
        } else if (fromTrans != null) {
            x1 = fromTrans.x + 15;
            y1 = fromTrans.y + 5;
        }

        if (toPlace != null) {
            x2 = toPlace.x + 20;
            y2 = toPlace.y + 20;
        } else if (toTrans != null) {
            x2 = toTrans.x + 15;
            y2 = toTrans.y + 5;
        }

        Color arcColor = new Color(100, 100, 100, 180);

        if ((selectedPlace != null && ((fromPlace == selectedPlace) || (toPlace == selectedPlace))) ||
                (selectedTransition != null && ((fromTrans == selectedTransition) || (toTrans == selectedTransition)))) {
            arcColor = new Color(255, 69, 0, 200);
        }

        g2d.setColor(arcColor);

        g2d.setStroke(new BasicStroke(2f));
        g2d.drawLine(x1, y1, x2, y2);

        drawArrow(g2d, x1, y1, x2, y2);
    }

    private void drawArrow(Graphics2D g2d, int x1, int y1, int x2, int y2) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int arrowSize = 12;
        int x3 = (int) (x2 - arrowSize * Math.cos(angle - Math.PI / 6));
        int y3 = (int) (y2 - arrowSize * Math.sin(angle - Math.PI / 6));
        int x4 = (int) (x2 - arrowSize * Math.cos(angle + Math.PI / 6));
        int y4 = (int) (y2 - arrowSize * Math.sin(angle + Math.PI / 6));

        g2d.fillPolygon(new int[]{x2, x3, x4}, new int[]{y2, y3, y4}, 3);
    }

    private Transition findTransition(String name) {
        for (Transition t : transitions) {
            if (t.name.equals(name)) return t;
        }
        return null;
    }

    private String getModeName() {
        switch (currentMode) {
            case SELECT: return "Выбор/Перемещение";
            case ADD_PLACE: return "Добавление позиции";
            case ADD_TRANSITION: return "Добавление перехода";
            case ADD_ARC: return "Добавление дуги";
            case DELETE: return "Удаление элемента";
            case MOVE: return "Перемещение элемента";
            default: return "Неизвестный режим";
        }
    }
}
