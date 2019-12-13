package com.leam.getdatafrompec;

import java.io.IOException;

public class GetDataFromPEC {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		String dir;
		String command;
		
        command = args[0];
        dir = args[1];
        
        ExtractMethods extract = new ExtractMethods();
        
        if (command.equals("getHonor")) extract.getHonor(dir);		
	}

}
