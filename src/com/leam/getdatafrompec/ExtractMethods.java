package com.leam.getdatafrompec;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;


public class ExtractMethods {

	public void getHonor(String dir) throws IOException { 
	    try {
			PDDocument pdf = PDDocument.load(new File(dir));
		    PDAcroForm form = pdf.getDocumentCatalog().getAcroForm();
		    String producer = pdf.getDocumentInformation().getProducer();		// get form producer
		    
            if (producer.toUpperCase().contains("LibreOffice".toUpperCase()) ||
                    form.getFields().size()>0) {
                //get honor field
                PDCheckBox honor = (PDCheckBox) form.getField("HONOR");
                if (honor.isChecked()) toClip("y");
                else toClip("n");
            } else {
            	toClip("error");
            }
            
            // close pdf form
            pdf.close();
            pdf = null;
            form = null;
	    } catch (Exception e) {
			e.printStackTrace();
		}
	    
        //JOptionPane.showMessageDialog(null,dir);
	}
	
	public void getMemos(String dir, String periodo, String curso) throws IOException {
        //Get the PEC files of dir
        File folder = new File(dir);
        FilenameFilter pdfFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".pdf");
            }
        };

        List<String> lines = new ArrayList<>();
        List<String> error = new ArrayList<>();
        Boolean lProblemas = false;
        try {
	        lines.add("periodo;curso;dni;pregunta;respuesta");
	        File[] listOfFiles = folder.listFiles(pdfFilter);
	        for (File file : listOfFiles) {
	            if (file.isFile()) {
	            	String n = file.getName();
	            	String dni = n.substring(n.lastIndexOf("_") + 1, n.indexOf(".pdf"));
	            	
	    			PDDocument pdf = PDDocument.load(file);
	    		    PDAcroForm form = pdf.getDocumentCatalog().getAcroForm();
	    		    String producer = pdf.getDocumentInformation().getProducer();		// get form producer
	                if (producer.toUpperCase().contains("LibreOffice".toUpperCase()) ||
	                        form.getFields().size()>0) {
		    		    for(PDField field : form.getFields()){
		    		    	String name = field.getPartialName();
		    		    	if (name.substring(0,1).equals("M")) {
		    		    		String c = field.getValueAsString();
		    		    		c = c.replace("'", "$");
		    		    		lines.add("'" + periodo + "';'" + curso + "';'" + dni + "';'" + 
		    		    				name + "';'" + c  + "'");
		    		    	}    		    	
		    		    }
	                } else {
	                	lProblemas = true;
	                	error.add(dni + "; " + producer);
	                }
	                
                    // close pdf form
                    pdf.close();
                    pdf = null;
                    form = null;
	            }
	        }
	        
	        Files.write(Paths.get(dir + "/respuestas.txt"), lines, Charset.forName("UTF-8"));
	        if (lProblemas) Files.write(Paths.get(dir + "/errores.txt"), error, Charset.forName("UTF-8"));
        } catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void toClip(String s) {
		Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection c = new StringSelection(s);
		clip.setContents(c, c);
	}

}
