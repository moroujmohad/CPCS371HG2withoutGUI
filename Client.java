
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.Scanner;

// Client-side application
public class Client {

    // IO communication with server
    private static DataInputStream fromServer;
    private static DataOutputStream toServer;

    // socket
    private static Socket serverSocket;

    // player role 1 or 2
    private static int playerRole;

    // for players guesses
    private static String guess;

    // to read input from player
    static Scanner input = new Scanner(System.in);

    // Client-side application
    public static void main(String args[]) throws Exception {

        try {
            System.out.println("Welcome to Guessing Game (Hangman)");
            System.out.println("General instruction:\n"
                    + "1- You have only 5 failed attempts to guess the word correctly\n"
                    + "2- You can guess a character, or the whole word\n");

            // ask the client if he want to join a game
            System.out.print("Enter 'start' to join the game: ");
            if (input.next().equalsIgnoreCase("start")) {
                // establish connectoion with server
                connectionEstablshment();

                // get a player number from the server
                playerRole = fromServer.readInt();
                //System.out.println(playerRole);

                // start the session
                start(playerRole);

                while (true) {

                    // reading turn message // A
                    System.out.print(fromServer.readUTF());

                    // reading whos player turn? // B
                    int turn = fromServer.readInt();
                    playerTurn(turn); // C

                    // read evaluation results // D
                    System.out.println(fromServer.readUTF());

                    // end the round? // E
                    if (fromServer.readBoolean()) {
                        // read current results + question // F
                        System.out.println(fromServer.readUTF());
                        input = new Scanner(System.in);
                        String replay = input.next().trim();
                        // send response // G
                        toServer.writeBoolean(replay.equalsIgnoreCase("y"));
                        // end the game // H
                        if (fromServer.readBoolean()) {
                            // read final results + bye // I
                            System.out.println(fromServer.readUTF());
                            break;
                        }

                    }
                    System.out.println();
                }

            }

        } catch (Exception e) {
            System.out.println("ERROR AT CLIENT-SIDE APPLICATION");
            System.out.println(e.getMessage());

        }
    }

    // connect the player to the game server
    public static void connectionEstablshment() throws IOException {

        try {
            serverSocket = new Socket("localhost", 4000);
            fromServer = new DataInputStream(serverSocket.getInputStream());
            toServer = new DataOutputStream(serverSocket.getOutputStream());
            System.out.println(new Date() + " Connected to server");
        } catch (Exception e) {

            System.out.println("SERVER IS NOT CONNECTED");
            System.out.println(e);
            System.exit(0);
        }
    }

    // inform each player with its role, to start the game
    public static void start(int playerRole) throws Exception {
        // first player joined the game
        if (playerRole == 1) {
            // inform the player 1 that he is the player 1!
            System.out.println("You are player 1!");
            System.out.println("Waiting for player 2 to join!");

            // waiting a signal from the server to start the game
            // (when second player joind the game)
            fromServer.readInt();

            // inform player 1 that the player 2 has joined the game
            // player 1 can start guessing
            System.out.println("Player 2 has joined, you start first");

        } //  second player joined the game
        else if (playerRole == 2) {
            // inform the player 2 that he is the player 2!
            System.out.println("You are player 2!");
            System.out.println("Waiting for player 1 to start the game!");

        }
    }

    // interaction between client and server 
    public static void playerTurn(int playerturn) throws Exception {

        if (playerRole == playerturn) {
            input = new Scanner(System.in);
            guess = input.next().trim().toLowerCase();
            toServer.writeUTF(guess);
        }

    }

}
