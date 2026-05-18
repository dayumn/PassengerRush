package com.cmsc22.views;

import com.cmsc22.models.*;
import com.cmsc22.controllers.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class GameStage {

    private Scene scene;
    private Stage stage;
    private Canvas canvas;

    private Jeepney jeepney1;
    private Jeepney jeepney2;
    private GameTimer gametimer;
    private Image backgroundImage;


    private static final int CELL_SIZE     = 30;

    private Label jeepney1PointsLabel;
    private Label jeepney1LoadLabel;
    private Label jeepney2PointsLabel;
    private Label jeepney2LoadLabel;
    private Label gameClockLabel;

    // The active network connection passed from LobbyStage
    private NetworkClient networkClient;

    private final int[][] mapGrid = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 3, 3, 3, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 9, 9, 9, 9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
            {0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
            {0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0},
            {0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0},
            {0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0},
            {0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0},
            {0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 6, 6, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0},
            {0, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 0},
            {0, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 0},
            {0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0},
            {0, 1, 1, 4, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 8, 1, 1, 0},
            {0, 1, 1, 4, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 8, 1, 1, 0},
            {0, 1, 1, 4, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 8, 1, 1, 0},
            {0, 1, 1, 4, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 8, 1, 1, 0},
            {0, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
            {0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 1, 0},
            {0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 1, 0},
            {0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 1, 0},
            {0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 1, 0},
            {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
            {0, 0, 5, 5, 5, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 7, 7, 7, 0, 0}
    };

    public GameStage(NetworkClient networkClient) {
        this.networkClient = networkClient;
        Image jeep1Up    = new Image(getClass().getResourceAsStream("/assets/images/Jeep1U.png"));
        Image jeep1Down  = new Image(getClass().getResourceAsStream("/assets/images/Jeep1D.png"));
        Image jeep1Left  = new Image(getClass().getResourceAsStream("/assets/images/Jeep1L.png"));
        Image jeep1Right = new Image(getClass().getResourceAsStream("/assets/images/Jeep1R.png"));
        this.jeepney1 = new Jeepney(0, 0, "My Jeepney", jeep1Up, jeep1Down, jeep1Left, jeep1Right);

        Image jeep2Up    = new Image(getClass().getResourceAsStream("/assets/images/Jeep2U.png"));
        Image jeep2Down  = new Image(getClass().getResourceAsStream("/assets/images/Jeep2D.png"));
        Image jeep2Left  = new Image(getClass().getResourceAsStream("/assets/images/Jeep2L.png"));
        Image jeep2Right = new Image(getClass().getResourceAsStream("/assets/images/Jeep2R.png"));
        this.jeepney2 = new Jeepney(0, 0, "Your Jeepney", jeep2Up, jeep2Down, jeep2Left, jeep2Right);

        this.backgroundImage = new Image(getClass().getResourceAsStream("/assets/images/backgroundScene.png"));

        javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        double screenWidth = bounds.getWidth();
        double screenHeight = bounds.getHeight();

        this.canvas = new Canvas(screenWidth, screenHeight);

        jeepney1PointsLabel = createLabel("Total Points: 0");
        jeepney1LoadLabel   = createLabel("Current Load: 0/14");
        jeepney2PointsLabel = createLabel("Total Points: 0");
        jeepney2LoadLabel   = createLabel("Current Load: 0/14");
        gameClockLabel      = createLabel("00:00");

        VBox jeepney1Container = createVBox("Player 1", jeepney1PointsLabel, jeepney1LoadLabel);
        VBox jeepney2Container = createVBox("Player 2", jeepney2PointsLabel, jeepney2LoadLabel);



        BackgroundFill backgroundFill = new BackgroundFill(
                Color.rgb(0, 0, 0, 0.3), new CornerRadii(5), new Insets(-8));
        Background background = new Background(backgroundFill);
        jeepney1Container.setBackground(background);
        jeepney2Container.setBackground(background);
        gameClockLabel.setBackground(background);

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        HBox topContainer = new HBox(jeepney1Container, spacer1, gameClockLabel, spacer2, jeepney2Container);
        topContainer.setAlignment(Pos.TOP_CENTER);
        topContainer.setPadding(new Insets(10, 20, 10, 20)); // Add padding on sides so text isn't glued to edges
        topContainer.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane stackPane = new StackPane();
        stackPane.getChildren().addAll(canvas, topContainer);
        StackPane.setAlignment(topContainer, Pos.TOP_CENTER);

        this.scene = new Scene(stackPane, screenWidth, screenHeight);
        canvas.setFocusTraversable(true);
    }

    private VBox createVBox(String title, Label... labels) {
        VBox vbox = new VBox(5);
        vbox.getChildren().add(createLabel(title));
        vbox.getChildren().addAll(labels);
        vbox.setAlignment(Pos.TOP_LEFT);
        return vbox;
    }

    private Label createLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font("Press Start 2P", 16));
        return label;
    }

    public void setStage(Stage primaryStage, Scene titleScene) {
        this.stage = primaryStage;
        this.stage.setTitle("Jeepney Game");

        // Build GameTimer
        this.gametimer = new GameTimer(
                scene, jeepney1, jeepney2, mapGrid, CELL_SIZE, canvas,
                jeepney1PointsLabel, jeepney1LoadLabel,
                jeepney2PointsLabel, jeepney2LoadLabel,
                gameClockLabel, stage, titleScene, backgroundImage);

        // Show stage first
        this.stage.setScene(scene);
        this.stage.show();
        canvas.requestFocus();
        gametimer.registerScene(scene);
        this.gametimer.start(); 

        // If we came from the Lobby, the START signal was already received, so we can start immediately
        if (this.networkClient != null) {
            this.networkClient.setJeepneys(jeepney1, jeepney2);
            this.gametimer.setNetworkClient(this.networkClient);
            this.networkClient.setFellListener(() -> gametimer.hideOpponent());
            
            // CHAT — relay messages to the chat overlay
            this.networkClient.setChatListener((senderId, text) ->
                    gametimer.addChatMessage(senderId, text));

            this.gametimer.startGame();
        } else {
            // Solo mode
            this.gametimer.startGame();
        }
    }
}