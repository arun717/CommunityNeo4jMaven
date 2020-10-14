package org.jrg.parser;


import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import java.util.HashMap;

/**
 * This class provides supporting methods for creating various type of nodes
 * for the dependency graph and connecting them to the graph by creation of corresponding 
 * dependency/connecting edges.
 * @author Ritu Arora
 *
 */
public class DependencyGraphNodes {

	public static enum dGraphNodeType implements Label {
		PROJECT, PACKAGE, CLASS, INTERFACE, METHOD, ATTRIBUTE;
	}

	public static enum dMethodNodeType implements Label {
		VariableDeclarationNode, PACKAGE;
	}

	Relationship relationship;

	public static enum RelTypes implements RelationshipType {
		CONNECTING, DEPENDENCY;
	}

	public static enum methodRelTypes implements RelationshipType {
		BODY;
	}

	HashMap<Long, String> nodeHashMap = new HashMap<Long, String>();
	HashMap<Long, String> nodeCanonicalHashMap = new HashMap<Long, String>();
	HashMap<Long, String> nodeClassCanonicalHashMap = new HashMap<Long, String>();
	HashMap<Long, String> edgeHashMap = new HashMap<Long, String>();

	HashMap<Long, String> methodHashMap = new HashMap<Long, String>();
	HashMap<Long, String> methodEdgeHashMap = new HashMap<Long, String>();

	/**
	 * Creates a new package node, sets all the required attributes and connects it to
	 * the rootNode.
	 * @param graphDb
	 * @param rootNode
	 * @param packName
	 * @param projectName
	 * @return
	 */
	public Node addPackageNode(GraphDatabaseService graphDb, Node rootNode,
			String packName, String projectName, Transaction tx) {

		// System.out.println("Creating Class Node::"+className);
		Node existingPackageNode = checkPackageNodeExists(graphDb, rootNode,
				packName, tx);

		if (existingPackageNode == null)// package node does not exist- create
										// new
		{
			//Node packageNode = graphDb.createNode(dGraphNodeType.PACKAGE);
			Node packageNode = tx.createNode(dGraphNodeType.PACKAGE);
			packageNode.addLabel(dGraphNodeType.PACKAGE);
			packageNode.setProperty("name", packName);
			packageNode.setProperty("canonicalName", packName);
			packageNode.setProperty("nodeType", "PACKAGE");
			packageNode.setProperty("projectName", projectName);

			nodeHashMap.put(packageNode.getId(), packName);// adding canonical
															// name
			nodeCanonicalHashMap.put(packageNode.getId(), packName);// adding
																	// canonical
																	// name

			relationship = packageNode.createRelationshipTo(rootNode,
					RelTypes.CONNECTING);
			relationship.setProperty("edgeType", "OWNER");

			// name and package node name
			relationship.setProperty("name",
					packageNode.getProperty("canonicalName").toString());
			// System.out.println("relationship Id:"+relationship.getId());
			// System.out.println("relationship Name:"+relationship.getProperty("name").toString());
			edgeHashMap.put(relationship.getId(),
					relationship.getProperty("name").toString());// adding
																	// canonical
																	// name

			return packageNode;
		} else
			return existingPackageNode;
	}

	/**
	 * This method creates a new class node, sets all the specified attributes 
	 * and joins the class node to the specified node (pNode) using connecting edge.
	 */
	public Node addConnectingClassNode(GraphDatabaseService graphDb,
			Node pNode, String smallClassName, String className,
			String imports, String packageName, String modifier, String extend,
			String implemented, int lineNumber,  Transaction tx) {

		// System.out.println("Creating Class Node::"+className);
//		Node classNode = graphDb.createNode(dGraphNodeType.CLASS);
		Node classNode = tx.createNode(dGraphNodeType.CLASS);
		classNode.addLabel(dGraphNodeType.CLASS);
		classNode.setProperty("name", smallClassName);
		classNode.setProperty("canonicalName", className);
		classNode.setProperty("nodeType", "CLASS");
		classNode.setProperty("modifier", modifier);
		classNode.setProperty("imports", imports);
		classNode.setProperty("packageName", packageName);
		classNode.setProperty("lineNumber", lineNumber);
		if (extend != null)
			classNode.setProperty("extends", extend);
		else
			classNode.setProperty("extends", "null");
		classNode.setProperty("implements", implemented);
		// System.out.println("cNode Id:"+classNode.getId());
		nodeHashMap.put(classNode.getId(), smallClassName);
		nodeCanonicalHashMap.put(classNode.getId(), className);// adding
																// canonical
																// name

		// for classHashMap
		nodeClassCanonicalHashMap.put(classNode.getId(), className);// adding
																	// canonical
																	// name
		relationship = classNode.createRelationshipTo(pNode,
				RelTypes.CONNECTING);
		relationship.setProperty("edgeType", "OWNER");

		// later set this something else if required //summation of class name
		// and package node name
		relationship.setProperty("name", classNode.getProperty("canonicalName")
				.toString());
		// System.out.println("relationship Id:"+relationship.getId());
		// System.out.println("relationship Name:"+relationship.getProperty("name").toString());
		edgeHashMap.put(relationship.getId(), relationship.getProperty("name")
				.toString());// adding canonical name

		return classNode;
	}

