package org.qcri.sparkpca;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.mahout.math.SequentialAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FileFormat {
	 private final static Logger log = LoggerFactory.getLogger(FileFormat.class);
	 public enum OutputFormat {
		DENSE,  //Dense matrix 
		LIL, //List of lists
		COO, //Coordinate List
	 } 
	 public enum InputFormat {
		DENSE,
		COO
	 } 
	 public static void main(String[] args) {
		final String inputPath;
		final int cardinality;
		final String outputPath;
		final InputFormat inputFormat;
		try {
			inputPath=System.getProperty("Input");
		}
		catch(Exception e) {
			printLogMessage("Input");
			return;
		}
		try {
			inputFormat=InputFormat.valueOf(System.getProperty("InputFmt"));
		}
		catch(IllegalArgumentException e) {
		    	 log.warn("Invalid Format " + System.getProperty("InputFmt") );
		    	 return;
		}
		catch(Exception e) {
		    	printLogMessage("InputFmt");
		    	return;
		}
		try {
			outputPath=System.getProperty("Output");
		}
		catch(Exception e) {
			printLogMessage("Output");
			return;
		}
		try {
			cardinality=Integer.parseInt(System.getProperty("Cardinality"));
		}
		catch(Exception e) {
			printLogMessage("Cardinality");
			return;
		}
		int base=-1;
		try {
			base=Integer.parseInt(System.getProperty("Base"));
		}
		catch(Exception e) {
			log.warn("It is not specified whether the input is zero-based or one-based");
		}
		
		switch(inputFormat)
		{
			case COO:
				if(base==-1) {
					log.error("You have to specify whether the rows and columns IDs start with 0 or 1 using the argument -DBase");
					return;
				}
				convertFromCooToSeq(inputPath,cardinality,base,outputPath);
			case DENSE:
				convertFromDenseToSeq(inputPath,cardinality,outputPath);
		}
		
	}
	public static void convertFromDenseToSeq(String inputPath, int cardinality, String outputPath)
	{
		try
    	{
	    	 final Configuration conf = new Configuration();
	         final FileSystem fs = FileSystem.get(conf);
	         final SequenceFile.Writer writer = SequenceFile.createWriter(fs, conf, new Path(outputPath), IntWritable.class, VectorWritable.class, CompressionType.BLOCK);
	
	         final IntWritable key = new IntWritable();
	         final VectorWritable value = new VectorWritable();
	         
	         int lineNumber=0;
	         String thisLine;
	         String[] filePathList=null;
	          if(new File(inputPath).isFile()) // if it is a file
	          { 
	        	  filePathList= new String[1];
	        	  filePathList[0]=inputPath;
	          }
	          else
	          {
	        	  filePathList=new File(inputPath).list();
	          }
	          if(filePathList==null)
	          {
	        	  log.error("The path " + inputPath + " does not exist");
	          	  return;
	          }
	          for(String path:filePathList)
	          {
		          BufferedReader br = new BufferedReader(new FileReader(inputPath + File.separator + path));
		          Vector vector = null;
		          
		          while ((thisLine = br.readLine()) != null) { // while loop begins here
		              
		        	  String [] splitted = thisLine.split("\\s+");
		        	  vector = new SequentialAccessSparseVector(splitted.length);
		        	  for (int i=0; i < splitted.length; i++)
		        	  {
		        		  vector.set(i, Double.parseDouble(splitted[i]));
		        	  }
		        	  key.set(lineNumber);
		        	  value.set(vector);
		        	  System.out.println(vector);
		        	  writer.append(key,value);//write last row
		        	  lineNumber++;
		          }
		          writer.close();
	          }   
		    }
	    	catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    	
	}
	public static void convertFromCooToSeq(String inputCooFile, int cardinality, int base, String outputFile){
    	try
    	{
    	 final Configuration conf = new Configuration();
         final FileSystem fs = FileSystem.get(conf);
         final SequenceFile.Writer writer = SequenceFile.createWriter(fs, conf, new Path(outputFile), IntWritable.class, VectorWritable.class, CompressionType.BLOCK);

         final IntWritable key = new IntWritable();
         final VectorWritable value = new VectorWritable();
         
    
          String thisLine;
          BufferedReader br = new BufferedReader(new FileReader(inputCooFile));
          Vector vector = null;
          int lineNumber=0;
          int prevRowID=1;
          //boolean first=true;
          while ((thisLine = br.readLine()) != null) { // while loop begins here   		   
        	  String [] splitted = thisLine.split(",");
        	  int rowID=Integer.parseInt(splitted[0]);
        	  int colID=Integer.parseInt(splitted[1]);
        	  double element=Integer.parseInt(splitted[2]);
        	  if(lineNumber==1)
        	  {
        		  //first=false;
        		  vector = new SequentialAccessSparseVector(cardinality);
        	  }
        	  else if(rowID != prevRowID)
        	  {
        		  key.set(prevRowID-base);
        		  value.set(vector);
            	  //System.out.println(vector);
            	  writer.append(key,value);//write last row
            	  vector = new SequentialAccessSparseVector(cardinality);
        	  }
        	  prevRowID=rowID;
        	  vector.set(colID-base,element);
          }
          key.set(prevRowID-base);
          value.set(vector);
          //System.out.println("last vector");
          //System.out.println(vector);
    	  writer.append(key,value);//write last row
          writer.close();
          
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    }
	private static void printLogMessage(String argName )
	 {
		log.error("Missing arguments -D" + argName);
		log.info("Usage: -DInput=<path/to/input/matrix> -DOutput=<path/to/outputfile> -DInputFmt=<DENSE/COO> -DCardinaality=<number of columns> [-Dbase=<0/1>(0 if input is zero-based, 1 if input is 1-based]"); 
	 }
	
}
