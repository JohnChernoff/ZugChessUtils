package uci;

import org.chernovia.chess.UCI_Parser;

public class UCI_Test {

    public static void main(String[] args) throws InterruptedException {
        String normFEN = "r2q1rk1/pp1nppbp/2p3p1/5n2/3PR3/1BP5/PP1N1PPP/R1BQ2K1 b - - 2 12";
        String mateFEN = "rnbqkbnr/pppp1ppp/4p3/8/6P1/5P2/PPPPP2P/RNBQKBNR b KQkq - 0 1";
        String promFEN = "1n2kbnr/ppPppppp/1r6/8/8/8/PP1PPPPP/RNBQKBNR w KQk - 0 1";
        UCI_Parser parser = new UCI_Parser();
        parser.startEngine(args[0]);
        parser.setOptions(1,2048,2000);
        parser.getBestMoves(promFEN,3,2000).whenComplete((moves,oops) -> print(moves.toString()));
        parser.getBestMove(normFEN,5000).whenComplete((move,oops) -> print(move.toString()));
        Thread.sleep(10000);
        parser.stopEngine();
    }

    public static void print(String msg) {
        System.out.println(msg);
    }
}
