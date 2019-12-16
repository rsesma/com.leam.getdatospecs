package com.leam.getdatafrompec;

import java.io.IOException;

public class GetDataFromPEC {

	public static void main(String[] args) throws IOException {
		String command = args[0];
		String dir = args[1];
		
        ExtractMethods extract = new ExtractMethods();
        
        if (command.equals("getHonor")) extract.getHonor(dir);
        if (command.equals("getMemos")) extract.getMemos(dir, args[2], args[3]);
	}

}
