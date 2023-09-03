package real_combat.campaign.missions;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.econ.impl.ShipQuality;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.misc.ProductionReportIntel;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.CountingMap;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * 1、完成之后可以更换模块
 * 2、更换模块所需要的材料比较少
 * 3、无限循环
 */
public class RC_ChangeHyperionPlus extends HubMissionWithBarEvent { //implements ShipRecoveryListener {
	public static float PROD_DAYS = 60f;
	public static enum Stage {
		START,
		WAITING,
		DELIVERED,
		COMPLETED, // unused, left in for save compat 兼容性
		FAILED
	}
	protected boolean armsDealer = true;
	protected ProductionReportIntel.ProductionData data;
	protected MarketAPI market;
	protected PersonAPI callisto;
	protected FactionAPI faction;
	protected int cost = 500000;
	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		if (!setGlobalReference("$RC_ChangeHyperionPlus_ref", null)) {
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
		//用这个控制
		//setStageOnGlobalFlag(Stage.WAITING,"$RC_ChangeHyperionPlus_start");
		return true;
	}
	
	protected boolean callAction(String action, String ruleId, InteractionDialogAPI dialog, List<Token> params,
								 Map<String, MemoryAPI> memoryMap) {
		if ("showShip".equals(action)) {
			//获取玩家的舰队
			FleetDataAPI fleetData = Global.getSector().getPlayerFleet().getFleetData();
			List<FleetMemberAPI> playerFleetMembers = fleetData.getMembersListCopy();
			FleetMemberAPI hyperion = null;
			//狂怒	onslaught
			FleetMemberAPI onslaught = null;
			//典范	paragon
			FleetMemberAPI paragon = null;
			//征服者	conquest
			FleetMemberAPI conquestL = null;
			FleetMemberAPI conquestR = null;
			for (FleetMemberAPI f:playerFleetMembers) {
				String hullId = f.getHullSpec().getBaseHullId();//f.getHullSpec().getHullId().replace("_default_D","");
				if ("hyperion".equals(hullId)&&hyperion==null) {
					hyperion = f;
				}
				else if ("onslaught".equals(hullId)&&onslaught==null) {
					onslaught = f;
				}
				else if ("paragon".equals(hullId)&&paragon==null) {
					paragon = f;
				}
				else if ("conquest".equals(hullId)&&conquestL==null) {
					conquestL = f;
				}
				else if ("conquest".equals(hullId)&&conquestR==null) {
					conquestR = f;
				}
			}
			if (hyperion==null||onslaught==null||paragon==null||conquestL==null||conquestR==null) {
				//材料不足
				FireBest.fire(null, dialog, memoryMap, "RC_ChangeHyperionNeed");
			}
			else {
				//合一个大概出来给玩家看看
				CampaignFleetAPI ships = Global.getFactory().createEmptyFleet(market.getFactionId(), "temp", true);
				ships.getFleetData().addFleetMember("hyperion_plus_Hull");

				for (FleetMemberAPI member : ships.getFleetData().getMembersListCopy()) {
					member.getVariant().setSource(VariantSource.REFIT);
					member.getVariant().removePermaMod("RC_TrinityForceCore");
					/*
					for (String s :hyperion.getVariant().getPermaMods()) {
						member.getVariant().addPermaMod(s,true);
					}
					 */
					List<String> slotIds = member.getVariant().getModuleSlots();
					for (int i=0;i<slotIds.size();i++)
					{
						String s = slotIds.get(i);
						ShipVariantAPI variant = member.getVariant().getModuleVariant(s);
						variant.setSource(VariantSource.REFIT);
						if (i==0) {
							ShipVariantAPI newVariant = onslaught.getVariant();
							//ShipVariantAPI newVariant = Global.getSettings().getVariant("conquest_Standard");
							newVariant.setSource(VariantSource.REFIT);
							member.getVariant().setModuleVariant(s, newVariant);
						}
						else if (i==1) {
							ShipVariantAPI newVariant = paragon.getVariant();
							//ShipVariantAPI newVariant = Global.getSettings().getVariant("conquest_Standard");
							newVariant.setSource(VariantSource.REFIT);
							member.getVariant().setModuleVariant(s, newVariant);
						}
						else if (i==2) {
							ShipVariantAPI newVariant = conquestR.getVariant();
							//ShipVariantAPI newVariant = Global.getSettings().getVariant("conquest_Standard");
							newVariant.setSource(VariantSource.REFIT);
							member.getVariant().setModuleVariant(s, newVariant);
						}
						else if (i==3) {
							ShipVariantAPI newVariant = conquestL.getVariant();
							//ShipVariantAPI newVariant = Global.getSettings().getVariant("conquest_Standard");
							newVariant.setSource(VariantSource.REFIT);
							member.getVariant().setModuleVariant(s, newVariant);
						}
					}
					TextPanelAPI text = dialog.getTextPanel();
					text.addPara("这个配置是否满意？");
					dialog.getVisualPanel().showFleetMemberInfo(member, true);
					break;
				}
			}
			return true;
		}
		if ("change".equals(action)) {
			boolean hasIndustry = market.hasIndustry(Industries.ORBITALWORKS);
			if (!hasIndustry||market.getIndustry(Industries.ORBITALWORKS).isDisrupted()) {
				//家没了
				FireBest.fire(null, dialog, memoryMap, "RC_ChangeHyperionIndustry");
			}
			//获取玩家的舰队
			FleetDataAPI fleetData = Global.getSector().getPlayerFleet().getFleetData();
			List<FleetMemberAPI> playerFleetMembers = fleetData.getMembersListCopy();
			FleetMemberAPI hyperion = null;
			//攻势	onslaught
			FleetMemberAPI onslaught = null;
			//典范	paragon
			FleetMemberAPI paragon = null;
			//征服者	conquest
			FleetMemberAPI conquestL = null;
			FleetMemberAPI conquestR = null;
			for (FleetMemberAPI f:playerFleetMembers) {
				String hullId = f.getHullSpec().getBaseHullId();//f.getHullSpec().getHullId().replace("_default_D","");
				if ("hyperion".equals(hullId)&&hyperion==null) {
					hyperion = f;
				}
				else if ("onslaught".equals(hullId)&&onslaught==null) {
					onslaught = f;
				}
				else if ("paragon".equals(hullId)&&paragon==null) {
					paragon = f;
				}
				else if ("conquest".equals(hullId)&&conquestL==null) {
					conquestL = f;
				}
				else if ("conquest".equals(hullId)&&conquestR==null) {
					conquestR = f;
				}
			}
			CargoAPI cargo = fleetData.getFleet().getCargo();
			if (hyperion==null||onslaught==null||paragon==null||conquestL==null||conquestR==null||cargo.getCredits().get()<cost) {
				//材料不足
				FireBest.fire(null, dialog, memoryMap, "RC_ChangeHyperionNeed");
			}
			else {
				setStartingStage(Stage.WAITING);
				convertProdToCargo(hyperion,onslaught,paragon,conquestL,conquestR,dialog);
				accept(dialog, memoryMap);
				FireBest.fire(null, dialog, memoryMap, "RC_ChangeHyperionSuccess");
			}
			return true;
		}
		return super.callAction(action, ruleId, dialog, params, memoryMap);
	}
	protected void convertProdToCargo(FleetMemberAPI hyperion,FleetMemberAPI onslaught,FleetMemberAPI paragon,FleetMemberAPI conquestL,FleetMemberAPI conquestR,InteractionDialogAPI dialog) {
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
		ships.getFleetData().addFleetMember("hyperion_plus_variant");

		FleetDataAPI fleetData = Global.getSector().getPlayerFleet().getFleetData();

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
			List<String> slotIds = member.getVariant().getModuleSlots();
			member.getVariant().setSource(VariantSource.REFIT);
			for(int i=0;i<slotIds.size();i++)
			{
				String s = slotIds.get(i);
				ShipVariantAPI variant = member.getVariant().getModuleVariant(s);
				variant.setSource(VariantSource.REFIT);
				if (i==0) {
					ShipVariantAPI newVariant = onslaught.getVariant();
					//ShipVariantAPI newVariant = Global.getSettings().getVariant("conquest_Standard");
					newVariant.setSource(VariantSource.REFIT);
					member.getVariant().setModuleVariant(s, newVariant);
					AddRemoveCommodity.addFleetMemberLossText(onslaught,dialog.getTextPanel());
					fleetData.removeFleetMember(onslaught);
				}
				else if (i==1) {
					ShipVariantAPI newVariant = paragon.getVariant();
					//ShipVariantAPI newVariant = Global.getSettings().getVariant("conquest_Standard");
					newVariant.setSource(VariantSource.REFIT);
					member.getVariant().setModuleVariant(s, newVariant);
					AddRemoveCommodity.addFleetMemberLossText(paragon,dialog.getTextPanel());
					fleetData.removeFleetMember(paragon);
				}
				else if (i==2) {
					ShipVariantAPI newVariant = conquestR.getVariant();
					//ShipVariantAPI newVariant = Global.getSettings().getVariant("conquest_Standard");
					newVariant.setSource(VariantSource.REFIT);
					member.getVariant().setModuleVariant(s, newVariant);
					AddRemoveCommodity.addFleetMemberLossText(conquestR,dialog.getTextPanel());
					fleetData.removeFleetMember(conquestR);
				}
				else if (i==3) {
					ShipVariantAPI newVariant = conquestL.getVariant();
					//ShipVariantAPI newVariant = Global.getSettings().getVariant("conquest_Standard");
					newVariant.setSource(VariantSource.REFIT);
					member.getVariant().setModuleVariant(s, newVariant);
					AddRemoveCommodity.addFleetMemberLossText(conquestL,dialog.getTextPanel());
					fleetData.removeFleetMember(conquestL);
				}
			}
			AddRemoveCommodity.addFleetMemberLossText(hyperion,dialog.getTextPanel());
			fleetData.removeFleetMember(hyperion);

			AddRemoveCommodity.addCreditsLossText(cost, dialog.getTextPanel());
			Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(cost);

			cargo.getMothballedShips().addFleetMember(member);
		}
	}
	protected void updateInteractionDataImpl() {
		set("$RC_ChangeHyperionPlus_days", "" + (int) PROD_DAYS);
		//星体
		set("$RC_ChangeHyperionPlus_astral", "星体");
		//攻势
		set("$RC_ChangeHyperionPlus_onslaught", "攻势");
		//典范
		set("$RC_ChangeHyperionPlus_paragon", "典范");
		//征服者
		set("$RC_ChangeHyperionPlus_conquest", "征服者");
		//亥伯龙
		set("$RC_ChangeHyperionPlus_hyperionO", "亥伯龙");
		//海波龙原型
		set("$RC_ChangeHyperionPlus_hyperionMeta", "海波龙原型");

		set("$RC_ChangeHyperionPlus_cost", "500,000");
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
		//调整 接受成功
		adjustRep(dialog.getTextPanel(), null, CoreReputationPlugin.RepActions.MISSION_SUCCESS);
		//去除当前步骤感叹号
		makeUnimportant(callisto,null);
		//完成
		Global.getSector().getMemoryWithoutUpdate().set("$RC_ChangeHyperionPlus_completed",true);
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
			if (plugin == null) {return;}
			plugin.setPlayerPaidToUnlock(true);

			CargoAPI cargo = plugin.getCargo();
			for (CargoAPI curr : data.data.values()) {
				cargo.addAll(curr, true);
			}
			//设置重要
			makeImportant(callisto, "$RC_ChangeHyperionPlusComplete_returnHere", null);
			//给下一步添加感叹号
			Global.getSector().getMemoryWithoutUpdate().set("$RC_ChangeHyperionPlusComplete_returnHere",true);
			//改造之后可以接着改造
			if (market != null) {
				RC_ChangeHyperionPlusComplete c = new RC_ChangeHyperionPlusComplete();
				c.create(market,false);
			}
		}
	}


	public class HasIndustryConditionChecker implements ConditionChecker {
		protected boolean conditionsMet = false;
		protected MarketAPI market;
		protected int count = 0;
		public HasIndustryConditionChecker(MarketAPI market){
			this.market = market;
		}
		public boolean conditionsMet() {
			doCheck();
			return conditionsMet;
		}

		public void doCheck() {
			if (conditionsMet) return;
			if (!market.hasIndustry(Industries.ORBITALWORKS)) {
				conditionsMet = true;
			}
			//!market.getPrimaryEntity().isAlive()||!market.getPrimaryEntity().isExpired()||
			else if (market.getIndustry(Industries.ORBITALWORKS).isDisrupted()) {
				conditionsMet = true;
			}
		}
	}
}





