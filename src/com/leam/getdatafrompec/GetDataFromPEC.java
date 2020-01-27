package com.leam.getdatafrompec;

import java.io.IOException;

public class GetDataFromPEC {

	public static void main(String[] args) throws IOException {
		String command = args[0];
		String dir = args[1];
		
        ExtractMethods extract = new ExtractMethods();
        
        // params: dir
        if (command.equals("get")) extract.getDatosGeneral(dir);
        // params: dir
        if (command.equals("getNOTA")) extract.getNotaPEC1(dir);
        // params: dir, periodo
        if (command.equals("entregaPEC1")) extract.getEntregaHonorPEC1(dir, args[2]);
        // params: dir, periodo, curso
        if (command.equals("entrega")) extract.getEntregaHonor(dir, args[2], args[3]);
        // params: dir, periodo, curso
        if (command.equals("getMemos")) extract.getMemos(dir, args[2], args[3]);
        // params: dir, periodo, curso
        if (command.equals("getRespIO")) extract.getRespuestasIO(dir, args[2], args[3]);
        // params: dir, periodo, curso
        if (command.equals("getP")) extract.getP(dir, args[2], args[3]);
        // params: dir, periodo, curso
        if (command.equals("getPIO1")) extract.getPIO1(dir, args[2], args[3]);
	}

}
