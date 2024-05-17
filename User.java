import java.io.Serializable;

public class User implements Serializable {

    private final String username;
    private int numWin;
    private int totalGames;
    private double avgTime;
    private int rank;

    public User(String username) {
        this.username = username;
        numWin = 0;
        totalGames = 0;
        avgTime = 0;
        rank = 0;
    }

    public User(String username, int numWin, int totalGames, double avgTime) {
        this.username = username;
        this.numWin = numWin;
        this.totalGames = totalGames;
        this.avgTime = avgTime;
        this.rank = 0;
    }

    public String getUsername() {
        return username;
    }
    public int getNumWin() {
        return numWin;
    }
    public void setNumWin(int numWin) {
        this.numWin = numWin;
    }
    public int getTotalGames() {
        return totalGames;
    }
    public void setTotalGames(int totalGames) {
        this.totalGames = totalGames;
    }
    public double getAvgTime() {
        return avgTime;
    }
    public void setAvgTime(double avgTime) {
        this.avgTime = avgTime;
    }
    public void setRank(int rank) { this.rank = rank; }
    public int getRank() { return this.rank; }
}
