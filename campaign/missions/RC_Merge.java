package real_combat.campaign.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 融合
 * 两艘S插的船融合叠加S插
 * seed = Misc.genRandomSeed();
 * 两个船的ID会产生固定结果
 * Random random = Misc.getRandom(Global.getSector().getPlayerBattleSeed(), 11);
 * 两个船 相同的船插大概率保留0.8
 * 最大船插位 全部船插合并
 * 最小船插数 最小船插素材
 * 选船
 * 显示每个船插保留的概率
 * 模块与模块融合
 */
public class RC_Merge extends HubMissionWithBarEvent { //implements ShipRecoveryListener {
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

	protected FleetDataAPI fleetData;
	protected int cost;
	transient protected InteractionDialogAPI interactionDialog;

	protected List<FleetMemberAPI> merge = new ArrayList<>();
	protected List<String> smods = new ArrayList<>();
	protected Map<String,List<String>> moduleSmods = new HashMap<>();
	protected Map<String,Double> chanceMap = new HashMap<>();
	protected Map<String,Map<String,Double>> moduleChanceMap = new HashMap<>();
	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		if (!setGlobalReference("$RC_Merge_ref", null)) {
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
		setStartingStage(RC_Merge.Stage.WAITING);
		setSuccessStage(RC_Merge.Stage.DELIVERED);
		setFailureStage(RC_Merge.Stage.FAILED);
		//不能放弃
		setNoAbandon();
		//经过多少天从这个状态到另一个状态
		connectWithDaysElapsed(RC_Merge.Stage.WAITING, RC_Merge.Stage.DELIVERED, PROD_DAYS);
		//set 阶段 On 市场 不文明
		setStageOnMarketDecivilized(RC_Merge.Stage.FAILED, market);
		return true;
	}

	protected boolean callAction(String action, String ruleId, InteractionDialogAPI dialog, List<Token> params,
								 Map<String, MemoryAPI> memoryMap) {
		TextPanelAPI text = dialog.getTextPanel();
		if ("showPicker".equals(action)) {
			fleetData = Global.getSector().getPlayerFleet().getFleetData();
			merge.clear();
			smods.clear();
			chanceMap = new HashMap<>();
			moduleSmods = new HashMap<>();
			moduleChanceMap = new HashMap<>();
			showPicker(dialog,memoryMap);
			return true;
		}else if ("showChance".equals(action)) {
			int mergeCount = 0;
			if (Global.getSector().getMemoryWithoutUpdate().get("$RC_Merge_mergeCount")!=null) {
				mergeCount = (int)Global.getSector().getMemoryWithoutUpdate().get("$RC_Merge_mergeCount");
			}

			Set<String> smodsSet = new HashSet<>();
			smodsSet.addAll(smods);
			for (String ss:smodsSet) {
				int count = 0;
				for (String s:smods) {
					if (s.equals(ss)) {
						count++;
					}
				}
				HullModSpecAPI hullMod = Global.getSettings().getHullModSpec(ss);
				double chance = (1f - 1f/(count+1f))*100f+mergeCount;
				chanceMap.put(ss,chance);

				//显示概率
				Color color = Color.GREEN;
				if (chance<=34) {
					color = Color.RED;
				}
				else if (chance<=50) {
					color = Color.YELLOW;
				}
				text.addPara(merge.get(0).getHullSpec().getHullName() + " " + merge.get(0).getShipName() + " " +hullMod.getDisplayName()+" 改造后保留概率为： %s", color, (new BigDecimal(chance).setScale(2, RoundingMode.UP)+"%"));
			}
			for (String slotId:merge.get(0).getVariant().getModuleSlots()) {
				List<String> moduleSmod = moduleSmods.get(slotId);
				Map soltChanceMap = new HashMap();
				if (moduleSmod!=null) {
					Set<String> mSmodsSet = new HashSet<>();
					mSmodsSet.addAll(moduleSmod);
					for (String ss:mSmodsSet) {
						int count = 0;
						for (String s:moduleSmod) {
							if (s.equals(ss)) {
								count++;
							}
						}
						HullModSpecAPI hullMod = Global.getSettings().getHullModSpec(ss);
						double chance = (1f - 1f/(count+1f))*100f+mergeCount;
						soltChanceMap.put(ss,chance);

						//显示概率
						Color color = Color.GREEN;
						if (chance<=34) {
							color = Color.RED;
						}
						else if (chance<=50) {
							color = Color.YELLOW;
						}
						text.addPara(merge.get(0).getVariant().getModuleVariant(slotId).getHullSpec().getHullName() + " " +hullMod.getDisplayName()+" 改造后保留概率为： %s", color, (new BigDecimal(chance).setScale(2, RoundingMode.UP)+"%"));
					}
				}
				moduleChanceMap.put(slotId,soltChanceMap);
			}
			mergeCount++;
			Global.getSector().getMemoryWithoutUpdate().set("$RC_Merge_mergeCount", mergeCount);
			text.addPara("%s",Color.RED,"注意！改造完成后仅保留第一艘船素材！");
			dialog.getVisualPanel().showFleetMemberInfo(merge.get(0), true);
			return true;
		}
		else if ("merge".equals(action)) {
			interactionDialog = dialog;
			String ids = "";
			FleetMemberAPI first = merge.get(0);
			for (FleetMemberAPI m:merge) {
				ids+=m.getId();
				if ( m!=first ) {
					AddRemoveCommodity.addFleetMemberLossText(m, dialog.getTextPanel());
					fleetData.removeFleetMember(m);
				}
			}
			//正则表达式以匹配字符串中的数字
			String regex = "\\d+";
			//创建一个模式对象
			Pattern pattern = Pattern.compile(regex);
			//创建一个Matcher对象
			Matcher matcher = pattern.matcher(ids);
			String seed = "";
			while(matcher.find()) {
				seed+=matcher.group();
			}
			Random random = Misc.getRandom(Long.parseLong(seed), 11);
			Set<String> smodsSet = new HashSet<>();
			smodsSet.addAll(smods);
			first.getVariant().setSource(VariantSource.REFIT);
			for (String s:smodsSet) {
				first.getVariant().removePermaMod(s);
				double chance = chanceMap.get(s);
				if (random.nextDouble()<=chance/100f) {
					first.getVariant().addPermaMod(s,true);
				}
			}
			for (String slotId:first.getVariant().getModuleSlots()) {
				Set<String> mSmodsSet = new HashSet<>();
				mSmodsSet.addAll(moduleSmods.get(slotId));
				first.getVariant().getModuleVariant(slotId).setSource(VariantSource.REFIT);
				for (String s:mSmodsSet) {
					first.getVariant().removePermaMod(s);
					double chance = moduleChanceMap.get(slotId).get(s);
					if (random.nextDouble()<=chance/100f) {
						first.getVariant().getModuleVariant(slotId).addPermaMod(s,true);
					}
				}
			}
			AddRemoveCommodity.addFleetMemberGainText(first,text);
			dialog.getVisualPanel().showFleetMemberInfo(first, true);
			//改完之后返回到主列表
			Global.getSoundPlayer().playUISound("ui_refit_slot_cleared_large", 1, 1);
			FireBest.fire(null, dialog, memoryMap, "RC_Merge_Next");
			return true;
		}
		return super.callAction(action, ruleId, dialog, params, memoryMap);
	}

