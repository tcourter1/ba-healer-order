package com.bahealerorder.healer.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HealerCodeDefaultRole
{
	SOLO_HEALER("Default Solo Heal", "Solo Heal"),
	DH_SPAM_MAIN("Default DH Spam", "DH Spam"),
	DH_TAG_SECOND("Default DH Tag", "DH Tag");

	private final String presetBadge;
	private final String displayName;
}
