package org.jrg.convertor;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.neo4j.cypher.internal.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

/**
 * 
 * @author Arun Maurya
 * 
 */

public class ExtendedJRGConvertor 
{
	GraphDatabaseService graphDb = null;
	Transaction tx= null;
	
	public ExtendedJRGConvertor(GraphDatabaseService db) throws IOException
	{
		graphDb = db;
		tx = graphDb.beginTx();
		createJavaProject();
	}
	
	public void createJavaProject () throws IOException
	{
		System.out.println("===================Start of CreateJavaProject method========================");
		
		try {
			
			//======================================================================================================
			// @comment:To fetch the name of the outermost Project directory name
			// @comment:Manually create the src folder inside the above project folder
			Label label = Label.label( "PROJECT" );
			Node node = tx.findNode(label, "nodeType", "PROJECT");
			String projectDirectory = node.getProperty("name").toString();
			System.out.println("create a folder named >>> "+node.getProperty("name"));  // using name since canonical name is similar to name
			String tempPath = projectDirectory+"/src/";
			File file = new File(tempPath);
			
			try {
				file.mkdirs();
			}
			catch(Exception e)
			{
				System.out.println("Failed to create project directory");
			}
			
			
			//======================================================================================================			
			// @comment: To fetch the name of the packages to create
			label = Label.label("PACKAGE");
			ResourceIterator<Node> packageNodes = tx.findNodes(label);
			HashMap<String, String> packageHashMap = new HashMap<String, String>();
			while(packageNodes.hasNext())
			{
				node = packageNodes.next();
				for (String key : node.getPropertyKeys()) 
					packageHashMap.put(key, node.getProperty(key).toString());
				String canonicalName = packageHashMap.get("canonicalName");
				String parts [] = canonicalName.split("\\.");
				String pkgPath = tempPath;
				for (int t=0;t<parts.length;t++)
					pkgPath += parts[t]+"/";
				file = new File(pkgPath);
				try {
					file.mkdirs();
				}
				catch(Exception e)
				{
					System.out.println("Failed to create package directory");
				}
				
			}

			//======================================================================================================			
			// @comment:To get all the Java Class Files to be created
			ArrayList<String> al_classArray = new ArrayList<String>();
			BufferedWriter writer = null;
	        HashMap<String, String> classHashMap = new HashMap<String, String>();
			label = Label.label("CLASS");
			ResourceIterator<Node> classNodes = tx.findNodes(label);
			while (classNodes.hasNext()) 
			{
				node = classNodes.next();
				al_classArray.add(node.getProperty("name").toString());
				for (String key : node.getPropertyKeys()) 
					classHashMap.put(key, node.getProperty(key).toString());

//				for (String key : classHashMap.keySet()) {
//				    System.out.println(key + " " + classHashMap.get(key));
//				}
//				System.out.println();
				
				
				String canonicalName = classHashMap.get("canonicalName");
				String classFilePath = tempPath + canonicalName.replaceAll("\\.","/")+".java";
				writer = new BufferedWriter(new FileWriter(classFilePath, false));
				
				String val = classHashMap.get("packageName");
				if(!val.equalsIgnoreCase("null"))
					writer.append("package "+ val + ";\n");
				
				val = classHashMap.get("imports");
				if(!val.equalsIgnoreCase("[]"))
					writer.append(val.substring(val.indexOf("[") + 1, val.indexOf("]")) + "\n");
				
				val = classHashMap.get("modifier");
				if(!val.equalsIgnoreCase("null"))
					writer.append(val +" ");
				
				writer.append("class ");
				
				val = classHashMap.get("name");
				if(!val.equalsIgnoreCase("null"))
					writer.append(val + " ");
				
				val = classHashMap.get("extends");
				if(!val.equalsIgnoreCase("null"))
					writer.append("extends " + val + " ");
				
				val = classHashMap.get("implements");
				val = val.substring(val.indexOf("[") + 1, val.indexOf("]"));

				if(!val.equalsIgnoreCase("null"))
					writer.append("implements " + val);
				
				writer.append(" {\n");
				
				writer.close();				
				
			} // end of while loop
			
			//======================================================================================================				
			//@ comment: find ATTRIBUTES connected to respective classes and write to file
			
			BufferedWriter attribute_writer = null;
			
			HashMap<String, String> attrHashMap = new HashMap<String, String>();
			for(int q=0; q<al_classArray.size(); q++)
			{
				String classname = al_classArray.get(q);
				String cql_query = "MATCH(n:ATTRIBUTE)-[r:CONNECTING]->(c:CLASS{name:'"+classname+"'}) RETURN n ORDER BY n.lineNumber"; 
				Result result_attr = tx.execute(cql_query);
				while(result_attr.hasNext())
				{
					Map<String,Object> a_row = result_attr.next();
					for(Entry<String,Object> column: a_row.entrySet())
					{
						node = (Node)column.getValue();
						for (String key : node.getPropertyKeys()) 
							attrHashMap.put(key, node.getProperty(key).toString());
						
						// use canonical name to find in which file to append string
						String attribute_val = attrHashMap.get("canonicalName").toString();

						// creating file name to write to
						String pathFromCName = attribute_val.replaceAll("\\.", "/");
						int pos = pathFromCName.lastIndexOf("/");
						pathFromCName = pathFromCName.substring(0,pos);
						String attributeFilePath = tempPath + pathFromCName + ".java";

						// writer object pointed to file to write into
						attribute_writer = new BufferedWriter(new FileWriter(attributeFilePath, true));
						
						attribute_val = attrHashMap.get("modifier").toString();
						if(!attribute_val.equalsIgnoreCase("null"))
							attribute_writer.write(attribute_val);
						
						attribute_val = attrHashMap.get("dataType").toString();
						if(!attribute_val.equalsIgnoreCase("null"))
							attribute_writer.write(" " + attribute_val);
						
						attribute_val = attrHashMap.get("name").toString();
						if(!attribute_val.equalsIgnoreCase("null"))
							attribute_writer.write(" " + attribute_val);
						
						attribute_val = attrHashMap.get("initializer").toString();
						if(!attribute_val.equalsIgnoreCase("null"))
							attribute_writer.write(" = " + attribute_val);
						
						attribute_writer.write(";\n");
						attribute_writer.close();
						
					}// inner for end under attributes
				
				} // while end under attributes
				
			}// class names for end under attributes

			//======================================================================================================				
			//@ comment: find methods and write into class files 
			
			BufferedWriter method_writer = null;
			HashMap<String, String> methodHashMap = new HashMap<String, String>();
			
			for(int t=0; t<al_classArray.size(); t++)
			{
				
				String classname = al_classArray.get(t);
				String cql_query = "MATCH(n:METHOD)-[r:CONNECTING]->(c:CLASS{name:'"+classname+"'}) RETURN n ORDER BY n.lineNumber"; 
				Result result = tx.execute(cql_query);
				 
				while(result.hasNext())
				{
					Map<String,Object> row = result.next();
					for(Entry<String,Object> column: row.entrySet())
					{
						//System.out.println(column.getKey() + ":" + column.getValue() +"\n" );
						node = (Node)column.getValue();
						for (String key : node.getPropertyKeys()) 
							methodHashMap.put(key, node.getProperty(key).toString());
						
						// To print all method properties of nodes
//						for (String key : methodHashMap.keySet()) {
//						    System.out.println(key + " : " + methodHashMap.get(key));
//						}
//						System.out.println();
						
						// use canonical name to find in which file to append string
						String method_val = methodHashMap.get("canonicalName").toString();
											

						// creating file name to write to
						String pathFromName = method_val.replaceAll("\\.", "/");
						int pos = pathFromName.lastIndexOf("/");
						pathFromName = pathFromName.substring(0,pos);
						String methodFilePath = tempPath + pathFromName + ".java";

						// writer object pointed to file to write into
						method_writer = new BufferedWriter(new FileWriter(methodFilePath, true));
						
						method_val = methodHashMap.get("modifier").toString();
						if(!method_val.equalsIgnoreCase("null"))
							method_writer.write(method_val + " ");
						
						method_val = methodHashMap.get("returnType").toString();
						if(!method_val.equalsIgnoreCase("null"))
							method_writer.write(method_val + " ");
						
						method_val = methodHashMap.get("name").toString();
						method_val = method_val.substring(0,method_val.indexOf('('));
						if(!method_val.equalsIgnoreCase("null"))
							method_writer.write(method_val);
						method_writer.write("(");

						method_val = methodHashMap.get("parameterList").toString();
						if (method_val.charAt(0) == '[')
							method_val = method_val.substring(1,method_val.length()-1); 

						if(!method_val.equalsIgnoreCase("null"))
							method_writer.write(method_val);
						method_writer.write(")\n");
						
						method_val = methodHashMap.get("body").toString();
						System.out.println(method_val);  // for testing purposes to print the method body
						if(!method_val.equalsIgnoreCase("null"))
							method_writer.write(method_val);
						
						//method_writer.write("\n}");
						method_writer.close();
					
					}// end of inner for loop under methods								
					
				}// end of while loop under methods
				
			}// end of for loop for each class under methods
			
			
			//======================================================================================================
			//@ comment: to add final " } " for each class file
			
			BufferedWriter endWriter = null;
			ArrayList<String> al_endClassArray = new ArrayList<String>();
			HashMap<String, String> EndclassHashMap = new HashMap<String, String>();
			label = Label.label("CLASS");
			ResourceIterator<Node> classNodesend = tx.findNodes(label);
			while (classNodesend.hasNext()) 
			{
				node = classNodesend.next();
				al_endClassArray.add(node.getProperty("name").toString());
				for (String key : node.getPropertyKeys()) 
					EndclassHashMap.put(key, node.getProperty(key).toString());
								
				String canonicalName = EndclassHashMap.get("canonicalName");
				String endClassFilePath = tempPath + canonicalName.replaceAll("\\.","/")+".java";
				endWriter = new BufferedWriter(new FileWriter(endClassFilePath, true));
												
				endWriter.append("}");
				
				endWriter.close();				
				
			} // end of while loop for closing brace of every class

				
		}// end of try block
		finally {

			if (tx != null) {
				tx.close();
				System.out.println("Closing transaction object after createJavaProject method");
			}
		}
		System.out.println("===================End of CreateJavaProject method========================");
	}
}

/*** ======= Comments for testing ===========
 1) test code with a neo4j graph having more than 1 package under the project node
 2) test with classes with no implements,extends
 3) check for node label "INTERFACE"
***/