import java.awt.*;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class GameClientMainWindow extends JFrame implements ActionListener {
    JButton profileButton = new JButton("User Profile");
    JButton playGameButton = new JButton("Play Game");
    JButton leaderboardButton = new JButton("Leaderboard");
    JButton logoutButton = new JButton("Logout");
    JButton newGameButton = new JButton("New Game");
    JButton nextGameButton = new JButton("Next Game");

    GameClient client;
    JPanel profilePanel = new JPanel();
    JPanel playGameButtonPanel = new JPanel();
    JPanel leaderboardPanel = new JPanel();
    JPanel waitingPanel = new JPanel();
    GameClientPlayWindow playingPanel = new GameClientPlayWindow();
    JPanel nextGamePanel = new JPanel();
    JPanel mainPanel = new JPanel();

    private int gameStatus = 0; // 0: new game, 1: waiting for game, 2: ongoing game, 3: next game

    public class MyWindowListener extends WindowAdapter {
        public void windowClosing(WindowEvent e) {
            super.windowClosing(e);
            if (client.server != null) {
                try {
                    System.out.println("Main frame closed - logging out...");
                    client.server.logout(client.user.getUsername());
                    client.mainWindow.setVisible(false);
                    client.registerFrame.setVisible(false);
                    client.loginFrame.setVisible(true);
                } catch (RemoteException err) {
                    System.err.println("Failed invoking RMI: " + e);
                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource() == leaderboardButton) {
            remove(mainPanel);
            repaint();
            mainPanel = createLeaderboard();
            add(mainPanel, BorderLayout.CENTER);
            invalidate();
            validate();
        } else if (arg0.getSource() == profileButton) {
            remove(mainPanel);
            repaint();
            profilePanel = new JPanel();
            client.retrieveLatestUserInfo();
            createHomePage(client.user);
            mainPanel = profilePanel;
            add(mainPanel, BorderLayout.CENTER);
            invalidate();
            validate();
        } else if (arg0.getSource() == playGameButton) {
            remove(mainPanel);
            repaint();
            switch (gameStatus) {
                case 0:
                    mainPanel = playGameButtonPanel;
                    break;
                case 1:
                    mainPanel = waitingPanel;
                    break;
                case 2:
                    mainPanel = playingPanel;
                    break;
                case 3:
                    mainPanel = nextGamePanel;
                    break;
            }
            add(mainPanel, BorderLayout.CENTER);
            invalidate();
            validate();
        } else if (arg0.getSource() == newGameButton || arg0.getSource() == nextGameButton) {
            gameStatus = 1;
            waitingPanel = new JPanel();
            remove(mainPanel);
            repaint();
            waitingPanel.add(new JLabel("Waiting for players..."));
            mainPanel = waitingPanel;
            add(mainPanel, BorderLayout.CENTER);
            invalidate();
            validate();
            new waitForJoin().execute();
        } else if (arg0.getSource() == logoutButton) {
            if (client.server != null) {
                try {
                    client.server.logout(client.user.getUsername());
                    client.mainWindow.setVisible(false);
                    client.registerFrame.setVisible(false);
                    client.loginFrame.setVisible(true);
                } catch (RemoteException e) {
                    System.err.println("Failed invoking RMI: logout" + e);
                }
            }
        }
    }

    private class waitForJoin extends SwingWorker<Void, Void> {
        @Override
        protected Void doInBackground() throws Exception {
            client.jmsclient.sendJoinMessage(client.user.getUsername());
            System.out.println("Message Sent to join game: " + client.user.getUsername());
            return null;
        }
        @Override
        protected void done() {
            super.done();
        }
    }


    public void showGamePanel() {
        gameStatus = 2;
        remove(mainPanel);
        repaint();
        try {
            playingPanel.startGame(client.jmsclient.playerList, client.jmsclient.gameMsg, client);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        mainPanel = playingPanel;
        add(mainPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void showGameOver() {
        gameStatus = 3;
        remove(mainPanel);
        repaint();
        nextGamePanel = new JPanel();
        repaint();
        JLabel j1 = new JLabel("Winner: " + client.jmsclient.gameOverMsg.getWinner() + "\n");
        j1.setFont(new Font("Serif", Font.PLAIN, 25));
        JLabel j2 = new JLabel(client.jmsclient.gameOverMsg.getAnswer() + "\n");
        j2.setFont(new Font("Serif", Font.BOLD, 40));
        nextGamePanel.setLayout(new BoxLayout(nextGamePanel, BoxLayout.PAGE_AXIS));
        nextGamePanel.add(j1, BorderLayout.CENTER);
        nextGamePanel.add(j2, BorderLayout.CENTER);
        nextGameButton.setPreferredSize(new Dimension(100, 40));
        nextGameButton.addActionListener(this);
        nextGamePanel.add(nextGameButton, BorderLayout.SOUTH);
        mainPanel = nextGamePanel;
        add(mainPanel, BorderLayout.CENTER);
        invalidate();
        validate();
    }

    public GameClientMainWindow(GameClient client, User player) {
        this.client = client;
        setTitle("JPoker 24-Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Build main menu (button panel)
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 4));
        buttonPanel.add(profileButton);
        buttonPanel.add(playGameButton);
        buttonPanel.add(leaderboardButton);
        buttonPanel.add(logoutButton);

        // Build home page (user profile)
        createHomePage(player);

        // Build leaderboard
        JPanel leaderboardPanel1 = createLeaderboard();
        leaderboardPanel.add(leaderboardPanel1);

        // Play Game Panel
        playGameButtonPanel.add(newGameButton);

        // Set main panel to home page
        mainPanel = profilePanel;

        getContentPane().add(BorderLayout.CENTER, mainPanel);
        leaderboardButton.addActionListener(this);
        profileButton.addActionListener(this);
        playGameButton.addActionListener(this);
        logoutButton.addActionListener(this);
        newGameButton.addActionListener(this);

        getContentPane().add(BorderLayout.NORTH, buttonPanel);

        pack();
        setLocationRelativeTo(null);
        setSize(600, 450);
        setVisible(true);
        addWindowListener(new MyWindowListener());
    }

    private void createHomePage(User player) {
        JLabel[] texts = new JLabel[5];
        texts[0] = new JLabel(player.getUsername() + "\n");
        texts[1] = new JLabel("Number of Wins: " + player.getNumWin() + "\n");
        texts[2] = new JLabel("Number of games: " + player.getTotalGames() + "\n");
        texts[4] = new JLabel("Rank: #" + player.getRank() + "\n");
        texts[3] = new JLabel("Average time to win: " + player.getAvgTime() + "s\n");

        for (int i = 0; i < 5; i++) {
            texts[i].setAlignmentX(LEFT_ALIGNMENT);
            texts[i].setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 20));
            if (i == 0) {
                texts[i].setFont(new Font("Serif", Font.BOLD, 30));
            } else if (i == 4) {
                texts[i].setFont(new Font("Serif", Font.PLAIN, 25));
            } else {
                texts[i].setFont(new Font("Serif", Font.PLAIN, 20));
            }
            profilePanel.setLayout(new BoxLayout(profilePanel, BoxLayout.Y_AXIS));
            profilePanel.add(texts[i]);
        }
    }

    private JPanel createLeaderboard() {
        JPanel leaderboardPanel = new JPanel();
        leaderboardPanel.setLayout(new GridLayout(1, 0));

        String[] columnNames = {"Rank", "Player", "Games Won", "Games Played", "Average Time"};
        Object[][] data = { // Dummy data
                {1,     "PlayerZero",   18, 25, "16.9s"},
                {2,     "Player 4",     15, 17, "12.5s"},
                {3,     "DudeMan",      13, 22, "12.5s"},
                {4,     "JackSparrow",  12, 19, "12.5s"},
                {5,     "OriginalBoi",  10, 25, "12.5s"},
                {6,     "JackOff",       8, 15, "12.5s"},
                {7,     "Johnson",       6, 9, "12.5s"},
                {8,     "Black",         4, 10, "12.5s"},
                {9,     "White",         3, 9, "12.5s"},
                {10,    "Beeswax",       0, 1, "12.5s"}
        };

        try { // Fetch real data
            data = client.server.getTopPlayers();
        } catch (RemoteException e) {
            System.out.println("Error invoking RMI: getTopPlayers() " + e);
        }

        // Define new table model to make cells non-editable
        DefaultTableModel tableModel = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable leaderboardTable = new JTable(data, columnNames);
        leaderboardTable.setModel(tableModel);
        leaderboardTable.setPreferredScrollableViewportSize(new Dimension(150, 300));
        leaderboardTable.setFillsViewportHeight(true);
        leaderboardTable.setRowHeight(30);
        leaderboardPanel.add(leaderboardTable);

        JScrollPane scrollPane = new JScrollPane(leaderboardTable);
        leaderboardPanel.add(scrollPane);

        return leaderboardPanel;
    }

}