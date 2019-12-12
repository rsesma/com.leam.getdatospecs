package com.leam.getdatafrompec;

import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;


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
