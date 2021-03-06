package com.sunlightlabs.android.congress;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.sunlightlabs.android.congress.notifications.Subscription;
import com.sunlightlabs.congress.models.Bill;
import com.sunlightlabs.congress.models.CongressException;
import com.sunlightlabs.congress.models.Legislator;
import com.sunlightlabs.congress.services.Drumbone;

public class Database {
	private static final int DATABASE_VERSION = 3; // updated last for version 2.9

	public boolean closed = true;

	private static final String TAG = "CongressDatabase";
	private static final String DATABASE_NAME = "congress.db";

	private static final String[] LEGISLATOR_COLUMNS = new String[] { "id", "bioguide_id",
			"govtrack_id", "first_name", "last_name", "nickname", "name_suffix", "title", "party",
			"state", "district", "gender", "congress_office", "website", "phone", "twitter_id",
			"youtube_url" };
	private static final String[] BILL_COLUMNS = new String[] { "id", "type", "number", "session",
			"code", "short_title", "official_title", "house_result", "senate_result", "passed",
			"vetoed", "override_house_result", "override_senate_result", "awaiting_signature",
			"enacted", "last_vote_at", "last_action_at", "introduced_at", "house_result_at",
			"senate_result_at", "passed_at", "vetoed_at", "override_house_result_at",
			"override_senate_result_at", "awaiting_signature_since", "enacted_at", "sponsor_id",
			"sponsor_party", "sponsor_state", "sponsor_title", "sponsor_first_name",
			"sponsor_nickname", "sponsor_last_name" };

	private static final String[] SUBSCRIPTION_COLUMNS = new String[] {
			"id", "name", "data", "last_seen_id", "notification_class" };

	private DatabaseHelper helper;
	private SQLiteDatabase database;
	private Context context;
	// uses Drumbone date format
	private static SimpleDateFormat df = new SimpleDateFormat(Drumbone.dateFormat[0]);

	public Database(Context context) {
		this.context = context;
	}

	private ContentValues fromLegislator(Legislator legislator, int size) {
		try {
			Class<?> cls = Class.forName("com.sunlightlabs.congress.models.Legislator");
			ContentValues cv = new ContentValues(size);
			for (int i = 0; i < LEGISLATOR_COLUMNS.length; i++) {
				String column = LEGISLATOR_COLUMNS[i];
				cv.put(column, (String) cls.getDeclaredField(column).get(legislator));
			}
			return cv;
		} catch (Exception e) {
			return null;
		}
	}

	private ContentValues fromLegislator(Legislator legislator) {
		return fromLegislator(legislator, LEGISLATOR_COLUMNS.length);
	}

	public long addLegislator(Legislator legislator) {
		ContentValues cv = fromLegislator(legislator);
		if (cv != null)
			return database.insert("legislators", null, cv);
		return -1;
	}

	public int removeLegislator(String id) {
		return database.delete("legislators", "id=?", new String[] { id });
	}

	public Cursor getLegislator(String id) {
		Cursor cursor = database.query("legislators", LEGISLATOR_COLUMNS, "id=?",
				new String[] { id }, null, null, null);

		cursor.moveToFirst();
		return cursor;
	}

	public Cursor getLegislators() {
		return database.rawQuery("SELECT * FROM legislators", null);
	}

	public static String formatDate(Date date) {
		return date == null ? null : df.format(date);
	}

	public static Date parseDate(String date) throws ParseException {
		return date == null ? null : df.parse(date);
	}

	public long addBill(Bill bill) {
		try {
			Class<?> cls = Class.forName("com.sunlightlabs.congress.models.Bill");
			ContentValues cv = new ContentValues(BILL_COLUMNS.length);
			for (int i = 0; i < BILL_COLUMNS.length - 7; i++) {
				String column = BILL_COLUMNS[i];
				Object field = cls.getDeclaredField(column).get(bill);
				if (field instanceof Date)
					cv.put(column, formatDate((Date) field));
				else
					cv.put(column, field == null ? null : field.toString());
			}
			Legislator sponsor = bill.sponsor;
			cv.put("sponsor_id", sponsor.getId());
			cv.put("sponsor_party", sponsor.party);
			cv.put("sponsor_state", sponsor.state);
			cv.put("sponsor_title", sponsor.title);
			cv.put("sponsor_first_name", sponsor.firstName());
			cv.put("sponsor_nickname", sponsor.nickname);
			cv.put("sponsor_last_name", sponsor.last_name);
			return database.insert("bills", null, cv);
		} catch (Exception e) {
			return -1;
		}
	}

