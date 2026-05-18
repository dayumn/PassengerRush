package com.cmsc22.views;

import com.cmsc22.models.*;
import com.cmsc22.views.*;
import com.cmsc22.controllers.*;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class GameOverScene {

    public GameOverScene(Jeepney winner, Stage primaryStage, Scene titleScene) {
        StackPane layout = new StackPane();
        String imagePath;

        if (winner == null) {
            imagePath = "tie.png";
        } else {
            imagePath = winner.getName().equals("My Jeepney") ? "myjeep.png" : "yourjeep.png";
        }

        Image gameOverImage = new Image(getClass().getResourceAsStream("/assets/images/" + imagePath));
        ImageView gameOverImageView = new ImageView(gameOverImage);
        gameOverImageView.setFitWidth(primaryStage.getWidth()); 
        gameOverImageView.setFitHeight(primaryStage.getHeight());
        gameOverImageView.setPreserveRatio(false); 

        layout.getChildren().add(gameOverImageView);

        Button backButton = new Button("Back to Menu");
        backButton.setMinWidth(200);
        backButton.setMinHeight(50);
        backButton.setTranslateY(120); 
        backButton.setOpacity(0); 
        backButton.setOnAction(e -> primaryStage.setScene(titleScene));

        layout.getChildren().add(backButton);
        StackPane.setAlignment(backButton, Pos.CENTER); 

        Scene gameOverScene = new Scene(layout, primaryStage.getWidth(), primaryStage.getHeight());
        primaryStage.setScene(gameOverScene);
        primaryStage.show();
    }
}