	/**
	 * This method creates a new interface node, sets all the specified attributes 
	 * and joins the newly created node to the specified node (pNode) using connecting edge.
	 */
	public Node addConnectingInterfaceNode(GraphDatabaseService graphDb,
			Node pNode, String smallClassName, String interfaceName,
			String imports, String packageName, String modifier, int lineNumber, Transaction tx) {
		// System.out.println("Creating Class Node::"+className);
//		Node interfaceNode = graphDb.createNode(dGraphNodeType.INTERFACE);
		Node interfaceNode = tx.createNode(dGraphNodeType.INTERFACE);
		
		interfaceNode.addLabel(dGraphNodeType.INTERFACE);
		interfaceNode.setProperty("name", smallClassName);
		interfaceNode.setProperty("canonicalName", interfaceName);
		interfaceNode.setProperty("nodeType", "INTERFACE");
		interfaceNode.setProperty("modifier", modifier);
		interfaceNode.setProperty("imports", imports);
		interfaceNode.setProperty("packageName", packageName);
		interfaceNode.setProperty("lineNumber", lineNumber);
		// System.out.println("cNode Id:"+classNode.getId());
		nodeHashMap.put(interfaceNode.getId(), smallClassName);
		nodeCanonicalHashMap.put(interfaceNode.getId(), interfaceName);

		// for classHashMap
		nodeClassCanonicalHashMap.put(interfaceNode.getId(), interfaceName);// adding
																			// canonical
																			// name

		relationship = interfaceNode.createRelationshipTo(pNode,
				RelTypes.CONNECTING);
		relationship.setProperty("edgeType", "OWNER");

		// later set this something else if required //summation of class name
		// and package node name
		relationship.setProperty("name",
				interfaceNode.getProperty("canonicalName").toString());
		// System.out.println("relationship Id:"+relationship.getId());
		// System.out.println("relationship Name:"+relationship.getProperty("name").toString());
		edgeHashMap.put(relationship.getId(), relationship.getProperty("name")
				.toString());

		return interfaceNode;
	}

	/**
	 * This method creates a new method node, sets all the specified attributes 
	 * and joins the newly created node to the specified node (cNode) using connecting edge.
	 */
	public Node addMethodNode(GraphDatabaseService graphDb, Node cNode,
			String smallMethodName, String methodName, String modifier,
			String returnType, String parameterList, String body, int lineNumber, Transaction tx) {
		// System.out.println("Creating Method Node::"+methodName);
//		Node mNode = graphDb.createNode(dGraphNodeType.METHOD);
		Node mNode = tx.createNode(dGraphNodeType.METHOD);
		
		mNode.addLabel(dGraphNodeType.METHOD);
		// to support method overloading 
		// adding parameter details to name and canonicalName
	//	System.out.println("ParameterList from addMethodNode "+ parameterList);
		String parameterName= parseParameterList(parameterList);
	//	System.out.println("parameterName from addMethodNode "+ parameterName);
		
		mNode.setProperty("name", smallMethodName+"("+parameterName+")");
		mNode.setProperty("canonicalName", methodName+"("+parameterName+")");

		
		//mNode.setProperty("name", smallMethodName); //original
		//mNode.setProperty("canonicalName", methodName); //original
		
		mNode.setProperty("nodeType", "METHOD");
		mNode.setProperty("modifier", modifier);
		mNode.setProperty("returnType", returnType);
		mNode.setProperty("parameterList", parameterList);
		mNode.setProperty("lineNumber", lineNumber);
		// System.out.println("Method ")
		mNode.setProperty("body", body);

		// System.out.println("mNode Id:"+mNode.getId());
		nodeHashMap.put(mNode.getId(), smallMethodName);
		nodeCanonicalHashMap.put(mNode.getId(), methodName);
		relationship = mNode.createRelationshipTo(cNode, RelTypes.CONNECTING);
		relationship.setProperty("edgeType", "OWNER");
		relationship.setProperty("name",
				methodName + "::"
						+ cNode.getProperty("canonicalName").toString());

		// System.out.println("relationship Id:"+relationship.getId());
		// System.out.println("relationship Name:"+relationship.getProperty("name").toString());
		edgeHashMap.put(relationship.getId(), relationship.getProperty("name")
				.toString());

		return mNode;
	}

