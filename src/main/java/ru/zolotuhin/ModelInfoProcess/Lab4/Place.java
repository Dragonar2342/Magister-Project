package ru.zolotuhin.ModelInfoProcess.Lab4;

import java.awt.*;

public class Place {
    public String name;
    public int x, y;
    public int tokens;
    public Color baseColor;
    public Color activeColor;
    public Color borderColor = new Color(70, 70, 70);
    public boolean selected = false;

    public Place(String name, int x, int y, int tokens, Color color) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.tokens = tokens;
        this.baseColor = color;
        this.activeColor = null;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fillOval(x + 2, y + 2, 40, 40);

        Color fillColor = (activeColor != null) ? activeColor : baseColor;
        g2d.setColor(fillColor);
        g2d.fillOval(x, y, 40, 40);

        if (selected) {
            g2d.setColor(new Color(30, 144, 255));
            g2d.setStroke(new BasicStroke(3f));
        } else {
            g2d.setColor(borderColor);
            g2d.setStroke(new BasicStroke(2f));
        }
        g2d.drawOval(x, y, 40, 40);

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 11));

        String displayName = name;
        FontMetrics fm = g2d.getFontMetrics();

        if (fm.stringWidth(name) > 36) {
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

            while (fm.stringWidth(line1) > 36 && line1.length() > 3) {
                line1 = line1.substring(0, line1.length() - 1);
            }
            while (fm.stringWidth(line2) > 36 && line2.length() > 3) {
                line2 = line2.substring(0, line2.length() - 1);
            }

            if (line2.length() > 0) {
                int textWidth1 = fm.stringWidth(line1);
                int textWidth2 = fm.stringWidth(line2);
                g2d.drawString(line1, x + 20 - textWidth1/2, y + 12);
                g2d.drawString(line2, x + 20 - textWidth2/2, y + 24);
            } else {
                while (fm.stringWidth(line1 + "...") > 36 && line1.length() > 3) {
                    line1 = line1.substring(0, line1.length() - 1);
                }
                displayName = line1 + "...";
                int textWidth = fm.stringWidth(displayName);
                g2d.drawString(displayName, x + 20 - textWidth/2, y + 18);
            }
        } else {
            int textWidth = fm.stringWidth(displayName);
            g2d.drawString(displayName, x + 20 - textWidth/2, y + 18);
        }

        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        String tokenStr = String.valueOf(tokens);
        int textWidth = fm.stringWidth(tokenStr);
        g2d.drawString(tokenStr, x + 20 - textWidth/2, y + 38);

        if (tokens > 0) {
            g2d.setColor(new Color(255, 69, 0));
            int tokenRadius = 4;
            int maxVisibleTokens = 8; // Увеличили количество видимых меток
            int visibleTokens = Math.min(tokens, maxVisibleTokens);

            double angleStep = 2 * Math.PI / visibleTokens;
            for (int i = 0; i < visibleTokens; i++) {
                double angle = i * angleStep;
                int tokenX = x + 20 + (int)(15 * Math.cos(angle)) - tokenRadius;
                int tokenY = y + 20 + (int)(15 * Math.sin(angle)) - tokenRadius;
                g2d.fillOval(tokenX, tokenY, tokenRadius*2, tokenRadius*2);
            }

            if (tokens > maxVisibleTokens) {
                g2d.setColor(Color.RED);
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                g2d.drawString("+" + (tokens - maxVisibleTokens), x + 32, y + 15);
            }
        }

        g2d.setStroke(new BasicStroke(1f));
    }

    public boolean contains(int px, int py) {
        int centerX = x + 20;
        int centerY = y + 20;
        int radius = 20;
        return Math.pow(px - centerX, 2) + Math.pow(py - centerY, 2) <= radius * radius;
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
