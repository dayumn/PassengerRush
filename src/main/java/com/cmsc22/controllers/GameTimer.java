package com.cmsc22.controllers;

import com.cmsc22.models.*;
import com.cmsc22.views.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.ArrayDeque;
import java.util.Deque;

public class GameTimer extends AnimationTimer {
    private Scene scene;
    private Canvas canvas;
    private Stage primaryStage;

    private Jeepney jeepney1;
    private Jeepney jeepney2;
    private int[][] mapGrid;
    private int cellSize;
    private Set<KeyCode> activeKeys = new HashSet<>();

    private List<Passenger> passengers = new ArrayList<>();
    private List<LoadingArea> loadingAreas = new ArrayList<>();
    private List<Image> passengerImages = new ArrayList<>();
    private Random random = new Random(42);
    private Timeline passengerSpawner;

    private boolean[][] passengerGrid;

    private long jeepney1LoadingStartTime = 0;
    private long jeepney2LoadingStartTime = 0;
    private long jeepney1UnloadingStartTime = 0;
    private long jeepney2UnloadingStartTime = 0;
    private final int LOADING_DELAY = 3000;
    private final int UNLOAD_DELAY = 8000;

    private long jeepneyCollisionTime = 0;
    private long jeepneyFreezeTime = 0;
    private final int FREEZE_DELAY = 1000;              // 1 second freeze
    private final int COLLISION_DELAY = 3000;           // 3 second total collision buffer (1s freeze + 2s immunity)
    private final long SPAWN_GRACE_PERIOD = 5000;       // 5 second no-collision at start

    private Timeline jeepney1LoadTimer;
    private Timeline jeepney2LoadTimer;
    private Timeline jeepney1UnloadTimer;
    private Timeline jeepney2UnloadTimer;

    private Image speedImage = new Image(getClass().getResourceAsStream("/assets/images/speed.png"));
    private Image crackImage = new Image(getClass().getResourceAsStream("/assets/images/crack.png"));
    private Image invincibilityImage = new Image(getClass().getResourceAsStream("/assets/images/insurance.png"));
    private boolean jeepney1Invincible = false;
    private boolean jeepney2Invincible = false;

    private int maxPowerUpsOnscreen;
    private List<PowerUp> jeepney1PowerUps = new ArrayList<>();
    private List<PowerUp> jeepney2PowerUps = new ArrayList<>();
    private List<PowerUp> powerUps = new ArrayList<>();
    private long powerUpRespawnTime = 0;
    private final long POWERUP_RESPAWN_DELAY = 10000;

    private Image manholeImage = new Image(getClass().getResourceAsStream("/assets/images/manhole.png"));
    private Manhole manhole;
    private double initialJeepney1X, initialJeepney1Y, initialJeepney2X, initialJeepney2Y;

    private Label jeepney1PointsLabel;
    private Label jeepney1LoadLabel;
    private Label jeepney2PointsLabel;
    private Label jeepney2LoadLabel;
    private Label gameClockLabel;

    private long startTime;
    private boolean gameOver = false;
    private Scene titleScene;

    private String jeepney1Direction = "DOWN";

    private NetworkClient networkClient = null;

    private Image backgroundImage;
    private final double ZOOM_FACTOR = 2.0;
    private final long INTRO_DURATION_MS = 2500;

    // ── Chat state ────────────────────────────────────────────────────────────
    private static final int CHAT_MAX_HISTORY = 6; // lines kept on screen
    private static final long CHAT_FADE_MS = 8000; // ms before history fades
    private final Deque<String> chatHistory = new ArrayDeque<>();
    private final Deque<Long> chatTimestamps = new ArrayDeque<>();
    private boolean chatInputActive = false; // true while player is typing
    private StringBuilder chatBuffer = new StringBuilder();
    // ─────────────────────────────────────────────────────────────────────────

    // FIX #1 — game waits for START signal from server before ticking
    private boolean gameStarted = false;

    public void setNetworkClient(NetworkClient client) {
        this.networkClient = client;
    }

    // Called by GameStage when a CHAT message arrives from the network
    public void addChatMessage(int senderId, String text) {
        String label = "P" + senderId + ": " + text;
        chatHistory.addLast(label);
        chatTimestamps.addLast(System.currentTimeMillis());
        if (chatHistory.size() > CHAT_MAX_HISTORY) {
            chatHistory.removeFirst();
            chatTimestamps.removeFirst();
        }
        System.out.println("[Chat] " + label);
    }