	/**
	 * This method creates a new attribute node, sets all the specified attributes 
	 * and joins the newly created node to the specified node (cNode) using connecting edge.
	 */
	public Node addAttributeNode(GraphDatabaseService graphDb, Node cNode,
			String smallAttributeName, String attributeName, String modifier,
			String dataType, String initializer, int lineno, Transaction tx) {
		// System.out.println("Creating Method Node::"+methodName);
//		Node aNode = graphDb.createNode(dGraphNodeType.ATTRIBUTE);
		Node aNode = tx.createNode(dGraphNodeType.ATTRIBUTE);
		
		aNode.addLabel(dGraphNodeType.ATTRIBUTE);
		aNode.setProperty("name", smallAttributeName);
		aNode.setProperty("canonicalName", attributeName);
		aNode.setProperty("nodeType", "ATTRIBUTE");
		aNode.setProperty("modifier", modifier);
		aNode.setProperty("dataType", dataType);
		aNode.setProperty("lineNumber", lineno);
		if (initializer != null)
			aNode.setProperty("initializer", initializer);
		else
			aNode.setProperty("initializer", "null");

		nodeHashMap.put(aNode.getId(), smallAttributeName);// adding canonical
															// name
		nodeCanonicalHashMap.put(aNode.getId(), attributeName);// adding
																// canonical
																// name

		relationship = aNode.createRelationshipTo(cNode, RelTypes.CONNECTING);

		relationship.setProperty("edgeType", "OWNER");
		relationship.setProperty("name", attributeName + "::"
				+ cNode.getProperty("canonicalName").toString());

		// System.out.println("relationship Id:"+relationship.getId());
		// System.out.println("relationship Name:"+relationship.getProperty("name").toString());
		edgeHashMap.put(relationship.getId(), relationship.getProperty("name")
				.toString());

		return aNode;
	}

	
	/**
	 * This method creates a new variable declaration node, sets all the specified attributes 
	 * and joins the newly created node to the specified node (mNode) using connecting edge.
	 */
	public Node addVariableDeclarationNode(GraphDatabaseService graphDb,
			Node mNode, String smallAttributeName, String attributeName,
			String modifier, String dataType, String initializer, int lineNo, Transaction tx) {
		// System.out.println("Creating Method Node::"+methodName);
		//Node aNode = graphDb.createNode(dMethodNodeType.VariableDeclarationNode);
		Node aNode = tx.createNode(dMethodNodeType.VariableDeclarationNode);
		
		aNode.addLabel(dMethodNodeType.VariableDeclarationNode);
		aNode.setProperty("name", smallAttributeName);
		aNode.setProperty("canonicalName", attributeName);
		aNode.setProperty("nodeType", "METHOD-ATTRIBUTE");
		aNode.setProperty("modifier", modifier);
		aNode.setProperty("dataType", dataType);
		aNode.setProperty("lineNumber", lineNo);
		if (initializer != null)
			aNode.setProperty("initializer", initializer);
		else
			aNode.setProperty("initializer", "null");
		// aNode.setProperty( "initializer", initializer );

		methodHashMap.put(aNode.getId(), smallAttributeName);// adding canonical
																// name
		nodeHashMap.put(aNode.getId(), smallAttributeName);
		nodeCanonicalHashMap.put(aNode.getId(), attributeName);// adding
																// canonical
																// name
		relationship = aNode.createRelationshipTo(mNode, methodRelTypes.BODY);

		relationship.setProperty("edgeType", "BODY");
		relationship.setProperty("name", attributeName + "::"
				+ mNode.getProperty("canonicalName").toString());

		// System.out.println("relationship Id:"+relationship.getId());
		// System.out.println("relationship Name:"+relationship.getProperty("name").toString());
		methodEdgeHashMap.put(relationship.getId(),
				relationship.getProperty("name").toString());

		return aNode;
	}

