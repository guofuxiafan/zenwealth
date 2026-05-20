package com.zenwealth.ui;

import com.zenwealth.model.AssetType;
import com.zenwealth.model.Portfolio;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class StackedBar extends VBox {
    private static final double BAR_HEIGHT = 28;
    private static final Map<AssetType, Color> COLORS = new LinkedHashMap<>();
    private static final Map<AssetType, Color> DARK_COLORS = new LinkedHashMap<>();
    private static final Map<AssetType, String> LABELS = new LinkedHashMap<>();
    private static final Map<AssetType, String> ICONS = new LinkedHashMap<>();
    private static final NumberFormat NF = NumberFormat.getCurrencyInstance(Locale.CHINA);

    static {
        COLORS.put(AssetType.CASH, Color.web("#10B981"));
        COLORS.put(AssetType.DEPOSIT, Color.web("#3B82F6"));
        COLORS.put(AssetType.STOCK, Color.web("#EC4899"));
        COLORS.put(AssetType.GOLD, Color.web("#F59E0B"));
        COLORS.put(AssetType.BOND, Color.web("#8B5CF6"));

        DARK_COLORS.put(AssetType.CASH, Color.web("#059669"));
        DARK_COLORS.put(AssetType.DEPOSIT, Color.web("#2563EB"));
        DARK_COLORS.put(AssetType.STOCK, Color.web("#DB2777"));
        DARK_COLORS.put(AssetType.GOLD, Color.web("#D97706"));
        DARK_COLORS.put(AssetType.BOND, Color.web("#7C3AED"));

        LABELS.put(AssetType.CASH, "现金");
        LABELS.put(AssetType.DEPOSIT, "定存");
        LABELS.put(AssetType.STOCK, "股票");
        LABELS.put(AssetType.GOLD, "黄金ETF");
        LABELS.put(AssetType.BOND, "国债");

        ICONS.put(AssetType.CASH, "●");
        ICONS.put(AssetType.DEPOSIT, "●");
        ICONS.put(AssetType.STOCK, "●");
        ICONS.put(AssetType.GOLD, "●");
        ICONS.put(AssetType.BOND, "●");
    }

    private final StackPane barPane;
    private final Label totalLabel;

    public StackedBar() {
        setSpacing(8);

        barPane = new StackPane();
        barPane.setPrefHeight(BAR_HEIGHT);
        barPane.setMaxHeight(BAR_HEIGHT);
        barPane.setMinHeight(BAR_HEIGHT);
        barPane.setStyle("-fx-background-color: #E5E7EB; -fx-background-radius: 6;");

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.08));
        shadow.setRadius(4);
        shadow.setOffsetY(1);
        barPane.setEffect(shadow);

        totalLabel = new Label();
        totalLabel.setFont(Font.font("System", 12));
        totalLabel.setStyle("-fx-text-fill: #6B7280;");
        totalLabel.setMaxWidth(Double.MAX_VALUE);
        totalLabel.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(barPane, totalLabel);
    }

    public void update(Portfolio portfolio) {
        barPane.getChildren().clear();

        double total = portfolio.getTotalMarketValue();
        if (total == 0) {
            Rectangle empty = new Rectangle(900, BAR_HEIGHT);
            empty.setFill(Color.web("#E5E7EB"));
            empty.setArcWidth(6);
            empty.setArcHeight(6);
            barPane.getChildren().add(empty);
            totalLabel.setText("");
            return;
        }

        totalLabel.setText("总资产: " + NF.format(total));

        Map<AssetType, Double> ratios = portfolio.getCategoryRatios();
        HBox segments = new HBox(0);
        segments.setMaxWidth(900);

        for (AssetType type : AssetType.values()) {
            double ratio = ratios.getOrDefault(type, 0.0);
            double value = portfolio.getCategoryValue(type);

            if (ratio < 0.005) continue;

            double width = Math.max(ratio * 900, 40);
            StackPane segment = new StackPane();
            segment.setPrefWidth(width);
            segment.setPrefHeight(BAR_HEIGHT);
            segment.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 0;",
                toHex(COLORS.get(type))
            ));

            Label innerLabel;
            if (width >= 60) {
                innerLabel = new Label(String.format("%s %.0f%%", LABELS.get(type), ratio * 100));
                innerLabel.setFont(Font.font("System", 10));
                innerLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            } else {
                innerLabel = new Label(String.format("%.0f%%", ratio * 100));
                innerLabel.setFont(Font.font("System", 9));
                innerLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            }
            segment.getChildren().add(innerLabel);

            String tip = String.format("%s: %s (%.1f%%)",
                LABELS.get(type), NF.format(value), ratio * 100);
            Tooltip.install(segment, new Tooltip(tip));

            segments.getChildren().add(segment);
        }

        barPane.getChildren().add(segments);

        Rectangle clip = new Rectangle(900, BAR_HEIGHT);
        clip.setArcWidth(6);
        clip.setArcHeight(6);
        segments.setClip(clip);
    }

    private static String toHex(Color c) {
        return String.format("#%02X%02X%02X",
            (int) (c.getRed() * 255),
            (int) (c.getGreen() * 255),
            (int) (c.getBlue() * 255));
    }
}
