/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.serenegiant.usb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.text.TextUtils;
import android.util.Log;

/**
 * Define USB device filter criteria for matching UVC cameras.
 *
 * Provides immutable filter criteria used to match UsbDevice instances
 * against vendor/product/class/subclass/protocol and optional name fields.
 * Used by USBMonitor and DeviceFilter parsing to select or exclude devices.
 *
 * Lifecycle:
 *   Creation → use for matching → garbage collected. Instances are immutable
 *   after construction and require no explicit cleanup.
 *
 * Key invariants:
 *   - mVendorId / mProductId / mClass / mSubclass / mProtocol use -1 to mean
 *     unspecified (wildcard match).
 *   - String fields are null when unspecified.
 *   - isExclude indicates whether a matching device should be excluded.
 *
 * Thread safety:
 *   Instances are immutable after construction and safe for concurrent use.
 *
 * Properties:
 *     mVendorId: USB vendor ID, -1 for unspecified.
 *     mProductId: USB product ID, -1 for unspecified.
 *     mClass: USB device/interface class, -1 for unspecified.
 *     mSubclass: USB device/interface subclass, -1 for unspecified.
 *     mProtocol: USB device/interface protocol, -1 for unspecified.
 *     mManufacturerName: Manufacturer name string or null.
 *     mProductName: Product name string or null.
 *     mSerialNumber: Serial number string or null.
 *     isExclude: True if matching devices should be excluded.
 *
 * Subclassing:
 *   Class is final and not intended for subclassing.
 */
public final class DeviceFilter {

	private static final String TAG = "DeviceFilter";

	/**
	 * USB vendor ID used for matching. -1 means unspecified (wildcard).
	 */
	public final int mVendorId;
	/**
	 * USB product ID used for matching. -1 means unspecified (wildcard).
	 */
	public final int mProductId;
	/**
	 * USB device or interface class used for matching. -1 means unspecified.
	 */
	public final int mClass;
	/**
	 * USB device or interface subclass used for matching. -1 means unspecified.
	 */
	public final int mSubclass;
	/**
	 * USB device or interface protocol used for matching. -1 means unspecified.
	 */
	public final int mProtocol;
	/**
	 * USB device manufacturer name used for matching. Null means unspecified.
	 */
	public final String mManufacturerName;
	/**
	 * USB device product name used for matching. Null means unspecified.
	 */
	public final String mProductName;
	/**
	 * USB device serial number used for matching. Null means unspecified.
	 */
	public final String mSerialNumber;
	/**
	 * True if matching devices should be excluded rather than included.
	 */
	public final boolean isExclude;

	/**
	 * Create a device filter with explicit criteria.
	 *
	 * Args:
	 *     vid: USB vendor ID, -1 for unspecified.
	 *     pid: USB product ID, -1 for unspecified.
	 *     clasz: USB class, -1 for unspecified.
	 *     subclass: USB subclass, -1 for unspecified.
	 *     protocol: USB protocol, -1 for unspecified.
	 *     manufacturer: Manufacturer name or null.
	 *     product: Product name or null.
	 *     serialNum: Serial number or null.
	 *
	 * Code Paths:
	 *     1. Delegates to full constructor with isExclude=false.
	 */
	public DeviceFilter(final int vid, final int pid, final int clasz, final int subclass,
			final int protocol, final String manufacturer, final String product, final String serialNum) {
		this(vid, pid, clasz, subclass, protocol, manufacturer, product, serialNum, false);
	}

	/**
	 * Create a device filter with explicit criteria and exclusion flag.
	 *
	 * Args:
	 *     vid: USB vendor ID, -1 for unspecified.
	 *     pid: USB product ID, -1 for unspecified.
	 *     clasz: USB class, -1 for unspecified.
	 *     subclass: USB subclass, -1 for unspecified.
	 *     protocol: USB protocol, -1 for unspecified.
	 *     manufacturer: Manufacturer name or null.
	 *     product: Product name or null.
	 *     serialNum: Serial number or null.
	 *     isExclude: True to exclude matching devices.
	 *
	 * Side Effects:
	 *     - Assigns immutable fields.
	 *
	 * Code Paths:
	 *     1. Stores parameters directly into final fields.
	 */
	public DeviceFilter(final int vid, final int pid, final int clasz, final int subclass,
			final int protocol, final String manufacturer, final String product, final String serialNum, final boolean isExclude) {
		mVendorId = vid;
		mProductId = pid;
		mClass = clasz;
		mSubclass = subclass;
		mProtocol = protocol;
		mManufacturerName = manufacturer;
		mProductName = product;
		mSerialNumber = serialNum;
		this.isExclude = isExclude;
/*		Log.i(TAG, String.format("vendorId=0x%04x,productId=0x%04x,class=0x%02x,subclass=0x%02x,protocol=0x%02x",
			mVendorId, mProductId, mClass, mSubclass, mProtocol)); */
	}

