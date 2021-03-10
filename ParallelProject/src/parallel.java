
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Semaphore;

import edu.princeton.cs.introcs.StdDraw;

public class parallel {

	long barrierTime=0;
	Draw draw;
	int overlapCount = 0;
	int numBodies;
	double sizeOfBody;
	int numSteps;
	double G = 6.67;
	int mass = 100;
	double DT = 0.05;
	ArrayList<seqBody> changeArray;
	ArrayList<seqBody> showArray;
	public boolean if_continue = true;

	public static void main(String[] args) {
		if (args.length < 4) {
			System.out.println(
					"In the parrallel, you need to provide number of workers , number of bodies, size of each body,number of time steps.");

			System.exit(1);
		}

		new parallel(args);

	}

	@SuppressWarnings("deprecation")
	public parallel(String[] args) {

		// number of workers ignored by sequential program
		int numWorker = Integer.parseInt(args[0]);

		if (numWorker < 1 || numWorker > 32) {
			System.out.println("You can only use numWorker less than 33 or larger than 0.");
			System.exit(1);
		}

		this.numBodies = Integer.parseInt(args[1]);
		this.sizeOfBody = Integer.parseInt(args[2]);
		this.numSteps = Integer.parseInt(args[3]);

		// conttol DT
		if (args.length == 5) {
			this.DT = Double.parseDouble(args[4]);
		}

		changeArray = new ArrayList<>();
		showArray = new ArrayList<>();

		// create random position and velocity
		ArrayList<Double> numbers = new ArrayList<Double>();
		double random;
		Random randomGenerator = new Random();
		while (numbers.size() < 2 * numBodies) {

			random = (500 - 2 * sizeOfBody) * randomGenerator.nextDouble() - 250 + sizeOfBody;
			if (!numbers.contains(random)) {
				numbers.add(random);
			}
		}

		// Use loop here to create the bodies
		for (int i = 0; i < numBodies; i++) {
			seqBody newThings = null;

			Point position = new Point(numbers.get(2 * i), numbers.get(2 * i + 1));

			Point velocity = new Point(0.0, 0.0);

			Point force = new Point(0.0, 0.0);
			newThings = new seqBody(position, velocity, force, sizeOfBody);

			changeArray.add(newThings);
			showArray.add(newThings);
		}
		
		if (args.length > 5) {

			// use stdDraw to set the size of graph
			StdDraw.enableDoubleBuffering();
			StdDraw.setXscale(-(250), (250));
			StdDraw.setYscale(-(250), (250));
		}

		// save the time of all calculation get started here
		long startTime = System.currentTimeMillis();
		long estimatedTime;

		// use for loop here to do number of steps that the project is asked to
		// do
		Worker[] worker = new Worker[numWorker];
		Semaphore graph = new Semaphore(0);
		Semaphore finished_graph = new Semaphore(0);
		Semaphore sem[][] = new Semaphore[5][32];

		//System.out.println("here");

		//System.out.println("here");
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < 32; j++) {
				sem[i][j] = new Semaphore(0);
			}
		}
		boolean if_draw=false;
		if (args.length > 5) {
			if_draw=true;
		}
		this.draw = new Draw(this, graph, finished_graph,if_draw);
		

		for (int i = 0; i < numWorker; i++) {
			worker[i] = new Worker(numWorker, numSteps, i, graph, sem, finished_graph, this);
		}

		for (int i = 0; i < numWorker; i++) {
			worker[i].start();
		}
		
		draw.start();
		

		for (int i = 0; i < numWorker; i++) {
			try {
				worker[i].join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// check the time again to get the cost of time in this project
		estimatedTime = System.currentTimeMillis() - startTime;
		long seconds = estimatedTime / 1000;
		long milliseconds = estimatedTime - (seconds * 1000);
		System.out.println("computation time:\t" + seconds + " seconds\t" + milliseconds + " milliseconds");
		long bseconds = barrierTime / 1000;
		long bmilliseconds = barrierTime - (bseconds * 1000);
		System.out.println("barrier time:\t" + bseconds + " seconds\t" + bmilliseconds + " milliseconds");

		int sum = 0;
		for (int i = 0; i < numWorker; i++) {
			sum += worker[i].collision;

		}

		System.out.printf("In the parrallel, work has total %d collision \n", sum);

		BufferedWriter newfile = null;
		String fileName = "parrallelOutput.txt";
		String sentences = "";

		for (int i = 0; i < showArray.size(); i++) {
			sentences += "Point " + i + ":" + "\n";
			sentences += "final x axis is " + showArray.get(i).acquirePosition().x + "\nfinal y axis is  "
					+ showArray.get(i).acquirePosition().y + "\n";
			sentences += "final Velocity in x axis is  " + showArray.get(i).acquireVelocity().x
					+ "\nfinal velocity in y axis is  " + showArray.get(i).acquireVelocity().y + "\n";
			sentences += "\n";
		}

		try {
			newfile = new BufferedWriter(new FileWriter(fileName));
			newfile.write(sentences);
			newfile.close();
		} catch (IOException e) {
			System.err.println("IOException: " + e.getMessage());
		}
	System.exit(0);
	}

	public void addBarrierTime(long estimatedTime) {
		this.barrierTime=this.barrierTime+estimatedTime;
	}

}
