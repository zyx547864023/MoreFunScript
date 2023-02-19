package real_combat.combat;

import java.awt.Color;
import java.util.List;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.combat.MineStrikeStatsAIInfoProvider;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import real_combat.hullmods.CKEM_Hullmod;
import real_combat.util.RC_Util;

public class MineStrikePlusStats extends BaseShipSystemScript implements MineStrikeStatsAIInfoProvider {
	
	protected float MINE_RANGE = 0f;
	
	//public static final float MIN_SPAWN_DIST = 75f;
	public float MIN_SPAWN_DIST = 0f;
	public float MIN_SPAWN_DIST_FRIGATE = 110f;

	public float MIN_SHIP_DIST_FRIGATE = 50f;
	
	public float LIVE_TIME = 3f;
	
	public static final Color JITTER_COLOR = new Color(255,155,255,75);
	public static final Color JITTER_UNDER_COLOR = new Color(255,155,255,155);

	public ShipAPI ship;

	public MissileAPI mine;

	private Color TorpedoMarkerColorAlarm = Misc.getHighlightColor();
	private Color TorpedoMarkerColorDanger = Misc.getNegativeHighlightColor();
	
	public float getRange(ShipAPI ship) {
		if (ship == null) return MINE_RANGE;
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(MINE_RANGE);
	}
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		//boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}
		
		
		float jitterLevel = effectLevel;
		if (state == State.OUT) {
			jitterLevel *= jitterLevel;
		}
		float maxRangeBonus = 25f;
		float jitterRangeBonus = jitterLevel * maxRangeBonus;
		if (state == State.OUT) {
		}
		this.ship = ship;
		ship.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, 11, 0f, 3f + jitterRangeBonus);
		ship.setJitter(this, JITTER_COLOR, jitterLevel, 4, 0f, 0 + jitterRangeBonus);
		
		if (state == State.IN) {
		} else if (effectLevel >= 1) {
			Vector2f target = ship.getMouseTarget();
			if (ship.getShipAI() != null && ship.getAIFlags().hasFlag(AIFlags.SYSTEM_TARGET_COORDS)){
				target = (Vector2f) ship.getAIFlags().getCustom(AIFlags.SYSTEM_TARGET_COORDS);
			}
			if (target != null) {
				float dist = Misc.getDistance(ship.getLocation(), target);
				float max = getMaxRange(ship) + ship.getCollisionRadius();
				if (dist > max) {
					float dir = Misc.getAngleInDegrees(ship.getLocation(), target);
					target = Misc.getUnitVectorAtDegreeAngle(dir);
					target.scale(max);
					Vector2f.add(target, ship.getLocation(), target);
				}
				
				target = findClearLocation(ship, target);
				
				if (target != null) {
					spawnMine(ship, target);
				}
			}
			
		} else if (state == State.OUT ) {
		}
	}
	
	public void unapply(MutableShipStatsAPI stats, String id) {
	}
	
	public void spawnMine(ShipAPI source, Vector2f mineLoc) {
		CombatEngineAPI engine = Global.getCombatEngine();
		Vector2f currLoc = mineLoc;//Misc.getPointAtRadius(mineLoc, 30f + (float) Math.random() * 30f);
		//Vector2f currLoc = null;
		float start = (float) Math.random() * 360f;
		for (float angle = start; angle < start + 390; angle += 30f) {
			if (angle != start) {
				Vector2f loc = Misc.getUnitVectorAtDegreeAngle(angle);
				loc.scale(50f + (float) Math.random() * 30f);
				currLoc = Vector2f.add(mineLoc, loc, new Vector2f());
			}
			/*
			for (MissileAPI other : Global.getCombatEngine().getMissiles()) {
				if (!other.isMine()) continue;
				
				float dist = Misc.getDistance(currLoc, other.getLocation());
				if (dist < other.getCollisionRadius() + 40f) {
					currLoc = null;
					break;
				}
			}
			if (currLoc != null) {
				break;
			}
			 */
		}
		if (currLoc == null) {
			currLoc = Misc.getPointAtRadius(mineLoc, 30f + (float) Math.random() * 30f);
		}
		//(ship.getLocation().x,ship.getLocation().y,mineLoc.x,mineLoc.y);
		float projectileAngle = VectorUtils.getAngle(ship.getLocation(),mineLoc);
		//projectileAngle = projectileAngle + 90;
		//Vector2f currLoc = mineLoc;
		DamagingProjectileAPI projectile = (DamagingProjectileAPI)engine.spawnProjectile(source, null,
				"minestrikeplus",//minelayer2//minestrikeplus
				mineLoc,//currLoc
				projectileAngle, //(float) Math.random() * 360f
				null);
		MissileAPI mine = (MissileAPI) projectile;

		this.mine = mine;
		//mine.getDamage().setDamage();
		if (source != null) {
			//mine.getDamage().setDamage(1000);
			Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(
											source, WeaponType.MISSILE, false, mine.getDamage());
//			float extraDamageMult = source.getMutableStats().getMissileWeaponDamageMult().getModifiedValue();
//			mine.getDamage().setMultiplier(mine.getDamage().getMultiplier() * extraDamageMult);
		}
		
		
		float fadeInTime = 0.5f;
		mine.getVelocity().scale(0);
		mine.fadeOutThenIn(fadeInTime);
		
		Global.getCombatEngine().addPlugin(createMissileJitterPlugin(mine, fadeInTime));
		
		//mine.setFlightTime((float) Math.random());
		float liveTime = LIVE_TIME;
		//liveTime = 0.01f;
		mine.setFlightTime(mine.getMaxFlightTime() - liveTime);
		Global.getSoundPlayer().playSound("mine_teleport", 1f, 1f, mine.getLocation(), mine.getVelocity());
	}
	
	protected EveryFrameCombatPlugin createMissileJitterPlugin(final MissileAPI mine, final float fadeInTime) {
		return new BaseEveryFrameCombatPlugin() {
			float elapsed = 0f;
			float wait = 0f;
			Vector2f newLocation = new Vector2f();
			boolean isSetLocation = false;
			float shipjitterLevel = 0.1f;
			boolean isFizzling = false;
			boolean isShieldOn = false;

			@Override
			public void advance(float amount, List<InputEventAPI> events) {
				if (Global.getCombatEngine().isPaused()) return;
				if(mine.isFizzling())
				{
					isFizzling = true;
				}
				//每一帧都要画
				ViewportAPI viewport = Global.getCombatEngine().getViewport();
				float alphaMult = viewport.getAlphaMult();
				if(alphaMult > 0f) {
					//在目标点画一个圈
					GL11.glMatrixMode(GL11.GL_PROJECTION);
					GL11.glPushMatrix();
					GL11.glLoadIdentity();
					GL11.glOrtho(viewport.getLLX(), viewport.getLLX() + viewport.getVisibleWidth(), viewport.getLLY(),
							viewport.getLLY() + viewport.getVisibleHeight(), -1,
							1);
					GL11.glMatrixMode(GL11.GL_MODELVIEW);
					GL11.glPushMatrix();
					GL11.glLoadIdentity();
					GL11.glDisable(GL11.GL_TEXTURE_2D);
					GL11.glEnable(GL11.GL_LINE_SMOOTH);
					GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
					GL11.glEnable(GL11.GL_BLEND);
					GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
					GL11.glTranslatef(0.01f, 0.01f, 0);

					Color color = TorpedoMarkerColorAlarm;
					float alpha = alphaMult;

					//算出目标坐标
					Vector2f arc = new Vector2f(mine.getSpawnLocation().x,mine.getSpawnLocation().y);
					//drawArc(Misc.getDarkHighlightColor(), alpha, 360f, arc, mine.getCollisionRadius(), 0f, 0f, 0f, 0f, 2f / viewport.getViewMult());
					arc.x += mine.getVelocity().x*2;
					arc.y += mine.getVelocity().y*2;

					drawArc(color, alpha, 360f, arc, mine.getCollisionRadius(), 0f, 0f, 0f, 0f, 2f / viewport.getViewMult());

					GL11.glDisable(GL11.GL_BLEND);
					GL11.glMatrixMode(GL11.GL_MODELVIEW);
					GL11.glPopMatrix();
					GL11.glMatrixMode(GL11.GL_PROJECTION);
					GL11.glPopMatrix();
					GL11.glPopAttrib();
				}

				elapsed += amount;
				
				float jitterLevel = mine.getCurrentBaseAlpha();
				if (jitterLevel < 0.5f) {
					jitterLevel *= 2f;
				} else {
					jitterLevel = (1f - jitterLevel) * 2f;
				}
				
				float jitterRange = 1f - mine.getCurrentBaseAlpha();
				//jitterRange = (float) Math.sqrt(jitterRange);
				float maxRangeBonus = 50f;
				float jitterRangeBonus = jitterRange * maxRangeBonus;
				Color c = JITTER_UNDER_COLOR;
				c = Misc.setAlpha(c, 70);
				//mine.setJitter(this, c, jitterLevel, 15, jitterRangeBonus * 0.1f, jitterRangeBonus);
				mine.setJitter(this, c, jitterLevel, 15, jitterRangeBonus * 0, jitterRangeBonus);
				
				if (jitterLevel >= 1 || elapsed > fadeInTime) {

				}
				CombatEngineAPI engine = Global.getCombatEngine();
				if (!engine.isEntityInPlay(mine)) {
					if(!isSetLocation)
					{
						newLocation = new Vector2f(mine.getLocation().getX(), mine.getLocation().getY());
						isSetLocation = true;

					}
					//等待0.5f
					wait += amount;
					if(wait>=0.4f&&wait<0.41f){
						//避免直接装上去
						Vector2f target = mine.getLocation();
						if (ship.getShipAI() != null && ship.getAIFlags().hasFlag(AIFlags.SYSTEM_TARGET_COORDS)){
							target = (Vector2f) ship.getAIFlags().getCustom(AIFlags.SYSTEM_TARGET_COORDS);
						}
						if (target != null) {
							target = findClearTeleportLocation(ship, target);
							if (target != null) {
								ship.getLocation().set(target.x, target.y);
								ship.getVelocity().set(ship.getVelocity().x*0.1f, ship.getVelocity().y*0.1f);
								if(!isShieldOn)
								{
									isShieldOn = ship.getShield().isOn();
								}
								if(isShieldOn) {
									ship.getShield().toggleOff();
								}
								//最后抖一下
								if (shipjitterLevel < 0.5f&&shipjitterLevel>0) {
									shipjitterLevel *= 2f;
								} else if(shipjitterLevel <= 0){
									shipjitterLevel = 0.1f;
								} else {
									shipjitterLevel = (1f - shipjitterLevel) * 2f;
								}
								jitterRangeBonus = maxRangeBonus;
								ship.setJitterUnder(this, JITTER_UNDER_COLOR, shipjitterLevel, 11, 0, 3f + jitterRangeBonus);
								ship.setJitter(this, JITTER_COLOR, shipjitterLevel, 4, 0, 0 + jitterRangeBonus);
							}
						}
						//如果有target
						if(ship.getShipTarget()!=null) {
							//Double targetAngle = CKEM_Hullmod.calcAngle(ship.getLocation().x, ship.getLocation().y, ship.getShipTarget().getLocation().x, ship.getShipTarget().getLocation().y);
							float targetAngle = VectorUtils.getAngle(ship.getLocation(),ship.getShipTarget().getLocation());
							//targetAngle = targetAngle + 90;
							ship.setFacing(targetAngle);
						}
						//mine.getSpec().setLaunchSpeed(originSpead);
						Global.getSoundPlayer().playSound("mine_teleport", 1f, 1f, ship.getLocation(), ship.getVelocity());

					}
					else if(wait>=0.41f)
					{
						if(isShieldOn) {
							ship.getShield().toggleOn();
						}
						Global.getCombatEngine().removePlugin(this);
					}
				}
				if(mine.isExpired()) {
					if(!isFizzling) {
						Global.getCombatEngine().removePlugin(this);
					}
					if (shipjitterLevel < 0.5f&&shipjitterLevel>0) {
						shipjitterLevel *= 2f;
					} else if(shipjitterLevel <= 0){
						shipjitterLevel = 0.1f;
					} else {
						shipjitterLevel = (1f - shipjitterLevel) * 2f;
					}
					jitterRangeBonus = maxRangeBonus;
					ship.setJitterUnder(this, JITTER_UNDER_COLOR, shipjitterLevel, 11, 0, 3f + jitterRangeBonus);
					ship.setJitter(this, JITTER_COLOR, shipjitterLevel, 4, 0, 0 + jitterRangeBonus);
				}
				if (isFizzling)
				{
					if (shipjitterLevel < 0.5f&&shipjitterLevel>0) {
						shipjitterLevel *= 2f;
					} else if(shipjitterLevel <= 0){
						shipjitterLevel = 0.1f;
					} else {
						shipjitterLevel = (1f - shipjitterLevel) * 2f;
					}
					jitterRangeBonus = maxRangeBonus;
					ship.setJitterUnder(this, JITTER_UNDER_COLOR, shipjitterLevel, 11, 0, 3f + jitterRangeBonus);
					ship.setJitter(this, JITTER_COLOR, shipjitterLevel, 4, 0, 0 + jitterRangeBonus);
				}
			}
		};
	}

	private void drawArc(Color color, float alpha, float angle, Vector2f loc, float radius, float aimAngle, float aimAngleTop, float x, float y, float thickness){
		GL11.glLineWidth(thickness);
		GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte)Math.max(0, Math.min(Math.round(alpha * 255f), 255)) );
		GL11.glBegin(GL11.GL_LINE_STRIP);
		for(int i = 0; i < Math.round(angle); i++){
			GL11.glVertex2f(
					loc.x + (radius * (float)Math.cos(Math.toRadians(aimAngleTop + i)) + x * (float)Math.cos(Math.toRadians(aimAngle - 90f)) - y * (float)Math.sin(Math.toRadians(aimAngle - 90f))),
					loc.y + (radius * (float)Math.sin(Math.toRadians(aimAngleTop + i)) + x * (float)Math.sin(Math.toRadians(aimAngle - 90f)) + y * (float)Math.cos(Math.toRadians(aimAngle - 90f)))
			);
		}
		GL11.glEnd();
	}
	
	protected float getMaxRange(ShipAPI ship) {
		return getMineRange(ship);
	}

	
	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isOutOfAmmo()) return null;
		if (system.getState() != SystemState.IDLE) return null;
		
		Vector2f target = ship.getMouseTarget();
		if (target != null) {
			float dist = Misc.getDistance(ship.getLocation(), target);
			float max = getMaxRange(ship) + ship.getCollisionRadius();
			if (dist > max) {
				return "OUT OF RANGE";
			} else {
				return "READY";
			}
		}
		return null;
	}

	
	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		return ship.getMouseTarget() != null;
	}
	
	
	private Vector2f findClearLocation(ShipAPI ship, Vector2f dest) {
		if (isLocationClear(dest)) return dest;
		
		float incr = 0f;

		WeightedRandomPicker<Vector2f> tested = new WeightedRandomPicker<Vector2f>();
		for (float distIndex = 1; distIndex <= 32f; distIndex *= 2f) {
			float start = (float) Math.random() * 360f;
			for (float angle = start; angle < start + 360; angle += 60f) {
				Vector2f loc = Misc.getUnitVectorAtDegreeAngle(angle);
				loc.scale(incr * distIndex);
				Vector2f.add(dest, loc, loc);
				tested.add(loc);
				if (isLocationClear(loc)) {
					return loc;
				}
			}
		}
		
		if (tested.isEmpty()) return dest; // shouldn't happen
		
		return tested.pick();
	}

	private Vector2f findClearTeleportLocation(ShipAPI ship, Vector2f dest) {
		if (isShipLocationClear(dest)) return dest;

		float incr = 5f;

		WeightedRandomPicker<Vector2f> tested = new WeightedRandomPicker<Vector2f>();
		for (float distIndex = 1; distIndex <= 32f; distIndex *= 2f) {
			float start = (float) Math.random() * 360f;
			for (float angle = start; angle < start + 360; angle += 60f) {
				Vector2f loc = Misc.getUnitVectorAtDegreeAngle(angle);
				loc.scale(incr * distIndex);
				Vector2f.add(dest, loc, loc);
				tested.add(loc);
				if (isShipLocationClear(loc)) {
					return loc;
				}
			}
		}

		if (tested.isEmpty()) return dest; // shouldn't happen

		return tested.pick();
	}

	private boolean isLocationClear(Vector2f loc) {
		for (ShipAPI other : Global.getCombatEngine().getShips()) {
			if (other.isShuttlePod()) continue;
			if (other.isFighter()) continue;
			
//			Vector2f otherLoc = other.getLocation();
//			float otherR = other.getCollisionRadius();
			
//			if (other.isPiece()) {
//				System.out.println("ewfewfewfwe");
//			}
			Vector2f otherLoc = other.getShieldCenterEvenIfNoShield();
			float otherR = other.getShieldRadiusEvenIfNoShield();
			if (other.isPiece()) {
				otherLoc = other.getLocation();
				otherR = other.getCollisionRadius();
			}
			
			
//			float dist = Misc.getDistance(loc, other.getLocation());
//			float r = other.getCollisionRadius();
			float dist = Misc.getDistance(loc, otherLoc);
			float r = otherR;
			//r = Math.min(r, Misc.getTargetingRadius(loc, other, false) + r * 0.25f);
			float checkDist = MIN_SPAWN_DIST;
			if (other.isFrigate()) checkDist = MIN_SPAWN_DIST_FRIGATE;
			if (dist < r + checkDist) {
				return false;
			}
		}
		for (CombatEntityAPI other : Global.getCombatEngine().getAsteroids()) {
			float dist = Misc.getDistance(loc, other.getLocation());
			if (dist < other.getCollisionRadius() + MIN_SPAWN_DIST) {
				return false;
			}
		}
		
		return true;
	}

	private boolean isShipLocationClear(Vector2f loc) {
		for (ShipAPI other : Global.getCombatEngine().getShips()) {
			if (other.isShuttlePod()) continue;
			if (other.isFighter()) continue;

//			Vector2f otherLoc = other.getLocation();
//			float otherR = other.getCollisionRadius();

//			if (other.isPiece()) {
//				System.out.println("ewfewfewfwe");
//			}
			Vector2f otherLoc = other.getShieldCenterEvenIfNoShield();
			float otherR = other.getShieldRadiusEvenIfNoShield();
			if (other.isPiece()) {
				otherLoc = other.getLocation();
				otherR = other.getCollisionRadius();
			}


//			float dist = Misc.getDistance(loc, other.getLocation());
//			float r = other.getCollisionRadius();
			float dist = Misc.getDistance(loc, otherLoc);
			float r = otherR;
			//r = Math.min(r, Misc.getTargetingRadius(loc, other, false) + r * 0.25f);
			float checkDist = MIN_SPAWN_DIST;
			if (other.isFrigate()) checkDist = MIN_SPAWN_DIST_FRIGATE;
			if (dist < r + checkDist) {
				return false;
			}
		}
		for (CombatEntityAPI other : Global.getCombatEngine().getAsteroids()) {
			float dist = Misc.getDistance(loc, other.getLocation());
			if (dist < other.getCollisionRadius() + MIN_SPAWN_DIST) {
				return false;
			}
		}

		return true;
	}

	public float getFuseTime() {
		return 3f;
	}


	public float getMineRange(ShipAPI ship) {
		return getRange(ship);
		//return MINE_RANGE;
	}


	public float getAngle(Vector2f str,Vector2f target) {
		Double angle = new Double(0);
		double cos = new Double(0);
		if((str.y>target.y&&str.x<=target.x)||(str.y<=target.y&&str.x<=target.x))
		{
			cos =(str.y-target.y)/(Math.sqrt(Math.pow(target.y-str.y,2)+Math.pow(target.x-str.x,2)));
			angle = Math.acos((str.y-target.y)/(Math.sqrt(Math.pow(target.y-str.y,2)+Math.pow(target.x-str.x,2))));
		}
		else if((str.y>target.y&&str.x>target.x)||(str.y<=target.y&&str.x>target.x))
		{
			cos =(str.y-target.y)/(Math.sqrt(Math.pow(target.y-str.y,2)+Math.pow(target.x-str.x,2)));
			angle = -Math.acos((str.y-target.y)/(Math.sqrt(Math.pow(target.y-str.y,2)+Math.pow(target.x-str.x,2))));
		}
		return angle.floatValue();
	}
}








