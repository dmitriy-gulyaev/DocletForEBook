package com.tr4w;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jsoup.Jsoup;
import org.jsoup.internal.Normalizer;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.safety.Whitelist;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.ParameterizedType;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;
import com.sun.javadoc.Type;
import com.sun.tools.javadoc.Main;

public class DocletForEBook {

  private DocletForEBook() {
  }

  private static void addEntry(ZipOutputStream zipOutputStream, String filename, String content, int method)
      throws IOException {
    ZipEntry ze = new ZipEntry(filename);
    ze.setMethod(method);

    if (method == 0) {
      ze.setSize(20);
      ze.setCompressedSize(20);
      ze.setCrc(0x2cab616f);
    }
    zipOutputStream.putNextEntry(ze);
    zipOutputStream.write(content.getBytes());
    zipOutputStream.closeEntry();
  }

  private static String getSubPackage(String[][] options) {
    for (String[] pair : options) {
      if (pair[0].endsWith("subpackages")) {
        return pair[1];
      }
    }

    return "no-subpackage";

  }

  private static void addEntry(ZipOutputStream zipOutputStream, String filename, String content) throws IOException {
    addEntry(zipOutputStream, filename, content, Deflater.DEFLATED);
  }

  public static boolean start(RootDoc root) throws IOException {
    String subPackage = getSubPackage(root.options());

    ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(shortened(subPackage) + ".epub"));
    List<Item> manifest = new ArrayList<DocletForEBook.Item>();
    List<Itemref> spine = new ArrayList<DocletForEBook.Itemref>();
    List<NavPoint> toc = new ArrayList<DocletForEBook.NavPoint>();

    // zipOutputStream.setMethod(ZipOutputStream.DEFLATED);
    // zipOutputStream.setLevel(0);
    addEntry(zipOutputStream, "mimetype", "application/epub+zip", Deflater.NO_COMPRESSION);

    // zipOutputStream.setLevel(Deflater.DEFAULT_COMPRESSION);
    addEntry(zipOutputStream, "META-INF/container.xml",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\"><rootfiles><rootfile full-path=\"OEBPS/package.opf\" media-type=\"application/oebps-package+xml\"/></rootfiles></container>");

    ClassDoc[] classes = root.classes();
    for (int i = 0; i < classes.length; ++i) {

      ClassDoc classDoc = classes[i];

      int lastDot = classDoc.toString().lastIndexOf('.');

      if (lastDot != subPackage.length()) {
        //continue;
      }

      String filename = classDoc.toString().replaceAll("\\.", "/") + ".xhtml";
      String filenameFull = "OEBPS/" + filename;

      String id = classDoc.toString().replaceAll("\\.", "_");

      manifest.add(new Item(id, filename));
      spine.add(new Itemref(id));
      toc.add(new NavPoint(toc.size(), classDoc.name(), filename));

      String html = processClassDoc(classDoc);
      addEntry(zipOutputStream, filenameFull, html);

    }

    StringBuilder packageOpf = new StringBuilder(1024);
    packageOpf.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n");
    packageOpf.append(
        "<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"3.0\" unique-identifier=\"pub-identifier\">\r\n");

    packageOpf.append("<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\r\n");
    packageOpf.append("<dc:language id=\"pub-language\">en</dc:language>");
    packageOpf.append("<dc:identifier id=\"pub-identifier\">doclet-for-epub</dc:identifier>");
    packageOpf.append("<dc:title id=\"pub-title\">" + shortened(subPackage) + '-' + LocalDate.now() + "</dc:title>");
    packageOpf.append("<meta property=\"dcterms:modified\">" + LocalDateTime.now() + "</meta>");
    packageOpf.append("</metadata>\r\n");

    packageOpf.append("<manifest>\r\n");
    packageOpf.append("    <item href=\"toc.ncx\" id=\"ncx\" media-type=\"application/x-dtbncx+xml\"/>\r\n");

    manifest.stream().sorted().forEach(item -> packageOpf.append("    " + item + "\r\n"));
    packageOpf.append("</manifest>\r\n");

    packageOpf.append("<spine toc=\"ncx\">\r\n");
    spine.stream().sorted().forEach(itemref -> packageOpf.append("    " + itemref + "\r\n"));
    packageOpf.append("</spine>\r\n");

    packageOpf.append("</package>\r\n");

    addEntry(zipOutputStream, "OEBPS/package.opf", packageOpf.toString());

    StringBuilder tocncx = new StringBuilder(1024);
    tocncx.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
    tocncx.append("<ncx xmlns=\"http://www.daisy.org/z3986/2005/ncx/\" version=\"2005-1\" xml:lang=\"en\">\r\n");
    tocncx.append("<head></head>\r\n");
    tocncx.append("<docTitle><text>" + shortened(subPackage) + "</text></docTitle>\r\n");
    tocncx.append("<navMap>\r\n");
    toc.stream().sorted().forEach(navPoint -> tocncx.append("    " + navPoint + "\r\n"));
    tocncx.append("</navMap></ncx>");
    addEntry(zipOutputStream, "OEBPS/toc.ncx", tocncx.toString());

