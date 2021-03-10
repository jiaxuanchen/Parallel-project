
public class seqBody {
	private Point position;
	private Point velocity;
	private Point force;
	private double sizeOfBody;

	public seqBody(Point position, Point velocity, Point force, double sizeOfBody) {
		this.position = position;
		this.velocity = velocity;
		this.force = force;
		this.sizeOfBody = sizeOfBody;
	}

	public Point acquirePosition() {
		return this.position;
	}

	public void makePosition(double x, double y) {
		this.position = new Point(x, y);
	}

	public Point acquireVelocity() {
		return this.velocity;
	}

	public void makeVelocity(double x, double y) {
		this.velocity = new Point(x, y);

	}

	public Point acquireForce() {
		return this.force;
	}

	public void makeForce(double x, double y) {
		this.force = new Point(x, y);
	}

	public double acquireSizeOfBody() {
		return this.sizeOfBody;
	}


	
}
