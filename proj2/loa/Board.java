/* Skeleton Copyright (C) 2015, 2020 Paul N. Hilfinger and the Regents of the
 * University of California.  All rights reserved. */
package loa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

import java.util.regex.Pattern;

import static loa.Piece.*;
import static loa.Square.*;

/** Represents the state of a game of Lines of Action.
 *  @author Kelvin Le
 */
class Board {

    /** Default number of moves for each side that results in a draw. */
    static final int DEFAULT_MOVE_LIMIT = 60;

    /** Pattern describing a valid square designator (cr). */
    static final Pattern ROW_COL = Pattern.compile("^[a-h][1-8]$");

    /** A Board whose initial contents are taken from INITIALCONTENTS
     *  and in which the player playing TURN is to move. The resulting
     *  Board has
     *        get(col, row) == INITIALCONTENTS[row][col]
     *  Assumes that PLAYER is not null and INITIALCONTENTS is 8x8.
     *
     *  CAUTION: The natural written notation for arrays initializers puts
     *  the BOTTOM row of INITIALCONTENTS at the top.
     */
    Board(Piece[][] initialContents, Piece turn) {
        initialize(initialContents, turn);
    }

    /** A new board in the standard initial position. */
    Board() {
        this(INITIAL_PIECES, BP);
    }

    /** A Board whose initial contents and state are copied from
     *  BOARD. */
    Board(Board board) {
        this();
        copyFrom(board);
    }

    /** Set my state to CONTENTS with SIDE to move. */
    void initialize(Piece[][] contents, Piece side) {
        _turn = side;
        _moveLimit = DEFAULT_MOVE_LIMIT;
        for (int i = 0; i < contents.length; i += 1) {
            int k = contents.length;
            k = k * i;
            for (int j = 0; j < contents[i].length; j += 1) {
                _board[k + j] = contents[i][j];
            }
        }
        _winner = null;
    }

    /** Set me to the initial configuration. */
    void clear() {
        initialize(INITIAL_PIECES, BP);
    }

    /** Set my state to a copy of BOARD. */
    void copyFrom(Board board) {
        if (board == this) {
            return;
        }
        for (int i = 0; i < board._board.length; i += 1) {
            _board[i] = board._board[i];
        }
        _turn = board._turn;
        _moves.addAll(board._moves);
    }

    /** Return the contents of the square at SQ. */
    Piece get(Square sq) {
        return _board[sq.index()];
    }

    /** Set the square at SQ to V and set the side that is to move next
     *  to NEXT, if NEXT is not null. */
    void set(Square sq, Piece v, Piece next) {
        _board[sq.index()] = v;
        if (next != null) {
            _turn = next;
        }
    }

    /** Set the square at SQ to V, without modifying the side that
     *  moves next. */
    void set(Square sq, Piece v) {
        set(sq, v, null);
    }

    /** Set limit on number of moves by each side that results in a tie to
     *  LIMIT, where 2 * LIMIT > movesMade(). */
    void setMoveLimit(int limit) {
        if (2 * limit <= movesMade()) {
            throw new IllegalArgumentException("move limit too small");
        }
        _moveLimit = 2 * limit;
    }

    /** Assuming isLegal(MOVE), make MOVE. This function assumes that
     *  MOVE.isCapture() will return false.  If it saves the move for
     *  later retraction, makeMove itself uses MOVE.captureMove() to produce
     *  the capturing move. */
    void makeMove(Move move) {
        assert isLegal(move);
        if (get(move.getTo()).equals(EMP)) {
            move = Move.mv(move.getFrom(), move.getTo());
            set(move.getFrom(), EMP);
            set(move.getTo(), _turn, _turn.opposite());
        } else {
            move = move.captureMove();
            set(move.getFrom(), EMP);
            set(move.getTo(), _turn, _turn.opposite());
        }
        _moves.add(move);
        _moveLimit = _moveLimit - 1;
        _winnerKnown = false;
        _subsetsInitialized = false;
    }

