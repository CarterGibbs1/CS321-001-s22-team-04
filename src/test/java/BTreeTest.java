

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BTreeTest {
	// folder location that RAFs and dumps go to
	static private final String TESTS_FOLDER = "./results/tests/TEST_";
	static private final Random random = new Random();
	static private final String VALID_LETTERS = "atcg";

	// how many times to run certain Tests, some of these drastically increase run time
	static private int[] timesToRun = new int[] {1, 10, 20, 50, 100, 500, 1000, 5000};
//	static private int run_BNode_RAF_RAFAppropriateSize = timesToRun[0];
	static private int run_BTree_RAF_IsSorted = timesToRun[0];
	static private int run_BTree_RAF_Search = timesToRun[1];
	static private int run_BTree_RAF_Cache = timesToRun[1];
	//example
	static private int run_EXAMPLE_LOOPED_TEST = timesToRun[3];

	static private Throwable ex = null;
	static private int currentProgress = 0;
	static private String testName = "";
	
	static private File resultFile = new File(TESTS_FOLDER + "TestResults.txt");
	static private PrintStream testReporting;
	
	// create progress bar to show things are working, destroy on completion (doesn't
	//really work on eclipse)
	static private final ProgressBar progress = new ProgressBar(25,
			run_BTree_RAF_IsSorted +
			run_BTree_RAF_Search +
			run_BTree_RAF_Cache +
			BTreeTest.class.getDeclaredMethods().length - 7 //don't count methods that aren't tests i.e. utility methods
			);

	// =================================================================================================================
	//                                                Utility Methods
	// =================================================================================================================
	
	/**
	 * Gets a random number between the origin (inclusive) and bound
	 * (exclusive).
	 * 
	 * @param origin Minimum (inclusive) number
	 * @param bound  Maximum (exclusive) number
	 * 
	 * @return Random number in given range
	 */
	private static int getRand(int origin, int bound) {
		return (random.nextInt(bound - origin) + origin);
	}
	
	/**
	 * If a test is stopped in the middle due to an exception, this method
	 * will ensure that the exception is still thrown while updating the
	 * progress bar to compensate for the lost tests in a for loop.
	 * 
	 * @param excpectedRuns Times this test is expected to run/loop
	 * 
	 * @throws Throwable The exception thrown by the test
	 */
	private static void progressAndExceptionCheck(int excpectedRuns) throws Throwable {
		for (; progress.getProgress() < excpectedRuns + currentProgress;) {
			progress.increaseProgress();
		}
		
		if(ex != null) {
			throw ex;
		}
	}
	
	/**
	 * Creates a dump of the given BTree and checks that all the given inputs are
	 * in the correct order and that the frequency of the inputs are correct as well.
	 * 
	 * @param inputs The list of Strings that were inserted into the tree
	 * @param tree   BTree to dump and check
	 * 
	 * @throws Throwable
	 */
	private static void checkDump(ArrayList<String> inputs, BTree tree) throws Throwable {
		// write dump to file
		PrintStream console = System.out;
		File fileT = new File(TESTS_FOLDER + testName + "_dump.txt");
		fileT.createNewFile();
		PrintStream file = new PrintStream(fileT);

		System.setOut(file);
		System.out.println(tree.dump());
		System.setOut(console);

		//copy given array
		ArrayList<String> foo = new ArrayList<String>();
		for(int i = 0; i < inputs.size(); i++) {foo.add(inputs.get(i));}
		
		ArrayList<TreeObject> p = new ArrayList<TreeObject>();
		String obj;
		//for all inputSequences, make a treeObject for it
		while(!foo.isEmpty()) {
			obj = foo.remove(0);
			p.add(new TreeObject(obj));			
			
			//remove all occurrences of the string and increment frequency for each one
			while(foo.remove(obj)) {
				p.get(p.size() - 1).incrementFrequency();
			}
		}
		
		mergesortTreeObject(p);
		
		// check that dump is correct
		Scanner scan = new Scanner(new File(TESTS_FOLDER + testName + "_dump.txt"));
		String exc;
		for (int i = 0; i < p.size(); i++) {
			exc = scan.nextLine();
			if (!exc.equals(p.get(i).toString())) {
				System.out.println(exc + " " + i + " | " + p.get(i) + " " + inputs.indexOf(p.get(i).keyToString()) + " " + inputs.lastIndexOf(p.get(i).keyToString()));
				assert (false);
			}
		}
	}
	
	/**
	 * Generate a random number of sequences of random length in the given ranges.
	 * 
	 * @param minNumSeq    Minimum (inclusive) number of sequences
	 * @param maxNumSeq    Maximum (exclusive) number of sequences
	 * @param minseqLength Minimum (inclusive) length of sequences > 1
	 * @param maxSeqLength Maximum (exclusive) length of sequences < 33
	 * 
	 * @return List of randomly generated sequences
	 * @return
	 */
	static private ArrayList<String> generateRandomSequences(int minNumSeq, int maxNumSeq, int minseqLength, int maxSeqLength) {
		int numSeq = getRand(minNumSeq, maxNumSeq);
		int lengthSeq = getRand(minseqLength, maxSeqLength);
		ArrayList<String> sequences = new ArrayList<String>();

		// construct numSeq amount of random sequences
		String sequence;
		for (int i = 0; i < numSeq; i++) {
			sequence = "";

			for (int j = 0; j < lengthSeq; j++) {
				sequence = sequence + VALID_LETTERS.charAt(getRand(0, 4));
			}

			sequences.add(sequence);
		}
		return sequences;
	}

	/**
	 * Recursively mergesort the given list of TreeObjects in increasing order.
	 * 
	 * @param list ArrayList to sort
	 */
	static private void mergesortTreeObject(ArrayList<TreeObject> list) {
		// base case: less than 2 elements
		if (list.size() < 2) {
			return;
		}

		// general case
		ArrayList<TreeObject> left = new ArrayList<TreeObject>();
		ArrayList<TreeObject> right = new ArrayList<TreeObject>();

		// split the list into two equally sized lists
		while (!list.isEmpty()) {
			if (left.size() < list.size()) {
				left.add(list.remove(0));
			} else {
				right.add(list.remove(0));
			}
		}

		// recursively sort left and right
		mergesortTreeObject(left);
		mergesortTreeObject(right);

		// reconstructing the list
		/*
		 * while right has elements, "move" the first element in right to the end of the
		 * sorted list if it is greater than the first element of left. If left is
		 * greater, move it's first element to the end of the sorted list. If either
		 * left or right runs out of elements, move the rest of the elements of the
		 * remaining list to the sorted list.
		 */
		while (!right.isEmpty()) {
			if (!left.isEmpty()) {
				list.add(left.get(0).compare(right.get(0)) > 0 ? right.remove(0) : left.remove(0));
			} else {
				list.add(right.remove(0));
			}
		}
		while (!left.isEmpty()) {
			list.add(left.remove(0));
		}
	}

	/**
	 * Test that the methods used to randomly generate sequences and sort them
	 * function correctly.
	 * <p>
	 * NOTE: Not directly related to BTree, but if this test fails then other tests
	 * using random sequences will not function properly.
	 */
	@Test
	public void randomGenerator_mergesort() {
		try {
			ArrayList<String> randSeq = generateRandomSequences(30, 50, 10, 20);

			// test that there are between 5 and 9 sequences
			assert (randSeq.size() >= 30 && randSeq.size() < 50);

			int seqLength = randSeq.get(0).length();

			// test that all sequences are the same length
			for (int i = 0; i < randSeq.size(); i++) {
				assert (seqLength == randSeq.get(i).length());
			}

			// create ArrayList of TreeObjects from sequences
			ArrayList<TreeObject> treeObjects = new ArrayList<TreeObject>();
			for (int i = 0; i < randSeq.size(); i++) {
				treeObjects.add(new TreeObject(randSeq.get(i)));
			}

			// sort TreeObjects
			mergesortTreeObject(treeObjects);

			// test that treeObjects is sorted in increasing order
			for (int i = 0; i < treeObjects.size() - 1; i++) {
				assert (treeObjects.get(i).compare(treeObjects.get(i + 1)) <= 0);
			}
			progress.increaseProgress();
		} catch (Exception e) {
			progress.increaseProgress();
			throw e;
		}
	}
	
	// =================================================================================================================
	//                                              Testing BNodes in Memory Only
	// =================================================================================================================

	/**
	 * Test that a BNode correctly inserts given elements in sorted order.
	 */
	@Test
	public void singleBNode_TestInsertion() {
		try {

			// 57 12 8 59 7 10 44
			String inputSequences = "TGC ATA AGA TGT ACT AGG GTA".toLowerCase();

			// instantiate and populate BNode with inputLetters
			TestBNode<String> testNode = new TestBNode<String>(new TreeObject(inputSequences.substring(0, 3)));
			for (int i = 4; i < inputSequences.length(); i += 4) {
				testNode.insert(new TreeObject(inputSequences.substring(i, i + 3)));
			}

			// see if the BNode contains sequences in order in long value
			assertEquals(testNode.toString(), "7 8 10 12 44 57 59");

			progress.increaseProgress();
		} catch (Exception e) {
			progress.increaseProgress();
			throw e;
		}
	}

	/**
	 * Test that a ROOT BNode correctly splits itself into three new nodes, the new
	 * root and it's two children.
	 */
	@Test
	public void BNode_TestSplit() {
		try {
			// 59 108 14 103 46
			String inputSequences = "ATGT CGTA AATG CGCT AGTG".toLowerCase();
			TestBNode<String> parent;
			TestBNode<String> rightChild;

			// instantiate and populate BNode with inputLetters
			TestBNode<String> testNode = new TestBNode<String>(new TreeObject(inputSequences.substring(0, 4)));
			for (int i = 5; i < inputSequences.length(); i += 5) {
				testNode.insert(new TreeObject(inputSequences.substring(i, i + 4)));
			}

			// split BNode and save parent and rightChild
			parent = testNode.split();
			rightChild = parent.getSubtree(new TreeObject("tttt"));

			assertEquals(parent.toString(), "59");
			assertEquals(testNode.toString(), "14 46");
			assertEquals(rightChild.toString(), "103 108");

			progress.increaseProgress();
		} catch (Exception e) {
			progress.increaseProgress();
			throw e;
		}
	}

	/**
	 * Test that when a BNode is not full, isFull() will return false and when BNode
	 * is full, isFull() will return true.
	 */
	@Test
	public void BNode_TestIsFull() {
		try {
			String inputSequences = "ATGTCTGACCGT".toLowerCase();
			int degree = 7;

			// instantiate and populate BNode with inputLetters
			TestBNode<String> testNode = new TestBNode<String>(new TreeObject(inputSequences.substring(0, 1)));
			for (int i = 1; i < inputSequences.length(); i++) {
				testNode.insert(new TreeObject(inputSequences.substring(i, i + 1)));
			}

			// testNode is not full
			assert (!testNode.isFull(degree));

			// testNode is now full
			testNode.insert(new TreeObject("a"));
			assert (testNode.isFull(degree));

			progress.increaseProgress();
		} catch (Exception e) {
			progress.increaseProgress();
			throw e;
		}
	}

	/**
	 * Test using a rudimentary BTree that the BNode methods isFull(), insert(key),
	 * and split() function correctly resulting in a BTree with the correct number
	 * of nodes and correct height.
	 */
	@Test
	public void BNode_CorrectHeightAndNodeCount() {
		try {
			String inputSequences = "ATGTCTGACCGTGACTTACGAAG".toLowerCase();
			int degree = 2;

			// instantiate and BNode root
			TestBNode<String> root = new TestBNode<String>(new TreeObject(inputSequences.substring(0, 1)));
			TestBNode<String> currentNode;

			// create a rudimentary BTree to insert into while counting the total BNode
			// amount and the height
			int height = 1;
			int totalNodes = 1;
			TreeObject key;
			for (int i = 1; i < inputSequences.length(); i++) {
				currentNode = root;
				key = new TreeObject(inputSequences.substring(i, i + 1));

				if (root.isFull(degree)) {
					root = currentNode = currentNode.split();
					totalNodes += 2;
					height++;
				}

				// get to appropriate leaf BNode
				while (!currentNode.isLeaf()) {
					currentNode = currentNode.getSubtree(key);

					// if the currentNode is full, split it
					if (currentNode.isFull(degree)) {
						currentNode = currentNode.split();
						totalNodes++;
					}
				}

				// once at LEAF, insert key
				currentNode.insert(key);

				// TODO: might need to check and split the leaf node we just inserted into
//    		if(currentNode.isFull()) {
//    			currentNode = currentNode.split();
//    			totalNodes++;
//    		}
			}

			assertEquals(totalNodes, 16); // 18 if we perform a split on the leaf nodes we just inserted into
			assertEquals(height, 4);
			
			progress.increaseProgress();
		} catch (Exception e) {
			progress.increaseProgress();
			throw e;
		}
	}

	// =================================================================================================================
	//                                          Testing TreeObjects in Memory Only
	// =================================================================================================================

	/**
	 * Test most public methods for TreeObject with a variety of cases
	 */
	@Test
	public void TreeObject_PublicMethods() {
		try {
			TreeObject tO = new TreeObject("tcacgaggtc");
			long key = Long.parseLong("11010001100010101101", 2);
			assertEquals(tO.getKey(), key);
//			assertEquals(tO.withZeros(), "11010001100010101101");
//			assertEquals(tO.getFrequency(), 5);

			tO.setFrequency(6);
			assertEquals(tO.getFrequency(), 6);
			tO.setFrequency(5);
			TreeObject tOTwo = new TreeObject("tcacgaggta");
			assert (tO.compare(tOTwo) > 0);
//			assertEquals(tO.toString(), "tcacgaggtc: 5");

			progress.increaseProgress();
		} catch (Exception e) {
			progress.increaseProgress();
			throw e;
		}
	}

	// =================================================================================================================
	//                                           Testing BNodes using RAF
	// =================================================================================================================

	/**
	 * Test that a single BNode with a few keys inserted and that is then written to
	 * the RAF is the same as the one stored in memory.
	 * 
	 * @throws Throwable 
	 */
	@Test
	public void BNode_RAF_InsertWriteRead() throws Throwable {
		testName = new Object() {}.getClass().getEnclosingMethod().getName(); //get the name of this method
		try {
			// 57 12 8 59 7 10 44
			String inputSequences = "TGC ATA AGA TGT ACT AGG GTA".toLowerCase();

			// delete old RAF and set new RAF, degree, and byteBuffer. Important that they
			// are done in this order
			BReadWrite.setRAF(TESTS_FOLDER + testName, true);
			BNode.setDegree(10);
			BReadWrite.setBuffer(BNode.getDiskSize());

			// instantiate and populate BNode with inputLetters
			BNode memoryNode = new BNode(new TreeObject(inputSequences.substring(0, 3)), BTree.getDiskSize());
			for (int i = 4; i < inputSequences.length(); i += 4) {
				memoryNode.insert(new TreeObject(inputSequences.substring(i, i + 3)));
			}

			// read BNode in RAF
			BReadWrite.writeBNode(memoryNode);
			BNode readNode = BReadWrite.readBNode(BTree.getDiskSize());

			// see if memoryNode contains sequences in order in long value,
			assertEquals(memoryNode.getKeysAsString(), "7 8 10 12 44 57 59");
			// then check if readNode from the RAF is the same as the memoryNode
			assertEquals(readNode.getKeysAsString(), memoryNode.getKeysAsString());

			progress.increaseProgress();
		} catch (Throwable e) {
			progress.increaseProgress();
			throw e;
		}
	}

	// =================================================================================================================
	//                                           Testing BTrees using RAF
	// =================================================================================================================
	
	/**
	 * Insert a random number of sequences of random length into a BTree, then write
	 * the BTree, and lastly check if both the memory held node and the read node are
	 * sorted correctly.
	 * <p>
	 * RANDOM: This test is random and thus, the RAFs will change every run.
	 * 
	 * @throws Throwable 
	 */
	@Test
	public void BTree_RAF_IsSorted() throws Throwable {
		ex = null;
		currentProgress = progress.getProgress();
		testName = new Object() {}.getClass().getEnclosingMethod().getName(); //get the name of this method
		
		try {
			
			for (int k = 0; k < run_BTree_RAF_IsSorted; k++) {// <--- THIS WILL TAKE A LONG TIME IF REALLY BIG
				
				// delete old RAF and set new RAF, degree, and byteBuffer. Important that they
				// are done in this order
				BReadWrite.setRAF(TESTS_FOLDER + testName + k, true);
				int degree = getRand(3, 30);
				BNode.setDegree(degree);
				BReadWrite.setBuffer(BNode.getDiskSize());
				
				//generate random sequences and create BTree
				ArrayList<String> inputSequences = generateRandomSequences(200000/5, 300000/5, 5, 15);// <--- THIS WILL TAKE A LONG TIME IF REALLY BIG
				BTree memoryTree = new BTree(degree, 5);
				
				//insert all sequences
				for(int i = 0; i < inputSequences.size(); i++) {
					memoryTree.insert(new TreeObject(inputSequences.get(i)));
				}
				
				//write BTree and then read
				BReadWrite.writeBTree(memoryTree);
				BTree readTree = BReadWrite.readBTree(-1);
				
				//check that both BTrees are sorted
				assert(memoryTree.isSorted());
				assert(readTree.isSorted());
				
				progress.increaseProgress();
			}
			
		} catch (Throwable e) {
			ex = e;
			progressAndExceptionCheck(run_BTree_RAF_IsSorted);
		}
	}
	
	/**
	 * Insert a random number of sequences of random length into a BTree, then insert
	 * the same random sequence a random number of times at random positions. A
	 * following search for the sequence should return a frequency equal to the number
	 * of sequences added.
	 * <p>
	 * RANDOM: This test is random and thus, the RAFs will change every run.
	 * 
	 * @throws Throwable 
	 */
	@Test
	public void BTree_RAF_Search() throws Throwable {
		ex = null;
		BTree memoryTree = null;
		String newSeq = null;
		int numNewSeq = 0;
		ArrayList<String> inputSequences = null;
		int oldH = 0;
		currentProgress = progress.getProgress();
		testName = new Object() {}.getClass().getEnclosingMethod().getName(); //get the name of this method
		
		try {
			
			for (int k = 0; k < run_BTree_RAF_Search; k++) {// <--- THIS WILL TAKE A LONG TIME IF REALLY BIG
				oldH = 0;
//				System.out.println(k);
				// delete old RAF and set new RAF, degree, and byteBuffer. Important that they
				// are done in this order
				BReadWrite.setRAF(TESTS_FOLDER + testName + k, true);
				int degree = getRand(5, 30);
				BNode.setDegree(degree);
				BReadWrite.setBuffer(BNode.getDiskSize());
				
				//generate random sequences
				inputSequences = generateRandomSequences(200000/5, 300000/5, 2, 32);// <--- THIS WILL TAKE A LONG TIME IF REALLY BIG
				TreeObject.setSequenceLength(inputSequences.get(0).length());
				
				//generate the same random sequence a random number of times and insert at random spots
				numNewSeq = getRand(20, 100);
				newSeq = inputSequences.get(getRand(0, inputSequences.size()));
				while(inputSequences.remove(newSeq));//remove all instances of newSeq
				for(int i = 0; i < numNewSeq; i++) {
					inputSequences.add(getRand(0, inputSequences.size()), newSeq);
				}
				
				//create BTree
				memoryTree = new BTree(degree, inputSequences.get(0).length());
//				BTree t2 = new BTree(new TreeObject(inputSequences.get(0)), degree, inputSequences.get(0).length());
				
				
				//debugging variable
				int y = 0;
				ArrayList<BNode> x = new ArrayList<BNode>();
				
				//insert all sequences
				for(int i = 0; i < inputSequences.size(); i++) {
					
							if(inputSequences.get(i).equals(newSeq)) {
//System.out.println(memoryTree.search(new TreeObject(newSeq, 0)) + " | " + y + " | " + i);
						y++;
						
					}
							if(memoryTree.getHeight() > oldH) {
	oldH++;
//	System.out.println("&&&&&&&&");
}	
					memoryTree.insert(new TreeObject(inputSequences.get(i)));
//					if(memoryTree.search(new TreeObject(newSeq, 0)) != y) {
//						memoryTree.search(new TreeObject(newSeq, 0));
//						System.out.println("\n" + memoryTree.dump() + "\n");
//						memoryTree.getAllBNodes(x, memoryTree.getRoot().getAddress());
//						memoryTree.insert(new TreeObject(newSeq, 0));
//					}
	
				}
				
				if(y != memoryTree.search(new TreeObject(newSeq))) {
					
				}
				
				//write BTree and then read
				BReadWrite.writeBTree(memoryTree);
				BTree readTree = BReadWrite.readBTree(-1);
				
				//check that both BTrees are sorted
				assert(memoryTree.search(new TreeObject(newSeq)) == numNewSeq);
				assert(readTree.search(new TreeObject(newSeq)) == numNewSeq);
				
				progress.increaseProgress();
				
//				System.out.println("\n=======================================\n");
				
			}
			checkDump(inputSequences, memoryTree);
		} catch (Throwable e) {
			System.out.println("==" + memoryTree.getDegree() + "| " + newSeq);
				ArrayList<BNode> x = new ArrayList<BNode>();
//						memoryTree.getAllBNodes(x, memoryTree.getRoot().getAddress());
			ex = e;
			progressAndExceptionCheck(run_BTree_RAF_Search);
		}
	}
	
	/**
	 * Insert a random number of sequences into a BTree and then check the printed
	 * to a file dump.
	 * <p>
	 * RANDOM: This test is random and thus, the RAFs will change every run.
	 * 
	 * @throws Throwable
	 */
	@Test
	public void BTree_RAF_Dump() throws Throwable {
		testName = new Object() {}.getClass().getEnclosingMethod().getName(); //get the name of this method
		 ArrayList<String> inputSequences = null;
		try {
			
			// delete old RAF and set new RAF, degree, and byteBuffer. Important that they
			// are done in this order
			BReadWrite.setRAF(TESTS_FOLDER + testName, true);
			int degree = getRand(2, 50);
			BNode.setDegree(degree);
			BReadWrite.setBuffer(BNode.getDiskSize());
			
			// generate random sequences
				inputSequences = generateRandomSequences(200000/5, 300000/5, 2, 32);// <--- THIS WILL TAKE A LONG TIME IF REALLY BIG
			TreeObject.setSequenceLength(inputSequences.get(0).length());
			
			//debugging loop
			int x = 0;
			for (int i = 1; i < inputSequences.size(); i++) {
				if(new TreeObject(inputSequences.get(x)).compare(new TreeObject(inputSequences.get(i))) > 0) {
					x = i;
				}
			}
//			String y = inputSequences.get(x);

			// create BTree
			BTree memoryTree = new BTree(degree, 5);

			// insert all sequences
			for (int i = 0; i < inputSequences.size(); i++) {
				memoryTree.insert(new TreeObject(inputSequences.get(i)));
			}
			
			checkDump(inputSequences, memoryTree);
			progress.increaseProgress();
		} catch (Throwable e) {
			progress.increaseProgress();
			throw e;
		}
	}
	
	
	/**
	 * Insert a random number of sequences into a BTree with and without a cache and
	 * report the time difference to 'TEST_TestResults.txt.' Also checks the dump of
	 * the last cache BTree that was created.
	 * <p>
	 * RANDOM: This test is random and thus, the RAFs will change every run.
	 * 
	 * @throws Throwable
	 */
	@Test
	public void BTree_RAF_CacheTimeImprovement() throws Throwable {
		ex = null;
		currentProgress = progress.getProgress();
		testName = new Object() {}.getClass().getEnclosingMethod().getName(); //get the name of this method
		
		int ncTime = 0;
		int chTime = 0;
		int ch2Time = 0;
		int cTime = 0;
		int cache = 100;
		int cache2 = 500;
		long tSize = 0;
		BTree wiC = null;
		BTree wiC2 = null;
		
		ArrayList<String> inputSequences = null;
		try {
			
			for (int k = 0; k < run_BTree_RAF_Cache; k++) {// <--- THIS WILL TAKE A LONG TIME IF REALLY BIG
				
				// delete old RAF and set new RAF, degree, and byteBuffer. Important that they
				// are done in this order
				BReadWrite.setRAF(TESTS_FOLDER + testName + k + "_NoCache", true);
				int degree = getRand(5, 30);
				BNode.setDegree(degree);
				BReadWrite.setBuffer(BNode.getDiskSize());
				
				// generate random sequences
				inputSequences = generateRandomSequences(20000/5, 30000/5, 2, 32);// <--- THIS WILL TAKE A LONG TIME IF REALLY BIG
				tSize += inputSequences.size();
				TreeObject.setSequenceLength(inputSequences.get(0).length());
				
				//create tree with and without cache
				BTree noC = new BTree(degree, 5);
				
				cTime = (int)System.currentTimeMillis();
				
				// insert all sequences
				for (int i = 0 ; i < inputSequences.size(); i++) {
					noC.insert(new TreeObject(inputSequences.get(i)));
				}
				
				ncTime += (System.currentTimeMillis() - cTime);
				
				BReadWrite.setRAF(TESTS_FOLDER + testName + k + "_Cache", true);
				wiC = new BTree(degree, 5, cache);
				
				cTime = (int)System.currentTimeMillis();
				
				// insert all sequences
				for (int i = 0; i < inputSequences.size(); i++) {
					wiC.insert(new TreeObject(inputSequences.get(i)));
				}
				wiC.emptyBCache();
				
				chTime += (System.currentTimeMillis() - cTime);
				
				
				BReadWrite.setRAF(TESTS_FOLDER + testName + k + "_Cache2", true);
				wiC2 = new BTree(degree, 5, cache2);
				
				cTime = (int)System.currentTimeMillis();
				
				// insert all sequences
				for (int i = 0; i < inputSequences.size(); i++) {
					wiC2.insert(new TreeObject(inputSequences.get(i)));
				}
				wiC2.emptyBCache();
				
				ch2Time += (System.currentTimeMillis() - cTime);
				
				
				//check that both BTrees are sorted
				assert(noC.isSorted());
				assert(wiC.isSorted());
				assert(wiC2.isSorted());
				
				
				progress.increaseProgress();
			}
//			checkDump(inputSequences, wiC); //check the last cache BTree's dump
			//report times to file
			if(!resultFile.exists()) {
				resultFile.createNewFile();
			}
			
			testReporting = new PrintStream(resultFile);
			testReporting.print(testName + ": cache-[" + cache + "] avg sequences-[" + (tSize/run_BTree_RAF_Cache)/1000 +"k]\n"
					+   "Without Cache Avg - " + (ncTime/run_BTree_RAF_Cache) + "ms"
					+ "\nWith Cach Avg     - " + (chTime/run_BTree_RAF_Cache) + "ms"
					+ "\nWith Cach2 Avg    - " + (ch2Time/run_BTree_RAF_Cache) + "ms"
					+ "\n");
			
		} catch (Throwable e) {
			ex = e;
			progressAndExceptionCheck(run_BTree_RAF_Cache);
		}
	}

	
	
	// =================================================================================================================
	//                                           MISC TESTS
	// =================================================================================================================
	
	
//	/**
//	 * Special test for seeing if BTreeAaron or BTree imp is faster
//	 * 
//	 * @throws Throwable 
//	 */
//	@Test
//	public void z_BTree_RAF_TimeBTAvsBT() throws Throwable {
//		ex = null;
//		testName = new Object() {}.getClass().getEnclosingMethod().getName(); //get the name of this method
//		
//		int tmTime = 0;
//		int taTime = 0;
//		
//		try {
//			
//			for (int k = 0; k < 50; k++) {// <--- THIS WILL TAKE A LONG TIME IF REALLY BIG
//				ArrayList<String> inputSequences = generateRandomSequences(200000/5, 300000/5, 5, 15);// <--- THIS WILL TAKE A LONG TIME IF REALLY BIG
//				
//				int cTime = (int)System.currentTimeMillis();
//				
////				System.out.println(k);
//				// delete old RAF and set new RAF, degree, and byteBuffer. Important that they
//				// are done in this order
//				BReadWrite.setRAF(TESTS_FOLDER + testName + k + "m", true);
//				int degree = getRand(5, 70);
//				BNode.setDegree(degree);
//				BReadWrite.setBuffer(BNode.getDiskSize());
//				
//				//create BTree
//				BTree memoryTree = new BTree(new TreeObject(inputSequences.get(0), 1), degree, 5);
//				
//				// insert all sequences
//				for (int i = 1; i < inputSequences.size(); i++) {
//					memoryTree.insert(new TreeObject(inputSequences.get(i), 1));
//				}
//				
//				//write BTree and then read
//				BReadWrite.writeBTree(memoryTree);
//				BTree readTree = BReadWrite.readBTree();
//				
//				//check that both BTrees are sorted
////				assert(memoryTree.search(new TreeObject(newSeq, 0)) == numNewSeq);
////				assert(readTree.search(new TreeObject(newSeq, 0)) == numNewSeq);
//				
//				tmTime += (System.currentTimeMillis() - cTime);
//				
//				//=========================================================================================================
//				cTime = (int)System.currentTimeMillis();
//				
////				System.out.println(k);
//				// delete old RAF and set new RAF, degree, and byteBuffer. Important that they
//				// are done in this order
//				degree = getRand(5, 70);
////				TestBNodeNoE.s
////				TestBNodeE.setDegree(degree);
//				TestBNodeNoE.setDegree(degree);
//				BReadWriteAlt.setBuffer(TestBNodeNoE.getDiskSize());
//				
//				//create BTree
//				TestBNodeNoE x = new TestBNodeNoE(new TreeObjectNoE(inputSequences.get(1), 1), 50);
//				BTreeAaron memoryTree2 = new BTreeAaron(degree, inputSequences.get(0).length(), x, TESTS_FOLDER + testName + k + "a");
//				
//				// insert all sequences
//				for (int i = 1; i < inputSequences.size(); i++) {
//					memoryTree2.insert(new TreeObjectNoE(inputSequences.get(i), 1));
//				}
//				
//				//write BTree and then read
//				BReadWrite.writeBTree(memoryTree);
//				BTree readTree2 = BReadWrite.readBTree();
//				
//				//check that both BTrees are sorted
////				assert(memoryTree.search(new TreeObject(newSeq, 0)) == numNewSeq);
////				assert(readTree.search(new TreeObject(newSeq, 0)) == numNewSeq);
//				
//				tmTime += (System.currentTimeMillis() - cTime);
//			}
//			
//			System.out.println("\\nnBTreeAaron:" + taTime/50);
//			System.out.println("\nBTree:" + tmTime/50);
//			
//		} catch (Throwable e) {
//			throw e;
//		}
//	}
	
	
	/**
	 * 
	 * 
	 * @throws Throwable
	 */
	public void EXAMPLE_TEST() throws Throwable {
		//you can thank stackoverflow for this one: https://stackoverflow.com/questions/442747/getting-the-name-of-the-currently-executing-method
		testName = new Object() {}.getClass().getEnclosingMethod().getName(); //get the name of this method
		try {
			
			// delete old RAF and set new RAF, degree, and byteBuffer. Important that they
			// are done in this order
			BReadWrite.setRAF(TESTS_FOLDER + testName, true);
			int degree = getRand(3, 7);
			BNode.setDegree(degree);
			BReadWrite.setBuffer(BNode.getDiskSize());
			
			// code goes here
			
			progress.increaseProgress();
		} catch (Throwable e) {
			progress.increaseProgress();
			throw e;
		}
	}

	/**
	 * 
	 * 
	 * @throws Throwable 
	 */
	public void EXAMPLE_LOOPED_TEST() throws Throwable {
		ex = null;
		currentProgress = progress.getProgress();
		testName = new Object() {}.getClass().getEnclosingMethod().getName(); //get the name of this method
		
		try {
			
			for (int k = 0; k < run_EXAMPLE_LOOPED_TEST; k++) {// <--- THIS WILL TAKE A LONG TIME IF REALLY BIG
				
				// delete old RAF and set new RAF, degree, and byteBuffer. Important that they
				// are done in this order
				BReadWrite.setRAF(TESTS_FOLDER + testName + k, true);
				int degree = getRand(3, 7);
				BNode.setDegree(degree);
				BReadWrite.setBuffer(BNode.getDiskSize());
				
				// code goes here
				
				progress.increaseProgress();
			}
			
		} catch (Throwable e) {
			ex = e;
			progressAndExceptionCheck(run_BTree_RAF_Search);
		}
	}

}
