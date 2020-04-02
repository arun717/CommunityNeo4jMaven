import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;


import java.io.*;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;


public class HelloWorld {
	private static final String Neo4J_DBPath = "C:/Users/Arun Maurya/Documents/Neo4j/default.graphdb";
	
	
	Node first;
	Node second;
	Relationship relation;
	GraphDatabaseService graphDataService;
	
	private static enum RelTypes implements RelationshipType
	{
		KNOWS
	}
	
	public static void main(String args[]){
		HelloWorld hello = new HelloWorld();
		hello.createDatabse();
		hello.removeData();
		hello.shutDown();
	}
	void createDatabse()
	{
		graphDataService = new GraphDatabaseFactory().newEmbeddedDatabase(Neo4J_DBPath);
		
		//Transaction transaction = GraphDatabaseFactory.beginTx();	
		Transaction transaction = graphDataService.beginTx();
		
		try{
			first = graphDataService.createNode();
			first.setProperty("name","Arun Maurya");
			
			second = graphDataService.createNode();
			second.setProperty("name","Ritu Arora");
			
			
			relation = first.createRelationshipTo(second,RelTypes.KNOWS);
			relation.setProperty("relationship-type","KNOWS");
			System.out.println(first.getProperty("name").toString() +" "+ relation.getProperty("relationship-type").toString() +" "+ second.getProperty("name").toString() );
			
			
			transaction.success();
			
		}
		finally{
			transaction.close();
		}
	}
	
	void removeData()
	{
		Transaction transaction = graphDataService.beginTx();
		try{
			//first.getSingleRelationship(RelTypes.KNOWS,Direction.OUTGOING).delete();
			//System.out.println("Nodes are removed");
			
			//first.delete();
			//second.delete();
			transaction.success();
		}
		finally{	
			transaction.close();
		}
	}
	
	void shutDown()
	{
		graphDataService.shutdown();
		System.out.println("Neo4J database is shutdown");
	}
}