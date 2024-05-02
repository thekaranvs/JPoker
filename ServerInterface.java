import java.rmi.*;

public interface ServerInterface extends Remote {

    int register(String username, String password) throws RemoteException;
    int login(String username, String password) throws RemoteException;
    void logout(String username) throws RemoteException;
    User getUserDetails(String username) throws RemoteException;

    Object[][] getTopPlayers() throws RemoteException;
}