	/**
	 * Create a device filter from an existing UsbDevice.
	 *
	 * Args:
	 *     device: UsbDevice to copy vendor/product/class info from.
	 *
	 * Code Paths:
	 *     1. Delegates to UsbDevice constructor with isExclude=false.
	 */
	public DeviceFilter(final UsbDevice device) {
		this(device, false);
	}

	/**
	 * Create a device filter from an existing UsbDevice with exclusion flag.
	 *
	 * Args:
	 *     device: UsbDevice to copy vendor/product/class info from.
	 *     isExclude: True to exclude matching devices.
	 *
	 * Side Effects:
	 *     - Assigns immutable fields from device properties.
	 *
	 * Code Paths:
	 *     1. Copies vendor/product/class/subclass/protocol from device.
	 *     2. String fields remain null (device names not retrieved here).
	 */
	public DeviceFilter(final UsbDevice device, final boolean isExclude) {
		mVendorId = device.getVendorId();
		mProductId = device.getProductId();
		mClass = device.getDeviceClass();
		mSubclass = device.getDeviceSubclass();
		mProtocol = device.getDeviceProtocol();
		mManufacturerName = null;	// device.getManufacturerName();
		mProductName = null;		// device.getProductName();
		mSerialNumber = null;		// device.getSerialNumber();
		this.isExclude = isExclude;
/*		Log.i(TAG, String.format("vendorId=0x%04x,productId=0x%04x,class=0x%02x,subclass=0x%02x,protocol=0x%02x",
			mVendorId, mProductId, mClass, mSubclass, mProtocol)); */
	}

	/**
	 * Parse device filters from XML resource.
	 *
	 * Args:
	 *     context: Android Context to access resources.
	 *     deviceFilterXmlId: Resource ID of XML defining device filters.
	 *
	 * Returns:
	 *     Unmodifiable list of DeviceFilter parsed from XML.
	 *
	 * Side Effects:
	 *     - Reads XML resource via Context.getResources().
	 *     - Logs XmlPullParserException and IOException at debug level.
	 *
	 * Code Paths:
	 *     1. Parses XML stream, creates DeviceFilter for each usb-device entry.
	 *     2. On XmlPullParserException → logs and returns partial list.
	 *     3. On IOException → logs and returns partial list.
	 */
	public static List<DeviceFilter> getDeviceFilters(final Context context, final int deviceFilterXmlId) {
		final XmlPullParser parser = context.getResources().getXml(deviceFilterXmlId);
		final List<DeviceFilter> deviceFilters = new ArrayList<DeviceFilter>();
		try {
			int eventType = parser.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
	            if (eventType == XmlPullParser.START_TAG) {
					final DeviceFilter deviceFilter = readEntryOne(context, parser);
					if (deviceFilter != null) {
						deviceFilters.add(deviceFilter);
					}
	            }
				eventType = parser.next();
			}
		} catch (final XmlPullParserException e) {
			Log.d(TAG, "XmlPullParserException", e);
		} catch (final IOException e) {
			Log.d(TAG, "IOException", e);
		}

