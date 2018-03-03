package com.gerantech.towers.sfs.utils;

import com.gerantech.towers.sfs.inbox.InboxUtils;
import com.smartfoxserver.v2.SmartFoxServer;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.extensions.SFSExtension;

import java.sql.SQLException;
import java.time.Instant;

/**
 * @author ManJav
 *
 */
public class BanSystem
{
	private final SFSExtension ext;

	public BanSystem()	{
		ext = (SFSExtension) SmartFoxServer.getInstance().getZoneManager().getZoneByName("towers").getExtension();
	}
	public static BanSystem getInstance()
	{
		return new BanSystem();
	}

	public String checkOffends(String params)
	{
		String[] args = params.split(",");
		int investigateScope = Integer.parseInt(args[0]);
		int banTime = Integer.parseInt(args[1]);
		IDBManager db = ext.getParentZone().getDBManager();

		ISFSArray offenders = null;
		ISFSArray banneds = null;
		ISFSArray udids = null;
		long now = Instant.now().getEpochSecond();

		// search all offends
		try {
			offenders = db.executeQuery("SELECT COUNT(*) as cnt, offender FROM infractions WHERE offend_at > FROM_UNIXTIME(" + (now - 3600 * investigateScope) + ") GROUP BY offender HAVING cnt > 10", new Object[]{});
		} catch (SQLException e) { e.printStackTrace(); }

		if( offenders.size() == 0 )
			return "We have not any offenders.";

		// search all banneds and udid
		String bannedQuery = "SELECT * FROM banneds WHERE expire_at > FROM_UNIXTIME(" + now + ") AND mode >= 1 AND (";
		String udidQuery = "SELECT player_id, udid FROM devices WHERE ";
		String ret = "";
		for( int i = 0; i < offenders.size(); i ++ )
		{
			boolean last = i < offenders.size() - 1;
			bannedQuery += ("player_id = " + offenders.getSFSObject(i).getInt("offender")) + (last ? " OR " : ")");
			udidQuery += ("player_id = " + offenders.getSFSObject(i).getInt("offender") ) + (last ? " OR " : "");

		}
		ext.trace(bannedQuery);
		ext.trace(udidQuery);
		try {
			banneds = db.executeQuery(bannedQuery, new Object[]{});
			udids = db.executeQuery(udidQuery, new Object[]{});
		} catch (SQLException e) { e.printStackTrace(); }

		ext.trace(banneds.getDump());
		ext.trace(udids.getDump());

		for( int i = 0; i < offenders.size(); i ++ )
		{
			// ban offenders
			for( int j = 0; j < banneds.size(); j ++ )
			{
				if( offenders.getSFSObject(i).getInt("offender").equals(banneds.getSFSObject(j).getInt("player_id")) )
				{
					offenders.getSFSObject(i).putBool("ban", true);
					break;
				}
			}

			// add udid
			for( int k = 0; k < udids.size(); k ++ )
			{
				ext.trace(k, udids.getSFSObject(k).getUtfString("udid"));
				if( offenders.getSFSObject(i).getInt("offender").equals(udids.getSFSObject(k).getInt("player_id")) )
				{
					offenders.getSFSObject(i).putUtfString("udid", udids.getSFSObject(k).getUtfString("udid"));
					break;
				}
			}

			// ban warned offenders
			warnOrBan(db, offenders.getSFSObject(i).getInt("offender"), offenders.getSFSObject(i).getUtfString("udid"), offenders.getSFSObject(i).containsKey("ban")?2:1, now, banTime);
			// add log
			ret += offenders.getSFSObject(i).getInt("offender") + (offenders.getSFSObject(i).containsKey("ban") ? " banned.\n" : " warned.\n");
		}

		return ret;
	}

	private void warnOrBan(IDBManager db, Integer offender, String udid, int banMode, long now, int banTime)
	{
		String msg = "تعلیق بعلت گزارش مکرر بازیکن ها";
		String q = "INSERT INTO banneds (player_id, udid, message, mode, expire_at) VALUES (" + offender + ", '" + udid + "', '" + msg + "', " + banMode + ", FROM_UNIXTIME(" + (now + banTime * 3600) + ") ) ON DUPLICATE KEY UPDATE mode = VALUES(mode), expire_at = VALUES(expire_at);";
		ext.trace(q);
		try {
			db.executeUpdate(q, new Object[]{});
		} catch (SQLException e) { e.printStackTrace(); }

		String message = banMode == 1 ? "متأسفانه گزارش های زیادی مبنی بر مزاحمت یا فحاشی شما، از سایر کاربران دریافت کردیم. توجه داشته باشید به محض تکرار، کاربری شما معلق خواهد شد." : "متأسفانه به دلیل ادامه تخلفات شما کاربری شما معلق شد.";
		OneSignalUtils.getInstance().send(message, "", offender);
		if( banMode == 1 )
			InboxUtils.getInstance().send(0, message, "ادمین", 10000, offender, null);
	}

	public ISFSObject checkBan(int playerId, String udid, long now)
	{
		ISFSArray bannedUsers = null;
		String query = "SELECT message, expire_at FROM banneds WHERE expire_at > FROM_UNIXTIME(" + now + ") AND mode = 2 AND (player_id = " + playerId + (udid == null ? "" : (" OR udid = '" + udid + "'")) + ")";
		ext.trace(query);
		try {
			bannedUsers = ext.getParentZone().getDBManager().executeQuery(query, new Object[]{});
		} catch (SQLException e) { e.printStackTrace(); }

		if( bannedUsers.size() == 0 )
			return null;

		bannedUsers.getSFSObject(0).putLong("until", bannedUsers.getSFSObject(0).getLong("expire_at")/1000-now);
		bannedUsers.getSFSObject(0).removeElement("expire_at");
		ext.trace(bannedUsers.getSFSObject(0).getDump());
		return bannedUsers.getSFSObject(0);
	}
}