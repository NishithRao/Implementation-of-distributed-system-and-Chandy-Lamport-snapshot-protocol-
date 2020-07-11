import java.io.*;
import java.net.*;
import java.util.*;


enum Color { RED,BLUE};
@SuppressWarnings("serial")
public class Lamport_Main implements Serializable  {
	static String outputFileName;
	int ID;
	int numOfNodes,minPerActive,maxPerActive,minSendDelay,snapshotDelay,maxNumber;
	int totalSentMessage = 0;
	boolean active=false;
	int[][] CreateMatrix;
	int[] vector;
	int[] neighbor;
	boolean blockApplication_Message = false;
	Color color = Color.BLUE;
	int logging=0;
	boolean initial = true;
	String configFileName;
	ArrayList<Node> nodes = new ArrayList<Node>();
	HashMap<Integer,Node> store_Nodes = new HashMap<Integer,Node>();
	HashMap<Integer,Socket> channel_Name = new HashMap<Integer,Socket>();
	HashMap<Integer,ObjectOutputStream> outputStreamData = new HashMap<Integer,ObjectOutputStream>();
	HashMap<Integer,ArrayList<Application_Message>> channelStates;
	HashMap<Integer,Boolean> receiving_Marker;
	HashMap<Integer, StateMessage> stateMessages;
	boolean[] GraphNode;
	StateMessage presentState;
	ArrayList<int[]> output = new ArrayList<int[]>();

	void initialize(Lamport_Main Lamport_Obj){
		Lamport_Obj.channelStates = new HashMap<Integer,ArrayList<Application_Message>>();
		Lamport_Obj.receiving_Marker = new HashMap<Integer,Boolean>();
		Lamport_Obj.stateMessages = new HashMap<Integer, StateMessage>();

		Set<Integer> keys = Lamport_Obj.channel_Name.keySet();
		for(Integer element : keys){
			ArrayList<Application_Message> arrList = new ArrayList<Application_Message>();
			Lamport_Obj.channelStates.put(element, arrList);
		}
		for(Integer e: Lamport_Obj.neighbor){
			Lamport_Obj.receiving_Marker.put(e,false);
		}
		Lamport_Obj.GraphNode = new boolean[Lamport_Obj.numOfNodes];
		Lamport_Obj.presentState = new StateMessage();
		Lamport_Obj.presentState.vector = new int[Lamport_Obj.numOfNodes];
	}


	public static void main(String[] args) throws IOException, InterruptedException {
		Lamport_Main Lamport_Obj = ParseConfig.ReadFile_Config(args[1]);
		Lamport_Obj.ID = Integer.parseInt(args[0]);
		int curNode = Lamport_Obj.ID;
		Lamport_Obj.configFileName = args[1];
		Lamport_Main.outputFileName = Lamport_Obj.configFileName.substring(0, Lamport_Obj.configFileName.lastIndexOf('.'));
		Converge_Cast.CreateSpanningTree(Lamport_Obj.CreateMatrix);
		for(int i=0;i<Lamport_Obj.nodes.size();i++){
			Lamport_Obj.store_Nodes.put(Lamport_Obj.nodes.get(i).nodeId, Lamport_Obj.nodes.get(i));
		}
		int serverPort = Lamport_Obj.nodes.get(Lamport_Obj.ID).port;
		ServerSocket listener = new ServerSocket(serverPort);
		Thread.sleep(10000);
		for(int i=0;i<Lamport_Obj.numOfNodes;i++){
			if(Lamport_Obj.CreateMatrix[curNode][i] == 1){
												String hostName = Lamport_Obj.store_Nodes.get(i).host;
				int port = Lamport_Obj.store_Nodes.get(i).port;
												InetAddress address = InetAddress.getByName(hostName);
												Socket client = new Socket(address,port);
				Lamport_Obj.channel_Name.put(i, client);
				ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
				Lamport_Obj.outputStreamData.put(i, oos);
			}
		}

		Set<Integer> keys = Lamport_Obj.channel_Name.keySet();
		Lamport_Obj.neighbor = new int[keys.size()];
		int index = 0;
		for(Integer element : keys) Lamport_Obj.neighbor[index++] = element.intValue();
		Lamport_Obj.vector = new int[Lamport_Obj.numOfNodes];

		Lamport_Obj.initialize(Lamport_Obj);

		if(curNode == 0){
			Lamport_Obj.active = true;
			new CLThread(Lamport_Obj).start();
			new EmitMessagesThread(Lamport_Obj).start();
		}
		try {
			while (true) {
				Socket socket = listener.accept();
				new Client_Thread(socket,Lamport_Obj).start();
			}
		}
		finally {
			listener.close();
		}
	}


