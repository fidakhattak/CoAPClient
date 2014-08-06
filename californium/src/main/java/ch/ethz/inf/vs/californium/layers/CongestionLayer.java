package ch.ethz.inf.vs.californium.layers;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.LinkedList;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.Message;
import ch.ethz.inf.vs.californium.coap.ObservingManager;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.UnsupportedRequest;
import ch.ethz.inf.vs.californium.util.Properties;

public class CongestionLayer extends UpperLayer {
	
	/*Receives message from the upper layer
	 * Creates or finds a flow based on the destination of the message
	 * Calls the relevant flow functions to further process the packet
	 */
private Map<String, Flow> flowTable = new HashMap<String, Flow>(); //transaction table
		
	private class Flow {
			String flowID; //unique flowid, url used for this purpose 
			private int messageID = (int) (Math.random() * 0x10000);  //Message ID global for all transactions of this flow
			private LinkedList<Transaction> fifoQueue = new LinkedList<Transaction>(); //transaction table
			private Map<String, Transaction> inflightQueue = new HashMap<String, Transaction>();
			private Timer timer = new Timer(true); 
			private final int maxInflightQueueSize = 1;
			
			/*Inner classes of the flow */	
			/*Transaction is the entity class borrowed from transaction layer 
			 * It translates a message to transaction, which can be logged for 
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
			/*In constructor do the following
				 * set flowid;  Done, by extracting uri and using as uniquely remote location identifier.
				 */
			public Flow(Message msg) {
				flowID	 = msg.getUriPath();
			}			
			/* Initialize the transaction as done before in transaction layer
			 * Add transaction to MAP
			 */
				// initialize new transmission context
			
			public Transaction addTransaction(Message msg) {

					Transaction transaction = new Transaction();
					transaction.msg = msg;
					if (msg.getMID() < 0) {
						msg.setMID(nextMessageID());
					}
					transaction.numRetransmit = 0;
					transaction.retransmitTask = null;
	
					fifoQueue.addLast(transaction);
	
					// schedule first retransmission ... NOT HERE BUT AT FLUSHING
				//	scheduleRetransmission(transaction);
					
					LOG.warning(String.format("Stored new transaction for %s", msg.key()));
	
					return transaction;
				}
			
			private synchronized void removeTransaction(Message msg) {

				Transaction transaction = inflightQueue.get(msg.key());
				
				// cancel any pending retransmission schedule
				transaction.retransmitTask.cancel();
				transaction.retransmitTask = null;
				
				// remove transaction from table
				inflightQueue.remove(transaction);
				flushQueue();
				LOG.finest(String.format("Cleared transaction for %s", transaction.msg.key()));
			}
			
			private int initialTimeout() {
				
				final double min = Properties.std.getDbl("RESPONSE_TIMEOUT");
				final double f = Properties.std.getDbl("RESPONSE_RANDOM_FACTOR");
				
				return (int) (min + (min * (f - 1d) * Math.random()));
			}
			
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
			
			public  int nextMessageID() {
	
				messageID = ++messageID % 0x10000;
	
				return messageID;
			}

			public void flushQueue() {
				// TODO Auto-generated method stub
				LOG.warning(String.format("FlushQueue Called, inflightQueueSize %d", inflightQueue.size()));
				while (inflightQueue.size() <= maxInflightQueueSize) {
					Transaction transaction = fifoQueue.pollFirst();
					if (transaction != null) {
						inflightQueue.put(transaction.msg.key(), transaction);
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

					// schedule next retransmisshttps://www.dropbox.com/sh/g3hjlw1f80v0ml2/AAC8EDB0Pv4bNodJBjBafSu7aion
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
	
	}
	
	
	
@Override
	protected void doSendMessage(Message msg) throws IOException {
	// TODO Auto-generated method stub
	LOG.warning(String.format("doSendMessage called for %s", msg.key()));
	if (msg.isConfirmable()) {
		String key = msg.getPeerAddress()+msg.getUriPath();
		Flow flow = flowTable.get(key);
		if (flow == null) {
			/*Create a new flow and do all the necessary initializations*/
			flow = new Flow(msg);
			flowTable.put(key, flow);
			LOG.warning(String.format("flow table created for %s", key));
		}	
		flow.addTransaction(msg);	
		flow.flushQueue();
		}
	else {
		sendMessageOverLowerLayer(msg);
		}
	}
	protected void sendCCMessage(Message msg) throws IOException {
	sendMessageOverLowerLayer(msg);
	}

@Override
	protected void doReceiveMessage(Message msg) {
	// TODO Auto-generated method stub
	LOG.warning(String.format("doReceiveMessage called for %s", msg.key()));
	if (msg.isAcknowledgement()) {
		String key = msg.getPeerAddress()+msg.getUriPath();
		Flow flow = flowTable.get(key); 
		LOG.warning(String.format("%s message is ACK ", msg.key()));
		if (flow != null) {
			flow.removeTransaction(msg);
			}
		}
	deliverMessage(msg);
  }
}