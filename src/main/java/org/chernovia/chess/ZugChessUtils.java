package org.chernovia.chess;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZugChessUtils {

    public static final Pattern VALID_MOVE_PATTERN = Pattern.compile("[a-h][1-8][a-h][1-8][qQrRbBnN]?");
    static Logger logger = Logger.getLogger("ZugChessUtils");

    public static void log(String msg, Level level) {
        logger.log(level,msg);
    }

    /*
     * Returns a SAN representation of given move
     * @param move the move object
     * @param sanBoard the board to make the move on
     * @return San representation of the given move if the move is legal; null otherwise
     */
    public static String getSan(final Move move, final Board sanBoard) {
        if (sanBoard.legalMoves().contains(move)) {
            final Board auxBoard = sanBoard.clone();
            auxBoard.doMove(move);
            Square from = move.getFrom(); Square to = move.getTo();
            Piece piece = sanBoard.getPiece(from);
            final String ending = auxBoard.isMated() ? "#" : auxBoard.isKingAttacked() ? "+" : "";
            if (piece.equals(Piece.BLACK_KING) && from.equals(Square.E8)) {
                if (to.equals(Square.G8)) {
                    return "O-O" + ending;
                } else if (to.equals(Square.C8)) {
                    return "O-O-O" + ending;
                }
            } else if (piece.equals(Piece.WHITE_KING) && from.equals(Square.E1)) {
                if (to.equals(Square.G1)) {
                    return "O-O" + ending;
                } else if (to.equals(Square.C1)) {
                    return "O-O-O" + ending;
                }
            }

            Piece target = sanBoard.getPiece(move.getTo());
            final String takes = (target != Piece.NONE ||
                    (piece.getPieceType() == PieceType.PAWN && !from.getFile().equals(to.getFile()))) ? "x" : "";
            final String promotion = move.getPromotion() != Piece.NONE ? "=" + move.getPromotion().getSanSymbol() : "";
            final String fromStr = getFromString(piece,from,to,sanBoard).toLowerCase();
            final String toStr = to.value().toLowerCase();
            final String sanSymbol = (piece.getSanSymbol().equals("") && takes.equals("x") && fromStr.equals("")) ?
                    from.value().substring(0, 1).toLowerCase() : piece.getSanSymbol();
            return sanSymbol + fromStr + takes + toStr + promotion + ending;
        }
        log("SAN Oops: " + move,Level.WARNING);
        return null;
    }

    public static String getFromString(final Piece piece, final Square from, final Square to, final Board board) {
        if (piece.getPieceType() == PieceType.PAWN || piece.getPieceType() == PieceType.NONE) {
            return "";
        }
        final List<Move> conflictingMoves = board.getPieceLocation(piece)
                .stream()
                .map(square -> new Move(square.value() + to.value(), board.getSideToMove()))
                .filter(move -> board.legalMoves().contains(move) && move.getFrom() != from)
                .toList();
        if (conflictingMoves.size() < 1) {
            return "";
        }
        final boolean conflictOnFile = conflictingMoves.stream().map(move -> move.getFrom().getFile()).anyMatch(file -> file == from.getFile());
        final boolean conflictOnRank = conflictingMoves.stream().map(move -> move.getFrom().getRank()).anyMatch(rank -> rank == from.getRank());
        if (conflictOnFile && conflictOnRank) {
            return from.value();
        } else if (conflictOnFile) {
            return from.value().substring(1, 2);
        } else {
            return from.value().substring(0, 1);
        }
    }

    public static Optional<Move> getMove(final String moveStr, Board board) { // log("Attempted Move: " + moveStr); //TODO: move to chessUtils
        final Matcher moveStrMatcher = VALID_MOVE_PATTERN.matcher(moveStr);
        if (moveStrMatcher.find()) {
            Move move = new Move(moveStrMatcher.group(),board.getSideToMove());
            final String san = getSan(move,board);
            if (san == null) return Optional.empty();
            else {
                log("Attempted Move: " + san,Level.FINE);
                move.setSan(san);
                return Optional.of(move);
            }
        }
        return Optional.empty();
    }
}
