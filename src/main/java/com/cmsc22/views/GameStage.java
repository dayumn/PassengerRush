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

    private static final int WINDOW_WIDTH  = 1380;
    private static final int WINDOW_HEIGHT = 800;
    private static final int CELL_SIZE     = 30;

    private Label jeepney1PointsLabel;
    private Label jeepney1LoadLabel;
    private Label jeepney2PointsLabel;
    private Label jeepney2LoadLabel;
    private Label gameClockLabel;

    // Shared across New Game clicks — always recreated fresh each time
    private static NetworkClient sharedNetworkClient = null;

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

        Image backgroundImage = new Image(getClass().getResourceAsStream("/assets/images/backgroundScene.png"));
        ImageView backgroundImageView = new ImageView(backgroundImage);

        this.canvas = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);

        jeepney1PointsLabel = createLabel("Total Points: 0");
        jeepney1LoadLabel   = createLabel("Current Load: 0/14");
        jeepney2PointsLabel = createLabel("Total Points: 0");
        jeepney2LoadLabel   = createLabel("Current Load: 0/14");
        gameClockLabel      = createLabel("00:00");

        VBox jeepney1Container = createVBox("Player 1", jeepney1PointsLabel, jeepney1LoadLabel);
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

        // Build GameTimer
        this.gametimer = new GameTimer(
                scene, jeepney1, jeepney2, mapGrid, CELL_SIZE, canvas,
                jeepney1PointsLabel, jeepney1LoadLabel,
                jeepney2PointsLabel, jeepney2LoadLabel,
                gameClockLabel, stage, titleScene);

        // Always disconnect old connection cleanly before making a new one
        if (sharedNetworkClient != null) {
            sharedNetworkClient.disconnect();
            sharedNetworkClient = null;
        }

        // Show stage first so "Searching for server..." is visible immediately
        this.stage.setScene(scene);
        this.stage.show();
        canvas.requestFocus();
        gametimer.registerScene(scene);
        this.gametimer.start(); // shows waiting screen until startGame() is called

        // AUTOMATIC DISCOVERY — runs in background so UI stays responsive
        Thread discoveryThread = new Thread(() -> {
            System.out.println("[GameStage] Searching for server on network...");

            // discoverServer() listens for UDP broadcast — waits up to 5 seconds
            String serverIP = ServerDiscovery.discoverServer();

            // Back on JavaFX thread to update game state
            Platform.runLater(() -> {
                if (serverIP != null) {
                    // Server found — connect
                    sharedNetworkClient = new NetworkClient(
                            serverIP, NetworkClient.DEFAULT_PORT, jeepney1, jeepney2);

                    if (sharedNetworkClient.connect()) {
                        sharedNetworkClient.startListening();
                        gametimer.setNetworkClient(sharedNetworkClient);

                        // FIX #1 — START signal triggers game timer on both clients
                        sharedNetworkClient.setGameStartListener(() -> gametimer.startGame());

                        // FIX #3 — FELL signal hides opponent jeepney
                        sharedNetworkClient.setFellListener(() -> gametimer.hideOpponent());

                        // CHAT — relay messages to the chat overlay
                        sharedNetworkClient.setChatListener((senderId, text) ->
                                gametimer.addChatMessage(senderId, text));

                        System.out.println("[GameStage] Multiplayer ON — connected to " + serverIP);
                    } else {
                        // Discovered but couldn't connect — fall back to solo
                        System.out.println("[GameStage] Could not connect to " + serverIP + " — solo mode");
                        sharedNetworkClient = null;
                        gametimer.startGame();
                    }
                } else {
                    // No server found on network — solo mode starts immediately
                    System.out.println("[GameStage] No server found — solo mode");
                    gametimer.startGame();
                }
            });
        }, "ServerDiscovery-Thread");

        discoveryThread.setDaemon(true);
        discoveryThread.start();
    }
}