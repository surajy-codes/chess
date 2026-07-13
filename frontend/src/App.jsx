import { useState } from 'react'
import AuthPage  from './pages/AuthPage'
import LobbyPage from './pages/LobbyPage'
import GamePage  from './pages/GamePage'

export default function App() {
  const [auth, setAuth]   = useState(null)  // { token, userId, username }
  const [game, setGame]   = useState(null)  // { gameId, color }

  if (!auth) return <AuthPage onAuth={setAuth} />
  if (!game) return <LobbyPage auth={auth} onGame={setGame} onLogout={() => setAuth(null)} />
  return <GamePage auth={auth} game={game} onLeave={() => setGame(null)} />
}