package com.bahealerorder.healer.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class FeedEvent
{
	private final int healerOrder;
	private final int elapsedSeconds;
	private final int callIndex;
}
