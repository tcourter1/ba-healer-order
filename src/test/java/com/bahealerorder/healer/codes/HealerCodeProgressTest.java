package com.bahealerorder.healer.codes;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class HealerCodeProgressTest
{
	@Test
	public void switchesToPostRestockTargetAfterFirstTargetIsFed()
	{
		WaveCode code = HealerCodeParser.parseWaveCode(null, "Restock Split", 8, false, "6,2");
		List<FeedEvent> feedEvents = new ArrayList<>();

		for (int i = 0; i < 6; i++)
		{
			feedEvents.add(new FeedEvent(1, i, 0));
		}

		HealerCodeStatus status = HealerCodeProgress.getDisplayStatus(code, 1, 0, feedEvents);

		assertEquals(2, status.getInstruction().getTargetFoodCount());
		assertEquals(0, status.getFoodFed());
		assertEquals(CodeDisplayState.IN_PROGRESS, status.getState());
		assertEquals(8, HealerCodeProgress.getExpectedFoodForOrder(code, 1, 0));

		feedEvents.add(new FeedEvent(1, 6, 0));
		status = HealerCodeProgress.getDisplayStatus(code, 1, 0, feedEvents);

		assertEquals(2, status.getInstruction().getTargetFoodCount());
		assertEquals(1, status.getFoodFed());
		assertEquals(CodeDisplayState.IN_PROGRESS, status.getState());
	}

	@Test
	public void beforeTimingUsesFirstFoodAndAfterTimingUsesLastFood()
	{
		WaveCode code = HealerCodeParser.parseWaveCode(null, "Timing", 8, false, "2[45](45)");
		List<FeedEvent> feedEvents = new ArrayList<>();
		feedEvents.add(new FeedEvent(1, 44, 0));
		feedEvents.add(new FeedEvent(1, 50, 0));

		HealerCodeStatus status = HealerCodeProgress.getDisplayStatus(code, 1, 0, feedEvents);

		assertEquals(CodeDisplayState.COMPLETE, status.getState());

		feedEvents.set(0, new FeedEvent(1, 45, 0));
		status = HealerCodeProgress.getDisplayStatus(code, 1, 0, feedEvents);
		assertEquals(CodeDisplayState.IN_PROGRESS, status.getState());
	}
}
