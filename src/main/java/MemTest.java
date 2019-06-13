import java.text.MessageFormat;

public class MemTest {
	public static void main(String[] args) {
		System.out.println(MessageFormat.format("{0} hello", "10"));

//		while (true) ;


		double[] tmp = new double[1024 * 1024 * 1024 / 5];
		try {
			Thread.sleep(1000000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
