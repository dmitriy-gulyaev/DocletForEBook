cls
javac -cp ";c:\Program Files\Java\jdk1.8.0_191\lib\tools.jar" DocletForEBook.java TagletForEBook.java
javadoc -docletpath . -doclet DocletForEBook -sourcepath "c:\Program Files\Java\jdk1.8.0_191\src-to-generate-javadoc" -subpackages java