		return Collections.unmodifiableList(deviceFilters);
	}

	/**
	 * read as integer values with default value from xml(w/o exception throws)
	 * resource integer id is also resolved into integer
	 *
	 * Args:
	 *     parser: XML pull parser positioned at the attribute to read.
	 *     namespace: XML namespace for the attribute; empty string means no namespace.
	 *     name: Attribute name to read.
	 *     defaultValue: Value returned when the attribute is missing, empty, or invalid.
	 *
	 * Returns:
	 *     The resulting value.
	 */
	private static final int getAttributeInteger(final Context context, final XmlPullParser parser, final String namespace, final String name, final int defaultValue) {
		int result = defaultValue;
		try {
			String v = parser.getAttributeValue(namespace, name);
			if (!TextUtils.isEmpty(v) && v.startsWith("@")) {
				final String r = v.substring(1);
				final int resId = context.getResources().getIdentifier(r, null, context.getPackageName());
				if (resId > 0) {
					result = context.getResources().getInteger(resId);
				}
			} else {
                int radix = 10;
                if (v != null && v.length() > 2 && v.charAt(0) == '0' &&
                    (v.charAt(1) == 'x' || v.charAt(1) == 'X')) {
                    // allow hex values starting with 0x or 0X
                    radix = 16;
                    v = v.substring(2);
                }
				result = Integer.parseInt(v, radix);
			}
		} catch (final NotFoundException e) {
			result = defaultValue;
		} catch (final NumberFormatException e) {
			result = defaultValue;
		} catch (final NullPointerException e) {
			result = defaultValue;
		}
		return result;
	}

	/**
	 * read as boolean values with default value from xml(w/o exception throws)
	 * resource boolean id is also resolved into boolean
	 * if the value is zero, return false, if the value is non-zero integer, return true
	 *
	 * Args:
	 *     context: Android context used to resolve resources or system services.
	 *     parser: XML pull parser positioned at the attribute to read.
	 *     namespace: XML namespace for the attribute; empty string means no namespace.
	 *     name: Attribute name to read.
	 *     defaultValue: Value returned when the attribute is missing, empty, or invalid.
	 *
	 * Returns:
	 *     The resulting value.
	 */
	private static final boolean getAttributeBoolean(final Context context, final XmlPullParser parser, final String namespace, final String name, final boolean defaultValue) {
		boolean result = defaultValue;
		try {
			String v = parser.getAttributeValue(namespace, name);
			if ("TRUE".equalsIgnoreCase(v)) {
				result = true;
			} else if ("FALSE".equalsIgnoreCase(v)) {
				result = false;
			} else if (!TextUtils.isEmpty(v) && v.startsWith("@")) {
				final String r = v.substring(1);
				final int resId = context.getResources().getIdentifier(r, null, context.getPackageName());
				if (resId > 0) {
					result = context.getResources().getBoolean(resId);
				}
			} else {
                int radix = 10;
                if (v != null && v.length() > 2 && v.charAt(0) == '0' &&
                    (v.charAt(1) == 'x' || v.charAt(1) == 'X')) {
                    // allow hex values starting with 0x or 0X
                    radix = 16;
                    v = v.substring(2);
                }
				final int val = Integer.parseInt(v, radix);
				result = val != 0;
			}
		} catch (final NotFoundException e) {
			result = defaultValue;
		} catch (final NumberFormatException e) {
			result = defaultValue;
		} catch (final NullPointerException e) {
			result = defaultValue;
		}
		return result;
	}

	/**
	 * read as String attribute with default value from xml(w/o exception throws)
	 * resource string id is also resolved into string
	 *
	 * Args:
	 *     parser: XML pull parser positioned at the attribute to read.
	 *     namespace: XML namespace for the attribute; empty string means no namespace.
	 *     name: Attribute name to read.
	 *     defaultValue: Value returned when the attribute is missing, empty, or invalid.
	 *
	 * Returns:
	 *     The resulting value.
	 */
	private static final String getAttributeString(final Context context, final XmlPullParser parser, final String namespace, final String name, final String defaultValue) {
		String result = defaultValue;
		try {
			result = parser.getAttributeValue(namespace, name);
			if (result == null)
				result = defaultValue;
			if (!TextUtils.isEmpty(result) && result.startsWith("@")) {
				final String r = result.substring(1);
				final int resId = context.getResources().getIdentifier(r, null, context.getPackageName());
				if (resId > 0)
					result = context.getResources().getString(resId);
			}
		} catch (final NotFoundException e) {
			result = defaultValue;
		} catch (final NumberFormatException e) {
			result = defaultValue;
		} catch (final NullPointerException e) {
			result = defaultValue;
		}
		return result;
	}

	/**
	 * Read a single device filter entry from XML parser.
	 *
	 * Args:
	 *     context: Android Context for resource resolution.
	 *     parser: XmlPullParser positioned at start of usb-device element.
	 *
	 * Returns:
	 *     DeviceFilter instance or null if no entry found.
	 *
	 * Raises:
	 *     XmlPullParserException: If XML parsing fails.
	 *     IOException: If I/O error occurs while reading XML.
	 *
	 * Side Effects:
	 *     - Advances parser to end of usb-device element.
	 *
	 * Code Paths:
	 *     1. If tag is usb-device and START_TAG → parses attributes, returns filter.
	 *     2. If tag is usb-device and END_TAG and hasValue → returns built filter.
	 *     3. Otherwise continues parsing until document end, returns null.
	 */
	public static DeviceFilter readEntryOne(final Context context, final XmlPullParser parser)
			throws XmlPullParserException, IOException {
		int vendorId = -1;
		int productId = -1;
		int deviceClass = -1;
		int deviceSubclass = -1;
		int deviceProtocol = -1;
		boolean exclude = false;
		String manufacturerName = null;
		String productName = null;
		String serialNumber = null;
		boolean hasValue = false;

		String tag;
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
        	tag = parser.getName();
        	if (!TextUtils.isEmpty(tag) && (tag.equalsIgnoreCase("usb-device"))) {
        		if (eventType == XmlPullParser.START_TAG) {
        			hasValue = true;
					vendorId = getAttributeInteger(context, parser, null, "vendor-id", -1);
        			if (vendorId == -1) {
        				vendorId = getAttributeInteger(context, parser, null, "vendorId", -1);
        				if (vendorId == -1)
                			vendorId = getAttributeInteger(context, parser, null, "venderId", -1);
        			}
    				productId = getAttributeInteger(context, parser, null, "product-id", -1);
        			if (productId == -1)
            			productId = getAttributeInteger(context, parser, null, "productId", -1);
        			deviceClass = getAttributeInteger(context, parser, null, "class", -1);
        			deviceSubclass = getAttributeInteger(context, parser, null, "subclass", -1);
        			deviceProtocol = getAttributeInteger(context, parser, null, "protocol", -1);
        			manufacturerName = getAttributeString(context, parser, null, "manufacturer-name", null);
        			if (TextUtils.isEmpty(manufacturerName))
        				manufacturerName = getAttributeString(context, parser, null, "manufacture", null);
        			productName = getAttributeString(context, parser, null, "product-name", null);
        			if (TextUtils.isEmpty(productName))
        				productName = getAttributeString(context, parser, null, "product", null);
        			serialNumber = getAttributeString(context, parser, null, "serial-number", null);
        			if (TextUtils.isEmpty(serialNumber))
            			serialNumber = getAttributeString(context, parser, null, "serial", null);
					exclude = getAttributeBoolean(context, parser, null, "exclude", false);
        		} else if (eventType == XmlPullParser.END_TAG) {
        			if (hasValue) {
	        			return new DeviceFilter(vendorId, productId, deviceClass,
	        					deviceSubclass, deviceProtocol, manufacturerName, productName,
	        					serialNumber, exclude);
        			}
        		}
        	}
        	eventType = parser.next();
        }
        return null;
	}

