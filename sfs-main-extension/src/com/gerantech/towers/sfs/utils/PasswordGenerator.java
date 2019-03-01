package com.gerantech.towers.sfs.utils;
import com.gt.utils.DBUtils;
import net.sf.json.JSONObject;

import java.time.Instant;
import java.util.Random;

public class PasswordGenerator
{

	private static final String ALPHA_CAPS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final String ALPHA = "abcdefghijklmnopqrstuvwxyz";
	private static final String NUM = "0123456789";
	private static final String SPL_CHARS = "!@#$%^&*_=+-/";

	
	public static char[] generate() 
	{
		return generate(8, 12, 1, 1, 1); 
	}
	public static char[] generate(int minLen, int maxLen, int noOfCAPSAlpha, int noOfDigits, int noOfSplChars) 
	{
		if (minLen > maxLen)
			throw new IllegalArgumentException("Min. Length > Max. Length!");
		if ((noOfCAPSAlpha + noOfDigits + noOfSplChars) > minLen)
			throw new IllegalArgumentException(
					"Min. Length should be atleast sum of (CAPS, DIGITS, SPL CHARS) Length!");
		Random rnd = new Random();
		int len = rnd.nextInt(maxLen - minLen + 1) + minLen;
		char[] pswd = new char[len];
		int index = 0;
		for (int i = 0; i < noOfCAPSAlpha; i++) {
			index = getNextIndex(rnd, len, pswd);
			pswd[index] = ALPHA_CAPS.charAt(rnd.nextInt(ALPHA_CAPS.length()));
		}
		for (int i = 0; i < noOfDigits; i++) {
			index = getNextIndex(rnd, len, pswd);
			pswd[index] = NUM.charAt(rnd.nextInt(NUM.length()));
		}
		for (int i = 0; i < noOfSplChars; i++) {
			index = getNextIndex(rnd, len, pswd);
			pswd[index] = SPL_CHARS.charAt(rnd.nextInt(SPL_CHARS.length()));
		}
		for (int i = 0; i < len; i++) {
			if (pswd[i] == 0) {
				pswd[i] = ALPHA.charAt(rnd.nextInt(ALPHA.length()));
			}
		}
		return pswd;
	}

	private static int getNextIndex(Random rnd, int len, char[] pswd) {
		int index = rnd.nextInt(len);
		while (pswd[index = rnd.nextInt(len)] != 0)
			;
		return index;
	}

    public static JSONObject getIdAndNameByInvitationCode(String invitationCode)
    {
        JSONObject ret = new JSONObject();
        int playerId = PasswordGenerator.recoverPlayerId(invitationCode);
        ret.put("playerIc", invitationCode);
        ret.put("playerName", DBUtils.getInstance().getPlayerNameById(playerId));
        return ret;
    }
	public static String getInvitationCode(int playerId)
	{
		return Integer.toString( playerId,30) + "z" + Integer.toString(playerId,35 ).toLowerCase();
	}
	public static int recoverPlayerId(String invitationCode)
	{
		return Integer.parseInt( invitationCode.split("z")[1] ,35 );
	}

	public static String getRestoreCode(int playerId)
	{
		return Integer.toString( playerId,35) + "z" + Integer.toString((int)Instant.now().getEpochSecond(),35 );
	}

	public static int validateAndRecoverPId(String restoreCode)
	{
		String[] codes = restoreCode.split("z");
		try {
			if (Integer.parseInt(codes[1], 35) + 3600 < Instant.now().getEpochSecond())
				return -1;
			System.out.print(Integer.parseInt(codes[1], 35) + " " + Instant.now().getEpochSecond() + "\n");
		} catch (Exception e) {
			e.printStackTrace();
			return  -1;
		}

		return  Integer.parseInt(codes[0], 35);
	}
}
