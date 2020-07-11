import java.util.*; 

class QueueNode {
	int node;
	int Depth;
	
	public QueueNode(int i, int j) {
		this.node = i;
		this.Depth = j;
	}
}
public class Converge_Cast {
	
	static int[] parent_Name;
	
	public static int getParent(int id) {
		return parent_Name[id];
	}
	
	static
	void CreateSpanningTree(int[][] CreateMatrix){
		boolean[] visit = new boolean[CreateMatrix.length];
		parent_Name = new int[CreateMatrix.length];
		Queue<QueueNode> queue = new LinkedList<QueueNode>();
		queue.add(new QueueNode(0,0));
		parent_Name[0] = 0;
		visit[0] = true;
		while(!queue.isEmpty()){
			QueueNode u = queue.remove();
			for(int i=0;i<CreateMatrix[u.node].length;i++){
				if(CreateMatrix[u.node][i] == 1 && visit[i] == false){
					queue.add(new QueueNode(i,u.Depth +1));
					Converge_Cast.parent_Name[i] = u.node;
					visit[i] = true;
				}
			}
		}
	}
}