	public int removeBill(String id) {
		return database.delete("bills", "id=?", new String[] { id });
	}

	public Cursor getBill(String id) {
		Cursor cursor = database.query("bills", BILL_COLUMNS, "id=?", new String[] { id },
				null, null, null);

		cursor.moveToFirst();
		return cursor;
	}

	public Cursor getBills() {
		return database.rawQuery("SELECT * FROM bills", null);
	}

	public Database open() {
		helper = new DatabaseHelper(context);
		closed = false;
		database = helper.getWritableDatabase();
		return this;
	}

	public boolean isOpen() {
		return database.isOpen();
	}

	public void close() {
		closed = true;
		helper.close();
	}

	public static Bill loadBill(Cursor c) throws CongressException {
		Bill bill = new Bill();

		bill.id = c.getString(c.getColumnIndex("id"));
		bill.type = c.getString(c.getColumnIndex("type"));
		bill.number = c.getInt(c.getColumnIndex("number"));
		bill.session = c.getInt(c.getColumnIndex("session"));
		bill.code = c.getString(c.getColumnIndex("code"));
		bill.short_title = c.getString(c.getColumnIndex("short_title"));
		bill.official_title = c.getString(c.getColumnIndex("official_title"));
		bill.house_result = c.getString(c.getColumnIndex("house_result"));
		bill.senate_result = c.getString(c.getColumnIndex("senate_result"));
		bill.passed = Boolean.parseBoolean(c.getString(c.getColumnIndex("passed")));
		bill.vetoed = Boolean.parseBoolean(c.getString(c.getColumnIndex("vetoed")));
		bill.override_house_result = c.getString(c.getColumnIndex("override_house_result"));
		bill.override_senate_result = c.getString(c.getColumnIndex("override_senate_result"));
		bill.awaiting_signature = Boolean.parseBoolean(c.getString(c
				.getColumnIndex("awaiting_signature")));
		bill.enacted = Boolean.parseBoolean(c.getString(c.getColumnIndex("enacted")));

		try {
			bill.last_vote_at = parseDate(c.getString(c.getColumnIndex("last_vote_at")));
			bill.last_action_at = parseDate(c.getString(c.getColumnIndex("last_action_at")));
			bill.introduced_at = parseDate(c.getString(c.getColumnIndex("introduced_at")));
			bill.house_result_at = parseDate(c.getString(c.getColumnIndex("house_result_at")));
			bill.senate_result_at = parseDate(c.getString(c.getColumnIndex("senate_result_at")));
			bill.passed_at = parseDate(c.getString(c.getColumnIndex("passed_at")));
			bill.vetoed_at = parseDate(c.getString(c.getColumnIndex("vetoed_at")));
			bill.override_house_result_at = parseDate(c.getString(c
					.getColumnIndex("override_house_result_at")));
			bill.override_senate_result_at = parseDate(c.getString(c
					.getColumnIndex("override_senate_result_at")));
			bill.awaiting_signature_since = parseDate(c.getString(c
					.getColumnIndex("awaiting_signature_since")));
			bill.enacted_at = parseDate(c.getString(c.getColumnIndex("enacted_at")));

		} catch (ParseException e) {
			throw new CongressException(e,
					"Cannot parse a date for a Bill taken from the database.");
		}

		Legislator sponsor = new Legislator();
		sponsor.id = c.getString(c.getColumnIndex("sponsor_id"));
		sponsor.party = c.getString(c.getColumnIndex("sponsor_party"));
		sponsor.state = c.getString(c.getColumnIndex("sponsor_state"));
		sponsor.title = c.getString(c.getColumnIndex("sponsor_title"));
		sponsor.first_name = c.getString(c.getColumnIndex("sponsor_first_name"));
		sponsor.last_name = c.getString(c.getColumnIndex("sponsor_last_name"));
		sponsor.nickname = c.getString(c.getColumnIndex("sponsor_nickname"));
		bill.sponsor = sponsor;

		return bill;
	}

