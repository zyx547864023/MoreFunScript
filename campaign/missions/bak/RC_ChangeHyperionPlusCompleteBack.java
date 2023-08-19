package real_combat.campaign.missions.bak;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 1、完成之后可以更换模块
 * 2、更换模块所需要的材料比较少
 * 3、无限循环
 */
public class RC_ChangeHyperionPlusCompleteBack extends HubMissionWithBarEvent { //implements ShipRecoveryListener {
	public static float PROD_DAYS = 1f;
	public static enum Stage {
		START,
		WAITING,
		DELIVERED,
		COMPLETED, // unused, left in for save compat 兼容性
		FAILED
	}
	protected MarketAPI market;
	protected PersonAPI callisto;
	protected FactionAPI faction;
	protected List<String> shipOptionList = new ArrayList<>();
	protected List<String> slotOptionList = new ArrayList<>();
	protected List<String> moduleOptionList = new ArrayList<>();

	protected FleetMemberAPI fleetMember;
	protected String slotId;
	protected FleetMemberAPI moduleMember;

	transient protected InteractionDialogAPI interactionDialog;
	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		if (!setGlobalReference("$RC_ChangeHyperionPlusComplete_ref", null)) {
			return false;
		}

		callisto = getImportantPerson("MeiDo");
		if (callisto == null) return false;
		faction = callisto.getFaction();

		setPersonOverride(callisto);

