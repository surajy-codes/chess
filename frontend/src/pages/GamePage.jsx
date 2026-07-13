import { useState, useCallback } from 'react'
import { Chessboard } from 'react-chessboard'
import useChessGame from '../hooks/useChessGame'
import styles from './GamePage.module.css'

export default function GamePage({ auth, game, onLeave }) {
  const { fen, turn, status, lastMove, event, isMyTurn, myColor, sendMove } = useChessGame(auth, game)
  const [moveError, setMoveError] = useState('')

  const isOver = ['CHECKMATE', 'STALEMATE', 'DRAW'].includes(status) ||
                 event?.event === 'GAME_OVER'

  function onDrop(sourceSquare, targetSquare, piece) {
    // only allow moves when it's your turn and game isn't over
    if (!isMyTurn || isOver) return false

    // client-side promotion detection (pawn reaching last rank)
    const promotion = piece?.[1] === 'P' &&
      ((myColor === 'white' && targetSquare[1] === '8') ||
       (myColor === 'black' && targetSquare[1] === '1'))
      ? 'q' : null  // auto-promote to queen for simplicity

    setMoveError('')
    sendMove(sourceSquare, targetSquare, promotion)
    return true  // return true to keep the piece in place while we wait for server confirmation
  }

  function statusText() {
    if (event?.event === 'GAME_OVER')             return event.message
    if (event?.event === 'OPPONENT_DISCONNECTED') return event.message
    if (event?.event === 'OPPONENT_RECONNECTED')  return 'Opponent reconnected'
    if (status === 'CHECKMATE') return turn === game.color ? 'You lost — checkmate' : 'You won — checkmate!'
    if (status === 'STALEMATE') return 'Draw — stalemate'
    if (status === 'DRAW')      return 'Draw'
    if (status === 'CHECK')     return isMyTurn ? 'You are in check!' : 'Opponent is in check'
    return isMyTurn ? 'Your turn' : "Opponent's turn"
  }

  function statusColor() {
    if (isOver || event?.event === 'GAME_OVER') return '#e0a03a'
    if (event?.event === 'OPPONENT_DISCONNECTED') return 'var(--danger)'
    if (status === 'CHECK') return '#e0a03a'
    if (isMyTurn) return 'var(--accent)'
    return 'var(--text-dim)'
  }

  const customSquareStyles = {}
  if (lastMove) {
    customSquareStyles[lastMove.from] = { background: 'rgba(129,182,76,0.35)' }
    customSquareStyles[lastMove.to]   = { background: 'rgba(129,182,76,0.35)' }
  }

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <span className={styles.logo}>♟ Chess</span>
        <button className="btn-secondary" onClick={onLeave}>← Leave game</button>
      </header>

      <main className={styles.main}>
        {/* Opponent info */}
        <div className={styles.playerBar}>
          <span className={styles.colorDot} style={{ background: myColor === 'white' ? '#333' : '#eee' }} />
          <span>Opponent</span>
          {!isMyTurn && !isOver && <span className={styles.turnPip} />}
        </div>

        {/* Board */}
        <div className={styles.boardWrap}>
          <Chessboard
            position={fen}
            onPieceDrop={onDrop}
            boardOrientation={myColor}
            customBoardStyle={{ borderRadius: '6px', boxShadow: '0 4px 24px rgba(0,0,0,0.5)' }}
            customDarkSquareStyle={{ backgroundColor: '#769656' }}
            customLightSquareStyle={{ backgroundColor: '#eeeed2' }}
            customSquareStyles={customSquareStyles}
            arePiecesDraggable={isMyTurn && !isOver}
          />
        </div>

        {/* My info */}
        <div className={styles.playerBar}>
          <span className={styles.colorDot} style={{ background: myColor === 'white' ? '#eee' : '#333' }} />
          <span>{auth.username} (you)</span>
          {isMyTurn && !isOver && <span className={styles.turnPip} />}
        </div>

        {/* Status */}
        <div className={styles.status} style={{ color: statusColor() }}>
          {statusText()}
        </div>

        {moveError && <p className="error" style={{ textAlign: 'center' }}>{moveError}</p>}

        {isOver && (
          <button className="btn-primary" onClick={onLeave} style={{ marginTop: '0.8rem', width: '100%' }}>
            Back to Lobby
          </button>
        )}
      </main>
    </div>
  )
}