package com.cmsc22.views;

import com.cmsc22.models.*;
import com.cmsc22.controllers.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
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

    private static final int WINDOW_WIDTH = 1380;
    private static final int WINDOW_HEIGHT = 800;
    private static final int CELL_SIZE = 30;

    private Label jeepney1PointsLabel;
    private Label jeepney1LoadLabel;
    private Label jeepney2PointsLabel;
    private Label jeepney2LoadLabel;
    private Label gameClockLabel;

    private static NetworkClient sharedNetworkClient = null;


    private static final String SERVER_IP = "10.12.34.66";

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

    public GameStage() {
        // --- Jeepney setup ---
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

        // --- Background ---
        Image backgroundImage = new Image(getClass().getResourceAsStream("/assets/images/backgroundScene.png"));
        ImageView backgroundImageView = new ImageView(backgroundImage);

        // --- Canvas ---
        this.canvas = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);

        // --- Labels (must be created BEFORE GameTimer) ---
        jeepney1PointsLabel = createLabel("Total Points: 0");
        jeepney1LoadLabel   = createLabel("Current Load: 0/14");
        jeepney2PointsLabel = createLabel("Total Points: 0");
        jeepney2LoadLabel   = createLabel("Current Load: 0/14");
        gameClockLabel      = createLabel("00:00");

        // --- HUD layout ---
        VBox jeepney1Container = createVBox("Player 1",   jeepney1PointsLabel, jeepney1LoadLabel);
        VBox jeepney2Container = createVBox("Player 2", jeepney2PointsLabel, jeepney2LoadLabel);

        jeepney1Container.setMaxWidth(300);
        jeepney2Container.setMaxWidth(300);
        jeepney1Container.setMaxHeight(50);
        jeepney2Container.setMaxHeight(50);

        BackgroundFill backgroundFill = new BackgroundFill(
                Color.rgb(0, 0, 0, 0.3), new CornerRadii(5), new Insets(-8));
        Background background = new Background(backgroundFill);
        jeepney1Container.setBackground(background);
        jeepney2Container.setBackground(background);
        gameClockLabel.setBackground(background);

        HBox topContainer = new HBox(530, jeepney1Container, gameClockLabel, jeepney2Container);
        topContainer.setAlignment(Pos.TOP_CENTER);
        topContainer.setPadding(new Insets(10, 0, 10, 0));

        StackPane stackPane = new StackPane();
        stackPane.getChildren().addAll(backgroundImageView, canvas, topContainer);
        StackPane.setAlignment(topContainer, Pos.TOP_CENTER);

        this.scene = new Scene(stackPane, WINDOW_WIDTH, WINDOW_HEIGHT);

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

        // Build GameTimer first
        this.gametimer = new GameTimer(
                scene, jeepney1, jeepney2, mapGrid, CELL_SIZE, canvas,
                jeepney1PointsLabel, jeepney1LoadLabel,
                jeepney2PointsLabel, jeepney2LoadLabel,
                gameClockLabel, stage, titleScene);

        // FIX #5 — always disconnect old connection cleanly before making a new one.
        // We can't reuse the old client because it holds stale jeepney references
        // from the previous GameStage instance. Always create a fresh connection.
        if (sharedNetworkClient != null) {
            sharedNetworkClient.disconnect();
            sharedNetworkClient = null;
        }

        sharedNetworkClient = new NetworkClient(
                SERVER_IP, NetworkClient.DEFAULT_PORT, jeepney1, jeepney2);

        if (sharedNetworkClient.connect()) {
            sharedNetworkClient.startListening();
            gametimer.setNetworkClient(sharedNetworkClient);

            // FIX #1 — register listener so START signal triggers game begin
            sharedNetworkClient.setGameStartListener(() -> gametimer.startGame());

            // FIX #3 — register listener so FELL signal hides opponent
            sharedNetworkClient.setFellListener(() -> gametimer.hideOpponent());

            System.out.println("[GameStage] Multiplayer mode ON");
        } else {
            // Server not reachable — run in solo mode, start immediately
            System.out.println("[GameStage] Server not found — solo mode");
            sharedNetworkClient = null;
            gametimer.startGame(); // solo mode starts right away
        }

        this.stage.setScene(scene);
        this.stage.show();
        canvas.requestFocus();
        gametimer.registerScene(scene);
        this.gametimer.start();
    }
}