package ch.ethz.inf.vs.californium.examples;

import java.util.ArrayList;
import java.util.Iterator;

/* Poll Manager class. An object of this class is created everytime a new poll task
 * is assigned to the CoAP Client. This manager takes care of the polling tasks
 * in loop. Once all the tasks are completed, it returns. 
 */

public class ManagePollTasks implements ManagePollInterface{
	private int startPps;
	private int endPps;
	private int step;
	private int totalPollRequests,pollTime;
	private String uri, payload, fileName;
	private boolean CON;
	//public static ArrayList<CoAPPollTask> polledServers = new ArrayList<CoAPPollTask>();
	private CoAPPollTask coapPollTask;
	
	
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
		System.out.println("New Manager created for " + this.uri);
	}

	public void pollManagerStart() {
		if (this.startPps <= this.endPps)
			newPollTask();		
	}
	public void pollManagerStop() {
		this.coapPollTask.cancel();		
	}
	
	@Override
	public void update(CoAPPollTask task) {
		// TODO Auto-generated method stub
		
		System.out.println("Update called");
		task.cancel();
		System.out.println("startPps =" +this.startPps +"endPps = " + this.endPps + "step = " + this.step);
		if (this.startPps <= this.endPps)
		newPollTask();
		else
			System.out.println(task.getURI()+" Task finished");
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
			
			coapPollTask = new CoAPPollTask(this.uri, this.payload, this.startPps,
				requests, time, this, this.CON, this.fileName);
			System.out.println("Task created for" + this.uri);
			System.out.println("startPps = " +this.startPps + "step = " + step);
			this.startPps += this.step;
}
	/*
	
	private static void removePoll(CoAPPollTask task) {
        removePoll(task.getURI());
		Iterator iterator;
		CoAPPollTask element;
		iterator = polledServers.iterator();
		while (iterator.hasNext()) {
			element = (CoAPPollTask) iterator.next();
			if (element.equals(task)) {
				element.cancel();
				polledServers.remove(element);
			}
		}
		return;
	}
*/
	public String getURI() {
		// TODO Auto-generated method stub
		return this.uri;
	}
}
