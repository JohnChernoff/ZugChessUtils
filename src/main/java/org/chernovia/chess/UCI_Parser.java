package org.chernovia.chess;

import com.github.bhlangonijr.chesslib.Board;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UCI_Parser {

    private Process engineProcess;
    private BufferedReader processReader;
    private OutputStreamWriter processWriter;
    private String id = "?";
    private Logger logger;
    private boolean calculating = false;
    private final long calcWaitInterval = 250;
    private final long minMoveTime = 250;
    public boolean running = false;

    public UCI_Parser() {
        this(Level.INFO);
    }
    public UCI_Parser(Level level) {
        setLogger(Logger.getLogger("UCI_Parser"));
        setLogLevel(level);
    }

    public void log(String msg) {
        log(msg,Level.FINE);
    }

    public void log(String msg, Level level) {
        logger.log(level,msg);
    }
    public void setLogLevel(Level level) { logger.setLevel(level); }
    public void setLogger(Logger l) { logger = l; }

    /**
     * Starts Stockfish engine as a process and initializes it
     *
     * @return True on success. False otherwise
     */
    public boolean startEngine(String path) {
        try {
            engineProcess = Runtime.getRuntime().exec(path);
            processReader = new BufferedReader(new InputStreamReader(engineProcess.getInputStream()));
            processWriter = new OutputStreamWriter(engineProcess.getOutputStream());
        } catch (IOException e) {
            log("Engine IO Error: " + e.getMessage(),Level.WARNING); //e.printStackTrace();
            return false;
        }
        id = String.valueOf(engineProcess.pid());
        log("New Engine Process: " + id);
        running = true;
        return true;
    }

    public String getID() {
        return id;
    }

    /**
     * Takes in any valid UCI command and executes it
     *
     * @param command
     */
    public void sendCommand(String command) { //System.out.println(id + " -> CMD: " + command);
        if (processWriter != null) try {
            processWriter.write(command + "\n");
            processWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This is generally called right after 'sendCommand' for getting the raw
     * output from Stockfish
     *
     * @param waitTime Time in milliseconds for which the function waits before
     *                 reading the output. Useful when a long running command is
     *                 executed
     * @return Raw output from Stockfish
     */
    public String getOutput(int waitTime) {
        return getOutput("readyok", waitTime);
    }

    public String getOutput(String keyString, int waitTime) {
        StringBuffer buffer = new StringBuffer();
        try { //System.out.println("Sleeping: " + waitTime);
            Thread.sleep(waitTime);
            sendCommand("isready");
            while (true) {
                String text = processReader.readLine(); //System.out.println(id + ": " + text);
                buffer.append(text + "\n");
                if (text.startsWith(keyString)) break;

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }

    public void setMulti(int lines) {
        sendCommand("setoption name MultiPV value " + lines);
    }

    public void setFEN(String fen) {
        sendCommand("position fen " + fen);
    }

    public void startAnalyzing(String fen, int lines, int moveTime) {
        long waitTime = waitToCalculate(calcWaitInterval);
        calculating = true;
        long actualMoveTime = moveTime > waitTime ? moveTime - waitTime : minMoveTime;
        setFEN(fen);
        setMulti(lines);
        sendCommand("go movetime " + actualMoveTime);
    }

    /**
     * This function returns the best move for a given position after
     * calculating for 'moveTime' ms
     *
     * @param fen      Position string
     * @param moveTime in milliseconds
     * @return A Completable Future containing the Best Move (as UCI_Move)
     */
    public CompletableFuture<UCI_Move> getBestMove(String fen, int moveTime) {
        return getBestMoves(fen,1,moveTime).handle((moves,oops) -> moves.get(0));
    }

    public CompletableFuture<List<UCI_Move>> getBestMoves(String fen, int lines, int moveTime) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            startAnalyzing(fen,lines,moveTime);
            Board board = new Board();
            board.loadFromFen(fen);
            List<UCI_Move> moves = new ArrayList<>(lines);
            for (int i = 0; i < lines; i++) moves.add(null);
            try {
                while (true) {
                    String text = processReader.readLine();
                    String[] tokens = text.split(" ");
                    if (tokens[0].equalsIgnoreCase("info")) { //for (String token : tokens) System.out.print(token + ","); System.out.println(); //"Info: " + text);
                        getField(tokens, "multipv")
                                .ifPresent(line -> getField(tokens, "pv")
                                        .ifPresent(move -> getField(tokens, "cp")
                                                .ifPresentOrElse(eval -> moves.set(Integer.parseInt(line) - 1,
                                                                new UCI_Move(move, board,parseEval(eval),System.currentTimeMillis() - startTime)),
                                                        () -> getField(tokens, "score").ifPresent(eval -> moves.set(Integer.parseInt(line) - 1,
                                                                new UCI_Move(move, board,parseEval(eval),System.currentTimeMillis() - startTime))))));

                    } else if (tokens[0].equalsIgnoreCase("bestmove")) break;
                }
            } catch (Exception e) {
                log("Best Moves Generation Error: " + e.getMessage(),Level.WARNING);
                e.printStackTrace();
            }
            calculating = false;
            moves.removeIf(Objects::isNull);
            return moves;
        });
    }

    long waitToCalculate(long interval) {
        long startTime = System.currentTimeMillis();
        while (calculating) {
            try { Thread.sleep(interval); } catch (InterruptedException ignore) {}
        }
        return System.currentTimeMillis() - startTime;
    }

    double parseEval(String eval) {
        return eval.equalsIgnoreCase("mate") ? 999 : Integer.parseInt(eval);
    }

    Optional<String> getField(String[] tokens, String field) {
        if (tokens.length > 1) {
            for (int i=0; i<tokens.length-1; i++) {
                if (tokens[i].equalsIgnoreCase(field)) {
                    return Optional.of(tokens[i + 1]);
                }
            }
        }
        return Optional.empty();
    }

    public void setOptions(int threads, int hashsize, int elo) {
        sendCommand("setoption name Threads value " + threads);
        sendCommand("setoption name Hash value " + hashsize);
        setElo(elo);
        log("Options set -> threads: " + threads + ", hash: " + hashsize + ", elo: " + elo);
    }

    public void setElo(int elo) {
        sendCommand("setoption name UCI_LimitStrength value true");
        sendCommand("setoption name UCI_Elo value " + elo);
    }

    /**
     * Stops Stockfish and cleans up before closing it
     */
    public void stopEngine() {
        if (engineProcess != null) {
            try {
                sendCommand("quit");
                if (processReader != null) processReader.close();
                if (processWriter != null) processWriter.close();
                if (!engineProcess.waitFor(5000, TimeUnit.MILLISECONDS)) {
                    engineProcess.destroyForcibly();
                }
            } catch (IOException | InterruptedException e) {
                log("Error closing engine: " + e.getMessage(),Level.WARNING);
            }
            log("Engine stopped: " + id);
            running = false;
        }
    }

    /**
     * Draws the current state of the chess board
     *
     * @param fen Position string
     */
    public void drawBoard(String fen) {
        sendCommand("position fen " + fen);
        sendCommand("d");

        String[] rows = getOutput(0).split("\n");

        for (int i = 1; i < 18; i++) {
            System.out.println(rows[i]);
        }
    }

}
