package real_combat.campaign.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 1、完成之后可以更换模块
 * 2、更换模块所需要的材料比较少
 * 3、无限循环
 */
public class RC_ChangeHyperionPlusComplete extends HubMissionWithBarEvent implements PortsideBarEvent { //implements ShipRecoveryListener {
	public static float PROD_DAYS = 1f;
	public static enum Stage {
		START,
		WAITING,
		DELIVERED,
		COMPLETED, // unused, left in for save compat 兼容性
		FAILED
	}
	public enum OptionId {
		LAST,
		NEXT,
		RETURN,
		LEAVE
	}
	protected MarketAPI market;
	protected PersonAPI callisto;
	protected FactionAPI faction;

	protected List<Object> storeList = new ArrayList<>();
	protected List<Option> optionList = new ArrayList<>();
	protected FleetDataAPI fleetData;
	//要改装的船
	protected FleetMemberAPI fleetMember;
	//要改装的插槽
	protected Slot slot;
	//要更换上去的模块
	protected Module module;
	protected int cost;
	transient protected InteractionDialogAPI interactionDialog;
	protected int page = 0;
	protected int tier = 0;
	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		if (!setGlobalReference("$RC_ChangeHyperionPlusComplete_ref", null)) {
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
		//connectWithDaysElapsed(Stage.WAITING, Stage.DELIVERED, 1f);
		//set 阶段 On 市场 不文明
		setStageOnMarketDecivilized(Stage.FAILED, market);
		return true;
	}
	
