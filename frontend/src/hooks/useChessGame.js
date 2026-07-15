import { useState, useEffect, useRef, useCallback } from 'react'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import { Chess } from 'chess.js'

const TERMINAL = ['CHECKMATE', 'STALEMATE', 'DRAW']

export default function useChessGame(auth, game) {
  const [chess] = useState(() => new Chess())
  const [fen, setFen] = useState(chess.fen())
  const [turn, setTurn] = useState(game.turn || 'WHITE')
  const [status, setStatus] = useState(game.status || 'ONGOING')
  const [lastMove, setLastMove] = useState(null)
  const [event, setEvent] = useState(null)
  const [connected, setConnected] = useState(false)

  const clientRef = useRef(null)
  const connectedRef = useRef(false)

  const isOver = TERMINAL.includes(status) || event?.event === 'GAME_OVER'
  const myColor = game.color === 'WHITE' ? 'white' : 'black'
  const isMyTurn = game.color === turn && !isOver

  // ── 1. Restore board state immediately on mount via REST ──────────────────
  // This runs before STOMP connects, so the board is never stuck at the
  // starting position after a page reload.
  useEffect(() => {
    // If the game was already over when stored, restore that state directly.
    if (TERMINAL.includes(game.status)) {
      setStatus(game.status)
      if (game.fen) { chess.load(game.fen); setFen(game.fen) }
      if (game.turn) setTurn(game.turn)
      return // no need for a server call
    }

    fetch(`/api/games/${game.gameId}/state`, {
      headers: { Authorization: `Bearer ${auth.token}` }
    })
      .then(r => r.ok ? r.json() : null)
      .then(state => {
        if (!state) return
        chess.load(state.fen)
        setFen(state.fen)
        setTurn(state.turn)
        setStatus(state.status)
      })
      .catch(() => { })
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  // ── 2. STOMP connection ───────────────────────────────────────────────────
  useEffect(() => {
    // Don't open a WebSocket for a finished game — result comes from stored state.
    if (TERMINAL.includes(game.status)) return

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${auth.token}` },
      reconnectDelay: 0,

      onConnect: () => {
        connectedRef.current = true
        setConnected(true)

        // ── board state updates ─────────────────────────────────────────────
        client.subscribe(`/topic/game/${game.gameId}`, msg => {
          const state = JSON.parse(msg.body)
          chess.load(state.fen)
          setFen(state.fen)
          setTurn(state.turn)
          setStatus(state.status)
          if (state.lastMove) {
            setLastMove({
              from: state.lastMove.slice(0, 2),
              to: state.lastMove.slice(2, 4)
            })
          }

          // Persist latest fen/turn/status so a reload can restore the result
          try {
            const stored = JSON.parse(localStorage.getItem('chess_game') || '{}')
            if (stored.gameId === game.gameId) {
              localStorage.setItem('chess_game', JSON.stringify({
                ...stored,
                fen: state.fen,
                turn: state.turn,
                status: state.status
              }))
            }
          } catch (_) { }
        })

        // ── game events ─────────────────────────────────────────────────────
        client.subscribe(`/topic/game/${game.gameId}/events`, msg => {
          const evt = JSON.parse(msg.body)

          // Suppress disconnect/reconnect events when WE are the one who disconnected/reconnected.
          // The server includes evt.data = 'WHITE' | 'BLACK' (the color that disconnected/reconnected).
          if (evt.event === 'OPPONENT_DISCONNECTED' && evt.data?.toUpperCase() === game.color?.toUpperCase()) return
          if (evt.event === 'OPPONENT_RECONNECTED' && evt.data?.toUpperCase() === game.color?.toUpperCase()) return

          setEvent(evt)

          // Auto-clear transient events after 5 seconds so they don't
          // permanently override status text.
          if (evt.event === 'OPPONENT_RECONNECTED') {
            const captured = evt
            setTimeout(() => setEvent(prev => prev === captured ? null : prev), 5000)
          }
        })

        // Registers this STOMP session server-side (handles reconnect too)
        client.publish({ destination: `/app/game/${game.gameId}/rejoin`, body: '{}' })
      },

      onDisconnect: () => {
        connectedRef.current = false
        setConnected(false)
      }
    })

    client.activate()
    clientRef.current = client
    return () => { client.deactivate(); connectedRef.current = false }
  }, [game.gameId, auth.token]) // eslint-disable-line react-hooks/exhaustive-deps

  const sendMove = useCallback((from, to, promotion = null) => {
    if (!connectedRef.current || !clientRef.current) return false

    // ── Optimistic local update ──────────────────────────────────────────────
    // Apply the move on the client-side chess.js board immediately so the FEN
    // updates before the server round-trip.  Without this, react-chessboard
    // re-renders with the OLD fen and the piece snaps back until the STOMP
    // broadcast arrives — which on checkmate can make it look like the move
    // never happened at all.
    const moveObj = { from, to }
    if (promotion) moveObj.promotion = promotion
    const result = chess.move(moveObj)
    if (result) {
      setFen(chess.fen())
      setLastMove({ from, to })

      // Derive status from the local board state
      if (chess.isCheckmate()) {
        setStatus('CHECKMATE')
      } else if (chess.isStalemate()) {
        setStatus('STALEMATE')
      } else if (chess.isDraw()) {
        setStatus('DRAW')
      } else if (chess.isCheck()) {
        setStatus('CHECK')
      } else {
        setStatus('ONGOING')
      }
      setTurn(chess.turn() === 'w' ? 'WHITE' : 'BLACK')

      // Persist optimistic state to localStorage immediately
      try {
        const stored = JSON.parse(localStorage.getItem('chess_game') || '{}')
        if (stored.gameId === game.gameId) {
          localStorage.setItem('chess_game', JSON.stringify({
            ...stored,
            fen: chess.fen(),
            turn: chess.turn() === 'w' ? 'WHITE' : 'BLACK',
            status: chess.isCheckmate() ? 'CHECKMATE'
              : chess.isStalemate() ? 'STALEMATE'
                : chess.isDraw() ? 'DRAW'
                  : chess.isCheck() ? 'CHECK'
                    : 'ONGOING'
          }))
        }
      } catch (_) { }
    }

    // Send to server — the broadcast back will reconcile state
    clientRef.current.publish({
      destination: `/app/game/${game.gameId}/move`,
      body: JSON.stringify({ from, to, promotion })
    })
    return true
  }, [game.gameId, chess])

  return { fen, turn, status, lastMove, event, isMyTurn, myColor, isOver, sendMove, connected }
}