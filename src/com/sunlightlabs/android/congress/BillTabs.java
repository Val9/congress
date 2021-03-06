package com.sunlightlabs.android.congress;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;

public class BillTabs extends TabActivity {
	private Bill bill;
	private String tab;

	private Database database;
	private Cursor cursor;

	private ImageView star;

	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bill);
		
		Bundle extras = getIntent().getExtras();
		bill = (Bill) extras.getSerializable("bill");
		tab = extras.getString("tab");
		
		database = new Database(this);
		database.open();
		cursor = database.getBill(bill.id);
		startManagingCursor(cursor);

		setupControls();
		setupTabs();
		
		if (firstTimeLoadingStar())
        	Utils.alert(this, R.string.first_time_loading_star);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		database.close();
	}

	public void setupControls() {
		star = (ImageView) findViewById(R.id.favorite);
		star.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				toggleDatabaseFavorite();
			}
		});

		toggleFavoriteStar(cursor.getCount() == 1);
		
		((TextView) findViewById(R.id.title_text)).setText(Bill.formatCode(bill.code));
		
		View share = findViewById(R.id.share);
		share.setVisibility(View.VISIBLE);
		share.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
	    		Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText());
	    		startActivity(Intent.createChooser(intent, "Share bill via:"));
			}
		});
	}
	
	public String shareText() {
		String url = Bill.thomasUrl(bill.type, bill.number, bill.session);
		String short_title = bill.short_title;
		if (short_title != null && !short_title.equals(""))
			return "Check out the " + short_title + " on THOMAS: " + url;
		else
			return "Check out the bill " + Bill.formatCode(bill.code) + " on THOMAS: " + url;
	}
	
	private void toggleFavoriteStar(boolean enabled) {
		if (enabled)
			star.setImageResource(R.drawable.star_on);
		else
			star.setImageResource(R.drawable.star_off);
	}

	private void toggleDatabaseFavorite() {
		String id = bill.id;
		cursor.requery();
		
		if (cursor.getCount() == 1) {
			if (database.removeBill(id) != 0)
				toggleFavoriteStar(false);
		} else {
			if (database.addBill(bill) != -1) {
				toggleFavoriteStar(true);
				
				if (!Utils.hasShownFavoritesMessage(this)) {
					Utils.alert(this, R.string.bill_favorites_message);
					Utils.markShownFavoritesMessage(this);
				}
			}
		}
	}
	
	public boolean firstTimeLoadingStar() {
		if (Utils.getBooleanPreference(this, "first_time_loading_star", true)) {
			Utils.setBooleanPreference(this, "first_time_loading_star", false);
			return true;
		}
		return false;
	}

	public void setupTabs() {
		Resources res = getResources();
		TabHost tabHost = getTabHost();
		
		Utils.addTab(this, tabHost, "info", detailsIntent(), getString(R.string.tab_details), res.getDrawable(R.drawable.tab_profile));
		Utils.addTab(this, tabHost, "news", newsIntent(), getString(R.string.tab_news), res.getDrawable(R.drawable.tab_news));
		Utils.addTab(this, tabHost, "history", historyIntent(), getString(R.string.tab_history), res.getDrawable(R.drawable.tab_history));
		
		if (bill.last_vote_at != null && bill.last_vote_at.getTime() > 0)
			Utils.addTab(this, tabHost, "votes", votesIntent(), getString(R.string.tab_votes), res.getDrawable(R.drawable.tab_video));
		
		if (tab != null) 
			tabHost.setCurrentTabByTag(tab);
		else
			tabHost.setCurrentTabByTag("info");
	}
	
	
	public Intent detailsIntent() {
		return Utils.billIntent(this, BillInfo.class, bill);
	}
	
	public Intent newsIntent() {
		return new Intent(this, NewsList.class)
			.putExtra("searchTerm", searchTermFor(bill))
			.putExtra("subscriptionId", bill.id)
			.putExtra("subscriptionName", Subscriber.notificationName(bill))
			.putExtra("subscriptionClass", "NewsBillSubscriber");
	}
	
	public Intent historyIntent() {
		return new Intent(this, BillHistory.class).putExtra("bill", bill);
	}
	
	public Intent votesIntent() {
		return new Intent(this, BillVotes.class).putExtra("bill", bill);
	}
	
	// for news searching, don't use legislator.titledName() because we don't want to use the name_suffix
	private static String searchTermFor(Bill bill) {
    	if (bill.short_title != null && !bill.short_title.equals(""))
    		return bill.short_title;
    	else
    		return Bill.formatCodeShort(bill.code);
    }
}