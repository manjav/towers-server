package com.gerantech.towers.sfs.handlers;

import java.io.Serializable;

public class RankData implements Serializable 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
    //private static final long serialVersionUID = UUID.randomUUID().version();
	private final String name;
	private final int point;
	private final int xp;
	private int RoomId;
	private int donatesCount;
	
	public RankData(String name, int point, int xp)
	{
	    super();
		this.name = name;
		this.point = point;
		this.xp = xp;
	}
	
	public int getPoint()
	{
		return point;
	}
	public int getXp()
	{
		return xp;
	}
	public String getName()
	{
		return name;
	}
}