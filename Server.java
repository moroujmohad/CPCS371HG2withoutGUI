
import java.net.*;
import java.io.*;
import java.util.Date;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Random;

// Server side application
public class Server {

    // ArrayList of String, used for the dictionary of the game
    static ArrayList<String> dictionary;

    // Count current number of sessions 
    static int sessionsCounter = 0;

    public static void main(String args[]) throws Exception {

        // Connection initiation 
        try {
            // Creating TCP server socket
            ServerSocket serverSocket = new ServerSocket(4000);
            System.out.println(new Date() + ": Server started at socket " + serverSocket.getLocalPort());

            // Import text file, add words to an array list
            importDictionary();

            // The server will always listens to clients requests 
            // each pair of clients will play within the same thread (game session)
            while (true) {
                // increment session counter for each two players
                sessionsCounter++;
                System.out.println("Wait for players to join session " + sessionsCounter);

                Socket p1 = serverSocket.accept();  // accept player 1
                System.out.println(new Date() + ": Player 1 joiend session " + sessionsCounter);
                System.out.println("Player1's IP address " + p1.getInetAddress().getHostAddress()); //get player 1 information
                new DataOutputStream(p1.getOutputStream()).writeInt(1); // inform client application that the player 1 has joined

                Socket p2 = serverSocket.accept();  // accept player 2
                System.out.println(new Date() + ": Player 2 joiend session " + sessionsCounter);
                System.out.println("Player2's IP address " + p1.getInetAddress().getHostAddress()); //get player 2 information
                new DataOutputStream(p2.getOutputStream()).writeInt(2); // inform client application that the player 2 has joined

                System.out.println(new Date() + ": Start a thread for session " + sessionsCounter);
                System.out.println("Session " + sessionsCounter + ": The Game is started ");

                // start the game
                GameSession game = new GameSession(p1, p2, sessionsCounter, dictionary);

                // start a new thread for session so the server can serve other clients at the same time
                new Thread(game).start();

            }

        } catch (IOException e) {
            System.out.println(e);

        }
    }

    // importDictionary is a method used to import input file that contains a list of words
    public static void importDictionary() throws Exception {

        try {
            // Import dictionary file
            File file = new File("dictionary.txt"); // create file object
            Scanner readFile = new Scanner(file); // read from file
            dictionary = new ArrayList(); // to store words
            while (readFile.hasNextLine()) {
                dictionary.add(readFile.next()); // add a word to the list
            }

            System.out.println("A dictionary with " + dictionary.size() + " was imported to the server\n");
        } catch (Exception e) {

            System.out.println("SERVER CAN NOT IMPORT THE DICTIONARY FILE");
            System.out.println(e);

            System.exit(0);
        }
    }

}

// Game Session for each two players joined the game
class GameSession extends Thread {

    // ------ Variable declaration ------ //
    // session id
    private static int sessionId;
    // players' sockets
    private final Socket player1;
    private final Socket player2;
    // read from players
    private static DataInputStream fromP1;
    private static DataInputStream fromP2;
    // write to players
    private static DataOutputStream toP1;
    private static DataOutputStream toP2;
    // number of left failed attempts
    private static int p1Attempts;
    private static int p2Attempts;
    // players' scores
    static int player1_score = 0;
    static int player2_score = 0;
    // each player has different view of the word according to his previous guesses
    static char[] currentWordView;
    // players' guess
    private static String guess;
    // game dictionary
    private final ArrayList<String> dictionary;
    // game word
    static String gameWord;
    // who is playing?
    static int playerRole;
    // round number
    private int roundNum = 0;
    private boolean endRound = false;
    private boolean winner = false;

    // game started here after two players joined the server
    public GameSession(Socket player1, Socket player2, int sessionId, ArrayList<String> dictionary) {
        this.player1 = player1;
        this.player2 = player2;
        this.sessionId = sessionId;
        this.dictionary = dictionary;
    }

    // To pick a random word from dictionary list
    public String pickWord() {

        Random random = new Random(); // create Random object

        // pick random word from the dictionary list
        return dictionary.get(random.nextInt(dictionary.size()));

    }

