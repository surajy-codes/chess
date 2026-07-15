import { useState, useEffect } from 'react'
import AuthPage from './pages/AuthPage'
import LobbyPage from './pages/LobbyPage'
import GamePage from './pages/GamePage'

const TERMINAL = ['CHECKMATE', 'STALEMATE', 'DRAW']

function saveAuth(auth) { localStorage.setItem('chess_auth', JSON.stringify(auth)) }
function saveGame(game) { localStorage.setItem('chess_game', JSON.stringify(game)) }
function clearGame() { localStorage.removeItem('chess_game') }
function clearAll() { localStorage.removeItem('chess_auth'); localStorage.removeItem('chess_game') }

export default function App() {
  const [auth, setAuth] = useState(() => {
    try { return JSON.parse(localStorage.getItem('chess_auth')) } catch { return null }
  })
  const [game, setGame] = useState(() => {
    try { return JSON.parse(localStorage.getItem('chess_game')) } catch { return null }
  })

  function handleAuth(data) { saveAuth(data); setAuth(data) }
  function handleGame(data) { saveGame(data); setGame(data) }
  function handleLeave() { clearGame(); setGame(null) }
  function handleLogout() { clearAll(); setAuth(null); setGame(null) }

  useEffect(() => {
    if (!auth || !game) return

    // If game was already in a terminal state when we stored it,
    // skip the server check — the active session is gone (cleaned up
    // after 5s), but we still want to show the result screen.
    if (TERMINAL.includes(game.status)) return

    fetch(`/api/games/${game.gameId}/state`, {
      headers: { Authorization: `Bearer ${auth.token}` }
    })
      .then(res => {
        if (res.status === 401 || res.status === 403) {
          clearAll(); setAuth(null); setGame(null)
        } else if (!res.ok) {
          // Active session gone but no stored terminal status — drop to lobby
          clearGame(); setGame(null)
        }
        // 200 → game is active, stay on GamePage
      })
      .catch(() => { clearGame(); setGame(null) })
  }, []) // only on mount

  if (!auth) return <AuthPage onAuth={handleAuth} />
  if (!game) return <LobbyPage auth={auth} onGame={handleGame} onLogout={handleLogout} />
  return <GamePage auth={auth} game={game} onLeave={handleLeave} />
}