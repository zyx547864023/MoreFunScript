package real_combat.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.BoundsAPI.SegmentAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.util.RC_Util;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Colors are:
 * beam fringe color, for beam fringe and emp arcs
 * beam glow color (beam weapon glow)
 * mine glow color (border around core of explosion, also pings?)
 * mine ping color (should be same as glow color)
 * explosion undercolor (specified in code only)
 * color subtracted around source of beam (code only)
 */
public class RC_BeamCrossEffect implements BeamEffectPlugin { //WithReset {
	public static Color NEGATIVE_SOURCE_COLOR = new Color(200,255,200,25);
	public static String RIFTCASCADE_MINELAYER = "riftcascade_minelayer";
	public static String PHASEBEAM_SUN = "phasebeam_sun";
	public static String PHASEBEAM_ICE = "phasebeam_ice";
	public static float SPAWN_INTERVAL = 0.15f;
	protected Vector2f prevMineLoc = null;
	protected float spawned = 0;
	protected float untilNextSpawn = 0;

	protected IntervalUtil tracker = new IntervalUtil(0.1f, 0.2f);

	protected Vector2f arcFrom = null;
	public static float SPAWN_SPACING = 175f;
	protected float spawnDir = 0;
	protected boolean doneSpawningMines = false;
	protected int numToSpawn = 0;
	public static int MAX_RIFTS = 5;
	public static float UNUSED_RANGE_PER_SPAWN = 120f;//80f
	public static float MIN_CORE_WIDTH = 20f;
	public RC_BeamCrossEffect() {

	}


	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		tracker.advance(amount);
		if (tracker.intervalElapsed()) {
			spawnNegativeParticles(engine, beam);
		}

		if (beam.getBrightness() < 1f) return;

		if (doneSpawningMines) return;

		if (numToSpawn <= 0 && beam.getDamageTarget() != null) {
			float range = beam.getWeapon().getRange();
			float length = beam.getLengthPrevFrame();
			//float perSpawn = range / NUM_SPAWNS;
			numToSpawn = (int) ((range - length) / UNUSED_RANGE_PER_SPAWN) + 1;
			if (numToSpawn > MAX_RIFTS) {
				numToSpawn = MAX_RIFTS;
			}
			untilNextSpawn = 0f;
		}
		else {

		}

		untilNextSpawn -= amount;
		if (untilNextSpawn > 0) return;

