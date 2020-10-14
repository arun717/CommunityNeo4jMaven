package org.jrg.parser;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.HashMap;
import java.util.Vector;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.jrg.callgraph.PlagCheck;
import org.jrg.convertor.ExtendedJRGConvertor;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.io.fs.FileUtils;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the main class responsible for creating the project dependency graph
 * using Neo4j Graph DB.
 * 
 * @author Ritu Arora
 * 
 */
public class ExtendedJRGCreator {
    //private static File dbDir = new File( "neo4jDB/Server" );
	private File dbDir = null;
	public static ArrayList<String> alist1 = new ArrayList<String>();
	public static ArrayList<String> alist2 = new ArrayList<String>();
	public ArrayList<Integer> currQ1 = new ArrayList<Integer>();
	public ArrayList<Integer> currQ2 = new ArrayList<Integer>();
	
	private String DB_PATH_SERVER;
	private String SRC_URL = null;
	private String projectName;
	private DependencyGraphNodes dpGraph;
	private File dependencyFile = null;
	Transaction tx = null;
	
	GraphDatabaseService graphDb;
    private DatabaseManagementService managementService;
    
	Node rootNode;
	String tryBody = null;
	CompilationUnit cu = null;

	public static enum dGraphNodeType implements Label {
		PROJECT, PACKAGE, CLASS, INTERFACE, METHOD, ATTRIBUTE;
	}

	public static enum dMethodNodeType implements Label {
		VariableDeclarationNode, PACKAGE;
	}

	public static enum methodRelTypes implements RelationshipType {
		BODY;
	}

	public static enum RelTypes implements RelationshipType {
		CONNECTING, DEPENDENCY;
	}

	public ExtendedJRGCreator(String dbLocation) {
		// TODO Auto-generated constructor stub
		this.dbDir = new File(dbLocation);
		this.DB_PATH_SERVER = dbLocation;
	}
	
	 // use for measuring time
	 public static void main(String args[]) {
		 start("StackProject", "C:\\ritu\\eclipseWorkspaces\\StackProject","neo4jDB/Server");
		 start("StackProject2", "C:\\ritu\\eclipseWorkspaces\\StackProject2","neo4jDB2/Server");
	 }
	 
	 
	  public static void start(String projectName,String projFilePath,String dbLocation) {
	  
		  ExtendedJRGCreator db= new ExtendedJRGCreator(dbLocation);

		 db.initializeDB(projectName, projFilePath,dbLocation);
		 
	 }
	
	 

	long lEndTime;// System.currentTimeMillis();
	long difference;
	long lStartTime;

	/*
	 * public long getCpuTime( ) { ThreadMXBean bean =
	 * ManagementFactory.getThreadMXBean( ); return
	 * bean.isCurrentThreadCpuTimeSupported( ) ? bean.getCurrentThreadCpuTime( )
	 * : 0L; }
	 */

