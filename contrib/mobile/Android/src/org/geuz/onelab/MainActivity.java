package org.geuz.onelab;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;


public class MainActivity extends Activity{

	private Gmsh _gmsh;
	private boolean _compute, _twoPane, _notify;
	private MenuItem _runStopMenuItem, _switchFragmentMenuItem;
	private ModelFragment _modelFragment;
	private OptionsFragment _optionsFragment;
	private ArrayList<String> _errors = new ArrayList<String>();
	private Dialog _errorDialog;

	public MainActivity() {
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		setContentView(R.layout.main_activity_layout);
		_gmsh = new Gmsh("", mainHandler);
		_notify = false;
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#64000000")));
		Intent intent = getIntent();
    	Bundle extras = intent.getExtras(); 
    	if(intent != null && intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW)) {
    		String tmp = intent.getData().getPath();
    		_gmsh.load(tmp);
    	}    		
    	else if(extras != null) {
    		//extras.getString("name");
    		String tmp = extras.getString("file");
    		_gmsh.load(tmp);
    	}
    	else
    		this.finish();
    	_twoPane = (findViewById(R.id.parameter_fragment) != null);
    	if(_twoPane) {
    		_optionsFragment = OptionsFragment.newInstance(_gmsh);
    		getFragmentManager().beginTransaction().add(R.id.parameter_fragment, _optionsFragment).commit();
    		_modelFragment = ModelFragment.newInstance(_gmsh);
    		getFragmentManager().beginTransaction().add(R.id.model_fragment, _modelFragment).commit();
    	}
    	else {
    		_optionsFragment = OptionsFragment.newInstance(_gmsh);
    		_modelFragment = ModelFragment.newInstance(_gmsh);
    		getFragmentManager().beginTransaction().add(R.id.model_fragment, _modelFragment).commit();
    	}
    	_optionsFragment.setOnOptionsChangedListener(new OptionsFragment.OnOptionsChangedListener() {
			
			public void OnOptionsChanged() {
				_modelFragment.requestRender();
			}
		});
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	if(!_twoPane) {
    		_switchFragmentMenuItem = menu.add(R.string.menu_parameters);
    		_switchFragmentMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    	}
    	_runStopMenuItem = menu.add(R.string.menu_run);
    	_runStopMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }
	@Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	if (item.getTitle().equals(getString(R.string.menu_parameters))) {
    		item.setTitle(R.string.menu_model);
    		FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.model_fragment, _optionsFragment);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.commit();
    	}
    	else if (item.getTitle().equals(getString(R.string.menu_model))) {
    		item.setTitle(R.string.menu_parameters);
    		FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.model_fragment, _modelFragment);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
            ft.commit();
    	}
    	else if(item.getTitle().equals(getString(R.string.menu_run))){
    		if(_switchFragmentMenuItem != null && _switchFragmentMenuItem.getTitle().equals(getString(R.string.menu_model))) {
    			_switchFragmentMenuItem.setTitle(R.string.menu_parameters);
        		FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.model_fragment, _modelFragment);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
                ft.commit();
    		}
    		new Run().execute();
    	}
    	else if(item.getTitle().equals(getString(R.string.menu_stop))){
    		_gmsh.onelabCB("stop");
    	}
		else if(item.getItemId() == android.R.id.home)
		{
			if(this._compute) {
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
    			_errorDialog = dialogBuilder.setTitle("Can't show the models list")
    			.setMessage("The compute have to be finished before you can select an other model.")
    			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
    			.show();
    		}
    		else
    			this.finish();
		}
    	return super.onMenuItemSelected(featureId, item);
    }
	
	private class Run extends AsyncTask<Void, Void, Integer[]> {

    	@Override
    	protected void onPreExecute() {
    		_compute = true;
    		_runStopMenuItem.setTitle(R.string.menu_stop);
    		super.onPreExecute();
    	}
    	
		@Override
		protected Integer[] doInBackground(Void... params) {
			_gmsh.onelabCB("compute");
			return new Integer[] {1};
		}

		@Override
		protected void onPostExecute(Integer[] result) {
			//(Vibrator) getSystemService(Context.VIBRATOR_SERVICE).vibrate(350);
			_runStopMenuItem.setTitle(R.string.menu_run);
			if(_modelFragment != null) _modelFragment.hideProgress();
			_compute = false;
			if(_notify) notifyEndOfCompute();
			super.onPostExecute(result);
		}
    	
    }
	private void showError(){
    	if(_errors.size()>0){
    		if(_errorDialog != null && _errorDialog.isShowing()) _errorDialog.dismiss();
    		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
    		_errorDialog = dialogBuilder.setTitle("Gmsh/GetDP Error(s)")
		    .setMessage(_errors.get(_errors.size()-1))
		    .setNegativeButton("Hide", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) {
		        	_errors.clear();
		        	_errorDialog.dismiss();
		        }
		     })
		    .setPositiveButton("Show more", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) {
		        	_errors.remove(_errors.size()-1);
		        	_errorDialog.dismiss();
		            showError();
		        }
		     })
		     .show();
    	}
    }
	@Override
	protected void onPause() {
		super.onPause();
		_notify = true;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		_notify = false;
	}
	
	private void notifyEndOfCompute() {
		Intent intent = new Intent(this, MainActivity.class);
	    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
	    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		Notification.Builder mBuilder =
		        new Notification.Builder(this)
		        .setSmallIcon(R.drawable.ic_launcher)
		        .setContentIntent(pendingIntent)
		        .setContentTitle("ONELAB")
		        .setDefaults(Notification.DEFAULT_ALL)
		        .setContentText("The compute is finished");
		NotificationManager mNotificationManager =
			    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(1337, mBuilder.getNotification());
	}

	private final Handler mainHandler = new Handler(){
    	public void handleMessage(android.os.Message msg) {
    		switch (msg.what) {
			case 0: // we get a message from gmsh library
				String message =(String) msg.obj;
				_errors.add(message);
				showError();
				break;
			case 1: // request render from gmsh library
				if(_modelFragment != null) _modelFragment.requestRender();
				if(_optionsFragment != null) _optionsFragment.refresh();
				break;
			case 2: // we get a message for loading
				if(_modelFragment != null) _modelFragment.showProgress((String) msg.obj);
				break;
			case 3: // we get a progress for loading
				//loading.setProgress(msg.arg1);
				break;
			default:
				break;
			}
    	};
    };
}
