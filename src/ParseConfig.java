import java.io.*;


public class ParseConfig {

	public static Lamport_Main ReadFile_Config(String FileName) throws IOException{
		Lamport_Main currentSystem = new Lamport_Main();
		int count = 0,flag = 0;
		int current_Node = 0;
		String curDir = System.getProperty("user.dir");
		String fileName = curDir+"/"+FileName;
		String line = null;
		try {
			FileReader ReadFile_name = new FileReader(fileName);
			BufferedReader bufferedReader = new BufferedReader(ReadFile_name);
			while((line = bufferedReader.readLine()) != null) {
				if(line.length() == 0)
					continue;
				if(!line.startsWith("#")){
					if(line.contains("#")){
						String[] inputString = line.split("#.*$");
						String[] splitString_Input = inputString[0].split("\\s+");
						if(flag == 0 && splitString_Input.length == 6){
							currentSystem.numOfNodes = Integer.parseInt(splitString_Input[0]);
							currentSystem.minPerActive = Integer.parseInt(splitString_Input[1]);
							currentSystem.maxPerActive = Integer.parseInt(splitString_Input[2]);
							currentSystem.minSendDelay = Integer.parseInt(splitString_Input[3]);
							currentSystem.snapshotDelay = Integer.parseInt(splitString_Input[4]);
							currentSystem.maxNumber = Integer.parseInt(splitString_Input[5]);
							flag++;
							currentSystem.CreateMatrix = new int[currentSystem.numOfNodes][currentSystem.numOfNodes];
						}
						else if(flag == 1 && count < currentSystem.numOfNodes)
						{							
							currentSystem.nodes.add(new Node(Integer.parseInt(splitString_Input[0]),splitString_Input[1],Integer.parseInt(splitString_Input[2])));
							count++;
							if(count == currentSystem.numOfNodes){
								flag = 2;
							}
						}
						else if(flag == 2){
							mergeMatrix(splitString_Input,currentSystem, current_Node);
							current_Node++;
						}
					}
					else {
						String[] inputString = line.split("\\s+");
						if(flag == 0 && inputString.length == 6){
							currentSystem.numOfNodes = Integer.parseInt(inputString[0]);
							currentSystem.minPerActive = Integer.parseInt(inputString[1]);
							currentSystem.maxPerActive = Integer.parseInt(inputString[2]);
							currentSystem.minSendDelay = Integer.parseInt(inputString[3]);
							currentSystem.snapshotDelay = Integer.parseInt(inputString[4]);
							currentSystem.maxNumber = Integer.parseInt(inputString[5]);
							flag++;
							currentSystem.CreateMatrix = new int[currentSystem.numOfNodes][currentSystem.numOfNodes];
						}
						else if(flag == 1 && count < currentSystem.numOfNodes)
						{
							currentSystem.nodes.add(new Node(Integer.parseInt(inputString[0]),inputString[1],Integer.parseInt(inputString[2])));
							count++;
							if(count == currentSystem.numOfNodes){
								flag = 2;
							}
						}
						else if(flag == 2){
							mergeMatrix(inputString,currentSystem,current_Node);
							current_Node++;
						}
					}
				}
			}
			bufferedReader.close();
		}
		catch(FileNotFoundException ex) {
			System.out.println("Unable to open file '" +fileName + "'");                
		}
		catch(IOException ex) {
			System.out.println("Error reading file '" + fileName + "'");                  
		}
		for(int i=0;i<currentSystem.numOfNodes;i++){
			for(int j=0;j<currentSystem.numOfNodes;j++){
				if(currentSystem.CreateMatrix[i][j] == 1){
					currentSystem.CreateMatrix[j][i] = 1;
				}
			}
		}
		return currentSystem;
	}

	static void mergeMatrix(String[] input, Lamport_Main mySystem, int curNode) {
		for(String i:input){
			mySystem.CreateMatrix[curNode][Integer.parseInt(i)] = 1;
		}
	}
}

