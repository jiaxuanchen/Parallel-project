import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import edu.princeton.cs.introcs.StdDraw;

public class sequential {

	private int overlapCount = 0;
	private int numBodies;
	private double sizeOfBody;
	private int numSteps;
	private double G = 6.67;
	private int mass = 100;
	private double DT = 0.05;
	private ArrayList<seqBody> changeArray;
	private ArrayList<seqBody> showArray;
	private int collision = 0;

	public static void main(String[] args) {
		if (args.length < 4) {
			System.out.println(
					"In the Sequential, you need to provide number of workers , number of bodies, size of each body,number of time steps.");

			System.exit(1);
		}

		new sequential(args);

	}

	public sequential(String[] args) {

		// number of workers ignored by sequential program
		int numWorker = Integer.parseInt(args[0]);

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

			StdDraw.enableDoubleBuffering();
			StdDraw.setXscale(-250, 250);
			StdDraw.setYscale(-250, 250);
		}
		// print time
		long startTime = System.currentTimeMillis();
		long estimatedTime;

		for (int i = 0; i < numSteps; i++) {
			calculateForces();
			// printBody(i);
			while (true) {
				if (moveBodies() == false) {
					continue;
				}
				detectBoundary();

				if (findCollision() == false) {
					continue;
				} else
					break;
			}
			if (args.length > 5) {

				makeGraphic();
			}
			try {
				// TimeUnit.SECONDS.sleep(1);
				TimeUnit.SECONDS.sleep((long) DT);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		estimatedTime = System.currentTimeMillis() - startTime;
		long seconds = estimatedTime / 1000;
		long milliseconds = estimatedTime - (seconds * 1000);

		System.out.println("computation time:\t" + seconds + " seconds\t" + milliseconds + " milliseconds");
		System.out.printf("The number of collisions detcted is %d", collision);

		BufferedWriter newfile = null;
		String fileName = "sequentialOutput.txt";
		String sentences = "";

		for (int i = 0; i < showArray.size(); i++) {
			sentences += "Point " + i + ":" + "\n";
			sentences += "final x-axis is " + showArray.get(i).acquirePosition().x + "\n final y-axis is  "
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

	private void printBody(int j) {
		int size = showArray.size();
		double x, vx, fx, s;
		double y, vy, fy;

		for (int i = 0; i < size; i++) {
			x = showArray.get(i).acquirePosition().x;
			vx = showArray.get(i).acquireVelocity().x;
			fx = showArray.get(i).acquireForce().x;
			s = showArray.get(i).acquireSizeOfBody();
			y = showArray.get(i).acquirePosition().y;
			vy = showArray.get(i).acquireVelocity().y;
			fy = showArray.get(i).acquireForce().y;

			System.out.printf("At NumTimeStep of %d:\n\t x=%f, y=%f\n\t vx=%f, vy=%f\n\t fx=%f, fy=%f\n\t size=%f\n", j,
					x, y, vx, vy, fx, fy, s);
		}
	}

	public void makeGraphic() {
		StdDraw.clear();

		for (int i = 0; i < showArray.size(); i++) {
			StdDraw.setPenColor(0, 255, 255);

			StdDraw.circle(showArray.get(i).acquirePosition().x, showArray.get(i).acquirePosition().y, this.sizeOfBody);
			StdDraw.setPenColor(StdDraw.PINK);

			StdDraw.filledCircle(showArray.get(i).acquirePosition().x, showArray.get(i).acquirePosition().y,
					this.sizeOfBody);

		}
		StdDraw.show();
	}

	// mid, ya
	public Point calculateRealLocation(double distance, Point mid, Point a) {
		double x, y;
		x = mid.x - 2 * (mid.x - a.x) * sizeOfBody / distance;
		y = mid.y - 2 * (mid.y - a.y) * sizeOfBody / distance;

		return new Point(x, y);
	}

	public boolean findCollision() {
		// System.out.println("Trying to find the collection\n");
		Point mid = new Point(0.0, 0.0);
		Point reala = new Point(0.0, 0.0);
		Point realb = new Point(0.0, 0.0);

		for (int i = 0; i < numBodies - 1; i++) {
			for (int j = i + 1; j < numBodies; j++) {
				overlapCount = 0;
				if (overlap(changeArray.get(i), changeArray.get(j)) == true) {
					collision++;
					double distance = Math.sqrt(Math
							.pow((changeArray.get(i).acquirePosition().x - changeArray.get(j).acquirePosition().x), 2.0)
							+ Math.pow(
									(changeArray.get(i).acquirePosition().y - changeArray.get(j).acquirePosition().y),
									2.0));
					// System.out.printf("distance: %f\n", distance);
					// System.out.println("Deceted Collection
					// here<-----------------------\n");
					if (distance < sizeOfBody && overlapCount > 10) {
						this.DT *= 0.5;
						// System.out.println("Deceted Collection OVERLAP
						// here<-----------------------\n");
						overlapCount++;
						return false;
					}

					mid.x = (changeArray.get(i).acquirePosition().x + changeArray.get(j).acquirePosition().x) / 2;
					mid.y = (changeArray.get(i).acquirePosition().y + changeArray.get(j).acquirePosition().y) / 2;
					reala = calculateRealLocation(distance, mid, changeArray.get(i).acquirePosition());
					realb = calculateRealLocation(distance, mid, changeArray.get(j).acquirePosition());
					changeArray.get(i).makePosition(reala.x, reala.y);
					changeArray.get(j).makePosition(realb.x, realb.y);
					getCollision(i, j);
				}
			}
		}

		changeArray.clear();
		changeArray.addAll(showArray);
		return true;
	}

	public void detectBoundary() {

		for (int i = 0; i < changeArray.size(); i++) {
			double xPosition = changeArray.get(i).acquirePosition().x;
			double yPosition = changeArray.get(i).acquirePosition().y;

			if (xPosition - this.sizeOfBody < -250) {
				// System.out.println("x out of left boundary\n");
				showArray.get(i).acquirePosition().x = -250 + this.sizeOfBody;

				showArray.get(i).acquireVelocity().x = -showArray.get(i).acquireVelocity().x;
			}

			if (xPosition + this.sizeOfBody > 250) {

				// System.out.println("x out of right boundary\n");

				showArray.get(i).acquirePosition().x = 250 - this.sizeOfBody;

				showArray.get(i).acquireVelocity().x = -showArray.get(i).acquireVelocity().x;
			}

			if (yPosition - this.sizeOfBody < -250) {
				// System.out.println("y out of boundary\n");
				showArray.get(i).acquirePosition().y = -250 + this.sizeOfBody;

				showArray.get(i).acquireVelocity().y = -showArray.get(i).acquireVelocity().y;
			}

			if (yPosition + this.sizeOfBody > 250) {
				// System.out.println("y out of boundary\n");
				showArray.get(i).acquirePosition().y = 250 - this.sizeOfBody;

				showArray.get(i).acquireVelocity().y = -showArray.get(i).acquireVelocity().y;
			}

		}
		changeArray.clear();
		changeArray.addAll(showArray);

	}

	public boolean moveBodies() {

		Point deltav; // dv = f/m * DT
		Point deltap; // dp = (v + dv/2) * DT
		Point force, velocity, position;
		seqBody body;

		for (int i = 0; i < numBodies; i++) {
			body = changeArray.get(i);
			force = body.acquireForce();
			velocity = body.acquireVelocity();
			position = body.acquirePosition();

			deltav = new Point(force.x / mass * DT, force.y / mass * DT);
			deltap = new Point((velocity.x + deltav.x / 2) * DT, (velocity.y + deltav.y / 2) * DT);
			// check if the delta of position is greater than the 2r

			if ((deltap.x * deltap.x + deltap.y * deltap.y) > (4.0 * body.acquireSizeOfBody()
					* body.acquireSizeOfBody())) {
				DT = DT * 0.8;
				return false;

			}

			body.makeVelocity(velocity.x + deltav.x, velocity.y + deltav.y);
			body.makePosition(position.x + deltap.x, position.y + deltap.y);
			body.makeForce(0.0, 0.0);
		}
		return true;

	}

	public void calculateForces() {
		double distance, magnitude;
		Point direction;
		seqBody body1, body2;
		Point pos1, pos2;
		Point force1, force2;

		for (int i = 0; i < numBodies - 1; i++) {
			body1 = changeArray.get(i);
			for (int j = i + 1; j < numBodies; j++) {
				body2 = changeArray.get(j);
				pos1 = body1.acquirePosition();
				pos2 = body2.acquirePosition();

				force1 = body1.acquireForce();
				force2 = body2.acquireForce();

				distance = Math.sqrt(Math.pow((pos1.x - pos2.x), 2.0) + Math.pow((pos1.y - pos2.y), 2.0));

				magnitude = (G * mass * mass) / (distance * distance);

				direction = new Point(pos2.x - pos1.x, pos2.y - pos1.y);

				force1.x = force1.x + magnitude * direction.x / distance;
				force2.x = force2.x - magnitude * direction.x / distance;

				force1.y = force1.y + magnitude * direction.y / distance;
				force2.y = force2.y - magnitude * direction.y / distance;

				showArray.get(i).makeForce(force1.x, force1.y);
				showArray.get(j).makeForce(force2.x, force2.y);

			}
		}

		changeArray.clear();
		changeArray.addAll(showArray);

	}

	public void getCollision(int start, int end) {
		double v1x = changeArray.get(start).acquireVelocity().x;
		double v1y = changeArray.get(start).acquireVelocity().y;

		double x1 = changeArray.get(start).acquirePosition().x;
		double y1 = changeArray.get(start).acquirePosition().y;

		double v2x = changeArray.get(end).acquireVelocity().x;
		double v2y = changeArray.get(end).acquireVelocity().y;

		double x2 = changeArray.get(end).acquirePosition().x;
		double y2 = changeArray.get(end).acquirePosition().y;
		double bottom = Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2);

		double v1fx = v2x * Math.pow(x2 - x1, 2) + v2y * (x2 - x1) * (y2 - y1) + v1x * Math.pow(y2 - y1, 2)
				- v1y * (x2 - x1) * (y2 - y1);

		v1fx = v1fx / bottom;

		double v1fy = v2x * (x2 - x1) * (y2 - y1) + v2y * Math.pow(y2 - y1, 2) - v1x * (y2 - y1) * (x2 - x1)
				+ v1y * Math.pow(x2 - x1, 2);

		v1fy = v1fy / bottom;

		double v2fx = v1x * Math.pow(x2 - x1, 2) + v1y * (x2 - x1) * (y2 - y1) + v2x * Math.pow(y2 - y1, 2)
				- v2y * (x2 - x1) * (y2 - y1);

		v2fx = v2fx / bottom;

		double v2fy = v1x * (x2 - x1) * (y2 - y1) + v1y * Math.pow(y2 - y1, 2) - v2x * (y2 - y1) * (x2 - x1) + v2y * Math.pow(x2 - x1, 2);

		v2fy = v2fy/ bottom;

		showArray.get(end).makeVelocity(v2fx, v2fy);

		showArray.get(start).makeVelocity(v1fx, v1fy);
		
	}

	public boolean overlap(seqBody current, seqBody another) {
		double distance = Math.sqrt(Math.pow((current.acquirePosition().x - another.acquirePosition().x), 2.0)
				+ Math.pow((current.acquirePosition().y - another.acquirePosition().y), 2.0));

		return distance <= (sizeOfBody * 2);
	}

}
