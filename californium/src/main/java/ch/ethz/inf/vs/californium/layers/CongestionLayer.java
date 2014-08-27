package ch.ethz.inf.vs.californium.layers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.LinkedList;

import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.ObservingManager;
import ch.ethz.inf.vs.californium.coap.EndpointAddress;
import ch.ethz.inf.vs.californium.util.Properties;


/*Receives message from the upper layer
 * Creates or finds a flow based on the destination of the message
 * Calls the relevant flow functions to further process the packet
 */

public class CongestionLayer extends UpperLayer {

	//Flow table to store all flows.
	private Map<String, Flow> flowTable = new HashMap<String, Flow>(); 


	/* Inherited function from layer abstract class.
	 * (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.layers.Layer#doSendMessage(ch.ethz.inf.vs.californium.coap.Message). 
	 * Used to send Message to lower layer. Based on the peer address,
	 * it finds or creates a flow. A flow regulates the CC parameters.
	 */

	@Override
	protected void doSendMessage(Message msg) throws IOException {

	//	LOG.warning(String.format("doSendMessage called for %s", msg.getPeerAddress().toString()));
		if (msg.isConfirmable()) {
			String key = msg.getPeerAddress().toString();
			Flow flow = flowTable.get(key);
			if (flow == null) {
				/*Create a new flow and do all the necessary initializations*/
				flow = new Flow(msg);
				flowTable.put(key, flow);
				LOG.warning(String.format("flow table created for %s", key));
			}	
			flow.addTransaction(msg);	
			flow.flushQueue();
	//		LOG.warning(String.format("Total number of flows %d",flowTable.size()));
		}
		else {
			sendMessageOverLowerLayer(msg);
		}
	}


	/*Wrapper class for sendMessageOverLowerLayer.
	 * Can be called by nested classes (like flow) to send
	 * messages during retransmissions. 
	 */
	protected void sendCCMessage(Message msg) throws IOException {

		sendMessageOverLowerLayer(msg);
	}

	/*Inherited function from Layer abstract class.
	 * (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.layers.Layer#doReceiveMessage(ch.ethz.inf.vs.californium.coap.Message)
	 * It receives the message from lower layer, processes it before passing
	 * to the next higher layer in stack.
	 */

	@Override
	protected void doReceiveMessage(Message msg) {

		LOG.warning(String.format("doReceiveMessage called for %s", msg.getPeerAddress().toString()));
		if (msg.isAcknowledgement()) {
			String key = msg.getPeerAddress().toString();
			Flow flow = flowTable.get(key); 
	//		LOG.warning(String.format("%s message is ACK ", msg.key()));
			if (flow != null) {
				flow.removeTransaction(msg);
			}
		}
		//pass message to the higher layer.
		deliverMessage(msg);
	}

	/*returns true/false based on the FifoQueue for a given endpoint*/
	
	public boolean isFree(String key) {
		System.out.println("isFree called for "+key);
		Flow flow = flowTable.get(key);
		if (flow != null) {
			    System.out.println("flow found for "+key);
			if (flow.fifoQueue.size() == 0)	
					return true;
				else
				return false;
		}
		System.out.println("flow not for "+key);
		return false;
	}
	

	/*Internal Flow class. It provides mechanisms to throttle 
	 * data rate by implementing congestion control methods.
	 * Every flow has its own set of CC variables, independent
	 * of other flows.
	 */

	private class Flow {

		//Message ID global for all transactions of this flow
		private int messageID = (int) (Math.random() * 0x10000);  
		//FiFo Queue, transactions are first put here
		private LinkedList<Transaction> fifoQueue = new LinkedList<Transaction>();
		//Cache for inflight transactions. Transactions are moved here from FiFo for transmission
		private Map<String, Transaction> inflightCache = new HashMap<String, Transaction>();
		//Timer for retranmisssions.
		private Timer timer = new Timer(true);
		//Max inflight Queue Size == NSTART of RFC
		private final int maxInflightQueueSize = 1;


		/*Inner classes of the flow */	
		/*Transaction is the entity class borrowed from transaction layer 
		 * It translates a message to transaction, which can be stored for 
		 * congestion control purposes
		 */
		
		private  class Transaction {
			Message msg;
			RetransmitTask retransmitTask;
			int numRetransmit;
			int timeout; // to satisfy RESPONSE_RANDOM_FACTOR
		}


		/*This class borrows from the transaction layer as well.
		 * utility class to handle retransmissions only
		 */

		private class RetransmitTask extends TimerTask {
			private Transaction transaction;

			RetransmitTask(Transaction transaction) {
				this.transaction = transaction;
			}

			@Override
			public void run() {
				handleResponseTimeout(transaction);
			}
		}


		/*Default constructor, currently does nothing. 
		 * Can be used to initialize flow related variables.
		 */

