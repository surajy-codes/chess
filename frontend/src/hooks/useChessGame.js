import { useState, useEffect, useRef, useCallback } from 'react'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import { Chess } from 'chess.js'

export default function useChessGame(auth, game) {
  const [chess]       = useState(() => new Chess())
  const [fen, setFen] = useState(chess.fen())
  const [turn, setTurn]       = useState('WHITE')
  const [status, setStatus]   = useState('ONGOING')  // ONGOING | CHECK | CHECKMATE | STALEMATE | DRAW
  const [lastMove, setLastMove] = useState(null)       // { from, to }
  const [event, setEvent]     = useState(null)         // latest game event message
  const clientRef = useRef(null)
  const connectedRef = useRef(false)

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${auth.token}` },
      onConnect: () => {
        connectedRef.current = true

        // subscribe to board state updates
        client.subscribe(`/topic/game/${game.gameId}`, (msg) => {
          const state = JSON.parse(msg.body)
          chess.load(state.fen)
          setFen(state.fen)
          setTurn(state.turn)
          setStatus(state.status)
          if (state.lastMove) {
            setLastMove({ from: state.lastMove.slice(0, 2), to: state.lastMove.slice(2, 4) })
          }
        })

        // subscribe to game events (disconnect, game over)
        client.subscribe(`/topic/game/${game.gameId}/events`, (msg) => {
          setEvent(JSON.parse(msg.body))
        })

        // register this session with the server (handles reconnect too)
        client.publish({
          destination: `/app/game/${game.gameId}/rejoin`,
          body: '{}'
        })
      }
    })

    client.activate()
    clientRef.current = client

    return () => {
      client.deactivate()
      connectedRef.current = false
    }
  }, [game.gameId, auth.token])

  const sendMove = useCallback((from, to, promotion = null) => {
    if (!connectedRef.current || !clientRef.current) return false

    clientRef.current.publish({
      destination: `/app/game/${game.gameId}/move`,
      body: JSON.stringify({ from, to, promotion })
    })
    return true
  }, [game.gameId])

  const isMyTurn = game.color === turn
  const myColor  = game.color === 'WHITE' ? 'white' : 'black'

  return { fen, turn, status, lastMove, event, isMyTurn, myColor, sendMove }
}