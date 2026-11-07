default:
	javac -d bin -cp "lib/*" Main.java
	java -cp "bin:lib/*" Main
