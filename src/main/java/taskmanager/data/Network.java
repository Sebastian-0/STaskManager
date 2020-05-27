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

package taskmanager.data;

import taskmanager.MeasurementContainer;
import taskmanager.Measurements;

public class Network {
	public Measurements<Long> inRate;
	public Measurements<Long> outRate;

	public String macAddress;
	public String[] ipv4Addresses;
	public String[] ipv6Addresses;

	public String name;

	public boolean isEnabled;

	public Network() {
		inRate = new MeasurementContainer<>(0L);
		outRate = new MeasurementContainer<>(0L);

		ipv4Addresses = new String[0];
		ipv6Addresses = new String[0];
	}

	void copyFrom(Network other, boolean doFullCopy) {
		if (ipv4Addresses.length != other.ipv4Addresses.length) {
			ipv4Addresses = new String[other.ipv4Addresses.length];
		}
		if (ipv6Addresses.length != other.ipv6Addresses.length) {
			ipv6Addresses = new String[other.ipv6Addresses.length];
		}

		if (doFullCopy) {
			inRate.copyFrom(other.inRate);
			outRate.copyFrom(other.outRate);
		} else {
			inRate.copyDelta(other.inRate);
			outRate.copyDelta(other.outRate);
		}

		macAddress = other.macAddress;
		System.arraycopy(other.ipv4Addresses, 0, ipv4Addresses, 0, other.ipv4Addresses.length);
		System.arraycopy(other.ipv6Addresses, 0, ipv6Addresses, 0, other.ipv6Addresses.length);

		name = other.name;
		isEnabled = other.isEnabled;
	}

	public void compactIpv6() {
		for (int i = 0; i < ipv6Addresses.length; i++) {
			ipv6Addresses[i] = ipv6Addresses[i].replaceAll("((?::0\\b){2,}):?(?!\\S*\\b\\1:0\\b)(\\S*)", "::$2");
		}
	}
}
