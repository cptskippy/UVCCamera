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

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.Spinner;

import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.USBMonitor;

import com.serenegiant.usbcameracommon.R;

/**
 * Show a UVC camera selection dialog.
 *
 * The hosting activity must implement {@link CameraDialogParent}.
 */
public class CameraDialog extends DialogFragment {
	private static final String TAG = CameraDialog.class.getSimpleName();

	/**
	 * Host contract for a camera-selection dialog.
	 */
	public interface CameraDialogParent {
		public USBMonitor getUSBMonitor();
		public void onDialogResult(boolean canceled);
	}

	/**
	 * Show a UVC camera selection dialog on the given activity.
	 *
	 * Args:
	 *   parent: the hosting activity; must implement {@link CameraDialogParent}
	 * Returns:
	 *   the shown dialog, or null if it could not be shown
	 */
	public static CameraDialog showDialog(final Activity parent/* add parameters here if you need */) {
		CameraDialog dialog = newInstance(/* add parameters here if you need */);
		try {
			dialog.show(parent.getFragmentManager(), TAG);
		} catch (final IllegalStateException e) {
			dialog = null;
		}
    	return dialog;
	}

	/**
	 * Create a new CameraDialog with empty arguments.
	 */
	public static CameraDialog newInstance(/* add parameters here if you need */) {
		final CameraDialog dialog = new CameraDialog();
		final Bundle args = new Bundle();
		// add parameters here if you need
		dialog.setArguments(args);
		return dialog;
	}

	protected USBMonitor mUSBMonitor;
	private Spinner mSpinner;
	private DeviceListAdapter mDeviceListAdapter;

	public CameraDialog(/* no arguments */) {
	// Fragment need default constructor
	}

	@SuppressWarnings("deprecation")
	@Override
	/**
	 * Acquire the USBMonitor from the hosting activity.
	 *
	 * Args:
	 *   activity: the hosting activity; must implement {@link CameraDialogParent}
	 */
	public void onAttach(final Activity activity) {
		super.onAttach(activity);
       if (mUSBMonitor == null)
        try {
    		mUSBMonitor = ((CameraDialogParent)activity).getUSBMonitor();
        } catch (final ClassCastException e) {
    	} catch (final NullPointerException e) {
        }
		if (mUSBMonitor == null) {
        	throw new ClassCastException(activity.toString() + " must implement CameraDialogParent#getUSBController");
		}
	}

	@Override
/**
 * Use the fragment arguments when no saved instance state was provided.
 *
 * Args:
 *   savedInstanceState: the saved instance state, or null
 */
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null)
			savedInstanceState = getArguments();
	}

	@Override
	/**
	 * Copy the fragment arguments into the out-state bundle.
	 *
	 * Args:
	 *   saveInstanceState: the bundle that receives the saved state
	 */
	public void onSaveInstanceState(final Bundle saveInstanceState) {
		final Bundle args = getArguments();
		if (args != null)
			saveInstanceState.putAll(args);
		super.onSaveInstanceState(saveInstanceState);
	}

	@Override
