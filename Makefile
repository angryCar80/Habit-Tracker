default:
	javac -d bin -cp "lib/*" *.java
	java -cp "bin:lib/*" Main

clean:
	rm -rf bin
