package agent.farm;

import java.util.List;

import product.Product;
import reader.Preferences;

/** 
 * Individual Agent for the ABM - represented as a farm
 * @author kellerke
 *
 */
public interface Member {
	
	public enum ACTION {
		REPETITION,
		OPTIMIZATION,
		IMITATION,
		OPT_OUT,
		EXIT,
	}

	int getAge();
	
	int getEducation();
	
	List<Product> getAction(List<Farm> farms);
	
	Preferences getPreferences();
	
	List<Product> getProducts();
	
	int getMemory();
}
