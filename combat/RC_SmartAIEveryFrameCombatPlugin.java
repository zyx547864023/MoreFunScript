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
import org.lazywizard.lazylib.VectorUtils;
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
	protected IntervalUtil tracker = new IntervalUtil(2f, 3f);
	protected IntervalUtil inTracker = new IntervalUtil(2f, 3f);
	protected IntervalUtil outTracker = new IntervalUtil(0.1f, 0.2f);
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
		if (Global.getSector().getPlayerFleet()!=null){
			if (Global.getSector().getPlayerFleet().getBattle()!=null){
				if (Global.getSector().getPlayerFleet().getBattle().getNonPlayerCombined()!=null){
					playerOP = getTotalCombatOP(Global.getSector().getPlayerFleet().getFleetData());
					enemyOP = getTotalCombatOP(Global.getSector().getPlayerFleet().getBattle().getNonPlayerCombined().getFleetData());
				}
			}
		}
		Set<ShipAPI> enemyList = new HashSet<>();
		if (engine.getCustomData().get(ID+"enemyList")!=null) {
			enemyList = (Set<ShipAPI>) engine.getCustomData().get(ID+"enemyList");
		}
		//outTracker.advance(amount);
		inTracker.advance(amount);
		//if (outTracker.intervalElapsed()) {
			for (ShipAPI s : engine.getShips()) {
				//
				if (!s.isFighter() && s.isAlive()) {
					/*
					if (inTracker.intervalElapsed()) {
						RC_BaseShipAI.Relationship relationship = new RC_BaseShipAI.Relationship();
						if (engine.getCustomData().get("RC_BaseShipAI.Relationship" + s.getId()) != null) {
							relationship = (RC_BaseShipAI.Relationship) engine.getCustomData().get("RC_BaseShipAI.Relationship" + s.getId());
						}
						relationship.enemyInMaxWeaponRange.clear();
						relationship.enemyInMinWeaponRange.clear();
						relationship.biggerList.clear();
						relationship.biggerEnemyList.clear();
						relationship.myFighterList.clear();
						relationship.other = null;
						relationship.beTargetCount = 0;
						relationship.maxWeaponRange = 0;
						relationship.minWeaponRange = 99999;
						relationship.minWingRange = 99999;

						relationship.nearestBiggerEnemy = null;
						relationship.nearestBiggerAlly = null;
						relationship.nearestShip = null;
						relationship.nearestShipHulk = null;
						relationship.nearestEnemyNotFighter = null;
						relationship.nearestAlly = null;
						relationship.nearestBiggerShip = null;
						float maxMissileRange = 0;
						for (WeaponAPI w : s.getAllWeapons()) {
							if (!w.isDisabled()) {
								if (w.usesAmmo() && w.getAmmo() > 0 || !w.usesAmmo()) {
									if (WeaponAPI.WeaponType.MISSILE.equals(w.getType())) {
										if (maxMissileRange < w.getRange()) {
											maxMissileRange = w.getRange();
										}
									} else {
										if (relationship.maxWeaponRange < w.getRange()) {
											relationship.maxWeaponRange = w.getRange();
										}
										if (relationship.minWeaponRange > w.getRange()) {
											relationship.minWeaponRange = w.getRange();
										}
									}
								}
							}
						}
						if (relationship.maxWeaponRange < relationship.minWeaponRange) {
							relationship.minWeaponRange = 0;
						}
						if (relationship.maxWeaponRange == 0) {
							relationship.maxWeaponRange = maxMissileRange;
						}
						for (ShipAPI s2 : engine.getShips()) {
							if (!s2.equals(s)) {
								//我的飞机
								if (!s2.isHulk() && s2.isAlive() && s2.getWing() != null) {
									if (s2.getWing().getSourceShip() != null) {
										if (s2.getWing().getSourceShip().equals(s)) {
											relationship.myFighterList.add(s2);
											if (s2.getWing().getRange() < relationship.minWingRange) {
												relationship.minWingRange = s2.getWing().getRange();
											}
										}
									}
								}
								//距离最近的的船包括hulk 用于闪避 获取other
								if (!s2.isFighter()) {
									if (relationship.nearestShipHulk == null) {
										relationship.nearestShipHulk = new RC_BaseShipAI.ShipAndDistance(s2, MathUtils.getDistance(s2, s));
									} else {
										relationship.nearestShipHulk = findNearestShip(relationship.nearestShipHulk, s2, s);
									}
									//大过自己的船 用于护盾相位
									if (s2.getHullSize().compareTo(s.getHullSize()) >= 0) {
										if (relationship.nearestBiggerShip == null) {
											relationship.nearestBiggerShip = new RC_BaseShipAI.ShipAndDistance(s2, MathUtils.getDistance(s2, s));
										} else {
											relationship.nearestBiggerShip = findNearestShip(relationship.nearestBiggerShip, s2, s);
										}
									}
								}
								//距离最近的船躲开爆炸
								if (!s2.isFighter() && !s2.isHulk()) {
									if (s2.getShipTarget() == s) {
										relationship.beTargetCount++;
									}
									if (relationship.nearestShip == null) {
										relationship.nearestShip = new RC_BaseShipAI.ShipAndDistance(s2, MathUtils.getDistance(s2, s));
									} else {
										relationship.nearestShip = findNearestShip(relationship.nearestShip, s2, s);
									}
									//用于获取other
									if (relationship.targetShip != null) {
										if (s2 != relationship.targetShip.ship && MathUtils.getDistance(s2, s) < relationship.targetShip.minDistance) {
											relationship.biggerList.add(s2);
										}
									}
								}
								//距离最近的队友
								if (s2.isAlive() && !s2.isFighter() && s2.getOwner() == s.getOwner()) {
									if (relationship.nearestAlly == null) {
										relationship.nearestAlly = new RC_BaseShipAI.ShipAndDistance(s2, MathUtils.getDistance(s2, s));
									} else {
										relationship.nearestAlly = findNearestShip(relationship.nearestAlly, s2, s);
									}
								}
								//距离最近且大于等于自己的队友
								if (s2.isAlive() && !s2.isFighter() && s2.getOwner() == s.getOwner() && s2.getHullSize().compareTo(s.getHullSize()) >= 0) {
									if (relationship.nearestBiggerAlly == null) {
										relationship.nearestBiggerAlly = new RC_BaseShipAI.ShipAndDistance(s2, MathUtils.getDistance(s2, s));
									} else {
										relationship.nearestBiggerAlly = findNearestShip(relationship.nearestBiggerAlly, s2, s);
									}
								}
								//距离最近的敌人
								if (s2.isAlive() && s2.getOwner() != s.getOwner() && s2.getOwner() != 100) {
									if (relationship.nearestEnemy == null) {
										relationship.nearestEnemy = new RC_BaseShipAI.ShipAndDistance(s2, MathUtils.getDistance(s2, s));
									} else {
										relationship.nearestEnemy = findNearestShip(relationship.nearestEnemy, s2, s);
									}

								}
								//距离最近的非飞机敌人
								if (s2.isAlive() && !s2.isFighter() && s2.getOwner() != s.getOwner() && s2.getOwner() != 100) {
									if (relationship.nearestEnemyNotFighter == null) {
										relationship.nearestEnemyNotFighter = new RC_BaseShipAI.ShipAndDistance(s2, MathUtils.getDistance(s2, s));
									} else {
										relationship.nearestEnemyNotFighter = findNearestShip(relationship.nearestEnemyNotFighter, s2, s);
									}
								}
								//距离最近且大于等于自己的敌人
								if (s2.isAlive() && !s2.isFighter() && s2.getOwner() != s.getOwner() && s2.getOwner() != 100 && s2.getHullSize().compareTo(s.getHullSize()) >= 0) {
									if (relationship.nearestBiggerEnemy == null) {
										relationship.nearestBiggerEnemy = new RC_BaseShipAI.ShipAndDistance(s2, MathUtils.getDistance(s2, s));
									} else {
										relationship.nearestBiggerEnemy = findNearestShip(relationship.nearestBiggerEnemy, s2, s);
									}
									relationship.biggerEnemyList.add(s2);
								}

								//最大射程范围内的船
								if (relationship.maxWeaponRange != 0) {
									if (MathUtils.getDistance(s2, s) <= relationship.maxWeaponRange && !s2.isFighter()) {
										if (s2.isAlive() && s2.getOwner() != s.getOwner() && s2.getOwner() != 100) {
											relationship.enemyInMaxWeaponRange.add(s2);
										}
									}
								}
								//最小射程范围内的船
								if (relationship.minWeaponRange != 0) {
									if (MathUtils.getDistance(s2, s) <= relationship.minWeaponRange && !s2.isFighter()) {
										if (s2.isAlive() && s2.getOwner() != s.getOwner() && s2.getOwner() != 100) {
											relationship.enemyInMinWeaponRange.add(s2);
										}
									}
								}
								//目标相同才有
								if (s.getShipTarget() == s2.getShipTarget() && !s2.isHulk() && s2.isAlive() && s2.getOwner() == s.getOwner()) {
									//两者存活
									if (s2.getHullSize().compareTo(s.getHullSize()) >= 0) {
										//屁股后面有船 并且 面对自己屁股
										float sToShip = VectorUtils.getAngle(s2.getLocation(), s.getLocation());
										if (Math.abs(MathUtils.getShortestRotation(sToShip, s2.getFacing())) < 45
												&& Math.abs(MathUtils.getShortestRotation(s2.getFacing(), s.getFacing())) < 30
										) {
											//屁股后面的队友 //还要是最近的
											if (relationship.back == null) {
												relationship.back = new RC_BaseShipAI.ShipAndDistance(s2, MathUtils.getDistance(s2, s));
											} else {
												relationship.back = findNearestShip(relationship.back, s2, s);
											}
											//屁股后面的船
										}
									}
								}
							}
						}
						engine.getCustomData().put("RC_BaseShipAI.Relationship" + s.getId(), relationship);
					}
					 */
				}
				if (s.getOwner() == 1 && !s.isFighter() && s != Global.getCombatEngine().getPlayerShip() && s.isAlive()) {
					CombatFleetManagerAPI manager = engine.getFleetManager(s.getOwner());
					CombatTaskManagerAPI task = manager.getTaskManager(false);
					CombatFleetManagerAPI.AssignmentInfo mission = task.getAssignmentFor(s);
					Vector2f targetLocation = null;
					boolean missionClear = false;
					if (mission != null) {
						if ((mission.getType() == CombatAssignmentType.CAPTURE) && s.getHullSize().compareTo(ShipAPI.HullSize.DESTROYER) <= 0) {
							targetLocation = mission.getTarget().getLocation();
							for (BattleObjectiveAPI o : engine.getObjectives()) {
								if (o.getOwner() == s.getOwner() && o.getLocation().equals(targetLocation)) {
									missionClear = true;
								}
							}
						} else if (s.isRetreating()) {

						} else {
							task.removeAssignment(mission);
							missionClear = true;
						}
					} else {
						missionClear = true;
					}
					if (missionClear) {
						if (s.getCustomData().get("RC_ShipAI") == null) {
							if (playerOP <= enemyOP) {
								if (aiSet.get(s.getHullSpec().getBaseHullId()) != null) {
									if ("base".equals(aiSet.get(s.getHullSpec().getBaseHullId()))) {
										s.setCustomData("RC_ShipAI", "base");
										s.setShipAI(new RC_BaseShipAI(s));

										enemyList.add(s);
									} else if ("frigate".equals(aiSet.get(s.getHullSpec().getBaseHullId()))) {
										s.setCustomData("RC_ShipAI", "frigate");
										s.setShipAI(new RC_FrigateAI(s));

										enemyList.add(s);
									} else if ("carrir_combat".equals(aiSet.get(s.getHullSpec().getBaseHullId()))) {
										s.setCustomData("RC_ShipAI", "carrir_combat");
										s.setShipAI(new RC_CarrirCombatAI(s));

										enemyList.add(s);
									} else if ("carrir".equals(aiSet.get(s.getHullSpec().getBaseHullId()))) {
										s.setCustomData("RC_ShipAI", "carrir");
										s.setShipAI(new RC_CarrirAI(s));

										enemyList.add(s);
									} else if ("onslaught".equals(aiSet.get(s.getHullSpec().getBaseHullId()))) {
										s.setCustomData("RC_ShipAI", "onslaught");
										s.setShipAI(new RC_OnslaughtAI(s));

										enemyList.add(s);
									} else if ("hyperion".equals(aiSet.get(s.getHullSpec().getBaseHullId()))) {
										s.setCustomData("RC_ShipAI", "hyperion");
										s.setShipAI(new RC_HyperionAI(s));

										enemyList.add(s);
									} else {
										s.setCustomData("RC_ShipAI", "base");
										s.setShipAI(new RC_BaseShipAI(s));

										enemyList.add(s);
									}
								}
							}
						} else {
							//
							if (playerOP > enemyOP) {
								enemyList.remove(s);
								s.removeCustomData("RC_ShipAI");
								s.resetDefaultAI();
							}
						}
					} else {
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
				engine.getCustomData().put(ID + "enemyList", enemyList);
			}
		//}
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

	public RC_BaseShipAI.ShipAndDistance findNearestShip(RC_BaseShipAI.ShipAndDistance shipAndDistance, ShipAPI s,ShipAPI ship) {
		float distance = MathUtils.getDistance(s, ship);
		if (shipAndDistance.minDistance>distance)
		{
			shipAndDistance.ship = s;
			shipAndDistance.minDistance = distance;
		}
		return shipAndDistance;
	}
}