	/**
	 * This method creates a new extends dependency edge, sets all the specified attributes 
	 * and joins the specified nodes using the newly created edge.
	 */
	public void addExtendsDependencyEdge(GraphDatabaseService graphDb,
			Long superClassID, Long subClassID, Transaction tx) {
		// adding edge from superclass id to sub class it
		//Node subClassNode = graphDb.getNodeById(subClassID);
	//	Node superClassNode = graphDb.getNodeById(superClassID);
		
		Node subClassNode = tx.getNodeById(subClassID);
		Node superClassNode = tx.getNodeById(superClassID);
		
		//System.out.println("Adding extends edge from::"
		//		+ subClassNode.getProperty("name") + "to"
		//		+ superClassNode.getProperty("name"));

		relationship = subClassNode.createRelationshipTo(superClassNode,
				RelTypes.DEPENDENCY);
		relationship.setProperty("edgeType", "EXTENDS");
		relationship.setProperty("name",
				subClassNode.getProperty("canonicalName").toString()
						+ "::"
						+ superClassNode.getProperty("canonicalName")
								.toString());
		edgeHashMap.put(relationship.getId(), relationship.getProperty("name")
				.toString());

	}

	/**
	 * This method creates a new implements dependency edge, sets all the specified attributes 
	 * and joins the specified nodes using the newly created edge.
	 */
	public void addImplementsDependencyEdge(GraphDatabaseService graphDb,
			Long interfaceID, Long subClassID, Transaction tx) {
		// adding edge from superclass id to sub class it
//		Node subClassNode = graphDb.getNodeById(subClassID);
//		Node interfaceNode = graphDb.getNodeById(interfaceID);
		
		Node subClassNode = tx.getNodeById(subClassID);
		Node interfaceNode = tx.getNodeById(interfaceID);
		
		
	//	System.out.println("Adding implements edge from::"
	//			+ subClassNode.getProperty("name") + "to"
	//			+ interfaceNode.getProperty("name"));
//
		relationship = subClassNode.createRelationshipTo(interfaceNode,
				RelTypes.DEPENDENCY);
		relationship.setProperty("edgeType", "IMPLEMENTS");
		relationship
				.setProperty(
						"name",
						subClassNode.getProperty("canonicalName").toString()
								+ "::"
								+ interfaceNode.getProperty("canonicalName")
										.toString());
		edgeHashMap.put(relationship.getId(), relationship.getProperty("name")
				.toString());

	}

	/**
	 * This method creates a new imports dependency edge, sets all the specified attributes 
	 * and joins the specified nodes using the newly created edge.
	 */
	public void addImportsDependencyEdge(GraphDatabaseService graphDb,
			Long importID, Long classID, Transaction tx) {
		// adding edge from superclass id to sub class it
	//	Node classNode = graphDb.getNodeById(classID);
	//	Node importClassNode = graphDb.getNodeById(importID);
		
		Node classNode = tx.getNodeById(classID);
		Node importClassNode = tx.getNodeById(importID);
//		System.out.println("Adding imports edge from::"
//				+ classNode.getProperty("name") + "to"
//				+ importClassNode.getProperty("name"));

		relationship = classNode.createRelationshipTo(importClassNode,
				RelTypes.DEPENDENCY);
		relationship.setProperty("edgeType", "IMPORTS");
		relationship.setProperty("name",
				classNode.getProperty("canonicalName").toString()
						+ "::"
						+ importClassNode.getProperty("canonicalName")
								.toString());
		edgeHashMap.put(relationship.getId(), relationship.getProperty("name")
				.toString());

	}

	/**
	 * This method creates a new dependency edge (based on edgeType), sets all the specified attributes 
	 * and joins the specified nodes using the newly created edge.
	 */
	public void addDependencyEdge(GraphDatabaseService graphDb,
			Long superClassID, Long subClassID, String edgeType, Transaction tx) {
		// adding edge from superclass id to sub class it
//		Node subClassNode = graphDb.getNodeById(subClassID);
//		Node superClassNode = graphDb.getNodeById(superClassID);

		Node subClassNode = tx.getNodeById(subClassID);
		Node superClassNode = tx.getNodeById(superClassID);
		// returns true if EDGE exists
		Boolean exists = checkDependencyEdgeExists(graphDb, subClassNode,
				superClassNode, edgeType);

		//System.out.println("From addDependencyEdge:: edge type::"+edgeType);
		if (!exists) {
//			System.out.println("Adding " + edgeType + " edge from:: "
//					+ subClassNode.getProperty("name") + " to "
//					+ superClassNode.getProperty("name"));
			relationship = subClassNode.createRelationshipTo(superClassNode,
					RelTypes.DEPENDENCY);
			relationship.setProperty("edgeType", edgeType);
			relationship.setProperty("name",
					subClassNode.getProperty("canonicalName").toString()
							+ "::"
							+ superClassNode.getProperty("canonicalName")
									.toString());
			edgeHashMap.put(relationship.getId(),
					relationship.getProperty("name").toString());
		}

	}

