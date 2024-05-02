import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;


import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class GameClient {
    private ServerInterface server;

    private JFrame loginFrame;
    private JFrame registerFrame;
    private JFrame mainFrame;
    private MainPanel viewPanel;

    private JPanel leaderboard;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField registerUsernameField;
    private JPasswordField registerPasswordField;
    private JPasswordField registerConfirmPasswordField;

    private User user;

    private int currentPanel = -1;

    /**
     * Parameterized constructor to connect with RMI service on another machine
     * @param host: Address of the host where RMI Registry is running
     */
    public GameClient(String host) {
        try {
            Registry registry = LocateRegistry.getRegistry(host);
            server = (ServerInterface)registry.lookup("Server");
        } catch(Exception e) {
            System.err.println("Failed accessing RMI: "+e);
            System.exit(1);
        }
    }

    /**
     * Default constructor (connects to RMI service on same machine)
     */
    public GameClient() {
        try {
            server = (ServerInterface)Naming.lookup("Server");
        } catch(Exception e) {
            System.err.println("Failed accessing RMI: "+e);
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        GameClient client = new GameClient();
        client.go();
    }

    public void go() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                generateGUI();
            }
        });
    }


    private JPanel createLoginPanel() {
        JPanel loginPanel = new JPanel();
        loginPanel.setPreferredSize(new Dimension(350,200));
        loginPanel.setBorder(BorderFactory.createTitledBorder("Login"));

        JPanel loginComponent = new JPanel();
        loginComponent.setLayout(new BoxLayout(loginComponent, BoxLayout.Y_AXIS));

        JPanel login_username_component = new JPanel();
        login_username_component.setLayout(new BorderLayout());
        JLabel usernameLabel = new JLabel("Username:");
        usernameField = new JTextField();
        usernameField.setPreferredSize(new Dimension(250, 25));
        login_username_component.add(usernameLabel, BorderLayout.CENTER);
        login_username_component.add(usernameField, BorderLayout.PAGE_END);
        loginComponent.add(login_username_component);

        JPanel login_password_component = new JPanel();
        login_password_component.setLayout(new BorderLayout());
        JLabel passwordLabel = new JLabel("Password:");
        passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(250, 25));
        login_password_component.add(passwordLabel, BorderLayout.CENTER);
        login_password_component.add(passwordField, BorderLayout.PAGE_END);
        loginComponent.add(login_password_component);

        JPanel login_buttons_component = new JPanel();
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(new LoginHandler());
        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(new NavigationHandler("registerNav"));
        login_buttons_component.add(loginButton);
        login_buttons_component.add(registerButton);
        loginComponent.add(login_buttons_component);

        loginPanel.add(loginComponent);

        return loginPanel;
    }

    private JPanel createRegisterPanel() {
        JPanel registerPanelParent = new JPanel();
        registerPanelParent.setPreferredSize(new Dimension(350, 150));
        registerPanelParent.setBorder(BorderFactory.createTitledBorder("Register"));

        JPanel registerPanel = new JPanel();
        registerPanel.setLayout(new BoxLayout(registerPanel, BoxLayout.Y_AXIS));

        JPanel r_usernameComponent = new JPanel();
        r_usernameComponent.setLayout(new BorderLayout());
        JLabel register_username_label = new JLabel("Username:");
        registerUsernameField = new JTextField();
        registerUsernameField.setPreferredSize(new Dimension(250, 25));
        r_usernameComponent.add(register_username_label, BorderLayout.CENTER);
        r_usernameComponent.add(registerUsernameField, BorderLayout.PAGE_END);
        registerPanel.add(r_usernameComponent);

        JPanel r_passwordComponent = new JPanel();
        r_passwordComponent.setLayout(new BorderLayout());
        JLabel register_password_label = new JLabel("Password:");
        registerPasswordField = new JPasswordField();
        registerPasswordField.setPreferredSize(new Dimension(250, 25));
        r_passwordComponent.add(register_password_label, BorderLayout.CENTER);
        r_passwordComponent.add(registerPasswordField, BorderLayout.PAGE_END);
        registerPanel.add(r_passwordComponent);

        JPanel r_confirmPasswordComponent = new JPanel();
        r_confirmPasswordComponent.setLayout(new BorderLayout());
        JLabel register_confirm_password_label = new JLabel("Confirm Password:");
        registerConfirmPasswordField = new JPasswordField();
        registerConfirmPasswordField.setPreferredSize(new Dimension(250, 25));
        r_confirmPasswordComponent.add(register_confirm_password_label, BorderLayout.CENTER);
        r_confirmPasswordComponent.add(registerConfirmPasswordField, BorderLayout.PAGE_END);
        registerPanel.add(r_confirmPasswordComponent);

        JPanel buttonPanel = new JPanel();
        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(new RegisterHandler());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new NavigationHandler("cancel"));
        buttonPanel.add(registerButton);
        buttonPanel.add(cancelButton);
        registerPanel.add(buttonPanel);

        registerPanelParent.add(registerPanel);

        return registerPanelParent;
    }

    private JPanel createMainMenu() {
        JPanel menu = new JPanel(new FlowLayout());

        JButton profile = new JButton("User Profile");
        profile.setPreferredSize(new Dimension(160, 30));
        profile.addActionListener(new NavigationHandler("userProfile"));
        menu.add(profile);

        JButton game = new JButton("Play Game");
        game.setPreferredSize(new Dimension(160, 30));
        game.addActionListener(new NavigationHandler("game"));
        menu.add(game);

        JButton board = new JButton("Leaderboard");
        board.setPreferredSize(new Dimension(160, 30));
        board.addActionListener(new NavigationHandler("leaderboard"));
        menu.add(board);

        JButton logout = new JButton("Logout");
        logout.setPreferredSize(new Dimension(160, 30));
        logout.addActionListener(new LogoutHandler());
        menu.add(logout);

        return menu;
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
            data = server.getTopPlayers();
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

    public void generateGUI() {
        loginFrame = new JFrame("Login");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setPreferredSize(new Dimension(500, 400));

        registerFrame = new JFrame("Register");
        registerFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        registerFrame.setPreferredSize(new Dimension(500, 400));

        mainFrame = new JFrame("JPoker 24-Game");
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.setPreferredSize(new Dimension(800, 400));
        mainFrame.setResizable(true);

        viewPanel = new MainPanel();

        mainFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                if (server != null) {
                    try {
                        System.out.println("Main frame closed - logging out...");
                        server.logout(user.getUsername());
                        mainFrame.setVisible(false);
                        registerFrame.setVisible(false);
                        loginFrame.setVisible(true);
                    } catch (RemoteException err) {
                        System.err.println("Failed invoking RMI: " + e);
                    }
                }
            }
        });

        loginFrame.add(createLoginPanel(), BorderLayout.CENTER);
        loginFrame.pack();

        registerFrame.add(createRegisterPanel(), BorderLayout.CENTER);
        registerFrame.pack();

        mainFrame.add(createMainMenu(), BorderLayout.NORTH);
        mainFrame.add(viewPanel, BorderLayout.CENTER);
        mainFrame.pack();

        loginFrame.setVisible(true);
    }

    private class LoginHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ae) {
            System.out.println("Attempting to log in...");

            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            if (username.isEmpty())
                JOptionPane.showMessageDialog(null, "Username should not be empty", "Error", JOptionPane.ERROR_MESSAGE);
            else if (password.isEmpty())
                JOptionPane.showMessageDialog(null, "Password should not be empty", "Error", JOptionPane.ERROR_MESSAGE);
            else {
                if (server != null) {
                    try {
                        int status = server.login(username, password);
                        System.out.println("Login status (4 is successful): " + status);
                        if (status == 1)
                            JOptionPane.showMessageDialog(null, "User does not exist! Please register first", "Error", JOptionPane.ERROR_MESSAGE);
                        else if (status == 2)
                            JOptionPane.showMessageDialog(null, "Wrong Password", "Error", JOptionPane.ERROR_MESSAGE);
                        else if (status == 3)
                            JOptionPane.showMessageDialog(null, "User already logged in", "Error", JOptionPane.ERROR_MESSAGE);
                        else if (status == 4) {
                            currentPanel = 0;
                            user = server.getUserDetails(username);
                            viewPanel = new MainPanel();
                            viewPanel.repaint();
                            if (leaderboard != null) {
                                mainFrame.remove(leaderboard);
                                mainFrame.add(viewPanel);
                            }
                            mainFrame.pack();
                            mainFrame.repaint();
                            loginFrame.setVisible(false);
                            registerFrame.setVisible(false);
                            mainFrame.setVisible(true);
                        }
                    } catch (RemoteException e) {
                        System.err.println("Failed invoking RMI: " + e);
                    }
                }
            }
        }
    }

    private class RegisterHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ae) {
            System.out.println("Attempting to register user...");
            String username = registerUsernameField.getText();
            String password = new String(registerPasswordField.getPassword());
            String confirmPassword = new String(registerConfirmPasswordField.getPassword());

            if (username.isEmpty())
                JOptionPane.showMessageDialog(null, "Username should not be empty", "Error", JOptionPane.ERROR_MESSAGE);
            else if (password.isEmpty())
                JOptionPane.showMessageDialog(null, "Password should not be empty", "Error", JOptionPane.ERROR_MESSAGE);
            else if (confirmPassword.isEmpty())
                JOptionPane.showMessageDialog(null, "Confirm password should not be empty", "Error", JOptionPane.ERROR_MESSAGE);
            if (!password.equals(confirmPassword))
                JOptionPane.showMessageDialog(null, "Passwords do not match", "Error", JOptionPane.ERROR_MESSAGE);
            else {
                if (server != null) {
                    try {
                        int status = server.register(username, password);
                        if (status == 4) {
                            user = server.getUserDetails(username);
                            if (currentPanel == -1) {
                                currentPanel = 0;
                                viewPanel = new MainPanel();

                            } else {
                                currentPanel = 0;
                                if (leaderboard != null) {
                                    mainFrame.remove(leaderboard);
                                }
                            }
                            mainFrame.add(viewPanel);
                            mainFrame.pack();
                            viewPanel.repaint();
                            mainFrame.repaint();
                            loginFrame.setVisible(false);
                            registerFrame.setVisible(false);
                            mainFrame.setVisible(true);
                        } else if (status == 1)
                            JOptionPane.showMessageDialog(null, "Username taken, please choose another", "Error", JOptionPane.ERROR_MESSAGE);
                    } catch (RemoteException e) {
                        System.err.println("Failed invoking RMI: register" + e);
                    }
                }
            }
        }
    }

    private class LogoutHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ae) {
            if (server != null) {
                try {
                    server.logout(user.getUsername());
                    mainFrame.setVisible(false);
                    registerFrame.setVisible(false);
                    loginFrame.setVisible(true);
                } catch (RemoteException e) {
                    System.err.println("Failed invoking RMI: logout" + e);
                }
            }
        }
    }

    private class NavigationHandler implements ActionListener {
        private final String eventSource;

        public NavigationHandler (String eventSource) {
            this.eventSource = eventSource;
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            System.out.println("Button pressed: " + ae.getActionCommand());
            switch (eventSource) {
                case "registerNav": {
                    loginFrame.setVisible(false);
                    mainFrame.setVisible(false);
                    registerFrame.setVisible(true);
                    break;
                }
                case "cancel": {
                    registerFrame.setVisible(false);
                    mainFrame.setVisible(false);
                    loginFrame.setVisible(true);
                    break;
                }
                case "userProfile": {
                    currentPanel = 0;
                    if (leaderboard != null) {
                        mainFrame.remove(leaderboard);
                        mainFrame.add(viewPanel);
                    }
                    viewPanel.repaint();
                    mainFrame.pack();
                    mainFrame.repaint();
                    mainFrame.setVisible(true);
                    break;
                }
                case "game": {
                    currentPanel = 1;
                    if (leaderboard != null) {
                        mainFrame.remove(leaderboard);
                        mainFrame.add(viewPanel);
                    }
                    viewPanel.repaint();
                    mainFrame.pack();
                    mainFrame.repaint();
                    mainFrame.setVisible(true);
                    break;
                }
                case "leaderboard": {
                    currentPanel = 2;
                    leaderboard = createLeaderboard();
                    mainFrame.remove(viewPanel);
                    mainFrame.add(leaderboard);
                    mainFrame.pack();
                    mainFrame.repaint();
                    mainFrame.setVisible(true);
                    break;
                }
                default: {
                    System.out.println("Unrecognized event source.");
                }
            }
        }
    }


    private class MainPanel extends JPanel {
        // 0: User Profile, 1: Play Game, 2: Leaderboard (subject to change)
        public MainPanel() {
        }
        public void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D)g;

            if (currentPanel == 0) {
                Font f = new Font("Calibri", Font.BOLD, 30);

                g2.setFont(f);
                g2.setStroke(new BasicStroke(2));
                g2.drawString(user.getUsername(), 30, 50);

                f = new Font("Calibri", Font.PLAIN, 20);
                g2.setFont(f);
                g2.drawString("Number of wins: " + user.getNumWin(), 30, 90);
                g2.drawString("Number of games: " + user.getTotalGames(), 30, 120);
                g2.drawString("Average time to win: " + user.getAvgTime() + " s", 30, 150);

                f = new Font("Arial", Font.BOLD, 25);
                g2.setFont(f);
                g2.drawString("Rank: #" + "TBD", 30, 190);
            } else if (currentPanel == 1) {
                Font f = new Font("Calibri", Font.BOLD, 30);
                g2.setFont(f);
                g2.drawString("Game Screen", 120, 150);
            } // currentPanel == 2 (Leaderboard) handled by separate class for now
            mainFrame.pack();
            mainFrame.setVisible(true);
        }
    }
}