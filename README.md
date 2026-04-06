# PassengerRush (Jeepney Game - MPPProject)

Welcome to the Jeepney Game project! This project has been restructured using Maven and the **MVC (Model-View-Controller)** pattern to make it scalable, maintainable, and ready for multiplayer/networking integration (Sockets).

## 📁 Project Structure

The project follows a standard enterprise Java (Maven) layout.

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

## 📐 Architecture Breakdown (MVC)

To implement Sockets correctly, **do not mix UI logic with Game logic**.

### 1. Models (`com.cmsc22.models`)
**Files:** `Jeepney.java`, `Passenger.java`, `PowerUp.java`, `Manhole.java`, `Sprite.java`
* These classes represent the "State" of the game.
* They hold data such as `x`, `y` coordinates, `width`, `height`, and `score`.
* **Important for Sockets:** The Server will only operate on Models. When sending data over the network, you only send the data inside these models (e.g., send coordinates, not the JavaFX `ImageView` object).

### 2. Views (`com.cmsc22.views`)
**Files:** `Main.java`, `GameStage.java`, `GameOverScene.java`, `LoadingArea.java`, `Graphics.java`
* This answers "How does the game look?"
* Contains all JavaFX imports.
* The view should simply *read* from Models and display them on the screen.

### 3. Controllers (`com.cmsc22.controllers`)
**Files:** `GameTimer.java` (and future `NetworkClient`/`NetworkServer`)
* Contains the game loops and rules.
* Responds to user input (`KeyPress`) and updates the Models accordingly.

---

## 🚀 How to Run the Game

1. **Install Prerequisites**: Ensure you have the `Extension Pack for Java` installed in VS Code.
2. Open the project folder in VS Code.
3. Open `src/main/java/com/cmsc22/views/Main.java`.
4. Click the `Run` CodeLens button located directly above the `public static void main(String[] args)` method.

*(Optional)* If VS Code fails to find packages initially:
* Open the Command Palette (`Ctrl+Shift+P` / `Cmd+Shift+P`).
* Type and select `Java: Clean Workspace`.
* Select `Restart and clean cache`.

---

## 🌐 Future Guide: Implementing Sockets / Multiplayer

When your group gets to the networking phase, keep these rules in mind:

### 1. Separate the Threads
JavaFX runs on a single main thread (the UI Thread). When you write a `SocketServer` or `SocketClient` to listen for incoming data, it **must run on a background thread**. If you put it on the main thread, the entire game screen will freeze.

### 2. Safely Updating the UI (`Platform.runLater`)
If your background Network thread receives a message that Player 2 moved, and you try to update the JavaFX `ImageView` directly from the background thread, the app will crash with an `IllegalStateException`.

**Always** wrap UI operations inside `Platform.runLater` when triggered from a network background thread:
```java
// Inside your Socket listener thread
String message = socketIn.readLine(); // "PLAYER2_MOVED_UP"

Platform.runLater(() -> {
    // This code block safely executes on the JavaFX UI thread
    jeepney2.moveUp();
});
```

### 3. Send Only What is Necessary (Coordinates)
Do not try to serialize and send JavaFX Objects (like `Image`) over ObjectOutputStreams. Instead, send tiny String commands or custom JSON strings:
* **Good:** `"P1:100:200"` (Player 1 is at X:100, Y:200)
* **Good:** `"SPAWN_POWERUP:500:300"`

### 4. Create Network Controllers
You should create `NetworkServer.java` and `NetworkClient.java` inside the `controllers` package to keep network connections decoupled from the `GameTimer`.

---

## 🛠️ Resources Loading
All images are now loaded dynamically from the classpath so that the game can be exported as a `.jar` later.
If you need to add more images, place them in `src/main/resources/assets/images/` and load them like this:
```java
// Example for creating an Image
Image newSprite = new Image(getClass().getResourceAsStream("/assets/images/new_sprite.png"));
```

Good luck scaling your Jeepney Game!
