/*
 * Protocoder 
 * A prototyping platform for Android devices 
 * 
 * Victor Diaz Barrales victormdb@gmail.com
 *
 * Copyright (C) 2014 Victor Diaz
 * Copyright (C) 2013 Motorola Mobility LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions: 
 * 
 * The above copyright notice and this permission notice shall be included in all 
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN 
 * THE SOFTWARE.
 * 
 */

package org.protocoder;

import java.lang.reflect.Field;
import org.protocoder.appApi.Protocoder;
import org.protocoder.fragments.PreferencesFragment;
import org.protocoder.network.ProtocoderHttpServer;
import org.protocoderrunner.base.BaseActivity;
import org.protocoderrunner.events.Events.ProjectEvent;
import org.protocoderrunner.project.Project;
import org.protocoder.fragments.NewProjectDialogFragment;
import org.protocoderrunner.network.CustomWebsocketServer;
import org.protocoderrunner.project.ProjectManager;
import org.protocoderrunner.utils.MLog;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.FileObserver;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Toast;
import de.greenrobot.event.EventBus;

@SuppressLint("NewApi")
public class MainActivity extends BaseActivity {

	private static final String TAG = "MainActivity";

	MainActivity c;
	Menu mMenu;

	private FileObserver observer;

	// connection
	BroadcastReceiver mStopServerReceiver;

	private ConnectivityChangeReceiver connectivityChangeReceiver;
    private Protocoder mProtocoder;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// TOFIX set action bar overlay
		// if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
		// requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		// }

		setContentView(R.layout.activity_forfragments);
		c = this;

		// Create the action bar programmatically
		ActionBar actionBar = getActionBar();
		actionBar.setHomeButtonEnabled(true);
		// actionBar.setIcon(R.drawable.icon);
		// getWindow().setBackgroundDrawable(new
		// ColorDrawable(android.graphics.Color.TRANSPARENT));
		// getActionBar().setBackgroundDrawable(getResources().getDrawable(R.color.transparent));

		// bg
		// PApplet p = new ProcessingSketchDefaultRenderer1();
		// addFragment(p, R.id.bgProcessing, false);

		// TOFIX set action bar padding
		// if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
		// RelativeLayout contentHolder = (RelativeLayout)
		// findViewById(R.id.contentHolder);
		// TypedValue tv = new TypedValue();
		// getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
		// int actionBarHeight = (int)
		// getResources().getDimension(tv.resourceId);
		// contentHolder.setPadding(0, actionBarHeight, 0, 0);
		// }


       /*
        *  Views
        */

        mProtocoder = Protocoder.getInstance(this);

        //colors
        final int c0 = getResources().getColor(R.color.project_user_color);
        final int c1 = getResources().getColor(R.color.project_example_color);

