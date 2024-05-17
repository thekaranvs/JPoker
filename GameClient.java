import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class GameClient {
    public ServerInterface server;

    JFrame loginFrame;
    JFrame registerFrame;
    GameClientMainWindow mainWindow;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField registerUsernameField;
    private JPasswordField registerPasswordField;
    private JPasswordField registerConfirmPasswordField;

    User user;
    public JMSClient jmsclient;

    /**
     * Parameterized constructor to connect with RMI service on another machine
     * @param host: Address of the host where RMI Registry is running
     */
    public GameClient(String host) {
        try {
            Registry registry = LocateRegistry.getRegistry(host);
            server = (ServerInterface)registry.lookup("Server");
            jmsclient =  new JMSClient(this);
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
            jmsclient =  new JMSClient(this);
        } catch(Exception e) {
            System.err.println("Failed accessing RMI or JMS client: "+e);
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

    public void generateGUI() {
        loginFrame = new JFrame("Login");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setPreferredSize(new Dimension(500, 400));

        registerFrame = new JFrame("Register");
        registerFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        registerFrame.setPreferredSize(new Dimension(500, 400));

        loginFrame.add(createLoginPanel(), BorderLayout.CENTER);
        loginFrame.pack();

        registerFrame.add(createRegisterPanel(), BorderLayout.CENTER);
        registerFrame.pack();

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
                            user = server.getUserDetails(username);
                            mainWindow = new GameClientMainWindow(GameClient.this, user);
                            loginFrame.setVisible(false);
                            registerFrame.setVisible(false);
                        }
                    } catch (RemoteException e) {
                        System.err.println("Failed invoking RMI: " + e);
                    }
                }
            }
        }
    }

    public void retrieveLatestUserInfo() {
        try {
            user = server.getUserDetails(user.getUsername());
        } catch (Exception e) {
            System.out.println("Error while retrieving latest user details: " + e);
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
                            mainWindow = new GameClientMainWindow(GameClient.this, user);
                            loginFrame.setVisible(false);
                            registerFrame.setVisible(false);
                        } else if (status == 1)
                            JOptionPane.showMessageDialog(null, "Username taken, please choose another", "Error", JOptionPane.ERROR_MESSAGE);
                    } catch (RemoteException e) {
                        System.err.println("Failed invoking RMI: register" + e);
                    }
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
                    registerFrame.setVisible(true);
                    break;
                }
                case "cancel": {
                    registerFrame.setVisible(false);
                    loginFrame.setVisible(true);
                    break;
                }
                default: {
                    System.out.println("Unrecognized event source.");
                }
            }
        }
    }
}