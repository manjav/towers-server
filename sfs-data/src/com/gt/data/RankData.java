package com.gt.data;

import java.io.Serializable;
public class RankData implements Serializable
{
	private static final long serialVersionUID = 1L;
	public String name;
	public int point;
	public int weeklyBattles;
	public int weeklyStars;

	public RankData(String name, int point, int weeklyBattles, int weeklyStars)
	{
	    super();
		this.name = name;
		this.point = point;
		this.weeklyBattles = weeklyBattles;
		this.weeklyStars = weeklyStars;
	}
}