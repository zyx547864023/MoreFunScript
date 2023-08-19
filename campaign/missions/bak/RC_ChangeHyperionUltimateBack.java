package real_combat.campaign.missions.bak;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.FactionProductionAPI.ItemInProductionAPI;
import com.fs.starfarer.api.campaign.FactionProductionAPI.ProductionItemType;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions;
import com.fs.starfarer.api.impl.campaign.econ.impl.ShipQuality;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.bases.PirateBaseManager;
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel;
import com.fs.starfarer.api.impl.campaign.intel.misc.ProductionReportIntel.ProductionData;
import com.fs.starfarer.api.impl.campaign.missions.DelayedFleetEncounter;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.CountingMap;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.*;

public class RC_ChangeHyperionUltimateBack extends HubMissionWithBarEvent {

	public static float ARMS_DEALER_PROB_PATROL_AFTER = 0.5f;

	public static float PROD_DAYS = 60f;

	public static float PROB_ARMS_DEALER_BAR = 0.25f;
	public static float PROB_MILITARY_BAR = 0.33f;
	public static float PROB_INDEPENDENT_BAR = 0.5f;

	public static float PROB_ARMS_DEALER_IS_CONTACT = 0.05f;

	public static float MIN_CAPACITY = 50000;
	public static float MAX_CAPACITY = 500000;
	public static int BAR_CAPACITY_BONUS_MIN = 50000;
	public static int BAR_CAPACITY_BONUS_MAX = 150000;


	public static float MAX_PROD_CAPACITY_AT_SHIP_UNITS = 10;
	public static float MAX_PROD_CAPACITY_MULT = 0.25f;

	public static float DEALER_MIN_CAPACITY = 1000000;
	public static float DEALER_MAX_CAPACITY = 2000000;
	public static Map<PersonImportance, Float> DEALER_MULT = new LinkedHashMap<PersonImportance, Float>();
	static {
		DEALER_MULT.put(PersonImportance.VERY_LOW, 0.1f);
		DEALER_MULT.put(PersonImportance.LOW, 0.2f);
		DEALER_MULT.put(PersonImportance.MEDIUM, 0.3f);
		DEALER_MULT.put(PersonImportance.HIGH, 0.6f);
		DEALER_MULT.put(PersonImportance.VERY_HIGH, 1f);
	}

	public static float MILITARY_CAP_MULT = 0.6f;

	public static float MILITARY_MAX_COST_DECREASE = 0.3f;
	public static float TRADE_MAX_COST_INCREASE = 0.3f;
	public static float DEALER_FIXED_COST_INCREASE = 0.5f;
	public static float DEALER_VARIABLE_COST_INCREASE = 0.5f;

	public static enum Stage {
		WAITING,
		DELIVERED,
		COMPLETED, // unused, left in for save compat
		FAILED,
	}

	protected Set<String> ships = new LinkedHashSet<String>();
	protected Set<String> weapons = new LinkedHashSet<String>();
	protected Set<String> fighters = new LinkedHashSet<String>();

	protected boolean armsDealer = false;
	protected int maxCapacity;
	protected float costMult;
	protected ProductionData data;
	protected int cost;
	protected FactionAPI faction;
	protected MarketAPI market;

	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		//genRandom = Misc.random;
		//允许军火交易
		boolean allowArmsDealer = true; // anywhere is fine
		//允许交易者
		boolean allowTrader = createdAt != null && createdAt.getCommodityData(Commodities.SHIPS).getMaxSupply() > 0;
		//允许军事
		boolean allowMilitary = allowTrader && createdAt != null && Misc.isMilitary(createdAt);
		//如果市场是玩家自己的不刷非法军火商
		if (createdAt.isPlayerOwned()) {
			allowTrader = false;
			allowMilitary = false;
		}
		//如果是海盗不允许军事
		if (Factions.PIRATES.equals(createdAt.getFaction().getId())) {
			allowMilitary = false;
		}

