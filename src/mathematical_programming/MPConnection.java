package mathematical_programming;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import activity.Activity;
import reader.ReadData;


public class MPConnection implements MP_Interface{
	File file; 
	
	public MPConnection() {
		//file = new File("p_allowedStratPrePost.csv");				           // delete last time period's simulation file
		file = new File("model\\p_allowedStratPrePost.csv");				           // delete last time period's simulation file
		if (file.exists()) {
			file.delete();
		}
	}

	@Override
	public void runModel() {
		Runtime runtime = Runtime.getRuntime();						           // java runtime to run commands
		
		//File f = new File("Grossmargin_P4,00.csv");					           // delete previous results file before new run
		File f = new File("model\\Grossmargin_P4,00.csv");
		f.delete();
		
		System.out.println("Starting MP model");
		
		try {
			String name = System.getProperty("os.name").toLowerCase();
			if (name.startsWith("win") ){
				runtime.exec("cmd /C" + "run_gams.bat");					   // actually run command
			}
			if (name.startsWith("mac")) {
				runtime.exec("/bin/bash -c ./run_gams_mac.command");		   // actually run command
			}
			
			System.out.println("Waiting for output generated by MP model");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Double> readMPIncomes() {
		List<Double> incomes = new ArrayList<Double>();						   // list of all farm incomes   
		
		List<Object> data = readMPOutputFiles();			                   // read data file generated by MP
		incomes = (List<Double>) data.get(0);

		return incomes;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<ArrayList<Activity>> readMPActivities() {
		List<ArrayList<Activity>> activities = new ArrayList<ArrayList<Activity>>();	   	 	   // list of all farm activities selected by MP model
		
		List<Object> data = readMPOutputFiles();			                   // read data file generated by MP
		activities = (List<ArrayList<Activity>>) data.get(1);
	
		return activities;
	}

	@Override
	public void inputsforMP(String farmId, List<String> possibleActivity) {
		
		int[][] output = new int[55][6]; 
		
		Random rand = new Random();
		int[] init = {1,2,3,4,5,6};	
		int initialization_strategy = rand.nextInt(5) ;                        // number between 1 and 5
		
		int init_value = init[initialization_strategy];                        // init first set of activities in gams control file

		output[0][init_value-1] = 1;
		
		try {
			FileWriter fw = new FileWriter(file,true);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter writer = new PrintWriter(bw);
			if (file.length() == 0) {
				writer.println(",,spre1,spre2,spre3,spre4,spre5,spre6");
			}
						
			for(int i = 1; i < activitySets.length + 1; i++) {
				if (possibleActivity.contains(String.format("\"activity%d\"", i))) {
					int[] ind = activitySets[i-1];
					int row = ind[0];
					int column = ind[1];
					output[row-1][column-1] = 1;							   // set correct bit to 1 in output matrix if this strategy is selected
				}
			}
			
			for(int i = 1; i < 56; i++) {	
				int[] row = output[i-1];									   // print each output row to build full gams file
				String name = farmId.substring(1, farmId.length() - 1);
				if (name.charAt(0) == '\\') {
					name = name.substring(1, name.length() - 1);               // in the input csv files, we use a \ to indicate a " in the output name. This is a workaround for an ugly issue with csv file input in R.
					name = "\"" + name + "\"";
				}
				
				writer.println(String.format("%s,spost%d,%d,%d,%d,%d,%d,%d", name, i, row[0],row[1],row[2],row[3],row[4],row[5]));
			}
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public List<Object> readMPOutputFiles() {
		List<Double> incomesFromMP = new ArrayList<Double>();				       // list of all agents' incomes produced by the MP
		List<List<Activity>> activitiesFromMP = new ArrayList<List<Activity>>();   // list of all agents' final activities selected by the MP
		List<Object> incomes_activitiesOutput = new ArrayList<Object>();		   // combination list of incomes and activities to return
		BufferedReader Buffer = null;	 									       // read input file
		String Line;														       // read each line of the file individually
		ArrayList<String> dataArray;										       // separate data line
		ReadData reader = new ReadData();
		
		List<Activity> allPossibleActivities = reader.getActivityList();		   // generated activity list with ID and name 
		
		File f = new File("model\\Grossmargin_P4,00.csv");							       // actual results file
		while (!f.exists()) {try {
			Thread.sleep(1000);												       // wait until the MP finishes running
		} catch (InterruptedException e) {
			e.printStackTrace();
		}}

		try {
			Buffer = new BufferedReader(new FileReader("model\\Grossmargin_P4,00.csv"));
			
			Line = Buffer.readLine();
			while ((Line = Buffer.readLine()) != null) {                       
				dataArray = CSVtoArrayList(Line);						          // Read farm's parameters line by line
				incomesFromMP.add( Double.parseDouble(dataArray.get(1)) );
				
				String pre = dataArray.get(2);								      // Break the MP output file which has pre- and post-sowing strategies into activities
				pre = pre.substring(4);
				String post = dataArray.get(3);
				post = post.substring(5);
				
				int[] activity = {Integer.valueOf(post),Integer.valueOf(pre)};
				int index = 0;
				for(int i = 0; i < activitySets.length; i++) {				      // activitySets were defined in DecisionResult to allow the correct output combinations to be set for gams
					int[] test = {activitySets[i][0],activitySets[i][1]};
					if (Arrays.equals(activity, test)) {
						index = i;
					}
				}
				
				for(int i = 0; i < allPossibleActivities.size(); i++) {
					String name = String.format("\"activity%d\"", index+1);
					List<Activity> farmActivityList = new ArrayList<Activity>();
					if (index < 10) {
						name = String.format("\"activity0%d\"", index+1);
					}
					if (allPossibleActivities.get(i).getName().equals(name) ) {
						int ID = allPossibleActivities.get(i).getID();
						Activity p = new Activity(ID, name); 
						farmActivityList.add(p);
					}
					if (farmActivityList.size() > 0) {
						activitiesFromMP.add(farmActivityList);
					}
				}	
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}									       

		try {
			Buffer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		incomes_activitiesOutput.add(incomesFromMP);
		incomes_activitiesOutput.add(activitiesFromMP);
		return incomes_activitiesOutput;
	}
	
	/**
	 * This function converts data from CSV file into array structure 
	 * @param CSV String from input CSv file to break into array
	 * @return Result ArrayList of strings 
	 */
	private static ArrayList<String> CSVtoArrayList(String CSV) {		       
		ArrayList<String> Result = new ArrayList<String>();
		
		if (CSV != null) {
			String[] splitData = CSV.split("\\s*,\\s*");
			for (int i = 0; i < splitData.length; i++) {
				if (!(splitData[i] == null) || !(splitData[i].length() == 0)) {
					Result.add(splitData[i].trim());
				}
			}
		}
		return Result;
	}
	
	
	/**
	 *  We have 72 activities in the system, and these tuples correspond to each activity.  first element in the tuple is a row in the activity matrix, and the second element is the column.
	 *  Each row element corresponds to a post sowing strategy, and each column is a pre sowing strategy. So [53,2] corresponds to post_strat 53, and pre_strat 2 strategy set.  
	 */
	public static int[][] activitySets = 
		{
				{1,1},												           // activity 1 is post sowing 1, pre sowing 1											    
				{1,2},														   // activity 2 is post 1, pre 2
				{1,3},														   // activity 3 is post 1, pre 3
				{1,4},													       // activity 4 is post 1, pre 4
				{1,5},														   // activity 5 is post 1, pre 5
				{1,6},														   // activity 6 is post 1, pre 6
				{3,2},														   // activity 7 is post 3, pre 2
				{3,3},														   // activity 8 is post 3, pre 2
				{3,4},
				{3,5},
				{3,6},
				{5,3},
				{5,4},
				{5,5},
				{7,3},
				{7,4},
				{7,5},
				{12,2},
				{12,3},
				{12,4},
				{12,5},
				{12,6},
				{13,3},
				{13,4},
				{13,5},
				{14,3},
				{14,4},
				{14,5},
				{16,3},
				{16,4},
				{16,5},
				{18,2},
				{18,3},
				{18,4},
				{18,5},
				{18,6},
				{21,3},
				{21,4},
				{21,5},
				{22,3},
				{22,4},
				{22,5},
				{23,2},
				{23,3},
				{23,6},
				{28,2},
				{28,3},
				{28,6},
				{33,2},
				{33,3},
				{33,6},
				{36,3},
				{36,4},
				{36,5},
				{37,3},
				{37,4},
				{37,5},
				{39,3},
				{39,4},
				{39,5},
				{51,2},
				{51,3},
				{51,4},
				{52,2},
				{52,3},
				{52,4},
				{53,2},
				{53,3},
				{53,4},
				{54,2},
				{54,3},
				{54,4}														   // strategy 72, post sowing 54, pre sowing 4
		};
	
	/**
	 *  empty matrix for the output gams file. The matrix corresponds to a 55 row by 6 column matrix where each row is a post strategy and the columns are a pre strategy 
	 */
	public static int[][] activity_matrix = 
		{
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0},
				{0,0,0,0,0,0}
		};
	

}
