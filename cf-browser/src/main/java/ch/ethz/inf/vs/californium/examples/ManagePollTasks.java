package ch.ethz.inf.vs.californium.examples;

import java.util.Iterator;

/* Poll Manager class. An object of this class is created everytime a new poll task
 * is assigned to the CoAP Client. This manager takes care of the polling tasks
 * in loop. Once all the tasks are completed, it returns. 
 */

public class ManagePollTasks implements ManagePollInterface{
	private static int startPps;
	private static int endPps;
	private static int step;
	private int totalPollRequests,pollTime;
	private static String uri, payload, fileName;
	private boolean CON;
	
	public ManagePollTasks(int startPps, int endPps, int step,
			int totalPollRequests,int pollTime, String uri, String payload, boolean con, String fileName) {
		super();
		this.startPps = startPps;
		this.endPps = endPps;
		this.step = step;
		this.totalPollRequests = totalPollRequests;
		this.pollTime=pollTime;
		this.uri = uri;
		this.payload = payload;
		this.fileName = fileName;
		this.CON = con; 
		if (startPps <= endPps)
			newPollTask();
	}

	
	@Override
	public void update(CoAPPollTask task) {
		// TODO Auto-generated method stub
		
		System.out.println("Update called");
		removePoll(task);
		task.cancel();
		System.out.println("startPps =" +this.startPps +"endPps = " + this.endPps + "step = " + this.step);
		if (this.startPps <= this.endPps)
		newPollTask();
		else
			return;			
	}
	
	private void newPollTask() {
		int requests=0;
		int time=0;
		
		if (pollTime !=0) {
			requests = pollTime * startPps;
		    time = pollTime;
		}
		else 
		if (pollTime ==0 && totalPollRequests !=0) {
			time = totalPollRequests/startPps;
		    requests = totalPollRequests;
		}
			
			CoAPPollTask pollTask = new CoAPPollTask(uri, payload, startPps,
				requests, time, this, CON, fileName);
			CoAPClientExtensive.polledServers.add(pollTask);
			System.out.println("startPps = " +this.startPps + "step = " + step);
			this.startPps += this.step;
}
	
	private static void removePoll(CoAPPollTask task) {
        CoAPClientExtensive.removePoll(task.getURI());
		
		/*
		Iterator iterator;
		CoAPPollTask element;
		iterator = CoAPClient.polledServers.iterator();
		while (iterator.hasNext()) {
			element = (CoAPPollTask) iterator.next();
			if (element.equals(task)) {
				element.cancel();
				CoAPClient.polledServers.remove(element);
			}
		}
		return;
	}
	*/
}
}
