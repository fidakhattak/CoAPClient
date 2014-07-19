package ch.ethz.inf.vs.californium.examples;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.Date;
import java.util.Calendar;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.ResponseHandler;
import ch.ethz.inf.vs.californium.coap.TokenManager;

public class CoAPPollTask extends TimerTask {

	Timer timer = new Timer(true);
	private String URI, Payload, fileName;
	private boolean CON;
	private static pollTaskStats stats;
    private ManagePollInterface callback;
	/*Statistics to be kept  */
    static int wait_counter; /*max set to 16 secs currently */


	CoAPPollTask(String uri, String payload, int freq, int totalPolReq, int pollTime, boolean con, String fileName) {	
			initialize(uri, payload, freq, totalPolReq, pollTime, con, fileName);
	}

	CoAPPollTask(String uri, String payload, int freq, int totalPolReq,int pollTime, ManagePollInterface callback, boolean con, String fileName) {	
		initialize(uri, payload, freq, totalPolReq, pollTime, con, fileName);
		this.callback = callback;
}

	private void initialize (String uri, String payload, int freq, int totalPolReq, int pollTime, boolean con, String filename)
	{
		URI = uri;
		Payload = payload;
		fileName = filename;
		System.out.println("Freq == " + freq);
		stats = new pollTaskStats(1, freq, totalPolReq, pollTime, con);
		timer.schedule(this, 0, 1000/freq);
		wait_counter = 0;
        fileName = filename;
		CON = con;		
	}

