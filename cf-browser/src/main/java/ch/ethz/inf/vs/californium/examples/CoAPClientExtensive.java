/**
 * A Gui CoAP Client built for the testbed
 * 
 * @author Fida Khattak
 */

package ch.ethz.inf.vs.californium.examples;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import ch.ethz.inf.vs.californium.coap.*;


public class CoAPClientExtensive extends JPanel  {

 	private static final long serialVersionUID = -8656652459991661071L;
     
	private static final String DEFAULT_URI = "coap://aaaa::212:7401:1:101:5683";
	private static final String TESTSERVER_URI = "coap://vs0.inf.ethz.ch:5683";
	private static final String COAP_PROTOCOL = "coap://";

	//public static ArrayList<CoAPPollTask> polledServers = new ArrayList<CoAPPollTask>();
	private ArrayList<ManagePollTasks> pollManagers = new ArrayList<ManagePollTasks>();
	private JComboBox cboBR;
	private JComboBox cboServers;
	private JTextArea txaPayload;
	private JTextArea txaResponse;

	/* poll text fields for frequency and total requests */

	private JTextArea pollStartPpsText;
	private JTextArea pollEndPpsText;
	private JTextArea pollStepPpsText;
	private JTextArea pollTotReqText;
	private JTextArea totalTimeText;
	private JTextArea fileNameText;
	private JPanel pnlResponse;
	private TitledBorder responseBorder;

	private DefaultMutableTreeNode dmtRes;
	private DefaultTreeModel dtmRes;
	private JTree treRes;
	private JList observeList;
	DefaultListModel obsListModel;
	private JList pollList;
	DefaultListModel pollListModel;
	
	private boolean CON;

