package com.bahealerorder.common;

import java.util.Set;

final class BaPartyChatExclusions
{
	private static final Set<String> MESSAGES = Set.of(
			"All eggs doth shine glory upon the world,",
			"A golden yolk, the finest shell.",
			"Easter's majesty, of this now I yell!",
			"Attacker: Accurate/Field/Water!",
			"Attacker: Aggressive/Blunt/Earth!",
			"Attacker: Controlled/Bullet/Wind!",
			"Attacker: Defensive/Barbed/Fire!",
			"Collector: Blue eggs!",
			"Collector: Green eggs!",
			"Collector: Red eggs!",
			"Defender: Drop Crackers!",
			"Defender: Drop Tofu!",
			"Defender: Drop Worms!",
			"Healer: Poison meat!",
			"Healer: Poison tofu!",
			"Healer: Poison worms!"
	);

	static boolean contains(String message)
	{
		return message != null && MESSAGES.contains(message);
	}

	private BaPartyChatExclusions()
	{
	}
}