		if (barEvent) {
			String post = null;
			//随机点数 联邦探员
			//武器经销商 酒吧
			if (rollProbability(PROB_ARMS_DEALER_BAR) && allowArmsDealer) {
				//平民
				setGiverRank(Ranks.CITIZEN);
				//武器经销商
				post = Ranks.POST_ARMS_DEALER;
				//联系人 黑社会
				setGiverTags(Tags.CONTACT_UNDERWORLD);
				//海盗
				setGiverFaction(Factions.PIRATES);
			}
			//军事酒吧
			else if (rollProbability(PROB_MILITARY_BAR) && allowMilitary) {
				List<String> posts = new ArrayList<String>();
				//供应商
				posts.add(Ranks.POST_SUPPLY_OFFICER);
				//军用市场
				if (Misc.isMilitary(createdAt)) {
					//基本指挥官
					posts.add(Ranks.POST_BASE_COMMANDER);
				}
				//其他 拥有轨道空间站
				if (Misc.hasOrbitalStation(createdAt)) {
					//空间站指挥官
					posts.add(Ranks.POST_STATION_COMMANDER);
				}
				//选择一个
				post = pickOne(posts);
				//选择一个个军衔
				setGiverRank(pickOne(Ranks.GROUND_CAPTAIN, Ranks.GROUND_COLONEL, Ranks.GROUND_MAJOR,
							 Ranks.SPACE_COMMANDER, Ranks.SPACE_CAPTAIN, Ranks.SPACE_ADMIRAL));
				//军事联系人
				setGiverTags(Tags.CONTACT_MILITARY);
			}
			//
			else if (allowTrader) {
				//贫民
				setGiverRank(Ranks.CITIZEN);
				post = pickOne(Ranks.POST_TRADER, Ranks.POST_COMMODITIES_AGENT, Ranks.POST_PORTMASTER,
				 			   Ranks.POST_MERCHANT, Ranks.POST_INVESTOR, Ranks.POST_EXECUTIVE,
				 		 	   Ranks.POST_SENIOR_EXECUTIVE, Ranks.POST_ADMINISTRATOR);
				//联系人交易
				setGiverTags(Tags.CONTACT_TRADE);
				//独立 自由联盟
				if (rollProbability(PROB_INDEPENDENT_BAR)) {
					setGiverFaction(Factions.INDEPENDENT);
				}
			}
			//选不到人就是非法军火
			if (post == null && allowArmsDealer) {
				setGiverRank(Ranks.CITIZEN);
				post = Ranks.POST_ARMS_DEALER;
				setGiverTags(Tags.CONTACT_UNDERWORLD);
				setGiverFaction(Factions.PIRATES);
			}
			//还选不到拉倒
			if (post == null) return false;
			//邮寄人
			setGiverPost(post);
			if (post.equals(Ranks.POST_SENIOR_EXECUTIVE) ||
					post.equals(Ranks.POST_BASE_COMMANDER) ||
					post.equals(Ranks.POST_ADMINISTRATOR)) {
				//重要联系人
				setGiverImportance(pickHighImportance());
			} else if (post.equals(Ranks.POST_ARMS_DEALER)) {
				setGiverImportance(pickArmsDealerImportance());
			} else {
				setGiverImportance(pickImportance());

			}
			//找到合适的联系人，找不到就创建
			findOrCreateGiver(createdAt, false, false);
			//任务完成设置联系人
			setGiverIsPotentialContactOnSuccess();
		}

		PersonAPI person = getPerson();
		if (person == null) return false;
		//设置人员任务参考
		if (!setPersonMissionRef(person, "$cpc_ref")) {
			return false;
		}
		//人在哪个市场？看不懂为什么和前面的市场不用同一个
		market = getPerson().getMarket();
		if (market == null) return false;
		//市场要有仓库
		if (Misc.getStorage(market) == null) return false;

		faction = person.getFaction();

//		armsDealer = Ranks.POST_ARMS_DEALER.equals(person.getPostId());
//		if (!armsDealer) allowArmsDealer = false;
		armsDealer = getPerson().hasTag(Tags.CONTACT_UNDERWORLD);
		//最大价格
		maxCapacity = getRoundNumber(MIN_CAPACITY + (MAX_CAPACITY - MIN_CAPACITY) * getQuality());
		if (barEvent) {
			maxCapacity += genRoundNumber(BAR_CAPACITY_BONUS_MIN, BAR_CAPACITY_BONUS_MAX);
		}
		//商品 数据 船舶单位容量
		float capMult = market.getCommodityData(Commodities.SHIPS).getMaxSupply() / MAX_PROD_CAPACITY_AT_SHIP_UNITS;
		if (capMult > 1) capMult = 1f;
		//最大容量
		if (capMult < MAX_PROD_CAPACITY_MULT) capMult = MAX_PROD_CAPACITY_MULT;
		maxCapacity *= capMult;
		if (person.hasTag(Tags.CONTACT_MILITARY) && allowMilitary) {
			maxCapacity *= MILITARY_CAP_MULT;
		}
		maxCapacity = getRoundNumber(maxCapacity);

