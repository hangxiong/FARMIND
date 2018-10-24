package mathematical_programming;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.math3.util.Precision;

import activity.Activity;
import agent.Farm;
import reader.ReadData;

/** 
 * Use the weedcontrol model from the AECP group as an optimizer on the activities. This class runs the model, reads the input, and modifies the required control files. 
 * @author kellerke
 *
 */
public class WeedControl implements MP_Interface{
	File file; 																   // file object to read/write 							
	List<Object> year_price = new ArrayList<Object>();						   // list of yearly prices for model
	List<Double> yearlyPrices = new ArrayList<Double>();					   // list of modeling prices per year
	List<String> listOfYears = new ArrayList<String>();						   // list of modeling price/years
	String strategyFile;													   // strategy file
	String resultsFile;														   // results file
	String gamsModelFile;													   // gams model file, used for editing actual gams script
	String yearlyPriceFile; 												   // price file for reading yearly prices 
	private static final Logger LOGGER = Logger.getLogger("FARMIND_LOGGING");
	
	@SuppressWarnings("unchecked")
	public WeedControl(Properties cmd, int simYear, int memoryLengthAverage) {
		strategyFile = String.format("%s\\p_AllowedStratPrePost.csv",cmd.getProperty("project_folder"));
		resultsFile = String.format("%s\\Grossmargin_P4,00.csv",cmd.getProperty("project_folder"));
		gamsModelFile = String.format("%s\\Fit_StratABM_Cal.gms",cmd.getProperty("project_folder"));
		yearlyPriceFile = String.format("./%s/yearly_prices.csv",cmd.getProperty("data_folder"));
		file = new File( strategyFile) ;				   // delete last time period's simulation file
		if (file.exists()) {
			file.delete();
		}
		
		year_price = readMPyearPrice(simYear,memoryLengthAverage);             // Initialize MP inputs (edit number of agents, year and price)
		yearlyPrices = (List<Double>) year_price.get(1);
		listOfYears = (List<String>) year_price.get(0);
	}

