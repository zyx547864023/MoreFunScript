package real_combat.campaign.missions;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;

public class FairyJump extends HubMissionWithBarEvent {

	public static float BASE_PRICE_MULT = 0.5f;
	
	protected FleetMemberAPI member;
	protected int price;
	
	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		try {
			if (barEvent) {
				findOrCreateGiver(createdAt, false, false);
			}
			//获取现在的person
			PersonAPI person = getPerson();
			//设置自由联盟
			person.setFaction("independent");
			//设置他是公民
			person.setRankId("citizen");
			person.setPostId("citizen");
			//设置名称是
			person.setName(new FullName("超可爱的", "女仆酱", FullName.Gender.FEMALE));
			//设置成女的
			person.setGender(FullName.Gender.FEMALE);
			//设置头像
			person.setPortraitSprite("graphics/portraits/independent/56.png");
			//设置回复
			person.setVoice("FairyJump");
			setPersonOverride(person);
			person = getPersonOverride();

			return true;
		}catch (Exception e){
			Global.getLogger(this.getClass()).info(e);
			return false;
		}

	}
	
	protected void updateInteractionDataImpl() {
		// this is weird - in the accept() method, the mission is aborted, which unsets
		// $sShip_ref. So: we use $sShip_ref2 in the ContactPostAccept rule
		// and $sShip_ref2 has an expiration of 0, so it'll get unset on its own later.
		set("$sShip_ref2", this);
		set("$sShip_price", Misc.getWithDGS(648));//Misc.getWithDGS(price));
		set("$sShip_priceTrue", Misc.getWithDGS(Global.getSector().getPlayerFleet().getCargo().getCredits().get()));//Misc.getWithDGS(price));
		set("$sShip_hisOrHer", getPerson().getHisOrHer());
	}
	
	@Override
	protected boolean callAction(String action, String ruleId, InteractionDialogAPI dialog, List<Token> params,
							     Map<String, MemoryAPI> memoryMap) {
		if ("showShip".equals(action)) {
			dialog.getVisualPanel().showFleetMemberInfo(member, true);
			return true;
		} else if ("showPerson".equals(action)) {
			dialog.getVisualPanel().showPersonInfo(getPerson(), true);
			return true;
		}
		return false;
	}

	@Override
	public String getBaseName() {
		return "Surplus Ship Hull"; // not used I don't think
	}
	
	@Override
	public void accept(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		// it's just an transaction immediate transaction handled in rules.csv
		// no intel item etc
		
		currentStage = new Object(); // so that the abort() assumes the mission was successful
		abort();
	}
	
}

