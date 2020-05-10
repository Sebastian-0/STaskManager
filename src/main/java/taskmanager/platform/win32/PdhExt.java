/*
 * Copyright (c) 2020. Sebastian Hjelm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * See LICENSE for further details.
 */

package taskmanager.platform.win32;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Pdh;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.win32.W32APIOptions;

public interface PdhExt extends Pdh {
  PdhExt INSTANCE = Native.load("Pdh", PdhExt.class, W32APIOptions.DEFAULT_OPTIONS);
  
  int PdhEnumObjects(String szDataSource, String szMachineName, Pointer mszObjectList, DWORDByReference pcchBufferSize,
      int dwDetailLevel, boolean bRefresh);
}