import javax.naming.NamingException;
import java.io.Serializable;
import java.rmi.*;
import java.rmi.server.*;
import java.time.Instant;
import java.util.ArrayList;
import java.sql.*;
import java.util.Arrays;
import java.util.Random;
import java.util.Stack;
import javax.jms.MessageProducer;
import javax.jms.MessageConsumer;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.naming.*;

public class GameServer extends UnicastRemoteObject implements ServerInterface {

    private static final String DB_HOST = "localhost";
    private static final String DB_USER = "c3358";
    private static final String DB_PASS = "c3358PASS";
    private static final String DB_NAME = "JPoker";

    private Connection conn;
    private JMS jms;
    private MessageProducer topicPublisher;
    private MessageConsumer queueReceiver;

    private ArrayList<User> currentPlayers;
    private String gameStatus;
    private Thread gameThread;


    public GameServer() throws RemoteException, SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, NamingException {
        try {
            jms = new JMS();
        } catch (Exception e) {
            System.out.println("GameServer: error while creating jms obj: " + e);
        }

        // Set up JDBC connection and initialise tables
        conn = DriverManager.getConnection("jdbc:mysql://" + DB_HOST + "/" + DB_NAME + "?user=" + DB_USER + "&password=" + DB_PASS);
        System.out.println("Connection to database successful!");

        // Create tables in DB (if they don't exist already)
        try {
            String createUserInfoTableSQL = "CREATE TABLE IF NOT EXISTS UserInfo (" +
                    "    username varchar(20) NOT NULL," +
                    "    password varchar(20) NOT NULL," +
                    "    games_won INT(5)," +
                    "    games_played INT(5)," +
                    "    win_time INT(5));";
            Statement createTableStmt = conn.createStatement();
            createTableStmt.executeUpdate(createUserInfoTableSQL);

            String createOnlineUserTableSQL = "CREATE TABLE IF NOT EXISTS OnlineUser (" +
                    "    username varchar(20) NOT NULL," +
                    "    last_logged_in TIMESTAMP," +
                    "    is_logged_in TINYINT);";
            createTableStmt.executeUpdate(createOnlineUserTableSQL);
        } catch (Exception e) {
            System.out.println("Error creating tables: " + e);
        }

        // Set all users logged in status to 0 i.e. offline
        try {
            PreparedStatement stmt = conn.prepareStatement("UPDATE OnlineUser SET is_logged_in = 0");
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Login status set to 0 (offline) for all existing users!");
            } else {
                System.out.println("No registered users!");
            }
        } catch (SQLException | IllegalArgumentException e) {
            System.out.println("Error updating OnlineUser: " + e);
        }

