package real_combat.campaign.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.econ.impl.ShipQuality;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.misc.ProductionReportIntel;
import com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionManager;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.CountingMap;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import org.lazywizard.lazylib.MathUtils;
import real_combat.campaign.intel.punitive.RC_PunitiveExpeditionManager;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * 有两种办法改returnhere csv里面赋值 update里面赋值
 */
public class RC_ChangeHyperionUltimate extends HubMissionWithBarEvent { //implements ShipRecoveryListener {
	public static float PROD_DAYS = 60f;
	public static enum Stage {
		START,
		WAITING,
		DELIVERED,
		COMPLETED, // unused, left in for save compat 兼容性
		FAILED,
	}
	protected boolean armsDealer = true;
	protected ProductionReportIntel.ProductionData data;
	protected MarketAPI market;
	protected PersonAPI callisto;
	protected FactionAPI faction;
	protected int cost = 500000;
	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		if (!setGlobalReference("$RC_ChangeHyperionUltimate_ref", null)) {
			return false;
		}

		callisto = getImportantPerson("MeiDo");
		if (callisto == null) {return false;}
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
		connectWithCustomCondition(Stage.WAITING, Stage.FAILED, new HasIndustryConditionChecker(market));
		//connectWithDaysElapsed(Stage.WAITING, Stage.DELIVERED, 1f);
		//set 阶段 On 市场 不文明
		setStageOnMarketDecivilized(Stage.FAILED, market);
		//任务完成设置联系人
		//setPersonIsPotentialContactOnSuccess(callisto, 1f);
		//这里不知道要不要增加一个新状态 csv里面update进行更新这样 好像不太对 不过也可以 如果玩家退出去了再进来会有感叹号
		makeImportant(callisto, "$RC_ChangeHyperionUltimate_returnHere", null);
		//makeImportant(callisto, null, Stage.WAITING);
		//用这个控制
		//setStageOnGlobalFlag(Stage.WAITING,"$RC_ChangeHyperionUltimate_start");
		return true;
	}

	protected boolean callAction(String action, String ruleId, InteractionDialogAPI dialog, List<Token> params,
								 Map<String, MemoryAPI> memoryMap) {
		if ("change".equals(action)) {
			boolean hasIndustry = market.hasIndustry(Industries.ORBITALWORKS);
			if (!hasIndustry||market.getIndustry(Industries.ORBITALWORKS).isDisrupted()) {
				//测试代码
				/*
				RC_PunitiveExpeditionManager punitiveExpeditionManager1 = new RC_PunitiveExpeditionManager(market,market.getIndustry(Industries.ORBITALWORKS));
				PunitiveExpeditionManager.PunExData p = new PunitiveExpeditionManager.PunExData();
				p.faction = Global.getSector().getFaction(Factions.LUDDIC_CHURCH);
				punitiveExpeditionManager1.createExpedition(p);
				RC_PunitiveExpeditionManager punitiveExpeditionManager2 = new RC_PunitiveExpeditionManager(market,market.getIndustry(Industries.ORBITALWORKS));
				p.faction = Global.getSector().getFaction(Factions.HEGEMONY);
				punitiveExpeditionManager2.createExpedition(p);
				RC_PunitiveExpeditionManager punitiveExpeditionManager3 = new RC_PunitiveExpeditionManager(market,market.getIndustry(Industries.ORBITALWORKS));
				p.faction = Global.getSector().getFaction(Factions.TRITACHYON);
				punitiveExpeditionManager3.createExpedition(p);
				*/
				/*
				genRandom = new Random();
				setCurrentStage(Stage.START,dialog,memoryMap);

				List<SectorEntityToken> jumpPoints = market.getStarSystem().getJumpPoints();
				for (SectorEntityToken point : jumpPoints) {
					for (int count = 0;count<100;count++){
						spawnFleet(point);
					}
				}
				*/
				//家没了
				FireBest.fire(null, dialog, memoryMap, "RC_ChangeHyperionIndustry");
				return true;
			}
			//获取玩家的舰队
			FleetDataAPI fleetData = Global.getSector().getPlayerFleet().getFleetData();
			List<FleetMemberAPI> playerFleetMembers = fleetData.getMembersListCopy();
			FleetMemberAPI hyperion = null;
			//狂怒	fury
			FleetMemberAPI fury = null;
			//奥德赛	odyssey
			FleetMemberAPI odyssey = null;
			//伯劳鸟	shrike
			FleetMemberAPI shrike = null;
			//圣甲虫	scarab
			FleetMemberAPI scarab = null;
			//折磨	afflictor
			FleetMemberAPI afflictor = null;
			//预言者	harbinger
			FleetMemberAPI harbinger = null;
			for (FleetMemberAPI f:playerFleetMembers) {
				if (f.getHullSpec().getHullId()==null||f.getHullSpec().getHullId()=="") continue;
				if (f.getHullSpec().getHullId().contains("hyperion_meta")&&hyperion==null) {
					hyperion = f;
				}
				else if (f.getHullSpec().getHullId().contains("fury")&&fury==null) {
					fury = f;
				}
				else if (f.getHullSpec().getHullId().contains("odyssey")&&odyssey==null) {
					odyssey = f;
				}
				else if (f.getHullSpec().getHullId().contains("scarab")&&scarab==null) {
					scarab = f;
				}
				else if (f.getHullSpec().getHullId().contains("shrike")&&shrike==null) {
					shrike = f;
				}
				else if (f.getHullSpec().getHullId().contains("afflictor")&&afflictor==null) {
					afflictor = f;
				}
				else if (f.getHullSpec().getHullId().contains("harbinger")&&harbinger==null) {
					harbinger = f;
				}
			}
			CargoAPI cargo = fleetData.getFleet().getCargo();
			if (hyperion==null||(fury==null&&odyssey==null&&shrike==null)||scarab==null||afflictor==null||harbinger==null||cargo.getCommodityQuantity(Commodities.ALPHA_CORE)==0||cargo.getCredits().get()<cost) {
				//材料不足
				FireBest.fire(null, dialog, memoryMap, "RC_ChangeHyperionNeed");
			}
			else {
				setStartingStage(Stage.WAITING);
				convertProdToCargo(hyperion, dialog);
				accept(dialog, memoryMap);
				FireBest.fire(null, dialog, memoryMap, "RC_ChangeHyperionSuccess");
			}
			return true;
		}
		return super.callAction(action, ruleId, dialog, params, memoryMap);
	}
	protected void convertProdToCargo(FleetMemberAPI hyperion,InteractionDialogAPI dialog) {
		data = new ProductionReportIntel.ProductionData();
		CargoAPI cargo = data.getCargo("Order manifest");
		//D插
		float quality = ShipQuality.getShipQuality(market, market.getFactionId());

		CampaignFleetAPI ships = Global.getFactory().createEmptyFleet(market.getFactionId(), "temp", true);
		ships.setCommander(Global.getSector().getPlayerPerson());
		genRandom = Misc.random;
		ships.getFleetData().setShipNameRandom(genRandom);
		DefaultFleetInflaterParams p = new DefaultFleetInflaterParams();
		p.quality = quality;
		p.mode = FactionAPI.ShipPickMode.PRIORITY_THEN_ALL;
		p.persistent = false;
		p.seed = genRandom.nextLong();
		p.timestamp = null;

		FleetInflater inflater = Misc.getInflater(ships, p);
		ships.setInflater(inflater);
		ships.getFleetData().addFleetMember("hyperion_ultimate_variant");

		for (FleetMemberAPI member : ships.getFleetData().getMembersListCopy()) {
			/*
			member.getVariant().setSource(VariantSource.REFIT);
			for (String s :hyperion.getVariant().getPermaMods()) {
				member.getVariant().addPermaMod(s,true);
			}
			*/
			TextPanelAPI text = dialog.getTextPanel();
			for (String s :hyperion.getVariant().getPermaMods()) {
				Global.getSector().getPlayerStats().addStoryPoints(1, text, false);
			}
			cargo.getMothballedShips().addFleetMember(member);
		}
	}
	protected void updateInteractionDataImpl() {
		set("$RC_ChangeHyperionUltimate_days", "" + (int) PROD_DAYS);
		//狂怒	fury
		set("$RC_ChangeHyperionUltimate_fury", "狂怒");
		//奥德赛	odyssey
		set("$RC_ChangeHyperionUltimate_odyssey", "奥德赛");
		//伯劳鸟 shrike
		set("$RC_ChangeHyperionUltimate_shrike", "伯劳鸟");
		//圣甲虫	scarab
		set("$RC_ChangeHyperionUltimate_scarab", "圣甲虫");
		//折磨	afflictor
		set("$RC_ChangeHyperionUltimate_afflictor", "折磨");
		//预言者	harbinger
		set("$RC_ChangeHyperionUltimate_harbinger", "预言者");
		//海波龙原型	hyperion_meta
		set("$RC_ChangeHyperionUltimate_hyperionMeta", "海波龙原型");
		//一颗A核
		set("$RC_ChangeHyperionUltimate_a", "阿尔法核心");
		//
		set("$RC_ChangeHyperionUltimate_cost", "500,000");
	}

	@Override
	public void addDescriptionForCurrentStage(TooltipMakerAPI info, float width, float height) {
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		if (currentStage == Stage.WAITING) {
			float elapsed = getElapsedInCurrentStage();
			int d = (int) Math.round(PROD_DAYS - elapsed);
			PersonAPI person = getPerson();

			LabelAPI label = info.addPara("The order will be delivered to storage " + market.getOnOrAt() + " " + market.getName() +
							" in %s " + getDayOrDays(d) + ".", opad,
					Misc.getHighlightColor(), "" + d);
			label.setHighlight(market.getName(), "" + d);
			label.setHighlightColors(market.getFaction().getBaseUIColor(), h);

			//intel.createSmallDescription(info, width, height);
			showCargoContents(info, width, height);


		} else if (currentStage == Stage.DELIVERED) {
			float elapsed = getElapsedInCurrentStage();
			int d = (int) Math.round(elapsed);
			LabelAPI label = info.addPara(" The order was delivered to storage" + market.getOnOrAt() + " " + market.getName() +
							" %s " + getDayOrDays(d) + " ago.", opad,
					Misc.getHighlightColor(), "" + d);
			label.setHighlight(market.getName(), "" + d);
			label.setHighlightColors(market.getFaction().getBaseUIColor(), h);

			showCargoContents(info, width, height);
			addDeleteButton(info, width);
		} else if (currentStage == Stage.FAILED) {
			//原来空间站炸了订单会取消
			if (market.hasCondition(Conditions.DECIVILIZED)) {
				info.addPara("This order will not be completed because %s" +
								" has decivilized.", opad,
						faction.getBaseUIColor(), market.getName());
			} else {
				//您已经了解到此订单将无法完成
				info.addPara("You've learned that this order will not be completed.", opad);
			}
		}
	}

	//下一步
	@Override
	public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
		Color h = Misc.getHighlightColor();
		if (currentStage == Stage.WAITING) {
			//获取运行时间当前阶段
			float elapsed = getElapsedInCurrentStage();
			//多少天交货
			addDays(info, "until delivery", PROD_DAYS - elapsed, tc, pad);
			return true;
		} else if (currentStage == Stage.DELIVERED) {
			info.addPara("Delivered to %s", pad, tc, market.getFaction().getBaseUIColor(), market.getName());
			return true;
		}
		return false;
	}

	@Override
	public void acceptImpl(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		//获取玩家的舰队
		FleetDataAPI fleetData = Global.getSector().getPlayerFleet().getFleetData();
		List<FleetMemberAPI> playerFleetMembers = fleetData.getMembersListCopy();
		FleetMemberAPI hyperion = null;
		FleetMemberAPI fury = null;
		FleetMemberAPI odyssey = null;
		FleetMemberAPI shrike = null;
		FleetMemberAPI scarab = null;
		FleetMemberAPI afflictor = null;
		FleetMemberAPI harbinger = null;

		for (FleetMemberAPI f:playerFleetMembers)
		{
			String hullId = f.getHullSpec().getHullId().replace("_default_D","");
			if (hullId.contains("hyperion_meta")) {
				hyperion = f;
			}
			else if (hullId.contains("fury")) {
				fury = f;
			}
			else if (hullId.contains("odyssey")) {
				odyssey = f;
			}
			else if (hullId.contains("shrike")) {
				shrike = f;
			}
			else if (hullId.contains("scarab")) {
				scarab = f;
			}
			else if (hullId.contains("afflictor")) {
				afflictor = f;
			}
			else if (hullId.contains("harbinger")) {
				harbinger = f;
			}
		}
		AddRemoveCommodity.addFleetMemberLossText(hyperion,dialog.getTextPanel());
		fleetData.removeFleetMember(hyperion);
		if (shrike!=null) {
			AddRemoveCommodity.addFleetMemberLossText(shrike, dialog.getTextPanel());
			fleetData.removeFleetMember(shrike);
		}
		else if (fury!=null) {
			AddRemoveCommodity.addFleetMemberLossText(fury, dialog.getTextPanel());
			fleetData.removeFleetMember(fury);
		}
		else {
			AddRemoveCommodity.addFleetMemberLossText(odyssey, dialog.getTextPanel());
			fleetData.removeFleetMember(odyssey);
		}
		AddRemoveCommodity.addFleetMemberLossText(scarab,dialog.getTextPanel());
		fleetData.removeFleetMember(scarab);
		AddRemoveCommodity.addFleetMemberLossText(afflictor,dialog.getTextPanel());
		fleetData.removeFleetMember(afflictor);
		AddRemoveCommodity.addFleetMemberLossText(harbinger,dialog.getTextPanel());
		fleetData.removeFleetMember(harbinger);
		//拿走AI核心
		AddRemoveCommodity.addCommodityLossText(Commodities.ALPHA_CORE,1, dialog.getTextPanel());
		fleetData.getFleet().getCargo().removeCommodity(Commodities.ALPHA_CORE,1);
		//钱
		AddRemoveCommodity.addCreditsLossText(cost, dialog.getTextPanel());
		Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(cost);
		//调整 接受成功
		adjustRep(dialog.getTextPanel(), null, CoreReputationPlugin.RepActions.MISSION_SUCCESS);
		//去除感叹号
		makeUnimportant(callisto,null);
		//完成
		Global.getSector().getMemoryWithoutUpdate().set("$RC_ChangeHyperionUltimate_completed",true);

		RC_PunitiveExpeditionManager punitiveExpeditionManager1 = new RC_PunitiveExpeditionManager(market,market.getIndustry(Industries.ORBITALWORKS));
		PunitiveExpeditionManager.PunExData p = new PunitiveExpeditionManager.PunExData();
		p.faction = Global.getSector().getFaction(Factions.LUDDIC_CHURCH);
		punitiveExpeditionManager1.createExpedition(p);
		RC_PunitiveExpeditionManager punitiveExpeditionManager2 = new RC_PunitiveExpeditionManager(market,market.getIndustry(Industries.ORBITALWORKS));
		p.faction = Global.getSector().getFaction(Factions.HEGEMONY);
		punitiveExpeditionManager2.createExpedition(p);
		RC_PunitiveExpeditionManager punitiveExpeditionManager3 = new RC_PunitiveExpeditionManager(market,market.getIndustry(Industries.ORBITALWORKS));
		p.faction = Global.getSector().getFaction(Factions.TRITACHYON);
		punitiveExpeditionManager3.createExpedition(p);
		RC_PunitiveExpeditionManager punitiveExpeditionManager4 = new RC_PunitiveExpeditionManager(market,market.getIndustry(Industries.ORBITALWORKS));
		p.faction = Global.getSector().getFaction(Factions.PERSEAN);
		punitiveExpeditionManager4.createExpedition(p);
	}

	//定制生产订单
	@Override
	public String getBaseName() {
		return "Custom Production Order";
	}
	//合同
	protected String getMissionTypeNoun() {
		return "contract";
	}

	@Override
	public SectorEntityToken getMapLocation(SectorMapAPI map) {
		return market.getPrimaryEntity();
	}
	public void showCargoContents(TooltipMakerAPI info, float width, float height) {
		if (data == null) return;

		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		Color tc = Misc.getTextColor();
		float pad = 3f;
		float small = 3f;
		float opad = 10f;

		List<String> keys = new ArrayList<String>(data.data.keySet());
		Collections.sort(keys, new Comparator<String>() {
			public int compare(String o1, String o2) {
				return o1.compareTo(o2);
			}
		});

		for (String key : keys) {
			CargoAPI cargo = data.data.get(key);
			if (cargo.isEmpty() &&
					((cargo.getMothballedShips() == null ||
							cargo.getMothballedShips().getMembersListCopy().isEmpty()))) {
				continue;
			}

			info.addSectionHeading(key, faction.getBaseUIColor(), faction.getDarkUIColor(),
					Alignment.MID, opad);

			if (!cargo.getStacksCopy().isEmpty()) {
				info.addPara("Ship weapons and fighters:", opad);
				info.showCargo(cargo, 20, true, opad);
			}

			if (!cargo.getMothballedShips().getMembersListCopy().isEmpty()) {
				CountingMap<String> counts = new CountingMap<String>();
				for (FleetMemberAPI member : cargo.getMothballedShips().getMembersListCopy()) {
					counts.add(member.getVariant().getHullSpec().getHullName() + " " + member.getVariant().getDesignation());
				}

				info.addPara("Ship hulls:", opad);
				info.showShips(cargo.getMothballedShips().getMembersListCopy(), 20, true,
						getCurrentStage() == Stage.WAITING, opad);
			}
		}
	}

	@Override
	public void setCurrentStage(Object next, InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		super.setCurrentStage(next, dialog, memoryMap);

		if (currentStage == Stage.DELIVERED) {
			StoragePlugin plugin = (StoragePlugin) Misc.getStorage(getPerson().getMarket());
			if (plugin == null) return;
			plugin.setPlayerPaidToUnlock(true);

			CargoAPI cargo = plugin.getCargo();
			for (CargoAPI curr : data.data.values()) {
				cargo.addAll(curr, true);
			}

			//刷一大堆 给玩家打
			for (int count=0;count<20;count++) {
				List<SectorEntityToken> jumpPoints = market.getStarSystem().getJumpPoints();
				for (SectorEntityToken point : jumpPoints) {
					spawnFleet(point);
				}
			}

			//设置重要
			makeImportant(callisto, "$RC_ChangeHyperionPlus_returnHere", null);
			Global.getSector().getMemoryWithoutUpdate().set("$RC_ChangeHyperionPlus_returnHere",true);
			//改造过后又新的理解
			if (market != null) {
				RC_ChangeHyperionPlus p = new RC_ChangeHyperionPlus();
				p.create(market, false);
			}
		}
	}

	public class HasIndustryConditionChecker implements ConditionChecker {
		protected boolean conditionsMet = false;
		protected MarketAPI market;
		protected float startDate = 0f;
		protected int count = 0;
		public HasIndustryConditionChecker(MarketAPI market){
			this.market = market;
		}
		public boolean conditionsMet() {
			doCheck();
			return conditionsMet;
		}

		public void advance(float amount) {
			if (conditionsMet) return;
			float days = Global.getSector().getClock().convertToDays(amount);
			if (startDate==0f) {
				startDate = days;
			}
			else if(days-startDate>1){
				startDate = days;

				List<SectorEntityToken> jumpPoints = market.getStarSystem().getJumpPoints();
				for (SectorEntityToken point : jumpPoints) {
					float random = MathUtils.getRandomNumberInRange(1,100);
					String factions = Factions.PIRATES;
					if (random<20) {
						factions = Factions.PIRATES;
					} else if(random<40) {
						factions = Factions.LUDDIC_CHURCH;
					} else if(random<60) {
						factions = Factions.HEGEMONY;
					} else if(random<80) {
						factions = Factions.TRITACHYON;
					} else {
						factions = Factions.LUDDIC_PATH;
					}
					beginWithinHyperspaceRangeTrigger(market.getStarSystem(), 3f, false, Stage.WAITING);
					triggerCreateFleet(FleetSize.HUGE, FleetQuality.HIGHER, factions, FleetTypes.PATROL_LARGE, point);
					triggerSetFleetFaction(factions);
					triggerSetFleetOfficers(OfficerNum.MORE, OfficerQuality.HIGHER);
					triggerAutoAdjustFleetStrengthMajor();
					triggerMakeHostileAndAggressive();
					triggerMakeNoRepImpact();
					triggerFleetAllowLongPursuit();
					triggerSetFleetAlwaysPursue();
					triggerSpawnFleetNear(market.getStarSystem().getCenter(), null, null);
					triggerOrderFleetPatrol(market.getStarSystem(), true, Tags.STATION, Tags.JUMP_POINT);
					triggerOrderFleetAttackLocation(market.getPrimaryEntity());
					endTrigger();
				}
			}
		}

		public void doCheck() {
			if (conditionsMet) return;
			count++;
			if (count>120) {
				count=0;
				List<SectorEntityToken> jumpPoints = market.getStarSystem().getJumpPoints();
				for (SectorEntityToken point : jumpPoints) {
					float random = MathUtils.getRandomNumberInRange(1,100);
					String factions = Factions.PIRATES;
					if (random<20) {
						factions = Factions.PIRATES;
					} else if(random<40) {
						factions = Factions.LUDDIC_CHURCH;
					} else if(random<60) {
						factions = Factions.HEGEMONY;
					} else if(random<80) {
						factions = Factions.TRITACHYON;
					} else {
						factions = Factions.LUDDIC_PATH;
					}
					beginWithinHyperspaceRangeTrigger(market.getStarSystem(), 3f, false, Stage.WAITING);
					triggerCreateFleet(FleetSize.HUGE, FleetQuality.HIGHER, factions, FleetTypes.PATROL_LARGE, point);
					triggerSetFleetFaction(factions);
					triggerSetFleetOfficers(OfficerNum.MORE, OfficerQuality.HIGHER);
					triggerAutoAdjustFleetStrengthMajor();
					triggerMakeHostileAndAggressive();
					triggerMakeNoRepImpact();
					triggerFleetAllowLongPursuit();
					triggerSetFleetAlwaysPursue();
					triggerSpawnFleetNear(market.getStarSystem().getCenter(), null, null);
					triggerOrderFleetPatrol(market.getStarSystem(), true, Tags.STATION, Tags.JUMP_POINT);
					triggerOrderFleetAttackLocation(market.getPrimaryEntity());
					endTrigger();
				}
			}

			if (!market.hasIndustry(Industries.ORBITALWORKS)) {
				conditionsMet = true;
			}
			//!market.getPrimaryEntity().isAlive()||!market.getPrimaryEntity().isExpired()||
			else if (market.getIndustry(Industries.ORBITALWORKS).isDisrupted()) {
				conditionsMet = true;
			}
		}
	}

	protected void spawnFleet(SectorEntityToken spawnPoint) {
		float random = MathUtils.getRandomNumberInRange(1,100);
		String factions = Factions.PIRATES;
		if (random<20) {
			factions = Factions.PIRATES;
		} else if(random<40) {
			factions = Factions.LUDDIC_CHURCH;
		} else if(random<60) {
			factions = Factions.HEGEMONY;
		} else if(random<80) {
			factions = Factions.TRITACHYON;
		} else {
			factions = Factions.LUDDIC_PATH;
		}

		beginWithinHyperspaceRangeTrigger(market.getStarSystem(), 3f, false, Stage.DELIVERED);//Stage.DELIVERED
		triggerCreateFleet(FleetSize.HUGE, FleetQuality.HIGHER, factions, FleetTypes.PATROL_LARGE, spawnPoint);
		triggerSetFleetFaction(factions);
		triggerSetFleetOfficers(OfficerNum.MORE, OfficerQuality.HIGHER);
		triggerAutoAdjustFleetStrengthMajor();
		triggerMakeHostileAndAggressive();
		triggerMakeNoRepImpact();
		triggerFleetAllowLongPursuit();
		triggerSetFleetAlwaysPursue();
		triggerSetFleetFlag("$RC_fleetTalk");
		//triggerPickLocationAroundEntity(spawnPoint, 100f);
		triggerSpawnFleetNear(market.getStarSystem().getCenter(), null, null);
		//triggerOrderFleetPatrol(market.getStarSystem(), true, Tags.STATION, Tags.JUMP_POINT);
		triggerOrderFleetInterceptPlayer();
		//triggerOrderFleetAttackLocation(market.getPrimaryEntity());
		endTrigger();
		/*
		beginWithinHyperspaceRangeTrigger(market.getStarSystem(), 3f, false, Stage.START);
		triggerCreateFleet(FleetSize.HUGE, FleetQuality.DEFAULT, market.getFactionId(), FleetTypes.PATROL_MEDIUM, market.getStarSystem());
		triggerAutoAdjustFleetStrengthMajor();
		triggerSetPirateFleet();
		triggerSpawnFleetNear(market.getStarSystem().getCenter(), null, null);
		triggerOrderFleetPatrol(market.getStarSystem(), true, Tags.STATION, Tags.JUMP_POINT);
		endTrigger();
		*/

		/*
		DelayedFleetEncounter e = new DelayedFleetEncounter(genRandom, getMissionId());
		e.setDelayMedium();
		float random = MathUtils.getRandomNumberInRange(1,100);
		String factions = Factions.PIRATES;
		if (random<20) {
			factions = Factions.PIRATES;
		} else if(random<40) {
			factions = Factions.LUDDIC_CHURCH;
		} else if(random<60) {
			factions = Factions.HEGEMONY;
		} else if(random<80) {
			factions = Factions.TRITACHYON;
		} else {
			factions = Factions.LUDDIC_PATH;
		}
		e.setLocationInnerSector(true, factions);
		e.beginCreate();
		e.triggerCreateFleet(FleetSize.MEDIUM, FleetQuality.VERY_HIGH, Factions.MERCENARY, FleetTypes.PATROL_LARGE, new Vector2f());
		e.triggerSetFleetOfficers(OfficerNum.MORE, OfficerQuality.HIGHER);
		e.triggerFleetSetFaction(factions);
		e.triggerMakeNoRepImpact();
		e.triggerSetStandardAggroInterceptFlags();
		e.endCreate();
		*/
	}
}





