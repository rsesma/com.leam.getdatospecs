package com.leam.getdatafrompec;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDComboBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;


public class ExtractMethods {

	public void getDatosGeneral(String dir) throws IOException { 
        // get all the pdf files of dir
        File folder = new File(dir);
        FilenameFilter pdfFilter;
        pdfFilter = (File dir1, String name) -> { return name.toLowerCase().endsWith(".pdf"); };
        File[] PECs = folder.listFiles(pdfFilter);

        boolean lProblems = false;
        boolean lComments = false;
        boolean lfirst = true;
        boolean lhonor = false;
        boolean lcomentarios = false;
        boolean lape1 = false;
        boolean lape2 = false;
        boolean lnom = false;
        List<String> lines = new ArrayList<>();
        List<String> mlines = new ArrayList<>();
        List<String> comments = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<String> memos = new ArrayList<>();
        for (File file : PECs) {
            if (file.isFile()) {
            	// get dni from filename
                String n = file.getName();
                String dni = n.substring(n.lastIndexOf("_")+1,n.lastIndexOf("."));
                
                System.out.println(dni);

                // open pdf form
				PDDocument pdf = PDDocument.load(file);
			    PDAcroForm form = pdf.getDocumentCatalog().getAcroForm();
                
			    String producer = pdf.getDocumentInformation().getProducer();		// get form producer                
                if (form.getFields().size()>0) {
                    if (!producer.substring(0,Math.min(11,producer.length())).equalsIgnoreCase("LibreOffice")) {
                    	// if the producer is not LibreOffice, the PDF file may be corrupted
                    	lProblems = true;
                        problems.add(dni + "; " + producer);
                    }
                    
                    if (lfirst) {
                        // get form fields names and sort alphabetically
        				for (PDField f : form.getFields()) {
        					String name = f.getFullyQualifiedName();
        					if (name.substring(0, 1).equalsIgnoreCase("P")) names.add(name);		// answers
        					if (name.substring(0, 1).equalsIgnoreCase("M")) memos.add(name);		// memo fields
        					if (name.equalsIgnoreCase("APE1")) lape1 = true;						// there's APE1 field
        					if (name.equalsIgnoreCase("APE2")) lape2 = true;						// there's APE2 field
        					if (name.equalsIgnoreCase("NOMBRE")) lnom = true;						// there's NOMBRE field
        					if (name.equalsIgnoreCase("HONOR")) lhonor = true;						// there's HONOR field
        					if (name.equalsIgnoreCase("COMENT")) lcomentarios = true;				// there's COMENT field
        				}
                        Collections.sort(names);
                        Collections.sort(memos);
                        lfirst = false;
                    }
                    
                    if (lcomentarios) {
	                    // build COMMENTS section
	                    if (!form.getField("COMENT").getValueAsString().isEmpty()) {
	                        lComments = true;
	                        comments.add(dni + ":" + form.getField("COMENT").getValueAsString() + "\n");
	                    }
                    }
                    // header with identification data
                    String c = (lape1 ? "'" + form.getField("APE1").getValueAsString() + "'" : "null") + "," +
                    		(lape2 ? "'" + form.getField("APE2").getValueAsString() + "'" : "null") + "," +
                    		(lnom ? "'" + form.getField("NOMBRE").getValueAsString() + "'" : "null") + "," +
                    		"'" + dni + "'";
                    if (lhonor) {
                    	PDCheckBox honor = (PDCheckBox) form.getField("HONOR");
                        c = c + (honor.isChecked() ? ",1" : ",0");
                    }

                    // loop through the sorted answers and get the contents
                    for (String name : names) {
                    	PDField f = form.getField(name);
                		if (f instanceof PDTextField) {
                			PDTextField ed = (PDTextField) f;				// text field: numeric or memo
                			c = c + ",'" + ed.getValue().replace(".", ",") + "'";
                		}
                		if (f instanceof PDComboBox) {
                			PDComboBox co = (PDComboBox) f;					// combobox field: closed answer
                			c = c + ",'" + co.getValue().get(0) + "'";
                		}
                    }
                    lines.add(c);
                    
                    if (!memos.isEmpty()) {
                    	// loop through the sorted memos and get the contents
                        String m = "'" + dni + "'";
	                    for (String name : memos) {
	                    	PDTextField ed = (PDTextField) form.getField(name);
	                		m = m + ",'" + ed.getValue().replace("'", "''") + "'";
                		}
	                    mlines.add(m);
                	}
                } else {
                	// if there are no fields on the form the PDF file may be corrupted
                    lProblems = true;
                    if (form.getFields().isEmpty()) {
                        problems.add(dni + "; no fields");
                    }
                }
                
                // close pdf form
                pdf.close();
                pdf = null;
                form = null; 
            }
        }

        // save data
        Files.write(Paths.get(dir + "/datos_pecs.txt"), lines, Charset.forName("UTF-8"));
        // save comments, if any
        if (lComments) Files.write(Paths.get(dir + "/comentarios.txt"), comments, Charset.forName("UTF-8"));
        // save problems, if any
        if (lProblems) Files.write(Paths.get(dir + "/errores.txt"), problems, Charset.forName("UTF-8"));
        // save memos, if any
        if (!memos.isEmpty()) Files.write(Paths.get(dir + "/memos.txt"), mlines, Charset.forName("UTF-8"));

        JOptionPane.showMessageDialog(null, "Proceso finalizado." +
                (lComments ? " Hay comentarios." : "") + 
                (lProblems ? " Hay errores." : ""));
	}
	
