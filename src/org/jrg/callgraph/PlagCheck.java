package org.jrg.callgraph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

public class PlagCheck {
	GraphDatabaseService graphDb = null;
	Transaction tx= null;

	public PlagCheck(GraphDatabaseService db, int progNo,ArrayList<String> alist1,ArrayList<String>alist2,ArrayList<Integer>currQ1,ArrayList<Integer>currQ2) throws IOException
	{
		graphDb = db;
		tx = graphDb.beginTx();
		codeCompare(progNo,alist1,alist2,currQ1,currQ2);
	}

	public void codeCompare(int progNo,ArrayList<String> alist1,ArrayList<String>alist2,ArrayList<Integer>currQ1,ArrayList<Integer>currQ2)
	{
		Node node = null;
		
		// to capture id of main node 
		
		String cql_query = "match (n:METHOD{name:'main(String)'}) return id(n)"; 						
		Result res = tx.execute(cql_query);				
		int main_id=-1;

		Map<String,Object> a_row = res.next();
		for(Entry<String,Object> column: a_row.entrySet())
		{
			main_id = ((Long) column.getValue()).intValue();
		}
		// progNo 1 is the base program
		if(progNo == 1)
		{
			currQ1.add(main_id);
		}
		// progNo 2 is the other program to compare
		if(progNo == 2)
		{
			currQ2.add(main_id);
		}
		
		// currQ1 and currQ2 stores the level by level node ids to explore their # of children
		
		System.out.print("\n printing currQ1:");
		for(int e:currQ1)
			System.out.println(e);
		System.out.print("\n printing currQ2:");
		for(int e2:currQ2)
			System.out.println(e2);
		
		// ===============to capture call graph from the main graph===========================================================
/*
		cql_query = "MATCH (m1:METHOD)-[:DEPENDENCY {edgeType:'CALLS'}]->(m2:METHOD) WHERE id(m1) = "+ main_id +" RETURN m2";
		res = tx.execute(cql_query);	
		HashMap<String, String> callGraphHashMap = new HashMap<String, String>();
		while(res.hasNext())
		{
			Map<String,Object> a_row1 = res.next();
			for(Entry<String,Object> column: a_row1.entrySet())
			{
				node = (Node)column.getValue();
				for (String key : node.getPropertyKeys()) 
				{
					callGraphHashMap.put(key, node.getProperty(key).toString());
					//System.out.println("Key : "+key+" value :"+node.getProperty(key).toString());
				}
				
			}
		}
*/
		// TO print the resultant call graph		
//		for (String key : callGraphHashMap.keySet()) {
//		    System.out.println(key + " : " + callGraphHashMap.get(key));
//		}
		//====================================================================================================================

		
		
		
		// TO find the ids of children nodes and store for next iteration
		
		while(currQ1.size()>0 || currQ2.size()>0)
		{
			int cnt1 = -1;
			String tmpList1="[]";
			ArrayList<Integer> nextLevelIds1 = new ArrayList<Integer>();
			if(progNo == 1)
			{
				for(Integer each : currQ1)
				{
					tmpList1="[";
				    //System.out.println("\n"+each +" is the one we are working on currently from currQ1 ");
				    String childIds1 = findChildrenIds(each);
				    if (childIds1 == "")
				    {
				    	continue;
				    }			
				    else
				    {
				    	String parts1 [] = childIds1.split(",");
					    for(String p:parts1)
					    {
					    	if(!p.equals(""))
					    		nextLevelIds1.add(Integer.parseInt(p));
					    }
				    }
				    cnt1 = getChildNodesCount(each);
				    tmpList1 = tmpList1 + String.valueOf(cnt1) + ",";
				}
				if(tmpList1.charAt(tmpList1.length()-1) == (','))
					tmpList1 = tmpList1.substring(0,tmpList1.length()-1)+"]";
				else
					tmpList1 = tmpList1 + "]";
				System.out.println("\n Number of children count prog1 = "+tmpList1);
			}
			
			int cnt2 = -1;
			String tmpList2 ="[]";
			ArrayList<Integer> nextLevelIds2 = new ArrayList<Integer>();						
			if(progNo == 2)
			{
				for(Integer each : currQ2)
				{
					tmpList2 ="[";
				    //System.out.println(each +" is the one we are working on from currQ2 ");
				    String childIds2 = findChildrenIds(each);
				    if (childIds2 == "")
				    {
				    	continue;
				    }			
				    else
				    {
				    	 String parts [] = childIds2.split(",");
				    	 for(String p:parts)
					    {
					    	if(!p.equals(""))
					    		nextLevelIds2.add(Integer.parseInt(p));
					    }
				    }
				    
				    cnt2 = getChildNodesCount(each);
				    tmpList2 = tmpList2 + String.valueOf(cnt2) + ",";
				}
				
				if(tmpList2.charAt(tmpList2.length()-1) == (','))
					tmpList2 = tmpList2.substring(0,tmpList2.length()-1)+"]";
				else
					tmpList2 = tmpList2 + "]";
				System.out.println("\n Number of children count prog2 = "+tmpList2);
				
				
			}
			

			alist1.add(tmpList1);
			alist2.add(tmpList2);
			
			// clear after being done with all nodes of that level and add new nodes for the next iteration
			currQ1.clear();
			currQ1.addAll(nextLevelIds1);
			currQ2.clear();
			currQ2.addAll(nextLevelIds2);

			
		} // end of while loop
		
		//=========================================================================================================================
		
		
							
		
		System.out.println("======printing alist1 contents==================");
		for(String each:alist1)
		{
			System.out.print(each +"|");
		}
		System.out.println();
		System.out.println("======printing alist2 contents==================");
		for(String each2:alist2)
		{
			System.out.print(each2 +"|");
		}
		System.out.println();
		
		
				
					
		//======================Code to find out common / plagiarism =============================================
		
		int arr1[] = new int[alist1.size()];
		int arr2[] = new int[alist2.size()];
		Collections.sort(alist1);
		Collections.sort(alist2);
		ArrayList<String> common = new ArrayList<String>();
		//System.out.println("======printing alist1 contents==================");
		for(String each:alist1)
		{
			//System.out.print(each +" ");
			if(alist2.contains(each))
			{
				common.add(each);
			}
		}
		System.out.println("size of common is "+ common.size());
		for(String ea:common)
		{
			System.out.print(ea +" ");
		}
		if(common.size() == 0)
		{
			System.out.println("Physical method call structure overlap percentage = "+ 0 +"%");
		}
		else
		{
			
			System.out.println("Physical method call structure overlap percentage = "+ (100*common.size())/alist1.size() +"%");
		}
							
	}
	
	
	
	
	public String findChildrenIds(int parentId)
	{
	//  TO get the list of id's of child nodes given the id of a parent node=================================================
		
			String cql_query = "MATCH (m1:METHOD)-[:DEPENDENCY {edgeType:'CALLS'}]->(m2:METHOD) WHERE id(m1) ="+ parentId +" RETURN id(m2)";
			Result res = tx.execute(cql_query);				
			String ids = "";
			int temp_id=-1;
			while(res.hasNext())
			{				
				Map<String,Object> a_row = res.next();
				for(Entry<String,Object> column: a_row.entrySet())
				{
					temp_id = ((Long) column.getValue()).intValue();
					//System.out.println("returning query ids as "+temp_id);
				}				
				ids+= String.valueOf(temp_id) + ",";
			}
			return ids;
	}
	
	public int getChildNodesCount(int parentId)
	{
		// To get the number of child nodes given the parent node id===============================================================
		
			String cql_query = "MATCH (m1:METHOD)-[:DEPENDENCY {edgeType:'CALLS'}]->(m2:METHOD) WHERE id(m1) ="+ parentId +" RETURN count(*)"; 						
			Result res = tx.execute(cql_query);				
			int count=-1;
			while(res.hasNext())
			{
				Map<String,Object> a_row = res.next();
				for(Entry<String,Object> column: a_row.entrySet())
				{
					count = ((Long) column.getValue()).intValue();				
				}					
			}	
			return count;	
	}

}


