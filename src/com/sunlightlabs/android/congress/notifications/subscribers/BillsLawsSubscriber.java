package com.sunlightlabs.android.congress.notifications.subscribers;

import java.util.List;

import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.BillList;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.services.BillService;

public class BillsLawsSubscriber extends Subscriber {
	private static final int PER_PAGE = 40;

	@Override
	public String decodeId(Object result) {
		return ((Bill) result).id;
	}

	@Override
	public List<?> fetchUpdates(Subscription subscription) {
		Utils.setupDrumbone(context);
		
		try {
			return BillService.recentLaws(PER_PAGE, 1);
		} catch (CongressException e) {
			Log.w(Utils.TAG, "Could not fetch the latest bills for " + subscription, e);
			return null;
		}
	}
	
	@Override
	public String notificationMessage(Subscription subscription, int results) {
		if (results > 1)
			return results + " new bills signed into law.";
		else
			return results + " new bill signed into law.";
	}

	@Override
	public Intent notificationIntent(Subscription subscription) {
		return new Intent(Intent.ACTION_MAIN)
			.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.BillList")
			.putExtra("type", BillList.BILLS_LAW);
	}
}