    @Override
    public void run() {

        // game started
        try {

            // I/O streams read from and write to clients
            fromP1 = new DataInputStream(player1.getInputStream());
            toP1 = new DataOutputStream(player1.getOutputStream());
            fromP2 = new DataInputStream(player2.getInputStream());
            toP2 = new DataOutputStream(player2.getOutputStream());

            while (true) {
                // increment number of rounds
                roundNum++;

                // pick a round word, then replace all letters with '-'
                gameWord = pickWord();
                createWordView();

                // reset left attempts
                p1Attempts = p2Attempts = 5;

                // reset endRound, winner
                endRound = false;
                winner = false;

                // SERVER LOG // print the word of this round to the server 
                System.out.println("Session " + sessionId + ", round " + roundNum
                        + ": The game word is: " + gameWord);

                // signal to player 1 to start the game
                toP1.writeInt(2);

                while (true) {

                    if (!endRound && !winner) {
                        playerTurn(1);
                    } // no more left attempts, or there is a winner
                    else {
                        endRound();  // E
                        break;
                    }
                    if (!endRound && !winner) {
                        playerTurn(2);
                    } // no more left attempts, or there is a winner
                    else {
                        endRound();  // E
                        break;
                    }

                }
            }

        } catch (Exception e) {
            System.out.println("ERROR AT GAME SESSION!");
            System.out.println(e.getMessage());
        }

    }

    // intialize the current view of the chosen word to both players
    public static void createWordView() {
        currentWordView = new char[gameWord.length()];
        for (int i = 0; i < gameWord.length(); i++) {
            currentWordView[i] = '-';

        }
    }

    // check if there is no more attempts, or there is a winner // E
    public void endRound() throws IOException {

        // send current results + question to both players // F
        toP1.writeUTF("\nPlayer 1 score is: " + player1_score
                + "\nPlayer 2 score is: " + player2_score
                + "\nEnter 'y' if you want to play another round .. ");
        toP2.writeUTF("\nPlayer 1 score is: " + player1_score
                + "\nPlayer 2 score is: " + player2_score
                + "\nEnter 'y' if you want to play another round .. ");

        // SERVER LOG
        System.out.println("Session " + sessionId + ", round " + roundNum
                + ": \nPlayer 1 score is: " + player1_score
                + "\nPlayer 2 score is: " + player2_score);

        if (!fromP1.readBoolean() || !fromP2.readBoolean()) { // G

            // final result
            String result;
            if (player1_score > player2_score) {
                result = "Player 1 won!";
            } else if (player1_score < player2_score) {
                result = "Player 2 won!";
            } else {
                result = "Server won!";
            }

            // one player or both, don't want to play another round // H
            toP1.writeBoolean(true);
            toP2.writeBoolean(true);

            // send who win+ bye // I
            toP1.writeUTF(result + "\nSee you soon, bye!");
            toP2.writeUTF(result + "\nSee you soon, bye!");

            // SERVER LOG
            System.out.println("Session " + sessionId + ", round " + roundNum
                    + ": GameOver, " + result);

            // close players connection
            player1.close();
            player2.close();

        } else {

            // both want to play another round // H
            toP1.writeBoolean(false);
            toP2.writeBoolean(false);

        }

    }

    // interaction between client and game server
    public void playerTurn(int playerRole) throws Exception {
        switch (playerRole) {
            case 1:
                // sending turn message // A
                toP2.writeUTF("\nIt's now player 1 turn"
                        + "\nThe current view of word: " + String.copyValueOf(currentWordView));
                toP1.writeUTF("\nIt's now your turn"
                        + "\nThe current view of word: " + String.copyValueOf(currentWordView)
                        + "\nYour left failed attempts: " + p1Attempts
                        + "\nYour score: " + player1_score
                        + "\nEnter your guess: ");

                // SERVER LOG
                System.out.println("Session " + sessionId + ", round " + roundNum
                        + ": It's now player 1 turn"
                        + "\nThe current view of word: " + String.copyValueOf(currentWordView));

                // player 1 turn // B
                toP1.writeInt(1);
                toP2.writeInt(1);

                // read from player 1, then check .. // C
                guess = fromP1.readUTF();
                checkGuess(1); // D

                endRound = (p1Attempts == 0 && p2Attempts == 0);
                break;

            case 2:
                // sending turn message // A
                toP1.writeUTF("\nIt's now player 2 turn"
                        + "\nThe current view of word: " + String.copyValueOf(currentWordView));
                toP2.writeUTF("\nIt's now your turn"
                        + "\nThe current view of word: " + String.copyValueOf(currentWordView)
                        + "\nYour left failed attempts: " + p2Attempts
                        + "\nYour score: " + player2_score
                        + "\nEnter your guess: ");

                // SERVER LOG
                System.out.println("Session " + sessionId + ", round " + roundNum
                        + ": It's now player 2 turn"
                        + "\nThe current view of word: " + String.copyValueOf(currentWordView));

                // player 2 turn // B
                toP1.writeInt(2);
                toP2.writeInt(2);

                // read from player 2, then check .. // C
                guess = fromP2.readUTF();
                checkGuess(2); // D
                endRound = (p1Attempts == 0 && p2Attempts == 0);
                break;
        }

        toP1.writeBoolean(endRound || winner); // E
        toP2.writeBoolean(endRound || winner); // E

    }

