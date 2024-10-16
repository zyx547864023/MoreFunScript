package real_combat.campaign.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.misc.ProductionReportIntel.ProductionData;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class RC_ChangeHyperion extends HubMissionWithBarEvent {
	public static enum Stage {
		WAITING,
		DELIVERED,
		COMPLETED, // unused, left in for save compat 兼容性
		FAILED,
	}

	protected boolean armsDealer = true;
	protected ProductionData data;
	protected FactionAPI faction;
	protected MarketAPI market;
	protected FleetMemberAPI hyperionMeta;

	/**
	 * 如果玩家已经拥有某船就不创建任务
	 * 1、添加联系人到空间站
	 * 2、check 队伍里同时存在
	 * @param createdAt
	 * @param barEvent
	 * @return
	 */
	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		if (!barEvent) {
			return false;
		}
		//这里要做的效果是只有玩家自己的星球才刷
		boolean isPlayOwned = createdAt.isPlayerOwned();
		//如果已经供应了就不允许在刷 同一个任务只允许存在一次 这个用rules 控制了
		//boolean isAlreadyExist = createdAt != null && createdAt.getCommodityData(Commodities.SHIPS).getMaxSupply() > 0;
		//必须是军事星球 好像没必要
		//boolean isMilitary = isAlreadyExist && createdAt != null && Misc.isMilitary(createdAt);
		//有轨道工作站 createdAt.hasIndustry(Industries.HEAVYINDUSTRY) ||
			boolean hasIndustry = createdAt.hasIndustry(Industries.ORBITALWORKS);
		//需要玩家舰队里面有 海波龙原型
		boolean hasHyperion = false;
		List<FleetMemberAPI> playerFleetMembers = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
		for (FleetMemberAPI f:playerFleetMembers)
		{
			if("hyperion_meta".equals(f.getHullSpec().getHullId()))
			{
				hasHyperion = true;
				hyperionMeta = f;
				break;
			}
		}
		if (!(isPlayOwned&&hasIndustry&&hasHyperion))
		//if(!(hasHyperion))
		{
			return false;
		}

		//如果是酒吧事件这么搞
		ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
		findOrCreateGiver(createdAt,false,false);
		PersonAPI person = getPerson();
		if (ip.getData("MeiDo")!=null) {
			person = ip.getPerson("MeiDo");
		}
		else
		{
			person = Global.getFactory().createPerson();
			person.setFaction(Factions.INDEPENDENT);
			//设置他是军火
			person.setRankId(Ranks.CITIZEN);
			person.setPostId(Ranks.POST_ARMS_DEALER);
			person.addTag(Tags.CONTACT_SCIENCE);
			person.setImportance(PersonImportance.VERY_HIGH);
			//设置名称是
			person.setName(new FullName("超可爱的", "梅朵", FullName.Gender.FEMALE));
			//设置成女的
			person.setGender(FullName.Gender.FEMALE);
			//设置头像
			person.setPortraitSprite("graphics/portraits/independent/56.png");
			person.setId("MeiDo");
			ip.addPerson(person);
		}
		setPersonOverride(person);
		if (person == null) {return false;}
		//设置人员任务参考 没有这个就GG
		if (!setPersonMissionRef(person, "$RC_ChangeHyperion_ref")) {
			//return false;
		}
		//人在哪个市场？看不懂为什么和前面的市场不用同一个
		market = createdAt;
		if (market == null) {return false;}
		//市场要有仓库
		if (Misc.getStorage(market) == null) {return false;}
		faction = person.getFaction();

		//设置阶段
		setStartingStage(Stage.COMPLETED);
		setSuccessStage(Stage.COMPLETED);
		setFailureStage(Stage.FAILED);
		//不能放弃
		setNoAbandon();
		//任务完成设置联系人
		//setPersonIsPotentialContactOnSuccess(person, 1f);
		return true;
	}

	//更新交互数据
	protected void updateInteractionDataImpl() {
		set("$RC_ChangeHyperion_hyperionMeta", "海波龙原型");
	}
	//当前阶段的说明
	@Override
	public void addDescriptionForCurrentStage(TooltipMakerAPI info, float width, float height) {
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		if (currentStage == Stage.COMPLETED) {
			info.addPara("去空间站找他吧 %s", opad,
					faction.getBaseUIColor(), market.getName());
		}
	}

	//定制生产订单
	@Override
	public String getBaseName() {
		return "与科员人员在酒吧接触";
	}

	@Override
	protected boolean callAction(final String action, final String ruleId, final InteractionDialogAPI dialog,
								 final List<Token> params,
								 final Map<String, MemoryAPI> memoryMap) {
		if ("showShip".equals(action)) {
			dialog.getVisualPanel().showFleetMemberInfo(hyperionMeta, true);
			return true;
		}
		else if ("accept".equals(action)) {
			ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
			PersonAPI person = ip.getPerson("MeiDo");
			if (person!=null) {
				if (market != null) {
					market.getCommDirectory().addPerson(person, 0);
					market.addPerson(person);
					RC_ChangeHyperionUltimate u = new RC_ChangeHyperionUltimate();
					u.create(market, false);
					u.updateInteractionData(dialog, memoryMap);
				}
				FireBest.fire(null, dialog, memoryMap, "RC_ChangeHyperionAccept");
			}
			return true;
		}
		return super.callAction(action, ruleId, dialog, params, memoryMap);
	}
}











