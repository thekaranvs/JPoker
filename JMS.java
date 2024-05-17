import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;
import java.io.Serializable;

public class JMS {

    // Configuration of JMS on computer (as specified in assignment)
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 3700;

    private static final String JMS_CONNECTION_FACTORY = "jms/JPoker24GameConnectionFactory";
    private static final String JMS_TOPIC = "jms/JPoker24GameTopic";
    private static final String JMS_QUEUE = "jms/JPoker24GameQueue";

    // JNDI and JMS specific objects
    private Context jndiContext;
    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Session session;
    private Queue queue;
    private Topic topic;

    public JMS() throws NamingException, JMSException {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    public JMS(String host, int port) throws NamingException, JMSException {
        // Access JNDI
        createJNDIContext(host, port);
        System.out.println("JMS: Created JNDI context - looking up conn factory and q and topic");
        // Lookup JMS resources (using variables defined above)
        lookupConnectionFactory();
        lookupQueue();
        lookupTopic();
        System.out.println("JMS: Looked up queue and topic - creating connection");
        // Create connection->session->sender
        createConnection();
        System.out.println("JMS: Connection created");
    }

    public Session getSession() throws JMSException {
        if (session != null)
            return session;
        try {
            return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (JMSException e) {
            System.err.println("Failed creating session: " + e);
            throw e;
        }
    }

    public MessageProducer createQueueSender() throws JMSException {
        try {
            return getSession().createProducer(queue);
        } catch (JMSException e) {
            System.err.println("Failed sending to queue: " + e);
            throw e;
        }
    }

    public MessageConsumer createQueueReceiver() throws JMSException {
        try {
            return getSession().createConsumer(queue);
        } catch (JMSException e) {
            System.err.println("Failed reading from queue: " + e);
            throw e;
        }
    }

    public MessageProducer createTopicPublisher() throws JMSException {
        try {
            return getSession().createProducer(topic);
        } catch (JMSException e) {
            System.err.println("Failed sending to topic: " + e);
            throw e;
        }
    }

    public MessageConsumer createTopicSubscriber() throws JMSException {
        try {
            return getSession().createConsumer(topic);
        } catch (JMSException e) {
            System.err.println("Failed reading from topic: " + e);
            throw e;
        }
    }

    public ObjectMessage createMessage(Serializable obj) throws JMSException {
        return getSession().createObjectMessage(obj);
    }

    private void createJNDIContext(String host, int port) throws NamingException {
        System.setProperty("org.omg.CORBA.ORBInitialHost", host);
        System.setProperty("org.omg.CORBA.ORBInitialPort", String.valueOf(port));
        try {
            jndiContext = new InitialContext();
        } catch (NamingException e) {
            System.err.println("Could not create JNDI API context: " + e);
            throw e;
        }
    }

    private void lookupConnectionFactory() throws NamingException {
        try {
            connectionFactory = (ConnectionFactory)jndiContext.lookup(JMS.JMS_CONNECTION_FACTORY);
        } catch (NamingException e) {
            System.err.println("JNDI API JMS connection factory lookup failed: " + e);
            throw e;
        }
    }

    private void lookupQueue() throws NamingException {
        try {
            queue = (Queue)jndiContext.lookup(JMS.JMS_QUEUE);
        } catch (NamingException e) {
            System.err.println("JNDI API JMS queue lookup failed: " + e);
            throw e;
        }
    }

    private void lookupTopic() throws NamingException {
        try {
            topic = (Topic)jndiContext.lookup(JMS.JMS_TOPIC);
        } catch (NamingException e) {
            System.err.println("JNDI API JMS topic lookup failed: " + e);
            throw e;
        }
    }

    private void createConnection() throws JMSException {
        try {
            connection = connectionFactory.createConnection();
            connection.start();
        } catch (JMSException e) {
            System.err.println("Failed to create connection to JMS provider: " + e);
            throw e;
        }
    }
}
