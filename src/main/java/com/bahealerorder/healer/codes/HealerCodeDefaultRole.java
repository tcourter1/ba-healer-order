package com.bahealerorder.healer.codes;

public enum HealerCodeDefaultRole
{
	SOLO_HEALER("Default Solo Heal"),
	DH_SPAM_MAIN("Default DH Spam"),
	DH_TAG_SECOND("Default DH Tag");

	private final String presetBadge;

	HealerCodeDefaultRole(String presetBadge)
	{
		this.presetBadge = presetBadge;
	}

	public String getPresetBadge()
	{
		return presetBadge;
	}
}
