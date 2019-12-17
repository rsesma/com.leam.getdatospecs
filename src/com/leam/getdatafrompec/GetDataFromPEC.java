package com.leam.getdatafrompec;

import java.io.IOException;

public class GetDataFromPEC {

	public static void main(String[] args) throws IOException {
		String command = args[0];
		String dir = args[1];
		
        ExtractMethods extract = new ExtractMethods();
        
        if (command.equals("getNOTA")) extract.getNotaPEC1(dir);
        // params: dir, periodo
        if (command.equals("entregaPEC1")) extract.getEntregaHonorPEC1(dir, args[2]);
        // params: dir, periodo, curso
        if (command.equals("entrega")) extract.getEntregaHonor(dir, args[2], args[3]);
        // params: dir, periodo, curso
        if (command.equals("getMemos")) extract.getMemos(dir, args[2], args[3]);
	}

}
