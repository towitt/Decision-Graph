package org.decisionGraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class RemoveAttribute {

	public static ArrayList<String> rm(ArrayList<String> attributes,  String exclude){
		if(attributes.size() == 0) return null;
		ArrayList<String> a = new ArrayList<String>();		
		for(String s : attributes){
			if(s.compareTo(exclude) != 0) a.add(s);
		}
		return a;
	}
	
	public static Set<String> rm(Set<String> attributes,  String exclude){
		if(attributes.size() == 0) return null;
		Set<String> a = new HashSet<String>();		
		for(String s : attributes){
			if(s.compareTo(exclude) != 0) a.add(s);
		}
		return a;
	}
}