		if (armsDealer && allowArmsDealer) { // don't care about ship production, since it's just acquisition from wherever
			PersonImportance imp = getPerson().getImportance();
			float mult = DEALER_MULT.get(imp);
			maxCapacity = getRoundNumber(mult *
						(DEALER_MIN_CAPACITY + (DEALER_MAX_CAPACITY - DEALER_MIN_CAPACITY) * getQuality()));
		}

		if (armsDealer && allowArmsDealer) {
			costMult = 1f + DEALER_FIXED_COST_INCREASE + DEALER_VARIABLE_COST_INCREASE * (1f - getRewardMultFraction());
			addArmsDealerBlueprints();
			if (ships.isEmpty() && weapons.isEmpty() && fighters.isEmpty()) return false;
		} else if (person.hasTag(Tags.CONTACT_MILITARY) && allowMilitary) {
			costMult = 1f - MILITARY_MAX_COST_DECREASE * getRewardMultFraction();
			addMilitaryBlueprints();
			if (ships.isEmpty() && weapons.isEmpty() && fighters.isEmpty()) return false;
		} else if (person.hasTag(Tags.CONTACT_TRADE) && allowTrader) {
			costMult = 1f + TRADE_MAX_COST_INCREASE * (1f - getRewardMultFraction());
		} else {
			return false;
		}
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