	protected void updateInteractionDataImpl() {
		set("$RC_Merge_cost", "200,000");
	}

	protected void showPicker(final InteractionDialogAPI dialog, final Map<String, MemoryAPI> memoryMap) {
		List<FleetMemberAPI> members = getEligibleShips(null);
		int cols = Math.min(members.size(), 7);
		if (cols < 4) cols = 4;
		int rows = (int)Math.ceil(members.size()/(float)cols);
		if (rows == 0) rows = 1;

		dialog.showFleetMemberPickerDialog("选择分析素材",
				"确认",
				"取消",
				rows, cols, 96,
				true, true, members,
				new FleetMemberPickerListener() {
					public void pickedFleetMembers(List<FleetMemberAPI> members) {
						if (members.isEmpty()) return;
						String baseHullId = members.get(0).getHullSpec().getBaseHullId();
						for (FleetMemberAPI m:members) {
							if (!baseHullId.equals(m.getHullSpec().getHullId())) {
								FireAll.fire(null, dialog, memoryMap, "RC_Merge_NotSame");
								return;
							}
						}
						pickShip(members);
						FireAll.fire(null, dialog, memoryMap, "RC_Merge_ShipPicked");
					}
					public void cancelledFleetMemberPicking() {

					}
				});
	}


	protected void pickShip(List<FleetMemberAPI> members) {
		merge.addAll(members);
		for (FleetMemberAPI m:members) {
			smods.addAll(m.getVariant().getSMods());
			for (String s:m.getVariant().getModuleSlots()) {
				ShipVariantAPI variant =  m.getVariant().getModuleVariant(s);
				List<String> newModuleSMods = new ArrayList<>();
				if (moduleSmods.get(s)!=null) {
					newModuleSMods = (List<String>) moduleSmods.get(s);
				}
				newModuleSMods.addAll(variant.getSMods());
				moduleSmods.put(s,newModuleSMods);
			}
		}
	}

	protected List<FleetMemberAPI> getEligibleShips(FleetMemberAPI fleetMember) {
		CampaignFleetAPI player = Global.getSector().getPlayerFleet();
		List<FleetMemberAPI> bestList = new ArrayList<>();

		// initial population from player fleet
		for (FleetMemberAPI member : player.getFleetData().getMembersListWithFightersCopy()) {
			//if (BuyShipRule.isShipAllowedStatic(member, this)) {
			if (member.getVariant()==null) {
				continue;
			}
			if (member.getVariant().getSMods()==null) {
				continue;
			}
			if (member.getVariant().getSMods().size()<2) {
				continue;
			}
			if (fleetMember==null) {
				if (!member.isFighterWing()) {
					bestList.add(member);
				}
			}
			else {
				if (member.getHullSpec().getBaseHullId().equals(fleetMember.getHullSpec().getBaseHullId())) {
					bestList.add(member);
				}
			}
			//}
		}
		return bestList;
	}
}