        Intent.ShortcutIconResource icon_project = Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_script);
        Intent.ShortcutIconResource icon_example = Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_script_example);

        // Instantiate fragments
        mProtocoder.protoScripts.addScriptList(icon_project, "projects", c0, false);
        mProtocoder.protoScripts.addScriptList(icon_example, "examples", c1, true);

        //start the servers
       // mProtocoder.app.startServers();

        // Check when a file is changed in the protocoder dir
		observer = new FileObserver(ProjectManager.FOLDER_USER_PROJECTS, FileObserver.CREATE | FileObserver.DELETE) {

			@Override
			public void onEvent(int event, String file) {
				if ((FileObserver.CREATE & event) != 0) {

					MLog.d(TAG, "File created [" + ProjectManager.FOLDER_USER_PROJECTS + "/" + file + "]");

					// check if its a "create" and not equal to probe because
					// thats created every time camera is
					// launched
				} else if ((FileObserver.DELETE & event) != 0) {
					MLog.d(TAG, "File deleted [" + ProjectManager.FOLDER_USER_PROJECTS + "/" + file + "]");

				}
			}
		};

        connectivityChangeReceiver = new ConnectivityChangeReceiver();
    }

	/**
	 * onResume
	 */
	@Override
	protected void onResume() {
		super.onResume();

        //set settings
        setScreenAlwaysOn(PreferencesFragment.getScreenOn(this));

        MLog.d(TAG, "Registering as an EventBus listener in MainActivity");
		EventBus.getDefault().register(this);

		mStopServerReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
                mProtocoder.app.hardKillConnections();
			}
		};

		registerReceiver(connectivityChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        mProtocoder.app.startServers();
		IntentFilter filterSend = new IntentFilter();
		filterSend.addAction("org.protocoder.intent.action.STOP_SERVER");
		registerReceiver(mStopServerReceiver, filterSend);
		observer.startWatching();

		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");

			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception e) {
			// presumably, not relevant
		}
    }

	/**
	 * onPause
	 */
	@Override
	protected void onPause() {
		super.onPause();

		EventBus.getDefault().unregister(this);
		unregisterReceiver(mStopServerReceiver);
		observer.stopWatching();
		unregisterReceiver(connectivityChangeReceiver);

	}

	/**
	 * onDestroy
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		ViewGroup vg = (ViewGroup) findViewById(R.layout.activity_forfragments);
		if (vg != null) {
			vg.invalidate();
			vg.removeAllViews();
		}
		mProtocoder.app.killConnections();
		// TODO add stop websocket
	}


	// TODO call intent and kill it in an appropiate way
	public void onEventMainThread(ProjectEvent evt) {
		// Using transaction so the view blocks
		MLog.d(TAG, "event -> " + evt.getAction());

		if (evt.getAction() == "run") {
            Project p = evt.getProject();
            Protocoder.getInstance(this).protoScripts.run(p.getFolder(), p.getName());
		} else if (evt.getAction() == "save") {
            Project p = evt.getProject();
            Protocoder.getInstance(this).protoScripts.refresh(p.getFolder(), p.getName());
		} else if (evt.getAction() == "new") {
			MLog.d(TAG, "creating new project " + evt.getProject().getName());
            Protocoder.getInstance(this).protoScripts.create("projects", evt.getProject().getName());
		} else if (evt.getAction() == "update") {
            Protocoder.getInstance(this).protoScripts.listRefresh();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity_menu, menu);

		mMenu = menu;
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			return true;
		} else if (itemId == R.id.menu_new) {
			showEditDialog();
			return true;
		} else if (itemId == R.id.menu_help) {
            Protocoder.getInstance(this).app.showHelp(true);
			return true;
		} else if (itemId == R.id.menu_settings) {
		    Protocoder.getInstance(this).app.showSettings(true);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	/*
	 * New project dialog
	 */
	private void showEditDialog() {
		FragmentManager fm = getSupportFragmentManager();
		NewProjectDialogFragment newProjectDialog = new NewProjectDialogFragment();
		newProjectDialog.show(fm, "fragment_edit_name");
       // implements NewProjectDialogFragment.NewProjectDialogListener
        newProjectDialog.setListener(new NewProjectDialogFragment.NewProjectDialogListener() {
            @Override
            public void onFinishEditDialog(String inputText) {
                Toast.makeText(c, "Creating " + inputText, Toast.LENGTH_SHORT).show();
                Protocoder.getInstance(c).protoScripts.create("projects", inputText);
            }
        });
	}



	/*
	 * Key management
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent evt) {
		MLog.d("BACK BUTTON", "Back button was pressed");
		if (keyCode == 4) {
			Fragment fragment = getSupportFragmentManager().findFragmentByTag("editorFragment");
			if (fragment != null && fragment.isVisible()) {
				MLog.d(TAG, "Removing editor");
				removeFragment(fragment);
				return true;
			} else {
				finish();
				return true;
			}
		}
		return true;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {

		if (event.getAction() == KeyEvent.ACTION_DOWN && event.isCtrlPressed()) {

			int keyCode = event.getKeyCode();
			switch (keyCode) {
			case KeyEvent.KEYCODE_R:
				MLog.d(TAG, "run app");
				break;

			default:
				break;
			}
		}

		return super.dispatchKeyEvent(event);
	}

	// check if connection has changed
	public class ConnectivityChangeReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			debugIntent(intent, "");
            mProtocoder.app.startServers();
		}

		private void debugIntent(Intent intent, String tag) {
			Log.v(tag, "action: " + intent.getAction());
			Log.v(tag, "component: " + intent.getComponent());
			Bundle extras = intent.getExtras();
			if (extras != null) {
				for (String key : extras.keySet()) {
					Log.v(tag, "key [" + key + "]: " + extras.get(key));
				}
			} else {
				Log.v(tag, "no extras");
			}
		}

	}
}
