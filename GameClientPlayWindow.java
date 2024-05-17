import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;

public class GameClientPlayWindow extends JPanel {
    ArrayList<User> playerList;
    GameMessage game;
    GameClient client;
    JPanel bottomPanel;
    JPanel rightPanel;
    JPanel topPanel;

    public GameClientPlayWindow() {
        bottomPanel = new JPanel();
        rightPanel = new JPanel();
        topPanel = new JPanel();
    }

    void startGame(ArrayList<User> playerList, GameMessage game, GameClient client) throws URISyntaxException {
        this.client = client;
        this.game = game;
        this.playerList = playerList;
        bottomPanel.removeAll();
        rightPanel.removeAll();
        topPanel.removeAll();

        // Display cards in top panel
        topPanel.setPreferredSize(new Dimension(350, 150));
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.LINE_AXIS));
        System.out.println("PlayWindow : Displaying cards");
        for (int i = 0; i < 4; i++) {
            JPanel p = new JPanel();
            p.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

            String cardURL = "cards/" + game.getGameCards().get(i);
            ImageIcon card = new ImageIcon(cardURL);

            Image img = card.getImage();
            Image resized = img.getScaledInstance(60, 90, Image.SCALE_SMOOTH);
            p.setPreferredSize(new Dimension(90, 110));
            p.add(new JLabel(new ImageIcon(resized)));
            topPanel.add(p);
        }

        // Display players in right panel
        System.out.println("PlayWindow: Display current players");
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.PAGE_AXIS));
        for (User playerinfo : playerList) {
            JPanel j = new JPanel();
            j.setBorder(new CompoundBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLACK), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
            j.setLayout(new BoxLayout(j, BoxLayout.PAGE_AXIS));
            j.setPreferredSize(new Dimension(180, 70));
            j.add(new JLabel(playerinfo.getUsername()));
            JLabel playerWinStats = new JLabel("Wins: " + playerinfo.getNumWin() + "/"
                    + playerinfo.getTotalGames() + " " + "Avg: " + playerinfo.getAvgTime() + "s");
            j.add(playerWinStats);
            rightPanel.add(j);
        }

        // Input field in bottom panel
        System.out.println("PlayWindow : Display input field");
        JTextField answerField = new JTextField();
        JLabel result = new JLabel("= 24");
        answerField.setPreferredSize(new Dimension(300, 40));
        bottomPanel.add(answerField, BorderLayout.WEST);
        bottomPanel.add(result, BorderLayout.CENTER);

        // Detects 'Enter' key press and submits answer to server
        answerField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String submission = answerField.getText();
                    System.out.println("Message Sent: " + answerField.getText());
                    String result = client.server.submitAnswer(client.user, game.getPlayerList(), submission);
                    System.out.println("PlayWindow: server replied with: " + result);

                    if (!result.equals("VALID"))
                        JOptionPane.showMessageDialog(new JFrame(), result, "Wrong Input", JOptionPane.ERROR_MESSAGE);
                } catch (RemoteException e1) {
                    JOptionPane.showMessageDialog(new JFrame(), e1.getMessage(), "Wrong Input", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        this.add(topPanel, BorderLayout.CENTER);
        this.add(rightPanel, BorderLayout.EAST);
        this.add(bottomPanel, BorderLayout.SOUTH);
    }
}