	//添加武器经销商蓝图
	protected void addArmsDealerBlueprints() {
		boolean [] add = new boolean[3];
		add[genRandom.nextInt(add.length)] = true;
		add[genRandom.nextInt(add.length)] = true;
		add[genRandom.nextInt(add.length)] = true;

		PersonImportance imp = getPerson().getImportance();
		if (imp == PersonImportance.VERY_HIGH) {
			add[0] = true;
			add[1] = true;
			add[2] = true;
		}

		Set<WeaponType> wTypes = new LinkedHashSet<WeaponType>();
		Set<WeaponSize> wSizes = new LinkedHashSet<WeaponSize>();
		Set<HullSize> hullSizes = new LinkedHashSet<HullSize>();

		WeightedRandomPicker<WeaponType> wTypePicker = new WeightedRandomPicker<WeaponType>(genRandom);
		wTypePicker.add(WeaponType.BALLISTIC);
		wTypePicker.add(WeaponType.ENERGY);
		wTypePicker.add(WeaponType.MISSILE);
		WeightedRandomPicker<WeaponSize> wSizePicker = new WeightedRandomPicker<WeaponSize>(genRandom);
		wSizePicker.add(WeaponSize.SMALL);
		wSizePicker.add(WeaponSize.MEDIUM);
		wSizePicker.add(WeaponSize.LARGE);

		int nWeapons = 0;
		int nShips = 0;
		int nFighters = 0;

		switch (imp) {
		case VERY_LOW:
			add[1] = true;
			wSizes.add(WeaponSize.SMALL);
			wTypes.add(wTypePicker.pickAndRemove());
			nWeapons = 5 + genRandom.nextInt(6);
			nFighters = 1 + genRandom.nextInt(3);
			break;
		case LOW:
			add[1] = true;
			wSizePicker.remove(WeaponSize.LARGE);
			wSizes.add(wSizePicker.pickAndRemove());
			wTypes.add(wTypePicker.pickAndRemove());
			hullSizes.add(HullSize.FRIGATE);
			nWeapons = 10 + genRandom.nextInt(6);
			nShips = 5 + genRandom.nextInt(3);
			nFighters = 3 + genRandom.nextInt(3);
			break;
		case MEDIUM:
			add[1] = true;
			wSizes.add(wSizePicker.pickAndRemove());
			wSizes.add(wSizePicker.pickAndRemove());
			wTypes.add(wTypePicker.pickAndRemove());
			hullSizes.add(HullSize.FRIGATE);
			hullSizes.add(HullSize.DESTROYER);
			nWeapons = 20 + genRandom.nextInt(6);
			nShips = 10 + genRandom.nextInt(3);
			nFighters = 5 + genRandom.nextInt(3);
			break;
		case HIGH:
			add[1] = true;
			wSizes.add(wSizePicker.pickAndRemove());
			wSizes.add(wSizePicker.pickAndRemove());
			wTypes.add(wTypePicker.pickAndRemove());
			wTypes.add(wTypePicker.pickAndRemove());
			hullSizes.add(HullSize.FRIGATE);
			hullSizes.add(HullSize.DESTROYER);
			hullSizes.add(HullSize.CRUISER);
			nWeapons = 20 + genRandom.nextInt(6);
			nShips = 10 + genRandom.nextInt(3);
			nFighters = 7 + genRandom.nextInt(3);
			break;
		case VERY_HIGH:
			wSizes.add(WeaponSize.SMALL);
			wSizes.add(WeaponSize.MEDIUM);
			wSizes.add(WeaponSize.LARGE);

			hullSizes.add(HullSize.FRIGATE);
			hullSizes.add(HullSize.DESTROYER);
			hullSizes.add(HullSize.CRUISER);
			hullSizes.add(HullSize.CAPITAL_SHIP);

			wTypes.add(WeaponType.BALLISTIC);
			wTypes.add(WeaponType.ENERGY);
			wTypes.add(WeaponType.MISSILE);
			nWeapons = 1000;
			nShips = 1000;
			nFighters = 1000;
			break;
		}


		FactionProductionAPI prod = Global.getSector().getPlayerFaction().getProduction().clone();
		prod.clear();

		if (add[0]) {
			WeightedRandomPicker<String> picker = new WeightedRandomPicker<String>(genRandom);
			for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
				//if (!spec.hasTag(Items.TAG_RARE_BP) && !spec.hasTag(Items.TAG_DEALER)) continue;
				if (spec.hasTag(Items.TAG_NO_DEALER)) continue;
				if (spec.hasTag(Tags.NO_SELL) && !spec.hasTag(Items.TAG_DEALER)) continue;
				if (spec.hasTag(Tags.RESTRICTED)) continue;
				if (spec.getHints().contains(ShipTypeHints.HIDE_IN_CODEX)) continue;
				if (spec.getHints().contains(ShipTypeHints.UNBOARDABLE)) continue;
				if (spec.isDefaultDHull() || spec.isDHull()) continue;
				if ("shuttlepod".equals(spec.getHullId())) continue;
				if (ships.contains(spec.getHullId())) continue;
				if (!hullSizes.contains(spec.getHullSize())) continue;
				float cost = prod.createSampleItem(ProductionItemType.SHIP, spec.getHullId(), 1).getBaseCost();
				cost = (int)Math.round(cost * costMult);
				if (cost > maxCapacity) continue;
				picker.add(spec.getHullId(), 10f);
			}
//			int num = 2 + (int)Math.round(genRandom.nextInt(5) * getQuality());
//			num += imp.ordinal() * 2;
//			if (imp == PersonImportance.VERY_HIGH) num = 1000;
			int num = nShips;
			for (int i = 0; i < num && !picker.isEmpty(); i++) {
				ships.add(picker.pickAndRemove());
			}
		}

		if (add[1]) {
			WeightedRandomPicker<String> picker = new WeightedRandomPicker<String>(genRandom);
			for (WeaponSpecAPI spec : Global.getSettings().getAllWeaponSpecs()) {
				//if (!spec.hasTag(Items.TAG_RARE_BP) && !spec.hasTag(Items.TAG_DEALER)) continue;
				if (spec.hasTag(Items.TAG_NO_DEALER)) continue;
				if (spec.hasTag(Tags.NO_SELL) && !spec.hasTag(Items.TAG_DEALER)) continue;
				if (spec.hasTag(Tags.RESTRICTED)) continue;
				if (spec.getAIHints().contains(AIHints.SYSTEM)) continue;
				if (weapons.contains(spec.getWeaponId())) continue;
				if (!wTypes.contains(spec.getType())) continue;
				if (!wSizes.contains(spec.getSize())) continue;
				float cost = prod.createSampleItem(ProductionItemType.WEAPON, spec.getWeaponId(), 1).getBaseCost();
				cost = (int)Math.round(cost * costMult);
				if (cost > maxCapacity) continue;
				picker.add(spec.getWeaponId(), 10f);
			}
//			int num = 3 + (int)Math.round(genRandom.nextInt(7) * getQuality());
//			num += imp.ordinal() * 2;
//			if (imp == PersonImportance.VERY_HIGH) num = 1000;
			int num = nWeapons;
			for (int i = 0; i < num && !picker.isEmpty(); i++) {
				weapons.add(picker.pickAndRemove());
			}
		}

