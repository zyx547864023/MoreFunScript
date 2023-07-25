package real_combat.campaign.missions.intel.bar.events;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator;

public class FamousShipBarEventCreator extends BaseBarEventCreator {
	
	public PortsideBarEvent createBarEvent() {
		FamousShipBarEvent famousShipBarEvent = new FamousShipBarEvent();
		famousShipBarEvent.getMarket();
		return famousShipBarEvent;
	}

	@Override
	public float getBarEventActiveDuration() {
		return 5f + (float) Math.random() * 5f;
	}

	@Override
	public float getBarEventFrequencyWeight() {
		return super.getBarEventFrequencyWeight();
	}

	@Override
	public float getBarEventTimeoutDuration() {
		return 0;
	}

	@Override
	public float getBarEventAcceptedTimeoutDuration() {
		return 0;
	}


}