		public Flow(Message msg) {
			//constructor
		}			

		/* Initialize the transaction for the given msg, 
		 * adds it to FiFo Queue. Returns the new transaction.
		 */

		public Transaction addTransaction(Message msg) {

			Transaction transaction = new Transaction();
			transaction.msg = msg;
			if (msg.getMID() < 0) {
				msg.setMID(nextMessageID());
			}
			transaction.numRetransmit = 0;
			transaction.retransmitTask = null;
			fifoQueue.addLast(transaction);
	//		LOG.warning(String.format("Stored new transaction for %s", msg.transactionKey()));
			return transaction;
		}

		/* Searches for the transaction for the given 
		 * msg in the Inflight Cache. When found, it cancels 
		 * all retransmit timers and clears it from Cache. 
		 */

		private synchronized void removeTransaction(Message msg) {

	//		LOG.warning(String.format("finding transaction for %s", msg.transactionKey()));
			Transaction transaction = inflightCache.get(msg.transactionKey());
			if (transaction != null) {
	//			LOG.warning(String.format("found transaction for %s", msg.transactionKey()));
				// cancel any pending retransmission schedule
				transaction.retransmitTask.cancel();
				transaction.retransmitTask = null;

				// remove transaction from table
				inflightCache.remove(transaction.msg.transactionKey());
	//			LOG.warning(String.format("Cleared transaction for %s", transaction.msg.transactionKey()));
				flushQueue();
			}		
		}

		/*Calculates the value of initial timeout
		 * for every transaction. 
		 */

		private int initialTimeout() {

			final double min = Properties.std.getDbl("RESPONSE_TIMEOUT");
			final double f = Properties.std.getDbl("RESPONSE_RANDOM_FACTOR");
			return (int) (min + (min * (f - 1d) * Math.random()));
		}

		/*This function is called to calculate the RTO value
		 * for a transaction before transmission or after
		 * a timeout has occurred. The strong/weak RTO
		 *  estimator goes here.
		 */

		private void scheduleRetransmission(Transaction transaction) {

			// cancel existing schedule (if any)
			if (transaction.retransmitTask != null) {
				transaction.retransmitTask.cancel();
			}
			// create new retransmission task
			transaction.retransmitTask = new RetransmitTask(transaction);
			// calculate timeout using exponential back-off
			if (transaction.timeout == 0) {
				// use initial timeout
				transaction.timeout = initialTimeout();
			} else {
				// double timeout
				transaction.timeout *= 2;
			}
			// schedule retransmission task
			timer.schedule(transaction.retransmitTask, transaction.timeout);
		}

		/*Returns a unique MessageID, incrementing sequentially for a flow. 
		 */

		public  int nextMessageID() {

			messageID = ++messageID % 0x10000;
			return messageID;
		}


		/* Private flow function, when called, checks the
		 * inflight cache and if the CC algorithm allows,
		 * it transfers the transactions from FiFo Queue to 
		 * inflight cache. Throttling of packets happen here.
		 * Timer for retransmission is started once the packet 
		 * is added to inflight cache and sent out to UDP layer. 
		 */

		private void flushQueue() {

			while ((inflightCache.size() <= maxInflightQueueSize) && (fifoQueue.size() > 0) ) {
				Transaction transaction = fifoQueue.pollFirst();
				LOG.warning(String.format("After polling fifoQueueSize %d", fifoQueue.size()));
				if (transaction != null) {
					inflightCache.put(transaction.msg.transactionKey(), transaction);
					scheduleRetransmission(transaction);
					try {
						sendCCMessage(transaction.msg);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}	


		/*This function is called when a timeout occurs. 
		 * It checks for the maximum allowed retransmissions, 
		 * either discards or retransmit the transaction based 
		 * on that information.
		 */
		
		private void handleResponseTimeout(Transaction transaction) {

			final int max = Properties.std.getInt("MAX_RETRANSMIT");
			// check if limit of retransmissions reached
			if (transaction.numRetransmit < max) {
				// retransmit message
				transaction.msg.setRetransmissioned(++transaction.numRetransmit); 
				LOG.info(String.format("Retransmitting %s (%d of %d)", transaction.msg.key(), transaction.numRetransmit, max));
				try {
					sendCCMessage(transaction.msg);
				} catch (IOException e) {
					LOG.severe(String.format("Retransmission failed: %s", e.getMessage()));
					removeTransaction(transaction.msg);
					return;
				}
				// schedule next retransmission
				scheduleRetransmission(transaction);
			} else {
				// cancel transmission
				removeTransaction(transaction.msg);
				// cancel observations
				ObservingManager.getInstance().removeObserver(transaction.msg.getPeerAddress().toString());
				// invoke event handler method
				transaction.msg.handleTimeout();
			}	
		}		
	} /* End of Flow Class */ 

}