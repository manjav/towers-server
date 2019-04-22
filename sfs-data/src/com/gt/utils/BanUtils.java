package com.gt.utils;

import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;

import java.sql.SQLException;
import java.time.Instant;

/**
 * @author ManJav
 *
 */
public class BanUtils extends UtilBase
{
	static public int ADMIN_ID = 10000;
	static public int SYSTEM_ID = 10001;
	static public BanUtils getInstance()
		{
			return (BanUtils)UtilBase.get(BanUtils.class);
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
		trace(bannedQuery);
		trace(udidQuery);
		try {
			banneds = db.executeQuery(bannedQuery, new Object[]{});
			udids = db.executeQuery(udidQuery, new Object[]{});
		} catch (SQLException e) { e.printStackTrace(); }

		trace(banneds.getDump());
		trace(udids.getDump());

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
				trace(k, udids.getSFSObject(k).getUtfString("udid"));
				if( offenders.getSFSObject(i).getInt("offender").equals(udids.getSFSObject(k).getInt("player_id")) )
				{
					offenders.getSFSObject(i).putUtfString("udid", udids.getSFSObject(k).getUtfString("udid"));
					break;
				}
			}

			// ban warned offenders
			warnOrBan(db, offenders.getSFSObject(i).getInt("offender"), offenders.getSFSObject(i).getUtfString("udid"), offenders.getSFSObject(i).containsKey("ban")?2:1, now, banTime, null);
			// add log
			ret += offenders.getSFSObject(i).getInt("offender") + (offenders.getSFSObject(i).containsKey("ban") ? " banned.\n" : " warned.\n");
		}

		return ret;
	}

	public void warnOrBan(IDBManager db, Integer offender, String udid, int banMode, long now, int banTime, String message) {
		if( message == null )
			message = banMode == 1 ? "متأسفانه گزارش های زیادی مبنی بر مزاحمت یا فحاشی شما، از سایر کاربران دریافت کردیم. توجه داشته باشید به محض تکرار، کاربری شما معلق خواهد شد." : "تعلیق بعلت تخلف از قوانین بازی";
		String q = "INSERT INTO banneds (player_id, udid, message, mode, expire_at, time) VALUES (" + offender + ", '" + udid + "', '" + message + "', " + banMode + ", FROM_UNIXTIME(" + (now + banTime * 3600) + "), 1 ) ON DUPLICATE KEY UPDATE message = VALUES(message), expire_at = VALUES(expire_at), expire_at = VALUES(expire_at), time = time+1;" + ";";
		trace(q);
		try {
			db.executeUpdate(q, new Object[]{});
		} catch (SQLException e) {e.printStackTrace();}

		//OneSignalUtils.getInstance().send(message, null, offender);
		if( banMode == 1 )
			InboxUtils.getInstance().send(0, message, 10000, offender, "");
		else if( banMode > 1 )
		{
			q = "UPDATE infractions SET proceed = 1 WHERE offender = " + offender;
			trace(q);
			try {
				db.executeUpdate(q, new Object[]{});
			} catch (SQLException e) {e.printStackTrace();}
		}
	}

	public ISFSObject checkBan(int playerId, String udid, long now)
	{
		ISFSArray bannedUsers = getBannedUsers(playerId, udid, 2, now, "message, expire_at, mode");
		if( bannedUsers == null || bannedUsers.size() == 0 )
			return null;

		bannedUsers.getSFSObject(0).putLong("until", bannedUsers.getSFSObject(0).getLong("expire_at") / 1000 - now);
		bannedUsers.getSFSObject(0).removeElement("expire_at");
		trace(bannedUsers.getSFSObject(0).getDump());
		return bannedUsers.getSFSObject(0);
	}

	public ISFSArray getBannedUsers(int playerId, String udid, int mode, long now, String requestFields)
	{
		ISFSArray ret = null;
		if( requestFields == null )
			requestFields = "message, expire_at, mode";
		String timeQuery = now > 0 ? "expire_at > FROM_UNIXTIME(" + now + ")  AND" : "";
		String query = "SELECT " + requestFields + " FROM banneds WHERE " + timeQuery + " mode >= " + mode + " AND (player_id = " + playerId + (udid == null ? "" : (" OR udid = '" + udid + "'")) + ")";
		try {
			ret = ext.getParentZone().getDBManager().executeQuery(query, new Object[]{});
		} catch (SQLException e) { e.printStackTrace();}
		return ret;
	}

	public ISFSArray getInfractions(int selectedPlayer, int proceed, int size, String requestFields)
	{
		if( requestFields == null )
			requestFields = "players.name, infractions.id, infractions.reporter, infractions.offender, infractions.content, infractions.lobby, infractions.offend_at, infractions.proceed";

		String query = "SELECT " + requestFields + " FROM ";
		if( requestFields.indexOf("players") > -1 )
			query += "players INNER JOIN infractions ON players.id = infractions.offender";
		else
			query += "infractions";

		if( selectedPlayer != 0 )
			query += " WHERE " + (selectedPlayer > 0 ? "infractions.offender" : "infractions.reporter") + " = " + Math.abs(selectedPlayer);
		if( proceed > -1 )
			query += (selectedPlayer != 0 ? " AND" : " WHERE") + " infractions.proceed=" + proceed;
		query += " ORDER BY infractions.offend_at DESC";
		if( size > -1 )
			query += " LIMIT " + size;
		ISFSArray ret = new SFSArray();
		try {
			ret = ext.getParentZone().getDBManager().executeQuery(query, new Object[] {});
		} catch (SQLException e) { e.printStackTrace(); }
		return ret;
	}
}