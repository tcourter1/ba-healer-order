package com.bahealerorder.tilemarkers;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public class TimedStrategyNote
{
	private final String text;
	private final Integer tick;

	public boolean isTimed()
	{
		return tick != null;
	}
}
