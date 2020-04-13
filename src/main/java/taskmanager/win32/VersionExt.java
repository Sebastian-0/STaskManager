/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.win32;

import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.win32.WinDef.WORD;

public class VersionExt {
	@FieldOrder({"wLanguage", "wCodePage"})
	public static class LANGANDCODEPAGE extends Structure {
		public WORD wLanguage;
		public WORD wCodePage;
	}
	
	@FieldOrder({"wLength", "wValueLength", "wType", "szKey", "Padding", "Children"})
	public static class STRING_TABLE extends Structure {
		public WORD   wLength;
		public WORD   wValueLength;
		public WORD   wType;
		public char   szKey;
		public WORD   Padding;
		public STRING Children;
	}
	
	@FieldOrder({"wLength", "wValueLength", "wType", "szKey", "Padding", "Value"})
	public static class STRING extends Structure {
		public WORD wLength;
		public WORD wValueLength;
		public WORD wType;
		public char szKey;
		public WORD Padding;
		public WORD Value;
	}
}
