package model;

import java.util.ArrayList;
import java.util.List;

public class Relation {
	private String class1;
	private String class2;
	private double poids;
	
	public Relation(String c1, String c2, double poids) {
		class1 = c1;
		class2 = c2;
		this.poids = poids;
	}

	

	public double getPoids() {
		return poids;
	}

	public void setPoids(double poids) {
		this.poids = poids;
	}



	public String getClass1() {
		return class1;
	}



	public void setClass1(String class1) {
		this.class1 = class1;
	}



	public String getClass2() {
		return class2;
	}



	public void setClass2(String class2) {
		this.class2 = class2;
	}
	

}
