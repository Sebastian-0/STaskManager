package taskmanager.win32;

import java.io.IOException;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;

public class WindowsProcess
{
  public static boolean kill(long pid) {
    HANDLE handle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_TERMINATE, false, (int) pid);
    boolean success = Kernel32.INSTANCE.TerminateProcess(handle, 1);
    Kernel32.INSTANCE.CloseHandle(handle);
    
    if (!success) {
      System.out.println("WindowsProcess: kill(): Failed to kill process (" + pid + "): " + Integer.toHexString(Native.getLastError()));
      return false;
    }
    
    return true;
  }
  
  public static boolean openPath(String path) {
    try {
      Runtime.getRuntime().exec("explorer.exe \"" + path + "\"");
    } catch (IOException e) {
      return false;
    }
    return true;
  }
}