		ShipAPI ship = beam.getSource();
		//如果和另一个光束相交
		List<BeamAPI> all = Global.getCombatEngine().getBeams();
		WeaponAPI thisWeapon = beam.getWeapon();
		String thisWeaponId = thisWeapon.getSpec().getWeaponId();
		boolean spawnedMine = false;
		if(PHASEBEAM_SUN.equals(thisWeaponId)){
			for(BeamAPI b:all)
			{
				if(b.getWeapon()!=null&&!b.equals(beam))
				{
					String thatWeaponId = b.getWeapon().getSpec().getWeaponId();
					//if((PHASEBEAM_SUN.equals(thatWeaponId)&&PHASEBEAM_ICE.equals(thisWeaponId))
					//||(PHASEBEAM_ICE.equals(thatWeaponId)&&PHASEBEAM_SUN.equals(thisWeaponId)))
					if(PHASEBEAM_ICE.equals(thatWeaponId))
					{
						Line2D line1 = new Line2D.Float(beam.getFrom().x, beam.getFrom().y, beam.getTo().x, beam.getTo().y);
						Line2D line2 = new Line2D.Float(b.getFrom().x, b.getFrom().y, b.getTo().x, b.getTo().y);
						boolean result = line2.intersectsLine(line1);
						if(result) {
							Point2D point = RC_Util.getIntersectPointBy2Line(line1, line2);
							if (point==null) {
								return;
							}
							Vector2f loc = new Vector2f(new Double(point.getX()).floatValue(), new Double(point.getY()).floatValue());
							if (beam.getDamageTarget() != null) {
								float perSpawn = SPAWN_SPACING;
								Vector2f arcTo = getNextArcLoc(engine, beam, perSpawn, loc);
								float thickness = beam.getWidth();
								EmpArcEntityAPI arc_sun = engine.spawnEmpArcVisual(arcFrom, null, arcTo, null, thickness, beam.getFringeColor(), Color.white);
								arc_sun.setCoreWidthOverride(Math.max(MIN_CORE_WIDTH, thickness * 0.67f));
								EmpArcEntityAPI arc_ice = engine.spawnEmpArcVisual(arcFrom, null, arcTo, null, thickness, b.getFringeColor(), Color.white);
								arc_ice.setCoreWidthOverride(Math.max(MIN_CORE_WIDTH, thickness * 0.67f));
								if (!arcTo.equals(arcFrom)) {
									spawnMine(ship, arcTo);
									spawnedMine = true;
								}
								arcFrom = arcTo;
							}
							else if (b.getDamageTarget() != null)
							{
								if (numToSpawn <= 0 && b.getDamageTarget() != null) {
									float range = b.getWeapon().getRange();
									float length = b.getLengthPrevFrame();
									//float perSpawn = range / NUM_SPAWNS;
									numToSpawn = (int) ((range - length) / UNUSED_RANGE_PER_SPAWN) + 1;
									if (numToSpawn > MAX_RIFTS) {
										numToSpawn = MAX_RIFTS;
									}
									untilNextSpawn = 0f;
								}
								float perSpawn = SPAWN_SPACING;
								Vector2f arcTo = getNextArcLoc(engine, b, perSpawn, loc);
								float thickness = beam.getWidth();
								EmpArcEntityAPI arc_sun = engine.spawnEmpArcVisual(arcFrom, null, arcTo, null, thickness, beam.getFringeColor(), Color.white);
								arc_sun.setCoreWidthOverride(Math.max(MIN_CORE_WIDTH, thickness * 0.67f));
								EmpArcEntityAPI arc_ice = engine.spawnEmpArcVisual(arcFrom, null, arcTo, null, thickness, b.getFringeColor(), Color.white);
								arc_ice.setCoreWidthOverride(Math.max(MIN_CORE_WIDTH, thickness * 0.67f));
								if (!arcTo.equals(arcFrom)) {
									spawnMine(ship, arcTo);
									spawnedMine = true;
								}
								arcFrom = arcTo;
							}
							else
							{
								float thickness = beam.getWidth();
								Vector2f arcTo = MathUtils.getRandomPointInCircle(loc,UNUSED_RANGE_PER_SPAWN);
								EmpArcEntityAPI arc_sun = engine.spawnEmpArcVisual(loc, null, arcTo, null, thickness, beam.getFringeColor(), Color.white);
								arc_sun.setCoreWidthOverride(Math.max(MIN_CORE_WIDTH, thickness * 0.67f));
								EmpArcEntityAPI arc_ice = engine.spawnEmpArcVisual(loc, null, arcTo, null, thickness, b.getFringeColor(), Color.white);
								arc_ice.setCoreWidthOverride(Math.max(MIN_CORE_WIDTH, thickness * 0.67f));
								spawnMine(ship, arcTo);
							}
						}
					}
				}
			}
		}
		untilNextSpawn = SPAWN_INTERVAL;

