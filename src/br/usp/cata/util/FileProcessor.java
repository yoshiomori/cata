package br.usp.cata.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import br.com.caelum.vraptor.interceptor.multipart.UploadedFile;
import br.usp.cata.component.FixesForLatexPDFTexts;


public class FileProcessor {
	
	private String fileName;
	// Each element in the list is a line of the text
	private ArrayList<String> text;
	
	/**
	 * Transforma o UploadedFile enviado pelo usuário em um ArrayList<String> para ser processado pelo sistema.
	 * @param file
	 * @param type
	 */
	public FileProcessor(UploadedFile file, String type) {
		this.fileName = file.getFileName();
		getText(file, type);
	}

	private FileInputStream saveFile(UploadedFile file) throws FileNotFoundException {
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY/MM/dd/hh/mm/ss");
		String folder = dateFormat.format(now);
		File fm = new File(System.getProperty("catalina.base") + "/webapps/cata/WEB-INF/static/files/" + folder);
		fm.mkdirs();
		InputStream is = file.getFile();
		FileOutputStream out;
		
		try {
			byte[] fileBytes = IOUtils.toByteArray(is);
			ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes);
			out = new FileOutputStream(fm + "/" + this.fileName);
			IOUtils.copy(bais, out);
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		File f = new File(fm + "/" + this.fileName);
		return new FileInputStream(f);
	}

	public String getFileName() {
		return fileName;
	}

	public ArrayList<String> getText() {
		return text;
	}
	
	private Charset guessEncoding(byte[] fileBytes) {
		Integer firstFourBytes = 0;
		if (fileBytes.length > 4) {
			for(Integer i = 0; i < 4; i++){
				firstFourBytes <<= 8;
				firstFourBytes |= fileBytes[i] & 255;
			}
			if(firstFourBytes == 0x0000feff)
				return Charset.forName("UTF-32BE");
			if(firstFourBytes == 0xfffe0000)
				return Charset.forName("UTF-32LE");
			if((firstFourBytes & 0xffff0000) == 0xfeff0000)
				return StandardCharsets.UTF_16BE;
			if((firstFourBytes & 0xffff0000) == 0xfffe0000)
				return StandardCharsets.UTF_16LE;
		}
		return StandardCharsets.UTF_8;
	}

	private void fixHiphenation(ArrayList<String> text) {
		for(int i = 0; i < text.size() - 1; i++) {
			String line = text.get(i);
			String nextLine = text.get(i+1);
			
			if(line.length() > 0 && line.charAt(line.length() - 1) == '-') {
				String newLine = line.substring(0, line.length() - 1);
				String newNextLine = "";
				
				int j;
				for(j = 0; j < nextLine.length() && nextLine.charAt(j) != ' '; j++);
				
				newLine += nextLine.substring(0, j);
				if(j < nextLine.length())
					newNextLine = nextLine.substring(j, nextLine.length());
				
				text.remove(i);
				text.add(i, newLine);
				
				text.remove(i+1);
				if(!newNextLine.equals("") || nextLine.equals(""))
					text.add(i+1, newNextLine);
			}
		}
	}
	
	private void fixUglyLatex(ArrayList<String> text) {
		Pattern pattern = FixesForLatexPDFTexts.getPattern();
		Map<String, String> tokens = FixesForLatexPDFTexts.getTokens();
		
		for(int i = 0; i < text.size(); i++) {
			Matcher matcher = pattern.matcher(text.get(i));
	
			StringBuffer sb = new StringBuffer();
			while(matcher.find()) {
			    matcher.appendReplacement(sb, tokens.get(matcher.group(1)));
			}
			matcher.appendTail(sb);
	
			text.remove(i);
			text.add(i, sb.toString());
		}
	}
	
	private void getText(UploadedFile file, String type) {
		text = new ArrayList<String>();
		InputStream is = null;
		try {
			is = saveFile(file);
		} catch (FileNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		if(file.getContentType().equals("text/plain")) {
			try {
				byte[] fileBytes = IOUtils.toByteArray(is);
				Charset charset = Charset.forName(type);
				BufferedReader br = new BufferedReader(
						new InputStreamReader(new ByteArrayInputStream(fileBytes), charset));
				
				String line;
		    	while((line = br.readLine()) != null)
		    		text.add(line);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else if(file.getContentType().equals("application/pdf")) {
			PDFParser parser = null;
		    String parsedText = "";
		    PDFTextStripper pdfStripper;
		    PDDocument pdDoc = null;
		    COSDocument cosDoc = null;
			
			try {
	            parser = new PDFParser(is);
	        } catch (Exception e) { //FIXME
	            e.printStackTrace();
	        }
	        
	        try {
	            parser.parse();
	            cosDoc = parser.getDocument();
	            pdfStripper = new PDFTextStripper();
	            pdDoc = new PDDocument(cosDoc);
	            parsedText = pdfStripper.getText(pdDoc);
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	            try {
			    	if (cosDoc != null) cosDoc.close();
			    	if (pdDoc != null) pdDoc.close();
			    } catch (Exception e1) { //FIXME
	               e1.printStackTrace();
	            }
	        }
	        
	        String[] textLines = parsedText.split("\n");
	        
	        for(String line : textLines)
	        	text.add(line);
	        		
	        this.fixHiphenation(text);
	        this.fixUglyLatex(text);
		}
		else if (file.getContentType().equals("application/msword")) {
			WordExtractor extractor = null;
			
			try {
				HWPFDocument document = new HWPFDocument(is);
				extractor = new WordExtractor(document);
				String[] paragraphs = extractor.getParagraphText();
			
				for(String line : paragraphs)
		        	 text.add(line);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if (file.getContentType().equals("text/x-tex")) {
			try {
				byte[] fileBytes = IOUtils.toByteArray(is);
				Charset charset = guessEncoding(fileBytes);
				BufferedReader br = new BufferedReader(
						new InputStreamReader(new ByteArrayInputStream(fileBytes), charset));
				
				String line;
		    	while((line = br.readLine()) != null) {
		    		text.add(line);
		    	}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else if (file.getContentType().equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
			try {
				XWPFDocument document = new XWPFDocument(is);
				Iterator<XWPFParagraph> paragraphs = document.getParagraphsIterator();
				while(paragraphs.hasNext()){
					XWPFParagraph paragraph = paragraphs.next();
					text.add(paragraph.getText());
				}
				document.close();
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
		
		//TODO: Add more file types
	}
}