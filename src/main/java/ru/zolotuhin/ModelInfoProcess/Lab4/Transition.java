package ru.zolotuhin.ModelInfoProcess.Lab4;

import java.awt.*;

public class Transition {
    public String name;
    public int x, y;
    public Color baseColor;
    public Color activeColor;
    public boolean selected = false;

    public Transition(String name, int x, int y, Color color) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.baseColor = color;
        this.activeColor = null;
    }

    public void draw(Graphics2D g2d) {
        // Тень
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fillRect(x + 2, y + 2, 30, 10);

        Color fillColor = (activeColor != null) ? activeColor : baseColor;
        g2d.setColor(fillColor);
        g2d.fillRect(x, y, 30, 10);

        if (selected) {
            g2d.setColor(new Color(30, 144, 255));
            g2d.setStroke(new BasicStroke(3f));
        } else {
            g2d.setColor(new Color(70, 70, 70));
            g2d.setStroke(new BasicStroke(1.5f));
        }
        g2d.drawRect(x, y, 30, 10);

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        FontMetrics fm = g2d.getFontMetrics();

        String displayName = name;

        if (fm.stringWidth(name) > 40) {
            int mid = name.length() / 2;
            for (int i = mid; i < name.length(); i++) {
                if (name.charAt(i) == '_' || name.charAt(i) == ' ') {
                    mid = i;
                    break;
                }
            }

            String line1 = name.substring(0, mid);
            String line2 = name.substring(mid).trim();
            if (line2.startsWith("_")) line2 = line2.substring(1);

            while (fm.stringWidth(line1) > 40 && line1.length() > 3) {
                line1 = line1.substring(0, line1.length() - 1);
            }
            while (fm.stringWidth(line2) > 40 && line2.length() > 3) {
                line2 = line2.substring(0, line2.length() - 1);
            }

            if (line2.length() > 0) {
                int textWidth1 = fm.stringWidth(line1);
                int textWidth2 = fm.stringWidth(line2);
                g2d.drawString(line1, x + 15 - textWidth1/2, y - 8);
                g2d.drawString(line2, x + 15 - textWidth2/2, y + 22);
            } else {
                while (fm.stringWidth(line1 + "...") > 40 && line1.length() > 3) {
                    line1 = line1.substring(0, line1.length() - 1);
                }
                displayName = line1 + "...";
                int textWidth = fm.stringWidth(displayName);
                g2d.drawString(displayName, x + 15 - textWidth/2, y - 3);
            }
        } else {
            int textWidth = fm.stringWidth(displayName);
            g2d.drawString(displayName, x + 15 - textWidth/2, y - 3);
        }

        if (activeColor != null && activeColor.equals(new Color(255, 215, 0))) {
            long time = System.currentTimeMillis() % 1000;
            double pulse = 0.5 + 0.5 * Math.sin(time * 2 * Math.PI / 1000);
            int alpha = (int)(150 + 100 * pulse);
            g2d.setColor(new Color(255, 255, 0, alpha));
            g2d.fillRect(x - 3, y - 3, 36, 16);
        }

        g2d.setStroke(new BasicStroke(1f));
    }

    public boolean contains(int px, int py) {
        return px >= x && px <= x + 30 && py >= y && py <= y + 10;
    }

    public void setActiveColor(Color color) {
        this.activeColor = color;
    }

    public void resetColor() {
        this.activeColor = null;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
