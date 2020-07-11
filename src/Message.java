import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

 
@SuppressWarnings("serial")
class StateMessage extends Message implements Serializable{
	boolean active;
	int nodeId;
	HashMap<Integer,ArrayList<Application_Message>> channel_states;
	int[] vector;
}

@SuppressWarnings("serial")
public class Message implements Serializable {
	Lamport_Main m = new Lamport_Main();
	int n = m.numOfNodes;
}

@SuppressWarnings("serial")
class marker_Message extends Message implements Serializable{
	String msg = "marker";
	int n_ID;
}


@SuppressWarnings("serial")
class Finish_Message extends Message implements Serializable{
	String msg = "halt";
}

@SuppressWarnings("serial")
class Application_Message extends Message implements Serializable{
	String msg = "hello";
	int nodeId;
	int[] vector;
}