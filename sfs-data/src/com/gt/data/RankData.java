package com.gt.data;

import java.io.Serializable;

public class RankData implements Serializable
{
	private static final long serialVersionUID = 1L;
	public int id;
	public String name;
	public int point;
	public int xp;

	public RankData(int id, String name, int point, int xp)
	{
	    super();
		this.id = id;
		this.name = name;
		this.point = point;
		this.xp = xp;
	}
}