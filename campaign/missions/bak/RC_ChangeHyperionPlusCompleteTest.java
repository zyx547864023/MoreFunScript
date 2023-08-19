package real_combat.campaign.missions.bak;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc.Token;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * 1、完成之后可以更换模块
 * 2、更换模块所需要的材料比较少
 * 3、无限循环
 */
public class RC_ChangeHyperionPlusCompleteTest extends BaseBarEventWithPerson { //implements ShipRecoveryListener {
	public static float PROD_DAYS = 1f;
	public static enum Stage {
		START,
		WAITING,
		DELIVERED,
		COMPLETED, // unused, left in for save compat 兼容性
		FAILED
	}
	public enum OptionId {
		YES,
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

	transient protected InteractionDialogAPI interactionDialog;
	protected int page = 0;
	protected int tier = 0;

	protected boolean callAction(String action, String ruleId, InteractionDialogAPI dialog, List<Token> params,
								 Map<String, MemoryAPI> memoryMap) {
		//第一次由callAction添加按钮 按钮内容是 玩家里面的船
		//一共三级 船 船插 想要更换的船
		if ("showOption".equals(action)) {
			boolean hasIndustry = market.hasIndustry(Industries.ORBITALWORKS);
			if (!hasIndustry)
			{
				//家没了
				//FireBest.fire(null, dialog, memoryMap, "RC_ChangeHyperionIndustry");
				//return true;
			}
			interactionDialog = dialog;
			OptionPanelAPI options = dialog.getOptionPanel();
			TextPanelAPI text = dialog.getTextPanel();
			//初始化
			options.clearOptions();
			page = 0;
			tier = 0;
			optionList = new ArrayList<>();
			fleetData = Global.getSector().getPlayerFleet().getFleetData();
			addOption(text,fleetData);
			addOption(options, page, optionList);
			return true;
		}
		else if ("change".equals(action)) {
			interactionDialog = dialog;
			OptionPanelAPI options = dialog.getOptionPanel();
			TextPanelAPI text = dialog.getTextPanel();

			//取到要改的船 要改的插槽 要改的
			ShipVariantAPI variant = fleetMember.getVariant();
			ShipVariantAPI nowModuleVarint = fleetMember.getVariant().getModuleVariant(slot.slotId);
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
			module.moduleVariant.setSource(VariantSource.REFIT);
			variant.setModuleVariant(slot.slotId,module.moduleVariant);
			//已改完
			//改完之后返回到主列表
			return true;
		}
		return true;
	}

	@Override
	public void init(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		try {
			super.init(dialog, memoryMap);
			done = false;
			person = Global.getSector().getImportantPeople().getPerson("MeiKo");
			dialog.getVisualPanel().showPersonInfo(person, true);
			optionSelected("我草", OptionId.YES);
			optionSelected("离开", OptionId.LEAVE);
		} catch (Exception e) {

		}
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
		try {
			super.addPromptAndOption(dialog, memoryMap);

			regen(dialog.getInteractionTarget().getMarket());

			TextPanelAPI text = dialog.getTextPanel();
			text.addPara("An animated " + getManOrWoman() + " is loudly telling what seems to be a story " +
					"about the tribulations and exploits of some fleet. A few patrons look on with varying " +
					"degrees of interest.");

			dialog.getOptionPanel().addOption("Listen to the animated storyteller", this);
		} catch (Exception e) {
			Global.getLogger(this.getClass()).info(e);
		}
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
		if (page!=0) {
			options.addOption("上一页", OptionId.LAST);
		}
		int count = 0;
		for (int i=page*5;i<optionList.size();i++) {
			if (count<5) {
				Option option = optionList.get(i);
				options.addOption(option.text, option.key);
				count++;
			}
		}
		if (page<optionList.size()/5) {
			options.addOption("下一页", OptionId.NEXT);
		}
		if (tier!=0) {
			options.addOption("返回上一层", OptionId.RETURN);
		}
		else {
			options.addOption("离开", "RC_ChangeHyperionLeave");
		}
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
				optionList.add(new Option("更换 " + s + "（" + hullName + "）模块",new Slot(s,hullName)));
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
				if (!f.equals(fleetMember)&&!f.getHullSpec().getHullName().equals(slot.hullName))
				{
					ShipVariantAPI newModuleVarint = f.getVariant();
					String hullName = newModuleVarint.getHullSpec().getHullName();
					String shipName = f.getShipName();
					optionList.add(new Option("使用 "+hullName+"（"+shipName+"）作为新的模块",newModuleVarint));
				}
			}
		}
		else if((optionData instanceof Module)) {
			module = (Module)optionData;
			text.addPara("改造需要 %s 星币",Color.YELLOW,fleetMember.getHullSpec().getBaseValue()+"");
			optionList.add(new Option("确认改造","RC_ChangeHyperionChange"));
			//展示改造后的效果
			CampaignFleetAPI ships = Global.getFactory().createEmptyFleet(market.getFactionId(), "temp", true);
			ships.getFleetData().addFleetMember(fleetMember.getVariant().getHullVariantId());
			for (FleetMemberAPI f : ships.getFleetData().getMembersListCopy()) {
				ShipVariantAPI varint = f.getVariant();
				varint.setSource(VariantSource.REFIT);
				ShipVariantAPI oldVarint = f.getVariant().getModuleVariant(slot.slotId);
				oldVarint.setSource(VariantSource.REFIT);
				module.moduleVariant.setSource(VariantSource.REFIT);
				varint.setModuleVariant(slot.slotId, module.moduleVariant);
				interactionDialog.getVisualPanel().showFleetMemberInfo(f, true);
			}
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