    // evaluate the answer // D
    public void checkGuess(int playerRole) throws IOException {
        switch (playerRole) {
            case 1:
                // guess a word + guess = word
                if (guess.length() > 1 && guess.equals(gameWord)) {
                    player1_score++;
                    winner = true;
                    toP1.writeUTF("\nYou guessed the word correctly, you won the round"
                            + "\nThe word is: " + gameWord);
                    toP2.writeUTF("\nPlayer 1 won the round"
                            + "\nThe word is: " + gameWord);

                    // SERVER LOG
                    System.out.println("Session " + sessionId + ", round " + roundNum
                            + ": Player 1 won the round");

                    // guess a char + word contains the char
                } else if (guess.length() == 1 && gameWord.contains(guess)) {

                    // update the view with this char
                    updateView();

                    // word completed
                    if (String.copyValueOf(currentWordView).equals(gameWord)) {
                        player1_score++;
                        winner = true;
                        toP1.writeUTF("\nYou guessed the word correctly, you won the round"
                                + "\nThe word is: " + String.copyValueOf(currentWordView));
                        toP2.writeUTF("\nPlayer 1 won the round"
                                + "\nThe word is: " + String.copyValueOf(currentWordView));

                        // SERVER LOG
                        System.out.println("Session " + sessionId + ", round " + roundNum
                                + ": Player 1 won the round");
                    } else {
                        // send result
                        toP1.writeUTF("\nYou guessed a correct letter: " + guess
                                + "\nThe current view of word: " + String.copyValueOf(currentWordView));
                        toP2.writeUTF("\nPlayer 1 guessed a correct letter: " + guess
                                + "\nThe current view of word: " + String.copyValueOf(currentWordView));

                        // SERVER LOG
                        System.out.println("Session " + sessionId + ", round " + roundNum
                                + ": Player 1 guessed a correct letter: " + guess
                                + "\nThe current view of word: " + String.copyValueOf(currentWordView));
                    }

                } // wrong guess
                else {
                    p1Attempts--;
                    toP1.writeUTF("Wrong guess!");
                    toP2.writeUTF("\nPlayer 1 failed to guess!");

                    // SERVER LOG
                    System.out.println("Session " + sessionId + ", round " + roundNum
                            + ": Player 1 failed to guess!");
                }
                break;

            case 2:
                // guess a word and guess = word
                if (guess.length() > 1 && guess.equals(gameWord)) {
                    player2_score++;
                    winner = true;
                    toP2.writeUTF("\nYou guessed the word correctly, you won the round"
                            + "\nThe word is: " + String.copyValueOf(currentWordView));
                    toP1.writeUTF("\nPlayer 2 won the round"
                            + "\nThe word is: " + String.copyValueOf(currentWordView));

                    // SERVER LOG
                    System.out.println("Session " + sessionId + ", round " + roundNum
                            + ": Player 2 won the round");

                    // guess a char and word contains the char
                } else if (guess.length() == 1 && gameWord.contains(guess)) {

                    // update the view with this char
                    updateView();

                    // word completed
                    if (String.copyValueOf(currentWordView).equals(gameWord)) {
                        player2_score++;
                        winner = true;
                        toP2.writeUTF("\nYou guessed the word correctly, you won the round"
                                + "\nThe word is: " + String.copyValueOf(currentWordView));
                        toP1.writeUTF("Player 2 won the round"
                                + "\nThe word is: " + String.copyValueOf(currentWordView));

                        // SERVER LOG
                        System.out.println("Session " + sessionId + ", round " + roundNum
                                + ": Player 2 won the round");
                    } else {
                        // send result
                        toP2.writeUTF("\nYou guessed a correct letter: " + guess
                                + "\nThe current view of word: " + String.copyValueOf(currentWordView));
                        toP1.writeUTF("\nPlayer 2 guessed a correct letter: " + guess
                                + "\nThe current view of word: " + String.copyValueOf(currentWordView));

                        // SERVER LOG
                        System.out.println("Session " + sessionId + ", round " + roundNum
                                + ": Player 2 guessed a correct letter: " + guess
                                + "\nThe current view of word: " + String.copyValueOf(currentWordView));
                    }

                } // wrong guess
                else {
                    p2Attempts--;
                    toP2.writeUTF("Wrong guess!");
                    toP1.writeUTF("\nPlayer 2 failed to guess!");

                    // SERVER LOG
                    System.out.println("Session " + sessionId + ", round " + roundNum
                            + ": Player 2 failed to guess!");
                }
                break;

        }
    }

// replace '-' with this char (update word view)
    public void updateView() {
        for (int k = 0; k < gameWord.length(); k++) {
            if (guess.charAt(0) == gameWord.charAt(k)) {
                currentWordView[k] = guess.charAt(0);
            }
        }
    }

}