/*	public void write(XmlSerializer serializer) throws IOException {
		serializer.startTag(null, "usb-device");
		if (mVendorId != -1) {
			serializer
					.attribute(null, "vendor-id", Integer.toString(mVendorId));
		}
		if (mProductId != -1) {
			serializer.attribute(null, "product-id",
					Integer.toString(mProductId));
		}
		if (mClass != -1) {
			serializer.attribute(null, "class", Integer.toString(mClass));
		}
		if (mSubclass != -1) {
			serializer.attribute(null, "subclass", Integer.toString(mSubclass));
		}
		if (mProtocol != -1) {
			serializer.attribute(null, "protocol", Integer.toString(mProtocol));
		}
		if (mManufacturerName != null) {
			serializer.attribute(null, "manufacturer-name", mManufacturerName);
		}
		if (mProductName != null) {
			serializer.attribute(null, "product-name", mProductName);
		}
		if (mSerialNumber != null) {
			serializer.attribute(null, "serial-number", mSerialNumber);
		}
		serializer.attribute(null, "serial-number", Boolean.toString(isExclude));
		serializer.endTag(null, "usb-device");
	} */

	/**
	 * Check whether the given class, subclass, and protocol match this filter.
	 *
	 * Args:
	 *     clasz: USB class value to match.
	 *     subclass: USB subclass value to match.
	 *     protocol: USB protocol value to match.
	 *
	 * Returns:
	 *     True if all specified filter fields match; false otherwise.
	 *
	 * Note:
	 *     This method does not consider the exclude flag; check isExclude separately.
	 */
	private boolean matches(final int clasz, final int subclass, final int protocol) {
		return ((mClass == -1 || clasz == mClass)
				&& (mSubclass == -1 || subclass == mSubclass) && (mProtocol == -1 || protocol == mProtocol));
	}

	/**
	 * Check if a UsbDevice matches this filter.
	 *
	 * Args:
	 *     device: UsbDevice to test against filter criteria.
	 *
	 * Returns:
	 *     True if device matches vendor/product and class/subclass/protocol.
	 *
	 * Code Paths:
	 *     1. Vendor ID mismatches and is specified → returns false.
	 *     2. Product ID mismatches and is specified → returns false.
	 *     3. Device class/subclass/protocol matches → returns true.
	 *     4. No device match → checks each interface for class/subclass/protocol match.
	 *     5. No matches found → returns false.
	 *
	 * Note:
	 *     isExclude flag is not evaluated here; check separately via isExclude().
	 */
	public boolean matches(final UsbDevice device) {
		if (mVendorId != -1 && device.getVendorId() != mVendorId) {
			return false;
		}
		if (mProductId != -1 && device.getProductId() != mProductId) {
			return false;
		}
/*		if (mManufacturerName != null && device.getManufacturerName() == null)
			return false;
		if (mProductName != null && device.getProductName() == null)
			return false;
		if (mSerialNumber != null && device.getSerialNumber() == null)
			return false;
		if (mManufacturerName != null && device.getManufacturerName() != null
				&& !mManufacturerName.equals(device.getManufacturerName()))
			return false;
		if (mProductName != null && device.getProductName() != null
				&& !mProductName.equals(device.getProductName()))
			return false;
		if (mSerialNumber != null && device.getSerialNumber() != null
				&& !mSerialNumber.equals(device.getSerialNumber()))
			return false; */

		// check device class/subclass/protocol
		if (matches(device.getDeviceClass(), device.getDeviceSubclass(), device.getDeviceProtocol())) {
			return true;
		}

		// if device doesn't match, check the interfaces
		final int count = device.getInterfaceCount();
		for (int i = 0; i < count; i++) {
			final UsbInterface intf = device.getInterface(i);
			if (matches(intf.getInterfaceClass(), intf.getInterfaceSubclass(), intf.getInterfaceProtocol())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Check if a UsbDevice should be excluded by this filter.
	 *
	 * Args:
	 *     device: UsbDevice to test.
	 *
	 * Returns:
	 *     True if filter matches device and isExclude is true.
	 *
	 * Code Paths:
	 *     1. If isExclude is false → returns false.
	 *     2. If device matches filter → returns true.
	 *     3. Otherwise → returns false.
	 */
	public boolean isExclude(final UsbDevice device) {
		return isExclude && matches(device);
	}

	/**
	 * Compare this filter with another filter for equivalence.
	 *
	 * Args:
	 *     f: DeviceFilter to compare against.
	 *
	 * Returns:
	 *     True if both filters have matching criteria and exclusion flag.
	 *
	 * Code Paths:
	 *     1. If isExclude differs → returns false.
	 *     2. If vendor/product/manufacturer/product/serial criteria differ → returns false.
	 *     3. If class/subclass/protocol match → returns true.
	 *     4. Otherwise → returns false.
	 */
	public boolean matches(final DeviceFilter f) {
		if (isExclude != f.isExclude) {
			return false;
		}
		if (mVendorId != -1 && f.mVendorId != mVendorId) {
			return false;
		}
		if (mProductId != -1 && f.mProductId != mProductId) {
			return false;
		}
		if (f.mManufacturerName != null && mManufacturerName == null) {
			return false;
		}
		if (f.mProductName != null && mProductName == null) {
			return false;
		}
		if (f.mSerialNumber != null && mSerialNumber == null) {
			return false;
		}
		if (mManufacturerName != null && f.mManufacturerName != null
				&& !mManufacturerName.equals(f.mManufacturerName)) {
			return false;
		}
		if (mProductName != null && f.mProductName != null
				&& !mProductName.equals(f.mProductName)) {
			return false;
		}
		if (mSerialNumber != null && f.mSerialNumber != null
				&& !mSerialNumber.equals(f.mSerialNumber)) {
			return false;
		}

		// check device class/subclass/protocol
		return matches(f.mClass, f.mSubclass, f.mProtocol);
	}

	/**
	 * Compare this filter with another object for equality.
	 *
	 * Args:
	 *     obj: Object to compare, expected DeviceFilter or UsbDevice.
	 *
	 * Returns:
	 *     True if objects are equal per filter criteria.
	 *
	 * Code Paths:
	 *     1. If wildcard fields present → returns false.
	 *     2. If obj is DeviceFilter → compares vendor/product/class/subclass/protocol and strings.
	 *     3. If obj is UsbDevice → compares device properties against filter.
	 *     4. Otherwise → returns false.
	 */
	@Override
	public boolean equals(final Object obj) {
		// can't compare if we have wildcard strings
		if (mVendorId == -1 || mProductId == -1 || mClass == -1
				|| mSubclass == -1 || mProtocol == -1) {
			return false;
		}
		if (obj instanceof DeviceFilter) {
			final DeviceFilter filter = (DeviceFilter) obj;

			if (filter.mVendorId != mVendorId
					|| filter.mProductId != mProductId
					|| filter.mClass != mClass || filter.mSubclass != mSubclass
					|| filter.mProtocol != mProtocol) {
				return false;
			}
			if ((filter.mManufacturerName != null && mManufacturerName == null)
					|| (filter.mManufacturerName == null && mManufacturerName != null)
					|| (filter.mProductName != null && mProductName == null)
					|| (filter.mProductName == null && mProductName != null)
					|| (filter.mSerialNumber != null && mSerialNumber == null)
					|| (filter.mSerialNumber == null && mSerialNumber != null)) {
				return false;
			}
			if ((filter.mManufacturerName != null && mManufacturerName != null && !mManufacturerName
					.equals(filter.mManufacturerName))
					|| (filter.mProductName != null && mProductName != null && !mProductName
							.equals(filter.mProductName))
					|| (filter.mSerialNumber != null && mSerialNumber != null && !mSerialNumber
							.equals(filter.mSerialNumber))) {
				return false;
			}
			return (filter.isExclude != isExclude);
		}
		if (obj instanceof UsbDevice) {
			final UsbDevice device = (UsbDevice) obj;
			if (isExclude
					|| (device.getVendorId() != mVendorId)
					|| (device.getProductId() != mProductId)
					|| (device.getDeviceClass() != mClass)
					|| (device.getDeviceSubclass() != mSubclass)
					|| (device.getDeviceProtocol() != mProtocol) ) {
				return false;
			}
/*			if ((mManufacturerName != null && device.getManufacturerName() == null)
					|| (mManufacturerName == null && device
							.getManufacturerName() != null)
					|| (mProductName != null && device.getProductName() == null)
					|| (mProductName == null && device.getProductName() != null)
					|| (mSerialNumber != null && device.getSerialNumber() == null)
					|| (mSerialNumber == null && device.getSerialNumber() != null)) {
				return (false);
			} */
/*			if ((device.getManufacturerName() != null && !mManufacturerName
					.equals(device.getManufacturerName()))
					|| (device.getProductName() != null && !mProductName
							.equals(device.getProductName()))
					|| (device.getSerialNumber() != null && !mSerialNumber
							.equals(device.getSerialNumber()))) {
				return (false);
			} */
			return true;
		}
		return false;
	}

	/**
	 * Compute hash code for this filter.
	 *
	 * Returns:
	 *     Integer hash based on vendor, product, class, subclass, and protocol.
	 *
	 * Side Effects:
	 *     None.
	 */
	@Override
	public int hashCode() {
		return (((mVendorId << 16) | mProductId) ^ ((mClass << 16)
				| (mSubclass << 8) | mProtocol));
	}

	/**
	 * Return string representation of this filter.
	 *
	 * Returns:
	 *     String containing all filter fields for debugging.
	 *
	 * Side Effects:
	 *     None.
	 */
	@Override
	public String toString() {
		return "DeviceFilter[mVendorId=" + mVendorId + ",mProductId="
			+ mProductId + ",mClass=" + mClass + ",mSubclass=" + mSubclass
			+ ",mProtocol=" + mProtocol
			+ ",mManufacturerName=" + mManufacturerName
			+ ",mProductName=" + mProductName
			+ ",mSerialNumber=" + mSerialNumber
			+ ",isExclude=" + isExclude
			+ "]";
	}

}