    zipOutputStream.close();

    return true;
  }

  private static String shortened(String  className) {
	  return className.replaceAll("org.springframework.", "o.s.");
  }
  
  private static String processClassDoc(ClassDoc classDoc) {

    StringBuilder sb = new StringBuilder(1024);

    sb.append("<!DOCTYPE html>\r\n");
    sb.append(
        "<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:epub=\"http://www.idpf.org/2007/ops\" xml:lang=\"en\" lang=\"en\">\r\n");
    sb.append("<head>");
    sb.append("<title>" + classDoc.typeName() + "</title>");
    // sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"epub3.css\"/>");
    sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>");
    sb.append("</head><body>");

    if (classDoc.isFinal()) {
      sb.append("final ");
    }

    if (classDoc.isClass()) {
      if (classDoc.isAbstract()) {
        sb.append("abstract ");
      }
      sb.append("Class ");
    } else if (classDoc.isInterface()) {
      sb.append("Interface ");
    }

    sb.append("<b>" + classDoc.name() + "</b>");

    ClassDoc tempClassDoc = classDoc;

    while (tempClassDoc.superclass() != null) {
      tempClassDoc = tempClassDoc.superclass();
      if (tempClassDoc.toString().equals("java.lang.Object")) {
        break;
      }
      sb.append(" &#8594; " + shortened(tempClassDoc.toString()));
    }

    if (classDoc.interfaceTypes() != null) {
      for (Type type : classDoc.interfaceTypes()) {
        sb.append(" + " + shortened(type.toString()));
      }
    }

    sb.append("<p>" + replaceTags(classDoc.commentText()) + "</p>");

    printTags(sb, classDoc.tags());

    printDoc(sb, "Constructors ", classDoc.constructors());
    printDoc(sb, "Fields", classDoc.fields());
    printDoc(sb, "Methods", classDoc.methods());

    sb.append("</body></html>");
    return sb.toString();
  }

  private static void printDoc(StringBuilder sb, String name, Doc[] docs) {
    if (docs.length == 0) {
      return;
    }
    sb.append("<h4>" + name + " (" + docs.length + ")</h4>");
    sb.append("<ul>");

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
          type = methodDoc.returnType().simpleTypeName() + methodDoc.returnType().dimension();
        }

        signature = Stream.of(methodDoc.parameters()).map(DocletForEBook::parameterToString)
            .collect(java.util.stream.Collectors.joining(", ", " (", ")"));
      } else if (doc instanceof FieldDoc) {
        type = ((FieldDoc) doc).type().toString();
      }
      sb.append("<li><b>" + (c + 1) + "</b>. ");

      ProgramElementDoc programElementDoc = (ProgramElementDoc) doc;
      sb.append(programElementDoc.modifiers() + " ");

      sb.append(type + " <b>" + doc.name() + "</b>" + signature + "<br/>");
      sb.append(replaceTags(doc.commentText()));
      printTags(sb, doc.tags());

      if (doc instanceof MethodDoc && c == docs.length - 1) {
        sb.append("&#9632;");
      }

      sb.append("</li>\n\n");
    }
    sb.append("</ul>");
  }

  private static String parameterToString(Parameter parameter) {
    String type = shortened(parameter.type().toString());
    return "<b>" + type + "</b> " + parameter.name();
  }

  private static void printTags(StringBuilder sb, Tag[] tags) {
    if (tags.length > 0) {
      Stream.of(tags).filter(DocletForEBook::filterTag).collect(Collectors.groupingBy(Tag::name))
          .forEach((tag, list) -> {
            sb.append("<br/><b>" + tag.substring(1) + "</b>: "
                + list.stream().map(tg -> replaceTags(tg.text())).collect(Collectors.joining(", ")));
          });
    }
  }

  private static boolean filterTag(Tag tag) {
    return !(tag.name().equals("@param") || tag.name().equals("@return"));
  }

  public static void main(String[] args) {
    Main.execute(new String[] { "-doclet", DocletForEBook.class.getName(), "-subpackages", "org.springframework.context",
        "-sourcepath", "c:/Java/src" });
  }

  private static String addMissedCloseTags(String comment) {
    StringBuilder tempArray = new StringBuilder(comment.length() + 100);
    boolean inTag = false;
    StringBuilder tag = null;

    Stack<String> stack = new Stack<>();

    for (int i = 0; i < comment.length(); i++) {
      char c = comment.charAt(i);

      if (c == '<') {
        char next = comment.charAt(i + 1);
        if (next != '/' && (/* Character.isUpperCase(next) || */ !Character.isLetter(next))) {
          tempArray.append("&lt;");
          continue;
        }

        inTag = true;
        tag = new StringBuilder();
      }

      if (inTag) {
        tag.append(c);
      }

      if (c == '>') {
        inTag = false;
        String tagStr = tag.toString().toLowerCase();
        String rawTagStr = tagStr.replace("<", "").replace(">", "");

        if (rawTagStr.charAt(0) == '/') {
          String prevTag = stack.pop();
          if (!rawTagStr.endsWith(prevTag)) {
            tempArray.append('<');
            tempArray.append('/');
            tempArray.append(prevTag);
            tempArray.append('>');
          }
        } else if (!stack.isEmpty()) {

          String prevTag = stack.pop();
          if (!rawTagStr.endsWith(prevTag)) {
            tempArray.append('<');
            tempArray.append('/');
            tempArray.append(prevTag);
            tempArray.append('>');
          }

        }

        stack.push(rawTagStr);
        tempArray.append(tagStr);
        continue;
      }

      if (inTag) {
        continue;
      }

      tempArray.append(c);

    }

    return tempArray.toString();
  }

  static String cleanXmlAndRemoveUnwantedTags(String textToEscape) {
    Whitelist whitelist = Whitelist.simpleText();
    whitelist.addTags("h3", "p", "li", "ul", "pre", "table", "tr", "td", "caption", "a");

    OutputSettings outputSettings = new OutputSettings().syntax(OutputSettings.Syntax.xml)
        .charset(StandardCharsets.UTF_8).prettyPrint(true);

    String safe = Jsoup.clean(textToEscape, "", whitelist, outputSettings);
    return safe;
  }

  private static String replaceTags(String comment) {

    comment = comment.replace("&nbsp;", " ");

    comment = cleanXmlAndRemoveUnwantedTags(comment);

    if (comment.indexOf('{') == -1) {
      return comment;
    }

    StringBuilder tempArray = new StringBuilder(comment.length() + 100);

    TagType currentTag = null;

    // int skip = 0;

    for (int i = 0; i < comment.length(); i++) {

      char c = comment.charAt(i);

      if (c == '{' && comment.charAt(i + 1) == '@') {

        for (TagType tagType : TagType.values()) {
          if (comment.indexOf(tagType.getTagName(), i) == i + 1) {
            currentTag = tagType;

            if (currentTag == TagType.LINK) {
              // skip = comment.indexOf(' ', i + 1) - i;
            }

            break;
          }
        }

        if (currentTag != null) {
          i += currentTag.getLength();
          tempArray.append('<');
          tempArray.append(currentTag.getHtmlTag());
          tempArray.append('>');
        }

      } else if (c == '}' && currentTag != null) {
        tempArray.append('<');
        tempArray.append('/');
        tempArray.append(currentTag.getHtmlTag());
        tempArray.append('>');
        currentTag = null;
        continue;
      } else {

        if (c == 0x0A) {
          // continue;
        }

        if (c == '<') {
          char next = comment.charAt(i + 1);
          if (next != '/' && (Character.isUpperCase(next) || !Character.isLetter(next))) {
            tempArray.append("&lt;");
            continue;
          }
        }

        // if (skip > 0) {
        // skip--;
        // continue;
        // }

        tempArray.append(c);

      }

    }
    return tempArray.toString();
  }

  enum TagType {

    LINKPLAIN("u"), LINK("u"), CODE("span");

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

  static class Item implements Comparable<Item> {
    final String id;
    final String href;

    Item(String id, String href) {
      this.id = id;
      this.href = href;
    }

    @Override
    public String toString() {
      return "<item id=\"" + id + "\" href=\"" + href + "\" media-type=\"application/xhtml+xml\"/>";
    }

    @Override
    public int compareTo(Item o) {
      return this.id.compareTo(o.id);
    }
  }

  static class Itemref implements Comparable<Itemref> {
    final String idref;

    Itemref(String idref) {
      this.idref = idref;
    }

    @Override
    public String toString() {
      return "<itemref idref=\"" + idref + "\"/>";
    }

    @Override
    public int compareTo(Itemref o) {
      return this.idref.compareTo(o.idref);
    }
  }

  static class NavPoint implements Comparable<NavPoint> {
    final String id;
    final String text;
    final String src;

    NavPoint(int id, String text, String src) {
      this.id = "nav_" + id;
      this.text = text;
      this.src = src;
    }

    @Override
    public String toString() {
      return "<navPoint id=\"" + id + "\"><navLabel><text>" + text + "</text></navLabel><content src=\"" + src
          + "\"/></navPoint>";
    }

    @Override
    public int compareTo(NavPoint o) {
      return this.text.compareTo(o.text);
    }
  }

}
