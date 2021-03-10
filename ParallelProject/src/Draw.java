import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import edu.princeton.cs.introcs.StdDraw;

public class Draw extends Thread {
	private parallel p;
	Semaphore draw;
	Semaphore fg;
	boolean if_draw;

	public Draw(parallel parallel, Semaphore draw, Semaphore fg, boolean if_draw) {
		this.p = parallel;
		this.draw = draw;
		this.fg = fg;
		this.if_draw=if_draw;
	}

	public void run() {
		for (int i = 0; i < p.numSteps; i++) {

			try {
				draw.acquire();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			// use make Graphic to draw the image
			//System.out.println("draw graph");
			if(if_draw)makeGraphic();

			try {
				// TimeUnit.SECONDS.sleep(1); //this is just for the case of
				// debug
				TimeUnit.SECONDS.sleep((long) p.DT);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			fg.release();
		}
	}

	// we reference the functions inside the StdDraw to draw the graph
	public void makeGraphic() {
		StdDraw.clear();

		for (int i = 0; i < p.showArray.size(); i++) {
			StdDraw.setPenColor(0, 255, 255);

			StdDraw.circle(p.showArray.get(i).acquirePosition().x, p.showArray.get(i).acquirePosition().y,
					p.showArray.get(i).acquireSizeOfBody());
			StdDraw.setPenColor(StdDraw.GRAY);

			StdDraw.filledCircle(p.showArray.get(i).acquirePosition().x, p.showArray.get(i).acquirePosition().y,
					p.showArray.get(i).acquireSizeOfBody());

		}
		StdDraw.show();
	}

	// In this function we calculate the real location of the object should be
	// when the overlap happened
	public Point calculateRealLocation(double distance, Point mid, Point a) {
		double x, y;
		x = mid.x - 2 * (mid.x - a.x) * p.sizeOfBody / distance;
		y = mid.y - 2 * (mid.y - a.y) * p.sizeOfBody / distance;

		return new Point(x, y);
	}
}
