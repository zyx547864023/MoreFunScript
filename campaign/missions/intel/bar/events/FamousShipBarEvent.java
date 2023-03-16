package real_combat.campaign.missions.intel.bar.events;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModPlugin;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.intel.PersonBountyIntel;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.MutableValue;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;
import java.awt.*;
import java.util.List;
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



