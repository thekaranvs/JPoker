import java.rmi.*;
import java.rmi.server.*;
import java.time.Instant;
import java.util.ArrayList;
import java.sql.*;

public class GameServer extends UnicastRemoteObject implements ServerInterface {

    private static final String DB_HOST = "localhost";
    private static final String DB_USER = "c3358";
    private static final String DB_PASS = "c3358PASS";
    private static final String DB_NAME = "JPoker";
    private Connection conn;

    private ArrayList<User> userList;

    public GameServer() throws RemoteException, SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {

        conn = DriverManager.getConnection("jdbc:mysql://" + DB_HOST + "/" + DB_NAME + "?user=" + DB_USER + "&password=" + DB_PASS);
        System.out.println("Connection to database successful!");

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

        try {
            PreparedStatement stmt = conn.prepareStatement("UPDATE OnlineUser SET is_logged_in = 0");
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Login status set to 0 for all existing users!");
            } else {
                System.out.println("No registered users!");
            }
        } catch (SQLException | IllegalArgumentException e) {
            System.out.println("Error updating OnlineUser: " + e);
        }
    }

    public static void main(String[] args) {
        try {
            GameServer server = new GameServer();
            System.setSecurityManager(new SecurityManager());
            Naming.rebind("Server", server);
        } catch (Exception e) {
            System.out.println("Error registering with RMI - terminating application..." + e);
            System.exit(1);
        }
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
}
