import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class B
{
  public static void main(String[] args) throws IOException {
    
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    in.readLine();
    int[] times = Arrays.stream(in.readLine().split(" ")).mapToInt(Integer::parseInt).toArray();
    int N = times.length;

    in.close();
    
    long time = System.currentTimeMillis();
    int longest = 0;
    for (int i = 0; i < N; i++)
    {
      int sum = 0;
      for (int j = i; j < N; j++)
      {
        sum += times[j];
        if (sum / (float)(j-i+1) > 100) {
          longest = Math.max(longest, j - i + 1);
        }
      }
    }
        
     System.out.println(longest);
     System.out.println((System.currentTimeMillis() - time)/1e3);
  }
}
