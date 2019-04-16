package com.task;

import java.io.Console;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConsoleFileManager {
	
	private Console console;
	private PrintWriter writer;
	private String input;
	private List<String> currentDirs = new ArrayList<>();
	private String root = "C:/";
	private String currentPath = "C:";
	
	private enum Commands{
		CD;		
	}
	
	public void launch() {
		console = System.console();	
		if (console == null) {
			System.err.println("No console found");
			System.exit(1);
		}
		writer = console.writer();
		writer.println(root);
		File file = new File(currentPath);
		printContents(file);
		work();
	}
	
	private void  work(){	
	    input = console.readLine();
		Object exception;
		while(!(input.equals("q"))) {
			exception = null;
			String[] args =  input.trim().split("\\s+");
			String command = args[0];		
			try{
				Commands.valueOf(command.toUpperCase());
			}catch(Exception e) {
				exception = e;
				//e.printStackTrace();
				writer.println("wrong command");				
			}
			if(exception == null) {	
				processCommand(command, args);
			}	
			input = console.readLine();
		}
	}
			
	private void printContents(File file) {
		List<File> list = Arrays.asList(file.listFiles());
		list.stream().filter(f -> f.isDirectory()).map(f -> f.getName()).forEach(writer::println);
		list.stream().filter(f -> f.isFile()).map(f -> f.getName()).forEach(writer::println);
		
		currentDirs.clear();
		list.stream().filter(f -> f.isDirectory()).map(f -> f.getName()).forEach(currentDirs::add);
	}
	
	private void processCommand(String command, String[] args) {
		switch(command) {
		case "cd":
			cd(args);
			break;
		}
	}
	
	private void cd(String[] args) {				
		if(args.length < 2) {
			writer.println("expected : cd <dirName>");
		}else {
			String dirName = createDirName(args);
			if(dirName.equals("..")){
				if(currentPath.equals("C:") || currentPath.equals(root)) {		
					currentPath = root;
				}else {
					goUpOneDirectory();
				}
				writer.println("\n" + currentPath);
				printContents(new File(currentPath));	
			}else if(!(currentDirs.contains(dirName))) {
				writer.println("wrong directory name");
			}else {
				currentPath = currentPath +"/"+ dirName;
				writer.println("\n" + currentPath);
				printContents(new File(currentPath));				
			}		
		}		 
	}
	
	private String createDirName(String[] args) {
		String dirName ="";
		for(int i=1; i< args.length; i++) {
			dirName+=args[i]+" ";
		}
		dirName = dirName.substring(0, dirName.length()-1);
		return dirName;
	}
	
	private void goUpOneDirectory() {
		String[] arr = currentPath.split("/");
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i< arr.length-1; i++) {
			sb.append(arr[i]+"/");			
		}
		
		sb.deleteCharAt(sb.length()-1);
		currentPath = sb.toString();
	}
	
	public static void main(String[] args) {	
		ConsoleFileManager app = new ConsoleFileManager();
		app.launch();
	}
}
