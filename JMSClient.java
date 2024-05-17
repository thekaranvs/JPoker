import java.util.ArrayList;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;

public class JMSClient implements MessageListener {

    JMS jmsHelper;
    private MessageProducer queueSender;
    private MessageConsumer topicSubscriber;
    private GameClient user;
    public ArrayList<User> playerList = new ArrayList<User>();

    public JMSClient(GameClient user) throws NamingException, JMSException {
        this.user = user;
        jmsHelper = new JMS();
        init();
    }

    private void init() throws JMSException {
        queueSender = jmsHelper.createQueueSender();
        topicSubscriber = jmsHelper.createTopicSubscriber();
        topicSubscriber.setMessageListener(this);

        System.out.println("JMSClient: init done!");
    }

    public void sendMessage(String name) {
        JMSMessage chatMessage = new JMSMessage(name, user.win, user.avgwin,user.gameplayed);
        if (chatMessage != null) {
            System.out.println("JMSClient: Trying to send message: "
                    + chatMessage);
            Message message = null;
            try {
                message = jmsHelper.createMessage(chatMessage);
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
        }
        System.out.println("JMSClient: Message send" + chatMessage);
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
        if (serverMsg.getCommand().equals("START")) {
            for (User player : serverMsg.getPlayerList()) {
                if (player.getUsername().equals(this.user.name)) {
                    playerList = serverMsg.getPlayerList();
                    user.mainwindow.playinggame();
                    System.out.println("JMSClient: we have a new play board now");
                }
            }
        } else if (serverMsg.getCommand().equals("GAME_OVER")) {
            if (serverMsg.getPlayerList().contains(this.user.name)) {
                user.mainwindow.gameoverboard();
                System.out.println("JMSClient: we finish the game");
            }
        }
//        else if (serverMsg instanceof QuitMsg) {
//            quitplayer = (QuitMsg) serverMsg;
//            if (quitplayer.players.contains(this.user.name)) {
//                user.mainwindow.pnlPlaying.updateplayer(quitplayer.players);
//                System.out.println("JMSClient: we lost a user");
//            }
//        }
    }

}