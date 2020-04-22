import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

public class MemTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(MemTest.class);

	public static void main(String[] args) {
		LOGGER.info("Double {} {}", 1, 1);
		System.out.println(MessageFormat.format("{0} hello", "10"));

		double[] tmp = new double[1024 * 1024 * 1024 / 5];
		try {
			Thread.sleep(1000000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
