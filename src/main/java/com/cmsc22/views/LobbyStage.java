package com.cmsc22.views;

import com.cmsc22.controllers.NetworkClient;
import com.cmsc22.controllers.ServerDiscovery;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.effect.DropShadow;
import javafx.stage.Stage;

public class LobbyStage {

    private Stage stage;
    private Scene scene;


    private NetworkClient networkClient;
    private boolean isReady = false;

    public void setStage(Stage primaryStage, Scene titleScene) {
        this.stage = primaryStage;
        
        Font.loadFont(getClass().getResourceAsStream("/assets/fonts/PressStart2P-Regular.ttf"), 16);

        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(60));

        javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        double screenWidth = bounds.getWidth();
        double screenHeight = bounds.getHeight();

        Image backgroundImage = new Image(getClass().getResourceAsStream("/assets/images/title.png"));
        ImageView backgroundImageView = new ImageView(backgroundImage);
        backgroundImageView.setFitWidth(screenWidth);
        backgroundImageView.setFitHeight(screenHeight);
        backgroundImageView.setPreserveRatio(false);
        
        Rectangle overlay = new Rectangle(screenWidth, screenHeight, Color.rgb(15, 15, 40, 0.65));

        String panelStyle = "-fx-background-color: rgba(20, 20, 45, 0.95); " +
                            "-fx-border-color: white; " +
                            "-fx-border-width: 4px; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 0, 0, 10, 10);";

        // --- Player List (Left) ---
        VBox playerListContainer = new VBox(20);
        playerListContainer.setPadding(new Insets(30));
        playerListContainer.setPrefWidth(350);
        playerListContainer.setStyle(panelStyle);
        
        Label playersLabel = createLabel("PLAYERS");
        playersLabel.setTextFill(Color.web("#FAD02C")); 
        
        Label player1 = createLabel("Player 1 (Connecting...)");
        Label player2 = createLabel("Waiting for Player 2...");
        player2.setTextFill(Color.web("#aaaaaa")); 
        
        playerListContainer.getChildren().addAll(playersLabel, player1, player2);

        // --- Chat Interface (Center) ---
        VBox chatContainer = new VBox(15);
        chatContainer.setPadding(new Insets(30));
        chatContainer.setStyle(panelStyle);
        BorderPane.setMargin(chatContainer, new Insets(0, 0, 0, 40));
        
        Label chatLabel = createLabel("LOBBY CHAT");
        chatLabel.setTextFill(Color.web("#FAD02C"));
        
        TextArea chatHistory = new TextArea();
        chatHistory.setEditable(false);
        chatHistory.setWrapText(true);
        chatHistory.setPrefHeight(450);
        chatHistory.setStyle("-fx-control-inner-background: #0a0a1a; -fx-text-fill: white; -fx-font-family: 'Press Start 2P'; -fx-font-size: 12px; -fx-background-color: transparent; -fx-border-color: #444; -fx-border-width: 2px;");
        chatHistory.appendText("System: Welcome to the Lobby!\n");

        HBox chatInputArea = new HBox(10);
        TextField chatInput = new TextField();
        chatInput.setPromptText("Type a message...");
        chatInput.setPrefHeight(50);
        chatInput.setStyle("-fx-background-color: #111; -fx-text-fill: white; -fx-font-family: 'Press Start 2P'; -fx-font-size: 14px; -fx-prompt-text-fill: #888; -fx-border-color: white; -fx-border-width: 3px;");
        
        Button sendButton = createStyledButton("Send", "#444", "#666");
        sendButton.setPrefHeight(50);
        
        Runnable sendMessage = () -> {
            String text = chatInput.getText().trim();
            if (!text.isEmpty()) {
                if (networkClient != null && networkClient.isConnected()) {
                    networkClient.sendChat(text);
                } else {
                    chatHistory.appendText("System: Not connected.\n");
                }
                chatInput.clear();
            }
        };
        sendButton.setOnAction(e -> sendMessage.run());
        chatInput.setOnAction(e -> sendMessage.run()); 
        
        HBox.setHgrow(chatInput, Priority.ALWAYS);
        chatInputArea.getChildren().addAll(chatInput, sendButton);
        chatContainer.getChildren().addAll(chatLabel, chatHistory, chatInputArea);

        // --- Controls (Bottom) ---
        HBox controlsContainer = new HBox(40);
        controlsContainer.setAlignment(Pos.CENTER);
        controlsContainer.setPadding(new Insets(40, 0, 0, 0));
        