		if (spawnedMine) {
			spawned++;
			if (spawned >= numToSpawn) {
				doneSpawningMines = true;
			}
		}
	}

	public void spawnNegativeParticles(CombatEngineAPI engine, BeamAPI beam) {
		float length = beam.getLengthPrevFrame();
		if (length <= 10f) return;

		Vector2f from = beam.getFrom();
		Vector2f to = beam.getRayEndPrevFrame();

		ShipAPI ship = beam.getSource();

		float angle = Misc.getAngleInDegrees(from, to);
		Vector2f dir = Misc.getUnitVectorAtDegreeAngle(angle);

		Color color = NEGATIVE_SOURCE_COLOR;

		float sizeMult = 1f;
		sizeMult = 0.67f;

		for (int i = 0; i < 3; i++) {
			float rampUp = 0.25f + 0.25f * (float) Math.random();
			float dur = 1f + 1f * (float) Math.random();
			float size = 200f + 50f * (float) Math.random();
			size *= sizeMult;
			Vector2f loc = Misc.getPointAtRadius(beam.getWeapon().getLocation(), size * 0.33f);
			engine.addNegativeParticle(loc, ship.getVelocity(), size, rampUp / dur, dur, color);
		}

		if (true) return;

		// particles along the beam
		float spawnOtherParticleRange = 100;
		if (length > spawnOtherParticleRange * 2f && (float) Math.random() < 0.25f) {
			//color = new Color(150,255,150,255);
			color = new Color(150,255,150,75);
			int numToSpawn = (int) ((length - spawnOtherParticleRange) / 200f + 1);
			numToSpawn = 1;
			for (int i = 0; i < numToSpawn; i++) {
				float distAlongBeam = spawnOtherParticleRange + (length - spawnOtherParticleRange * 2f) * (float) Math.random();
				float groupSpeed = 100f + (float) Math.random() * 100f;
				for (int j = 0; j < 7; j++) {
					float rampUp = 0.25f + 0.25f * (float) Math.random();
					float dur = 1f + 1f * (float) Math.random();
					float size = 50f + 50f * (float) Math.random();
					Vector2f loc = new Vector2f(dir);
					float sign = Math.signum((float) Math.random() - 0.5f);
					loc.scale(distAlongBeam + sign * (float) Math.random() * size * 0.5f);
					Vector2f.add(loc, from, loc);
					loc = Misc.getPointWithinRadius(loc, size * 0.25f);

					float dist = Misc.getDistance(loc, to);
					Vector2f vel = new Vector2f(dir);
					if ((float) Math.random() < 0.5f) {
						vel.negate();
						dist = Misc.getDistance(loc, from);
					}

					float speed = groupSpeed;
					float maxSpeed = dist / dur;
					if (speed > maxSpeed) speed = maxSpeed;
					vel.scale(speed);
					Vector2f.add(vel, ship.getVelocity(), vel);

					engine.addNegativeParticle(loc, vel, size, rampUp, dur, color);
				}
			}
		}
	}

	public float getSizeMult() {
		float sizeMult = 1f - spawned / (float) Math.max(1, numToSpawn - 1);
		sizeMult = 0.75f + (1f - sizeMult) * 0.5f;

		return sizeMult;
	}

	public void spawnMine(ShipAPI source, Vector2f mineLoc) {
		CombatEngineAPI engine = Global.getCombatEngine();
		MissileAPI mine = (MissileAPI) engine.spawnProjectile(source, null,
															  RIFTCASCADE_MINELAYER,
															  mineLoc,
															  (float) Math.random() * 360f, null);


		float sizeMult = getSizeMult();
		mine.setCustomData(RiftCascadeMineExplosion.SIZE_MULT_KEY, sizeMult);

		if (source != null) {
			Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(
											source, WeaponType.MISSILE, false, mine.getDamage());
		}

		mine.getDamage().getModifier().modifyMult("mine_sizeMult", sizeMult);


		float fadeInTime = 0.05f;
		mine.getVelocity().scale(0);
		mine.fadeOutThenIn(fadeInTime);

		float liveTime = 0f;
		mine.setFlightTime(mine.getMaxFlightTime() - liveTime);
		mine.addDamagedAlready(source);
		mine.setNoMineFFConcerns(true);

		prevMineLoc = mineLoc;
	}

	public Vector2f getNextArcLoc(CombatEngineAPI engine, BeamAPI beam, float perSpawn, Vector2f loc) {
		CombatEntityAPI target = beam.getDamageTarget();
		float radiusOverride = -1f;
		if (target instanceof ShipAPI) {
			ShipAPI ship = (ShipAPI) target;
			if (ship.getParentStation() != null && ship.getStationSlot() != null) {
				radiusOverride = Misc.getDistance(beam.getRayEndPrevFrame(), ship.getParentStation().getLocation()) + 0f;
				target = ship.getParentStation();
			}
		}

		if (arcFrom == null) {
			arcFrom = new Vector2f(loc);

			float beamAngle = Misc.getAngleInDegrees(beam.getFrom(), beam.getRayEndPrevFrame());
			float beamSourceToTarget = Misc.getAngleInDegrees(beam.getFrom(), target.getLocation());

			// this is the direction we'll rotate - from the target's center - so that it's spawning mines around the side
			// closer to the beam's straight line
			spawnDir = Misc.getClosestTurnDirection(beamAngle, beamSourceToTarget);
			if (spawnDir == 0) spawnDir = 1;

			boolean computeNextLoc = false;
			if (prevMineLoc != null) {
				float dist = Misc.getDistance(arcFrom, prevMineLoc);
				if (dist < perSpawn) {
					perSpawn -= dist;
					computeNextLoc = true;
				}
			}
			if (!computeNextLoc) {
				return arcFrom;
			}
		}


		Vector2f targetLoc = target.getLocation();
		float targetRadius = target.getCollisionRadius();
		if (radiusOverride >= 0) {
			targetRadius = radiusOverride;
		}


		boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getRayEndPrevFrame());
		if (hitShield) perSpawn *= 0.67f;