	public static Legislator loadLegislator(Cursor c) {
		Legislator legislator = new Legislator();

		legislator.id = c.getString(c.getColumnIndex("id"));
		legislator.bioguide_id = c.getString(c.getColumnIndex("bioguide_id"));
		legislator.govtrack_id = c.getString(c.getColumnIndex("govtrack_id"));
		legislator.first_name = c.getString(c.getColumnIndex("first_name"));
		legislator.last_name = c.getString(c.getColumnIndex("last_name"));
		legislator.nickname = c.getString(c.getColumnIndex("nickname"));
		legislator.name_suffix = c.getString(c.getColumnIndex("name_suffix"));
		legislator.title = c.getString(c.getColumnIndex("title"));
		legislator.party = c.getString(c.getColumnIndex("party"));
		legislator.state = c.getString(c.getColumnIndex("state"));
		legislator.district = c.getString(c.getColumnIndex("district"));
		legislator.gender = c.getString(c.getColumnIndex("gender"));
		legislator.congress_office = c.getString(c.getColumnIndex("congress_office"));
		legislator.website = c.getString(c.getColumnIndex("website"));
		legislator.phone = c.getString(c.getColumnIndex("phone"));
		legislator.twitter_id = c.getString(c.getColumnIndex("twitter_id"));
		legislator.youtube_url = c.getString(c.getColumnIndex("youtube_url"));
		
		return legislator;
	}

	public Cursor getSubscriptions() {
		return database.rawQuery("SELECT * FROM subscriptions", null);
	}

	public Cursor getSubscription(String id, String notificationClass) {
		StringBuilder query = new StringBuilder("id=? AND notification_class=?");

		return database.query("subscriptions", SUBSCRIPTION_COLUMNS, query.toString(),
				new String[] { id, notificationClass }, null, null, null);
	}
	
	public boolean hasSubscription(String id, String notificationClass) {
		Cursor c = getSubscription(id, notificationClass);
		boolean hasSubscription = c.moveToFirst();
		c.close();
		
		return hasSubscription;
	}

	public boolean hasSubscriptions() {
		Cursor c = database.rawQuery("SELECT * FROM subscriptions", null);
		boolean hasSubscriptions = c.moveToFirst();
		c.close();
		return hasSubscriptions;
	}

	public long addSubscription(Subscription subscription) {
		ContentValues cv = new ContentValues(SUBSCRIPTION_COLUMNS.length);
		cv.put("id", subscription.id);
		cv.put("name", subscription.name);
		cv.put("notification_class", subscription.notificationClass);
		cv.put("data", subscription.data);
		cv.put("last_seen_id", (String) subscription.lastSeenId);
		
		return database.insert("subscriptions", null, cv);
	}
	
	public long removeSubscription(String id, String notificationClass) {
		return database.delete("subscriptions", "id=? AND notification_class=?", 
				new String[] { id , notificationClass });
	}
	
	public Subscription loadSubscription(Cursor c) {
		String id = c.getString(c.getColumnIndex("id"));
		String name = c.getString(c.getColumnIndex("name"));
		String data = c.getString(c.getColumnIndex("data"));
		String lastSeenId = c.getString(c.getColumnIndex("last_seen_id"));
		String notificationClass = c.getString(c.getColumnIndex("notification_class"));
		
		return new Subscription(id, name, notificationClass, data, lastSeenId);
	}

	public Subscription loadSubscription(String id, String notificationClass) {
		Cursor c = getSubscription(id, notificationClass);
		if (c.moveToFirst()) {
			Subscription subscription = loadSubscription(c);
			c.close();
			return subscription;
		} else
			return null;
	}

	public int updateLastSeenId(Subscription subscription, String lastSeenId) {
		ContentValues cv = new ContentValues(1);
		cv.put("last_seen_id", lastSeenId);

		return database.update("subscriptions", cv, "id=? AND notification_class=?",
				new String[] { subscription.id, subscription.notificationClass });
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		private String sqlCreateTable(String table, String[] columns) {
			StringBuilder sql = new StringBuilder("CREATE TABLE " + table);
			sql.append(" (_id INTEGER PRIMARY KEY AUTOINCREMENT");

			for (int i = 0; i < columns.length; i++)
				sql.append(", " + columns[i] + " TEXT");

			sql.append(");");
			return sql.toString();
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(sqlCreateTable("bills", BILL_COLUMNS));
			db.execSQL(sqlCreateTable("legislators", LEGISLATOR_COLUMNS));
			db.execSQL(sqlCreateTable("subscriptions", SUBSCRIPTION_COLUMNS));
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading " + DATABASE_NAME + " from version " + oldVersion + " to "
					+ newVersion + ", wiping old data");

			// Version 1 - Never released
			// Version 2 - Favorites (bills and legislators table), as released
			//   in version 2.6
			// Version 3 - Notifications (subscriptions table), as released
			// 	 in version 2.9

			if (oldVersion <= 2)
				db.execSQL(sqlCreateTable("subscriptions", SUBSCRIPTION_COLUMNS));
		}
	}
}
