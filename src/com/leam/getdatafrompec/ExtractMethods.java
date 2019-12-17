package com.leam.getdatafrompec;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDComboBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;


public class ExtractMethods {

	public void getNotaPEC1(String dir) throws IOException { 
	    try {
			PDDocument pdf = PDDocument.load(new File(dir));
		    PDAcroForm form = pdf.getDocumentCatalog().getAcroForm();
			PDComboBox co = (PDComboBox) form.getField("NOTA");
		    toClip(co.getValue().get(0));
            
            // close pdf form
            pdf.close();
            pdf = null;
            form = null;
	    } catch (Exception e) {
			e.printStackTrace();
		}
	}
	
    public void getEntregaHonor(String dir, String periodo, String curso) throws IOException {  
        
        //Get the PEC files of dir
        File folder = new File(dir);
        FilenameFilter pdfFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".pdf");
            }
        };
        
        List<String> lines = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        Boolean lProblems = false;
        try {
	        lines.add("DNI;Curso;Periodo;npec;entregada;honor");
	        File[] listOfFiles = folder.listFiles(pdfFilter);
	        for (File file : listOfFiles) {
	            if (file.isFile()) {
	            	String n = file.getName();
	            	String dni = n.substring(n.lastIndexOf("_") + 1, n.indexOf(".pdf"));
		            boolean honor = false;
	            	
                    //open pdf file
	    			PDDocument pdf = PDDocument.load(file);
	    		    PDAcroForm form = pdf.getDocumentCatalog().getAcroForm();
	    		    String producer = pdf.getDocumentInformation().getProducer();		// get form producer
	                if (producer.toUpperCase().contains("LibreOffice".toUpperCase()) ||
	                        form.getFields().size()>0) {	                	
                        //get honor field
                        PDCheckBox cbHonor = (PDCheckBox) form.getField("HONOR");
                        honor = cbHonor.isChecked();
	                } else {
                        //the pdf is not readable
                        lProblems = true;
                        problems.add(dni);
                    }	                        
                    // close pdf form
                    pdf.close();
                    pdf = null;
                    form = null;
                    
    	            // entregada = 1 always
    	            String c = "'" + dni + "';'" + curso + "';'" + periodo + "';" +
    	            		((curso.equalsIgnoreCase("ST1")) ? "2" : "1") + ";1;" +
    	            		((honor) ? "1" : "0"); 
    	            lines.add(c);
	            }
	
	        }
	        //write pec1 data file
	        Path fdata = Paths.get(dir + "/datos_pec.txt");
	        Files.write(fdata, lines, Charset.forName("UTF-8"));
	        //write problems file
	        if (lProblems) {
	            Path fproblems = Paths.get(dir + "/problemas.txt");
	            Files.write(fproblems, problems, Charset.forName("UTF-8"));
	            JOptionPane.showMessageDialog(null,"Hay problemas.");
	        }
        } catch (Exception e) {
			e.printStackTrace();
		}
    }
	
    public void getEntregaHonorPEC1(String dir, String periodo) throws IOException {  
        
        //Get the folders of the original directory dir
        File orig = new File(dir);
        String[] directories = orig.list(new FilenameFilter() {
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });
        
        //Loop thorugh the folders
        boolean lProblems = false;
        List<String> lines = new ArrayList<String>();
        List<String> problems = new ArrayList<String>();
        lines.add("DNI;Curso;Periodo;npec;entregada;honor;mdb;pdf");
        try {
	        for (String f : directories) {
	            String dni = f.substring(f.lastIndexOf("_")+1);     //student's dni
	            
	            //Get list of files for the student and confirm PEC1 elements
	            boolean foundMdb = false;
	            boolean foundPdf = false;
	            boolean honor = false;
	            
	            File folder = new File(dir + "/" + f);
		        File[] listOfFiles = folder.listFiles();
		        for (File file : listOfFiles) {
		            if (file.isFile()) {
	                    String n = file.getName();
	                    String ext = n.substring(n.lastIndexOf(".")+1);     //file extension
	                    
	                    //there's a database
	                    if (ext.equals("mdb") || ext.equals("accdb") || ext.equals("odb")) foundMdb = true;
	                    
	                    //there's a pdf form file
	                    if (ext.equals("pdf")) {
	                        foundPdf = true;
	                        
	                        //open pdf file
	            			PDDocument pdf = PDDocument.load(file);
	            		    PDAcroForm form = pdf.getDocumentCatalog().getAcroForm();
	            		    String producer = pdf.getDocumentInformation().getProducer();		// get form producer
	
	                        if (producer.toUpperCase().contains("LibreOffice".toUpperCase()) ||
	                                form.getFields().size()>0) {
	                            //get honor field
	                            PDCheckBox cbHonor = (PDCheckBox) form.getField("HONOR");
	                            honor = cbHonor.isChecked();
	                        } else {
	                            //the pdf is not readable
	                            lProblems = true;
	                            problems.add(dni);
	                        }
	                        
	                        // close pdf form
	                        pdf.close();
	                        pdf = null;
	                        form = null;
	                    }
	                }
	            }
	
	            // curso = ST1, npec = 1, entregada = 1 always; add mdb, pdf, honor information to data
	            String c = "'" + dni + "';'ST1';'" + periodo + "';1;1;" +
	            		((honor) ? "1" : "0") + ";" +
	            		((foundMdb) ? "1" : "0") + ";" +
	            		((foundPdf) ? "1" : "0"); 
	            lines.add(c);
	        }
	        //write pec1 data file
	        Path fdata = Paths.get(dir + "/datos_pec1.txt");
	        Files.write(fdata, lines, Charset.forName("UTF-8"));
	        //write problems file
	        if (lProblems) {
	            Path fproblems = Paths.get(dir + "/problemas.txt");
	            Files.write(fproblems, problems, Charset.forName("UTF-8"));
	            JOptionPane.showMessageDialog(null,"Hay problemas.");
	        }
        } catch (Exception e) {
			e.printStackTrace();
		}
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
	        if (lProblemas) {
	        	Files.write(Paths.get(dir + "/errores.txt"), error, Charset.forName("UTF-8"));
	        	JOptionPane.showMessageDialog(null,"Hay problemas.");
	        }
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