		if (add[2]) {
			WeightedRandomPicker<String> picker = new WeightedRandomPicker<String>(genRandom);
			for (FighterWingSpecAPI spec : Global.getSettings().getAllFighterWingSpecs()) {
				//if (!spec.hasTag(Items.TAG_RARE_BP) && !spec.hasTag(Items.TAG_DEALER)) continue;
//				if (spec.hasTag(Tags.NO_DROP)) continue;
//				if (spec.hasTag(Tags.NO_SELL)) continue;
				if (spec.hasTag(Items.TAG_NO_DEALER)) continue;
				if (spec.hasTag(Tags.NO_SELL) && !spec.hasTag(Items.TAG_DEALER)) continue;
				if (spec.hasTag(Tags.RESTRICTED)) continue;
				if (fighters.contains(spec.getId())) continue;
				float cost = prod.createSampleItem(ProductionItemType.FIGHTER, spec.getId(), 1).getBaseCost();
				cost = (int)Math.round(cost * costMult);
				if (cost > maxCapacity) continue;
				picker.add(spec.getId(), 10f);
			}
//			int num = 1 + (int)Math.round(genRandom.nextInt(3) * getQuality());
//			num += imp.ordinal() * 2;
//			if (imp == PersonImportance.VERY_HIGH) num = 1000;
			int num = nFighters;
			for (int i = 0; i < num && !picker.isEmpty(); i++) {
				fighters.add(picker.pickAndRemove());
			}
		}
	}
	//添加军事蓝图
	protected void addMilitaryBlueprints() {
		for (String id : faction.getKnownShips()) {
			ShipHullSpecAPI spec = Global.getSettings().getHullSpec(id);
			if (spec.hasTag(Tags.NO_SELL)) continue;
			ships.add(id);
		}
		for (String id : faction.getKnownWeapons()) {
			WeaponSpecAPI spec = Global.getSettings().getWeaponSpec(id);
			if (spec.hasTag(Tags.NO_DROP)) continue;
			if (spec.hasTag(Tags.NO_SELL)) continue;
			weapons.add(id);
		}
		for (String id : faction.getKnownFighters()) {
			FighterWingSpecAPI spec = Global.getSettings().getFighterWingSpec(id);
			if (spec.hasTag(Tags.NO_DROP)) continue;
			if (spec.hasTag(Tags.NO_SELL)) continue;
			fighters.add(id);
		}
	}

	//更新交互数据
	protected void updateInteractionDataImpl() {
		set("$cpc_military", getPerson().hasTag(Tags.CONTACT_MILITARY));
		set("$cpc_trade", getPerson().hasTag(Tags.CONTACT_TRADE));
		set("$cpc_armsDealer", armsDealer);

		set("$cpc_barEvent", isBarEvent());
		set("$cpc_manOrWoman", getPerson().getManOrWoman());
		set("$cpc_maxCapacity", Misc.getWithDGS(maxCapacity));
		set("$cpc_costPercent", (int)Math.round(costMult * 100f) + "%");
		set("$cpc_days", "" + (int) PROD_DAYS);
	}
	//当前阶段的说明
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
	//接受接口
	@Override
	public void acceptImpl(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		float f = (float) cost / (float) maxCapacity;
		float p = ContactIntel.DEFAULT_POTENTIAL_CONTACT_PROB * f;
		if (armsDealer) {
			p = PROB_ARMS_DEALER_IS_CONTACT * f;
		}
		if (potentialContactsOnMissionSuccess != null) {
			for (PotentialContactData data : potentialContactsOnMissionSuccess) {
				data.probability = p;
			}
		}
		//移除金钱
		AddRemoveCommodity.addCreditsLossText(cost, dialog.getTextPanel());
		Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(cost);
		//调整 接受成功
		adjustRep(dialog.getTextPanel(), null, RepActions.MISSION_SUCCESS);
		//舰载联系人
		addPotentialContacts(dialog);

		ships = null;
		fighters = null;
		weapons = null;
	}

	//设置当前阶段
	@Override
	public void setCurrentStage(Object next, InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		super.setCurrentStage(next, dialog, memoryMap);
		//交货
		if (currentStage == Stage.DELIVERED) {
			//存储到联系人的地方
			StoragePlugin plugin = (StoragePlugin) Misc.getStorage(getPerson().getMarket());
			if (plugin == null) return;
			//玩家付费解锁？
			plugin.setPlayerPaidToUnlock(true);
			//获取插件的货物
			CargoAPI cargo = plugin.getCargo();
			for (CargoAPI curr : data.data.values()) {
				//将原本存起来的全部加到空间站
				cargo.addAll(curr, true);
			}

			//endSuccess(dialog, memoryMap)
			//巡逻队
			if (armsDealer && rollProbability(ARMS_DEALER_PROB_PATROL_AFTER)) {
				PersonAPI person = getPerson();
				if (person == null || person.getMarket() == null) return;
				String patrolFaction = person.getMarket().getFactionId();
				if (patrolFaction.equals(person.getFaction().getId()) ||
						Misc.isPirateFaction(person.getMarket().getFaction()) ||
						Misc.isDecentralized(person.getMarket().getFaction()) ||
						patrolFaction.equals(Factions.PLAYER)) {
					return;
				}

				DelayedFleetEncounter e = new DelayedFleetEncounter(genRandom, getMissionId());
				e.setDelayMedium();
				e.setLocationCoreOnly(true, patrolFaction);
				e.beginCreate();
				e.triggerCreateFleet(FleetSize.LARGE, FleetQuality.DEFAULT, patrolFaction, FleetTypes.PATROL_LARGE, new Vector2f());
				e.setFleetWantsThing(patrolFaction,
						"information regarding the arms dealer", "it",
						"information concerning the activities of known arms dealer, " + person.getNameString(),
						getRoundNumber(cost / 2),
						false, ComplicationRepImpact.FULL,
						DelayedFleetEncounter.TRIGGER_REP_LOSS_HIGH, getPerson());
				e.triggerSetAdjustStrengthBasedOnQuality(true, getQuality());
				e.triggerSetPatrol();
				e.triggerSetStandardAggroInterceptFlags();
				e.endCreate();
			}
		}
	}

	//用来打开订单界面
	@Override
	protected boolean callAction(final String action, final String ruleId, final InteractionDialogAPI dialog,
								 final List<Token> params,
								 final Map<String, MemoryAPI> memoryMap) {
		if ("pickPlayerBP".equals(action)) {
			dialog.showCustomProductionPicker(new BaseCustomProductionPickerDelegateImpl() {
				@Override
				public float getCostMult() {
					return costMult;
				}
				@Override
				public float getMaximumValue() {
					return maxCapacity;
				}
				@Override
				public void notifyProductionSelected(FactionProductionAPI production) {
					convertProdToCargo(production);
					FireBest.fire(null, dialog, memoryMap, "CPCBlueprintsPicked");
				}
			});
			return true;
		}
		if ("pickContactBP".equals(action)) {
			dialog.showCustomProductionPicker(new BaseCustomProductionPickerDelegateImpl() {
				@Override
				public Set<String> getAvailableFighters() {
					return fighters;
				}
				@Override
				public Set<String> getAvailableShipHulls() {
					return ships;
				}
				@Override
				public Set<String> getAvailableWeapons() {
					return weapons;
				}
				@Override
				public float getCostMult() {
					return costMult;
				}
				@Override
				public float getMaximumValue() {
					return maxCapacity;
				}
				@Override
				public void notifyProductionSelected(FactionProductionAPI production) {
					convertProdToCargo(production);
					FireBest.fire(null, dialog, memoryMap, "CPCBlueprintsPicked");
				}
			});
			return true;
		}

		return super.callAction(action, ruleId, dialog, params, memoryMap);
	}

	//将产品转换为货物
	protected void convertProdToCargo(FactionProductionAPI prod) {
		cost = prod.getTotalCurrentCost();
		//生产数据
		data = new ProductionData();
		//清单
		CargoAPI cargo = data.getCargo("Order manifest");
		//质量
		float quality = ShipQuality.getShipQuality(market, market.getFactionId());
		if (armsDealer) {
			quality = Math.max(quality, 1f);
		}

		CampaignFleetAPI ships = Global.getFactory().createEmptyFleet(market.getFactionId(), "temp", true);
		ships.setCommander(Global.getSector().getPlayerPerson());
		ships.getFleetData().setShipNameRandom(genRandom);
		//默认舰队扩张器参数
		DefaultFleetInflaterParams p = new DefaultFleetInflaterParams();
		p.quality = quality;
		//优先于所有
		p.mode = ShipPickMode.PRIORITY_THEN_ALL;
		//持久
		p.persistent = false;
		p.seed = genRandom.nextLong();
		p.timestamp = null;
		//舰队充气
		FleetInflater inflater = Misc.getInflater(ships, p);
		ships.setInflater(inflater);
		//产品生产
		for (ItemInProductionAPI item : prod.getCurrent()) {
			int count = item.getQuantity();

			if (item.getType() == ProductionItemType.SHIP) {
				for (int i = 0; i < count; i++) {
					ships.getFleetData().addFleetMember(item.getSpecId() + "_Hull");
				}
			} else if (item.getType() == ProductionItemType.FIGHTER) {
				cargo.addFighters(item.getSpecId(), count);
			} else if (item.getType() == ProductionItemType.WEAPON) {
				cargo.addWeapons(item.getSpecId(), count);
			}
		}

		// so that it adds d-mods
		ships.inflateIfNeeded();
		for (FleetMemberAPI member : ships.getFleetData().getMembersListCopy()) {
			// it should be due to the inflateIfNeeded() call, this is just a safety check
			if (member.getVariant().getSource() == VariantSource.REFIT) {
				member.getVariant().clear();
			}
			//闲置 船舶
			cargo.getMothballedShips().addFleetMember(member);
		}
	}
	//显示货物信息
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
			//添加节点标题
			info.addSectionHeading(key, faction.getBaseUIColor(), faction.getDarkUIColor(),
								   Alignment.MID, opad);
			//飞机武器
			if (!cargo.getStacksCopy().isEmpty()) {
				info.addPara("Ship weapons and fighters:", opad);
				info.showCargo(cargo, 20, true, opad);
			}

			if (!cargo.getMothballedShips().getMembersListCopy().isEmpty()) {
				CountingMap<String> counts = new CountingMap<String>();
				for (FleetMemberAPI member : cargo.getMothballedShips().getMembersListCopy()) {
					counts.add(member.getVariant().getHullSpec().getHullName() + " " + member.getVariant().getDesignation());
				}
				//舰船
				info.addPara("Ship hulls:", opad);
				info.showShips(cargo.getMothballedShips().getMembersListCopy(), 20, true,
							   getCurrentStage() == Stage.WAITING, opad);
			}
		}
	}
	//挑选军火商重要性
	public PersonImportance pickArmsDealerImportance() {
		WeightedRandomPicker<PersonImportance> picker = new WeightedRandomPicker<PersonImportance>(genRandom);

		picker.add(PersonImportance.VERY_LOW, 10f);
		picker.add(PersonImportance.LOW, 10f);

//		int credits = (int) Global.getSector().getPlayerFleet().getCargo().getCredits().get();
//		if (credits >= 200000) {
//			picker.add(PersonImportance.MEDIUM, 10f);
//		}
//		if (credits >= 1000000) {
//			picker.add(PersonImportance.HIGH, 10f);
//		}
//		if (credits >= 200000) {
//			picker.add(PersonImportance.VERY_HIGH, 10f);
//		}

		float cycles = PirateBaseManager.getInstance().getDaysSinceStart() / 365f;
		if (cycles > 1f) {
			picker.remove(PersonImportance.VERY_LOW);
			picker.add(PersonImportance.MEDIUM, 20f);
		}
		if (cycles > 3f) {
			picker.remove(PersonImportance.LOW);
			picker.add(PersonImportance.HIGH, 10f);
		}
		if (cycles > 5f) {
			//picker.add(PersonImportance.VERY_HIGH, 10f);
			// always very high importance past a certain point, since the goal is to allow easier procurement
			// of almost any "generally available" hull
			return PersonImportance.VERY_HIGH;
		}
		
		return picker.pick();
	}
	
}











