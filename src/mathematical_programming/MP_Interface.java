package mathematical_programming;

import java.util.ArrayList;
import java.util.List;

import activity.Activity;
import agent.Farm;


/** 
 * Contains the generic functions to allow the decisions to be exported to the MP model
 * @author kellerke
 *
 */
public interface MP_Interface {
	
	public void inputsforMP(Farm farm, List<String> possibleActivity);
	
	public void runModel(int nFarms, int year, boolean pricingAverage, int memoryLengthAverage);
	
	public List<Double> readMPIncomes(List<Farm> allFarms);
	
	public List<ArrayList<Activity>> readMPActivities(List<Farm> allFarms);
	
	public ArrayList<Activity> getExitActivity();
	
}
