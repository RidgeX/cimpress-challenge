package cimpress.json;

import java.util.*;

public class Output {
	public String id;
	public List<Square> squares;
	
	public Output(String id) {
		this.id = id;
		squares = new ArrayList<Square>();
	}
}