	void emitMessages() throws InterruptedException{
		int totalMessages = 1;
		int minSendDelay = 0;
		synchronized(this){
			totalMessages = this.generateRandomNumber(this.minPerActive,this.maxPerActive);
			if(totalMessages == 0){
				totalMessages = this.generateRandomNumber(this.minPerActive + 1,this.maxPerActive);
			}
			minSendDelay = this.minSendDelay;
		}
		for(int i=0;i<totalMessages;i++){
			synchronized(this){
				int IndexNeighbor = this.generateRandomNumber(0,this.neighbor.length-1);
				int curNeighbor = this.neighbor[IndexNeighbor];
				if(this.active == true){
					Application_Message m = new Application_Message();
					this.vector[this.ID]++;
					m.vector = new int[this.vector.length];
					System.arraycopy( this.vector, 0, m.vector, 0, this.vector.length );
					m.nodeId = this.ID;
					try {
						ObjectOutputStream oos = this.outputStreamData.get(curNeighbor);
						oos.writeObject(m);	
						oos.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}	
					totalSentMessage++;
				}
			}
			try {
				Thread.sleep(minSendDelay);
			} catch (InterruptedException e) {
				System.out.println("Error in EmitMessages");
			}
		}
		synchronized(this){
			this.active = false;
		}

	}

	int generateRandomNumber(int min, int max){
		Random random = new Random();
		int randomNum = random.nextInt((max - min) + 1) + min;
		return randomNum;
	}
}
class Client_Thread extends Thread {
	Socket cSocket;
	Lamport_Main Lamport_Obj;

	public Client_Thread(Socket csocket, Lamport_Main Lamport_Obj) {
		this.cSocket = csocket;
		this.Lamport_Obj = Lamport_Obj;
	}

	public void run() {
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(cSocket.getInputStream());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		while(true){
			try {
				Message msg;
				msg = (Message) ois.readObject();
				synchronized(Lamport_Obj){

					if(msg instanceof marker_Message){
						int channelNo = ((marker_Message) msg).n_ID;
						CLProtocol.sending_Marker(Lamport_Obj,channelNo);
					}	

					else if((Lamport_Obj.active == false) && msg instanceof Application_Message &&
							Lamport_Obj.totalSentMessage < Lamport_Obj.maxNumber && Lamport_Obj.logging == 0){
						Lamport_Obj.active = true;
						new EmitMessagesThread(Lamport_Obj).start();
					}
					else if((Lamport_Obj.active == false) && (msg instanceof Application_Message) && (Lamport_Obj.logging == 1)){
						int channelNo = ((Application_Message) msg).nodeId;
						CLProtocol.logging_Messages(channelNo,((Application_Message) msg) , Lamport_Obj);
					}

					else if(msg instanceof StateMessage){
						if(Lamport_Obj.ID == 0){
							Lamport_Obj.stateMessages.put(((StateMessage)msg).nodeId,((StateMessage)msg));
							Lamport_Obj.GraphNode[((StateMessage) msg).nodeId] = true;
							if(Lamport_Obj.stateMessages.size() == Lamport_Obj.numOfNodes){
								boolean restartChandy = CLProtocol.processState(Lamport_Obj,((StateMessage)msg));
								if(restartChandy){
									Lamport_Obj.initialize(Lamport_Obj);
									new CLThread(Lamport_Obj).start();
								}								
							}
						}
						else{
							CLProtocol.sendToParent(Lamport_Obj,((StateMessage)msg));
						}
					}
					else if(msg instanceof Finish_Message){
						CLProtocol.FinalMessage(Lamport_Obj);
					}

					if(msg instanceof Application_Message){
						for(int i = 0; i< Lamport_Obj.numOfNodes; i++){
							Lamport_Obj.vector[i] = Math.max(Lamport_Obj.vector[i], ((Application_Message) msg).vector[i]);
						}
						Lamport_Obj.vector[Lamport_Obj.ID]++;
					}
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

class CLThread extends Thread{

	Lamport_Main mainObj;
	public CLThread(Lamport_Main mainObj){
		this.mainObj = mainObj;
	}
	public void run(){
		if(mainObj.initial){
			mainObj.initial = false;
		}
		else{
			try {
				Thread.sleep(mainObj.snapshotDelay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		CLProtocol.CreateSnapshot(mainObj);
	}
}

class EmitMessagesThread extends Thread{

	Lamport_Main mainObj;
	public EmitMessagesThread(Lamport_Main mainObj){
		this.mainObj = mainObj;
	}
	public void run(){
		try {
			mainObj.emitMessages();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}


class Node {
	int nodeId;
	String host;
	int port;
	public Node(int nodeId, String host, int port) {
		super();
		this.nodeId = nodeId;
		this.host = host;
		this.port = port;
	}
}