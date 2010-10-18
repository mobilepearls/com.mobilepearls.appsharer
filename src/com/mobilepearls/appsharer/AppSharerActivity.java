package com.mobilepearls.appsharer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

public class AppSharerActivity extends ListActivity {

	final List<AppInfoWithLabel> installedAppsWithLabels = new ArrayList<AppInfoWithLabel>();
	final List<Dialog> dialogsToDismiss = new ArrayList<Dialog>();

	static class AppInfoWithLabel implements Comparable<AppInfoWithLabel> {
		public ApplicationInfo appInfo;
		public String label;

		@Override
		public String toString() {
			return label;
		}

		@Override
		public int compareTo(AppInfoWithLabel another) {
			return label.compareToIgnoreCase(another.label);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		updateList(true);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		AppInfoWithLabel appInfo = installedAppsWithLabels.get(position);
		String label = appInfo.label;
		String href = "http://market.android.com/details?id=" + appInfo.appInfo.packageName;
		String linkText = "Check out the app '" + label + "' at " + href;

		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, linkText);
		intent.putExtra(Intent.EXTRA_SUBJECT, "Check out " + label);
		startActivity(Intent.createChooser(intent, "Share via"));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add("Refresh").setIcon(R.drawable.ic_menu_refresh);
		menu.add("About").setIcon(android.R.drawable.ic_menu_info_details);
		return true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		for (Dialog d : dialogsToDismiss) {
			d.dismiss();
		}
		dialogsToDismiss.clear();
	}

	private void updateList(final boolean create) {
		final ProgressDialog progress = ProgressDialog.show(this, null, "Loading...", true, false, null);
		dialogsToDismiss.add(progress);
		new Thread() {
			@Override
			public void run() {
				final List<AppInfoWithLabel> appsWithLabelsLocal = create ? installedAppsWithLabels
						: new ArrayList<AppInfoWithLabel>(installedAppsWithLabels.size());
				final PackageManager packageManager = getPackageManager();
				final List<ApplicationInfo> installedApps = packageManager.getInstalledApplications(0);
				for (Iterator<ApplicationInfo> it = installedApps.iterator(); it.hasNext();) {
					ApplicationInfo appInfo = it.next();
					String label = appInfo.loadLabel(packageManager).toString();
					if (label == null || (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
							|| (appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
							|| (appInfo.flags & ApplicationInfo.FLAG_TEST_ONLY) != 0) {
						it.remove();
					} else {
						AppInfoWithLabel a = new AppInfoWithLabel();
						a.label = label;
						a.appInfo = appInfo;
						appsWithLabelsLocal.add(a);
					}
				}
				Collections.sort(appsWithLabelsLocal);

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (!progress.isShowing())
							return;

						if (create) {
							ArrayAdapter<AppInfoWithLabel> adapter = new ArrayAdapter<AppInfoWithLabel>(
									AppSharerActivity.this, R.layout.package_list_item, R.id.package_name_text_view,
									installedAppsWithLabels) {
								@Override
								public View getView(int position, View convertView, ViewGroup parent) {
									View row = super.getView(position, convertView, parent);
									ImageView testView = (ImageView) row.findViewById(R.id.package_icon_image_view);
									Drawable appIcon = packageManager.getApplicationIcon(installedAppsWithLabels
											.get(position).appInfo);
									testView.setImageDrawable(appIcon);
									return row;
								}
							};
							adapter.setNotifyOnChange(false);
							setListAdapter(adapter);
						} else {
							installedAppsWithLabels.clear();
							installedAppsWithLabels.addAll(appsWithLabelsLocal);
							((ArrayAdapter<?>) getListAdapter()).notifyDataSetChanged();
						}
						if (progress.isShowing())
							progress.dismiss();
						dialogsToDismiss.remove(progress);
					}
				});

			}
		}.start();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getTitle().equals("About")) {
			startActivity(new Intent(this, AppSharerAboutActivity.class));
		} else {
			updateList(false);
		}
		return true;
	}

}