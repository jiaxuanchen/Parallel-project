import java.util.concurrent.Semaphore;

public class Worker extends Thread {
	private int numSteps;
	private int numWorkers;
	private int id;
	private Semaphore fc;
	private Semaphore fg;
	private Semaphore sem[][];
	private parallel p;
	public int collision;

	public Worker(int workers, int n, int id, Semaphore fc, Semaphore[][] sem, Semaphore finished_graph,
			parallel parallel) {
		this.numWorkers = workers;
		this.numSteps = n;
		this.id = id;
		this.fc = fc;
		this.sem = sem;
		this.p = parallel;
		fg = finished_graph;
		this.collision = 0;
	}

	public void run() {
		for (int i = 0; i < numSteps; i++) {

			calculateForces();

			barrier();
			if (id == 0) {
				p.changeArray.clear();
				p.changeArray.addAll(p.showArray);
			}
			barrier();
			//
			// printBody(i);

			//
			while (true) {
				barrier();
				p.if_continue = true;
				if (!moveBodies())
					p.if_continue = false;
				barrier();
				if (p.if_continue == false) {
					continue;
				}
				detectBoundary();

				//
				barrier();
				/*
				 * if(id!=0) try { sem[1][id-1].acquire(); } catch
				 * (InterruptedException e) { // TODO Auto-generated catch block
				 * e.printStackTrace(); } //if(id==0){
				 * System.out.printf("id= %d\n",id); //}
				 * if(id!=numWorkers-1)sem[1][id+1].release();
				 */
				// if(id==0){
				// p.changeArray.clear();
				// p.changeArray.addAll(p.showArray);
				// }

				barrier();
				//System.out.println("here");
				if (!findCollision())
					p.if_continue = false;
				if (p.if_continue == false) {
					if (id == 0) {
						p.changeArray.clear();
						p.changeArray.addAll(p.showArray);
					}
					// ?
					continue;
				} else
					break;
			}
			//
			barrier();

			// use make Graphic to draw the image
			if (id == 0) {
				fc.release();
				try {
					fg.acquire();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			barrier();
			/*
			 * try { // TimeUnit.SECONDS.sleep(1); //this is just for the case
			 * of debug TimeUnit.SECONDS.sleep((long) DT); } catch
			 * (InterruptedException e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); }
			 */

		}

	}

	public void barrier() {
		int stage = 1;
		int i = 0;
		
		long startTime = System.currentTimeMillis();
		long estimatedTime;

		for (; stage < numWorkers; stage = stage * 2) {
			sem[i][id].release();
			// System.out.printf("NumWorkers = %d,id =%d\n",numWorkers,id);
			// System.out.printf("mutex[%d][%d] released,waiting for
			// mutex[%d][%d]\n",i,id,i,(id+stage)%numWorkers);

			try {
				sem[i][(id + stage) % numWorkers].acquire();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			i++;
		}
		estimatedTime = System.currentTimeMillis() - startTime;
		p.addBarrierTime(estimatedTime);
	}

	public void calculateForces() {
		double distance, magnitude;
		Point direction;
		seqBody body1, body2;
		Point pos1, pos2;
		Point force1, force2;

		for (int i = id; i < p.numBodies - 1; i = i + numWorkers) {
			body1 = p.changeArray.get(i);
			for (int j = i + 1; j < p.numBodies; j++) {
				body2 = p.changeArray.get(j);
				pos1 = body1.acquirePosition();
				pos2 = body2.acquirePosition();

				force1 = body1.acquireForce();
				force2 = body2.acquireForce();

				distance = Math.sqrt(Math.pow((pos1.x - pos2.x), 2.0) + Math.pow((pos1.y - pos2.y), 2.0));


				magnitude = (p.G * p.mass * p.mass) / (distance * distance);

				direction = new Point(pos2.x - pos1.x, pos2.y - pos1.y);

				force1.x = force1.x + magnitude * direction.x / distance;
				force2.x = force2.x - magnitude * direction.x / distance;

				force1.y = force1.y + magnitude * direction.y / distance;
				force2.y = force2.y - magnitude * direction.y / distance;

				p.showArray.get(i).makeForce(force1.x, force1.y);
				p.showArray.get(j).makeForce(force2.x, force2.y);

			}
		}

	}

	// this function can print all the point that at specific timestep(debug)
	private void printBody(int j) {
		int size = p.showArray.size();
		double x, vx, fx, s;
		double y, vy, fy;

		for (int i = 0; i < size; i++) {
			x = p.showArray.get(i).acquirePosition().x;
			vx = p.showArray.get(i).acquireVelocity().x;
			fx = p.showArray.get(i).acquireForce().x;
			s = p.showArray.get(i).acquireSizeOfBody();
			y = p.showArray.get(i).acquirePosition().y;
			vy = p.showArray.get(i).acquireVelocity().y;
			fy = p.showArray.get(i).acquireForce().y;

			System.out.printf("At NumTimeStep of %d:\n\t x=%f, y=%f\n\t vx=%f, vy=%f\n\t fx=%f, fy=%f\n\t size=%f\n", j,
					x, y, vx, vy, fx, fy, s);
		}
	}

	public boolean moveBodies() {

		Point deltav; // dv = f/m * DT
		Point deltap; // dp = (v + dv/2) * DT
		Point force, velocity, position;
		seqBody body;

		for (int i = id; i < p.numBodies; i = i + numWorkers) {
			body = p.changeArray.get(i);
			force = body.acquireForce();
			velocity = body.acquireVelocity();
			position = body.acquirePosition();

			deltav = new Point(force.x / p.mass * p.DT, force.y / p.mass * p.DT);
			deltap = new Point((velocity.x + deltav.x / 2) * p.DT, (velocity.y + deltav.y / 2) * p.DT);
			// check if the delta of position is greater than the 2r

			if ((deltap.x * deltap.x + deltap.y * deltap.y) > (4.0 * body.acquireSizeOfBody()
					* body.acquireSizeOfBody())) {
				p.DT = p.DT * 0.8;
				return false;

			}

			body.makeVelocity(velocity.x + deltav.x, velocity.y + deltav.y);
			body.makePosition(position.x + deltap.x, position.y + deltap.y);
			body.makeForce(0.0, 0.0);
		}
		return true;
	}

	//
	public void detectBoundary() {

		for (int i = id; i < p.changeArray.size(); i = i + numWorkers) {
			double xPosition = p.changeArray.get(i).acquirePosition().x;
			double yPosition = p.changeArray.get(i).acquirePosition().y;

			if (xPosition - p.sizeOfBody < -250) {
				p.showArray.get(i).acquirePosition().x = -250 + p.sizeOfBody;

				p.showArray.get(i).acquireVelocity().x = -p.showArray.get(i).acquireVelocity().x;
			}

			if (xPosition + p.sizeOfBody > 250) {


				p.showArray.get(i).acquirePosition().x = 250 - p.sizeOfBody;

				p.showArray.get(i).acquireVelocity().x = -p.showArray.get(i).acquireVelocity().x;
			}

			if (yPosition - p.sizeOfBody < -250) {
				p.showArray.get(i).acquirePosition().y = -250 + p.sizeOfBody;

				p.showArray.get(i).acquireVelocity().y = -p.showArray.get(i).acquireVelocity().y;
			}

			if (yPosition + p.sizeOfBody > 250) {
				p.showArray.get(i).acquirePosition().y = 250 - p.sizeOfBody;

				p.showArray.get(i).acquireVelocity().y = -p.showArray.get(i).acquireVelocity().y;
			}
		}
	}

	// In this function we find all the collision we could found in in one
	// iteration
	public boolean findCollision() {
		Point mid = new Point(0.0, 0.0);
		Point reala = new Point(0.0, 0.0);
		Point realb = new Point(0.0, 0.0);

		for (int i = id; i < p.numBodies - 1; i = i + numWorkers) {
			for (int j = i + 1; j < p.numBodies; j++) {
				p.overlapCount = 0;
				if (overlap(p.changeArray.get(i), p.changeArray.get(j)) == true) {
					this.collision += 1;
					double distance = Math.sqrt(Math.pow(
							(p.changeArray.get(i).acquirePosition().x - p.changeArray.get(j).acquirePosition().x), 2.0)
							+ Math.pow((p.changeArray.get(i).acquirePosition().y
									- p.changeArray.get(j).acquirePosition().y), 2.0));
					//System.out.printf("distance: %f\n", distance);
					//System.out.println("Deceted Collection here<-----------------------\n");
					if (distance < p.sizeOfBody && p.overlapCount > 10) {
						p.DT *= 0.5;
						//System.out.println("Deceted Collection OVERLAP here<-----------------------\n");
						p.overlapCount++;
						return false;
					}
					mid.x = (p.changeArray.get(i).acquirePosition().x + p.changeArray.get(j).acquirePosition().x) / 2;
					mid.y = (p.changeArray.get(i).acquirePosition().y + p.changeArray.get(j).acquirePosition().y) / 2;
					reala = calculateRealLocation(distance, mid, p.changeArray.get(i).acquirePosition());
					realb = calculateRealLocation(distance, mid, p.changeArray.get(j).acquirePosition());
					p.changeArray.get(i).makePosition(reala.x, reala.y);
					p.changeArray.get(j).makePosition(realb.x, realb.y);
					getCollision(i,j);
				}
			}
		}
		return true;
	}

	// In this function we calculate the real location of the object should be
	// when the overlap happened
	public Point calculateRealLocation(double distance, Point mid, Point a) {
		double x, y;
		x = mid.x - 2 * (mid.x - a.x) * p.sizeOfBody / distance;
		y = mid.y - 2 * (mid.y - a.y) * p.sizeOfBody / distance;

		return new Point(x, y);
	}

	public void getCollision(int start, int end) {
		double v1x = p.changeArray.get(start).acquireVelocity().x;
		double v1y = p.changeArray.get(start).acquireVelocity().y;

		double x1 = p.changeArray.get(start).acquirePosition().x;
		double y1 = p.changeArray.get(start).acquirePosition().y;

		double v2x = p.changeArray.get(end).acquireVelocity().x;
		double v2y = p.changeArray.get(end).acquireVelocity().y;

		double x2 = p.changeArray.get(end).acquirePosition().x;
		double y2 = p.changeArray.get(end).acquirePosition().y;

		double v1fx = v2x * Math.pow(x2 - x1, 2);
		v1fx += v2y * (x2 - x1) * (y2 - y1);
		v1fx += v1x * Math.pow(y2 - y1, 2);
		v1fx -= v1y * (x2 - x1) * (y2 - y1);
		v1fx /= Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2);

		double v1fy = v2x * (x2 - x1) * (y2 - y1);
		v1fy += v2y * Math.pow(y2 - y1, 2);
		v1fy -= v1x * (y2 - y1) * (x2 - x1);
		v1fy += v1y * Math.pow(x2 - x1, 2);
		v1fy /= Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2);

		double v2fx = v1x * Math.pow(x2 - x1, 2);
		v2fx += v1y * (x2 - x1) * (y2 - y1);
		v2fx += v2x * Math.pow(y2 - y1, 2);
		v2fx -= v2y * (x2 - x1) * (y2 - y1);
		v2fx /= Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2);

		double v2fy = v1x * (x2 - x1) * (y2 - y1);
		v2fy += v1y * Math.pow(y2 - y1, 2);
		v2fy -= v2x * (y2 - y1) * (x2 - x1);
		v2fy += v2y * Math.pow(x2 - x1, 2);
		v2fy /= Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2);

		p.showArray.get(end).makeVelocity(v2fx, v2fy);

		p.showArray.get(start).makeVelocity(v1fx, v1fy);
	}


	public boolean overlap(seqBody current, seqBody another) {
		double distance = Math.sqrt(Math.pow((current.acquirePosition().x - another.acquirePosition().x), 2.0)
				+ Math.pow((current.acquirePosition().y - another.acquirePosition().y), 2.0));

		return distance <= (current.acquireSizeOfBody() * 2);
	}

}