	public void run() {
		
		if (stats.getPolReqLeft() > 0) {
			GETRequest request = new GETRequest(CON);
			/*  Probably forces errors at the moment after retransmissions
			if (request.requiresToken()) {
				request.setToken(TokenManager.getInstance().acquireToken());
			}
			*/
			request.setURI(URI);
			request.setPayload(Payload);
			request.registerResponseHandler(new pollHandler());
			execute(request);
			stats.setPolReqLeft(stats.getPolReqLeft()-1);
			//			stats.setCurrentPolReq(stats.getCurrentPolReq()+1);
		}
	
		/*If there are still outstanding requests to this task, wait for a maximum
		 * of 32 seconds before saving the stats and exiting */
		
		else
		if (stats.getOutstandingRequests() > 0 && stats.getPolReqLeft() == 0 ) {
			while (wait_counter < 20 && stats.getOutstandingRequests() > 0) 
             {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				//	 TODO Auto-generated catch block
					e.printStackTrace();
				}
				wait_counter++;
			}
             try {
				exit();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 }
		
		else
		if(stats.getOutstandingRequests() == 0 && stats.getPolReqLeft() <= 0)
			try {
				exit();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	private void exit() throws IOException {
		saveStats();
		fileopen=0;
		this.callback.update(this);
	}
	

	private void execute(Request request) {
		try {
			request.execute();
		} catch (Exception ex) {
			ex.printStackTrace();
		}	
	}
	static int fileopen =0;	

	/* This function is called by the pollHandler to set the stats for this poll task */
	public static void setStats(Response response) throws IOException {
		/*
		String dirPath = "/home/fida/"; 
		String buffer = "";
		String temp = null;
		final BigInteger start_time;
		Matcher matcher;
		if (fileopen==0) {
			File f = new File(dirPath+response.getRequest().getUriPath());
			if (f.exists()) {
				if(f.delete()){
	    			System.out.println(f.getName() + " is deleted!");
	    		}
				else
				{
					System.out.println(f.getName() + " could not be deleted!");	
				}
			}
			FileWriter fstream = new FileWriter(dirPath+response.getRequest().getUriPath(),true);
			buffer+="Time,Current,Max,Min,Total\n";
			fileopen=1;
		}
	//  buffer+=System.currentTimeMillis()/1000;
		System.out.println("uri path is"+response.getRequest().getUriPath());
		FileWriter fstream = new FileWriter(dirPath+response.getRequest().getUriPath(),true);
		BufferedWriter out = new BufferedWriter(fstream);
		StringTokenizer st = new StringTokenizer(response.getPayloadString(),",");
		Pattern pattern = Pattern.compile("<(.*?)>");
		while (st.hasMoreElements()) {
			 temp = st.nextElement().toString();
			 matcher= pattern.matcher(temp);
			 if (matcher.find()) {
			  buffer+=matcher.group(1)+",";
			  System.out.println("temp is"+temp+" and buffer is"+buffer);
			 }
		}
		out.write(System.getProperty( "line.separator" )+buffer);
	    out.flush();
		out.close();
		*/
		stats.setCurrentPolRes(stats.getCurrentPolRes()+1);
		stats.setTotalRTT(stats.getTotalRTT()+response.getRTT());

		/* Total Retransmissions for this tasks. A single packet retransmitted multiple times is multiple retransmissions*/
		if (stats.CON) {
		stats.setTotRetransmissions(stats.getTotRetransmissions()+response.getRequest().getRetransmissioned());
		}
		stats.getOutstandingRequests();
		stats.setTotalPayloadSize(stats.getTotalPayloadSize()+response.getPayload().length);
	}	

	/* This function save stats in a file. It is called before calling the 
	 * manager.update() function in exit ()
	 *  */

	
	private void saveStats() throws IOException {
		String dirPath = "/home/fida/Stats/"; 
		FileWriter fstream = new FileWriter(dirPath+fileName,true);
		BufferedWriter out = new BufferedWriter(fstream);
		
		int completedReqs = this.stats.getTotalPollRequests()- this.stats.getOutstandingRequests();
		this.stats.setAverageRTT(this.stats.getTotalRTT()/completedReqs); 
		this.stats.setPollEndTime();
		this.stats.calculateGoodPut();

		out.write(System.getProperty( "line.separator" )+this.stats.toString());
	    out.flush();
		out.close();
	}

	public String getURI() {
		return URI;
	}

	public void setURI(String uRI) {
		URI = uRI;
	}
}

 /* A class to store the important stats that will be logged later */

class pollTaskStats {

	private  int task_number;
	private  double averageRTT;
	private  double totalRTT;
	private  int totRetransmissions;
	private  int totalPollRequests;
	private  int currentPolRes;
	private  int polReqLeft;
	private  int pps;
	private  int outstandingRequests;
	private long pollStartTime;
	private long pollEndTime;
	private int totalPayloadSize;
	private int goodPut;
	private int pollTime;
	boolean CON;
	
	public pollTaskStats(int task_number, int pps, int totalPollRequests,int pollTime, boolean con) {
		super();
		this.task_number = task_number;
		this.pps = pps;
		this.totalPollRequests = totalPollRequests;
		this.polReqLeft=totalPollRequests;
		this.CON = con;
		this.pollTime = pollTime;
     	this.currentPolRes = 0;
		this.averageRTT = 0;
		this.totalRTT = 0;
		this.totRetransmissions = 0;
		this.pollStartTime = Calendar.getInstance().getTimeInMillis();
		this.pollEndTime = 0;
		this.totalPayloadSize = 0;
		this.goodPut = 0;

	}	
	
	public int getTask_number() {
		return task_number;
	}

	public void setTask_number(int task_number) {
		this.task_number = task_number;
	}
	
	public long getPollStartTime() {
		return this.pollStartTime;
	}

	public void setPollStartTime(long pollStartTime) {
		this.pollStartTime = pollStartTime;
	}

	public long getPollEndTime() {
		return this.pollEndTime;
	}

	public void setPollEndTime() {
		this.pollEndTime = Calendar.getInstance().getTimeInMillis();
	}

	public int getCurrentPolRes() {
		return currentPolRes;
	}

	public void setCurrentPolRes(int currentPolRes) {
		this.currentPolRes = currentPolRes;
	}

	public double getAverageRTT() {
		return averageRTT;
	}

	public void setAverageRTT(double averageRTT) {
		this.averageRTT = averageRTT;
	}

	public double getTotalRTT() {
		return totalRTT;
	}

	public void setTotalRTT(double totalRTT) {
		this.totalRTT = totalRTT;
	}

	public int getTotRetransmissions() {
		return totRetransmissions;
	}

	public void setTotRetransmissions(int totRetransmissions) {
		this.totRetransmissions = totRetransmissions;
	}

	public int getTotalPollRequests() {
		return totalPollRequests;
	}

	public void setTotalPollRequests(int totalPollRequests) {
		this.totalPollRequests = totalPollRequests;
	}

	public int getPolReqLeft() {
		return polReqLeft;
	}

	public void setPolReqLeft(int polReqLeft) {
		this.polReqLeft = polReqLeft;
	}

	public int getPps() {
		return pps;
	}

	public void setPps(int pps) {
		this.pps = pps;
	}

	public int getOutstandingRequests() {
		this.outstandingRequests = (this.totalPollRequests - this.polReqLeft) - (this.currentPolRes);
		return this.outstandingRequests;
	}
	
	public int getTotalPayloadSize() {
		return this.totalPayloadSize;
	}

	public void setTotalPayloadSize(int payloadSize) {
		this.totalPayloadSize = payloadSize;
	}

	public void calculateGoodPut()
	{
		 this.goodPut = (int) (this.totalPayloadSize / ((this.pollEndTime - this.pollStartTime) / 1000));
	}
	
	@Override
	public String toString() {
		 
		  return Integer.toString(pollTime) + "\t"
				+ Integer.toString(pps) + "\t" 
				+ Double.toString(averageRTT)+ "\t"
				+ Integer.toString(totRetransmissions)+ "\t"
				+ Integer.toString(totalPollRequests) + "\t" 
				+ Integer.toString(outstandingRequests)+ "\t"
				+ Long.toString(pollEndTime -pollStartTime)+ "\t"
				+ Integer.toString(goodPut)+ "\t";
	}	
}


class pollHandler implements ResponseHandler {

	@Override
	public void handleResponse(Response response) {
		// TODO Auto-generated method stub
		try {
			CoAPPollTask.setStats(response);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

	}

}
