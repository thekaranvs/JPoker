import java.util.ArrayList;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;

// Extends the JMS program to include helper functions to augment the client program
public class JMSClient implements MessageListener {

    JMS jmsHelper;
    private MessageProducer queueSender;
    private MessageConsumer topicSubscriber;
    private GameClient client;
    public ArrayList<User> playerList = new ArrayList<User>();
    public GameMessage gameMsg;
    public GameMessage gameOverMsg;

    public JMSClient(GameClient client) throws NamingException, JMSException {
        this.client = client;
        jmsHelper = new JMS();
        queueSender = jmsHelper.createQueueSender();
        topicSubscriber = jmsHelper.createTopicSubscriber();
        topicSubscriber.setMessageListener(this);

        System.out.println("JMSClient: initialization done!");
    }

    public void sendJoinMessage(String username) {
        GameMessage playGameMessage = new GameMessage();
        playGameMessage.setCommand("JOIN");
        playGameMessage.setUser(client.user);
        playGameMessage.setUserName(username);

        System.out.println("JMSClient: Sending join message: " + playGameMessage);
        Message message = null;
        try {
            message = jmsHelper.createMessage(playGameMessage);
        } catch (JMSException e) {
            System.out.println("JMSClient: Error while creating message to send");
        }
        if (message != null) {
            try {
                queueSender.send(message);
            } catch (JMSException e) {
                System.err.println("JMSClient: Failed to send message");
            }
        }
        System.out.println("JMSClient: Message sent");
    }

    @Override
    public void onMessage(Message jmsMessage) {
        System.out.println("JMSClient: message received!");
        GameMessage serverMsg = null;
        try {
            serverMsg = (GameMessage)((ObjectMessage)jmsMessage).getObject();
        } catch (JMSException e) {
            e.printStackTrace();
        }
        System.out.println("JMSClient: Received message: " + serverMsg);
        if (serverMsg.getCommand().equals("START")) {
            gameMsg = serverMsg;
            for (User player : serverMsg.getPlayerList()) {
                if (player.getUsername().equals(this.client.user.getUsername())) {
                    playerList = serverMsg.getPlayerList();
                    client.mainWindow.showGamePanel();
                }
            }
        } else if (serverMsg.getCommand().equals("GAME_OVER")) {
            gameOverMsg = serverMsg;
            client.mainWindow.showGameOver();
            System.out.println("JMSClient: Game Over!");
        }
    }

}