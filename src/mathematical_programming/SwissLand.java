package mathematical_programming;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import activity.Activity;
import agent.Farm;

public class SwissLand implements MP_Interface{

	private static final Logger LOGGER = Logger.getLogger("FARMIND_LOGGING");
	
	@Override
	public void inputsforMP(Farm farm, List<String> possibleActivity) {
		
		List<String> allActivities = new ArrayList<String>();
		
		for(Activity act: farm.getActivities()) {
			allActivities.add(act.getName());
		}

		try {
            BufferedReader oldScript = new BufferedReader(new FileReader("projdir/DataBaseOut/If_agentTiere.gms"));
            String line;
            String script = "";
            while ((line = oldScript.readLine()) != null) {
            	
            	for(String activity: allActivities ) {
            		String Farm_Activity = String.format("%s.%s", farm.getFarmName(), activity);
            		if (line.contains(Farm_Activity)) {
            			if( possibleActivity.contains(activity) ) {
            				line = String.format("%s.%s %f", farm.getFarmName(), activity, 0.0);
            			}
            			else {
            				line = String.format("%s.%s %.2f", farm.getFarmName(), activity, 0.0);
            			}	
            		}	
            	}
            	
                script += line + '\n';
            }
            
            oldScript.close();
            FileOutputStream newScript = new FileOutputStream("projdir/DataBaseOut/If_agentTiere.gms");
            newScript.write(script.getBytes());
            newScript.close();
        }
		
        catch (IOException ioe) {
        	ioe.printStackTrace();
        }
		
	}

	@Override
	public void runModel(int nFarm, int year ) {
		Runtime runtime = Runtime.getRuntime();						           // java runtime to run commands
		
		this.editMPscript(nFarm, year);										   // edit the gams script with updated pricing information
		
		File f = new File("projdir\\Grossmargin_P4,00.csv");
		f.delete();
		
		LOGGER.info("Starting MP model");
		
		try {
			String name = System.getProperty("os.name").toLowerCase();
			if (name.startsWith("win") ){
				runtime.exec("cmd /C" + "run_gams.bat");					   // actually run command
			}
			if (name.startsWith("mac")) {
				runtime.exec("/bin/bash -c ./run_gams_mac.command");		   // actually run command
			}
			
			LOGGER.info("Waiting for output generated by MP model");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<Double> readMPIncomes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ArrayList<Activity>> readMPActivities() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Activity> getExitActivity() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * edit the MP gams script with the updated year and price information
	 * @param nFarm: number of farms
	 * @param year: which year in iteration so we can select the proper price information
	 */
    private void editMPscript(int nFarm, int year) {	
    	    	

	}
    
	
}
