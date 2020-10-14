package org.jrg.parser;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;

import static java.nio.file.FileVisitResult.*;

/**
 * This class searches for files with extension ".java" and creates the corresponding
 * graph for each java file.
 * @author Ritu Arora
 *
 */
public class Finder extends SimpleFileVisitor<Path> {
    private int numMatches = 0;
    ExtendedJRGCreator dSererGraph=null;

    Finder(ExtendedJRGCreator dGraph)
    {
    	dSererGraph= dGraph;
    }
    
    void find(Path file) {
        Path name = file.getFileName();
     //   System.out.println(file);
        if (name != null && name.toString().contains(".java")) {
            numMatches++;
           // System.out.println(file);
            try {
				dSererGraph.createConnectingGraph(file.toFile());
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
    }

    // Prints the total number of
    // matches to standard out.
    void done() {
        System.out.println("Matched: "+ numMatches);
    }

    // Invoke the pattern matching
    // method on each file.
    @Override
    public FileVisitResult visitFile(Path file,
            BasicFileAttributes attrs) {
        find(file);
        return CONTINUE;
    }

    // Invoke the pattern matching
    // method on each directory.
    public FileVisitResult preVisitDirectory(Path dir,
            BasicFileAttributes attrs) {
        find(dir);
        return CONTINUE;
    }

    public FileVisitResult visitFileFailed(Path file,
            IOException exc) {
        System.err.println(exc);
        return CONTINUE;
    }
    
    public void writeToFile(String fileName)
    {
    	try
		{
    	File configfile = new File("D:\\filesExisting.txt");
        
        if (!configfile.exists()) {
        	configfile.createNewFile();
        	}
    	
		FileWriter fw = new FileWriter(configfile.getAbsoluteFile(), true);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(fileName);
		bw.newLine();
		bw.close();

		
		} catch (IOException e) {
			e.printStackTrace();
		} 
    }
}

