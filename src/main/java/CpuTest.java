import java.text.MessageFormat;

public class CpuTest {
	public static void main(String[] args) {
		final int cores = Runtime.getRuntime().availableProcessors();
		System.out.println(MessageFormat.format("{0} hello", "10"));

		for (int i = 0; i < cores; i++) {
			new Thread(() -> { while (true); }).start();
		}
	}
}