    // FIX #1 — called by NetworkClient.GameStartListener when server sends START
    public void startGame() {
        gameStarted = true;
        startTime = System.currentTimeMillis(); // sync timer to server START signal
        
        initialJeepney1X = jeepney1.getXPos();
        initialJeepney1Y = jeepney1.getYPos();
        initialJeepney2X = jeepney2.getXPos();
        initialJeepney2Y = jeepney2.getYPos();

        System.out.println("[GameTimer] Game started by server signal.");
    }

    // FIX #3 — called by NetworkClient.FellListener when opponent hits manhole
    public void hideOpponent() {
        jeepney2.setVisible(false);
        jeepney2.setPassengers(0);
    }

    public GameTimer(Scene scene, Jeepney jeepney1, Jeepney jeepney2, int[][] mapGrid, int cellSize, Canvas canvas,
                     Label jeepney1PointsLabel, Label jeepney1LoadLabel, Label jeepney2PointsLabel, Label jeepney2LoadLabel,
                     Label gameClockLabel, Stage primaryStage, Scene titleScene, Image backgroundImage) {
        this.scene = scene;
        this.primaryStage = primaryStage;
        this.titleScene = titleScene;
        this.backgroundImage = backgroundImage;

        this.jeepney1 = jeepney1;
        this.jeepney2 = jeepney2;
        this.mapGrid = mapGrid;
        this.cellSize = cellSize;

        initialJeepney1X = jeepney1.getXPos();
        initialJeepney1Y = jeepney1.getYPos();
        initialJeepney2X = jeepney2.getXPos();
        initialJeepney2Y = jeepney2.getYPos();

        this.setInitialPositions();
        this.setupKeyHandling();

        this.canvas = canvas;

        passengerImages.add(new Image(getClass().getResourceAsStream("/assets/images/passenger1.png")));
        passengerImages.add(new Image(getClass().getResourceAsStream("/assets/images/passenger2.png")));
        passengerImages.add(new Image(getClass().getResourceAsStream("/assets/images/passenger3.png")));
        passengerImages.add(new Image(getClass().getResourceAsStream("/assets/images/passenger4.png")));
        passengerImages.add(new Image(getClass().getResourceAsStream("/assets/images/passenger5.png")));

        this.passengerGrid = new boolean[mapGrid.length][mapGrid[0].length];

        for (int row = 0; row < mapGrid.length; row++) {
            for (int col = 0; col < mapGrid[0].length; col++) {
                int cellValue = mapGrid[row][col];
                if (cellValue >= 3 && cellValue <= 9) {
                    LoadingArea area = new LoadingArea(col * cellSize, row * cellSize, cellSize, cellValue);
                    loadingAreas.add(area);
                }
            }
        }

        placeInitialPassengers();
        placeManhole();
        spawnPowerUp();

        this.maxPowerUpsOnscreen = 2;

        passengerSpawner = new Timeline(new KeyFrame(Duration.seconds(20), e -> spawnPassengers()));
        passengerSpawner.setCycleCount(Timeline.INDEFINITE);
        passengerSpawner.play();

        jeepney1LoadTimer = new Timeline(new KeyFrame(Duration.millis(LOADING_DELAY), e -> loadPassengers(jeepney1)));
        jeepney2LoadTimer = new Timeline(new KeyFrame(Duration.millis(LOADING_DELAY), e -> loadPassengers(jeepney2)));
        jeepney1LoadTimer.setCycleCount(Timeline.INDEFINITE);
        jeepney2LoadTimer.setCycleCount(Timeline.INDEFINITE);

        jeepney1UnloadTimer = new Timeline(
                new KeyFrame(Duration.millis(UNLOAD_DELAY), e -> unloadPassengers(jeepney1)));
        jeepney2UnloadTimer = new Timeline(
                new KeyFrame(Duration.millis(UNLOAD_DELAY), e -> unloadPassengers(jeepney2)));
        jeepney1UnloadTimer.setCycleCount(1);
        jeepney2UnloadTimer.setCycleCount(1);

        this.jeepney1PointsLabel = jeepney1PointsLabel;
        this.jeepney1LoadLabel = jeepney1LoadLabel;
        this.jeepney2PointsLabel = jeepney2PointsLabel;
        this.jeepney2LoadLabel = jeepney2LoadLabel;
        this.gameClockLabel = gameClockLabel;

        startTime = System.currentTimeMillis();
    }

