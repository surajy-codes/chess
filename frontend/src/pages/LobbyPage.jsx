import { useState, useEffect, useRef } from 'react'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import styles from './LobbyPage.module.css'

export default function LobbyPage({ auth, onGame, onLogout }) {
  const [searching, setSearching] = useState(false)
  const [roomId, setRoomId] = useState('')
  const [joinId, setJoinId] = useState('')
  const [error, setError] = useState('')
  const clientRef = useRef(null)
  const clientIdRef = useRef(crypto.randomUUID())

  // connect STOMP just for matchmaking
  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${auth.token}` },
      onConnect: () => { clientRef.current = client }
    })
    client.activate()
    return () => client.deactivate()
  }, [auth.token])

  async function createRoom() {
    setError('')
    const res = await fetch('/api/games/room', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${auth.token}` },
      body: JSON.stringify({ username: auth.username })
    })
    const data = await res.json()
    if (!res.ok) { setError('Failed to create room'); return }
    setRoomId(data.roomId)

    // subscribe to be notified when someone joins this room
    if (clientRef.current) {
      clientRef.current.subscribe(`/topic/room/${data.roomId}/joined`, (msg) => {
        const info = JSON.parse(msg.body)
        onGame({ gameId: info.roomId, color: 'WHITE' })
      })
    }
  }

  async function joinRoom() {
    setError('')
    const res = await fetch(`/api/games/${joinId}/join`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${auth.token}` },
      body: JSON.stringify({ username: auth.username })
    })
    const data = await res.json()
    if (!res.ok) { setError(data || 'Failed to join room'); return }
    onGame({ gameId: data.roomId, color: data.color })
  }

  function findMatch() {
    if (!clientRef.current) { setError('Not connected yet, try again'); return }
    setSearching(true)
    setError('')
    const clientId = clientIdRef.current

    clientRef.current.subscribe(`/topic/matchmaking/${clientId}`, (msg) => {
      const data = JSON.parse(msg.body)
      setSearching(false)
      onGame({ gameId: data.gameId, color: data.color })
    })

    clientRef.current.publish({
      destination: '/app/matchmaking/join',
      body: JSON.stringify({ clientId, username: auth.username })
    })
  }

  function cancelSearch() {
    clientRef.current?.publish({
      destination: '/app/matchmaking/leave',
      body: JSON.stringify({ clientId: clientIdRef.current, username: auth.username })
    })
    setSearching(false)
  }

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <span className={styles.logo}>♟ Chess</span>
        <div className={styles.user}>
          <span className="dim">{auth.username}</span>
          <button className="btn-secondary" onClick={onLogout}>Log out</button>
        </div>
      </header>

      <main className={styles.main}>
        <h1 className={styles.heading}>Play Chess</h1>

        {/* Matchmaking */}
        <div className="card" style={{ marginBottom: '1rem' }}>
          <h3 className={styles.cardTitle}>Quick Match</h3>
          <p className="dim" style={{ marginBottom: '1rem' }}>Get paired with a random opponent</p>
          {searching ? (
            <div className={styles.searching}>
              <span className={styles.spinner} />
              Searching for opponent...
              <button className="btn-secondary" onClick={cancelSearch}>Cancel</button>
            </div>
          ) : (
            <button className="btn-primary" onClick={findMatch}>Find Match</button>
          )}
        </div>

        {/* Create room */}
        <div className="card" style={{ marginBottom: '1rem' }}>
          <h3 className={styles.cardTitle}>Create Room</h3>
          <p className="dim" style={{ marginBottom: '1rem' }}>Share the room ID with a friend</p>
          <button className="btn-secondary" onClick={createRoom} style={{ marginBottom: '0.8rem' }}>
            Create Room
          </button>
          {roomId && (
            <div className={styles.roomId}>
              <span className="dim">Room ID:</span>
              <code>{roomId}</code>
              <button className={styles.copy} onClick={() => navigator.clipboard.writeText(roomId)}>Copy</button>
            </div>
          )}
        </div>

        {/* Join room */}
        <div className="card">
          <h3 className={styles.cardTitle}>Join Room</h3>
          <div className={styles.joinRow}>
            <input
              value={joinId}
              onChange={e => setJoinId(e.target.value)}
              placeholder="Paste room ID"
            />
            <button className="btn-primary" onClick={joinRoom}>Join</button>
          </div>
        </div>

        {error && <p className="error" style={{ marginTop: '0.8rem' }}>{error}</p>}
      </main>
    </div>
  )
}