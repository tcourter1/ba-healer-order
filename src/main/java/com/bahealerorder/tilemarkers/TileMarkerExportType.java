package com.bahealerorder.tilemarkers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TileMarkerExportType
{
	ASSIGNMENT_PRESET("preset"),
	STRATEGY_PRESET("wave strategy"),
	MARKER_SET("tile set"),
	STRATEGY_COLLECTION("wave strategy collection"),
	MARKER_SET_COLLECTION("tile marker set collection");

	private final String displayName;
}
