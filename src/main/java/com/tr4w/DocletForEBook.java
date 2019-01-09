package com.tr4w;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import com.sun.javadoc.*;

public class DocletForEBook {

    private DocletForEBook() {}


    public static boolean start(RootDoc root) throws IOException {

        ClassDoc[] classes = root.classes();
        for (int i = 0; i < classes.length; ++i) {

            ClassDoc classDoc = classes[i];

            java.io.File file = new java.io.File("out/" + classDoc.toString().replaceAll("\\.", "/") + ".html");
            // System.out.println(file);
            file.getParentFile().mkdirs();


            try (java.io.FileWriter fileWriter = new java.io.FileWriter(file)) {
                fileWriter.write("<html><head>");
                fileWriter.write("<title>" + classDoc.typeName() + "</title>");
                // fileWriter.write("<style> u {color: #ffffff; background: #444444; text-decoration-line:
                // none;}</style>");
                fileWriter.write("</head><body>");

                if (classDoc.isAbstract()) {
                    fileWriter.write("abstract ");
                }
                if (classDoc.isFinal()) {
                    fileWriter.write("final ");
                }

                if (classDoc.isClass()) {
                    fileWriter.write("Class ");
                } else if (classDoc.isInterface()) {
                    fileWriter.write("Interface ");
                }

                fileWriter.write("<b>" + classDoc + "</b>");

                ClassDoc tempClassDoc = classDoc;
                while (tempClassDoc.superclass() != null) {
                    tempClassDoc = tempClassDoc.superclass();
                    if (tempClassDoc.toString().equals("java.lang.Object")) {
                        break;
                    }
                    fileWriter.write("<br/>&nbsp;" + tempClassDoc);

                }

                fileWriter.write("<p>" + replaceTags(classDoc.commentText()) + "</p>");

                printTags(fileWriter, classDoc.tags());

                printDoc(fileWriter, "Constructors ", classDoc.constructors());
                printDoc(fileWriter, "Fields", classDoc.fields());
                printDoc(fileWriter, "Methods", classDoc.methods());

                fileWriter.write("</body></html>");
            }

        }
        return true;
    }

    private static void printDoc(FileWriter fileWriter, String name, Doc[] docs) throws IOException {
        if (docs.length == 0) {
            return;
        }
        fileWriter.write("<h4>" + name + " (" + docs.length + ")</h4>");
        fileWriter.write("<ul>");

        Arrays.sort(docs);
        for (int c = 0; c < docs.length; ++c) {
            Doc doc = docs[c];
            String type = "";
            String signature = "";


            if (doc instanceof ConstructorDoc) {
                ConstructorDoc constructorDoc = (ConstructorDoc) doc;
                signature = Stream.of(constructorDoc.parameters()).map(DocletForEBook::parameterToString)
                        .collect(java.util.stream.Collectors.joining(", ", " (", ")"));
            } else if (doc instanceof MethodDoc) {
                MethodDoc methodDoc = (MethodDoc) doc;
                ParameterizedType parameterizedType = methodDoc.returnType().asParameterizedType();
                if (1 == 2 && parameterizedType != null) {
                    type = methodDoc.returnType().asParameterizedType().asClassDoc().toString();
                } else {
                    type = methodDoc.returnType().simpleTypeName();
                }

                signature = Stream.of(methodDoc.parameters()).map(DocletForEBook::parameterToString)
                        .collect(java.util.stream.Collectors.joining(", ", " (", ")"));
            } else if (doc instanceof FieldDoc) {
                type = ((FieldDoc) doc).type().toString();
            }
            fileWriter.write("<li><b>" + (c + 1) + "</b>. ");
            if (((com.sun.javadoc.ProgramElementDoc) doc).isStatic()) {
                fileWriter.write("static ");
            }
            fileWriter.write(type + " <b>" + doc.name() + "</b>" + signature + "<br/>");
            fileWriter.write(replaceTags(doc.commentText()));
            printTags(fileWriter, doc.tags());


            fileWriter.write("</li>");
        }
        fileWriter.write("</ul>");
    }

    private static String parameterToString(com.sun.javadoc.Parameter parameter) {
        return "<b><tt>" + parameter.type() + "</tt></b> " + parameter.name();


    }

    private static void printTags(FileWriter fileWriter, Tag[] tags) throws IOException {
        if (tags.length > 0) {
            // fileWriter.write("<br/>");
            Stream.of(tags).filter(DocletForEBook::filterTag).forEach(tag -> {
                try {
                    fileWriter.write("<br/><b>" + replaceTags(tag.toString())
                            // .replace("<", "&lt;")
                            .replace(":", "</b>: ").substring(1));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            // fileWriter.write("<br/>");
        }
    }

    private static boolean filterTag(Tag tag) {
        return !(tag.name().equals("@param") || tag.name().equals("@return"));
    }


    private static String replaceTags(String comment) {
        if (comment.indexOf('{') == -1) {
            return comment;
        }
        // if(1==1)return comment;

        StringBuilder tempArray = new StringBuilder(comment.length() + 100);

        TagType currentTag = null;

        for (int i = 0; i < comment.length(); i++) {
            if (comment.charAt(i) == '{') {


                for (TagType tagType : TagType.values()) {
                    if (comment.indexOf(tagType.getTagName(), i) == i + 1) {
                        currentTag = tagType;
                        break;
                    }
                }

                if (currentTag != null) {
                    i += currentTag.getLength() + 2;
                    tempArray.append('<');
                    tempArray.append(currentTag.getHtmlTag());
                    tempArray.append('>');
                }


            } else if (comment.charAt(i) == '}' && currentTag != null) {
                tempArray.append('<');
                tempArray.append('/');
                tempArray.append(currentTag.getHtmlTag());
                tempArray.append('>');
                currentTag = null;
                continue;
            }


            tempArray.append(comment.charAt(i));
        }
        return tempArray.toString();
    }


    enum TagType {

        LINK("b"), CODE("tt");

        TagType(String htmlTag) {
            this.htmlTag = htmlTag;
            this.tagName = '@' + name().toLowerCase();
            this.length = this.tagName.length();
        }

        public String getTagName() {
            return tagName;
        }


        public String getHtmlTag() {
            return htmlTag;
        }


        public int getLength() {
            return length;
        }

        private String tagName;
        private String htmlTag;
        private int length;


    }
}