	public void getDatosSol(String dir, String sol) throws IOException { 
        // get all the pdf files of dir
        File folder = new File(dir);
        FilenameFilter pdfFilter;
        pdfFilter = (File dir1, String name) -> { return name.toLowerCase().endsWith(".pdf"); };
        File[] PECs = folder.listFiles(pdfFilter);

        boolean lProblems = false;
        boolean lComments = false;
        boolean lfirst = true;
        boolean lhonor = false;
        boolean lcomentarios = false;
        boolean lape1 = false;
        boolean lape2 = false;
        boolean lnom = false;
        List<String> lines = new ArrayList<>();
        List<String> comments = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (File file : PECs) {
            if (file.isFile()) {
            	// get dni from filename
                String n = file.getName();
                String dni = n.substring(n.lastIndexOf("_")+1,n.lastIndexOf("."));
                
                System.out.println(dni);

                // open pdf form
				PDDocument pdf = PDDocument.load(file);
			    PDAcroForm form = pdf.getDocumentCatalog().getAcroForm();
                
			    String producer = pdf.getDocumentInformation().getProducer();		// get form producer                
                if (form.getFields().size()>0) {
                    if (!producer.substring(0,Math.min(11,producer.length())).equalsIgnoreCase("LibreOffice")) {
                    	// if the producer is not LibreOffice, the PDF file may be corrupted
                    	lProblems = true;
                        problems.add(dni + "; " + producer);
                    }
                    
                    if (lfirst) {
                        // get form fields names and sort alphabetically
        				for (PDField f : form.getFields()) {
        					String name = f.getFullyQualifiedName();
        					if (name.equalsIgnoreCase("APE1")) lape1 = true;						// there's APE1 field
        					if (name.equalsIgnoreCase("APE2")) lape2 = true;						// there's APE2 field
        					if (name.equalsIgnoreCase("NOMBRE")) lnom = true;						// there's NOMBRE field
        					if (name.equalsIgnoreCase("HONOR")) lhonor = true;						// there's HONOR field
        					if (name.equalsIgnoreCase("COMENT")) lcomentarios = true;				// there's COMENT field
        				}
        				// read sol txt line by line and get names
        				File f= new File(sol);
        				BufferedReader b = new BufferedReader(new FileReader(f));
        				String line = "";
        	            while ((line = b.readLine()) != null) {
        	            	if (!line.substring(0,2).equalsIgnoreCase("id")) {    
        	            		String t[] = line.split(",");
        	            		names.add(t[1].replace("'",""));
        	            	}
        	            }
        	            b.close();
        	            b = null;
                        lfirst = false;
                    }
                    
                    if (lcomentarios) {
	                    // build COMMENTS section
	                    if (!form.getField("COMENT").getValueAsString().isEmpty()) {
	                        lComments = true;
	                        comments.add(dni + ":" + form.getField("COMENT").getValueAsString() + "\n");
	                    }
                    }
                    // header with identification data
                    String c = (lape1 ? "'" + form.getField("APE1").getValueAsString() + "'" : "null") + "," +
                    		(lape2 ? "'" + form.getField("APE2").getValueAsString() + "'" : "null") + "," +
                    		(lnom ? "'" + form.getField("NOMBRE").getValueAsString() + "'" : "null") + "," +
                    		"'" + dni + "'";
                    if (lhonor) {
                    	PDCheckBox honor = (PDCheckBox) form.getField("HONOR");
                        c = c + (honor.isChecked() ? ",1" : ",0");
                    }

                    // loop through the sol answers and get the contents
                    for (String name : names) {
                    	PDField f = form.getField(name);
                		if (f instanceof PDTextField) {
                			PDTextField ed = (PDTextField) f;				// text field: numeric or memo
                			c = c + ",'" + ed.getValue().replace(".", ",") + "'";
                		}
                		if (f instanceof PDComboBox) {
                			PDComboBox co = (PDComboBox) f;					// combobox field: closed answer
                			c = c + ",'" + co.getValue().get(0) + "'";
                		}
                    }
                    lines.add(c);                    
                } else {
                	// if there are no fields on the form the PDF file may be corrupted
                    lProblems = true;
                    if (form.getFields().isEmpty()) {
                        problems.add(dni + "; no fields");
                    }
                }
                
                // close pdf form
                pdf.close();
                pdf = null;
                form = null; 
            }
        }

        // save data
        Files.write(Paths.get(dir + "/datos_pecs.txt"), lines, Charset.forName("UTF-8"));
        // save comments, if any
        if (lComments) Files.write(Paths.get(dir + "/comentarios.txt"), comments, Charset.forName("UTF-8"));
        // save problems, if any
        if (lProblems) Files.write(Paths.get(dir + "/errores.txt"), problems, Charset.forName("UTF-8"));

        JOptionPane.showMessageDialog(null, "Proceso finalizado." +
                (lComments ? " Hay comentarios." : "") + 
                (lProblems ? " Hay errores." : ""));
	}
	
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
	