    private void setInitialPositions() {
        double imageHeight = 30;

        // Player 1 spawn — adjust these two values to reposition
        jeepney1.setXPos(scene.getWidth() / 2 - 45);
        jeepney1.setYPos(cellSize + (cellSize - imageHeight) / 2 + 30);

        // Player 2 spawn — 6 cells to the right of Player 1
        jeepney2.setXPos(scene.getWidth() / 2 - 45 + (cellSize * 6));
        jeepney2.setYPos(cellSize + (cellSize - imageHeight) / 2 + 30);
    }

    private void setupKeyHandling() {
        scene.setOnKeyPressed(e -> {
            if (chatInputActive) {
                handleChatKey(e.getCode(), e.getText());
            } else {
                if (e.getCode() == KeyCode.ENTER) {
                    // Open chat bar
                    chatInputActive = true;
                    chatBuffer.setLength(0);
                } else {
                    activeKeys.add(e.getCode());
                }
            }
        });
        scene.setOnKeyReleased(e -> {
            if (!chatInputActive)
                activeKeys.remove(e.getCode());
        });
    }

    /** Handles key presses while the chat input bar is open. */
    private void handleChatKey(KeyCode code, String text) {
        switch (code) {
            case ENTER -> {
                // Send and close
                String msg = chatBuffer.toString().trim();
                if (!msg.isBlank()) {
                    if (networkClient != null) {
                        networkClient.sendChat(msg);
                    } else {
                        // Solo mode: show locally as Player 1
                        addChatMessage(1, msg);
                    }
                }
                chatInputActive = false;
                chatBuffer.setLength(0);
            }
            case ESCAPE -> {
                chatInputActive = false;
                chatBuffer.setLength(0);
            }
            case BACK_SPACE -> {
                if (chatBuffer.length() > 0)
                    chatBuffer.deleteCharAt(chatBuffer.length() - 1);
            }
            default -> {
                // Append printable characters
                if (text != null && !text.isEmpty() && chatBuffer.length() < 120) {
                    char c = text.charAt(0);
                    if (c >= 32 && c < 127)
                        chatBuffer.append(c);
                }
            }
        }
    }

    private int getGridX(double x) {
        return (int) x / cellSize;
    }

    private int getGridY(double y) {
        return (int) y / cellSize;
    }

    private boolean canMoveTo(double newX, double newY) {
        int gridX = getGridX(newX);
        int gridY = getGridY(newY);
        if (gridX < 0 || gridY < 0 || gridX >= mapGrid[0].length || gridY >= mapGrid.length)
            return false;
        return mapGrid[gridY][gridX] >= 1;
    }

    private boolean collidesWithOpponent(Jeepney jeepney, double newX, double newY) {
        if (jeepneyCollisionTime > 0) return false; // Immunity: allow passing through!
        
        Jeepney opponent = (jeepney == jeepney1) ? jeepney2 : jeepney1;
        if (!opponent.isVisible()) return false;
        
        double oldX = jeepney.getXPos();
        double oldY = jeepney.getYPos();
        jeepney.setXPos(newX);
        jeepney.setYPos(newY);
        boolean collides = jeepney.collidesWith(opponent);
        jeepney.setXPos(oldX);
        jeepney.setYPos(oldY);
        return collides;
    }