        currentPlayers = new ArrayList<User>();
        gameStatus = "WAIT";
    }

    public static void main(String[] args) {
        try {
            GameServer server = new GameServer();
            System.setSecurityManager(new SecurityManager());
            Naming.rebind("Server", server);
            server.run();
        } catch (Exception e) {
            System.out.println("Error registering with JDBC or RMI - terminating application... " + e);
            System.exit(1);
        }
    }

    public void run() throws JMSException {
        queueReceiver = jms.createQueueReceiver();
        topicPublisher = jms.createTopicPublisher();
        System.out.println("Ready to receive commands from clients!");

        while (true) {
            GameMessage clientMsg = (GameMessage)((ObjectMessage)receiveMessage()).getObject();
            System.out.println("\nReceived message from client: " + clientMsg.toString() + "\n");

            if (gameStatus.equals("WAIT")) {
                gameThread = new Thread(new HandleClientMsg(clientMsg));
                gameThread.start();
            } else {
                new Thread(new HandleClientMsg(clientMsg)).start();
            }
        }
    }

    private Message receiveMessage() throws JMSException {
        try {
            if (queueReceiver == null) queueReceiver = jms.createQueueReceiver();
            return queueReceiver.receive();
        } catch (JMSException e) {
            System.out.println("Failed to receive message from JMS: " + e);
            throw e;
        }
    }

    private void broadcastMessage(Message jmsMessage) throws JMSException {
        try {
            if (topicPublisher == null) topicPublisher = jms.createTopicPublisher();
            topicPublisher.send(jmsMessage);
        } catch (JMSException e) {
            System.out.println("Failed to send message to topic: " + e);
            throw e;
        }
    }

    private Message convertToMessage(Serializable obj) throws JMSException {
        return jms.createMessage(obj);
    }

    /**
     * Function to register user and add details to UserInfo.txt
     * @param username The login name / username
     * @param password The password
     * @return Integer status code (4: Success, 1: Error - User already exists)
     * @throws RemoteException RMI function throws
     */
    public synchronized int register(String username, String password) throws RemoteException {
        System.out.println(username + " attempting to register...");
        int status = 0;

        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT username FROM UserInfo WHERE username = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                status = 1;
                System.out.println(username + " already taken.");
            } else {
                status = 4;
                stmt = conn.prepareStatement("INSERT INTO UserInfo (username, password, games_won, games_played, win_time) VALUES (?, ?, ?, ?, ?)");
                stmt.setString(1, username);
                stmt.setString(2, password);
                stmt.setInt(3, 0);
                stmt.setInt(4, 0);
                stmt.setInt(5, 0);
                stmt.execute();

                System.out.println("Added user record to UserInfo table!");
                System.out.println(username + " successfully registered!");

                stmt = conn.prepareStatement("INSERT INTO OnlineUser (username, last_logged_in, is_logged_in) VALUES (?, ?, ?)");
                stmt.setString(1, username);
                stmt.setTimestamp(2, Timestamp.from(Instant.now()));
                stmt.setInt(3, 1);
                stmt.execute();
            }

        } catch (SQLException | IllegalArgumentException e) {
            System.out.println("Error accessing record in UserInfo: " + e);
        }

        return status;
    }

    /**
     * Function to log in
     * @param username The login name / username
     * @param password The password
     * @return Integer status code (4: Success, 1: Error - User doesn't exist, 2: Error - Wrong password, 3: Error - User online)
     * @throws RemoteException RMI function throws
     */
    public synchronized int login(String username, String password) throws RemoteException {
        System.out.println(username + " attempting to login...");
        int status = 0;

        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT password FROM UserInfo WHERE username = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                status = 1;
                System.out.println(username + " not found.");
            } else {
                String correctPassword = rs.getString(1);

                // Check if user is already logged in
                stmt = conn.prepareStatement("SELECT is_logged_in FROM OnlineUser WHERE username = ?");
                stmt.setString(1, username);
                rs = stmt.executeQuery();

                boolean isLoggedIn = false;
                if (rs.next()) {
                    isLoggedIn = rs.getInt(1) == 1;
                } else {
                    System.out.println("ERROR - user did not exist in OnlineUser table!");
                }

                if (!correctPassword.equals(password)) {
                    status = 2;
                    System.out.println("Password did not match.");
                } else if (isLoggedIn) {
                    status = 3;
                    System.out.println(username + " already logged in and online.");
                } else {
                    status = 4;

                    // Update OnlineUser table
                    stmt = conn.prepareStatement("UPDATE OnlineUser SET is_logged_in = ?, last_logged_in = ? WHERE username = ?");
                    stmt.setInt(1, 1);
                    stmt.setTimestamp(2, Timestamp.from(Instant.now()));
                    stmt.setString(3, username);
                    int rows = stmt.executeUpdate();

                    if (rows > 0) {
                        System.out.println(username + " successfully logged in!");
                    } else {
                        System.out.println("Error while updating OnlineUser table - proceeding anyway");
                    }
                }
            }
        } catch (SQLException | IllegalArgumentException e) {
            System.out.println("Error accessing tables: " + e);
        }

        return status;
    }

    /**
     * Logs user out and removes from OnlineUser.txt
     * @param username Username of logged-in user
     * @throws RemoteException RMI function throws
     */
    public synchronized void logout(String username) throws RemoteException {
        System.out.println(username + " logging out...");

        try {
            PreparedStatement stmt = conn.prepareStatement("UPDATE OnlineUser SET is_logged_in = 0 WHERE username = ?");
            stmt.setString(1, username);
            int rows = stmt.executeUpdate();

            if (rows > 0) {
                System.out.println(username + " successfully logged out!");
            } else {
                System.out.println("Error while logging user out");
            }

        } catch (SQLException | IllegalArgumentException e) {
            System.out.println("Error accessing record in OnlineUser: " + e);
        }
    }

    public synchronized User getUserDetails(String username) throws RemoteException {
        User user = null;
        
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT username, games_won, games_played, win_time FROM UserInfo WHERE userName = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            String[] details = new String[4];
            if (rs.next()) {
                System.out.println("Retrieved data of " + username);
                user = new User(rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getDouble(4));
            }
        } catch (SQLException | IllegalArgumentException e) {
            System.out.println("Error accessing record in UserInfo: " + e);
        }

        return user;
    }

    public synchronized Object[][] getTopPlayers() throws RemoteException {
        Object[][] leaderboardData = new Object[10][5];
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM UserInfo ORDER BY games_won DESC LIMIT 10");
            ResultSet rs = stmt.executeQuery();

            int rank = 1;
            while (rs.next()) {
                Object[] leaderboardEntry = {rank, rs.getString("username"), rs.getString("games_won"), rs.getString("games_played"), rs.getString("win_time")};
                leaderboardData[rank-1] = leaderboardEntry;
                rank++;
            }
        } catch (SQLException | IllegalArgumentException e) {
            System.out.println("Error accessing records in UserInfo: " + e);
        }

        return leaderboardData;
    }

    private GameMessage createNewGameMessage(ArrayList<User> playerList) {
        GameMessage msg = new GameMessage();

        msg.setCommand("START");
        msg.setPlayerList(playerList);

        int [] num = new int[4];
        ArrayList<String> cards = new ArrayList<String>(4);

        char[] suit = {'a','b','c','d'}; // the 4 suits prefix (cards stored as a4.png for e.g.)
        for(int i = 0; i < 4; i++) {
            Random rand = new Random();
            int m = rand.nextInt(4);
            int n = rand.nextInt(13) + 1;

            while(i == 1 && n == num[0]) {
                n = rand.nextInt(13) + 1;
            }
            while(i == 2 && (n == num[0] || n == num[1])){
                n = rand.nextInt(13) + 1;
            }
            while(i == 3 && (n == num[0] || n == num[1] || n == num[2])) {
                n = rand.nextInt(13) + 1;
            }

            num[i] = n;
            cards.add(i,suit[m]+Integer.toString(n)+".png");
        }
        msg.setGameCards(cards); // Test if solvable?

        return msg;
    }

    private void startGame(ArrayList<User> playerList) throws JMSException {
        GameMessage newGameMsg = createNewGameMessage(playerList);
        broadcastMessage(convertToMessage(newGameMsg));
        System.out.println("New game message broadcasted!");
    }

    class HandleClientMsg implements Runnable {
        private User playerInfo;

        public HandleClientMsg(GameMessage clientMsg){
            this.playerInfo = clientMsg.getGamePlayer();
        }
        public void run() {
            try {
                currentPlayers.add(playerInfo);

                if (gameStatus.equals("WAIT")) {
                    System.out.println("First player has joined new game: " + playerInfo.getUsername());
                    gameStatus = "ONE_WAITING";

                    try { Thread.sleep(10000);}
                    catch (InterruptedException e) { return; }

                    if (currentPlayers.size() >= 2){
                        System.out.println("10 seconds passed since player 1 joined. Starting game as >1 player joined");
                        gameStatus = "WAIT";
                        startGame(currentPlayers);
                        currentPlayers.clear();
                    } else {
                        System.out.println("10 seconds have elapsed since player 1 joined - will start game as soon as P2 joins");
                        gameStatus = "NOT_ENOUGH";
                    }
                }
                else if (gameStatus.equals("ONE_WAITING")) {
                    gameStatus = "TWO_WAITING";
                    System.out.println("Second player has joined game: " + playerInfo.getUsername());
                    System.out.println("Waiting for more players until 10s timeout...");
                }
                else if (gameStatus.equals("TWO_WAITING")) {
                    System.out.println("Third player has joined game: " + playerInfo.getUsername());
                    gameStatus = "THREE_WAITING";
                }
                else if (gameStatus.equals("THREE_WAITING")) {
                    System.out.println("Fourth player has joined game: " + playerInfo.getUsername());
                    System.out.println("Starting game...");
                    gameStatus = "WAIT";
                    gameThread.interrupt();
                    startGame(currentPlayers);
                    currentPlayers.clear();
                }
                else if (gameStatus.equals("NOT_ENOUGH")) {
                    System.out.println("Second player has joined game: " + playerInfo.getUsername());
                    gameStatus = "WAIT";
                    startGame(currentPlayers);
                    currentPlayers.clear();
                }
            } catch (Exception e){
                System.out.println("HandleClientMsg error: "+e);
                e.printStackTrace();
            }
        }
    }


    // return true if op1 has higher or the same priority than op2
    public static boolean isPrior(String op1, String op2) {
        if (op1.charAt(0) == '*' || op1.charAt(0) == '/') {
            return true;
        } else if (op1.charAt(0) == '+' || op1.charAt(0) == '-') {
            if (op2.equals("*"))
                return false;
            else
                return true;
        } else if (op1.charAt(0) == '(')
            return false;
        else
            return false;
    }

    public double evaluate(String answer) {
        // parse the expression
        ArrayList<String> validSigns = new ArrayList<String>(Arrays.asList("+","-","*","/"));
        ArrayList<String> validValues = new ArrayList<String>(Arrays.asList("A","2","3","4","5","6","7","8","9","10","J","Q","K"));
        ArrayList<String> validNumbers = new ArrayList<String>(Arrays.asList("1","2","3","4","5","6","7","8","9","10","11","12","13"));

        // convert from infix to postfix
        String[] tokens = new String[answer.length()];  // infix
        int index = 0;
        int length = 0;
        for (int i = 0; i < answer.length(); i++) {
            if (answer.charAt(i) == '1') {         // Check whether it is "10"
                if (i == answer.length()-1)
                    return -1;
                else if (answer.charAt(i+1) == '0') {
                    tokens[index] = "" + answer.charAt(i) + answer.charAt(i+1);
                    index++;
                    length++;
                    i++;
                } else
                    return -1;
            }
            if (validSigns.contains(answer.charAt(i)+"") || validValues.contains(answer.charAt(i)+"")) {
                tokens[index] = "" + answer.charAt(i);
                index++;
                length++;
            } else if (answer.charAt(i) == '(' || answer.charAt(i) == ')') {
                tokens[index] = "" + answer.charAt(i);
                index++;
            }
        }

        String[] outputs = new String[length]; // post-fix
        Stack<String> opstack = new Stack<String>();
        length = index;
        index = 0;
        for (int i = 0; i< length; i++) {
            if (validValues.contains(tokens[i])) {
                if (answer.charAt(i) == 'A')
                    outputs[index] = "1";
                else if (answer.charAt(i) == 'J')
                    outputs[index] = "11";
                else if (answer.charAt(i) == 'Q')
                    outputs[index] = "12";
                else if (answer.charAt(i) == 'K')
                    outputs[index] = "13";
                else
                    outputs[index] = tokens[i];
                index++;
            } else if (tokens[i].charAt(0) == '(') {
                opstack.push(tokens[i]);
            } else if (tokens[i].charAt(0) == ')') {
                String top = opstack.pop();
                while (top.charAt(0) != '(') {
                    outputs[index] = top;
                    index++;
                    top = opstack.pop();
                }
            } else if (validSigns.contains(tokens[i])) {
                Stack<String> tmp = new Stack<String>();
                while (!opstack.isEmpty()) {
                    if (opstack.peek().equals("(")) {
                        tmp.push(opstack.pop());
                        System.out.println(tmp.peek() + " is popped from the op_stack and pushed to the tmp_stack!");
                        break;
                    }
                    if (isPrior(opstack.peek(), tokens[i])) {
                        outputs[index] = opstack.pop();
                        index++;
                    } else {
                        tmp.push(opstack.pop());
                    }
                }
                while (!tmp.isEmpty()) {
                    opstack.push(tmp.pop());
                }
                opstack.push(tokens[i]);
            } else {
                return -1; // invalid sign used
            }
        }
        while(!opstack.isEmpty()) {
            outputs[index] = opstack.pop();
            index++;
        }

        // check the validity of numbers
//        for (String token: outputs) {
//            if (validNumbers.contains(token) && !gameNumbers.contains(token))
//                return -1; // invalid number used
//        }

        // evaluation using post-fix order
        Stack<Double> stack = new Stack<Double>();
        for(String token: outputs) {
            if(token.equals("+")) {
                stack.push(stack.pop()+stack.pop());
            } else if(token.equals("-")) {
                Double p1 = stack.pop();
                Double p2 = stack.pop();
                stack.push(p2-p1);
            } else if(token.equals("*")) {
                stack.push(stack.pop()*stack.pop());
            } else if(token.equals("/")) {
                Double p1 = stack.pop();
                Double p2 = stack.pop();
                stack.push(p2/p1);
            } else {
                stack.push((double) Integer.parseInt(token));
            }
        }
        return stack.pop();
    }
}
