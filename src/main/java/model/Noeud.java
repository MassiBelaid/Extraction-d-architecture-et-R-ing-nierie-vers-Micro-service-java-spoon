package model;

import java.util.ArrayList;
import java.util.List;

public class Noeud {
	private List<String> classes;
	
	public Noeud() {
		classes = new ArrayList<String>();
	}
	
	public void addClasse(String c) {
		classes.add(c);
	}

	public List<String> getClasses() {
		return classes;
	}

	public void setClasses(List<String> classes) {
		this.classes = classes;
	}
	
	
	

}
