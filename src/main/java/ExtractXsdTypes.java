package main.java;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractXsdTypes {

    private static final String SOURCE_FILE = "src/main/resources/xsd/sourceXSD.xsd";
    private static final String FILE_INIT = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n" + 
    		"<xs:schema version=\"1.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n";
    private static final String FILE_END = "\n\n</xs:schema>";
    private static final String INCLUDE_LINE = "\n  <xs:include schemaLocation=\"";
    private static final String INCLUDE_LINE_END = ".xsd\" />\r\n";
    private static final String XSD_DIR = "src/main/resources/xsd/";    
    private static final String COMPLEXTYPE_REGEX = "(\\s*)<xs:(?:complexType|simpleType) name=\"(.*?)\".*?>.*?<\\/xs:(?:complexType|simpleType)>";
    private static final Pattern pattern = Pattern.compile(COMPLEXTYPE_REGEX, Pattern.MULTILINE | Pattern.DOTALL);
    private static final String SUBPATTERN_REGEX = "^.*<xs:schema.*?>.*?(<xs:.*)";
    private static final Pattern subPattern = Pattern.compile(SUBPATTERN_REGEX, Pattern.MULTILINE | Pattern.DOTALL);
    
	public static void main(String[] args) {
		
		try {
			System.out.println("Initiating process...");
			// reading sourceXSD file
			String data = new String(Files.readAllBytes(Paths.get(SOURCE_FILE)));
			Matcher matcher = pattern.matcher(data);
			StringBuffer sb = new StringBuffer();
			String newHeader = FILE_INIT;
			// separating the complexType and simpleTypes
			while (matcher.find())
			{
				// gets the complexType and the name to create the new separated xsd file
				String complexType = matcher.group(0);
				String complexTypeName = matcher.group(2);
				
				// add to the string that will set the includes
				newHeader += INCLUDE_LINE + complexTypeName + INCLUDE_LINE_END;

				// removing empty spaces for estetical reasons (fashion)
				complexType = complexType.replace(matcher.group(1), "");
				
				// get the path of the new file
				Path path = Paths.get(XSD_DIR + complexTypeName + ".xsd");
				// create the file content with the complexType
				String fileContent = FILE_INIT + "\n  " + complexType + FILE_END;
				// write new file
				Files.write(path, fileContent.getBytes(), StandardOpenOption.CREATE_NEW);
				
				//Replace it in the "general" file while matching
				matcher.appendReplacement(sb, "");
			}
			//applies the changes in the data string
			matcher.appendTail(sb);
			String newXsdSchema = sb.toString();
			Matcher subMatcher = subPattern.matcher(newXsdSchema);
			// add the include properties in the source file
			if (subMatcher.find())
			{
				newXsdSchema = newHeader + "\n  " + subMatcher.group(1);
			}
			
			// write the new updated source file
			Files.write(Paths.get(SOURCE_FILE), newXsdSchema.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
			System.out.println("XSD extracted successfully!");
			
		} catch (IOException e) { 
			e.printStackTrace();
		} 
	}

}