	protected boolean callAction(String action, String ruleId, InteractionDialogAPI dialog, List<Token> params,
								 Map<String, MemoryAPI> memoryMap) {
		OptionPanelAPI options = dialog.getOptionPanel();
		TextPanelAPI text = dialog.getTextPanel();
		//第一次由callAction添加按钮 按钮内容是 玩家里面的船
		//一共三级 船 船插 想要更换的船
		if (action.contains("showOption")) {
			String index = action.replace("showOption","");
			boolean hasIndustry = market.hasIndustry(Industries.ORBITALWORKS);
			if (!hasIndustry)
			{
				//家没了
				FireBest.fire(null, dialog, memoryMap, "RC_ChangeHyperionIndustry");
				return true;
			}
			if (optionList.size()==0) {
				interactionDialog = dialog;
				//初始化
				tier = 0;
				fleetData = Global.getSector().getPlayerFleet().getFleetData();
				addOption(text, fleetData);
				addOption(options, page, optionList);
			}
			else {
				tier += 1;
				//这就知道他选了哪个了
				if (optionList.isEmpty()) {
					tier = 0;
					fleetData = Global.getSector().getPlayerFleet().getFleetData();
					addOption(text, fleetData);
					addOption(options, page, optionList);
				}
				else {
					Option optionData = optionList.get(Integer.parseInt(index) + page * 5);
					optionList = new ArrayList<>();
					storeList.add(optionData.key);
					addOption(text, optionData.key);
					addOption(options, page, optionList);
				}
				page = 0;
			}
			return true;
		}
		else if ("last".equals(action)) {
			page -= 1;
			addOption(options, page, optionList);
			return true;
		}
		else if ("next".equals(action)) {
			page += 1;
			addOption(options, page, optionList);
			return true;
		}
		else if ("return".equals(action)) {
			tier -= 1;
			page = 0;
			optionList = new ArrayList<>();
			if (storeList.size() - 1>=0) {
				storeList.remove(storeList.size() - 1);
				if (storeList.size()>0) {
					Object optionData = storeList.get(storeList.size() - 1);
					addOption(text, optionData);
				}
				else {
					fleetData = Global.getSector().getPlayerFleet().getFleetData();
					addOption(text, fleetData);
				}
			}
			addOption(options, page, optionList);
			return true;
		}
		else if ("leave".equals(action)) {
			page = 0;
			tier = 0;
			optionList = new ArrayList<>();
			storeList = new ArrayList<>();
			return true;
		}
		else if ("change".equals(action)) {
			page = 0;
			tier = 0;
			optionList = new ArrayList<>();
			storeList = new ArrayList<>();
			interactionDialog = dialog;

			CargoAPI cargo = fleetData.getFleet().getCargo();
			if (cargo.getCredits().get()<cost){
				FireBest.fire(null, dialog, memoryMap, "RC_ChangeHyperionNeed");
				return true;
			}

			//取到要改的船 要改的插槽 要改的
			ShipVariantAPI variant = fleetMember.getVariant();
			ShipVariantAPI nowModuleVarint = fleetMember.getVariant().getModuleVariant(slot.slotId);

			variant.setSource(VariantSource.REFIT);
			nowModuleVarint.setSource(VariantSource.REFIT);
			module.moduleVariant.setSource(VariantSource.REFIT);
			variant.setModuleVariant(slot.slotId,module.moduleVariant);
			//已改完
			AddRemoveCommodity.addCreditsLossText(cost, dialog.getTextPanel());
			Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(cost);

			AddRemoveCommodity.addFleetMemberLossText(module.moduleMember, dialog.getTextPanel());
			fleetData.removeFleetMember(module.moduleMember);
			//改完之后返回到主列表
			Global.getSoundPlayer().playUISound("ui_refit_slot_cleared_large", 1, 1);
			//adjustRep(dialog.getTextPanel(), null, CoreReputationPlugin.RepActions.MISSION_SUCCESS);
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

	@Override
	public void init(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {

	}

	@Override
	public boolean isDialogFinished() {
		return false;
	}

	@Override
	public boolean endWithContinue() {
		return false;
	}

	/**
	 * 按钮点击之后 改造需要的钱
	 * @param optionText
	 * @param optionData
	 */
	@Override
	public void optionSelected(String optionText, Object optionData) {
		try {
			OptionPanelAPI options = interactionDialog.getOptionPanel();
			TextPanelAPI text = interactionDialog.getTextPanel();
			options.clearOptions();
			if (!(optionData instanceof String)) {
				//如果按钮不是 清空
				if (!(optionData instanceof OptionId)) {
					page = 0;
					optionList = new ArrayList<>();
				}
				//功能按钮
				if (optionData instanceof OptionId) {
					OptionId optionId = (OptionId) optionData;
					if (OptionId.LAST.equals(optionId)) {
						page -= page;
					} else if (OptionId.NEXT.equals(optionId)) {
						page += page;
					} else if (OptionId.RETURN.equals(optionId)) {
						storeList.remove(storeList.size() - 1);
						optionData = storeList.get(storeList.size() - 1);
						addOption(text, optionData);
					}
				}
				else {
					optionList = new ArrayList<>();
					storeList.add(optionData);
					addOption(text, optionData);
				}
				addOption(options, page, optionList);
			}
		}
		catch (Exception e) {
			Global.getLogger(this.getClass()).info(e);
		}
	}

	@Override
	public boolean shouldRemoveEvent() {
		return false;
	}

	@Override
	public void addPromptAndOption(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {

	}

	@Override
	public void wasShownAtMarket(MarketAPI market) {

	}

	@Override
	public String getBarEventId() {
		return null;
	}

	@Override
	public boolean isAlwaysShow() {
		return false;
	}

	private void addOption(OptionPanelAPI options, int page, List<Option> optionList)
	{
		options.clearOptions();
		if (tier == 3) {
			options.addOption(optionList.get(0).text, "RC_ChangeHyperionChange");
		}
		else {
			int count = 0;
			for (int i = page * 5; i < optionList.size(); i++) {
				if (count < 5) {
					Option option = optionList.get(i);
					options.addOption(option.text, "RC_ChangeHyperionShowOption" + count);
					count++;
				}
			}
			if (page!=0 && optionList.size()>5) {
				options.addOption("上一页", "RC_ChangeHyperionLast");
			}
			if (page<optionList.size()/5) {
				options.addOption("下一页", "RC_ChangeHyperionNext");
			}
		}
		if (tier!=0) {
			options.addOption("返回上一层", "RC_ChangeHyperionReturn");
		}
		options.addOption("离开", "RC_ChangeHyperionLeave");
	}

	private void addOption(TextPanelAPI text,Object optionData)
	{
		if ((optionData instanceof FleetDataAPI)) {
			FleetDataAPI fleetData = (FleetDataAPI)optionData;
			List<FleetMemberAPI> playerFleetMembers = fleetData.getMembersListCopy();
			for (FleetMemberAPI f:playerFleetMembers)
			{
				//按钮ID就是船的ID
				if (f.getVariant().getModuleSlots().size()>0)
				{
					String shipName = f.getShipName();
					String hullName = f.getHullSpec().getHullName();
					optionList.add(new Option("改造"+hullName+"（"+shipName+"）",f));
				}
			}
		}
		else if ((optionData instanceof FleetMemberAPI)) {
			//初始化
			fleetMember = (FleetMemberAPI)optionData;
			//展示这艘模块船
			interactionDialog.getVisualPanel().showFleetMemberInfo(fleetMember, true);
			//列出所有模块部分
			List<String> slotIds = fleetMember.getVariant().getModuleSlots();
			for (String s:slotIds) {
				String hullName = fleetMember.getVariant().getModuleVariant(s).getHullSpec().getHullName();
				optionList.add(new Option("更换 " + hullName + "（" + s + "）模块",new Slot(s,hullName)));
			}
		}
		else if((optionData instanceof Slot)) {
			slot = (Slot)optionData;
			//选择玩家的材料船
			FleetDataAPI fleetData = Global.getSector().getPlayerFleet().getFleetData();
			List<FleetMemberAPI> playerFleetMembers = fleetData.getMembersListCopy();
			for (FleetMemberAPI f:playerFleetMembers)
			{
				//排除掉自己和相同的船
				if (!f.getHullSpec().getHullName().equals(fleetMember.getHullSpec().getHullName())&&!f.getHullSpec().getHullName().equals(slot.hullName))
				{
					ShipVariantAPI newModuleVarint = f.getVariant();
					String hullName = newModuleVarint.getHullSpec().getHullName();
					String shipName = f.getShipName();
					optionList.add(new Option("使用 "+hullName+"（"+shipName+"）作为新的模块",new Module(f,newModuleVarint)));
				}
			}
		}
		else if((optionData instanceof Module)) {
			module = (Module)optionData;
			cost = (int)fleetMember.getHullSpec().getBaseValue();
			text.addPara("改造需要 %s 星币",Color.YELLOW,cost+"");
			optionList.add(new Option("确认改造","RC_ChangeHyperionChange"));
			//展示改造后的效果
			FleetMemberAPI newFleetMember = Global.getFactory().createFleetMember(FleetMemberType.SHIP,fleetMember.getVariant());

			ShipVariantAPI varint = newFleetMember.getVariant();
			varint.setSource(VariantSource.REFIT);
			ShipVariantAPI oldVarint = newFleetMember.getVariant().getModuleVariant(slot.slotId);
			oldVarint.setSource(VariantSource.REFIT);
			module.moduleVariant.setSource(VariantSource.REFIT);
			varint.setModuleVariant(slot.slotId, module.moduleVariant);
			interactionDialog.getVisualPanel().showFleetMemberInfo(newFleetMember, true);
			//显示完改回去
			varint.setModuleVariant(slot.slotId, oldVarint);
			module.moduleVariant.removePermaMod("RC_TrinityForceModule");
			module.moduleVariant.removePermaMod("converted_fighterbay");
		}
	}

	public class Option{
		String text;
		Object key;
		public Option(String text,Object key){
			this.text = text;
			this.key = key;

		}
	}

	public class Slot{
		String slotId;
		String hullName;
		public Slot(String slotId,String hullName){
			this.slotId = slotId;
			this.hullName = hullName;
		}
	}

	public class Module{
		FleetMemberAPI moduleMember;
		ShipVariantAPI moduleVariant;
		public Module(FleetMemberAPI moduleMember,ShipVariantAPI moduleVariant){
			this.moduleMember = moduleMember;
			this.moduleVariant = moduleVariant;
		}
	}
}