    private void moveJeepney(Jeepney jeepney, KeyCode up, KeyCode down, KeyCode left, KeyCode right) {
        if (jeepneyFreezeTime > 0) return;

        long elapsedMs = System.currentTimeMillis() - startTime;
        if (gameStarted && elapsedMs < INTRO_DURATION_MS) return;

        // In multiplayer: jeepney2 is the opponent — driven by server, skip local
        // movement
        if (networkClient != null && jeepney == jeepney2)
            return;

        double moveAmount = jeepney.getSpeed();
        double newX = jeepney.getXPos();
        double newY = jeepney.getYPos();

        if (jeepney == jeepney1) {
            if (activeKeys.contains(KeyCode.W) && canMoveTo(newX, newY - moveAmount) && !collidesWithOpponent(jeepney, newX, newY - moveAmount)) {
                newY -= moveAmount;
                jeepney.setImage(new Image(getClass().getResourceAsStream("/assets/images/Jeep1U.png")));
                jeepney1Direction = "UP";
            } else if (activeKeys.contains(KeyCode.S) && canMoveTo(newX, newY + moveAmount) && !collidesWithOpponent(jeepney, newX, newY + moveAmount)) {
                newY += moveAmount;
                jeepney.setImage(new Image(getClass().getResourceAsStream("/assets/images/Jeep1D.png")));
                jeepney1Direction = "DOWN";
            } else if (activeKeys.contains(KeyCode.A) && canMoveTo(newX - moveAmount, newY) && !collidesWithOpponent(jeepney, newX - moveAmount, newY)) {
                newX -= moveAmount;
                jeepney.setImage(new Image(getClass().getResourceAsStream("/assets/images/Jeep1L.png")));
                jeepney1Direction = "LEFT";
            } else if (activeKeys.contains(KeyCode.D) && canMoveTo(newX + moveAmount, newY) && !collidesWithOpponent(jeepney, newX + moveAmount, newY)) {
                newX += moveAmount;
                jeepney.setImage(new Image(getClass().getResourceAsStream("/assets/images/Jeep1R.png")));
                jeepney1Direction = "RIGHT";
            }
        } else {
            // Solo mode only (networkClient == null) — jeepney2 local controls
            if (activeKeys.contains(KeyCode.UP) && canMoveTo(newX, newY - moveAmount) && !collidesWithOpponent(jeepney, newX, newY - moveAmount)) {
                newY -= moveAmount;
                jeepney.setImage(new Image(getClass().getResourceAsStream("/assets/images/Jeep2U.png")));
            } else if (activeKeys.contains(KeyCode.DOWN) && canMoveTo(newX, newY + moveAmount) && !collidesWithOpponent(jeepney, newX, newY + moveAmount)) {
                newY += moveAmount;
                jeepney.setImage(new Image(getClass().getResourceAsStream("/assets/images/Jeep2D.png")));
            } else if (activeKeys.contains(KeyCode.LEFT) && canMoveTo(newX - moveAmount, newY) && !collidesWithOpponent(jeepney, newX - moveAmount, newY)) {
                newX -= moveAmount;
                jeepney.setImage(new Image(getClass().getResourceAsStream("/assets/images/Jeep2L.png")));
            } else if (activeKeys.contains(KeyCode.RIGHT) && canMoveTo(newX + moveAmount, newY) && !collidesWithOpponent(jeepney, newX + moveAmount, newY)) {
                newX += moveAmount;
                jeepney.setImage(new Image(getClass().getResourceAsStream("/assets/images/Jeep2R.png")));
            }
        }

        jeepney.setXPos(newX);
        jeepney.setYPos(newY);

        // Send position + direction to server every frame
        if (networkClient != null && jeepney == jeepney1) {
            networkClient.sendPosition(
                    jeepney1.getXPos(),
                    jeepney1.getYPos(),
                    jeepney1.getPassengers(),
                    jeepney1.getPoints(),
                    jeepney1Direction);
        }
    }

