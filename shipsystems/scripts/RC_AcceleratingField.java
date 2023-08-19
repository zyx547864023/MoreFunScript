package real_combat.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.hullmods.RC_SpiderCore;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * 每一个陨石有三个状态
 *
 * 脚抖动 获取最近的陨石移动到 船和模块夹角的位置前方 然后发射 移动速度和chargeup相关 发射速度1000 抖动结束
 *
 * 时间不冲突
 */

public class RC_AcceleratingField extends BaseShipSystemScript {
    private final static String ID = "RC_AcceleratingField";
    private final static String IS_ON = "IS_ON";
    private final static String WHO_CATCH = "WHO_CATCH";
    private final static String WHO_SHOOT = "WHO_SHOOT";
    private static float maxSpeed = 1000f;
    private ShipAPI ship;
    private ShipAPI motherShip;
    CombatEngineAPI engine = Global.getCombatEngine();
    private int count = 0;
    private boolean init = false;
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (engine == null) return;
        if (engine.isPaused()) {return;}
        ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        if (ship.getCustomData().get(RC_SpiderCore.ID)==null) {
            return;
        }
        else if(motherShip==null){
            motherShip = (ShipAPI) ship.getCustomData().get(RC_SpiderCore.ID);
        }
        try {
            if (!init) {
                init = true;
                count++;
                Global.getCombatEngine().addLayeredRenderingPlugin(new RC_AcceleratingFieldCombatPlugin(ship));
                Global.getLogger(this.getClass()).info("count" + count);
            }
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        if (engine == null) return;
        if(engine.isPaused()) {return;}
        if (ship == null) {
            return;
        }
        try {
            init = false;
            ship.setCustomData(ID+IS_ON,false);
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        return null;
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        //在范围内有陨石
        for (CombatEntityAPI a : engine.getAsteroids()) {
            float distance = MathUtils.getDistance(ship, a);
            Map<String, Object> customData = a.getCustomData();
            if (ship.getCustomData().get(RC_SpiderCore.ID)==null) {
                return false;
            }
            else if(motherShip==null){
                motherShip = (ShipAPI) ship.getCustomData().get(RC_SpiderCore.ID);
            }
            if (customData != null&&motherShip!=null) {
               if (customData.get(WHO_CATCH) != null&&customData.get(WHO_SHOOT) == null&&distance < ship.getCollisionRadius() * 1.5f) {
                   ShipAPI target = motherShip.getShipTarget();
                   if (target==null) {
                       float minDistance = motherShip.getCollisionRadius()*10f;
                       List<ShipAPI> enemyList = AIUtils.getNearbyEnemies(motherShip,minDistance);
                       for (ShipAPI e :enemyList) {
                           float newdistance = MathUtils.getDistance(motherShip,e);
                           if (distance<minDistance&&!e.isFighter()){
                               minDistance = newdistance;
                               target = e;
                           }
                       }
                       if (target==null) {
                           return false;
                       }
                   }
                   if (target.isFighter()) {
                       return false;
                   }
                   return true;
               }
            }
        }
        return false;
    }

    public class RC_AcceleratingFieldCombatPlugin extends BaseCombatLayeredRenderingPlugin {
        CombatEntityAPI proj = null;
        private boolean isShoot = false;
        private final ShipAPI ship;
        private CombatEngineAPI engine = Global.getCombatEngine();
        public RC_AcceleratingFieldCombatPlugin(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void advance(float amount) {
            if (engine == null) return;
            if (engine.isPaused()) {return;}
            if (!ship.isAlive()) {return;}

            if (!isShoot) {
                //获取最近的陨石
                float minDistance = ship.getCollisionRadius()*1.5f;
                for(CombatEntityAPI a:engine.getAsteroids())
                {
                    Map<String, Object> customData = a.getCustomData();
                    if (customData!=null) {
                        ShipAPI whoCatch = (ShipAPI) customData.get(WHO_CATCH);
                        ShipAPI whoShoot = (ShipAPI) customData.get(WHO_SHOOT);
                        if (whoCatch!=null&&whoShoot==null) {
                            float distance = MathUtils.getDistance(ship,a);
                            if (distance < minDistance && whoCatch.equals(ship.getCustomData().get(RC_SpiderCore.ID))) {
                                minDistance = distance;
                                proj = a;
                            }
                        }
                    }
                }
            }
            if (proj!=null&&!isShoot) {
                proj.removeCustomData("RC_AsteroidArmSTATUS");
                proj.setCollisionClass(CollisionClass.NONE);
                float distance = MathUtils.getDistance(ship,proj);
                if (distance>ship.getCollisionRadius()) {
                    proj.getVelocity().set(new Vector2f((ship.getLocation().x - proj.getLocation().x), (ship.getLocation().y - proj.getLocation().y)));
                }
                else {
                    Vector2f hulkLocation = proj.getLocation();
                    ShipAPI target = motherShip.getShipTarget();
                    if (target==null) {
                        float minDistance = motherShip.getCollisionRadius()*10f;
                        List<ShipAPI> enemyList = AIUtils.getNearbyEnemies(motherShip,minDistance);
                        for (ShipAPI e :enemyList) {
                            float newdistance = MathUtils.getDistance(motherShip,e);
                            if (distance<minDistance&&!e.isFighter()){
                                minDistance = newdistance;
                                target = e;
                            }
                        }
                        if (target==null) {
                            return;
                        }
                    }
                    Vector2f targetLocation = target.getLocation();
                    float shipToHulkAngle = VectorUtils.getAngle(hulkLocation, targetLocation);
                    //Global.getLogger(this.getClass()).info(ID+"||"+proj);
                    proj.setMass(proj.getMass()*3);
                    proj.getVelocity().set(MathUtils.getPoint(new Vector2f(0, 0), maxSpeed, shipToHulkAngle));
                    //proj.getCustomData().remove(WHO_CATCH);
                    proj.setCustomData(WHO_CATCH, ship);
                    proj.setCustomData(WHO_SHOOT, ship);

                    float maxRangeBonus = 25f;
                    ship.setJitterUnder(this, new Color(255, 165, 90, 55), 1, 11, 0f, 3f + maxRangeBonus);
                    ship.setJitter(this, new Color(255, 165, 90, 55), 1, 4, 0f, 0 + maxRangeBonus);
                    isShoot = true;
                }
            }
        }

        public void getOut(CombatEntityAPI s){

        }

        public void init(CombatEngineAPI engine) {
            count = 0;
            init = false;
        }

        @Override
        public float getRenderRadius() {
            return 1000000f;
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {

        }

        @Override
        public boolean isExpired() {
            //发射之后删除
            if (proj!=null&&motherShip!=null) {
                if (MathUtils.getDistance(motherShip, proj) > motherShip.getCollisionRadius()*0.25f) {
                    proj.setCollisionClass(CollisionClass.ASTEROID);
                    count--;
                    Global.getLogger(this.getClass()).info("count"+count);
                    return isShoot;
                }
            }
            return false;
        }
    }
}