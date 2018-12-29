package main.java;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

public class ExtractRamlTypes {
    
	private static final String AUX_FILE = "src/main/resources/file1.txt";
    private File auxFile;
    private BufferedWriter writer;
    private BufferedWriter typesWriter;
    private BufferedWriter definitionsWriter;
    private boolean typesFound;
     
    public static void main(String[] args) {
        
    	ExtractRamlTypes obj = new ExtractRamlTypes();
        obj.writer = null;
        obj.typesWriter = null;
        obj.definitionsWriter = null;
        obj.auxFile = new File(AUX_FILE);
        processFiles("src/main/resources/types/", obj, true, false);
        processFiles("src/main/resources/dev/", obj, false, true);
        processFiles("src/main/resources/bkp/", obj, false, false);
        obj.clearfile(AUX_FILE);
        System.out.println("PROCESS FINISHED!");
    }

    private static void processFiles(String dir, ExtractRamlTypes obj, Boolean areTypeFiles, Boolean areDevFiles) {
    	
    	prepareFiles(dir, obj, areTypeFiles);
    	obj.typesFound =  false;
    	
    	try (Stream<String> stream = Files.lines(Paths.get("src/main/resources/source.raml"))) {
            stream.forEach(
                line -> {
                	if ("types:".equals(line)) {
                		obj.typesFound = true;
                	}
                	if(obj.typesFound) {
	                    if( (line.length() >= 2) && (line.charAt(2) != ' ') && !("types:".equals(line))) {  
	                    	try {
	                    		obj.writer.close();
	                    		obj.auxFile = obj.createFile(line, dir);
								String filename = obj.prepareFileName(line);
								obj.writer = new BufferedWriter(new FileWriter(obj.auxFile, true));
		                        obj.writer.write(obj.getFileHeader(filename, areDevFiles));
		                        if (areTypeFiles) {
			                        obj.typesWriter.write("  TOKEN: !include TOKEN.raml".replaceAll("TOKEN", filename));
			                        obj.typesWriter.newLine();
			                        obj.definitionsWriter.write("  TOKEN: types.TOKEN".replaceAll("TOKEN", filename));
			                        obj.definitionsWriter.newLine();
		                        }
	                    	} catch (IOException e) {
								e.printStackTrace();
							}
	                    } else {
	                    	if (!line.isEmpty() && ( !"    type: object".equals(line))) {
	                    		try {
	                    			if (areTypeFiles) {
		                    			line = replaceLineByRegex(line, "^(\\s{8}items\\:\\s{1})([A-Z])", true);
		                    			line = replaceLineByRegex(line, "^(\\s{8}type\\:\\s{1})([A-Z])", true);
	                    			} else if (areDevFiles) {
		                    			line = replaceLineByRegex(line, "^(\\s{8}items\\:\\s{1})([A-Z]{1}[a-zA-Z]+)", false);
		                    			line = replaceLineByRegex(line, "^(\\s{8}type\\:\\s{1})([A-Z]{1}[a-zA-Z]+)", false);
	                    			}
	                                obj.writer.write(line.substring(4));
	                    			obj.writer.newLine();
		                    	} catch (IOException e) {
									e.printStackTrace();
								}
	                        }
	                    }
	                }
    			}
            );
            obj.writer.close();
            obj.typesWriter.close();
            obj.definitionsWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        } 
    	
    }
    
    private static void prepareFiles(String dir, ExtractRamlTypes obj, Boolean areTypeFiles) {
    	String typesFile = "src/main/resources/types.raml";
    	String definitionsFile = "src/main/resources/definitions.raml";
    	try {
	    	cleanDirectory(new File(dir));
	    	obj.writer = new BufferedWriter(new FileWriter(obj.auxFile, true));
	    	if (areTypeFiles) {
	    		obj.clearfile(typesFile);
		        obj.clearfile(definitionsFile);
		        obj.clearfile(AUX_FILE);
		        obj.typesWriter = new BufferedWriter(new FileWriter(new File(typesFile), true));
		        obj.typesWriter.write(obj.getLibraryHeader());
		        obj.definitionsWriter = new BufferedWriter(new FileWriter(new File(definitionsFile), true));
	    	}
    	} catch (IOException e1) {
			e1.printStackTrace();
		}
    }
    
    private static String replaceLineByRegex(String line, String regex, Boolean areTypeFiles) {
    	return areTypeFiles? Pattern.compile(regex).matcher(line).replaceAll("$1types.$2") : Pattern.compile(regex).matcher(line).replaceAll("$1!include $2.raml");
    	
    }
    
    public static void cleanDirectory(File directory) throws IOException {
        if (!directory.exists()) {
        	FileUtils.forceMkdir(directory);
        }
        FileUtils.cleanDirectory(directory);
    }
    
    private void clearfile(String fileName) {
        File temp = new File(fileName);
        if (temp.exists()) {
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(temp, "rw");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                raf.setLength(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private File createFile(String fileName, String directory) {
        File file = new File(directory + prepareFileName(fileName).concat(".raml"));
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
    
    private String prepareFileName(String fileName) {
        return fileName.replace(":","").replaceAll("\\s","");
    }
    
    private String getFileHeader(String typeName, Boolean areDevFiles) {
    	return areDevFiles ? getFileHeaderDev(typeName) :  getFileHeader(typeName);
    }
    
    private String getFileHeader(String typeName) {
        return "#%RAML 1.0 DataType\r\n" + 
                "\r\n" + 
                "uses:\r\n" + 
                "  types: types.raml\r\n" +
                "\r\n" + 
                "type: object\r\n" + 
                "displayName: "+ typeName +"\r\n" + 
                "description: "+ typeName +"\r\n";
    }
    
    private String getFileHeaderDev(String typeName) {
        return "#%RAML 1.0 DataType\r\n" + 
                "\r\n" + 
                "type: object\r\n" + 
                "displayName: "+ typeName +"\r\n" + 
                "description: "+ typeName +"\r\n";
    }
    
    private String getLibraryHeader() {
        return "#%RAML 1.0 Library\r\n" + 
                "\r\n" + 
                "types:\r\n";
    }
}
