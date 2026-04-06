package com.cmsc22.views;

import com.cmsc22.models.*;
import com.cmsc22.views.*;
import com.cmsc22.controllers.*;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class Main extends Application {

    private Stage primaryStage; // Store the primaryStage
    private Scene titleScene;   // Store the titleScene

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        try {
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            double screenWidth = screenBounds.getWidth();
            double screenHeight = screenBounds.getHeight();

            titleScene = createTitleScene(screenWidth, screenHeight);
            primaryStage.setTitle("Main Menu");
            primaryStage.setScene(titleScene);
            primaryStage.setMaximized(true);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Scene createTitleScene(double screenWidth, double screenHeight) {
        StackPane layout = new StackPane();
        ImageView background = createBackgroundImage( "title.png", screenWidth, screenHeight);

        Button newGameButton = createButton("New Game", 20, e -> {
            GameStage gameStage = new GameStage();
            gameStage.setStage(primaryStage, titleScene); // Pass titleScene
        });
        Button aboutButton = createButton("About", 125, e -> primaryStage.setScene(createAboutScene(titleScene, screenWidth, screenHeight)));
        Button developersButton = createButton("Developers", 250, e -> primaryStage.setScene(createDevelopersScene(titleScene, screenWidth, screenHeight)));

        layout.getChildren().addAll(background, newGameButton, aboutButton, developersButton);
        return new Scene(layout, screenWidth, screenHeight);
    }

    private Button createButton(String text, double translateY, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button button = new Button(text);
        button.setMinWidth(200);
        button.setMinHeight(50);
        button.setTranslateY(translateY);
        button.setOpacity(0);
        button.setOnAction(action);
        return button;
    }


    private Scene createAboutScene(Scene previousScene, double screenWidth, double screenHeight) {
        StackPane layout = new StackPane();
        ImageView background = createBackgroundImage("about.png", screenWidth, screenHeight);
        Button backButton = createButton("Back", 275, e -> primaryStage.setScene(previousScene));
        layout.getChildren().addAll(background, backButton);
        return new Scene(layout, screenWidth, screenHeight);
    }


    private Scene createDevelopersScene(Scene previousScene, double screenWidth, double screenHeight) {
        StackPane layout = new StackPane();
        ImageView background = createBackgroundImage("developers.png", screenWidth, screenHeight);
        Button backButton = createButton("Back", 275, e -> primaryStage.setScene(previousScene));
        layout.getChildren().addAll(background, backButton);
        return new Scene(layout, screenWidth, screenHeight);
    }

    private ImageView createBackgroundImage(String imagePath, double screenWidth, double screenHeight) {
        Image image = new Image(getClass().getResourceAsStream("/assets/images/" + imagePath.trim()));
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(screenWidth);
        imageView.setFitHeight(screenHeight);
        imageView.setPreserveRatio(false);
        return imageView;
    }

    public static void main(String[] args) {
        launch(args);
    }
}