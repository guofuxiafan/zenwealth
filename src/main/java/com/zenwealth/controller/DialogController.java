package com.zenwealth.controller;

import com.zenwealth.model.*;
import com.zenwealth.util.StockDatabase;
import com.zenwealth.util.StockDatabase.StockEntry;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class DialogController {
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField nameField;
    @FXML private Label extraLabel1, extraLabel2, extraLabel3;
    @FXML private TextField extraField1, extraField2, extraField3;
    @FXML private ListView<StockEntry> searchList;
    @FXML private GridPane formGrid;
    @FXML private StackPane rootPane;

    private static StockDatabase stockDb;
    private Asset editTarget;

    public static Asset showAddDialog(AssetType defaultType) throws IOException {
        DialogController ctrl = loadDialog();
        ctrl.typeCombo.setDisable(false);
        ctrl.typeCombo.setValue(label(defaultType));
        Optional<Asset> result = showAndWait(ctrl, "新增资产");
        return result.orElse(null);
    }

    public static Asset showEditDialog(Asset asset) throws IOException {
        DialogController ctrl = loadDialog();
        ctrl.editTarget = asset;
        ctrl.typeCombo.setValue(label(asset.type));
        ctrl.typeCombo.setDisable(true);
        ctrl.populateFrom(asset);
        Optional<Asset> result = showAndWait(ctrl, "修改资产");
        return result.orElse(null);
    }

    private static DialogController loadDialog() throws IOException {
        FXMLLoader loader = new FXMLLoader(
            DialogController.class.getResource("/com/zenwealth/view/dialog.fxml")
        );
        loader.load();
        DialogController ctrl = loader.getController();
        ctrl.typeCombo.setItems(javafx.collections.FXCollections.observableArrayList(
            "现金", "定存", "股票", "黄金ETF", "国债"
        ));
        ctrl.typeCombo.valueProperty().addListener((obs, o, n) -> ctrl.onTypeChanged());
        ctrl.hideAll();
        return ctrl;
    }

    private static Optional<Asset> showAndWait(DialogController ctrl, String title) {
        Dialog<Asset> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().setContent(ctrl.rootPane);
        ButtonType okType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        AtomicReference<Asset> assetRef = new AtomicReference<>();

        Button okButton = (Button) dialog.getDialogPane().lookupButton(okType);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                assetRef.set(ctrl.buildAsset());
            } catch (RuntimeException e) {
                new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> btn == okType ? assetRef.get() : null);

        return dialog.showAndWait();
    }

    private void onTypeChanged() {
        String type = typeCombo.getValue();
        if (type == null) return;
        hideAll();
        clearFields();
        hideSearchList();

        switch (type) {
            case "现金" -> {
                extraLabel1.setText("金额:");
                show(extraLabel1, extraField1);
            }
            case "定存" -> {
                extraLabel1.setText("金额:");
                show(extraLabel1, extraField1);
                extraLabel2.setText("年利率(%):");
                show(extraLabel2, extraField2);
            }
            case "股票" -> {
                setupStockSearch();
                extraLabel1.setText("搜索股票:");
                show(extraLabel1, extraField1);
                extraField1.setPromptText("输入名称或代码模糊搜索...");
                extraLayout2();
            }
            case "黄金ETF" -> {
                extraField1.textProperty().removeListener(onSearchChange);
                extraField1.setText("518880.SH");
                extraField1.setEditable(false);
                extraField1.setPromptText("");
                extraLabel1.setText("黄金ETF:");
                show(extraLabel1, extraField1);
                extraLayout2();
            }
            default -> {
                extraLabel1.setText("代码:");
                show(extraLabel1, extraField1);
                extraField1.setPromptText("");
                extraField1.setEditable(true);
                extraLayout2();
            }
        }
    }

    private void setupStockSearch() {
        if (stockDb == null) {
            stockDb = StockDatabase.getInstance();
        }
        extraField1.textProperty().removeListener(onSearchChange);
        extraField1.textProperty().addListener(onSearchChange);

        searchList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                StockEntry sel = searchList.getSelectionModel().getSelectedItem();
                if (sel != null) selectStockEntry(sel);
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hideSearchList();
                extraField1.requestFocus();
            }
        });
        searchList.setOnMouseClicked(e -> {
            StockEntry sel = searchList.getSelectionModel().getSelectedItem();
            if (sel != null) selectStockEntry(sel);
        });
    }

    private final javafx.beans.value.ChangeListener<String> onSearchChange =
        (obs, old, val) -> {
            if (stockDb == null) stockDb = StockDatabase.getInstance();
            if (val == null || val.isBlank()) {
                hideSearchList();
                return;
            }
            List<StockEntry> results = stockDb.search(val);
            if (results.isEmpty()) {
                hideSearchList();
                return;
            }
            searchList.getItems().setAll(results);
            double top = extraField1.getBoundsInParent().getMaxY() + 2;
            double left = extraField1.getBoundsInParent().getMinX();
            StackPane.setMargin(searchList, new Insets(top, 0, 0, left));
            searchList.setVisible(true);
            searchList.setManaged(true);
            searchList.setPrefHeight(Math.min(results.size() * 30 + 4, 180));
            searchList.getSelectionModel().selectFirst();
        };

    private void selectStockEntry(StockEntry entry) {
        extraField1.textProperty().removeListener(onSearchChange);
        extraField1.setText(entry.code());
        nameField.setText(entry.name());
        hideSearchList();
        setupStockSearch();
        extraField2.requestFocus();
    }

    private void clearFields() {
        extraField1.clear();
        extraField2.clear();
        extraField3.clear();
    }

    private void extraLayout2() {
        extraLabel2.setText("数量:");
        show(extraLabel2, extraField2);
        extraLabel3.setText("单价:");
        show(extraLabel3, extraField3);
    }

    private void hideSearchList() {
        searchList.setVisible(false);
        searchList.setManaged(false);
    }

    private void hideAll() {
        extraLabel1.setVisible(false);
        extraField1.setVisible(false);
        extraLabel2.setVisible(false);
        extraField2.setVisible(false);
        extraLabel3.setVisible(false);
        extraField3.setVisible(false);
    }

    private void show(Label lbl, TextField fld) {
        lbl.setVisible(true);
        fld.setVisible(true);
    }

    private void populateFrom(Asset a) {
        nameField.setText(a.name);
        if (a instanceof CashAsset ca) {
            extraField1.setText(String.valueOf(ca.amount));
        } else if (a instanceof DepositAsset da) {
            extraField1.setText(String.valueOf(da.amount));
            extraField2.setText(String.valueOf(da.interestRate));
        } else if (a instanceof EquityAsset ea) {
            extraField1.setText(ea.code);
            extraField2.setText(String.valueOf(ea.quantity));
            extraField3.setText(String.valueOf(ea.unitPrice));
        }
    }

    private Asset buildAsset() {
        String name = nameField.getText().trim();
        String type = typeCombo.getValue();
        if (name.isEmpty()) throw new RuntimeException("名称不能为空");

        if ("股票".equals(type)) {
            String code = extraField1.getText().trim();
            if (code.isEmpty()) throw new RuntimeException("请搜索并选择股票");
            if (stockDb == null) stockDb = StockDatabase.getInstance();
            if (!stockDb.isValidCode(code)) {
                throw new RuntimeException("股票代码 " + code + " 不在数据库中，请通过搜索选择");
            }
            return new EquityAsset(name, AssetType.STOCK,
                code,
                parseDouble(extraField2, "数量"),
                parseDouble(extraField3, "单价"));
        }

        return switch (type) {
            case "现金" -> new CashAsset(name, parseDouble(extraField1, "金额"));
            case "定存" -> new DepositAsset(name,
                parseDouble(extraField1, "金额"),
                parseDouble(extraField2, "年利率"));
            case "黄金ETF" -> new EquityAsset(name, AssetType.GOLD,
                "518880.SH",
                parseDouble(extraField2, "数量"),
                parseDouble(extraField3, "单价"));
            case "国债" -> new EquityAsset(name, AssetType.BOND,
                extraField1.getText().trim(),
                parseDouble(extraField2, "数量"),
                parseDouble(extraField3, "单价"));
            default -> throw new RuntimeException("未知类型: " + type);
        };
    }

    private static double parseDouble(TextField f, String fieldName) {
        String s = f.getText().trim();
        if (s.isEmpty()) throw new RuntimeException(fieldName + "不能为空");
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new RuntimeException(fieldName + "格式不正确");
        }
    }

    private static String label(AssetType t) {
        return switch (t) {
            case CASH -> "现金";
            case DEPOSIT -> "定存";
            case STOCK -> "股票";
            case GOLD -> "黄金ETF";
            case BOND -> "国债";
        };
    }
}