    @Override
    public void handle(long now) {
        if (gameOver)
            return;

        GraphicsContext gc = canvas.getGraphicsContext2D();

        // FIX #1 — show waiting screen until server sends START
        // In solo mode (networkClient == null), skip straight to game
        if (networkClient != null && !gameStarted) {
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Press Start 2P", 24));
            gc.fillText("Waiting for Player 2...",
                    canvas.getWidth() / 2 - 220,
                    canvas.getHeight() / 2);
            return;
        }

        moveJeepney(jeepney1, KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D);
        moveJeepney(jeepney2, KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT);

        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.save();

        double screenCenterX = canvas.getWidth() / 2.0;
        double screenCenterY = canvas.getHeight() / 2.0;
        
        long elapsedIntroMs = System.currentTimeMillis() - startTime;
        double progress = 1.0;
        if (elapsedIntroMs < INTRO_DURATION_MS) {
            double t = (double) elapsedIntroMs / INTRO_DURATION_MS;
            progress = t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2; // easeInOutQuad
        }
        
        double currentZoom = 1.0 + (ZOOM_FACTOR - 1.0) * progress;
        
        double playerX = jeepney1.getXPos();
        double playerY = jeepney1.getYPos();
        
        double camX = playerX * currentZoom - screenCenterX;
        double camY = playerY * currentZoom - screenCenterY;
        
        double mapWidth = mapGrid[0].length * cellSize;
        double mapHeight = mapGrid.length * cellSize;
        
        double maxCamX = Math.max(0, (mapWidth * currentZoom) - canvas.getWidth());
        double maxCamY = Math.max(0, (mapHeight * currentZoom) - canvas.getHeight());
        
        camX = Math.max(0, Math.min(camX, maxCamX));
        camY = Math.max(0, Math.min(camY, maxCamY));
        
        gc.translate(-camX, -camY);
        gc.scale(currentZoom, currentZoom);
        
        if (backgroundImage != null) {
            gc.drawImage(backgroundImage, 0, 0, mapWidth, mapHeight);
        }

        handlePassengerPickup(jeepney1, now);
        // FIX #3 — skip pickup/unload for invisible opponent
        if (jeepney2.isVisible())
            handlePassengerPickup(jeepney2, now);
        handlePassengerUnload(jeepney1, now);
        if (jeepney2.isVisible())
            handlePassengerUnload(jeepney2, now);

        handleJeepneyCollision(now);
        handlePowerUps(now);
        handlePowerUpActivation(now);
        handleManholeCollision();

        for (Passenger passenger : passengers)
            passenger.render(gc);
        for (PowerUp powerUp : powerUps)
            powerUp.render(gc);
        if (manhole != null)
            manhole.render(gc);

        jeepney1.render(gc, jeepney1Invincible);
        jeepney2.render(gc, jeepney2Invincible); // render() checks isVisible() internally

        gc.restore();

        updateLabels();
        updateClock();
        renderChat(gc);

        long elapsedTime = System.currentTimeMillis() - startTime;
        if (elapsedTime >= 180000) {
            gameOver = true;
            Jeepney winner = determineWinner();
            // FIX #5 — disconnect cleanly when game ends so slot is freed
            if (networkClient != null)
                networkClient.disconnect();
            new GameOverScene(winner, primaryStage, titleScene);
        }
    }

    private void spawnPassengers() {
        for (LoadingArea area : loadingAreas) {
            int numToSpawn = Math.min(area.getCapacity(), random.nextInt(2));
            for (int i = 0; i < numToSpawn; i++) {
                spawnPassenger(area);
            }
        }
    }

    private void placeInitialPassengers() {
        for (LoadingArea area : loadingAreas) {
            int numToSpawn = random.nextInt(2);
            for (int i = 0; i < numToSpawn; i++) {
                spawnPassenger(area);
            }
        }
    }