    public void getEntregaHonorIO(String dir, String periodo, String curso) throws IOException {
        //Get the folders of dir
        File folder = new File(dir);
        FileFilter folderFilter = new FileFilter() {
            public boolean accept(File file) {
                return !file.isFile();
            }
        };
        
        List<String> lines = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        Boolean lProblems = false;
        
        File[] folders = folder.listFiles(folderFilter);
        for(File f : folders) {
        	String n = f.getName();
        	String dni = n.substring(n.lastIndexOf("_") + 1);
        	
            //Get the PDF files of dir
            File subfolder = new File(f.getAbsolutePath());
            FilenameFilter pdfFilter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".pdf");
                }
            };
            
            try {
                boolean lError = true;
	            boolean honor = false;
		        File[] listOfFiles = subfolder.listFiles(pdfFilter);
		        for (File file : listOfFiles) {
		            if (file.isFile()) {
	                    //open pdf file
		    			PDDocument pdf = PDDocument.load(file);
		    		    PDAcroForm form = pdf.getDocumentCatalog().getAcroForm();
		    		    String producer = pdf.getDocumentInformation().getProducer();		// get form producer
		    		    if (form != null) {
		                	if (producer.toUpperCase().contains("LibreOffice".toUpperCase()) ||
		                        form.getFields().size()>0) {	                	
		                        //get honor field
		                        PDCheckBox cbHonor = (PDCheckBox) form.getField("HONOR");
		                        honor = cbHonor.isChecked();
		                        lError = false;
	                        }
	                    }
	                    // close pdf form
	                    pdf.close();
	                    pdf = null;
	                    form = null;
		            }
		        }
		        
                if (!lError) {
    	            String c = dni + ";" +  ((honor) ? "1" : "0"); 
    	            lines.add(c);
                } else {
                	lProblems = true;
                    problems.add(dni);               	
                }
            } catch (Exception e) {
    			e.printStackTrace();
    		}
        }
        
        //write data file
        Path fdata = Paths.get(dir + "/datos_pec.txt");
        Files.write(fdata, lines, Charset.forName("UTF-8"));
        //write problems file
        if (lProblems) {
            Path fproblems = Paths.get(dir + "/problemas.txt");
            Files.write(fproblems, problems, Charset.forName("UTF-8"));
            JOptionPane.showMessageDialog(null,"Hay problemas.");
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
		            
		            System.out.println(dni);
	            	
                    //open pdf file
	    			PDDocument pdf = PDDocument.load(file);
	    		    PDAcroForm form = pdf.getDocumentCatalog().getAcroForm();
	    		    String producer = pdf.getDocumentInformation().getProducer();		// get form producer
	                Boolean lError = true;
	    		    if (form != null) {
	                	if (producer.toUpperCase().contains("LibreOffice".toUpperCase()) ||
	                        form.getFields().size()>0) {	                	
	                        //get honor field
	                        PDCheckBox cbHonor = (PDCheckBox) form.getField("HONOR");
	                        honor = cbHonor.isChecked();
	                        
	                        lError = false;
                        }
                    }
                    // close pdf form
                    pdf.close();
                    pdf = null;
                    form = null;

                    if (!lError) {
	    	            // entregada = 1 always
	    	            String c = "'" + dni + "';'" + curso + "';'" + periodo + "';" +
	    	            		((curso.equalsIgnoreCase("ST1")) ? "2" : "1") + ";1;" +
	    	            		((honor) ? "1" : "0"); 
	    	            lines.add(c);
                    } else {
                    	lProblems = true;
                        problems.add(dni);                    	
                    }
	            }
	
	        }
	        //write data file
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

	public void getRespuestasIO(String dir, String periodo, String curso) throws IOException {
        //Get the PEC files of dir
        File folder = new File(dir);
        FilenameFilter pdfFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".pdf");
            }
        };

        List<String> lines = new ArrayList<>();
        try {
	        lines.add("periodo;curso;dni;q;n;resp");
	        File[] listOfFiles = folder.listFiles(pdfFilter);
	        for (File file : listOfFiles) {
	            if (file.isFile()) {
	            	String n = file.getName();
	            	String dni = n.substring(n.lastIndexOf("_") + 1, n.indexOf(".pdf"));
	            	String head = "'" + periodo + "';'" + curso + "';'" + dni + "';"; 

                	PDDocument pdf = PDDocument.load(file);
	    		    PDAcroForm form = pdf.getDocumentCatalog().getAcroForm();
                	for (PDField field : form.getFields()){
	    		    	String name = field.getPartialName();
	    		    	if (name.substring(0,1).equals("M") || name.substring(0,1).equals("P")) { 
	    		    		if (!name.substring(name.length()-1).equals("A")) {
		    		    		String c = field.getValueAsString();
		    		    		if (!(name.substring(0,1).equals("M") && 
		    		    				c.replace(",", ".").matches("-?\\d+(\\.\\d+)?"))) {
		    		    			lines.add(head + "'" + name.substring(1) + "';" +
		    		    				name.substring(1,3) + ";'" + c.replace("'", "$") + "'");
		    		    		}
	    		    		}
	    		    	}
	    		    }
	    			pdf.close();
	    			pdf = null;
                    form = null;
	            }
	        }
	        
	        Files.write(Paths.get(dir + "/respuestas.txt"), lines, Charset.forName("UTF-8"));
	        
	        System.out.println("End");
        } catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public void getP(String dir, String periodo, String curso) throws IOException {

		// build INSERT INTO sql with dir PEC data
        try {
        	File f = new File(dir); 
        	String n = f.getName();
        	String dni = n.substring(n.lastIndexOf("_") + 1, n.indexOf(".pdf"));
        	String sql = "";
			PDDocument pdf = PDDocument.load(f);
		    PDAcroForm form = pdf.getDocumentCatalog().getAcroForm();
		    for(PDField field : form.getFields()){
		    	String name = field.getPartialName();
		    	String v = "";
		    	if (name.substring(0,1).equals("P")) {
            		if (field instanceof PDTextField) {
            			PDTextField ed = (PDTextField) field;				// text field: numeric or memo
            			v = ed.getValue().replace(".", ",");
            		}
            		if (field instanceof PDComboBox) {
            			PDComboBox co = (PDComboBox) field;					// combobox field: closed answer
            			v = co.getValue().get(0);
            		}

                	sql = sql + "INSERT INTO pec_respuestas (Periodo, Curso, DNI, Pregunta, respuesta) " +
                			"VALUES ('" + periodo + "','" + curso + "','" + dni + "','" + name.substring(1) + "','" + v + "');" +
                			System.lineSeparator();
		    	}
		    }
		    // copy sql sentence to clipboard
		    toClip(sql);
		    
            // close pdf form
            pdf.close();
            pdf = null;
            form = null;
        } catch (Exception e) {
			e.printStackTrace();
        	JOptionPane.showMessageDialog(null, e.getMessage());
		}
	}
	
	
	public void getPIO1(String dir, String periodo, String curso) throws IOException {

		// build INSERT INTO sql with dir PEC data
        try {
        	File f = new File(dir); 
        	String n = f.getName();
        	String dni = n.substring(n.lastIndexOf("_") + 1, n.indexOf(".pdf"));
        	String sql = "";
			PDDocument pdf = PDDocument.load(f);
		    PDAcroForm form = pdf.getDocumentCatalog().getAcroForm();
		    for(PDField field : form.getFields()){
		    	String name = field.getPartialName();
		    	String v = "";
		    	if (name.substring(0,1).equals("P") && name.substring(name.length()-1).equals("A")) {
		    		// only the P..._A fields are of interest on IO1
            		if (field instanceof PDTextField) {
            			PDTextField ed = (PDTextField) field;				// text field: numeric or memo
            			v = ed.getValue().replace(".", ",");
            		}
            		if (field instanceof PDComboBox) {
            			PDComboBox co = (PDComboBox) field;					// combobox field: closed answer
            			v = co.getValue().get(0);
            		}

                	sql = sql + "INSERT INTO pec_respuestas (Periodo, Curso, DNI, Pregunta, respuesta) " +
                			"VALUES ('" + periodo + "','" + curso + "','" + dni + "','" + name.substring(1) + "','" + v + "');" +
                			System.lineSeparator();
		    	}
		    }
		    // copy sql sentence to clipboard
		    toClip(sql);
		    
            // close pdf form
            pdf.close();
            pdf = null;
            form = null;
        } catch (Exception e) {
			e.printStackTrace();
        	JOptionPane.showMessageDialog(null, e.getMessage());
		}
	}
	

	public void toClip(String s) {
		Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection c = new StringSelection(s);
		clip.setContents(c, c);
	}

}
