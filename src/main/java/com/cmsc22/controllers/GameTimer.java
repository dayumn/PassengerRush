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
import javafx.stage.Stage;
import javafx.util.Duration;

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
    private final int COLLISION_DELAY = 3000;           // 3 second collision buffer
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
    private final double initialJeepney1X, initialJeepney1Y, initialJeepney2X, initialJeepney2Y;

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

    // FIX #1 — game waits for START signal from server before ticking
    private boolean gameStarted = false;

    public void setNetworkClient(NetworkClient client) {
        this.networkClient = client;
    }

    // FIX #1 — called by NetworkClient.GameStartListener when server sends START
    public void startGame() {
        gameStarted = true;
        startTime = System.currentTimeMillis(); // sync timer to server START signal
        System.out.println("[GameTimer] Game started by server signal.");
    }

    // FIX #3 — called by NetworkClient.FellListener when opponent hits manhole
    public void hideOpponent() {
        jeepney2.setVisible(false);
        jeepney2.setPassengers(0);
    }

    public GameTimer(Scene scene, Jeepney jeepney1, Jeepney jeepney2, int[][] mapGrid, int cellSize, Canvas canvas,
                     Label jeepney1PointsLabel, Label jeepney1LoadLabel, Label jeepney2PointsLabel, Label jeepney2LoadLabel,
                     Label gameClockLabel, Stage primaryStage, Scene titleScene) {
        this.scene = scene;
        this.primaryStage = primaryStage;
        this.titleScene = titleScene;

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

        jeepney1UnloadTimer = new Timeline(new KeyFrame(Duration.millis(UNLOAD_DELAY), e -> unloadPassengers(jeepney1)));
        jeepney2UnloadTimer = new Timeline(new KeyFrame(Duration.millis(UNLOAD_DELAY), e -> unloadPassengers(jeepney2)));
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
        scene.setOnKeyPressed(e -> activeKeys.add(e.getCode()));
        scene.setOnKeyReleased(e -> activeKeys.remove(e.getCode()));
    }

    private int getGridX(double x) { return (int) x / cellSize; }
    private int getGridY(double y) { return (int) y / cellSize; }

    private boolean canMoveTo(double newX, double newY) {
        int gridX = getGridX(newX);
        int gridY = getGridY(newY);
        if (gridX < 0 || gridY < 0 || gridX >= mapGrid[0].length || gridY >= mapGrid.length) return false;
        return mapGrid[gridY][gridX] >= 1;
    }

    private void moveJeepney(Jeepney jeepney, KeyCode up, KeyCode down, KeyCode left, KeyCode right) {
        if (jeepneyCollisionTime > 0) return;

        // In multiplayer: jeepney2 is the opponent — driven by server, skip local movement
        if (networkClient != null && jeepney == jeepney2) return;

        double moveAmount = jeepney.getSpeed();
        double newX = jeepney.getXPos();
        double newY = jeepney.getYPos();

        if (jeepney == jeepney1) {
            if (activeKeys.contains(KeyCode.W) && canMoveTo(newX, newY - moveAmount)) {
                newY -= moveAmount;
                jeepney.setImage(new Image(getClass().getResourceAsStream("/assets/images/Jeep1U.png")));
                jeepney1Direction = "UP";
            } else if (activeKeys.contains(KeyCode.S) && canMoveTo(newX, newY + moveAmount)) {
                newY += moveAmount;
                jeepney.setImage(new Image(getClass().getResourceAsStream("/assets/images/Jeep1D.png")));
                jeepney1Direction = "DOWN";
            } else if (activeKeys.contains(KeyCode.A) && canMoveTo(newX - moveAmount, newY)) {
                newX -= moveAmount;
                jeepney.setImage(new Image(getClass().getResourceAsStream("/assets/images/Jeep1L.png")));
                jeepney1Direction = "LEFT";
            } else if (activeKeys.contains(KeyCode.D) && canMoveTo(newX + moveAmount, newY)) {
                newX += moveAmount;
                jeepney.setImage(new Image(getClass().getResourceAsStream("/assets/images/Jeep1R.png")));
                jeepney1Direction = "RIGHT";
            }
        } else {
            // Solo mode only (networkClient == null) — jeepney2 local controls
            if (activeKeys.contains(KeyCode.UP) && canMoveTo(newX, newY - moveAmount)) {
                newY -= moveAmount;
                jeepney.setImage(new Image(getClass().getResourceAsStream("/assets/images/Jeep2U.png")));
            } else if (activeKeys.contains(KeyCode.DOWN) && canMoveTo(newX, newY + moveAmount)) {
                newY += moveAmount;
                jeepney.setImage(new Image(getClass().getResourceAsStream("/assets/images/Jeep2D.png")));
            } else if (activeKeys.contains(KeyCode.LEFT) && canMoveTo(newX - moveAmount, newY)) {
                newX -= moveAmount;
                jeepney.setImage(new Image(getClass().getResourceAsStream("/assets/images/Jeep2L.png")));
            } else if (activeKeys.contains(KeyCode.RIGHT) && canMoveTo(newX + moveAmount, newY)) {
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
                    jeepney1Direction
            );
        }
    }

    @Override
    public void handle(long now) {
        if (gameOver) return;

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

        handlePassengerPickup(jeepney1, now);
        // FIX #3 — skip pickup/unload for invisible opponent
        if (jeepney2.isVisible()) handlePassengerPickup(jeepney2, now);
        handlePassengerUnload(jeepney1, now);
        if (jeepney2.isVisible()) handlePassengerUnload(jeepney2, now);

        handleJeepneyCollision(now);
        handlePowerUps(now);
        handlePowerUpActivation(now);
        handleManholeCollision();

        for (Passenger passenger : passengers) passenger.render(gc);
        for (PowerUp powerUp : powerUps)       powerUp.render(gc);
        if (manhole != null)                   manhole.render(gc);

        jeepney1.render(gc, jeepney1Invincible);
        jeepney2.render(gc, jeepney2Invincible); // render() checks isVisible() internally

        updateLabels();
        updateClock();

        long elapsedTime = System.currentTimeMillis() - startTime;
        if (elapsedTime >= 180000) {
            gameOver = true;
            Jeepney winner = determineWinner();
            // FIX #5 — disconnect cleanly when game ends so slot is freed
            if (networkClient != null) networkClient.disconnect();
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
                if (jeepney == jeepney1) resetUnloadTimer(jeepney1);
                else resetUnloadTimer(jeepney2);
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
        if (elapsedMs < SPAWN_GRACE_PERIOD) return;

        // FIX #3 — skip collision if opponent is invisible (fell into manhole)
        if (!jeepney2.isVisible()) return;

        if (jeepney1.collidesWith(jeepney2)) {
            if (jeepneyCollisionTime == 0) {
                jeepneyCollisionTime = now;
                if (!jeepney1Invincible) jeepney1.setPassengers(0);
                if (!jeepney2Invincible) jeepney2.setPassengers(0);
                new Timeline(new KeyFrame(Duration.millis(COLLISION_DELAY),
                        e -> jeepneyCollisionTime = 0)).play();
            }
            if (jeepney1Invincible) jeepney1Invincible = false;
            if (jeepney2Invincible) jeepney2Invincible = false;
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
                if (powerUp.getType().equals("invincibility")) jeepney1Invincible = true;
                spawnPowerUp();
            } else if (jeepney2.isVisible() && jeepney2.collidesWith(powerUp) && jeepney2PowerUps.size() < 3) {
                // FIX #3 — only pick up powerups if jeepney2 is visible
                powerUps.remove(i);
                jeepney2PowerUps.add(powerUp);
                if (powerUp.getType().equals("invincibility")) jeepney2Invincible = true;
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

    // FIX #3 — manhole hides jeepney and notifies server instead of resetting position
    private void handleManholeCollision() {
        if (manhole == null) return;

        // Local jeepney hits manhole
        if (jeepney1.isVisible() && jeepney1.collidesWith(manhole)) {
            jeepney1.setVisible(false);
            jeepney1.setPassengers(0);
            if (networkClient != null) {
                networkClient.sendFell(); // tell server we fell
            }
        }

        // Opponent hits manhole — solo mode only
        // In multiplayer the opponent's visibility is controlled by server FELL broadcast
        if (networkClient == null && jeepney2.isVisible() && jeepney2.collidesWith(manhole)) {
            jeepney2.setVisible(false);
            jeepney2.setPassengers(0);
        }
    }

    private Jeepney determineWinner() {
        if (jeepney1.getPoints() > jeepney2.getPoints()) return jeepney1;
        else if (jeepney2.getPoints() > jeepney1.getPoints()) return jeepney2;
        else return null;
    }

    private void updateLabels() {
        jeepney1PointsLabel.setText("Total Points: " + jeepney1.getPoints());
        jeepney1LoadLabel.setText("Current Load: " + jeepney1.getPassengers() + "/14");
        jeepney2PointsLabel.setText("Total Points: " + jeepney2.getPoints());
        jeepney2LoadLabel.setText("Current Load: " + jeepney2.getPassengers() + "/14");
    }

    private void updateClock() {
        long elapsedTime = System.currentTimeMillis() - startTime;
        long seconds = (elapsedTime / 1000) % 60;
        long minutes = (elapsedTime / (1000 * 60)) % 60;
        gameClockLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }

    public void registerScene(Scene newScene) {
        newScene.setOnKeyPressed(e -> activeKeys.add(e.getCode()));
        newScene.setOnKeyReleased(e -> activeKeys.remove(e.getCode()));
    }
}