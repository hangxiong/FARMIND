package agent.farm;

import java.util.List;

import product.Product;

public interface Member {
	
	public enum ACTION {
		REPETITION,
		OPTIMIZATION,
		IMITATION,
		OPT_OUT,
	}

	int getAge();
	
	int getEducation();
	
	List<Product> getAction();
	
	List<Product> getPreferences();
	
	int getMemory();
}
