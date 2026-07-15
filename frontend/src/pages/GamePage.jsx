import { useState } from 'react'
import { Chessboard } from 'react-chessboard'
import useChessGame from '../hooks/useChessGame'
import styles from './GamePage.module.css'

export default function GamePage({ auth, game, onLeave }) {
  const {
    fen, turn, status, lastMove,
    event, isMyTurn, myColor, isOver,
    sendMove, connected
  } = useChessGame(auth, game)

  // Track the pending promotion move so the promotion dialog can complete it
  const [promotionMove, setPromotionMove] = useState(null)

  function onDrop(sourceSquare, targetSquare, piece) {
    if (!isMyTurn || isOver) return false

    // Detect promotion — let the built-in dialog handle piece selection
    const isPromotion =
      piece?.[1] === 'P' &&
      ((myColor === 'white' && targetSquare[1] === '8') ||
        (myColor === 'black' && targetSquare[1] === '1'))

    if (isPromotion) {
      // Stash the move; the promotion dialog will call onPromotionPieceSelect
      setPromotionMove({ from: sourceSquare, to: targetSquare })
      return false // don't finalize yet — wait for piece selection
    }

    sendMove(sourceSquare, targetSquare, null)
    return true
  }

  // Called when the user picks a piece from the promotion dialog
  function onPromotionPieceSelect(piece, promoteFromSquare, promoteToSquare) {
    const from = promoteFromSquare || promotionMove?.from
    const to = promoteToSquare || promotionMove?.to
    if (!from || !to) return false
    // piece looks like "wQ", "bR", etc. — extract the letter and lowercase it
    const promotionPiece = piece?.[1]?.toLowerCase() || 'q'
    sendMove(from, to, promotionPiece)
    setPromotionMove(null)
    return true
  }

  // Tell react-chessboard when to show the promotion dialog
  function onPromotionCheck(sourceSquare, targetSquare, piece) {
    return (
      piece?.[1] === 'P' &&
      ((myColor === 'white' && targetSquare[1] === '8') ||
        (myColor === 'black' && targetSquare[1] === '1'))
    )
  }

  // ── Status text ─────────────────────────────────────────────────────────
  // Terminal states ALWAYS take priority over event messages.
  function statusText() {
    if (status === 'CHECKMATE') {
      // turn is now the LOSING side (the side that was mated).
      // If that side is us, we lost.
      return turn === game.color
        ? '♚ You lost — checkmate'
        : '♛ You won — checkmate!'
    }
    if (status === 'STALEMATE') return '½ Draw — stalemate'
    if (status === 'DRAW') return '½ Draw'

    // GAME_OVER comes from disconnect forfeit — higher priority than transient events
    if (event?.event === 'GAME_OVER') return '🏁 ' + event.message

    // Transient events — only shown while they are set (auto-cleared after 5s)
    if (event?.event === 'OPPONENT_DISCONNECTED') return '⚠ ' + event.message
    if (event?.event === 'OPPONENT_RECONNECTED') return '✓ Opponent reconnected'

    if (status === 'CHECK') return isMyTurn ? '⚠ You are in check!' : 'Opponent is in check'
    if (!connected) return 'Reconnecting...'
    return isMyTurn ? 'Your turn' : "Opponent's turn"
  }

  function statusColor() {
    if (status === 'CHECKMATE') {
      return turn === game.color ? 'var(--danger)' : 'var(--accent)'
    }
    if (status === 'STALEMATE' || status === 'DRAW') return 'var(--text-dim)'
    if (event?.event === 'GAME_OVER') return '#e0a03a'
    if (event?.event === 'OPPONENT_DISCONNECTED') return 'var(--danger)'
    if (event?.event === 'OPPONENT_RECONNECTED') return 'var(--accent)'
    if (status === 'CHECK') return '#e0a03a'
    if (!connected) return 'var(--text-dim)'
    if (isMyTurn) return 'var(--accent)'
    return 'var(--text-dim)'
  }

  // Highlight last-move squares
  const customSquareStyles = {}
  if (lastMove) {
    customSquareStyles[lastMove.from] = { background: 'rgba(129,182,76,0.35)' }
    customSquareStyles[lastMove.to] = { background: 'rgba(129,182,76,0.35)' }
  }

  const opponentColor = myColor === 'white' ? 'black' : 'white'

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <span className={styles.logo}>♟ Chess</span>
        <button className="btn-secondary" onClick={onLeave}>← Leave game</button>
      </header>

      <main className={styles.main}>
        {/* Opponent row */}
        <div className={styles.playerBar}>
          <span className={styles.colorDot}
            style={{ background: opponentColor === 'white' ? '#eee' : '#333' }} />
          <span>Opponent</span>
          {!isMyTurn && !isOver && <span className={styles.turnPip} />}
        </div>

        {/* Board */}
        <div className={styles.boardWrap}>
          <Chessboard
            position={fen}
            onPieceDrop={onDrop}
            onPromotionPieceSelect={onPromotionPieceSelect}
            onPromotionCheck={onPromotionCheck}
            boardOrientation={myColor}
            customBoardStyle={{ borderRadius: '6px', boxShadow: '0 4px 24px rgba(0,0,0,0.5)' }}
            customDarkSquareStyle={{ backgroundColor: '#769656' }}
            customLightSquareStyle={{ backgroundColor: '#eeeed2' }}
            customSquareStyles={customSquareStyles}
            arePiecesDraggable={isMyTurn && !isOver}
          />
        </div>

        {/* My row */}
        <div className={styles.playerBar}>
          <span className={styles.colorDot}
            style={{ background: myColor === 'white' ? '#eee' : '#333' }} />
          <span>{auth.username} (you)</span>
          {isMyTurn && !isOver && <span className={styles.turnPip} />}
        </div>

        {/* Status */}
        <div className={styles.status} style={{ color: statusColor() }}>
          {statusText()}
        </div>

        {isOver && (
          <button
            className="btn-primary"
            onClick={onLeave}
            style={{ marginTop: '0.8rem', width: '100%' }}
          >
            Back to Lobby
          </button>
        )}
      </main>
    </div>
  )
}