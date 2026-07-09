package com.bahealerorder.healer.codes;

public enum HealerCodeExportType
{
	RUN_PRESET("run preset"),
	WAVE_CODE("wave code");

	private final String displayName;

	HealerCodeExportType(String displayName)
	{
		this.displayName = displayName;
	}

	public String getDisplayName()
	{
		return displayName;
	}
}