		//人在哪个市场？看不懂为什么和前面的市场不用同一个
		market = createdAt;
		if (market == null) {return false;}
		//市场要有仓库
		if (Misc.getStorage(market) == null) {return false;}
		//设置阶段
		setStartingStage(Stage.WAITING);
		setSuccessStage(Stage.DELIVERED);
		setFailureStage(Stage.FAILED);
		//不能放弃
		setNoAbandon();
		//经过多少天从这个状态到另一个状态
		connectWithDaysElapsed(Stage.WAITING, Stage.DELIVERED, PROD_DAYS);
		//connectWithDaysElapsed(Stage.WAITING, Stage.DELIVERED, 1f);
		//set 阶段 On 市场 不文明
		setStageOnMarketDecivilized(Stage.FAILED, market);
		return true;
	}

	protected boolean callAction(String action, String ruleId, InteractionDialogAPI dialog, List<Token> params,
								 Map<String, MemoryAPI> memoryMap) {
		//第一次由callAction添加按钮 按钮内容是 玩家里面的船
		//一共三级 船 船插 想要更换的船
		if ("showOption".equals(action)) {
			boolean hasIndustry = market.hasIndustry(Industries.ORBITALWORKS);
			if (!hasIndustry)
			{
				//家没了
				FireBest.fire(null, dialog, memoryMap, "RC_ChangeHyperionIndustry");
				return true;
			}

			interactionDialog = dialog;
			OptionPanelAPI options = dialog.getOptionPanel();
			TextPanelAPI text = dialog.getTextPanel();

			FleetDataAPI fleetData = Global.getSector().getPlayerFleet().getFleetData();
			List<FleetMemberAPI> playerFleetMembers = fleetData.getMembersListCopy();
			shipOptionList = new ArrayList<>();
			for (FleetMemberAPI f:playerFleetMembers)
			{
				//按钮ID就是船的ID
				if (f.getVariant().getModuleSlots().size()>0)
				{
					//shipOptionList.add(f);
				}
			}
		}
		else if ("change".equals(action)) {
			interactionDialog = dialog;
			OptionPanelAPI options = dialog.getOptionPanel();
			TextPanelAPI text = dialog.getTextPanel();

			//取到要改的船 要改的插槽 要改的
			ShipVariantAPI variant = fleetMember.getVariant();
			ShipVariantAPI nowModuleVarint = fleetMember.getVariant().getModuleVariant(slotId);
			ShipVariantAPI newModuleVarint = moduleMember.getVariant();
			/*
			String shipName = fleetMember.getShipName();
			String nowHullName = nowModuleVarint.getHullSpec().getHullName();
			String newHullName = newModuleVarint.getHullSpec().getHullName();

			if(nowHullName.equals(newHullName))
			{
				//addPara(String format, Color color, Color hl, String... highlights)
				text.addPara(shipName + " （" + slotId + "） 当前模块 " + "【"+nowHullName+"】" + "",Color.WHITE,Color.YELLOW,"");
				//已经是
				FireBest.fire(null, dialog, memoryMap, "RC_ChangeHyperionAlready");
				return true;
			}
			else
			{
			 */
			variant.setSource(VariantSource.REFIT);
			nowModuleVarint.setSource(VariantSource.REFIT);
			newModuleVarint.setSource(VariantSource.REFIT);
			variant.setModuleVariant(slotId,newModuleVarint);
			//已改完

			//改完之后返回到主列表
			return true;
		}
		//WS0002
		else if ("changeConquest".equals(action)) {
			boolean hasIndustry = market.hasIndustry(Industries.ORBITALWORKS);
			if (!hasIndustry)
			{
				//家没了
				FireBest.fire(null, dialog, memoryMap, "RC_ChangeHyperionIndustry");
				return true;
			}
			//这里直接换零件不搞生产模式了
			//获取玩家的舰队
			FleetDataAPI fleetData = Global.getSector().getPlayerFleet().getFleetData();
			List<FleetMemberAPI> playerFleetMembers = fleetData.getMembersListCopy();
			FleetMemberAPI hyperion = null;
			FleetMemberAPI conquest = null;
			for (FleetMemberAPI f:playerFleetMembers)
			{
				if("hyperion_plus".equals(f.getHullSpec().getHullId())&&hyperion==null) {
					hyperion = f;

				}
				else if("conquest".equals(f.getHullSpec().getHullId())&&conquest==null) {
					conquest = f;
				}
			}
			if (hyperion == null||conquest == null) {
				//材料不足
				FireBest.fire(null, dialog, memoryMap, "RC_ChangeHyperionNeed");
				return true;
			}
			else {
				ShipVariantAPI variant = conquest.getVariant();
				ShipVariantAPI moduleVarint = hyperion.getVariant().getModuleVariant("WS0002");
				if(variant.getHullSpec().getHullId().equals(moduleVarint.getHullSpec().getHullId()))
				{
					//左侧已经是
					set("$RC_ChangeHyperionPlusComplete_mod", variant.getHullSpec().getHullName());
					updateInteractionData(dialog,memoryMap);
					FireBest.fire(null, dialog, memoryMap, "RC_ChangeHyperionAlready");
				}
				else
				{
					hyperion.getVariant().setSource(VariantSource.REFIT);
					hyperion.getVariant().getModuleVariant("WS0002").setSource(VariantSource.REFIT);
					variant.setSource(VariantSource.REFIT);
					hyperion.getVariant().setModuleVariant("WS0002",variant);
				}
			}
			return true;
		}
		if ("changeAstral".equals(action)) {
			boolean hasIndustry = market.hasIndustry(Industries.ORBITALWORKS);
			if (!hasIndustry)
			{
				//家没了
				FireBest.fire(null, dialog, memoryMap, "RC_ChangeHyperionIndustry");
				return true;
			}
			//这里直接换零件不搞生产模式了
			//获取玩家的舰队
			FleetDataAPI fleetData = Global.getSector().getPlayerFleet().getFleetData();
			List<FleetMemberAPI> playerFleetMembers = fleetData.getMembersListCopy();
			FleetMemberAPI hyperion = null;
			FleetMemberAPI astral = null;
			for (FleetMemberAPI f:playerFleetMembers)
			{
				if("hyperion_plus".equals(f.getHullSpec().getHullId())&&hyperion==null) {
					hyperion = f;

				}
				else if("astral".equals(f.getHullSpec().getHullId())&&astral==null) {
					astral = f;
				}
			}
			if (hyperion == null||astral == null) {
				//材料不足
				FireBest.fire(null, dialog, memoryMap, "RC_ChangeHyperionNeed");
				return true;
			}
			else {
				ShipVariantAPI variant = astral.getVariant();
				ShipVariantAPI moduleVarint = hyperion.getVariant().getModuleVariant("WS0002");
				if(variant.getHullSpec().getHullId().equals(moduleVarint.getHullSpec().getHullId()))
				{
					//左侧已经是
					set("$RC_ChangeHyperionPlusComplete_mod", variant.getHullSpec().getHullName());
					updateInteractionData(dialog,memoryMap);
					FireBest.fire(null, dialog, memoryMap, "RC_ChangeHyperionAlready");
				}
				else
				{
					hyperion.getVariant().setSource(VariantSource.REFIT);
					hyperion.getVariant().getModuleVariant("WS0002").setSource(VariantSource.REFIT);
					variant.setSource(VariantSource.REFIT);
					hyperion.getVariant().setModuleVariant("WS0002",variant);
				}
			}
			return true;
		}
		if ("changeOdyssey".equals(action)) {
			boolean hasIndustry = market.hasIndustry(Industries.ORBITALWORKS);
			if (!hasIndustry)
			{
				//家没了
				FireBest.fire(null, dialog, memoryMap, "RC_ChangeHyperionIndustry");
				return true;
			}
			//这里直接换零件不搞生产模式了
			//获取玩家的舰队
			FleetDataAPI fleetData = Global.getSector().getPlayerFleet().getFleetData();
			List<FleetMemberAPI> playerFleetMembers = fleetData.getMembersListCopy();
			FleetMemberAPI hyperion = null;
			FleetMemberAPI odyssey = null;
			for (FleetMemberAPI f:playerFleetMembers)
			{
				if("hyperion_plus".equals(f.getHullSpec().getHullId())&&hyperion==null) {
					hyperion = f;

				}
				else if("odyssey".equals(f.getHullSpec().getHullId())&&odyssey==null) {
					odyssey = f;
				}
			}
			if (hyperion == null||odyssey == null) {
				//材料不足
				FireBest.fire(null, dialog, memoryMap, "RC_ChangeHyperionNeed");
				return true;
			}
			else {
				ShipVariantAPI variant = odyssey.getVariant();
				ShipVariantAPI moduleVarint = hyperion.getVariant().getModuleVariant("WS0002");
				if(variant.getHullSpec().getHullId().equals(moduleVarint.getHullSpec().getHullId()))
				{
					//左侧已经是
					set("$RC_ChangeHyperionPlusComplete_mod", variant.getHullSpec().getHullName());
					updateInteractionData(dialog,memoryMap);
					FireBest.fire(null, dialog, memoryMap, "RC_ChangeHyperionAlready");
				}
				else
				{
					hyperion.getVariant().setSource(VariantSource.REFIT);
					hyperion.getVariant().getModuleVariant("WS0002").setSource(VariantSource.REFIT);
					variant.setSource(VariantSource.REFIT);
					hyperion.getVariant().setModuleVariant("WS0002",variant);
				}
			}
			return true;
		}
		return super.callAction(action, ruleId, dialog, params, memoryMap);
	}

	protected void updateInteractionDataImpl() {
		set("$RC_ChangeHyperionPlusComplete_days", "" + (int) PROD_DAYS);
		//星体
		set("$RC_ChangeHyperionPlusComplete_astral", "星体");
		//征服者
		set("$RC_ChangeHyperionPlusComplete_conquest", "征服者");
		//奥德赛	odyssey
		set("$RC_ChangeHyperionPlusComplete_odyssey", "奥德赛");
		//
		set("$RC_ChangeHyperionPlusComplete_cost", "200,000");
	}
}





