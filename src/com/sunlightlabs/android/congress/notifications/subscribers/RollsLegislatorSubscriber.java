package com.sunlightlabs.android.congress.notifications.subscribers;

import java.util.List;

import android.content.Intent;
import android.util.Log;

import com.sunlightlabs.android.congress.RollList;
import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.android.congress.notifications.Subscriber;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Roll;
import com.sunlightlabs.congress.services.RollService;

public class RollsLegislatorSubscriber extends Subscriber {
	private static final int PER_PAGE = 40;

	@Override
	public String decodeId(Object result) {
		return ((Roll) result).id;
	}

	@Override
	public List<?> fetchUpdates(Subscription subscription) {
		Utils.setupDrumbone(context);
		String chamber = subscription.data;
		
		try {
			return RollService.latestVotes(subscription.id, chamber, PER_PAGE, 1);
		} catch (CongressException e) {
			Log.w(Utils.TAG, "Could not fetch the latest votes for " + subscription, e);
			return null;
		}
	}
	
	@Override
	public String notificationMessage(Subscription subscription, int results) {
		if (results > 1)
			return results + " new votes cast.";
		else
			return results + " new vote cast.";
	}

	@Override
	public Intent notificationIntent(Subscription subscription) {
		return Utils.legislatorLoadIntent(subscription.id, 
			new Intent(Intent.ACTION_MAIN)
				.setClassName("com.sunlightlabs.android.congress", "com.sunlightlabs.android.congress.RollList")
				.putExtra("type", RollList.ROLLS_VOTER));
	}
}