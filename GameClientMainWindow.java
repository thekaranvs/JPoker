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
import java.sql.SQLException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class GameClientMainWindow extends JFrame implements ActionListener {
    JButton profile = new JButton("User Profile");
    JButton play = new JButton("Play Game");
    JButton leaderboard = new JButton("Leader Board");
    JButton logout = new JButton("Logout");
    JButton btnNewgame = new JButton("New Game");
    JButton btnRestartGame = new JButton("Restart Game");

    GameClient client;
    JPanel pnlWelcome = new JPanel();
    JPanel pnlStartBtn = new JPanel();
    JPanel pnlLB = new JPanel();
    JPanel pnlWaiting = new JPanel();
    GameClientPlayWindow pnlPlaying = new GameClientPlayWindow();
    JPanel pnlRestart = new JPanel();
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
        if (arg0.getSource() == leaderboard) {
            remove(mainPanel);
            repaint();
            mainPanel = createLeaderboard();
            add(mainPanel, BorderLayout.CENTER);
            invalidate();
            validate();
        } else if (arg0.getSource() == profile) {
            remove(mainPanel);
            repaint();
            pnlWelcome = new JPanel();
            client.retrieveLatestUserInfo();
            createHomePage(client.user);
            mainPanel = pnlWelcome;
            add(mainPanel, BorderLayout.CENTER);
            invalidate();
            validate();
        } else if (arg0.getSource() == play) {
            remove(mainPanel);
            repaint();
            switch (gameStatus) {
                case 0:
                    mainPanel = pnlStartBtn;
                    break;
                case 1:
                    mainPanel = pnlWaiting;
                    break;
                case 2:
                    mainPanel = pnlPlaying;
                    break;
                case 3:
                    mainPanel = pnlRestart;
                    break;
            }
            add(mainPanel, BorderLayout.CENTER);
            invalidate();
            validate();
        } else if (arg0.getSource() == btnNewgame || arg0.getSource() == btnRestartGame) {
            gameStatus = 1;
            pnlWaiting = new JPanel();
            remove(mainPanel);
            repaint();
            pnlWaiting.add(new JLabel("Waiting for players..."));
            mainPanel = pnlWaiting;
            add(mainPanel, BorderLayout.CENTER);
            invalidate();
            validate();
            new waitForJoin().execute();
        } else if (arg0.getSource() == logout) {
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

    /**
     * Called by onMessage. init4.
     */
    public void playinggame() {
        gameStatus = 2;
        remove(mainPanel);
        repaint();
        try {
            pnlPlaying.startGame(client.jmsclient.playerList,
                    client.jmsclient.gameMsg, client);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        mainPanel = pnlPlaying;
        add(mainPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    /**
     * Called by onMessage.
     */
    public void gameoverboard() {
        gameStatus = 3;
        remove(mainPanel);
        repaint();
        pnlRestart = new JPanel();
        repaint();
        JLabel j1 = new JLabel("Winner: " + client.jmsclient.gameOverMsg.getWinner() + "\n");
        j1.setFont(new Font("Serif", Font.PLAIN, 25));
        JLabel j2 = new JLabel(client.jmsclient.gameOverMsg.getAnswer() + "\n");
        j2.setFont(new Font("Serif", Font.BOLD, 40));
        pnlRestart.setLayout(new BoxLayout(pnlRestart, BoxLayout.PAGE_AXIS));
        pnlRestart.add(j1, BorderLayout.CENTER);
        pnlRestart.add(j2, BorderLayout.CENTER);
        btnRestartGame.setPreferredSize(new Dimension(100, 40));
        btnRestartGame.addActionListener(this);
        pnlRestart.add(btnRestartGame, BorderLayout.SOUTH);
        mainPanel = pnlRestart;
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
        buttonPanel.add(profile);
        buttonPanel.add(play);
        buttonPanel.add(leaderboard);
        buttonPanel.add(logout);

        // Build home page (user profile)
        createHomePage(player);

        // Build leaderboard
        JPanel leaderboardPanel = createLeaderboard();
        pnlLB.add(leaderboardPanel);

        // Play Game Panel
        pnlStartBtn.add(btnNewgame);

        // Set main panel to home page
        mainPanel = pnlWelcome;

        getContentPane().add(BorderLayout.CENTER, mainPanel);
        leaderboard.addActionListener(this);
        profile.addActionListener(this);
        play.addActionListener(this);
        logout.addActionListener(this);
        btnNewgame.addActionListener(this);

        getContentPane().add(BorderLayout.NORTH, buttonPanel);

        pack();
        setLocationRelativeTo(null);
        setSize(800, 600);
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
            pnlWelcome.setLayout(new BoxLayout(pnlWelcome, BoxLayout.Y_AXIS));
            pnlWelcome.add(texts[i]);
        }
    }

    private JPanel createLeaderboard() {
        JPanel leaderboardPanel = new JPanel();
        leaderboardPanel.setLayout(new GridLayout(1, 0));

        String[] columnNames = {"Rank", "Player", "Games Won", "Games Played", "Average Time"};
        Object[][] data = {
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

        try {
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