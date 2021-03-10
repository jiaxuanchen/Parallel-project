import java.io.IOException;

public class run {
	public static void main(String[] args) throws IOException {
		String[] sample = new String[6];
		sample[0] = "32";
		sample[1] = "100";
		sample[2] = "10";
		sample[3] = "1000000";
		sample[4] = "0.5";
		sample[5] = "";

		sequential ha = new sequential(sample);

		// parallel another = new parallel(sample);
		

	}
}