/**
 * Build the AlertDialog that lists the available UVC cameras.
 *
 * Args:
 *   savedInstanceState: the saved instance state, or null
 * Returns:
 *   the created dialog
 */
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(initView());
    	builder.setTitle(R.string.select);
	    builder.setPositiveButton(android.R.string.ok, mOnDialogClickListener);
	    builder.setNegativeButton(android.R.string.cancel , mOnDialogClickListener);
	    builder.setNeutralButton(R.string.refresh, null);
	    final Dialog dialog = builder.create();
	    dialog.setCancelable(true);
	    dialog.setCanceledOnTouchOutside(true);
        return dialog;
	}

	/**
	 * Create the view that this fragment shows.
	 *
	 * Returns:
	 *   the inflated dialog view
	 */
	private final View initView() {
		final View rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_camera, null);
		mSpinner = (Spinner)rootView.findViewById(R.id.spinner1);
		final View empty = rootView.findViewById(android.R.id.empty);
		mSpinner.setEmptyView(empty);
		return rootView;
	}


	@Override
	/**
	 * Refresh the device list and bind the refresh button.
	 */
	public void onResume() {
		super.onResume();
		updateDevices();
	    final Button button = (Button)getDialog().findViewById(android.R.id.button3);
	    if (button != null) {
	    	button.setOnClickListener(mOnClickListener);
	    }
	}

	private final OnClickListener mOnClickListener = new OnClickListener() {
		@Override
		/**
		 * Refresh the device list when the refresh button is clicked.
		 *
		 * Args:
		 *   v: the clicked view
		 */
		public void onClick(final View v) {
			switch (v.getId()) {
			case android.R.id.button3:
				updateDevices();
				break;
			}
		}
	};

	private final DialogInterface.OnClickListener mOnDialogClickListener = new DialogInterface.OnClickListener() {
		@Override
		/**
		 * Handle the OK or Cancel button click.
		 *
		 * Args:
		 *   dialog: the clicked dialog
		 *   which: the id of the clicked button
		 */
		public void onClick(final DialogInterface dialog, final int which) {
			switch (which) {
			case DialogInterface.BUTTON_POSITIVE:
				final Object item = mSpinner.getSelectedItem();
				if (item instanceof UsbDevice) {
					mUSBMonitor.requestPermission((UsbDevice)item);
					((CameraDialogParent)getActivity()).onDialogResult(false);
				}
				break;
			case DialogInterface.BUTTON_NEGATIVE:
				((CameraDialogParent)getActivity()).onDialogResult(true);
				break;
			}
		}
	};

	@Override
	/**
	 * Notify the hosting activity that the dialog was canceled.
	 *
	 * Args:
	 *   dialog: the canceled dialog
	 */
	public void onCancel(final DialogInterface dialog) {
		((CameraDialogParent)getActivity()).onDialogResult(true);
		super.onCancel(dialog);
	}

	/**
	 * Rebuild the device list and update the spinner adapter.
	 */
	public void updateDevices() {
	//		mUSBMonitor.dumpDevices();
		final List<DeviceFilter> filter = DeviceFilter.getDeviceFilters(getActivity(), com.github.cptskippy.uvccamera.lib.R.xml.device_filter);
		mDeviceListAdapter = new DeviceListAdapter(getActivity(), mUSBMonitor.getDeviceList(filter.get(0)));
		mSpinner.setAdapter(mDeviceListAdapter);
	}

	private static final class DeviceListAdapter extends BaseAdapter {

		private final LayoutInflater mInflater;
		private final List<UsbDevice> mList;

		public DeviceListAdapter(final Context context, final List<UsbDevice>list) {
			mInflater = LayoutInflater.from(context);
			mList = list != null ? list : new ArrayList<UsbDevice>();
		}

		@Override
		/**
		 * Return the number of devices in the list.
		 */
		public int getCount() {
			return mList.size();
		}

		@Override
		/**
		 * Get the device at the given position.
		 *
		 * Args:
		 *   position: the row index
		 * Returns:
		 *   the device, or null if the position is out of range
		 */
		public UsbDevice getItem(final int position) {
			if ((position >= 0) && (position < mList.size()))
				return mList.get(position);
			else
				return null;
		}

		@Override
		/**
		 * Return the row position as the item id.
		 *
		 * Args:
		 *   position: the row index
		 * Returns:
		 *   the row position
		 */
		public long getItemId(final int position) {
			return position;
		}

		@Override
		/**
		 * Create or fill the list row for a device.
		 *
		 * Args:
		 *   position: the row index
		 *   convertView: the row view to reuse, or null
		 *   parent: the list view
		 * Returns:
		 *   the row view
		 */
		public View getView(final int position, View convertView, final ViewGroup parent) {
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.listitem_device, parent, false);
			}
			if (convertView instanceof CheckedTextView) {
				final UsbDevice device = getItem(position);
				((CheckedTextView)convertView).setText(
					String.format("UVC Camera:(%x:%x:%s)", device.getVendorId(), device.getProductId(), device.getDeviceName()));
			}
			return convertView;
		}
	}
}
