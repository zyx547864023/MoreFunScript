package real_combat.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.RCModPlugin;
import real_combat.ai.*;

import java.util.*;

/**
 * AI
 */
public class RC_SmartAIEveryFrameCombatPlugin implements EveryFrameCombatPlugin {
	private CombatEngineAPI engine = Global.getCombatEngine();
	private static final String AISET = "data/config/aiset.csv";
	private static final String ID = "RC_SmartAIEveryFrameCombatPlugin";

	private static Map<String,String> aiSet = new HashMap<>();
	private static boolean initialized = false;
	private float enemyOP = 0;
	private float playerOP =0;
	protected IntervalUtil tracker = new IntervalUtil(10f, 11f);
	public static void reloadSettings(){
		SettingsAPI settings = Global.getSettings();
		List<ModSpecAPI> mods = settings.getModManager().getEnabledModsCopy();
		for (ModSpecAPI mod : mods) {
			JSONArray aiSetJSONArray;
			try {
				aiSetJSONArray = settings.loadCSV(AISET, mod.getId());
				if (aiSetJSONArray.length()!=0) {
					for (int i = 0; i < aiSetJSONArray.length(); i++) {
						try {
							JSONObject row = aiSetJSONArray.getJSONObject(i);
							String id = row.getString("id");
							//每个白名单来自于哪个mod
							String ai = row.getString("ai");
							aiSet.put(id,ai);
						} catch (Exception e) {
							continue;
						}
					}
				}
			} catch (Exception e) {
				continue;
			}
		}
		initialized = true;
	}
	@Override
	public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

	}
	/*
	if (s.getOwner() == 0&&!s.isFighter()&&s!=Global.getCombatEngine().getPlayerShip() && s.isAlive()) {
		RC_SmartAI smartAI = new RC_SmartAI(s);
		smartAI.firingSynergy();
		smartAI.saveMissile();
		smartAI.safeV();
		smartAI.retreatCommand();
	}
	 */
	@Override
	public void advance(float amount, List<InputEventAPI> events) {
		if (engine==null) {return;}
		if (engine.isPaused()) {return;}
		if (!RCModPlugin.isSmartAIEnabled()) {return;}
		/*
		if (playerOP==0&&enemyOP==0) {
			for (ShipAPI s : engine.getShips()) {
				if (s.getOwner() == 0 && s.isAlive() && !s.isFighter()) {
					playerOP += s.getHullSpec().getFleetPoints();
				} else if (s.getOwner() == 1 && s.isAlive() && !s.isFighter()) {
					enemyOP += s.getHullSpec().getFleetPoints();
				}
			}
		}
		else {
			tracker.advance(amount);
			if(tracker.intervalElapsed()) {
				playerOP=0;
				enemyOP=0;
				for (ShipAPI s : engine.getShips()) {
					if (s.getOwner() == 0 && s.isAlive() && !s.isFighter()) {
						playerOP += s.getHullSpec().getFleetPoints();
					} else if (s.getOwner() == 1 && s.isAlive() && !s.isFighter()) {
						enemyOP += s.getHullSpec().getFleetPoints();
					}
				}
			}
		}
		 */
		/*
		if (Global.getSector().getPlayerFleet()!=null){
			if (Global.getSector().getPlayerFleet().getBattle()!=null){
				if (Global.getSector().getPlayerFleet().getBattle().getNonPlayerCombined()!=null){
					playerOP = getTotalCombatOP(Global.getSector().getPlayerFleet().getFleetData());
					enemyOP = getTotalCombatOP(Global.getSector().getPlayerFleet().getBattle().getNonPlayerCombined().getFleetData());
				}
			}
		}
		*/
		Set<ShipAPI> enemyList = new HashSet<>();
		if (engine.getCustomData().get(ID+"enemyList")!=null) {
			enemyList = (Set<ShipAPI>) engine.getCustomData().get(ID+"enemyList");
		}
		for (ShipAPI s:engine.getShips()) {
			if (s.getOwner() == 1&&!s.isFighter()&&s!=Global.getCombatEngine().getPlayerShip() && s.isAlive()) {
				CombatFleetManagerAPI manager = engine.getFleetManager(s.getOwner());
				CombatTaskManagerAPI task = manager.getTaskManager(false);
				CombatFleetManagerAPI.AssignmentInfo mission = task.getAssignmentFor(s);
				Vector2f targetLocation = null;
				boolean missionClear = false;
				if (mission!=null) {
					if ((mission.getType() == CombatAssignmentType.CAPTURE)&&s.getHullSize().compareTo(ShipAPI.HullSize.DESTROYER)<=0) {
						targetLocation = mission.getTarget().getLocation();
						for (BattleObjectiveAPI o:engine.getObjectives()) {
							if (o.getOwner()==s.getOwner()&&o.getLocation().equals(targetLocation)) {
								missionClear = true;
							}
						}
					}
					else {
						task.removeAssignment(mission);
						missionClear = true;
					}
				}
				else {
					missionClear = true;
				}
				if (missionClear) {
					if (s.getCustomData().get("RC_ShipAI") == null) {
						if(playerOP<=enemyOP) {
							if (aiSet.get(s.getHullSpec().getBaseHullId()) != null) {
								if ("base".equals(aiSet.get(s.getHullSpec().getBaseHullId()))) {
									s.setCustomData("RC_ShipAI", "base");
									s.setShipAI(new RC_BaseShipAI(s));

									enemyList.add(s);
								}
								else if("frigate".equals(aiSet.get(s.getHullSpec().getBaseHullId()))){
									s.setCustomData("RC_ShipAI", "frigate");
									s.setShipAI(new RC_FrigateAI(s));

									enemyList.add(s);
								}
								else if("carrir_combat".equals(aiSet.get(s.getHullSpec().getBaseHullId()))){
									s.setCustomData("RC_ShipAI", "carrir_combat");
									s.setShipAI(new RC_CarrirCombatAI(s));

									enemyList.add(s);
								}
								else if("carrir".equals(aiSet.get(s.getHullSpec().getBaseHullId()))){
									s.setCustomData("RC_ShipAI", "carrir");
									s.setShipAI(new RC_CarrirAI(s));

									enemyList.add(s);
								}
								else if("onslaught".equals(aiSet.get(s.getHullSpec().getBaseHullId()))){
									s.setCustomData("RC_ShipAI", "onslaught");
									s.setShipAI(new RC_OnslaughtAI(s));

									enemyList.add(s);
								}
								else {
									s.setCustomData("RC_ShipAI", "base");
									s.setShipAI(new RC_BaseShipAI(s));

									enemyList.add(s);
								}
							}
						}
					}
					else {
						//
						if(playerOP>enemyOP) {
							enemyList.remove(s);
							s.removeCustomData("RC_ShipAI");
							s.resetDefaultAI();
						}
					}
				}
				else {
					if (s.getCustomData().get("RC_ShipAI") != null) {
						enemyList.remove(s);
						s.removeCustomData("RC_ShipAI");
						s.resetDefaultAI();
					}
				}
				//刷新
				if (s.getCustomData().get("RC_ShipAI") != null) {

				}
			}
			//可供支配的队友
			engine.getCustomData().put(ID+"enemyList",enemyList);
		}
	}

	@Override
	public void renderInWorldCoords(ViewportAPI viewport) {

	}

	@Override
	public void renderInUICoords(ViewportAPI viewport) {
	}

	@Override
	public void init(CombatEngineAPI engine) {
	}

	public static float getTotalCombatOP(FleetDataAPI data) {
		float op = 0;
		for (FleetMemberAPI curr : data.getMembersListCopy()) {
			if (curr.isMothballed()) continue;
			if (isCivilian(curr)) continue;
			op += getPoints(curr);
		}
		return Math.round(op);
	}

	public static boolean isCivilian(FleetMemberAPI member) {
		if (member == null) return false;
		MutableShipStatsAPI stats = member.getStats();
		return stats != null && stats.getVariant() != null &&
				((stats.getVariant().hasHullMod(HullMods.CIVGRADE) &&
						!stats.getVariant().hasHullMod(HullMods.MILITARIZED_SUBSYSTEMS)) ||
						(!stats.getVariant().hasHullMod(HullMods.CIVGRADE) &&
								stats.getVariant().getHullSpec().getHints().contains(ShipHullSpecAPI.ShipTypeHints.CIVILIAN)));// &&
		//!stats.getVariant().getHullSpec().getHints().contains(ShipTypeHints.CARRIER);
	}

	protected static float getPoints(FleetMemberAPI member) {
		return member.getDeploymentPointsCost();
	}
}