	public CoAPClientExtensive() {
		JButton btnGet = new JButton("GET");
		JButton btnPos = new JButton("POST");
		JButton btnPut = new JButton("PUT");
		JButton btnDel = new JButton("DELETE");
		JButton btnObs = new JButton("Observe");
		JButton btnRemoveObs = new JButton("Remove Observe");
		JButton btnAddPoll = new JButton("AddPoll");
		JButton btnRemovePoll = new JButton("Remove Poll");
		JButton btnStartPoll = new JButton("Start Poll");
		JButton btnDisc = new JButton("Discover Server");
		JButton btnFind = new JButton("Find Servers");
		final JCheckBox conBox = new JCheckBox("confirmable", true);

		
		btnGet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				performRequest(new GETRequest(CON));
			}
		});

		btnPos.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				performRequest(new POSTRequest());
			}
		});

		btnPut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				performRequest(new PUTRequest());
			}
		});

		btnDel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				performRequest(new DELETERequest());
			}
		});

		btnObs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				observe(new GETRequest());
			}

		});

		btnAddPoll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				poll();
			}

		});

		btnDisc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				discover();
			}

		});

		btnFind.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				findServers();
			}

		});

		btnRemoveObs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean success = removeObserve((String) observeList
						.getSelectedValue());
				obsListModel.removeElement(observeList.getSelectedValue());
			}

		});

		btnRemovePoll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				 removePoll((String) pollList
						.getSelectedValue());
				boolean success =pollListModel.removeElement(pollList.getSelectedValue());
				if (!success)
					System.out.println("Failed to remove "
							+ pollList.getSelectedValue().toString());
			}

		});

		btnStartPoll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				 startPoll();
			}

		});
		
		
		
		conBox.addActionListener(new ActionListener () {
		public void actionPerformed(ActionEvent e)
	        {
	        if(e.getSource()==conBox){ if(conBox.isSelected()){
	        	CON = true;
	        	System.out.println("Confirmable requests");
	            }
	        else {
	         {
	        	CON = false;
	        	System.out.println("NONConfirmable requests"); }
	        	}
	          }
	        }
		});

		/* Panel and list for observe initialization */

		/* Initialize the list,Button and Panel */
		obsListModel = new DefaultListModel();
		observeList = new JList(obsListModel);
		JPanel observeListPanel = new JPanel(new BorderLayout());
		observeListPanel.add(observeList, BorderLayout.CENTER);

		/* ScrollPane initialization */
		int v = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
		int h = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;

		JScrollPane scrollableObsPane = new JScrollPane(observeListPanel, v, h);
		scrollableObsPane.setBorder(new TitledBorder("Observed Resources"));
		scrollableObsPane.setPreferredSize(new Dimension(scrollableObsPane
				.getPreferredSize().width, 50));
		JPanel observePanel = new JPanel(new BorderLayout());
		observePanel.add(scrollableObsPane, BorderLayout.CENTER);
		btnRemoveObs.setPreferredSize(new Dimension(150, 4));
		observePanel.add(btnRemoveObs, BorderLayout.EAST);
		/* End of observe Panel */

		/* Panel and list for poll initialization */

		/* Initialize the list,Button and Panel */
		pollListModel = new DefaultListModel();
		pollList = new JList(pollListModel);
		JPanel pollListPanel = new JPanel(new BorderLayout());
		pollListPanel.add(pollList, BorderLayout.CENTER);

		JScrollPane scrollablePollPane = new JScrollPane(pollListPanel, v, h);
		scrollablePollPane.setBorder(new TitledBorder("Polled Resources"));
		scrollablePollPane.setPreferredSize(new Dimension(scrollableObsPane
				.getPreferredSize().width, 50));
		JPanel pollPanel = new JPanel(new BorderLayout());
		pollPanel.add(scrollablePollPane, BorderLayout.CENTER);
		pollPanel.add(btnRemovePoll, BorderLayout.EAST);
		pollPanel.add(btnStartPoll, BorderLayout.WEST);
		/* End of observe Panel */

		/* Combine observe and poll panels */

		JPanel obsPollPanel = new JPanel(new BorderLayout());
		obsPollPanel.add(observePanel, BorderLayout.NORTH);
		obsPollPanel.add(pollPanel, BorderLayout.SOUTH);

		cboServers = new JComboBox();
		cboServers.setEditable(true);
		cboServers.setMinimumSize(cboServers.getPreferredSize());
		cboServers.addItem(DEFAULT_URI);
		cboServers.addItem(TESTSERVER_URI);
		cboServers.setSelectedIndex(0);

		cboBR = new JComboBox();
		cboBR.setEditable(true);
		cboBR.setMinimumSize(cboBR.getPreferredSize());
		cboBR.addItem("Enter Border Router Address");
		cboBR.addItem("[aaaa::250:c2a8:c46b:41ec]");
		cboBR.setSelectedIndex(0);

		txaPayload = new JTextArea("", 8, 50);
		txaResponse = new JTextArea("", 8, 50);
		txaResponse.setEditable(false);

		pollStartPpsText = new JTextArea("Enter Initial PPS", 1, 2);
		pollEndPpsText = new JTextArea("Enter Final PPS", 1, 2);
		pollStepPpsText = new JTextArea("Enter PPS Step", 1, 2);
		pollTotReqText = new JTextArea("Enter Total Poll Requests", 1, 3);
		totalTimeText = new JTextArea("Total Run Time", 1, 3);
		fileNameText = new JTextArea("File name", 1, 3);
		JPanel pnlBR = new JPanel(new BorderLayout());
		pnlBR.add(cboBR, BorderLayout.CENTER);
		pnlBR.add(btnFind, BorderLayout.EAST);

		JPanel pnlDisc = new JPanel(new BorderLayout());
		pnlDisc.add(cboServers, BorderLayout.CENTER);
		pnlDisc.add(btnDisc, BorderLayout.EAST);

		JPanel pnlUpper = new JPanel(new BorderLayout());
		pnlUpper.setBorder(new TitledBorder(
				"CoAP Server and Resource Discovery"));
		pnlUpper.add(pnlBR, BorderLayout.NORTH);
		pnlUpper.add(pnlDisc, BorderLayout.SOUTH);
		pnlUpper.setMaximumSize(new Dimension(Integer.MAX_VALUE, pnlUpper
				.getPreferredSize().height));

		/* panel for upper buttons */
		JPanel pnlButtonsUpper = new JPanel(new GridLayout(1, 4, 10, 10));
		pnlButtonsUpper.setBorder(new EmptyBorder(10, 10, 10, 10));
		pnlButtonsUpper.add(conBox);
		pnlButtonsUpper.add(btnGet);
		pnlButtonsUpper.add(btnPos);
		pnlButtonsUpper.add(btnPut);
		pnlButtonsUpper.add(btnDel);
		pnlButtonsUpper.add(btnObs);

		/* panel for lower buttons (poll and text fields) */

		JPanel pnlButtonsLower = new JPanel(new GridLayout(1, 5, 2, 2));
		pnlButtonsLower.setBorder(new EmptyBorder(1, 1, 1, 1));
		pnlButtonsLower.add(btnAddPoll);
		pnlButtonsLower.add(pollStartPpsText);
		pnlButtonsLower.add(pollEndPpsText);
		pnlButtonsLower.add(pollStepPpsText);
		pnlButtonsLower.add(pollTotReqText);
		pnlButtonsLower.add(totalTimeText);
		pnlButtonsLower.add(fileNameText);
		JPanel pnlButtons = new JPanel(new BorderLayout());
		pnlButtons.setBorder(new TitledBorder("Actions"));
		pnlButtons.add(pnlButtonsUpper, BorderLayout.NORTH);
		pnlButtons.add(pnlButtonsLower, BorderLayout.SOUTH);

		JPanel pnlRequest = new JPanel(new BorderLayout());
		pnlRequest.setBorder(new TitledBorder("Request"));
		pnlRequest.add(new JScrollPane(txaPayload), BorderLayout.CENTER);
		pnlRequest.add(pnlButtons, BorderLayout.SOUTH);

		pnlResponse = new JPanel(new BorderLayout());
		responseBorder = new TitledBorder("Response");
		pnlResponse.setBorder(responseBorder);
		pnlResponse.add(new JScrollPane(txaResponse), BorderLayout.CENTER);
		pnlResponse.add(obsPollPanel, BorderLayout.SOUTH);

		JPanel panelC = new JPanel();
		panelC.setLayout(new BoxLayout(panelC, BoxLayout.Y_AXIS));

		panelC.add(pnlUpper);
		panelC.add(pnlRequest);

		JSplitPane splReqRes = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splReqRes.setContinuousLayout(true);
		splReqRes.setResizeWeight(.5f);
		splReqRes.setTopComponent(panelC);
		splReqRes.setBottomComponent(pnlResponse);

		dmtRes = new DefaultMutableTreeNode("Resources");
		dtmRes = new DefaultTreeModel(dmtRes);
		treRes = new JTree(dtmRes);

		JScrollPane scrRes = new JScrollPane(treRes);
		scrRes.setPreferredSize(new Dimension(200,
				scrRes.getPreferredSize().height));

		JPanel panelE = new JPanel(new BorderLayout());
		panelE.setBorder(new TitledBorder("Resources"));
		panelE.add(scrRes, BorderLayout.CENTER);

		setLayout(new BorderLayout());

		JSplitPane splCE = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splCE.setContinuousLayout(true);
		splCE.setResizeWeight(.5f);
		splCE.setLeftComponent(panelE);
		splCE.setRightComponent(splReqRes);
		add(splCE);

		treRes.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				TreePath tp = e.getNewLeadSelectionPath();
				if (tp != null) {
					Object[] nodes = tp.getPath();
					StringBuffer sb = new StringBuffer(COAP_PROTOCOL
							+ getHost());
					for (int i = 1; i < nodes.length; i++)
						// nodes[0] is Resource and not necessary
						sb.append("/" + nodes[i].toString());
					cboServers.setSelectedItem(sb.toString());
				}
			}
		});

		// discover();
	}

	private void findServers() {
		String Address = (String) cboBR.getSelectedItem();
		ArrayList<String> BrResponse = new ArrayList<String>();
		URL myURL = null;
		try {
			myURL = new URL("HTTP", Address, 80, "/index.html");
		} catch (MalformedURLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(myURL.openStream()));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String inputLine = null;
		String start = "</pre>Routes<pre>";
		boolean routes = false;
		try {
			while ((inputLine = in.readLine()) != null) {
				if (inputLine.contains(start)) {
					routes = true;
					inputLine = inputLine.substring(start.length());
				}
				if (routes) {
					if (inputLine.contains("/128")) {
						inputLine = inputLine.substring(0,
								inputLine.indexOf("/128"));
						BrResponse.add("coap://[" + inputLine + "]");
					}
				}
			}
			System.out.println(BrResponse);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// cboServers.setModel(new DefaultComboBoxModel(BrResponse.length()));
		cboServers.setModel(new DefaultComboBoxModel(BrResponse
				.toArray(new String[BrResponse.size()])));
		cboServers.addItem(BrResponse.toString());
		return;
	}

	private void discover() {
		dmtRes.removeAllChildren();
		dtmRes.reload();

		Request request = new GETRequest();
		request.setURI(COAP_PROTOCOL + getHost() + "/.well-known/core");
		request.registerResponseHandler(new ResponseHandler() {
			public void handleResponse(Response response) {
				String text = response.getPayloadString();
				Scanner scanner = new Scanner(text);
				Pattern pattern = Pattern.compile("<");
				scanner.useDelimiter(pattern);

				ArrayList<String> ress1 = new ArrayList<String>();
				ArrayList<String> ress2 = new ArrayList<String>();
				while (scanner.hasNext()) {
					String part = scanner.next();
					String res = part.split(">")[0];
					ress1.add(COAP_PROTOCOL + getHost() + res);
					ress2.add(res);
				}
				// cboServers.setModel(new
				// DefaultComboBoxModel(ress1.toArray(new
				// String[ress1.size()])));
				populateTree(ress2);
			}
		});
		execute(request);
	}

	private void populateTree(List<String> ress) {
		Node root = new Node("Resource");
		for (String res : ress) {
			String[] parts = res.split("/");
			Node cur = root;
			for (int i = 1; i < parts.length; i++) {
				Node n = cur.get(parts[i]);
				if (n == null)
					cur.children.add(n = new Node(parts[i]));
				cur = n;
			}
		}
		dmtRes.removeAllChildren();
		addNodes(dmtRes, root);
		dtmRes.reload();
		for (int i = 0; i < treRes.getRowCount(); i++) {
			treRes.expandRow(i);
		}
	}

	private void addNodes(DefaultMutableTreeNode parent, Node node) {
		for (Node n : node.children) {
			DefaultMutableTreeNode dmt = new DefaultMutableTreeNode(n.name);
			parent.add(dmt);
			addNodes(dmt, n);
		}
	}

	private class Node {
		private String name;
		private ArrayList<Node> children = new ArrayList<Node>();

		private Node(String name) {
			this.name = name;
		}

		private Node get(String name) {
			for (Node c : children)
				if (name.equals(c.name))
					return c;
			return null;
		}
	}

	public static class MyPostRequest extends POSTRequest {
	}

	private void performRequest(Request request) {
		txaResponse.setText("no response yet");
		responseBorder.setTitle("Response: none");
		pnlResponse.repaint();
		request.registerResponseHandler(new ResponsePrinter());
		request.setPayload(txaPayload.getText());
		request.setURI(cboServers.getSelectedItem().toString()
				.replace(" ", "%20"));
		execute(request);
	}

	/* Poll related functions */

	private void poll() {
		txaResponse.setText("no response yet");
		responseBorder.setTitle("Response: none");
		pnlResponse.repaint();

		String uri = cboServers.getSelectedItem().toString()
				.replace(" ", "%20");
		String payload = txaPayload.getText();
		float startPps, endPps, step;
		int totalPollReq,totalTime;
		String fileName;
		startPps = Float.parseFloat(pollStartPpsText.getText());
		endPps = Float.parseFloat(pollEndPpsText.getText());
		step = Float.parseFloat(pollStepPpsText.getText());
		totalPollReq = Integer.parseInt(pollTotReqText.getText());
		totalTime = Integer.parseInt(totalTimeText.getText());
        fileName = fileNameText.getText();
		Iterator iterator;

		ManagePollTasks element;

		iterator = pollManagers.iterator();
		while (iterator.hasNext()) {
			element = (ManagePollTasks) iterator.next();
			if (element.getURI().equals(uri)) {
				System.out.println("URI " + uri
						+ " already exists in poll list");
				return;
			}
		}

		if (totalPollReq == 0 && totalTime == 0) {
			System.out.println("Enter total Poll Requeste or Time");
			return;
		}
		pollListModel.addElement(uri);
		element = new ManagePollTasks(startPps, endPps,
				step, totalPollReq,totalTime, uri, payload, CON, fileName);
		pollManagers.add(element); 
		System.out.println("pollManager Size when adding "+element.getURI() + " " + pollManagers.size());
		
	}

	public void startPoll() {
	//	Iterator iterator1;
		ManagePollTasks element;
	//iterator1 = pollManagers.iterator();
		int i = 0;
		while (i < pollManagers.size()) {
//			element = (ManagePollTasks) iterator1.next();
			element = pollManagers.get(i);
			System.out.println("In startPoll Function, pollManagersSize = " +pollManagers.size()+ "iteration = " +i);
			element.pollManagerStart();
			System.out.println(element.getURI()+"Started");
			i++;
		}
	}
	
	
	private boolean removePoll(String uri) {

		Iterator iterator;
		ManagePollTasks element;
		iterator = pollManagers.iterator();
		while (iterator.hasNext()) {
			element = (ManagePollTasks) iterator.next();
			System.out.println(""+element.getURI());
			if (element.getURI().equals(uri)) {
				System.out.println("Found poll element" +element.getURI());
				element.pollManagerStop();
				pollManagers.remove(element);
				return true;
			}
			else 
				System.out.println("Iterator couldn't find the element");
		}
		System.out.println("Iterator has no next");
		return false;
	}

	
	/* Handler function for observe requests */

	/* Observe related functions */

	private void observe(Request request) {
		txaResponse.setText("no response yet");
		responseBorder.setTitle("Response: none");
		pnlResponse.repaint();
		if (request.requiresToken()) {
			request.setToken(TokenManager.getInstance().acquireToken());
		}
		request.enableResponseQueue(true);
		request.setOption(new Option(0, OptionNumberRegistry.OBSERVE));
		request.registerResponseHandler(new ObserveHandler());
		request.setPayload(txaPayload.getText());
		request.setURI(cboServers.getSelectedItem().toString()
				.replace(" ", "%20"));
		execute(request);
		obsListModel
		.addElement(request.getPeerAddress() + request.getUriPath());
	}

	/* Handler function for observe requests */

	private class ObserveHandler implements ResponseHandler {
		public void handleResponse(Response response) {
			boolean success = true;
			success &= checkResponse(response.getRequest(), response);
			if (success) {
				txaResponse.setText(response.getPayloadString());
				responseBorder.setTitle("Response: "
						+ CodeRegistry.toString(response.getCode()));
				pnlResponse.repaint();
			} else {
				removeObserve(response.getRequest().getPeerAddress()
						+ response.getRequest().getUriPath());
				obsListModel.removeElement(response.getRequest()
						.getPeerAddress() + response.getRequest().getUriPath());
			}
		}
	}

	private boolean removeObserve(String URL) {
		System.out.println(URL);
		boolean success = false;
		Request request = new GETRequest();

		request.setURI("coap://" + URL);
		request.removeOptions(OptionNumberRegistry.OBSERVE);
		request.setMID(-1);
		request.enableResponseQueue(true);
		try {
			request.execute();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			Response response = request.receiveResponse();
			success &= hasObserve(response, true);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return success;
	}

	/**
	 * Checks for Observe option.
	 * 
	 * @param response
	 *            the response
	 * @return true, if successful
	 */

	protected boolean hasObserve(Response response, boolean invert) {
		boolean success = response.hasOption(OptionNumberRegistry.OBSERVE);

		// invert to check for not having the option
		success ^= invert;

		if (!success) {
			System.out.println("FAIL: Response without Observe");
		} else if (!invert) {
			System.out.printf("PASS: Observe (%d)\n",
					response.getFirstOption(OptionNumberRegistry.OBSERVE)
					.getIntValue());
		} else {
			System.out.println("PASS: No Observe");
		}

		return success;
	}

	protected boolean hasObserve(Response response) {
		return hasObserve(response, false);
	}

	protected boolean hasContentType(Response response) {
		boolean success = response.hasOption(OptionNumberRegistry.CONTENT_TYPE);

		if (!success) {
			System.out.println("FAIL: Response without Content-Type");
		} else {
			System.out.printf("PASS: Content-Type (%s)\n",
					MediaTypeRegistry.toString(response.getContentType()));
		}

		return success;
	}

	protected boolean checkInt(int expected, int actual, String fieldName) {
		boolean success = expected == actual;

		if (!success) {
			System.out.println("FAIL: Expected " + fieldName + ": " + expected
					+ ", but was: " + actual);
		} else {
			System.out.println("PASS: Correct " + fieldName
					+ String.format(" (%d)", actual));
		}

		return success;
	}

	protected boolean checkResponse(Request request, Response response) {
		final int EXPECTED_RESPONSE_CODE = 69;
		boolean success = true;

		success &= checkInt(EXPECTED_RESPONSE_CODE, response.getCode(), "code");
		success &= hasObserve(response);
		// success &= hasContentType(response);
		return success;
	}

	private void execute(Request request) {
		try {
			request.execute();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private class ResponsePrinter implements ResponseHandler {
		public void handleResponse(Response response) {
			txaResponse.setText(response.getPayloadString());
			responseBorder.setTitle("Response: "
					+ CodeRegistry.toString(response.getCode()));
			pnlResponse.repaint();
		}
	}

	public static void main(String[] args) {
		setLookAndFeel();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new JFrame("CoAP Client");
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.add(new CoAPClientExtensive());
				frame.pack();
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
			}
		});
	}

	private static void setLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager
					.getCrossPlatformLookAndFeelClassName());
			// UIManager.getSystemLookAndFeelClassName());
			// "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getHost() {
		String uri = (String) cboServers.getSelectedItem();
		StringTokenizer st = new StringTokenizer(uri, "/");
		st.nextToken();
		String host = st.nextToken();
		return host;
	}
}


class PollManagerParameters {
	float startPps, endPps, step;
	int totalPollReq, totalTime; 
	String uri, fileName, payload;
	boolean CON;
	 public PollManagerParameters (float startPps, float endPps, float step, int totalPollReq,
			 	int totalTime, String uri, String fileName, String payload, boolean con) 	{
		 this.uri = uri;
		 this.payload = payload;
		 this.fileName = fileName;
		 this.startPps = startPps;
		 this.endPps = endPps;
		 this.step = step;
		 this.totalPollReq = totalPollReq;
		 this.totalTime = totalTime;
		 this.CON = con;
	 }
}
