package com.bahealerorder.tilemarkers;

import com.bahealerorder.common.BaWaveLifecycleService;
import com.bahealerorder.common.BaRoleDetector;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.ui.overlay.OverlayManager;

@Singleton
public class GeneralTileMarkerController
{
	private final OverlayManager overlayManager;
	private final BaWaveLifecycleService waveLifecycleService;
	private final BaRoleDetector roleDetector;
	private final GeneralTileMarkerStrategyManager strategyManager;
	private final GeneralTileMarkerOverlay overlay;

	@Inject
	private GeneralTileMarkerController(
			OverlayManager overlayManager,
			BaWaveLifecycleService waveLifecycleService,
			BaRoleDetector roleDetector,
			GeneralTileMarkerStrategyManager strategyManager,
			GeneralTileMarkerOverlay overlay)
	{
		this.overlayManager = overlayManager;
		this.waveLifecycleService = waveLifecycleService;
		this.roleDetector = roleDetector;
		this.strategyManager = strategyManager;
		this.overlay = overlay;
	}

	public void startUp()
	{
		strategyManager.load();
		overlay.setController(this);
		overlayManager.add(overlay);
	}

	public void shutDown()
	{
		overlayManager.remove(overlay);
		overlay.setController(null);
	}

	boolean shouldShowTileMarkers()
	{
		return waveLifecycleService.isWaveActive() && !getCurrentMarkers().isEmpty();
	}

	List<TileMarker> getCurrentMarkers()
	{
		if (!waveLifecycleService.isWaveActive()) return Collections.emptyList();

		return strategyManager.getActiveMarkers(waveLifecycleService.getWave(), roleDetector.getCurrentRole());
	}
}