        Button backButton = createStyledButton("Back to Menu", "#444", "#666");
        backButton.setOnAction(e -> {
            if (networkClient != null) networkClient.disconnect();
            stage.setScene(titleScene);
        });
        
        Button readyButton = createStyledButton("Ready", "#e6a100", "#ffb81c");
        readyButton.setOnAction(e -> {
            if (networkClient != null && !isReady) {
                networkClient.sendReady();
                isReady = true;
                readyButton.setDisable(true);
                readyButton.setText("Ready!");
            }
        });
        
        Button startButton = createStyledButton("Start Game", "#4CAF50", "#45a049");
        startButton.setDisable(true); 
        startButton.setOnAction(e -> {
            if (networkClient != null) {
                networkClient.sendStartGame();
            }
        });
        
        controlsContainer.getChildren().addAll(backButton, readyButton, startButton);

        layout.setLeft(playerListContainer);
        layout.setCenter(chatContainer);
        layout.setBottom(controlsContainer);

        StackPane root = new StackPane();
        root.getChildren().addAll(backgroundImageView, overlay, layout);

        this.scene = new Scene(root, screenWidth, screenHeight);
        this.stage.setScene(scene);
        
        // Connect to server
        new Thread(() -> {
            String serverIP = ServerDiscovery.discoverServer();
            if (serverIP != null) {
                networkClient = new NetworkClient(serverIP, NetworkClient.DEFAULT_PORT, null, null);
                if (networkClient.connect()) {
                    networkClient.startListening();
                    
                    networkClient.setChatListener((senderId, text) -> {
                        Platform.runLater(() -> {
                            String prefix = (networkClient != null && networkClient.getPlayerId() == senderId) ? "You" : "Player " + senderId;
                            chatHistory.appendText(prefix + ": " + text + "\n");
                        });
                    });
                    
                    networkClient.setLobbyUpdateListener((p1c, p1r, p2c, p2r) -> {
                        Platform.runLater(() -> {
                            if (p1c) {
                                player1.setText("Player 1" + (p1r ? " (Ready)" : " (Connected)"));
                                player1.setTextFill(p1r ? Color.web("#4CAF50") : Color.WHITE);
                            }
                            if (p2c) {
                                player2.setText("Player 2" + (p2r ? " (Ready)" : " (Connected)"));
                                player2.setTextFill(p2r ? Color.web("#4CAF50") : Color.WHITE);
                            } else {
                                player2.setText("Waiting for Player 2...");
                                player2.setTextFill(Color.web("#aaaaaa"));
                            }

                            if (p1r && p2r) {
                                startButton.setDisable(false);
                            }
                        });
                    });

                    networkClient.setGameStartListener(() -> {
                        Platform.runLater(() -> {
                            GameStage gameStage = new GameStage(networkClient);
                            gameStage.setStage(primaryStage, titleScene);
                        });
                    });
                }
            } else {
                Platform.runLater(() -> {
                    player1.setText("No server found.");
                    player1.setTextFill(Color.RED);
                });
            }
        }).start();
    }
    
    private Label createLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.WHITE);
        label.setFont(Font.font("Press Start 2P", 16));
        label.setStyle("-fx-effect: dropshadow(one-pass-box, black, 0, 0, 3, 3);");
        return label;
    }
    
    private Button createStyledButton(String text, String defaultColor, String hoverColor) {
        Button button = new Button(text);
        String baseStyle = "-fx-background-color: " + defaultColor + "; " +
                           "-fx-text-fill: white; " +
                           "-fx-font-family: 'Press Start 2P', monospace; " +
                           "-fx-font-size: 16px; " +
                           "-fx-padding: 15px 30px; " +
                           "-fx-border-color: white #222 #222 white; " +
                           "-fx-border-width: 4px; " +
                           "-fx-cursor: hand; " +
                           "-fx-effect: dropshadow(one-pass-box, black, 0, 0, 6, 6);";
                           
        String hoverStyle = "-fx-background-color: #FAD02C; " +
                            "-fx-text-fill: black; " +
                            "-fx-font-family: 'Press Start 2P', monospace; " +
                            "-fx-font-size: 16px; " +
                            "-fx-padding: 15px 30px; " +
                            "-fx-border-color: white #222 #222 white; " +
                            "-fx-border-width: 4px; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: dropshadow(one-pass-box, black, 0, 0, 8, 8);";
        
        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> {
            if (!button.isDisabled()) button.setStyle(hoverStyle);
        });
        button.setOnMouseExited(e -> {
            if (!button.isDisabled()) button.setStyle(baseStyle);
        });
        return button;
    }
}
