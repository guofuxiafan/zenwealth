package com.zenwealth.controller;

import com.zenwealth.model.*;
import com.zenwealth.service.*;
import com.zenwealth.ui.StackedBar;
import com.zenwealth.util.Constants;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class MainController {
    @FXML private Label totalLabel;
    @FXML private ListView<String> sidebarList;
    @FXML private Button refreshButton;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private StackPane tableArea;
    @FXML private StackPane barArea;
    @FXML private Label sectionTitle;

    private final Portfolio portfolio = new Portfolio();
    private final StorageManager storage = new StorageManager();
    private final TushareService tushare = new TushareService();
    private TableView<Asset> tableView;
    private AssetType selectedType = AssetType.CASH;
    private static final NumberFormat NF = NumberFormat.getCurrencyInstance(Locale.CHINA);

    private static final Map<AssetType, Color> TYPE_COLORS = new LinkedHashMap<>();
    private static final Map<AssetType, String> TYPE_LABELS = new LinkedHashMap<>();

    static {
        TYPE_COLORS.put(AssetType.CASH, Color.web("#10B981"));
        TYPE_COLORS.put(AssetType.DEPOSIT, Color.web("#3B82F6"));
        TYPE_COLORS.put(AssetType.STOCK, Color.web("#EC4899"));
        TYPE_COLORS.put(AssetType.GOLD, Color.web("#F59E0B"));
        TYPE_COLORS.put(AssetType.BOND, Color.web("#8B5CF6"));

        TYPE_LABELS.put(AssetType.CASH, "现金");
        TYPE_LABELS.put(AssetType.DEPOSIT, "定存");
        TYPE_LABELS.put(AssetType.STOCK, "股票");
        TYPE_LABELS.put(AssetType.GOLD, "黄金ETF");
        TYPE_LABELS.put(AssetType.BOND, "国债");
    }

    @FXML
    public void initialize() {
        loadData();
        setupSidebar();
        setupTableView();
        setupButtons();
        setupStackedBar();
        refreshDisplay();
        sidebarList.getSelectionModel().selectFirst();
    }

    private void loadData() {
        try {
            Portfolio loaded = storage.load(Constants.DATA_FILE);
            loaded.getAssets().forEach(portfolio::addAsset);
        } catch (IOException e) {
            System.err.println("Failed to load assets: " + e.getMessage());
        }
    }

    private void saveData() {
        try {
            storage.save(portfolio, Constants.DATA_FILE);
        } catch (IOException e) {
            System.err.println("Failed to save: " + e.getMessage());
        }
    }

    private void setupSidebar() {
        for (AssetType t : AssetType.values()) {
            sidebarList.getItems().add(TYPE_LABELS.get(t));
        }
        sidebarList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                AssetType type = AssetType.values()[getIndex()];
                Color c = TYPE_COLORS.get(type);
                double catValue = portfolio.getCategoryValue(type);

                Circle dot = new Circle(4, c);
                Label nameLbl = new Label("  " + item);
                nameLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151;");

                Label valLbl = new Label(NF.format(catValue));
                valLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

                HBox cell = new HBox(5, dot, nameLbl);
                cell.setAlignment(Pos.CENTER_LEFT);

                VBox box = new VBox(2, cell, valLbl);
                box.setPadding(new Insets(6, 12, 6, 12));
                setGraphic(box);
            }
        });

        sidebarList.getSelectionModel().selectedIndexProperty().addListener(
            (obs, old, newIdx) -> {
                if (newIdx.intValue() >= 0) {
                    selectedType = AssetType.values()[newIdx.intValue()];
                    sectionTitle.setText(TYPE_LABELS.get(selectedType) + " 持仓明细");
                    rebuildTable();
                    refreshSidebar();
                }
            }
        );
    }

    private void refreshSidebar() {
        sidebarList.refresh();
    }

    @SuppressWarnings("unchecked")
    private void setupTableView() {
        tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setPlaceholder(new Label("暂无持仓数据，点击 [+ 新增] 添加资产"));
        tableView.setStyle("-fx-table-cell-border-color: transparent; -fx-background-color: transparent;");
        tableArea.getChildren().add(tableView);
    }

    @SuppressWarnings("unchecked")
    private void rebuildTable() {
        tableView.getColumns().clear();
        tableView.getItems().clear();

        if (selectedType == AssetType.CASH || selectedType == AssetType.DEPOSIT) {
            TableColumn<Asset, String> nameCol = new TableColumn<>("名称");
            nameCol.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().name));
            TableColumn<Asset, String> amountCol = new TableColumn<>("金额 (¥)");
            amountCol.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(fmt(
                    selectedType == AssetType.CASH
                        ? ((CashAsset) d.getValue()).amount
                        : ((DepositAsset) d.getValue()).amount
                )));
            TableColumn<Asset, String> ratioCol = new TableColumn<>("占比");
            ratioCol.setCellValueFactory(d -> ratioCell(d.getValue()));
            tableView.getColumns().addAll(nameCol, amountCol);

            if (selectedType == AssetType.DEPOSIT) {
                TableColumn<Asset, String> rateCol = new TableColumn<>("年利率(%)");
                rateCol.setCellValueFactory(d ->
                    new javafx.beans.property.SimpleStringProperty(
                        String.format("%.2f%%", ((DepositAsset) d.getValue()).interestRate)
                    ));
                tableView.getColumns().add(rateCol);
            }
            tableView.getColumns().add(ratioCol);
        } else {
            TableColumn<Asset, String> codeCol = new TableColumn<>("代码");
            codeCol.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                    ((EquityAsset) d.getValue()).code));
            TableColumn<Asset, String> nameCol = new TableColumn<>("名称");
            nameCol.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().name));
            TableColumn<Asset, String> qtyCol = new TableColumn<>("数量");
            qtyCol.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                    String.valueOf(((EquityAsset) d.getValue()).quantity)
                ));
            TableColumn<Asset, String> priceCol = new TableColumn<>("单价 (¥)");
            priceCol.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                    NF.format(((EquityAsset) d.getValue()).unitPrice)
                ));
            TableColumn<Asset, String> valueCol = new TableColumn<>("市值 (¥)");
            valueCol.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(fmt(d.getValue().getMarketValue())));
            TableColumn<Asset, String> ratioCol = new TableColumn<>("占比");
            ratioCol.setCellValueFactory(d -> ratioCell(d.getValue()));

            tableView.getColumns().addAll(codeCol, nameCol, qtyCol, priceCol, valueCol, ratioCol);
        }

        tableView.setItems(FXCollections.observableArrayList(
            portfolio.getAssetsByType(selectedType)
        ));
    }

    private javafx.beans.property.SimpleStringProperty ratioCell(Asset a) {
        double total = portfolio.getTotalMarketValue();
        double pct = total > 0 ? a.getMarketValue() / total * 100 : 0;
        return new javafx.beans.property.SimpleStringProperty(String.format("%.1f%%", pct));
    }

    private void setupStackedBar() {
        StackedBar bar = new StackedBar();
        bar.update(portfolio);
        barArea.getChildren().add(bar);
    }

    private void updateStackedBar() {
        if (!barArea.getChildren().isEmpty()) {
            ((StackedBar) barArea.getChildren().get(0)).update(portfolio);
        }
    }

    private void setupButtons() {
        addButton.setOnAction(e -> onAdd());
        editButton.setOnAction(e -> onEdit());
        deleteButton.setOnAction(e -> onDelete());
        refreshButton.setOnAction(e -> onRefresh());
    }

    private void onAdd() {
        try {
            Asset asset = DialogController.showAddDialog(selectedType);
            if (asset != null) {
                portfolio.addAsset(asset);
                saveData();
                rebuildTable();
                updateStackedBar();
                refreshTotal();
                refreshSidebar();
            }
        } catch (Exception ex) {
            showError("添加失败: " + ex.getMessage());
        }
    }

    private void onEdit() {
        Asset selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("请先选中一条持仓");
            return;
        }
        int index = portfolio.getAssets().indexOf(selected);
        if (index < 0) return;
        try {
            Asset updated = DialogController.showEditDialog(selected);
            if (updated != null) {
                portfolio.replaceAsset(index, updated);
                saveData();
                rebuildTable();
                updateStackedBar();
                refreshTotal();
                refreshSidebar();
            }
        } catch (Exception ex) {
            showError("修改失败: " + ex.getMessage());
        }
    }

    private void onDelete() {
        Asset selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("请先选中一条持仓");
            return;
        }
        int index = portfolio.getAssets().indexOf(selected);
        if (index < 0) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "确认删除 " + selected.name + " ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                portfolio.removeAsset(index);
                saveData();
                rebuildTable();
                updateStackedBar();
                refreshTotal();
                refreshSidebar();
            }
        });
    }

    private void onRefresh() {
        refreshButton.setDisable(true);
        refreshButton.setText("刷新中...");
        new Thread(() -> {
            try {
                for (Asset a : portfolio.getAssets()) {
                    if (a instanceof EquityAsset ea) {
                        try {
                            ea.unitPrice = tushare.fetchPrice(ea.code, ea.type);
                        } catch (IOException e) {
                            System.err.println("Failed: " + ea.code + " - " + e.getMessage());
                        }
                    }
                }
                Platform.runLater(() -> {
                    saveData();
                    rebuildTable();
                    updateStackedBar();
                    refreshTotal();
                    refreshSidebar();
                    refreshButton.setDisable(false);
                    refreshButton.setText("刷新");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("刷新失败: " + e.getMessage());
                    refreshButton.setDisable(false);
                    refreshButton.setText("刷新");
                });
            }
        }).start();
    }

    private void refreshDisplay() {
        sectionTitle.setText(TYPE_LABELS.get(selectedType) + " 持仓明细");
        rebuildTable();
        refreshTotal();
    }

    private void refreshTotal() {
        totalLabel.setText("总资产: " + fmt(portfolio.getTotalMarketValue()));
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private static String fmt(double v) {
        return NF.format(v);
    }
}
