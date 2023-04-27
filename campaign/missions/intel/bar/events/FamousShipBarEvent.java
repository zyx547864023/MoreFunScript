package real_combat.campaign.missions.intel.bar.events;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson;
import com.fs.starfarer.api.util.MutableValue;
import org.lwjgl.input.Keyboard;

import java.util.*;

public class FamousShipBarEvent extends BaseBarEventWithPerson {
	public enum OptionId {
		PLAYER_SHIP_INIT,
		FLAGSHIP_INIT,
		DERELICT_INIT,
		INQUIRE,
		BOGUS_STORY,
		ACCEPT,
		DOUBLE_DOWN,
		LEAVE,
	}

	@Override
	public boolean shouldShowAtMarket(MarketAPI market) {
		return true;
	}

	@Override
	protected void regen(MarketAPI market) {
		if (this.market == market) {
			return;
		}
		super.regen(market);
	}

	@Override
	public void addPromptAndOption(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		try {
			super.addPromptAndOption(dialog, memoryMap);
			regen(dialog.getInteractionTarget().getMarket());
			TextPanelAPI text = dialog.getTextPanel();
			text.addPara("啦啦啦啊啦啦啦");
			dialog.getOptionPanel().addOption("牛逼", this);
		} catch (Exception e) {

		}
	}

	@Override
	public void init(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		try {
			super.init(dialog, memoryMap);
			done = false;
			dialog.getVisualPanel().showPersonInfo(person, true);
			optionSelected("牛逼", OptionId.PLAYER_SHIP_INIT);
		} catch (Exception e) {

		}
	}

	protected void endEvent() {
		BarEventManager.getInstance().notifyWasInteractedWith(this);
	}

	@Override
	public void optionSelected(String optionText, Object optionData) {
		try {
			MutableValue purse = Global.getSector().getPlayerFleet().getCargo().getCredits();
			OptionPanelAPI options = dialog.getOptionPanel();
			TextPanelAPI text = dialog.getTextPanel();
			options.clearOptions();

			switch ((OptionId) optionData) {
				case PLAYER_SHIP_INIT: {
					Global.getSector().getPlayerStats().addStoryPoints(1000, text, false);
					options.addOption("继续", OptionId.LEAVE);
					options.setShortcut(OptionId.LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
					endEvent();
					break;
				}
				case LEAVE: {
					done = noContinue = true;
					break;
				}
			}
		} catch (Exception e) {

		}
	}

	@Override
	protected String getPersonFaction() { return market.getFactionId(); }

	@Override
	protected String getPersonRank() { return Ranks.CITIZEN; }

	@Override
	protected String getPersonPost() { return Ranks.CITIZEN; }
}