    /** Retract (unmake) one move, returning to the state immediately before
     *  that move.  Requires that movesMade () > 0. */
    void retract() {
        assert movesMade() > 0;
        _moveLimit += 1;
        Move lastMove = _moves.get(_moves.size() - 1);
        set(lastMove.getFrom(), _turn.opposite());
        if (!lastMove.isCapture()) {
            set(lastMove.getTo(), EMP);
        } else {
            set(lastMove.getTo(), _turn);
        }
        _turn = _turn.opposite();
        _moves.remove(_moves.size() - 1);
        _winnerKnown = false;
        _subsetsInitialized = false;
    }

    /** Return the Piece representing who is next to move. */
    Piece turn() {
        return _turn;
    }

    /** Return true iff FROM - TO is a legal move for the player currently on
     *  move. */
    boolean isLegal(Square from, Square to) {
        if (from.isValidMove(to) && exists(to.col(), to.row())
                && !blocked(from, to) && get(from).equals(_turn)) {
            int dir = from.direction(to);
            int reverseDir = to.direction(from);
            int numOnLine = 1;
            for (int i = 1; i < Math.sqrt(_board.length); i += 1) {
                if (from.moveDest(dir, i) != null) {
                    if (_board[from.moveDest(dir, i).index()] != EMP) {
                        numOnLine += 1;
                    }
                }
                if (from.moveDest(reverseDir, i) != null) {
                    if (_board[from.moveDest(reverseDir, i).index()] != EMP) {
                        numOnLine += 1;
                    }
                }
            }
            if (numOnLine == from.distance(to)) {
                return true;
            }
        }
        return false;
    }

    /** Return true iff MOVE is legal for the player currently on move.
     *  The isCapture() property is ignored. */
    boolean isLegal(Move move) {
        return isLegal(move.getFrom(), move.getTo());
    }

    /** Return a sequence of all legal moves from this position. */
    List<Move> legalMoves() {
        possibleMoves = new ArrayList<>();
        for (int i = 0; i < ALL_SQUARES.length; i += 1) {
            for (int j = 0; j < ALL_SQUARES.length; j += 1) {
                if (_board[ALL_SQUARES[i].index()].equals(_turn)
                        && ALL_SQUARES[i].isValidMove(ALL_SQUARES[j])) {
                    if (isLegal(ALL_SQUARES[i], ALL_SQUARES[j])) {
                        possibleMoves.add(Move.mv(ALL_SQUARES[i],
                                ALL_SQUARES[j]));
                    }
                }
            }
        }
        return possibleMoves;
    }

    /** Return true iff the game is over (either player has all his
     *  pieces continguous or there is a tie). */
    boolean gameOver() {
        return winner() != null;
    }

    /** Return true iff SIDE's pieces are continguous. */
    boolean piecesContiguous(Piece side) {
        return getRegionSizes(side).size() == 1;
    }

    /** Return the winning side, if any.  If the game is not over, result is
     *  null.  If the game has ended in a tie, returns EMP. */
    Piece winner() {
        if (!_winnerKnown) {
            computeRegions();
            if (_moveLimit == 0 && getRegionSizes(_turn).size() > 1
                    && getRegionSizes(_turn.opposite()).size() > 1) {
                _winner = EMP;
            } else if (getRegionSizes(_turn).size() == 1
                    && getRegionSizes(_turn.opposite()).size() == 1) {
                _winner = _turn.opposite();
            } else if (getRegionSizes(_turn).size() == 1
                    && getRegionSizes(_turn.opposite()).size() > 1) {
                _winner = _turn;
            } else if (getRegionSizes(_turn.opposite()).size() == 1
                    && getRegionSizes(_turn).size() > 1) {
                _winner = _turn.opposite();
            } else {
                _winner = null;
            }
            _winnerKnown = true;
        }
        return _winner;
    }

    /** Return the total number of moves that have been made (and not
     *  retracted).  Each valid call to makeMove with a normal move increases
     *  this number by 1. */
    int movesMade() {
        return _moves.size();
    }

    @Override
    public boolean equals(Object obj) {
        Board b = (Board) obj;
        return Arrays.deepEquals(_board, b._board) && _turn == b._turn;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(_board) * 2 + _turn.hashCode();
    }