    private void spawnPassenger(LoadingArea area) {
        int col = (int) (area.getX() / cellSize);
        int row = (int) (area.getY() / cellSize);
        int numCols = (int) (area.getSize() / cellSize);
        int numRows = (int) (area.getSize() / cellSize);

        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                int currentCol = col + c;
                int currentRow = row + r;
                if (currentCol >= 0 && currentCol < passengerGrid[0].length
                        && currentRow >= 0 && currentRow < passengerGrid.length
                        && !passengerGrid[currentRow][currentCol]) {
                    double x = (currentCol * cellSize) + cellSize / 2;
                    double y = (currentRow * cellSize) + cellSize / 2;
                    Image passengerImage = passengerImages.get(random.nextInt(passengerImages.size()));
                    Passenger passenger = new Passenger(x, y, passengerImage);
                    passengers.add(passenger);
                    passengerGrid[currentRow][currentCol] = true;
                    return;
                }
            }
        }
    }

    private void handlePassengerPickup(Jeepney jeepney, long now) {
        boolean inLoadingZone = isInLoadingZone(jeepney);
        if (inLoadingZone) {
            if (jeepney == jeepney1 && jeepney1LoadingStartTime == 0) {
                jeepney1LoadingStartTime = now;
                jeepney1LoadTimer.play();
            } else if (jeepney == jeepney2 && jeepney2LoadingStartTime == 0) {
                jeepney2LoadingStartTime = now;
                jeepney2LoadTimer.play();
            }
        } else {
            resetLoadingTimer(jeepney);
        }
    }

    private void loadPassengers(Jeepney jeepney) {
        for (int i = passengers.size() - 1; i >= 0; i--) {
            Passenger p = passengers.get(i);
            if (jeepney.collidesWith(p) && jeepney.getPassengers() < jeepney.getCapacity()) {
                passengers.remove(i);
                jeepney.incrementPassengers();
                updatePassengerGrid(p, false);
            }
        }
    }

    private void resetLoadingTimer(Jeepney jeepney) {
        if (jeepney == jeepney1) {
            jeepney1LoadingStartTime = 0;
            jeepney1LoadTimer.stop();
        } else {
            jeepney2LoadingStartTime = 0;
            jeepney2LoadTimer.stop();
        }
    }

    private void updatePassengerGrid(Passenger p, boolean occupied) {
        int col = (int) (p.getXPos() / cellSize);
        int row = (int) (p.getYPos() / cellSize);
        passengerGrid[row][col] = occupied;
    }

    private boolean isInLoadingZone(Jeepney jeepney) {
        int gridX = (int) jeepney.getXPos() / cellSize;
        int gridY = (int) jeepney.getYPos() / cellSize;
        return mapGrid[gridY][gridX] >= 3 && mapGrid[gridY][gridX] <= 9;
    }

    private void handlePassengerUnload(Jeepney jeepney, long now) {
        if (jeepney.getPassengers() > 0) {
            if (isInUnloadZone(jeepney)) {
                if (jeepney == jeepney1 && jeepney1UnloadingStartTime == 0) {
                    jeepney1UnloadingStartTime = now;
                    jeepney1UnloadTimer.play();
                } else if (jeepney == jeepney2 && jeepney2UnloadingStartTime == 0) {
                    jeepney2UnloadingStartTime = now;
                    jeepney2UnloadTimer.play();
                }
            } else {
                if (jeepney == jeepney1)
                    resetUnloadTimer(jeepney1);
                else
                    resetUnloadTimer(jeepney2);
            }
        }
    }

    private void resetUnloadTimer(Jeepney jeepney) {
        if (jeepney == jeepney1) {
            jeepney1UnloadingStartTime = 0;
            jeepney1UnloadTimer.stop();
        } else {
            jeepney2UnloadingStartTime = 0;
            jeepney2UnloadTimer.stop();
        }
    }

    private void unloadPassengers(Jeepney jeepney) {
        jeepney.incrementPoints(jeepney.getPassengers());
        jeepney.setPassengers(0);
    }

    private boolean isInUnloadZone(Jeepney jeepney) {
        int gridX = (int) jeepney.getXPos() / cellSize;
        int gridY = (int) jeepney.getYPos() / cellSize;
        return mapGrid[gridY][gridX] == 2;
    }

    private void handleJeepneyCollision(long now) {
        // FIX #2 — no collision during spawn grace period (first 5 seconds)
        long elapsedMs = System.currentTimeMillis() - startTime;
        if (elapsedMs < SPAWN_GRACE_PERIOD)
            return;

        // FIX #3 — skip collision if opponent is invisible (fell into manhole)
        if (!jeepney2.isVisible())
            return;

        if (jeepney1.collidesWith(jeepney2)) {
            if (jeepneyCollisionTime == 0) {
                jeepneyCollisionTime = now;
                jeepneyFreezeTime = now;

                if (!jeepney1Invincible) jeepney1.setPassengers(0);
                if (!jeepney2Invincible) jeepney2.setPassengers(0);
                
                if (jeepney1Invincible) jeepney1Invincible = false;
                if (jeepney2Invincible) jeepney2Invincible = false;

                new Timeline(new KeyFrame(Duration.millis(FREEZE_DELAY),
                        e -> jeepneyFreezeTime = 0)).play();
                new Timeline(new KeyFrame(Duration.millis(COLLISION_DELAY),
                        e -> jeepneyCollisionTime = 0)).play();
            }
        }
    }

    private void spawnPowerUp() {
        if (powerUps.isEmpty() && System.currentTimeMillis() > powerUpRespawnTime) {
            int row, col;
            do {
                row = random.nextInt(mapGrid.length);
                col = random.nextInt(mapGrid[0].length);
            } while (mapGrid[row][col] != 1);

            double x = col * cellSize + cellSize / 2;
            double y = row * cellSize + cellSize / 2;
            String powerUpType = random.nextBoolean() ? (random.nextBoolean() ? "speed" : "crack") : "invincibility";
            Image powerUpImage = powerUpType.equals("speed") ? speedImage
                    : (powerUpType.equals("crack") ? crackImage : invincibilityImage);
            PowerUp powerUp = new PowerUp(x, y, powerUpType, powerUpImage, 3000,
                    powerUpType.equals("speed") ? 3 : (powerUpType.equals("crack") ? -3 : 0));
            powerUps.add(powerUp);
            powerUpRespawnTime = System.currentTimeMillis() + POWERUP_RESPAWN_DELAY;
        }
    }

    private void handlePowerUps(long now) {
        for (int i = powerUps.size() - 1; i >= 0; i--) {
            PowerUp powerUp = powerUps.get(i);
            if (jeepney1.collidesWith(powerUp) && jeepney1PowerUps.size() < 3) {
                powerUps.remove(i);
                jeepney1PowerUps.add(powerUp);
                if (powerUp.getType().equals("invincibility"))
                    jeepney1Invincible = true;
                spawnPowerUp();
            } else if (jeepney2.isVisible() && jeepney2.collidesWith(powerUp) && jeepney2PowerUps.size() < 3) {
                // FIX #3 — only pick up powerups if jeepney2 is visible
                powerUps.remove(i);
                jeepney2PowerUps.add(powerUp);
                if (powerUp.getType().equals("invincibility"))
                    jeepney2Invincible = true;
                spawnPowerUp();
            }
        }
    }

    private void handlePowerUpActivation(long now) {
        if (activeKeys.contains(KeyCode.E) && !jeepney1PowerUps.isEmpty()) {
            usePowerUp(jeepney1, jeepney2, jeepney1PowerUps);
        } else if (activeKeys.contains(KeyCode.SHIFT) && !jeepney2PowerUps.isEmpty()) {
            usePowerUp(jeepney2, jeepney1, jeepney2PowerUps);
        }
    }

    private void usePowerUp(Jeepney user, Jeepney target, List<PowerUp> powerUps) {
        if (!powerUps.isEmpty()) {
            int randomIndex = random.nextInt(powerUps.size());
            PowerUp powerUp = powerUps.remove(randomIndex);
            if (powerUp.getType().equals("speed")) {
                activateSpeedBoost(user, powerUp.getDuration(), powerUp.getEffectValue());
            } else if (powerUp.getType().equals("crack")) {
                activateCrack(target, powerUp.getDuration());
            }
        }
    }

    private void activateCrack(Jeepney jeepney, long duration) {
        jeepney.decreaseSpeed(2);
        new Timeline(new KeyFrame(Duration.millis(duration), e -> jeepney.increaseSpeed(2))).play();
    }

    private void activateSpeedBoost(Jeepney jeepney, long duration, int boostAmount) {
        jeepney.increaseSpeed(boostAmount);
        new Timeline(new KeyFrame(Duration.millis(duration), e -> jeepney.decreaseSpeed(boostAmount))).play();
    }

    private void placeManhole() {
        int row, col;
        do {
            row = random.nextInt(mapGrid.length);
            col = random.nextInt(mapGrid[0].length);
        } while (mapGrid[row][col] != 1);
        double x = col * cellSize + (cellSize - manholeImage.getWidth()) / 2;
        double y = row * cellSize + (cellSize - manholeImage.getHeight()) / 2;
        manhole = new Manhole(x, y, manholeImage);
    }

    // FIX #3 — manhole hides jeepney and notifies server instead of resetting
    // position
    private void handleManholeCollision() {
        if (manhole == null)
            return;

        // Local jeepney hits manhole
        if (jeepney1.isVisible() && jeepney1.collidesWith(manhole)) {
            jeepney1.setVisible(false);
            jeepney1.setPassengers(0);
            if (networkClient != null) {
                networkClient.sendFell(); // tell server we fell
            }
            
            // Respawn after 2 seconds
            new Timeline(new KeyFrame(Duration.millis(2000), e -> {
                jeepney1.setXPos(initialJeepney1X);
                jeepney1.setYPos(initialJeepney1Y);
                jeepney1.setVisible(true);
            })).play();
        }

        // Opponent hits manhole — solo mode only
        // In multiplayer the opponent's visibility is controlled by server FELL
        // broadcast
        if (networkClient == null && jeepney2.isVisible() && jeepney2.collidesWith(manhole)) {
            jeepney2.setVisible(false);
            jeepney2.setPassengers(0);
        }
    }

    private Jeepney determineWinner() {
        if (jeepney1.getPoints() > jeepney2.getPoints())
            return jeepney1;
        else if (jeepney2.getPoints() > jeepney1.getPoints())
            return jeepney2;
        else
            return null;
    }

    private void updateLabels() {
        jeepney1PointsLabel.setText("Total Points: " + jeepney1.getPoints());
        jeepney1LoadLabel.setText("Current Load: " + jeepney1.getPassengers() + "/14");
        jeepney2PointsLabel.setText("Total Points: " + jeepney2.getPoints());
        jeepney2LoadLabel.setText("Current Load: " + jeepney2.getPassengers() + "/14");
    }

    private void updateClock() {
        long elapsedTime = System.currentTimeMillis() - startTime;
        long remainingTime = Math.max(0, 180000 - elapsedTime);
        long seconds = (remainingTime / 1000) % 60;
        long minutes = (remainingTime / (1000 * 60)) % 60;
        gameClockLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }

    public void registerScene(Scene newScene) {
        newScene.setOnKeyPressed(e -> {
            if (chatInputActive) {
                handleChatKey(e.getCode(), e.getText());
            } else {
                if (e.getCode() == KeyCode.ENTER) {
                    chatInputActive = true;
                    chatBuffer.setLength(0);
                } else {
                    activeKeys.add(e.getCode());
                }
            }
        });
        newScene.setOnKeyReleased(e -> {
            if (!chatInputActive)
                activeKeys.remove(e.getCode());
        });
    }

    // ── Chat rendering ───────────────────────────────────────────────────────
    private void renderChat(GraphicsContext gc) {
        double panelX = 10;
        double panelW = 500;
        double lineH = 24; // increased line spacing
        double fontSize = 18; // larger font
        double barH = 34;
        double barY = canvas.getHeight() - 75;
        double historyY = barY - 12; // history sits just above the input bar

        gc.save();
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, FontPosture.REGULAR, fontSize));

        // ── Always-visible input bar ─────────────────────────────────────────
        // Background: darker + more opaque when typing, subtle when idle
        double barAlpha = chatInputActive ? 0.82 : 0.55;
        gc.setFill(Color.color(0.05, 0.05, 0.15, barAlpha));
        gc.fillRoundRect(panelX, barY, panelW, barH, 8, 8);

        // Border glow when active
        if (chatInputActive) {
            gc.setStroke(Color.color(1, 0.85, 0.1, 0.9));
            gc.setLineWidth(2);
            gc.strokeRoundRect(panelX, barY, panelW, barH, 8, 8);
        } else {
            gc.setStroke(Color.color(1, 1, 1, 0.25));
            gc.setLineWidth(1);
            gc.strokeRoundRect(panelX, barY, panelW, barH, 8, 8);
        }

        // Prompt text
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, FontPosture.REGULAR, fontSize));
        if (chatInputActive) {
            String cursor = (System.currentTimeMillis() / 500 % 2 == 0) ? "|" : " ";
            String display = "Say: " + chatBuffer.toString() + cursor;
            gc.setFill(Color.color(1, 0.92, 0.2, 1)); // vivid yellow
            gc.fillText(display, panelX + 10, barY + barH - 9);

            // Hint
            gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, FontPosture.REGULAR, 11));
            gc.setFill(Color.color(0.75, 0.75, 0.75, 0.85));
            gc.fillText("Enter=send  Esc=cancel", panelX + panelW - 150, barY + barH - 9);
        } else {
            // Idle placeholder
            gc.setFill(Color.color(0.85, 0.85, 0.85, 0.55));
            gc.fillText("Press [Enter] to chat...", panelX + 10, barY + barH - 9);
        }

        // ── Chat history ─────────────────────────────────────────────────────
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, FontPosture.REGULAR, fontSize));
        long now = System.currentTimeMillis();
        String[] lines = chatHistory.toArray(new String[0]);
        Long[] times = chatTimestamps.toArray(new Long[0]);

        for (int i = lines.length - 1; i >= 0; i--) {
            long age = now - times[i];
            if (age > CHAT_FADE_MS && !chatInputActive)
                continue;
            // Floor alpha at 0.15 so messages never fully vanish while bar is idle
            double alpha = chatInputActive ? 0.95 : Math.max(0.15, 1.0 - (double) age / CHAT_FADE_MS);
            double y = historyY - (lines.length - 1 - i) * lineH;

            // Semi-transparent row background for readability
            gc.setFill(Color.color(0, 0, 0, alpha * 0.45));
            gc.fillRect(panelX - 2, y - fontSize + 2, panelW + 4, lineH);

            // Drop-shadow
            gc.setFill(Color.color(0, 0, 0, alpha * 0.7));
            gc.fillText(lines[i], panelX + 2, y + 2);
            // White text
            gc.setFill(Color.color(1, 1, 1, alpha));
            gc.fillText(lines[i], panelX, y);
        }

        gc.restore();
    }
    // ─────────────────────────────────────────────────────────────────────────
}
