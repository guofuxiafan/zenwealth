package com.zenwealth;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ZenWealthApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/zenwealth/view/main.fxml")
        );
        Scene scene = new Scene(loader.load(), 960, 640);
        stage.setTitle("ZenWealth - Personal Wealth Management");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
