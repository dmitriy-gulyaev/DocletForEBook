import com.sun.javadoc.*;
import java.io.*;

public class DocletForEBook {

    public static boolean start(RootDoc root) throws IOException {
		
		TagletForEBook tagletForEBook = new TagletForEBook();
		
        ClassDoc[] classes = root.classes();
        for (int i = 0; i < classes.length; ++i) {
			
			ClassDoc classDoc = classes[i];
			
			java.io.File file = new java.io.File("out/"+classDoc.toString().replaceAll("\\.","/")+".html");
			System.out.println(file);
			file.getParentFile().mkdirs();
			
			java.io.FileWriter fileWriter = new java.io.FileWriter(file);
			fileWriter.write("<html><head>");
			fileWriter.write("<title>"+classDoc.typeName()+"</title>");
			fileWriter.write("</head><body>");
			
			if(classDoc.isClass()){
				fileWriter.write("Class ");
			} else if(classDoc.isInterface()){
				fileWriter.write("Interface ");
			}
			
			fileWriter.write("<b>"+classDoc+"</b>");
			
			ClassDoc tempClassDoc = classDoc;
			while(tempClassDoc.superclass() != null) {
			  tempClassDoc = tempClassDoc.superclass();
			  if (tempClassDoc.toString().equals("java.lang.Object")) {
  			    break;
			  }
			  fileWriter.write("<br/>" + tempClassDoc);

			}
			
			fileWriter.write("<p>" + replaceTags(classDoc.commentText()) + "</p>");
			
			printDoc(fileWriter, "Fields", classDoc.fields());
			printDoc(fileWriter, "Methods", classDoc.methods());
			
			
			fileWriter.write("</body></html>");
			fileWriter.close();
			
        }
        return true;
    }
	
	private static void printDoc(FileWriter fileWriter, String name, Doc[] docs) throws IOException{
		if(docs.length == 0) {
			return;
		}
		fileWriter.write("<h4>" + name + " (" + docs.length + ")</h4>");
		fileWriter.write("<ul>");
			for (int c = 0; c < docs.length; ++c) {
				Doc doc = docs[c];
				String type = "";
				String signature = "";
				if (doc instanceof MethodDoc) {
					type = ((MethodDoc) doc).returnType().toString();
					signature = ((MethodDoc) doc).signature();
				} if (doc instanceof FieldDoc) {
					type = ((FieldDoc) doc).type().toString();
				}
				fileWriter.write("<li><b>" + (c + 1) + "</b>. ");
				fileWriter.write(type + " <b>" + doc.name() + "</b>"+signature+"<br/>");
				fileWriter.write(replaceTags(doc.commentText()));
				fileWriter.write("</li>");
			}
		fileWriter.write("</ul>");
	}
	
	private static String replaceTags(String comment){
		char[] tempArray = new char[comment.length()+4000];
		int dstIndex = 0;
		
		String currentTag = null;
		int tagLength = 0;
		for(int i=0;i< comment.length();i++) {
			if(comment.charAt(i) == '{') {
				if(comment.charAt(i+2) == 'l' || comment.charAt(i+2) == 'c'){
					currentTag = "b";
					tagLength = 4;
				}
				if(currentTag != null) {
					i+= tagLength+2;
					tempArray[++dstIndex] = '<';
					tempArray[++dstIndex] = 'u';
					tempArray[++dstIndex] = '>';
				}
				

			} else if(comment.charAt(i) == '}' && currentTag != null) {
					tempArray[++dstIndex] = '<';
					tempArray[++dstIndex] = '/';
					tempArray[++dstIndex] = 'u';
					tempArray[++dstIndex] = '>';
					currentTag = null;
					//i++;
					continue;
			}
			tempArray[++dstIndex] = comment.charAt(i);
			}
			return new String(tempArray);
	}
	
	

}