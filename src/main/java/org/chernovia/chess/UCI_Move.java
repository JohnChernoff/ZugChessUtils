package org.chernovia.chess;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.move.Move;

public class UCI_Move {
    Move move;
    String moveString;
    double eval;
    long time;

    public UCI_Move(String m, Board board, double e, long t) {
        moveString = m;
        move = new Move(m,board.getSideToMove());
        move.setSan(ZugChessUtils.getSan(move,board));
        eval = e;
        time = t;
    }

    public String getMoveStr() { return moveString; }
    public Move getMove() { return move; }
    public double getEval() { return eval; }

    String getPromotionString() {
        if (move.getPromotion() == Piece.NONE) return ""; else return "=" + move.getPromotion();
    }

    @Override
    public String toString() {
        return "[Move: " + move.getSan() + ", Eval: " + eval + ", Time: " + time + "]";
    }

}