    @Override
    public String toString() {
        Formatter out = new Formatter();
        out.format("===%n");
        for (int r = BOARD_SIZE - 1; r >= 0; r -= 1) {
            out.format("    ");
            for (int c = 0; c < BOARD_SIZE; c += 1) {
                out.format("%s ", get(sq(c, r)).abbrev());
            }
            out.format("%n");
        }
        out.format("Next move: %s%n===", turn().fullName());
        return out.toString();
    }

    /** Return true if a move from FROM to TO is blocked by an opposing
     *  piece or by a friendly piece on the target square. */
    private boolean blocked(Square from, Square to) {
        if (_board[to.index()] == _turn) {
            return true;
        }
        int dir = from.direction(to);
        for (int i = 1; i < from.distance(to); i += 1) {
            if (_board[from.moveDest(dir, i).index()] == _turn.opposite()) {
                return true;
            }
        }
        return false;
    }

    /** Return the size of the as-yet unvisited cluster of squares
     *  containing P at and adjacent to SQ.  VISITED indicates squares that
     *  have already been processed or are in different clusters.  Update
     *  VISITED to reflect squares counted. */
    private int numContig(Square sq, boolean[][] visited, Piece p) {
        if (_board[sq.index()] == EMP) {
            return 0;
        }
        if (_board[sq.index()] != p) {
            return 0;
        }
        if (visited[sq.col()][sq.row()]) {
            return 0;
        }
        visited[sq.col()][sq.row()] = true;
        int count = 1;
        for (Square square : sq.adjacent()) {
            count += numContig(square, visited, p);
        }
        return count;
    }

    /** Set the values of _whiteRegionSizes and _blackRegionSizes. */
    private void computeRegions() {
        if (_subsetsInitialized) {
            return;
        }
        _whiteRegionSizes.clear();
        _blackRegionSizes.clear();
        boolean[][] checking = new boolean[_board.length][_board.length];
        for (int i = 0; i < ALL_SQUARES.length; i += 1) {
            if (_board[ALL_SQUARES[i].index()] != EMP) {
                int aRegion = numContig(ALL_SQUARES[i],
                        checking, _board[ALL_SQUARES[i].index()]);
                if (aRegion != 0) {
                    if (_board[ALL_SQUARES[i].index()] == BP) {
                        _blackRegionSizes.add(aRegion);
                    } else {
                        _whiteRegionSizes.add(aRegion);
                    }
                }
            }
        }
        Collections.sort(_whiteRegionSizes, Collections.reverseOrder());
        Collections.sort(_blackRegionSizes, Collections.reverseOrder());
        _subsetsInitialized = true;
    }

    /** Return the sizes of all the regions in the current union-find
     *  structure for side S. */
    List<Integer> getRegionSizes(Piece s) {
        computeRegions();
        if (s == WP) {
            return _whiteRegionSizes;
        } else {
            return _blackRegionSizes;
        }
    }

    /** List of all moves a piece can make. */
    private List<Move> possibleMoves;

    /** The standard initial configuration for Lines of Action (bottom row
     *  first). */
    static final Piece[][] INITIAL_PIECES = {
        { EMP, BP,  BP,  BP,  BP,  BP,  BP,  EMP },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { EMP, BP,  BP,  BP,  BP,  BP,  BP,  EMP }
    };

    /** Current contents of the board.  Square S is at _board[S.index()]. */
    private final Piece[] _board = new Piece[BOARD_SIZE  * BOARD_SIZE];

    /** List of all unretracted moves on this board, in order. */
    private final ArrayList<Move> _moves = new ArrayList<>();
    /** Current side on move. */
    private Piece _turn;
    /** Limit on number of moves before tie is declared.  */
    private int _moveLimit;
    /** True iff the value of _winner is known to be valid. */
    private boolean _winnerKnown;
    /** Cached value of the winner (BP, WP, EMP (for tie), or null (game still
     *  in progress).  Use only if _winnerKnown. */
    private Piece _winner;

    /** True iff subsets computation is up-to-date. */
    private boolean _subsetsInitialized;

    /** List of the sizes of continguous clusters of pieces, by color. */
    private final ArrayList<Integer>
        _whiteRegionSizes = new ArrayList<>(),
        _blackRegionSizes = new ArrayList<>();
}
