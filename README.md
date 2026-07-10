# ♟️ Chess

A real-time multiplayer Chess application built with **Spring Boot** and **React**, featuring **WebSocket + STOMP** communication for instant gameplay synchronization. Players can create or join games and compete with live move updates powered by bidirectional communication.

---

## ✨ Features

* ♟️ Real-time multiplayer gameplay
* ⚡ Instant move synchronization using **WebSockets** and **STOMP**
* 🔄 Server-authoritative game state management
* ✅ Move validation and turn enforcement
* 👥 Create and join game sessions
* 📜 Live move history
* 🎨 Responsive and interactive React UI
* 🏗️ Scalable client-server architecture

---

## 🛠️ Tech Stack

### Backend

* Java
* Spring Boot
* Spring WebSocket
* STOMP Protocol
* Maven

### Frontend

* React
* JavaScript
* HTML5
* CSS3

---

## 📂 Project Structure

```text
chess/
├── backend/        # Spring Boot application
└── frontend/       # React application
```

---

## 🚀 Getting Started

### Clone the repository

```bash
git clone https://github.com/<your-username>/chess.git
cd chess
```

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

or

```bash
mvn spring-boot:run
```

The backend will start on:

```text
http://localhost:8080
```

### Frontend

```bash
cd frontend
npm install
npm start
```

The React application will be available at:

```text
http://localhost:3000
```

---

## ⚙️ How It Works

1. Players create or join a chess game.
2. The React client establishes a **WebSocket** connection with the Spring Boot server.
3. Communication is handled using the **STOMP** messaging protocol.
4. Every move is validated on the server before updating the game state.
5. Valid moves are broadcast instantly to both players, ensuring synchronized gameplay.

---

## 📸 Screenshots

Add screenshots of:

* Home page
* Game lobby
* Chessboard
* Gameplay

---

## 🚧 Future Improvements

* User authentication
* Matchmaking
* Chess clock
* Spectator mode
* Game replay
* Draw and resignation support
* Player ratings (ELO)
* Persistent game history
* AI opponent
* Online chat

---

## 🤝 Contributing

Contributions are welcome! Feel free to open an issue or submit a pull request.

---

## 📄 License

This project is licensed under the MIT License.
