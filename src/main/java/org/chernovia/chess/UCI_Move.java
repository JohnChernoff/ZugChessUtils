package org.chernovia.chess;

import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

public class UCI_Move {
    Move move;
    String moveString;
    double eval;
    long time;
    Side side;

    public UCI_Move(String m, Side s, double e, long t) {
        moveString = m;
        side = s;
        Square from = Square.fromValue(m.substring(0,2).toUpperCase());
        Square to = Square.fromValue(m.substring(2,4).toUpperCase());
        if (m.length() > 4) {
            String promPiece = m.substring(4);
            move = new Move(from,to,Piece.fromFenSymbol(side == Side.WHITE ? promPiece.toUpperCase() : promPiece.toLowerCase()));
        }
        else move = new Move(from,to);
        eval = e;
        time = t;
    }

    String getPromotionString() {
        if (move.getPromotion() == Piece.NONE) return ""; else return "=" + move.getPromotion();
    }

    @Override
    public String toString() {
        return "[Move: " + move.getFrom() + move.getTo() + getPromotionString() + ", Side: " + side + ", Eval: " + eval + ", Time: " + time + "]";
    }

}
