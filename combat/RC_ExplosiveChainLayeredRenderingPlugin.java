package real_combat.combat;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.weapons.RC_ChainPlugin;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * 绕圈圈画绳子
 *
 * 在导弹段生成新的锁链
 * 往发射端拉
 *
 *
 */

public class RC_ExplosiveChainLayeredRenderingPlugin extends BaseCombatLayeredRenderingPlugin {
    private final static String ID = "RC_ExplosiveChainPlugin";
    private final static SpriteAPI SPRITE = Global.getSettings().getSprite("fx", "chain1");

    public RC_ExplosiveChainLayeredRenderingPlugin() {
    }

    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (!engine.isPaused()) {
            //没有命中到达最远距离收回
            List<RC_ChainPlugin.RC_Chain> chainList = new ArrayList<>();
            if (engine.getCustomData().get(ID) != null) {
                chainList = (List<RC_ChainPlugin.RC_Chain>) engine.getCustomData().get(ID);
            }
            List<RC_ChainPlugin.RC_Chain> removeList = new ArrayList<>();
            for (RC_ChainPlugin.RC_Chain c : chainList) {
                if (c.stage == RC_ChainPlugin.Stage.ON_HIT) {

                } else if (c.stage == RC_ChainPlugin.Stage.ON_FIRE) {
                    Vector2f lastLocation = null;
                    RC_ChainPlugin.RC_ChainOne last = null;
                    //头部移动其他跟着移动
                    for (RC_ChainPlugin.RC_ChainOne co : c.chainOneList) {
                        if (lastLocation == null) {
                            co.location = c.projectile.getLocation();
                            last = co;
                        } else {
                            //给其他部位速度
                            //距离超过一定距离才
                            if (MathUtils.getDistance(co.location, lastLocation) > SPRITE.getHeight() * 1 / 2) {
                                co.location = MathUtils.getPoint(co.location, c.projectile.getMoveSpeed() * amount / engine.getTimeMult().getModifiedValue(), VectorUtils.getAngle(co.location, lastLocation));
                            }
                        }
                        //排除飞机
                        //从外面进去rank+1
                        //co出去了，co之前的都要出去
                        /*
                        for (ShipAPI s: RC_BaseShipAI.getEnemiesOnMapNotFighter(c.projectile, new HashSet<ShipAPI>())) {
                            //
                            if (CollisionUtils.isPointWithinCollisionCircle(co.location, s)) {
                                if (!co.inOrOut) {
                                    co.rank+=1;
                                    co.inOrOut = true;
                                }
                            }
                            else {
                                if (co.inOrOut) {
                                    co.inOrOut = false;
                                }
                            }
                        }
                        */
                        lastLocation = co.location;
                    }
                    lastLocation = null;
                    last = null;
                    for (int index = c.chainOneList.size() - 1; index >= 0; index--) {
                        RC_ChainPlugin.RC_ChainOne co = c.chainOneList.get(index);
                        if (lastLocation == null) {
                            co.facing = c.projectile.getFacing();
                            last = co;
                        } else {
                            co.facing = VectorUtils.getAngle(co.location, lastLocation);
                        }
                        lastLocation = co.location;
                    }
                } else if (c.stage == RC_ChainPlugin.Stage.ON_HIT_WAIT) {

                }
                //基于质量差别选择
                //持续时间
                //持续距离
                else if (c.stage == RC_ChainPlugin.Stage.ON_PULL) {
                    //改为爆炸
                    if (c.chainOneList.size() > 0) {
                        //engine.spawnExplosion(c.chainOneList.get(c.chainOneList.size()-1).location,new Vector2f(0,0),  Color.ORANGE ,c.projectile.getCollisionRadius()*5,1f);
                        //engine.applyDamage(c.projectile.getSource(),c.chainOneList.get(c.chainOneList.size()-1).location,c.projectile.getDamageAmount(),DamageType.FRAGMENTATION,0,false,false,c.projectile.getSource(),true);
                        engine.spawnDamagingExplosion(createExplosionSpec(c.projectile.getCollisionRadius() * 2f, c.projectile.getCollisionRadius(), c.projectile.getDamage().getDamage()), c.projectile.getSource(), c.chainOneList.get(c.chainOneList.size() - 1).location, false);
                        c.chainOneList.remove(c.chainOneList.size() - 1);
                    }
                    if (c.chainOneList.size() > 0) {
                        //engine.spawnExplosion(c.chainOneList.get(0).location,new Vector2f(0,0),  Color.ORANGE ,c.projectile.getCollisionRadius()*5,1f);
                        //engine.applyDamage(c.projectile.getSource(),c.chainOneList.get(0).location,c.projectile.getDamageAmount(),DamageType.FRAGMENTATION,0,false,false,c.projectile.getSource(),true);
                        engine.spawnDamagingExplosion(createExplosionSpec(c.projectile.getCollisionRadius() * 2f, c.projectile.getCollisionRadius(), c.projectile.getDamage().getDamage()), c.projectile.getSource(), c.chainOneList.get(0).location, false);
                        c.chainOneList.remove(0);
                    }
                }

                if (c.stage == RC_ChainPlugin.Stage.ON_HIT) {
                    //开始放绳子
                    c.hitTime -= amount;
                    if (c.hitTime <= 0) {
                        c.stage = RC_ChainPlugin.Stage.ON_HIT_WAIT;
                        c.hitTime = 1;
                    }
                } else if (c.stage == RC_ChainPlugin.Stage.ON_HIT_WAIT) {
                    c.hitTime -= amount;
                    if (c.hitTime <= 0) {
                        c.stage = RC_ChainPlugin.Stage.ON_PULL;
                    }
                } else if (c.stage == RC_ChainPlugin.Stage.ON_FIRE) {
                    if ((c.projectile.isFading() && !c.projectile.didDamage()) || c.projectile.isExpired()) {
                        c.stage = RC_ChainPlugin.Stage.ON_HIT_WAIT;
                        c.hitTime = 1;
                    } else {
                        if (c.chainOneList.size() > 0) {
                            if (MathUtils.getDistance(c.chainOneList.get(c.chainOneList.size() - 1).location, c.weapon.getLocation()) > SPRITE.getHeight() * 1 / 2) {
                                if (c.chainOneList.size() % 2 == 0) {
                                    c.chainOneList.add(new RC_ChainPlugin.RC_ChainOne(c.weapon.getLocation(), c.weapon.getCurrAngle(), Global.getSettings().getSprite("fx", "chain1")));
                                } else {
                                    c.chainOneList.add(new RC_ChainPlugin.RC_ChainOne(c.weapon.getLocation(), c.weapon.getCurrAngle(), Global.getSettings().getSprite("fx", "chain2")));
                                }
                            }
                        } else {
                            c.chainOneList.add(new RC_ChainPlugin.RC_ChainOne(c.weapon.getLocation(), c.weapon.getCurrAngle(), Global.getSettings().getSprite("fx", "chain1")));
                        }
                    }
                } else if (c.stage == RC_ChainPlugin.Stage.ON_PULL) {
                    if (c.chainOneList.size() > 0) {

                    } else {
                        removeList.add(c);
                    }
                }
            }
            chainList.removeAll(removeList);
            engine.getCustomData().put(ID, chainList);
        }
    }

    public void getOut(CombatEntityAPI s) {

    }

    public void init(CombatEngineAPI engine) {

    }

    @Override
    public float getRenderRadius() {
        return 1000000f;
    }

    /**
     * 用magic控制渲染上下
     * @return
     */
    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER,CombatEngineLayers.UNDER_SHIPS_LAYER);
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine==null) return;
        List<RC_ChainPlugin.RC_Chain> chainList = new ArrayList<>();
        if (engine.getCustomData().get(ID) != null) {
            chainList = (List<RC_ChainPlugin.RC_Chain>) engine.getCustomData().get(ID);
        }
        for (RC_ChainPlugin.RC_Chain c : chainList) {
            int count = 0;
            for (RC_ChainPlugin.RC_ChainOne co : c.chainOneList) {
                if (count == 0 || count == c.chainOneList.size() - 1) {
                    count++;
                    continue;
                }
                //渲染
                if (count % 2 == 0) {
                    SpriteAPI sprite = co.sprite;
                    sprite.setAngle(co.facing - 90);
                    //sprite.renderAtCenter(co.location.x, co.location.y);
                    if (co.rank%2==0) {
                        //sprite.setColor(Color.RED);
                        sprite.renderAtCenter(co.location.x, co.location.y);
                    }
                    else {
                        if (engine.isPaused()) {
                            if (!co.isDraw) {
                                //sprite.setColor(Color.GREEN);
                                org.magiclib.plugins.MagicRenderPlugin.addSingleframe(sprite, co.location, CombatEngineLayers.UNDER_SHIPS_LAYER);
                                co.isDraw = true;
                            }
                        } else {
                            //sprite.setColor(Color.GREEN);
                            org.magiclib.plugins.MagicRenderPlugin.addSingleframe(sprite, co.location, CombatEngineLayers.UNDER_SHIPS_LAYER);
                            co.isDraw = false;
                        }
                    }
                }
                count++;
            }
            count = 0;
            for (RC_ChainPlugin.RC_ChainOne co : c.chainOneList) {
                if (count == 0 || count == c.chainOneList.size() - 1) {
                    count++;
                    continue;
                }
                //渲染
                if (count % 2 != 0) {
                    SpriteAPI sprite = co.sprite;
                    sprite.setAngle(co.facing - 90);
                    //sprite.renderAtCenter(co.location.x, co.location.y);
                    if (co.rank%2==0) {
                        //sprite.setColor(Color.RED);
                        sprite.renderAtCenter(co.location.x, co.location.y);
                    }
                    else {
                        if (engine.isPaused()) {
                            if (!co.isDraw) {
                                //sprite.setColor(Color.GREEN);
                                org.magiclib.plugins.MagicRenderPlugin.addSingleframe(sprite, co.location, CombatEngineLayers.UNDER_SHIPS_LAYER);
                                co.isDraw = true;
                            }
                        } else {
                            //sprite.setColor(Color.GREEN);
                            org.magiclib.plugins.MagicRenderPlugin.addSingleframe(sprite, co.location, CombatEngineLayers.UNDER_SHIPS_LAYER);
                            co.isDraw = false;
                        }
                    }
                }
                count++;
            }
            SpriteAPI rocketSprite = Global.getSettings().getSprite("graphics/missiles/missile_salamander.png");
            if (c.anchoredEntity != null && c.stage != RC_ChainPlugin.Stage.ON_PULL) {
                float targetFace = c.anchoredEntity.getAnchor().getFacing();
                float relativeAngle = c.anchoredEntity.getRelativeAngle();
                rocketSprite.setAngle(relativeAngle + targetFace);
                rocketSprite.renderAtCenter(c.anchoredEntity.getLocation().x, c.anchoredEntity.getLocation().y);
            } else {

            }
            if (c.chainOneList.size() > 0 && c.stage == RC_ChainPlugin.Stage.ON_PULL) {
                rocketSprite.setAngle(c.chainOneList.get(0).facing + 90);
                rocketSprite.renderAtCenter(c.chainOneList.get(0).location.x, c.chainOneList.get(0).location.y);
            }
        }
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    public DamagingExplosionSpec createExplosionSpec(float radius, float coreRadius, float damage) {
        DamagingExplosionSpec spec = new DamagingExplosionSpec(
                0.1f, // duration
                radius, // radius
                coreRadius, // coreRadius
                damage, // maxDamage
                damage / 2f, // minDamage
                CollisionClass.PROJECTILE_FF, // collisionClass
                CollisionClass.PROJECTILE_FIGHTER, // collisionClassByFighter
                1f, // particleSizeMin
                1f, // particleSizeRange
                0.5f, // particleDuration
                50, // particleCount
                new Color(255, 255, 255, 255), // particleColor
                new Color(252, 199, 0, 255)  // explosionColor
        );

        spec.setDamageType(DamageType.FRAGMENTATION);
        spec.setUseDetailedExplosion(false);
        spec.setSoundSetId("explosion_guardian");
        return spec;
    }
    
    private static final EnumSet<CollisionClass> ALLOWED_COLLISIONS
            = EnumSet.of(CollisionClass.ASTEROID, CollisionClass.FIGHTER, CollisionClass.MISSILE_FF, CollisionClass.MISSILE_NO_FF, CollisionClass.SHIP);

    public boolean checkCollections(Vector2f location,float radius) {
        List<CombatEntityAPI> entitiesToCheck = CombatUtils.getEntitiesWithinRange(location, radius);
        Iterator<CombatEntityAPI> iter = entitiesToCheck.iterator();
        while (iter.hasNext()) {
            //排除友军
            //排除飞机
            //排除子弹
        }
        Collections.sort(entitiesToCheck, new CollectionUtils.SortEntitiesByDistance(location));
        iter = entitiesToCheck.iterator();

        while (iter.hasNext()) {
            CombatEntityAPI entity = iter.next();
            if (CollisionUtils.getCollides(location, location, entity.getLocation(), entity.getCollisionRadius())) {
                if (CollisionUtils.isPointWithinCollisionCircle(location, entity)) {
                    if (CollisionUtils.isPointWithinBounds(location, entity)) {

                        break;
                    }
                }
                if (entity instanceof ShipAPI) {
                    /* Shield (near side) */
                    ShipAPI ship = (ShipAPI) entity;
                    if ((ship.getShield() != null) && ship.getShield().isOn()) {

                    }
                    Vector2f collision = CollisionUtils.getCollisionPoint(location, location, entity);
                    if (collision != null) {

                        break;
                    }
                    /* Shield (back side) */
                    if ((ship.getShield() != null) && ship.getShield().isOn()) {

                    }
                } else {

                }
            }
        }
        return false;
    }
}