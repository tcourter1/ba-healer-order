package com.bahealerorder.healer.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HealerCodeExportType
{
	RUN_PRESET("run preset"),
	WAVE_CODE("wave code");

	private final String displayName;
}