	/**
	 * This method initializes the Neo4j Graph DB and invokes further methods.
	 * @param pName
	 * @param srcPath
	 */
	public void initializeDB(String pName, String srcPath, String dbLocation) {
		SRC_URL = srcPath;
		lStartTime = System.currentTimeMillis();// getCpuTime( ); //

		try {

			projectName = pName;
			dpGraph = new DependencyGraphNodes();

			clearDb();
//			File dbDir = new File(DB_PATH_SERVER);
	        managementService = new DatabaseManagementServiceBuilder( dbDir ).build();
	        graphDb = managementService.database( DEFAULT_DATABASE_NAME );
	        registerShutdownHook( managementService );
	        
//			GraphDatabaseFactory graphFactory = new GraphDatabaseFactory();
//			GraphDatabaseBuilder graphBuilder = graphFactory.newEmbeddedDatabaseBuilder(dbDir);
//			GraphDatabaseBuilder graphBuilder = graphFactory.newEmbeddedDatabaseBuilder(DB_PATH_SERVER);
//			graphDb = graphBuilder.newGraphDatabase();
//			registerShutdownHook(graphDb);

			initializeDependencyEdgesFile();

			if (dependencyFile == null)
				System.out.println(" Dp is  null after initializeDependencyEdgesFile");

			createDB();

			writeHashTableToFile(dpGraph.getCanonicalNodeHashMap());
			
			// use below line if you want to regenerate java file from graph
			ExtendedJRGConvertor convertor = new ExtendedJRGConvertor(graphDb);
			
			
			//below line calls the graph similarity checker code
			
			int progNo =-1;
			if(dbLocation.equalsIgnoreCase("neo4jDB/Server"))
				progNo = 1;
			if(dbLocation.equalsIgnoreCase("neo4jDB2/Server"))
				progNo = 2;
			PlagCheck pc = new PlagCheck(graphDb,progNo,alist1,alist2,currQ1,currQ2);
			
			//convertor.createJavaProject();
			System.out.println("calling before shutdown of db");
			shutDown(graphDb);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Out of DB");
		lEndTime = System.currentTimeMillis();// getCpuTime( );
		difference = lEndTime - lStartTime;

		System.out.println("Elapsed nanoseconds: " + difference +"\n\n");
	}

	/**
	 * Creates the project root node and returns the same.
	 * Invokes further methods for creating the entire graph.
	 * @return
	 * @throws Exception
	 */
	public Node createDB() throws Exception {

		try {
			System.out.println("creating transaction object");
			tx = graphDb.beginTx();
			createRootNode(tx);
			parseDirectoryForConnectingGraph();
			lEndTime = System.currentTimeMillis();// getCpuTime( );
			difference = lEndTime - lStartTime;

			System.out
					.println("Elapsed nanoseconds after creating connecting graph: "
							+ difference);

			parseDirectoryForDependencyGraph();
			lEndTime = System.currentTimeMillis();// getCpuTime( );
			difference = lEndTime - lStartTime;

			System.out
					.println("Elapsed nanoseconds after creating dependency graph: "
							+ difference);
			createAttributeDependencyGraph();
			lEndTime = System.currentTimeMillis();// getCpuTime( );
			difference = lEndTime - lStartTime;

			System.out
					.println("Elapsed nanoseconds after creating attribute dependency graph: "
							+ difference);
			createMethodAttributeDependencyGraph();

			lEndTime = System.currentTimeMillis();// getCpuTime( );
			difference = lEndTime - lStartTime;

			System.out
					.println("Elapsed nanoseconds after creating method attribute  dependency graph: "
							+ difference);
			System.out.println("created graph");
			// START SNIPPET: transaction
//			tx.success();
	          tx.commit();
		} catch (Exception e) {
			if (tx != null)
				throw e;
			e.printStackTrace();
		} finally {

			if (tx != null) {
				tx.close();
				System.out.println("Closing transaction object");
			}
		}
		return rootNode;
	}

	/**
	 * Creates the root node (project node) and sets all the required properties.
	 */
	public void createRootNode(Transaction tx) {
//		rootNode = graphDb.createNode(dGraphNodeType.PROJECT);
		rootNode = tx.createNode(dGraphNodeType.PROJECT);
		
		System.out.println("created rootNode object");

		rootNode.setProperty("name", projectName);
		rootNode.setProperty("nodeType", "PROJECT");
		rootNode.setProperty("canonicalName", projectName);
		rootNode.addLabel(dGraphNodeType.PROJECT);
	}

	/**
	 * This method parses the entire directory structure of the cloned repository
	 * to create the graph. In order to parse the directory structure, it makes use
	 * of the Finder class. 
	 * This method creates the connecting graph of the project repository.
	 */
	public void parseDirectoryForConnectingGraph() {

		Path startingDir = Paths.get(SRC_URL);
		
		Finder finder = new Finder(this);
		try {
			Files.walkFileTree(startingDir, finder);
		} catch (IOException e) {
			e.printStackTrace();
		}
		finder.done();
	}

	public void parseDirectoryForDependencyGraph() {

		Path startingDir = Paths.get(SRC_URL);
		
		FinderDependency finder = new FinderDependency(this);
		try {
			Files.walkFileTree(startingDir, finder);
		} catch (IOException e) {

			e.printStackTrace();
		}
		finder.done();
	}

	public void createConnectingGraph(File f) throws Exception {
		
		// call createGraphAST for each file
		String filePath = null;
		Node cNode = null;
		// for (File f : files ) {
		filePath = f.getAbsolutePath();
		if (f.isFile() && (f.getName().contains(".java"))) {

			// System.out.println("In file: "+ f.getName());
			CompilationUnit cu = parse(readFileToString(filePath), f.getName());
			String className = null;
			String smallClassName = null; // (f.getName()).substring(0, index);;
			String packName = null;
			Node packageNode = null;
			if (cu.getPackage() != null) {
				// System.out.println("package name is not null");
				packName = parsePackageName(cu.getPackage().toString());
				// className= packName+"."+(f.getName()).substring(0, index);

				// create package node, if the same does not exist
				String projName = rootNode.getProperty("name").toString();
				packageNode = dpGraph.addPackageNode(graphDb, rootNode,
						packName, projName, tx);
			} else {
				// className= (f.getName()).substring(0, index);
				packName = "null";
			}
			// System.out.println("cu.types:: "+cu.types());
			List<AbstractTypeDeclaration> types = cu.types();
			String modifier = null;
			Boolean isInterface = false;
			String extend = null;
			String implemented = null;
			TypeDeclaration t;
			for (AbstractTypeDeclaration t1 : types) {
				if (t1 instanceof org.eclipse.jdt.core.dom.TypeDeclaration) {
					t = (org.eclipse.jdt.core.dom.TypeDeclaration) t1;
					// System.out.println("Instance of org.eclipse.jdt.core.dom.TypeDeclaration");

					// System.out.println("Creating Class");
					isInterface = t.isInterface();
					// System.out.println("Name of TypeDeclaration:: "+
					// t.getName());
					smallClassName = t.getName().toString();
					if (packName.equals("null"))
						className = t.getName().toString();
					else
						className = packName + "." + t.getName().toString();
					modifier = Modifier.toString(t.getModifiers());
					Type superClass = t.getSuperclassType();
					if (superClass != null)
						extend = superClass.toString();

					// System.out.println("SuperClassType::"+extend);
					List implementedList = t.superInterfaceTypes();
					// System.out.println("SuperImplemented::"+implementedList);

					if (!implementedList.isEmpty()) {
						implemented = implementedList.toString();
					} else {
						implemented = "[null]";
						// System.out.println("Implemented list is null");
					}
					int lineNumber = 0;
					if (t != null && cu != null) {
						lineNumber = cu.getLineNumber(t.getStartPosition());
					}
					if (isInterface) {
						if (packageNode != null)
							cNode = dpGraph.addConnectingInterfaceNode(graphDb,
									packageNode, smallClassName, className, cu
											.imports().toString(), packName,
									modifier, lineNumber,tx);
						else
							cNode = dpGraph.addConnectingInterfaceNode(graphDb,
									rootNode, smallClassName, className, cu
											.imports().toString(), packName,
									modifier, lineNumber, tx);
						// writeToFile(smallClassName+" INTERFACE ");
					} else {
						if (packageNode != null)
							cNode = dpGraph.addConnectingClassNode(graphDb,
									packageNode, smallClassName, className, cu
											.imports().toString(), packName,
									modifier, extend, implemented, lineNumber, tx);
						else
							cNode = dpGraph.addConnectingClassNode(graphDb,
									rootNode, smallClassName, className, cu
											.imports().toString(), packName,
									modifier, extend, implemented, lineNumber, tx);
						// writeToFile(smallClassName+" CLASS ");
					}

					// System.out.println("Class modifiers::"+Modifier.toString(t.getModifiers()));
					FieldDeclaration[] fieldArr = t.getFields();
					String attributeModifier = null;
					String attributeType = null;
					for (FieldDeclaration fArr : fieldArr) {
						attributeType = fArr.getType().toString();
						// System.out.println("field type::"+attributeType);
						attributeModifier = Modifier.toString(fArr
								.getModifiers());
						// System.out.println("field modifiers::"+Modifier.toString(fArr.getModifiers()));

						List<VariableDeclarationFragment> frag = fArr
								.fragments();
						int i = 0;
						String initializer = null;
						String smallAttributeName = null;
						String attributeName = null;
						Node aNode = null;
						for (VariableDeclarationFragment fd : frag) {
							Object o = fArr.fragments().get(i);
							i++;

							if (o instanceof VariableDeclarationFragment) {
								smallAttributeName = ((VariableDeclarationFragment) o)
										.getName().toString();
								ChildPropertyDescriptor s2 = ((VariableDeclarationFragment) o)
										.getNameProperty();

								Expression s3 = ((VariableDeclarationFragment) o)
										.getInitializer();
								if (s3 != null)
									initializer = s3.toString();
								int s4 = ((VariableDeclarationFragment) o)
										.getExtraDimensions();

								// System.out.println("SimpleName()::"+smallAttributeName);
								attributeName = className + "."
										+ smallAttributeName;
								// System.out.println("getInitializer::"+s3);

								lineNumber = 0;
								if (fd != null && cu != null)
									lineNumber = cu.getLineNumber(fd
											.getStartPosition());
								aNode = dpGraph.addAttributeNode(graphDb,
										cNode, smallAttributeName,
										attributeName, attributeModifier,
										attributeType, initializer, lineNumber, tx);
							} // if
						}// for variable declaration
					}// field declaration
				}// if t of typeDecalration

			}// type declaration
				// for each file, get its Methods and add nodes
			if (cNode != null)
				getMethodGraph(cu, dpGraph, graphDb, cNode);
		}// for all java files
		// }//for all files
	}// for createCOnnectingGraph

	/*
	 * public void writeToFile(String fileName) { try { File configfile = new
	 * File("D:\\ClassesCreated.txt");
	 * 
	 * if (!configfile.exists()) { configfile.createNewFile(); }
	 * 
	 * FileWriter fw = new FileWriter(configfile.getAbsoluteFile(), true);
	 * BufferedWriter bw = new BufferedWriter(fw); bw.write(fileName);
	 * bw.newLine(); bw.close();
	 * 
	 * } catch (IOException e) { e.printStackTrace(); } }
	 */

	public void createDependencyGraph(File f) throws Exception {
		// read file content into a string
		// call createGraphAST for each file
		HashMap<Long, String> nodeHashMap = dpGraph.getNodeHashMap();

		// System.out.println("Creating Dependency Graph");
		String filePath = null;
		Node cNode = null;
		String className = null;
		String smallClassName = null;
		String packName = null;
		// for (File f : files ) {
		filePath = f.getAbsolutePath();
		// System.out.println(filePath);
		if (f.isFile() && (f.getName().contains(".java"))) {
			// System.out.println("In file: "+ f.getName());

			CompilationUnit cu = parse(readFileToString(filePath), f.getName());

			// smallClassName= (f.getName()).substring(0, index);;
			packName = null;
			if (cu.getPackage() != null) {
				// System.out.println("package name is not null");
				packName = parsePackageName(cu.getPackage().toString());
				// className= packName+"."+(f.getName()).substring(0, index);
			} else {
				// className= (f.getName()).substring(0, index);
				packName = "null";
			}

			List<AbstractTypeDeclaration> types = cu.types();

			boolean isInterface = false;
			Long idSuperNode = (long) -1;
			Long idCurrentClassNode = (long) -1;
			TypeDeclaration t;
			for (AbstractTypeDeclaration t1 : types) {

				if (t1 instanceof org.eclipse.jdt.core.dom.TypeDeclaration) {
					t = (org.eclipse.jdt.core.dom.TypeDeclaration) t1;
					// System.out.println("Instance of org.eclipse.jdt.core.dom.TypeDeclaration");
					isInterface = t.isInterface();
					Type superClass = t.getSuperclassType();
					// System.out.println("SuperClassType::"+superClass);
					smallClassName = t.getName().toString();

					if (packName.equals("null"))
						className = t.getName().toString();
					else
						className = packName + "." + t.getName().toString();
					if (superClass != null) {
						// search for Class node in hashmap
						// System.out.println("Searching for superClassNode::"+superClass);
						idSuperNode = searchClassNode(superClass.toString(),
								nodeHashMap);
						if (idSuperNode != -1) {
							// super class node found
							// create dependency edge from current node to super
							// class node
							idCurrentClassNode = searchClassNode(
									smallClassName, nodeHashMap);

							writeDependencyEdgesToFile(smallClassName,
									superClass.toString(), "EXTENDS");
							// write to dependencyEdge file
							// writeDependencyEdgesToFile(idCurrentClassNode,
							// idSuperNode, "EXTENDS");
							dpGraph.addExtendsDependencyEdge(graphDb,
									idSuperNode, idCurrentClassNode, tx);
						}
					}// if (superClass !=null)

					// System.out.println("SuperInterfaceType::"+t.superInterfaceTypes());
					List<SimpleType> interfacesImplemented = t
							.superInterfaceTypes();
					// iterate over list and search for all nodes and add
					// depedency edges
					Long idinterfaceNode = (long) -1;
					if (!interfacesImplemented.isEmpty()) {
						// code goes here....
						for (Type interfaces : interfacesImplemented) {
							// System.out.println("Interfaces::"+interfaces);
							// search for the existence of interface in graph
							idinterfaceNode = searchClassNode(
									interfaces.toString(), nodeHashMap);
							if (idinterfaceNode != -1) {
								// interface node found
								// create dependency edge from current node to
								// interface node
								idCurrentClassNode = searchClassNode(
										smallClassName, nodeHashMap);

								writeDependencyEdgesToFile(smallClassName,
										interfaces.toString(), "IMPLEMENTS");
								// write to dependencyEdge file
								// writeDependencyEdgesToFile(idCurrentClassNode,
								// idinterfaceNode, "IMPLEMENTS");
								dpGraph.addImplementsDependencyEdge(graphDb,
										idinterfaceNode, idCurrentClassNode, tx);
							}// if (idinterfaceNode != -1)
						}// for (SimpleType interfaces: interfacesImplemented)

					}// if (!interfacesImplemented.isEmpty())

					// create import edges
					String imports = cu.imports().toString();
					if (!imports.equals("[]")) {
						// System.out.println("imports:: "+imports);
						Vector importsVector = parseImports(imports);
						Enumeration enumImports = importsVector.elements();
						String importClass = null;
						Long importClassId = (long) -1;
						while (enumImports.hasMoreElements()) {
							importClass = enumImports.nextElement().toString();
							// System.out.println("Enumeration:: "+
							// importClass);
							importClassId = searchClassNode(importClass,
									nodeHashMap);
							if (importClassId != -1) {
								// interface node found
								// create dependency edge from current node to
								// interface node
								idCurrentClassNode = searchClassNode(
										smallClassName, nodeHashMap);

								writeDependencyEdgesToFile(smallClassName,
										importClass, "IMPORTS");
								// write to dependencyEdge file
								// writeDependencyEdgesToFile(idCurrentClassNode,
								// importClassId, "IMPORTS");

								dpGraph.addImportsDependencyEdge(graphDb,
										importClassId, idCurrentClassNode, tx);
							}// if (idinterfaceNode != -1)
						}

					}
				}// if
				else
					System.out.println("Not creating class for:: "
							+ f.getName());
			}// for (TypeDeclaration t: types)

			// create calls edges
			// System.out.println("ClassName:::"+className);
			// if (className != null)
			visitFileAST(dpGraph, graphDb, cu, className);
		}// if(f.isFile() && (f.getName().contains(".java"))){
		// }//for (File f : files ) {
	}

	/*****************************************************************************/
	public void createAttributeDependencyGraph() {
		HashMap<Long, String> nodeHashMap = dpGraph.getNodeHashMap();
		// create uses dependency between class and class based on type of
		// attribute node
		// obtain all class nodes from graph
		Node attributeNode;
		String attributeType;
		Iterable<Relationship> relations;
		Node ownerClassNode;
		Long attributeNodeClassId = (long) -1;
		Long otherNodeClassId = (long) -1;

		ResourceIterator<Node> aNodes = tx.findNodes(dGraphNodeType.ATTRIBUTE);
		while (aNodes.hasNext()) {
			attributeNode = aNodes.next();
			attributeType = (String) attributeNode.getProperty("dataType");
			// System.out.println("dataType::"+attributeType);
			relations = attributeNode.getRelationships(RelTypes.CONNECTING);

			for (Relationship r : relations) {
				ownerClassNode = r.getOtherNode(attributeNode);
				// System.out.println("classNode::"+ownerClassNode.getProperty("name"));
				attributeNodeClassId = ownerClassNode.getId();
				// System.out.println("classNodeID::"+ownerClassNode.getId());
				// System.out.println("classNode::"+r.getOtherNode(attributeNode));
				otherNodeClassId = searchClassNode(attributeType, nodeHashMap);
				if (otherNodeClassId != -1) {
					// class node found
					// create dependency edge from current class node to class
					// node
					// uses relationship
					// System.out.println("Creating edge from Class to class for attribute node");

					// write to dependencyEdge file
					// writeDependencyEdgesToFile(attributeNodeClassId,
					// otherNodeClassId, "USES");

					dpGraph.addDependencyEdge(graphDb, otherNodeClassId,
							attributeNodeClassId, "USES", tx);
				}// if (idinterfaceNode != -1)
			}
		}
	}// public void createAttributeDependencyGraph()

	/*****************************************************************************/
	public void createMethodAttributeDependencyGraph() {
		HashMap<Long, String> nodeHashMap = dpGraph.getNodeHashMap();
		// create uses dependency between method and class based on type of
		// attribute node
		// obtain all attribute nodes from graph
		Node attributeNode1;
		String attributeType1;
		Node attributeMethodNode;
		Long attributeMethodNodeId = (long) -1;
		Long otherClassNodeId = (long) -1;
		Iterable<Relationship> relations;
		ResourceIterator<Node> methodANodes = tx
				.findNodes(dMethodNodeType.VariableDeclarationNode);
		while (methodANodes.hasNext()) {
			attributeNode1 = methodANodes.next();
			attributeType1 = (String) attributeNode1.getProperty("dataType");
			// System.out.println("dataType::"+attributeType);
			relations = attributeNode1.getRelationships(methodRelTypes.BODY);

			for (Relationship r : relations) {
				attributeMethodNode = r.getOtherNode(attributeNode1);
				// System.out.println("classNode::"+ownerClassNode.getProperty("name"));
				attributeMethodNodeId = attributeMethodNode.getId();
				// System.out.println("classNodeID::"+ownerClassNode.getId());
				// System.out.println("classNode::"+r.getOtherNode(attributeNode));
				otherClassNodeId = searchClassNode(attributeType1, nodeHashMap);
				if (otherClassNodeId != -1) {
					// class node found
					// create dependency edge from current method node to class
					// node
					// uses relationship
					// write to dependencyEdge file
					// writeDependencyEdgesToFile(attributeMethodNodeId,
					// otherClassNodeId, "USES");

					dpGraph.addDependencyEdge(graphDb, otherClassNodeId,
							attributeMethodNodeId, "USES", tx);
				}// if (idinterfaceNode != -1)
			}
		}
		/*****************************************************************************/
	}//

	public Long searchClassNode(String className,
			HashMap<Long, String> nodeHashMap) {
		// get id of class name in nodeHashMap else return -1
		Long id = (long) -1;

		for (Map.Entry<Long, String> entry : nodeHashMap.entrySet()) {
			if (Objects.equals(className, entry.getValue())) {
				id = entry.getKey();
				break;
			}
		}
		return id;
	}

	public Long searchHashNode(String nodeName,
			HashMap<Long, String> nodeHashMap) {
		// get id of class name in nodeHashMap else return -1
		Long id = (long) -1;

	//	System.out.println("NodeName from 708::"+nodeName);
		for (Map.Entry<Long, String> entry : nodeHashMap.entrySet()) {
			String entryValue= entry.getValue();
	//		System.out.println("NodeName from 711::"+entryValue);
		
			if (Objects.equals(nodeName, entryValue)) {
			//if (nodeName.equals(entryValue)) {
					
				id = entry.getKey();
		//		System.out.println("ID from 717::"+id);
				break;
			}
		}
	//	System.out.println("ID from 721::"+id);
		return id;
	}

	public String readFileToString(String filePath) throws IOException {
		StringBuilder fileData = new StringBuilder(1000);
		BufferedReader reader = new BufferedReader(new FileReader(filePath));

		char[] buf = new char[10];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {

			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		reader.close();
		return fileData.toString();
	}

	// use ASTParse to parse string
	public CompilationUnit parse(String str, String fileName) {
		// each str contains the str content of a single java file
		//ASTParser parser = ASTParser.newParser(AST.JLS8);
		// @ arun maurya   --- commented JLS8 and changed to JLS4 
		ASTParser parser = ASTParser.newParser(AST.JLS4);

		parser.setUnitName(fileName);
		String[] sources = { "" };
		String[] classpath = { "" };
		// setEnvironment for resolve bindings even if the args is empty
		parser.setEnvironment(classpath, sources, new String[] { "UTF-8" },
				true);

		parser.setSource(str.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);

		// In order to parse 1.5 code, some compiler options need to be set to
		// 1.5
		Map options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_6, options);
		parser.setCompilerOptions(options);

		final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

		// Document document= new Document(cu.getSource());
		return cu;
	}

/*	private void register


Hook(final GraphDatabaseService graphDb) {
		// Registers a shutdown hook for the Neo4j instance so that it
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the
		// running application).
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}
*/
	public void getMethodGraph(final CompilationUnit cu,
			final DependencyGraphNodes dpGraph,
			final GraphDatabaseService graphDb, final Node cNode) {

		cu.accept(new ASTVisitor() {
			String mName = null;
			String smallMethodName = null;
			String methodBody = null;
			List<MethodDeclaration> methods = new ArrayList<MethodDeclaration>();

			public boolean visit(MethodDeclaration node) {
				methods.add(node);
				// System.out.println("MethodName:: "+node.getName());
				// if (node.getName().equals("addToNodeHashMap"))
				// System.out.println("MethodBody:: "+node.getBody());
				int mod = node.getModifiers(); // get the int value of modifier

				smallMethodName = node.getName().toString();
				mName = cNode.getProperty("canonicalName") + "."
						+ smallMethodName;

				// if (node.getBody() !=null) methodBody=
				// transformMethodBody(cu, node.getBody());
				// else methodBody="null";

				if (node.getBody() != null)
				{
					methodBody = node.getBody().toString();
					//System.out.println("Start of method body    \n"+methodBody+ "\n");
				}
				else
					methodBody = "null";

				// add method node
				String param = null;
				List parameterList = node.parameters();
				if (!parameterList.isEmpty())
				{
					Object [] paraArray= parameterList.toArray();
				/*	for (int i=0; i < paraArray.length; i++)
							{
								System.out.println("ARRAY:: "+paraArray[i]);
							}*/
				}
				
				if (!parameterList.isEmpty())
					param = parameterList.toString();
				else
					param = "null";

				String returnTypeString = null;
				Type returnType = node.getReturnType2();
				if (returnType != null)
					returnTypeString = returnType.toString();
				else
					returnTypeString = "null";

				int lineNumber = 0;
				if (node != null && cu != null) {
					lineNumber = cu.getLineNumber(node.getStartPosition());
				}
				Node mNode = dpGraph.addMethodNode(graphDb, cNode,
						smallMethodName, mName, Modifier.toString(mod),
						returnTypeString, param, methodBody, lineNumber,tx);
				if (node.getBody() != null)
					visitMethodBlock(dpGraph, graphDb, node.getBody(), mNode);
				return false; // do not continue
			}

			public List<MethodDeclaration> getMethods() {
				return methods;
			}
		});
	}

	public void visitMethodBlock(final DependencyGraphNodes dpGraph,
			final GraphDatabaseService graphDb, Block methodBlock,
			final Node mNode) {
		methodBlock.accept(new ASTVisitor() {
			public boolean visit(VariableDeclarationStatement node) {

				String attributeType = node.getType().toString();
				// System.out.println("Variable::"+attributeType);

				String attributeModifier = Modifier.toString(node
						.getModifiers());
				// System.out.println("Modifier::"+attributeModifier);

				List<VariableDeclarationFragment> fd = node.fragments();
				int i = 0;
				for (VariableDeclarationFragment fArr : fd) {
					Object o = node.fragments().get(i);
					i++;

					if (o instanceof VariableDeclarationFragment) {
						String smallAttributeName = ((VariableDeclarationFragment) o)
								.getName().toString();
						String attributeName = mNode
								.getProperty("canonicalName")
								+ "."
								+ smallAttributeName;
						ChildPropertyDescriptor s2 = ((VariableDeclarationFragment) o)
								.getNameProperty();

						Expression s = ((VariableDeclarationFragment) o)
								.getInitializer();
						String initializer = null;
						if (s != null)
							initializer = ((VariableDeclarationFragment) o)
									.getInitializer().toString();
						else
							initializer = "null";
						// int s4 = ((VariableDeclarationFragment)
						// o).getExtraDimensions();

						// System.out.println("SimpleName()::"+smallAttributeName);
						// System.out.println("CompleteName::"+attributeName);
						// System.out.println("getInitializer::"+initializer);

						// create VariableDeclarationNode
						int lineNumber = 0;
						if (node != null && cu != null)
							lineNumber = cu.getLineNumber(node
									.getStartPosition());
						dpGraph.addVariableDeclarationNode(graphDb, mNode,
								smallAttributeName, attributeName,
								attributeModifier, attributeType, initializer,
								lineNumber, tx);
					}
				}
				return false;
			}
		});
	}

	public void visitFileAST(final DependencyGraphNodes dpGraph,
			final GraphDatabaseService graphDb, final CompilationUnit cu,
			final String className) {
		final HashMap<Long, String> nodeCanonicalHashMap = dpGraph
				.getCanonicalNodeHashMap();
		// visit each method invocation node
		cu.accept(new ASTVisitor() {
			String currentMethodName = null;
			String invokedMethodName = null;
			String currentParentName = null;

			public boolean visit(MethodDeclaration node) {

				return true;
			}

			public boolean visit(ClassInstanceCreation node) {

				String classInstanceCreation = node.getType().toString();
				ITypeBinding typeBinding = node.resolveTypeBinding();
				if (typeBinding != null) {
					if (typeBinding.getQualifiedName() != null)
						classInstanceCreation = typeBinding.getQualifiedName();

				}
				MethodDeclaration parentMethodDeclarationNode = (MethodDeclaration) getParentMethodDeclarationNode(node);
				if (parentMethodDeclarationNode != null)
					currentParentName = parse(className) + "."
							+ parentMethodDeclarationNode.getName().toString();
				else
					currentParentName = parse(className);
				// create node from currentParentname to node.getType() if
				// exists

				writeDependencyEdgesToFile(currentParentName,
						classInstanceCreation, "USES");

				// Long invokeClassNodeId = searchNode(graphDb,
				// dGraphNodeType.CLASS, "canonicalName",
				// classInstanceCreation);
				Long invokeClassNodeId = searchHashNode(classInstanceCreation,
						nodeCanonicalHashMap);
				// Long currentNodeId = searchNode(graphDb,
				// dGraphNodeType.METHOD, "canonicalName", currentParentName);
				Long currentNodeId = searchHashNode(currentParentName,
						nodeCanonicalHashMap);

				if (currentNodeId == (long) -1)
					currentNodeId = searchHashNode(currentParentName,
							nodeCanonicalHashMap);
				;// searchNode(graphDb, dGraphNodeType.CLASS, "canonicalName",
					// currentParentName);

				if ((invokeClassNodeId != (long) -1)
						&& (currentNodeId != (long) -1)) {
					// System.out.println("Creating edge from ClassInstanceCreation");

					// write to dependencyEdge file
					// writeDependencyEdgesToFile(currentNodeId,
					// invokeClassNodeId, "USES");

					dpGraph.addDependencyEdge(graphDb, invokeClassNodeId,
							currentNodeId, "USES", tx);
				}
				return true;
			}

			public boolean visit(MethodInvocation node) {
				// System.out.println("MethodInvocation Identifier: "+
				// node.getName().getIdentifier());
				final HashMap<Long, String> nodeCanonicalHashMap = dpGraph.getCanonicalNodeHashMap(); // new
				Expression expression = node.getExpression();
				if (expression != null) {
					ITypeBinding typeBinding = expression.resolveTypeBinding();

					if (typeBinding != null) {
						IType type = (IType) typeBinding.getJavaElement();

						// System.out.println("Type: " +
						// typeBinding.getQualifiedName());
						invokedMethodName = typeBinding.getQualifiedName()
								+ "." + node.getName();
					}
				}

				// System.out.println("invokedMethodName: " +invokedMethodName);
				// get parent nodes till you reach the MethodDeclaration node
				MethodDeclaration parentMethodDeclarationNode = (MethodDeclaration) getParentMethodDeclarationNode(node);
				if (parentMethodDeclarationNode != null)
					currentMethodName = parse(className) + "."
							+ parentMethodDeclarationNode.getName().toString();
				else
					currentMethodName = parse(className);
				// System.out.println("currentMethodName: "+currentMethodName);

		//		System.out.println ("Going to write to dependecny file from line 991");
				writeDependencyEdgesToFile(currentMethodName,
						invokedMethodName, "CALLS");

				// create calls edge from currentMethodName to invokedMethodName
				// Long invokeMethodNodeId = searchNode(graphDb,
				// dGraphNodeType.METHOD, "canonicalName", invokedMethodName);
				Long invokeMethodNodeId = searchHashNode(invokedMethodName,nodeCanonicalHashMap);
				// Long currentMethodNodeId = searchNode(graphDb,
				// dGraphNodeType.METHOD, "canonicalName", currentMethodName);
				Long currentMethodNodeId = searchHashNode(currentMethodName, nodeCanonicalHashMap);

		//		System.out.println("From:: 1005::"+invokedMethodName+invokeMethodNodeId);
		//		System.out.println("From:: 1005::"+currentMethodName+currentMethodNodeId);
				if ((invokeMethodNodeId != (long) -1)
						&& (currentMethodNodeId != (long) -1)) {
					// System.out.println("Creating edge from MethodInvocation");
					// write to dependencyEdge file
					// writeDependencyEdgesToFile(currentMethodNodeId,
					// invokeMethodNodeId, "CALLS");

				//	System.out.println ("Going to create dependecny edge from line 1011");
					dpGraph.addDependencyEdge(graphDb, invokeMethodNodeId,
							currentMethodNodeId, "CALLS", tx);
				}
				return true;
			}
		});
	}

	/*
	 * public Long searchNode(final GraphDatabaseService graphDb, Label
	 * nodeType, String key, String nodeName ) { //returns the id of the last
	 * node found //assumes only one such node exists found //modify to search
	 * on the canonical name Long nodeId = (long)-1; ResourceIterator<Node>
	 * nodes= graphDb.findNodes(nodeType, key, nodeName); while (nodes.hasNext()
	 * ) { nodeId= nodes.next().getId(); } return nodeId; }
	 */

	public ASTNode getParentMethodDeclarationNode(ASTNode node) {
		if (node instanceof MethodDeclaration)
			return (ASTNode) (node);
		else {
			if (node != null)
				return (getParentMethodDeclarationNode(node.getParent()));
			else
				return null;
		}

	}

	public String transformMethodBody(final CompilationUnit cu,
			Block methodBlock) {
		String methodBody = null;

		Block block = methodBlock;

		if (block != null && cu != null) {
			int lineNumber = cu.getLineNumber(block.getStartPosition()) - 1;

			// System.out.println("Line no:: "+lineNumber);
			List<Statement> statements = block.statements();

			int i = 0;
			String str = null;

			for (Statement st : statements) {
				if (st != null) {
					str = block.statements().get(i).toString();
					if (st instanceof TryStatement) {
						st.accept(new ASTVisitor() {
							String body = null;
							Block finallyBlock;
							List catchBody = null;
							String strTry = null;

							public boolean visit(TryStatement node) {

								System.out.println("TryBlock: "
										+ node.getBody());
								body = "try" + node.getBody();
								Block b = node.getBody();
								finallyBlock = node.getFinally();
								catchBody = node.catchClauses();
								if (finallyBlock == null && catchBody != null)
									strTry = body + catchBody.toString();
								else if (finallyBlock != null
										&& catchBody != null)
									strTry = body
											+ catchBody.toString()
											+ finallyBlock.statements()
													.toString();

								// System.out.println("TryBlock: " + strTry);
								tryBody = strTry;
								return true;

							}

						});
					}

					if (tryBody == null) {
						if (methodBody == null)
							methodBody = "[" + lineNumber + "]:" + str;
						else
							methodBody = methodBody + "[" + lineNumber + "]:"
									+ str;
					} else {
						if (methodBody == null)
							methodBody = "[" + lineNumber + "]:" + tryBody;
						else
							methodBody = methodBody + "[" + lineNumber + "]:"
									+ tryBody;
						// need to increment line No and parse trybody
					}

					lineNumber++;
					i++;
					tryBody = null;
				}
			}
		}
		if (methodBody == null)
			return "null";
		else
			return methodBody;
	}

	public void transformNewMethodBody(final CompilationUnit cu,
			Block methodBlock) {
		String body = methodBlock.statements().toString();
//		ASTParser parser = ASTParser.newParser(AST.JLS3);
		ASTParser parser = ASTParser.newParser(AST.JLS4);

		body = parseMethodBody(body);
		System.out.println("Body::" + body);
		parser.setSource(body.toCharArray());

		parser.setKind(ASTParser.K_STATEMENTS);

		Block block = (Block) parser.createAST(null);

		// here can access the first element of the returned statement list
		String str = block.statements().get(0).toString();

		System.out.println(str);

		block.accept(new ASTVisitor() {

			public boolean visit(SimpleName node) {

				System.out.println("Name: " + node.getFullyQualifiedName());

				return true;
			}

		});
	}

	public String parseMethodBody(String body) {
		return (body.replaceAll(",", "\n"));
	}

	public String parsePackageName(String pName) {
		String packName = null;

		int indexPackage = pName.lastIndexOf("package") + 1;
		pName = pName.substring(indexPackage, pName.length());
		// System.out.println("pName:::"+pName);

		int index = pName.indexOf(" ") + 1;
		packName = pName.substring(index, pName.length() - 2);
		// System.out.println("PackageName:::"+packName);
		return packName;
	}

	void shutDown(GraphDatabaseService graphDb) {
		System.out.println("Shutting down database ...");
		managementService.shutdown();
		System.out.println("DB server shuting down complete");
	}

	private void clearDb() {
		
		try {
			if (new File(DB_PATH_SERVER).exists()) {
			    // return the new DB_PATH_SERVER name
				DB_PATH_SERVER = "neo4jDB2/Server";
				return;
			}
			else
			{
				FileUtils.deleteRecursively(new File(DB_PATH_SERVER));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void writeHashTableToFile(HashMap<Long, String> nodeMap) {
		// write hash to file
		try {
			File configfile = new File("neo4jDB/HashMap.txt");

			if (!configfile.exists()) {
				configfile.createNewFile();
			} else {
				// delete existing file and create new
				configfile.delete();
				configfile.createNewFile();
			}

			FileWriter fw = new FileWriter(configfile.getAbsoluteFile(), true);
			BufferedWriter bw = new BufferedWriter(fw);

			Iterator entries = nodeMap.entrySet().iterator();

			while (entries.hasNext()) {
				Entry thisEntry = (Entry) entries.next();
				Object key = thisEntry.getKey();
				Object value = thisEntry.getValue();
				bw.write(key + ":" + value);
				bw.newLine();
			}

			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void initializeDependencyEdgesFile() {

		// write hash to file
		try {
			dependencyFile = new File("neo4jDB/serverEdges.txt");

			if (!dependencyFile.exists()) {
				dependencyFile.createNewFile();
			} else {
				// delete existing file and create new
				dependencyFile.delete();
				dependencyFile.createNewFile();
			}

			if (dependencyFile == null)
				System.out
						.println(" Dp is null from initializeDependencyEdgesFile");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/*
	 * public void writeDependencyEdgesToFile(long fromNode, long toNode, String
	 * edgeType) { //write hash to file try {
	 * 
	 * if (dependencyFile == null)
	 * System.out.println(" dp is  null in writeDependencyEdges"); FileWriter fw
	 * = new FileWriter(dependencyFile.getAbsoluteFile(), true); BufferedWriter
	 * bw = new BufferedWriter(fw);
	 * 
	 * System.out.println("Writing server dependency edges: "+fromNode+"|"+toNode
	 * +"|"+edgeType); bw.write(fromNode+"|"+toNode+"|"+edgeType+";"); //
	 * bw.newLine(); bw.close();
	 * 
	 * } catch (IOException e) { e.printStackTrace(); } }
	 */

	public void writeDependencyEdgesToFile(String fromNode, String toNode,
			String edgeType) {
		// write hash to file
		try {

			if (dependencyFile == null)
				System.out.println(" dp is  null in writeDependencyEdges");
			FileWriter fw = new FileWriter(dependencyFile.getAbsoluteFile(),
					true);
			BufferedWriter bw = new BufferedWriter(fw);

		//	System.out.println("Writing server dependency edges: " + fromNode
		//			+ "|" + toNode + "|" + edgeType);
			bw.write(fromNode + "|" + toNode + "|" + edgeType + ";");
			// bw.newLine();
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String[] parseToStringArray(String eFiles) {
		final String[] temp2;
		String delimiter1 = "[,]";
		temp2 = eFiles.split(delimiter1);
		// System.out.println("From strat: "+temp2);

		return temp2;
	}

	public String parse(String fileName) {
		String fName = null;
		int i = fileName.indexOf(".java");
		if (i != -1)
			fName = fileName.substring(0, i);
		else
			fName = fileName;
		return fName;
	}

	public Vector parseImports(String line) {
		String[] temp1;
		Vector temp2 = new Vector();
		int index = 0;
		String delimiter1 = "[,]";
		String className;
		temp1 = line.split(delimiter1);
		// System.out.println("temp1 Length:: "+ temp1.length);
		for (int i = 0; i < temp1.length; i++) {
			// System.out.println("temp1:: "+ temp1[i]);
			index = temp1[i].lastIndexOf(".") + 1;
			if (i == temp1.length - 1) {
				temp2.add(temp1[i].substring(index, temp1[i].length() - 3));
			} else
				temp2.add(temp1[i].substring(index, temp1[i].length() - 2));
		}
		return temp2;
	}
	
    // tag::shutdownHook[]
    private static void registerShutdownHook( final DatabaseManagementService managementService )
    {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                managementService.shutdown();
            }
        } );
    }
    // end::shutdownHook[]
}