	@Override
	public void runModel(Properties cmd, int nFarm, int year, boolean pricingAverage, int memoryLengthAverage ) {
		Runtime runtime = Runtime.getRuntime();						           // java runtime to run commands
		
		this.editMPscript(nFarm, year, pricingAverage, memoryLengthAverage);   // edit the gams script with updated pricing information
		
		File f = new File(resultsFile);
		f.delete();
		
		LOGGER.info("Starting MP model");
		
		try {
			String name = System.getProperty("os.name").toLowerCase();
			if (name.startsWith("win") ){
				createRunGamsBatch(cmd, "win");								   // generate run_gams.bat based on input control files
				runtime.exec("cmd /C" + "run_gams.bat");					   // actually run command
			}
			if (name.startsWith("mac")) {
				createRunGamsBatch(cmd, "mac");			
				runtime.exec("/bin/bash -c ./run_gams_mac.command");		   // actually run command
			}
			
			LOGGER.info("Waiting for output generated by MP model");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void createRunGamsBatch(Properties cmd, String OS) {
		if (cmd.getProperty("debug").equals("1")) {
			if (OS.equals("win")) {
				LOGGER.info("Creating run_gams.bat file for debug");
				File f = new File("run_gams.bat");
				f.delete();
				FileWriter fw;
				try {
					fw = new FileWriter(f,true);
					BufferedWriter bw = new BufferedWriter(fw);
					PrintWriter writer = new PrintWriter(bw);
					writer.println("copy \".\\data\\Grossmargin_P4,00.csv\" .\\projdir");
					LOGGER.fine("copy \".\\data\\Grossmargin_P4,00.csv\" .\\projdir");
					
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (OS.equals("mac")) {
				LOGGER.info("Creating run_gams_mac file");
				File f = new File("run_gams_mac.command");
				f.delete();
				FileWriter fw;
				try {
					fw = new FileWriter(f,true);
					BufferedWriter bw = new BufferedWriter(fw);
					PrintWriter writer = new PrintWriter(bw);
					writer.println("#!/bin/bash");
					writer.println("cp ./data/Grossmargin_P4,00.csv ./projdir/Grossmargin_P4,00.csv");
					LOGGER.fine("cp ./data/Grossmargin_P4,00.csv ./projdir/Grossmargin_P4,00.csv");
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			LOGGER.info("Creating run_gams.bat file for actual gams system");
			File f = new File("run_gams.bat");
			f.delete();
			FileWriter fw;
			try {
				fw = new FileWriter(f,true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter writer = new PrintWriter(bw);
				if (cmd.getProperty("project_folder") == null)  {
					LOGGER.severe("Please include parameter project_folder into the control file.");
					System.exit(0);
				}
				String proj = "cd " + cmd.getProperty("project_folder");
				writer.println(proj);
				writer.println("gams Fit_StratABM_Cal");
				LOGGER.fine("in command file: " + proj + " gams Fit_StratABM_Cal");
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Double> readMPIncomes(Properties cmd, List<Farm> allFarms) {
		List<Double> incomes = new ArrayList<Double>();						   // list of all farm incomes   
		
		List<Object> data = readMPOutputFiles(cmd, allFarms);			       // read data file generated by MP
		incomes = (List<Double>) data.get(0);

		return incomes;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<ArrayList<Activity>> readMPActivities(Properties cmd, List<Farm> allFarms) {
		List<ArrayList<Activity>> activities = new ArrayList<ArrayList<Activity>>();	   	 	   // list of all farm activities selected by MP model
		
		List<Object> data = readMPOutputFiles(cmd, allFarms);			                           // read data file generated by MP
		activities = (List<ArrayList<Activity>>) data.get(1);
		
		if (activities.size() != allFarms.size()) {
			LOGGER.severe("Exiting FARMIND. Gams results do not match expected number of farms.");
			System.exit(0);
		} 
	
		return activities;
	}
	
	@Override
	public void inputsforMP(Farm farm, List<String> possibleActivity) {
		int[][] output = new int[55][6]; 

		for (int i = 0; i<6; i++) {                                            // set all the first row, except the last column, to 1 as the initialization strategies. 
			output[0][i] = 1;
		}
		
		try {
			FileWriter fw = new FileWriter(file,true);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter writer = new PrintWriter(bw);
			if (file.length() == 0) {
				writer.println(",,spre1,spre2,spre3,spre4,spre5,spre6");
			}
						
			for(int i = 1; i < activitySets.length + 1; i++) {
				String name = String.format("activity%d", i);
				if (i < 10) {
					name = String.format("activity0%d", i);
				}
				if (possibleActivity.contains(name)) {
					int[] ind = activitySets[i-1];
					int row = ind[0];
					int column = ind[1];
					output[row-1][column-1] = 1;							   // set correct bit to 1 in output matrix if this strategy is selected
				}
			}
			
			String farmId = farm.getFarmName();
			
			for(int i = 1; i < 56; i++) {	
				int[] row = output[i-1];									   // print each output row to build full gams file
				
				writer.println(String.format("%s,spost%d,%d,%d,%d,%d,%d,%d", farmId, i, row[0],row[1],row[2],row[3],row[4],row[5]));
			}
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	@Override
	public ArrayList<Activity> getExitActivity() {
		ArrayList<Activity> activities = new ArrayList<Activity>();	   	 	       // list of all farm activities selected by MP model
		Activity exit = new Activity(0,"activity01");
		activities.add(exit);
	
		return activities;
	}
	
	/**
	 * edit the MP gams script with the updated year and price information
	 * @param nFarm: number of farms
	 * @param year: which year in iteration so we can select the proper price information
	 */
    private void editMPscript(int nFarm, int year, boolean pricingAverage, int memoryLengthAverage) {	
    	    	
		if(year > listOfYears.size()) {
			System.out.println("Out of iteration years - limited by number of years for MP "
					+ "- the number of interation years should be less than that of total MP years minus 1");
		}
        
		Object MP_year = listOfYears.get(memoryLengthAverage + year-1);
		Object MP_price = yearlyPrices.get(memoryLengthAverage + year-1); 
		
		if (pricingAverage == true) {		
			List<Double> memoryPrice = new ArrayList<Double>();
			for(int i = memoryLengthAverage; i > 0; i--) {
				memoryPrice.add(yearlyPrices.get( memoryLengthAverage - i + (year - 1)));
			}
			
			MP_price = mean(memoryPrice);
		} 
		
		MP_price = Precision.round((double) MP_price,2);		
		LOGGER.fine(String.format("MP year price: %f",MP_price));
			
		try {
            BufferedReader oldScript = new BufferedReader(new FileReader( gamsModelFile));
            String line;
            String script = "";
            while ((line = oldScript.readLine()) != null) {
            	
                // Edit line in the gams script with the number of agents 
                if (line.contains("gn_inAnalysis =")) {
                    line = String.format("gn_inAnalysis =%d", nFarm);
                }
                // Modify price
                if (line.contains("Price of silage maize")) {
                    line = String.format("p_cropPrice    \"Price of silage maize Euro/st\" /%s/", MP_price);
                }
                // Modify year number (two lines concerned)
                if (line.contains("p_yieldMdm(gn,'t")) {
                    line = String.format("p_attyield(gn) =  p_yieldMdm(gn,'t%s')/p_drymatter * 10 * (1 + p_affYieldShare);", MP_year);
                }
                if (line.contains("p_TDistribution(gn,'t")) {
                    line = String.format("/ [100 * ( exp(p_CRate * p_TDistribution(gn,'t%s',s_process) )", MP_year);
                }
                script += line + '\n';
            }
            
            oldScript.close();
            FileOutputStream newScript = new FileOutputStream(gamsModelFile);
            newScript.write(script.getBytes());
            newScript.close();
        }
		
        catch (IOException ioe) {
        	ioe.printStackTrace();
        }
	}
    
    /** 
     * Read yearly pricing information for Thomas' gams model
     * @param memoryLengthAverage: how many years to use in the average of historical pricing
     * @param simYear: specific simulation year
     * @return Map of pricing information
     */
	public List<Object> readMPyearPrice(int simYear, int memoryLengthAverage) {	
		String Line;
		ArrayList<String> yearPrice;
		BufferedReader Buffer = null;		
		List<Object> year_price = new ArrayList<Object>();
		
		List<Double> prices = new ArrayList<Double>();					   // list of modeling prices per year
		List<String> years = new ArrayList<String>();						   // list of modeling price/years

		try {
 			// read input file
			Buffer = new BufferedReader(new FileReader(yearlyPriceFile));
			Line = Buffer.readLine();									       // first line with titles to throw away
			while ((Line = Buffer.readLine()) != null) { 
				yearPrice = CSVtoArrayList(Line);						   // Read farm's parameters line by line
				years.add(yearPrice.get(0));
				prices.add(Double.parseDouble(yearPrice.get(1)));				
			}
		
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (Buffer != null) Buffer.close();
			} catch (IOException Exception) {
				Exception.printStackTrace();
			}
		}
		
		double std = std(prices);
		
		if(std < 0.000001) {
			LOGGER.severe("Weedcontrol price information variance is too small. Please modify prices.");
			System.exit(0);
		}
		
		if (years.size() < (simYear + memoryLengthAverage)) {
			LOGGER.severe("Require more pricing information: memory length + simulation years.");
			System.exit(0);
		}
		
		year_price.add(years);
		year_price.add(prices);
		
		return year_price;
	}
	
	/** 
	 * Read the MP output files to get income and activities
	 * @param allFarms: list of all farms in system
	 * @param cmd :: properties object
	 * @return return incomes and activities produced by the MP model
	 */
	public List<Object> readMPOutputFiles(Properties cmd, List<Farm> allFarms) {
		List<Double> incomesFromMP = new ArrayList<Double>();				       // list of all agents' incomes produced by the MP
		List<List<Activity>> activitiesFromMP = new ArrayList<List<Activity>>();   // list of all agents' final activities selected by the MP
		List<Object> incomes_activitiesOutput = new ArrayList<Object>();		   // combination list of incomes and activities to return
		BufferedReader Buffer = null;	 									       // read input file
		String Line;														       // read each line of the file individually
		ArrayList<String> dataArray;										       // separate data line
		ReadData reader = new ReadData(cmd);
		
		List<Activity> allPossibleActivities = reader.getActivityList();		   // generated activity list with ID and name 
		
		File f = new File(resultsFile);					   // actual results file
		while (!f.exists()) {try {
			Thread.sleep(1000);												       // wait until the MP finishes running
		} catch (InterruptedException e) {
			e.printStackTrace();
		}}

		for(Farm farm:allFarms) {
			// get specific farm name
			String specific_farm_name = farm.getFarmName();
			// see if name is in file, if so, then add to list
			int name_found = 0;
			try {
				Buffer = new BufferedReader(new FileReader(resultsFile));
				Line = Buffer.readLine();
				while ((Line = Buffer.readLine()) != null) {        
					dataArray = CSVtoArrayList(Line);						          // Read farm's parameters line by line
					String farmName = dataArray.get(0);
					if(farmName.equals( specific_farm_name )) {
						name_found = 1;
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
							String name = String.format("activity%d", index+1);
							List<Activity> farmActivityList = new ArrayList<Activity>();
							if (index < 9) {
								name = String.format("activity0%d", index+1);
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
				}
				
				} catch (IOException e) {
					e.printStackTrace();
				}									       
		
				try {
					Buffer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				// if name is not found in file, then we add an income of 0 with an exit activity and go to next farm
				if (name_found == 0) {
					incomesFromMP.add(0.0);
					activitiesFromMP.add(this.getExitActivity());
				}
				
		}

		incomes_activitiesOutput.add(incomesFromMP);
		incomes_activitiesOutput.add(activitiesFromMP);
		
		// delete previous gams control file
		file = new File(strategyFile);				   // delete last time period's simulation file
		if (file.exists()) {
			file.delete();
		}
		return incomes_activitiesOutput;
	}
	
	/**
	 * This function converts data from CSV file into array structure 
	 * @param CSV String from input CSV file to break into array
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
	 * Return mean value of provided list 
	 * @param list: list of values to calculate mean with
	 * @return mean: mean value of list
	 */
	private double mean(List<Double> list) {
		double mean = 0;												       // mean value to return
		
		for (int i = 0; i<list.size(); i++) {
			mean = mean + list.get(i);
		}
		
		mean  = mean / list.size();
		return mean;
	}
	/**
	 * This function calculates the standard deviation of provided list.
	 * @param list: list for calculating standard deviation
	 * @return std: standard deviation value
	 */
	private double std(List<Double> list) {
		double std = 0;		
		for (int i=0; i<list.size();i++)
		{
		    std = std + Math.pow(list.get(i) - mean(list), 2);
		}
		
		std = Math.sqrt(std/list.size());
		return std;
	}
	
	/**
	 *  The following tuples correspond to the 72 activities in the application. The first element in the tuple is a row in the activity matrix, and the second element is the column.
	 *  Each row element corresponds to a post-sowing strategy, and each column is a pre-sowing strategy. E.g. [53,2] corresponds to the post_strat 53 and pre_strat 2 strategy set.  
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
}
