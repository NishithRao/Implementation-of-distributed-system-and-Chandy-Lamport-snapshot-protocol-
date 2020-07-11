import java.io.*;
import java.util.*;

public class CLProtocol {
	public static void CreateSnapshot(Lamport_Main SnapshotObject) {
		synchronized(SnapshotObject){
			SnapshotObject.GraphNode[SnapshotObject.ID] = true;
			sending_Marker(SnapshotObject,SnapshotObject.ID);
		}
	}

	public static void sending_Marker(Lamport_Main SnapshotObject, int channel_ID){
		synchronized(SnapshotObject){
			if(SnapshotObject.color == Color.BLUE){
				SnapshotObject.receiving_Marker.put(channel_ID, true);
				SnapshotObject.color = Color.RED;
				SnapshotObject.presentState.active = SnapshotObject.active;
				SnapshotObject.presentState.vector = SnapshotObject.vector;
				SnapshotObject.presentState.nodeId = SnapshotObject.ID;
				int[] vector = new int[SnapshotObject.presentState.vector.length];
				for(int i=0;i<vector.length;i++){
					vector[i] = SnapshotObject.presentState.vector[i];
				}
				SnapshotObject.output.add(vector);
				SnapshotObject.logging = 1;
				for(int i : SnapshotObject.neighbor){
					marker_Message m = new marker_Message();
					m.n_ID = SnapshotObject.ID;
					ObjectOutputStream Output_Stream = SnapshotObject.outputStreamData.get(i);
					try {
						Output_Stream.writeObject(m);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if((SnapshotObject.neighbor.length == 1) && (SnapshotObject.ID !=0)){
					int parent_Object = Converge_Cast.getParent(SnapshotObject.ID);
					SnapshotObject.presentState.channel_states = SnapshotObject.channelStates;
					SnapshotObject.color = Color.BLUE;
					SnapshotObject.logging = 0;
					ObjectOutputStream output_Stream = SnapshotObject.outputStreamData.get(parent_Object);
					try {
						output_Stream.writeObject(SnapshotObject.presentState);
					} catch (IOException e) {
						e.printStackTrace();
					}
					SnapshotObject.initialize(SnapshotObject);
				}


			}
			else if(SnapshotObject.color == Color.RED){
				SnapshotObject.receiving_Marker.put(channel_ID, true);
				int i=0;
				while(i<SnapshotObject.neighbor.length && SnapshotObject.receiving_Marker.get(SnapshotObject.neighbor[i]) == true){
					i++;
				}
				if(i == SnapshotObject.neighbor.length && SnapshotObject.ID != 0){
					int parent = Converge_Cast.getParent(SnapshotObject.ID);
					SnapshotObject.presentState.channel_states = SnapshotObject.channelStates;
					SnapshotObject.color = Color.BLUE;
					SnapshotObject.logging = 0;
					ObjectOutputStream Output_Stream = SnapshotObject.outputStreamData.get(parent);
					try {
						Output_Stream.writeObject(SnapshotObject.presentState);
					} catch (IOException e) {
						e.printStackTrace();
					}
					SnapshotObject.initialize(SnapshotObject);
				}
				if(i == SnapshotObject.neighbor.length &&  SnapshotObject.ID == 0){
					SnapshotObject.presentState.channel_states = SnapshotObject.channelStates;
					SnapshotObject.stateMessages.put(SnapshotObject.ID, SnapshotObject.presentState);
					SnapshotObject.color = Color.BLUE;
					SnapshotObject.logging = 0;
				}
			}
		}
	}

	public static boolean processState(Lamport_Main Lamport_Obj, StateMessage msg) throws InterruptedException {
		int i=0,j=0,k=0;
		synchronized(Lamport_Obj){
			while(i<Lamport_Obj.GraphNode.length && Lamport_Obj.GraphNode[i] == true){
				i++;
			}
			if(i == Lamport_Obj.GraphNode.length){
				for(j=0;j<Lamport_Obj.stateMessages.size();j++){
					if(Lamport_Obj.stateMessages.get(j).active == true){
						return true;
					}
				}
				if(j == Lamport_Obj.numOfNodes){
					for(k=0;k<Lamport_Obj.numOfNodes;k++){
						StateMessage value = Lamport_Obj.stateMessages.get(k);
						for(ArrayList<Application_Message> g:value.channel_states.values()){
							if(!g.isEmpty()){
								return true;
							}
						}
					}
				}
				if(k == Lamport_Obj.numOfNodes){
					FinalMessage(Lamport_Obj);
					return false;
				}
			}
		}
		return false;
	}


	public static void logging_Messages(int channel_ID, Application_Message m, Lamport_Main Lamport_Obj) {
		synchronized(Lamport_Obj){
			if(!(Lamport_Obj.channelStates.get(channel_ID).isEmpty()) && Lamport_Obj.receiving_Marker.get(channel_ID) != true){
				Lamport_Obj.channelStates.get(channel_ID).add(m);
			}
			else if((Lamport_Obj.channelStates.get(channel_ID).isEmpty()) && Lamport_Obj.receiving_Marker.get(channel_ID) != true){
				ArrayList<Application_Message> Messages = Lamport_Obj.channelStates.get(channel_ID);
				Messages.add(m);
				Lamport_Obj.channelStates.put(channel_ID, Messages);
			}
		}
	}

	public static void sendToParent(Lamport_Main Lamport_Obj, StateMessage stateMessage) {
		synchronized(Lamport_Obj){
			int parent = Converge_Cast.getParent(Lamport_Obj.ID);
			ObjectOutputStream Output_Stream = Lamport_Obj.outputStreamData.get(parent);
			try {
				Output_Stream.writeObject(stateMessage);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void FinalMessage(Lamport_Main Lamport_Obj) {
		synchronized(Lamport_Obj){
			new Output_Writer(Lamport_Obj).writeToFile();
			for(int s : Lamport_Obj.neighbor){
				Finish_Message m = new Finish_Message();
				ObjectOutputStream output_Stream = Lamport_Obj.outputStreamData.get(s);
				try {
					output_Stream.writeObject(m);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			System.out.println("\n Node ID : " + Lamport_Obj.ID + "- written to output file");
			System.exit(0);
		}
	}
}

class Output_Writer {
	Lamport_Main Lamport_Obj;

	public Output_Writer(Lamport_Main Lamport_Obj) {
		this.Lamport_Obj = Lamport_Obj;
	}


	public void writeToFile() {
		String fileName = Lamport_Main.outputFileName+"-"+ Lamport_Obj.ID +".out";
		synchronized(Lamport_Obj.output){
			try {
				File file = new File(fileName);
				FileWriter fileWriter;
				if(file.exists()){
					fileWriter = new FileWriter(file,true);
				}
				else
				{
					fileWriter = new FileWriter(file);
				}
				BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

				for(int i = 0; i< Lamport_Obj.output.size(); i++){
					for(int j: Lamport_Obj.output.get(i)){
						bufferedWriter.write(j+" ");
						
					}
					if(i<(Lamport_Obj.output.size()-1)){
	            bufferedWriter.write("\n");
					}
				}			
				Lamport_Obj.output.clear();
				bufferedWriter.close();
			}
			catch(IOException ex) {
				System.out.println("Error writing to file '" + fileName + "'");
			}
		}
	}

}
