import java.io.Serializable;
import java.util.ArrayList;

public class GameMessage implements Serializable {
    private String command;  // specify what the message is used for
    private String to;
    private String username;
    private String password;

    private User user;

    private ArrayList<User> playerList;
    private ArrayList<String> gameCards;
    private String answer;          // the answer submitted by the user
    private long timeUsed;          // time used to come up with this answer

    private double result;          // what's the result of the expression submitted by the player
    private String winner;          // the winner's user_name if it is the case
    private long endTime;           // the end time for the winner


    public GameMessage() {
        command = null;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String id) {
        command = id;
    }

    public String getUserName() {
        return username;
    }

    public void setUserName(String name) {
        username = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public User getGamePlayer() {
        return user;
    }

    public void setUser(User newPlayer) {
        this.user = newPlayer;
    }

    public ArrayList<User> getPlayerList() {
        return playerList;
    }

    public void setPlayerList(ArrayList<User> playerList) {
        this.playerList = playerList;
    }

    public ArrayList<String> getGameCards() {
        return gameCards;
    }

    public void setGameCards(ArrayList<String> gameCards) {
        this.gameCards = gameCards;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public long getTimeUsed() {
        return timeUsed;
    }

    public void setTimeUsed(long timeUsed) {
        this.timeUsed = timeUsed;
    }

    public double getResult() {
        return this.result;
    }

    public void setResult(double trueResult) {
        this.result = trueResult;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String toString() {
        String stringForm;
        if (username == null)
            stringForm = "Command: " + command;
        else
            stringForm = "Command: " + command + "from Username: " + username;
        return stringForm;
    }
}