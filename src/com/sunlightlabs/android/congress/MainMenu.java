package com.sunlightlabs.android.congress;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainMenu extends Activity {
	public static final int RESULT_ZIP = 1;
	public static final int RESULT_LASTNAME = 2;
	public static final int RESULT_STATE = 3;
	
	private static final int ABOUT = 0;
	
	private Location location;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        setupControls();
    }
	
	
	public void setupControls() {
        Button fetchZip = (Button) this.findViewById(R.id.fetch_zip);
        Button fetchLocation = (Button) this.findViewById(R.id.fetch_location);
        
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		location = null;
		
		if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
			location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		
		if (location == null && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
			location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        
    	if (location == null) {
    		fetchLocation.setEnabled(false);
	    	fetchLocation.setText("No known location.");
    	} else {
    		fetchLocation.setOnClickListener(new View.OnClickListener() {
	    		public void onClick(View v) {
	    			if (location != null)
	    				searchByLatLong(location.getLatitude(), location.getLongitude());
	    		}
	    	});
    	}
    	
    	fetchZip.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			getResponse(RESULT_ZIP);
    		}
    	});
    	
    	Button fetchLastName = (Button) this.findViewById(R.id.fetch_last_name);
    	fetchLastName.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				getResponse(RESULT_LASTNAME);
			}
		});
    	
    	Button fetchState = (Button) this.findViewById(R.id.fetch_state);
    	fetchState.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				getResponse(RESULT_STATE);
			}
		});
    }
	
	public void searchByZip(String zipCode) {
		Bundle extras = new Bundle();
		extras.putString("zip_code", zipCode);
		search(extras);
    }
	
	public void searchByLatLong(double latitude, double longitude) {
		Bundle extras = new Bundle();
		extras.putDouble("latitude", latitude);
		extras.putDouble("longitude", longitude);
		search(extras);
	}
	
	public void searchByLastName(String lastName) {
		Bundle extras = new Bundle();
		extras.putString("last_name", lastName);
		search(extras);
	}
	
	public void searchByState(String state) {
		Bundle extras = new Bundle();
		extras.putString("state", state);
		search(extras);
	}
	
	private void search(Bundle extras) {
		Intent i = new Intent();
		i.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.LegislatorList");
		i.putExtras(extras);
		startActivity(i);
	}
	
	private void getResponse(int requestCode) {
		Intent intent = new Intent();
		intent.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.GetText");
		Bundle extras = new Bundle();
		
		switch (requestCode) {
		case RESULT_ZIP:
			extras.putString("ask", "Enter a zip code:");
			extras.putString("hint", "e.g. 11216");
			extras.putInt("inputType", InputType.TYPE_CLASS_NUMBER);
			break;
		case RESULT_LASTNAME:
			extras.putString("ask", "Enter a last name:");
			extras.putString("hint", "e.g. Schumer");
			extras.putInt("inputType", InputType.TYPE_TEXT_FLAG_CAP_WORDS);
			break;
		case RESULT_STATE:
			extras.putString("ask", "2-letter state code:");
			extras.putString("hint", "e.g. NY");
			extras.putInt("inputType", InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
			break;
		default:
			break;
		}
		
		intent.putExtras(extras);
		startActivityForResult(intent, requestCode);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
		case RESULT_ZIP:
			if (resultCode == RESULT_OK) {
				String zipCode = data.getExtras().getString("response");
				if (!zipCode.equals(""))
					searchByZip(zipCode);
			}
			break;
		case RESULT_LASTNAME:
			if (resultCode == RESULT_OK) {
				String lastName = data.getExtras().getString("response");
				if (!lastName.equals(""))
					searchByLastName(lastName);
			}
			break;
		case RESULT_STATE:
			if (resultCode == RESULT_OK) {
				String state = data.getExtras().getString("response");
				if (!state.equals(""))
					searchByState(state);
			}
			break;
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
        switch(id) {
        case ABOUT:
        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	LayoutInflater inflater = getLayoutInflater();
        	LinearLayout view = (LinearLayout) inflater.inflate(R.layout.about, null);
        	
        	TextView about2 = (TextView) view.findViewById(R.id.about_2);
        	about2.setText(R.string.about_2);
        	Linkify.addLinks(about2, Linkify.WEB_URLS);
        	
        	builder.setView(view);
        	builder.setPositiveButton("Cool", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {}
			});
            return builder.create();
        default:
            return null;
        }
    }
	
	@Override 
    public boolean onCreateOptionsMenu(Menu menu) { 
	    super.onCreateOptionsMenu(menu); 
	    getMenuInflater().inflate(R.menu.main, menu);
	    return true;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) { 
    	case R.id.settings: 
    		startActivity(new Intent(this, Preferences.class));
    		break;
    	case R.id.feedback:
    		Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", getResources().getString(R.string.contact_email), null));
    		intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.contact_subject));
    		startActivity(intent);
    		break;
    	case R.id.about:
    		showDialog(ABOUT);
    		break;
    	}
    	return true;
    }
}