//		// this is the direction we'll rotate - from the target's center - so that it's spawning mines around the side
//		// closer to the beam's straight line

		float prevAngle = Misc.getAngleInDegrees(targetLoc, arcFrom);
		float anglePerSegment = 360f * perSpawn / (3.14f * 2f * targetRadius);
		if (anglePerSegment > 90f) anglePerSegment = 90f;
		float angle = prevAngle + anglePerSegment * spawnDir;


		Vector2f arcTo = Misc.getUnitVectorAtDegreeAngle(angle);
		arcTo.scale(targetRadius);
		Vector2f.add(targetLoc, arcTo, arcTo);

		float actualRadius = Global.getSettings().getTargetingRadius(arcTo, target, hitShield);
		if (radiusOverride >= 0) {
			actualRadius = radiusOverride;
		}
		if (!hitShield) {
			actualRadius += 30f + 50f * (float) Math.random();
		} else {
			actualRadius += 30f + 50f * (float) Math.random();
		}

		arcTo = Misc.getUnitVectorAtDegreeAngle(angle);
		arcTo.scale(actualRadius);
		Vector2f.add(targetLoc, arcTo, arcTo);


		// now we've got an arcTo location somewhere roughly circular; try to cleave more closely to the hull
		// if the target is a ship
		if (target instanceof ShipAPI && !hitShield) {
			ShipAPI ship = (ShipAPI) target;
			BoundsAPI bounds = ship.getExactBounds();
			if (bounds != null) {
				Vector2f best = null;
				float bestDist = Float.MAX_VALUE;
				for (SegmentAPI segment : bounds.getSegments()) {
					float test = Misc.getDistance(segment.getP1(), arcTo);
					if (test < bestDist) {
						bestDist = test;
						best = segment.getP1();
					}
				}
				if (best != null) {
					Object o = Global.getSettings().getWeaponSpec(RIFTCASCADE_MINELAYER).getProjectileSpec();
					if (o instanceof MissileSpecAPI) {
						MissileSpecAPI spec = (MissileSpecAPI) o;
						float explosionRadius = (float) spec.getBehaviorJSON().optJSONObject("explosionSpec").optDouble("coreRadius");
						float sizeMult = getSizeMult();
						explosionRadius *= sizeMult;

						Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(best, arcTo));
						dir.scale(explosionRadius * 0.9f);
						Vector2f.add(best, dir, dir);
						arcTo = dir;

					}
				}
			}

		}

		return arcTo;
	}
}