	/**
	 * This method returns true if a dependency edge of the type specified by
	 * edgeType already exists between specified nodes.  
	 */
	public boolean checkDependencyEdgeExists(GraphDatabaseService graphDb,
			Node subClassNode, Node superClassNode, String edgeType) {
		Iterable<Relationship> relations;
		relations = subClassNode.getRelationships(RelTypes.DEPENDENCY);
		Long otherClassID = (long) -1;
		for (Relationship r : relations) {
			otherClassID = r.getOtherNode(subClassNode).getId();
			if (otherClassID == superClassNode.getId()
					&& (r.getProperty("edgeType").equals(edgeType))) {
				// edge exists
				return true;
			}
		}
		return false;
	}

	public Node checkPackageNodeExists(GraphDatabaseService graphDb,
			Node rootNode, String packName, Transaction tx) {
		Node pNode = tx.findNode(dGraphNodeType.PACKAGE, "name", packName);
		return pNode;
	}

	public Node createIndependentClassNode(GraphDatabaseService graphDb,
			String smallClassName, String className, String imports,
			String packageName, String modifier, String extend,
			String implemented, int lineNumber, Transaction tx) {

		// System.out.println("Creating Class Node::"+className);
//		Node classNode = graphDb.createNode(dGraphNodeType.CLASS);
		Node classNode = tx.createNode(dGraphNodeType.CLASS);
		
		classNode.addLabel(dGraphNodeType.CLASS);
		classNode.setProperty("name", smallClassName);
		classNode.setProperty("canonicalName", className);
		classNode.setProperty("nodeType", "CLASS");
		classNode.setProperty("modifier", modifier);
		classNode.setProperty("imports", imports);
		classNode.setProperty("packageName", packageName);
		classNode.setProperty("lineNumber", lineNumber);
		if (extend != null)
			classNode.setProperty("extends", extend);
		else
			classNode.setProperty("extends", "null");
		classNode.setProperty("implements", implemented);
		// System.out.println("cNode Id:"+classNode.getId());
		nodeHashMap.put(classNode.getId(), smallClassName);// not adding
															// canonical name
		// nodeCanonicalHashMap.put(classNode.getId(), className);

		return classNode;
	}

	public Node createIndependentInterfaceNode(GraphDatabaseService graphDb,
			String smallClassName, String interfaceName, String imports,
			String packageName, String modifier, int lineNumber, Transaction tx) {
		// System.out.println("Creating Class Node::"+className);
//		Node interfaceNode = graphDb.createNode(dGraphNodeType.INTERFACE);
		Node interfaceNode = tx.createNode(dGraphNodeType.INTERFACE);
		
		interfaceNode.addLabel(dGraphNodeType.INTERFACE);
		interfaceNode.setProperty("name", smallClassName);
		interfaceNode.setProperty("canonicalName", interfaceName);
		interfaceNode.setProperty("nodeType", "INTERFACE");
		interfaceNode.setProperty("modifier", modifier);
		interfaceNode.setProperty("imports", imports);
		interfaceNode.setProperty("packageName", packageName);
		interfaceNode.setProperty("lineNumber", lineNumber);
		// System.out.println("cNode Id:"+classNode.getId());
		nodeHashMap.put(interfaceNode.getId(), smallClassName);
		// nodeCanonicalHashMap.put(interfaceNode.getId(), interfaceName);

		return interfaceNode;
	}

	public HashMap getNodeHashMap() {
		return nodeHashMap;
	}

	public HashMap getCanonicalNodeHashMap() {
		return nodeCanonicalHashMap;
	}

	public HashMap getClassCanonicalNodeHashMap() {
		return nodeClassCanonicalHashMap;
	}

	public void addToNodeHashMap() {

	}

	public HashMap getEdgeHashMap() {
		return edgeHashMap;
	}

	public void addToEdgeHashMap() {

	}

	public String parseParameterList(String paramList)
	{
		String param= null;
		String s = null;
		paramList= paramList.substring(1, paramList.length()-2);
		if (paramList != null) {
			String[] temp1;
			String delimiter1 = "[,]";
			temp1 = paramList.split(delimiter1);
			
			for (int i = 0; i < temp1.length; i++) {
				//System.out.println("i=" + i + temp1[i]);
				temp1[i]= temp1[i].trim();
				int index = temp1[i].indexOf(" ");
				s = temp1[i].substring(0, index+1);
				s= s.trim();
				if (param == null)
					param =s;
				else param = param+","+s;
				
			}
		return param;
		}
		return param;
	}
}

