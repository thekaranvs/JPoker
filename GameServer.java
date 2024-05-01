import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.ArrayList;
import java.util.HashMap;

public class GameServer extends UnicastRemoteObject implements ServerInterface {

    private HashMap<String, String> userInfoMap;
    private ArrayList<String> onlineUserList;

    public GameServer() throws RemoteException {
        userInfoMap = new HashMap<String, String>();
        onlineUserList = new ArrayList<String>();

        try {
            File userInfoFile = new File("UserInfo.txt");
            if (userInfoFile.createNewFile()) {
                System.out.println("Creating UserInfo.txt...");
            } else {
                System.out.println("Found existing UserInfo.txt - reading data (if any)...");
                BufferedReader reader = new BufferedReader(new FileReader(userInfoFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] userDetails = line.split(",");
                    userInfoMap.put(userDetails[0], userDetails[1]);
                }
                reader.close();
            }

            File onlineUserFile = new File("OnlineUser.txt");
            if (onlineUserFile.createNewFile()) {
                System.out.println("Creating OnlineUser.txt...");
            }
            FileWriter fw = new FileWriter(onlineUserFile);
            fw.write("");
            fw.flush();
            fw.close();
            System.out.println("Ready to process requests!");
        } catch (IOException e) {
            System.out.println("Error while creating/reading files");
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        try {
            GameServer server = new GameServer();
            System.setSecurityManager(new SecurityManager());
            Naming.rebind("Server", server);
        } catch (Exception e) {
            System.out.println("Error registering with RMI");
        }
    }

    private synchronized void addUserToUserInfoMap(String username, String password) { userInfoMap.put(username, password); }

    private synchronized void addUserToOnlineUserList(String username) { onlineUserList.add(username); }

    private synchronized void removeUserFromOnlineUserList(String username) { onlineUserList.remove(username); }

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

        if (userInfoMap.containsKey(username)) {
            status = 1;
            System.out.println(username + " taken.");
        } else {
            status = 4;
            addUserToUserInfoMap(username, password);
            writeToFile("UserInfo.txt", username, password, false);
            System.out.println(username + " successfully registered!");
            login(username, password);
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

        if (!userInfoMap.containsKey(username)) {
            status = 1;
            System.out.println(username + " not found.");
        } else if (!password.equals(userInfoMap.get(username))) {
            status = 2;
            System.out.println("Password did not match.");
        } else if (onlineUserList.contains(username)) {
            status = 3;
            System.out.println(username + " already logged in and online.");
        } else {
            status = 4;
            addUserToOnlineUserList(username);
            writeToFile("OnlineUser.txt", username, null, false);
            System.out.println(username + " successfully logged in!");
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
        removeUserFromOnlineUserList(username);
        // Clear online user text file
        writeToFile("OnlineUser.txt", null, null, true);

        // Rewrite online users to file
        for (String user : onlineUserList) {
            writeToFile("OnlineUser.txt", user, null, false);
        }
        System.out.println(username + " successfully logged out!");
    }

    /**
     * Outputs username and/or password to specified file
     * @param filename The name of the file
     * @param username The username
     * @param password The password
     * @param overwrite Boolean whether to overwrite file or append
     */
    public synchronized void writeToFile(String filename, String username, String password, boolean overwrite) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename, !overwrite));
            if (username == null) {
                bw.write("");
            } else {
                String message = username + "," + password;
                if (password == null) {
                    message = username;
                }
                bw.write(message);
                bw.newLine();
            }
            bw.flush();
            bw.close();
        } catch (IOException e) {
            System.out.println("Error while writing to file!");
            e.printStackTrace();
        }
    }
}
