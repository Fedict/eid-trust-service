package be.fedict.trust.service.bean;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.Depends;

import be.fedict.trust.service.NotificationService;

@Stateless
@Depends({
		"org.hornetq:module=JMS,name=\"" + HarvesterMDB.HARVESTER_QUEUE_NAME
				+ "\",type=Queue",
		"org.hornetq:module=JMS,name=\"" + DownloaderMDB.DOWNLOADER_QUEUE_NAME
				+ "\",type=Queue" })
public class NotificationServiceBean implements NotificationService {

	private static final Log LOG = LogFactory
			.getLog(NotificationServiceBean.class);

	@Resource(mappedName = "java:JmsXA")
	private QueueConnectionFactory queueConnectionFactory;

	@Resource(mappedName = DownloaderMDB.DOWNLOADER_QUEUE_LOCATION)
	private Queue downloaderQueue;

	@Resource(mappedName = HarvesterMDB.HARVESTER_QUEUE_LOCATION)
	private Queue harvesterQueue;

	private void sendMessage(JMSMessage message, Queue queue)
			throws JMSException {
		QueueConnection queueConnection = this.queueConnectionFactory
				.createQueueConnection();
		try {
			QueueSession queueSession = queueConnection.createQueueSession(
					true, Session.AUTO_ACKNOWLEDGE);
			try {
				String messageType = message.getClass().getSimpleName();
				Message jmsMessage = message.getJMSMessage(queueSession);
				jmsMessage.setStringProperty(JMSMessage.MESSAGE_TYPE_PROPERTY,
						messageType);
				QueueSender queueSender = queueSession.createSender(queue);
				try {
					queueSender.send(jmsMessage);
				} finally {
					queueSender.close();
				}
			} finally {
				queueSession.close();
			}
		} finally {
			queueConnection.close();
		}
	}

	public void notifyDownloader(String issuerName, boolean update)
			throws JMSException {
		LOG.debug("notifying downloader for CA: " + issuerName);
		JMSMessage downloadMessage = new DownloadMessage(issuerName, update);
		sendMessage(downloadMessage, this.downloaderQueue);
	}

	public void notifyHarvester(String issuerName, String crlFile,
			boolean update) throws JMSException {
		LOG.debug("notifying harvester for CA: " + issuerName);
		JMSMessage harvestMessage = new HarvestMessage(issuerName, crlFile,
				update);
		sendMessage(harvestMessage, this.harvesterQueue);
	}

	public void notifyColdStart(String crlUrl, String certUrl)
			throws JMSException {
		LOG.debug("notifying downloader for CA cold starting: " + crlUrl);
		ColdStartMessage coldStartMessage = new ColdStartMessage(crlUrl,
				certUrl);
		sendMessage(coldStartMessage, this.downloaderQueue);
	}

	public void notifyRemoveCA(String issuerName) throws JMSException {
		LOG.debug("notifying harvester for removal of CRL cache for CA: "
				+ issuerName);
		RemoveCAMessage removeCAMessage = new RemoveCAMessage(issuerName);
		sendMessage(removeCAMessage, this.harvesterQueue);
	}
}
