# Passenger Rush

**By:**
* Hugz Bernados
* Justin Pena
* Quevin Custodio

**Course:** CMSC 137: Data Communications and Networking

Welcome to **Passenger Rush**! This is a fast-paced Jeepney Game project built with Java.

## Project Description

Passenger Rush is an action-packed game where you take control of a Jeepney! Your goal is to navigate the busy streets, pick up passengers to increase your score, and collect power-ups for special boosts. However, you must stay alert and avoid open manholes and other obstacles on the road to prevent crashing.

## Instructions to Play

1. **Start the Game**: Follow the running instructions below to launch the game.
2. **Move the Jeepney**: Use the **Arrow Keys** (Up, Down, Left, Right) to navigate.
3. **Pick up Passengers**: Drive over passengers to collect them and earn points.
4. **Grab Power-Ups**: Collect power-ups for temporary advantages.
5. **Avoid Obstacles**: Dodge the manholes on the road. Hitting one will result in a Game Over!

## Prerequisites

* **Java Development Kit (JDK) 26** is REQUIRED to compile and run this project. Please ensure your `JAVA_HOME` and environment variables are properly pointing to Java 26.
* An IDE with Java support (such as VS Code with the Extension Pack for Java) or Maven.

---

## How to Run the Game

Since this game features multiplayer networking, you must run the server before launching the game clients.

1. **Install Prerequisites**: Ensure you have **Java 26** installed. If using VS Code, install the `Extension Pack for Java`.
2. Open the project folder in VS Code.
3. **Start the Server**:
   * Open `/controllers/NetworkServer.java`.
   * Click the `Run` CodeLens button located directly above the `public static void main(String[] args)` method to start the game server.
4. **Start the Game (Client)**:
   * Open `/views/Main.java`.
   * Click the `Run` CodeLens button above `public static void main(String[] args)`.
   * *(To play multiplayer, run `Main.java` multiple times or on different machines connected to the same network.)*

*(Optional)* If VS Code fails to find packages initially:
* Open the Command Palette (`Ctrl+Shift+P` / `Cmd+Shift+P`).
* Type and select `Java: Clean Workspace`.
* Select `Restart and clean cache`.

---

## Project Structure

The project follows a standard enterprise Java (Maven) layout utilizing the **MVC (Model-View-Controller)** pattern.

```text
src/
├── main/
│   ├── java/
│   │   ├── com/cmsc22/
│   │   │   ├── models/         # Pure data & game entities (No UI code here)
│   │   │   ├── views/          # JavaFX Scenes, UI elements, Windows
│   │   │   └── controllers/    # Game logic, loop, handling input, networking
│   │   └── module-info.java    # Java module explicitly opening views
│   └── resources/
│       └── assets/
│           ├── images/         # Game sprites, backgrounds (.png)
│           └── application.css # Stylesheets
└── pom.xml                     # Maven configuration & dependencies
```

## Architecture Breakdown (MVC)

To implement Sockets correctly, **do not mix UI logic with Game logic**.

### 1. Models
**Files:** `Jeepney.java`, `Passenger.java`, `PowerUp.java`, `Manhole.java`, `Sprite.java`
* These classes represent the "State" of the game.
* They hold data such as `x`, `y` coordinates, `width`, `height`, and `score`.
* **Important for Sockets:** The Server will only operate on Models. When sending data over the network, you only send the data inside these models (e.g., send coordinates, not the JavaFX `ImageView` object).

### 2. Views
**Files:** `Main.java`, `GameStage.java`, `GameOverScene.java`, `LoadingArea.java`, `Graphics.java`
* This answers "How does the game look?"
* Contains all JavaFX imports.
* The view should simply *read* from Models and display them on the screen.

### 3. Controllers
**Files:** `GameTimer.java` (and future `NetworkClient`/`NetworkServer`)
* Contains the game loops and rules.
* Responds to user input (`KeyPress`) and updates the Models accordingly.

