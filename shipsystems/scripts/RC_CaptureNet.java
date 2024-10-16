package real_combat.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;
import real_combat.entity.RC_AnchoredEntity;
import real_combat.hullmods.RC_SpiderCore;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 *  开关
 *  震动
 *  如果前面有船
 *  锁定目标或者最近的船
 *  发射两根电弧
 *  敌方无护盾或护盾<捕获阶段
 *  EMP伤害
 *  结网
 *  目标移动速度降低转向速度降低
 */

public class RC_CaptureNet extends BaseShipSystemScript {
    private final static String ID = "RC_CaptureNet";
    private ShipAPI ship;
    CombatEngineAPI engine = Global.getCombatEngine();
    private boolean init = false;
    private final static String IS_ON = "IS_ON";
    private final static String IS_SET = "IS_SET";
    private final static String WHO_CATCH = "WHO_CATCH";
    private final static String STATUS = "STATUS";
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (engine == null) return;
        if (engine.isPaused()) {return;}
        ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        try {
            ship.setCustomData(ID+IS_ON,true);
            if (!init) {
                init = true;
                Global.getCombatEngine().addLayeredRenderingPlugin(new RC_CaptureNetCombatPlugin(ship));
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
        return true;
    }

    public class RC_CaptureNetCombatPlugin extends BaseCombatLayeredRenderingPlugin {
        protected IntervalUtil tracker = new IntervalUtil(0.1f, 0.1f);
        protected IntervalUtil targetTracker = new IntervalUtil(0.3f, 0.3f);
        private final Color FRINGE = new Color(138, 0, 0, 255);
        private final Color CORE =  new Color(0,0,0, 255);
        private final static float THICKNESS = 20f;
        private final static float DISTANCE = 11f;
        private final ShipAPI ship;
        private ShipAPI target;
        private CaptureNet captureNet;
        private CombatEngineAPI engine = Global.getCombatEngine();
        public RC_CaptureNetCombatPlugin(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void advance(float amount) {
            if (engine == null) return;
            if (engine.isPaused()) {return;}
            if (!ship.isAlive()) {return;}

            if (ship.getCustomData().get(ID+IS_ON)==null) {return;}
            //如果启动了
            if ((boolean) ship.getCustomData().get(ID+IS_ON)) {
                float maxRangeBonus = 25f;
                ship.setJitterUnder(this, new Color(138, 0, 0, 55), 1, 11, 0f, 3f + maxRangeBonus);
                ship.setJitter(this, new Color(138, 0, 0, 55), 1, 4, 0f, 0 + maxRangeBonus);
                //如果没有设置
                if (target==null) {
                    //寻找目标
                    //母船目标
                    ShipAPI motherShip = (ShipAPI) ship.getCustomData().get(RC_SpiderCore.ID);
                    //ShipAPI motherShip = ship.getParentStation();
                    if (motherShip!=null) {
                        if (motherShip.getShipTarget()!=null) {
                            target = motherShip.getShipTarget();
                            float shipToTarget = VectorUtils.getAngle(ship.getLocation(),target.getLocation());
                            float shipFacing = ship.getFacing();
                            float distance = MathUtils.getDistance(ship,target);
                            if (Math.abs(MathUtils.getShortestRotation(shipFacing,shipToTarget))>30||distance>1500F||!target.isAlive()) {
                                //要清空被谁抓住了
                                target = null;
                            }
                            if (target!=null) {
                                if (target.getCustomData().get(ID + WHO_CATCH) != null) {
                                    target = null;
                                }
                            }
                        }
                    }
                    if (target==null) {
                        target = ship.getShipTarget();
                        if (target!=null) {
                            float shipToTarget = VectorUtils.getAngle(ship.getLocation(), target.getLocation());
                            float shipFacing = ship.getFacing();
                            float distance = MathUtils.getDistance(ship, target);
                            if (Math.abs(MathUtils.getShortestRotation(shipFacing, shipToTarget)) > 30 || distance > 1500F ||!target.isAlive()) {
                                //要清空被谁抓住了
                                target = null;
                            }
                            if (target!=null) {
                                if (target.getCustomData().get(ID + WHO_CATCH) != null) {
                                    target = null;
                                }
                            }
                        }
                    }

                    if (target==null) {
                        target = AIUtils.getNearestEnemy(ship);
                        if (target!=null) {
                            float shipToTarget = VectorUtils.getAngle(ship.getLocation(), target.getLocation());
                            float shipFacing = ship.getFacing();
                            float distance = MathUtils.getDistance(ship, target);
                            if (Math.abs(MathUtils.getShortestRotation(shipFacing, shipToTarget)) > 30 || distance > 1500F||!target.isAlive()) {
                                //要清空被谁抓住了
                                target = null;
                            }
                            if (target!=null) {
                                if (target.getCustomData().get(ID + WHO_CATCH) != null) {
                                    target = null;
                                }
                            }
                        }
                    }
                }
                //已设置则执行效果
                else {
                    float shipToTarget = VectorUtils.getAngle(ship.getLocation(),target.getLocation());
                    float shipFacing = ship.getFacing();
                    float distance = MathUtils.getDistance(ship,target);
                    if (Math.abs(MathUtils.getShortestRotation(shipFacing,shipToTarget))>30||distance>1500F||!target.isAlive()) {
                        //要清空被谁抓住了
                        captureNet = null;
                        target.getMutableStats().getMaxSpeed().unmodify(ID);
                        target.getMutableStats().getMaxTurnRate().unmodify(ID);
                        target.getMutableStats().getAcceleration().unmodify(ID);
                        target.getMutableStats().getTurnAcceleration().unmodify(ID);
                        target.removeCustomData(ID + WHO_CATCH);
                        target = null;
                        return;
                    }
                    if (captureNet==null) {
                        target.setCustomData(ID + WHO_CATCH,ship);
                        int stage = 0;
                        float radius = 0;
                        while (radius<target.getCollisionRadius()) {
                            stage++;
                            radius += stage*DISTANCE;
                        }
                        captureNet = new CaptureNet(target.getLocation(),stage,0,0.1f, VectorUtils.getAngle(ship.getLocation(),target.getLocation()),10);
                    }

                    tracker.advance(amount);
                    if (tracker.intervalElapsed()) {
                        //判断阶段是否结束
                        if (captureNet.allStage > captureNet.nowStage) {
                            captureNet.nowStage++;
                        } else {
                            captureNet.nowStage = 0;
                        }
                        if (captureNet.allStage > captureNet.biggestStage) {
                            captureNet.biggestStage++;
                        }
                        float emp = captureNet.nowStage*10;
                        float dam = captureNet.nowStage*10;

                        Map<String, List<Vector2f>> ringPoint = new HashMap<>();
                        float oneAngle = 360 / captureNet.line;
                        float facing = captureNet.startAngle;

                        for (int nowLine = 0; nowLine < captureNet.line; nowLine++) {
                            Vector2f center = captureNet.location;
                            for (int nowStage = 0; nowStage < captureNet.allStage; nowStage++) {
                                Vector2f topPoint = MathUtils.getPoint(center, nowStage * DISTANCE, facing);
                                Vector2f topPointFloat = new Vector2f(topPoint.x + MathUtils.getRandomNumberInRange(-5f, 5f), topPoint.y + MathUtils.getRandomNumberInRange(-5f, 5f));
                                List<Vector2f> topPointList = new ArrayList<>();
                                if (ringPoint.get(nowStage + "") != null) {
                                    topPointList = ringPoint.get(nowStage + "");
                                }
                                topPointList.add(topPointFloat);
                                ringPoint.put(nowStage + "", topPointList);
                                if (nowStage == captureNet.nowStage) {
                                    EmpArcEntityAPI empArcEntity = engine.spawnEmpArc(ship, center, new SimpleEntity(center), new SimpleEntity(topPointFloat),
                                            DamageType.ENERGY,
                                            dam,
                                            emp, // emp
                                            100000f, // max range
                                            "tachyon_lance_emp_impact",
                                            THICKNESS, // thickness
                                            FRINGE,
                                            CORE
                                    );
                                }
                                center = topPoint;
                            }
                            facing += oneAngle;
                        }
                        int stage = captureNet.nowStage;
                        List<Vector2f> t = ringPoint.get(stage + "");
                        if (t != null) {
                            Vector2f nowV = null;
                            for (int index = 0; index < t.size(); index++) {
                                Vector2f v = t.get(index);
                                if (nowV == null) {
                                    nowV = v;
                                } else {
                                    EmpArcEntityAPI empArcEntity = engine.spawnEmpArc(ship, nowV, new SimpleEntity(nowV), new SimpleEntity(v),
                                            DamageType.ENERGY,
                                            dam,
                                            emp, // emp
                                            100000f, // max range
                                            "tachyon_lance_emp_impact",
                                            THICKNESS, // thickness
                                            FRINGE,
                                            CORE
                                    );
                                    nowV = v;
                                }
                            }
                            if (t.size() > 1) {
                                EmpArcEntityAPI empArcEntity = engine.spawnEmpArc(ship, t.get(0), new SimpleEntity(t.get(0)), new SimpleEntity(t.get(t.size() - 1)),
                                        DamageType.ENERGY,
                                        dam,
                                        emp, // emp
                                        100000f, // max range
                                        "tachyon_lance_emp_impact",
                                        THICKNESS, // thickness
                                        FRINGE,
                                        CORE
                                );
                            }
                        }
                        //船减速 按照网状层级减速
                        target.getMutableStats().getMaxSpeed().modifyPercent(ID, -stage*10);
                        target.getMutableStats().getMaxTurnRate().modifyPercent(ID, -stage*10);
                        target.getMutableStats().getAcceleration().modifyPercent(ID, -stage*10);
                        target.getMutableStats().getTurnAcceleration().modifyPercent(ID, -stage*10);
                        //拉一根电弧到船前面
                        Boolean shieldHit = false;
                        if (target.getShield()!=null)
                        {
                            if (target.getShield().getActiveArc() > captureNet.nowStage/captureNet.allStage*360) {
                                shieldHit = true;
                            }
                        }

                        if (shieldHit) {
                            if (0 == captureNet.nowStage % 2) {
                                Vector2f leftPoint = MathUtils.getPoint(ship.getLocation(), 135, ship.getFacing() + 20);
                                EmpArcEntityAPI empArcEntityLeft = engine.spawnEmpArcVisual(leftPoint, ship, target.getLocation(), target,
                                        THICKNESS, // thickness
                                        FRINGE,
                                        CORE
                                );
                            } else {
                                Vector2f rightPoint = MathUtils.getPoint(ship.getLocation(), 135, ship.getFacing() - 20);
                                EmpArcEntityAPI empArcEntityRight = engine.spawnEmpArcVisual(rightPoint, ship, target.getLocation(), target,
                                        THICKNESS, // thickness
                                        FRINGE,
                                        CORE
                                );
                            }
                        } else {
                            if (0 == captureNet.nowStage % 2) {
                                Vector2f leftPoint = MathUtils.getPoint(ship.getLocation(), 135, ship.getFacing() + 20);
                                EmpArcEntityAPI empArcEntity = engine.spawnEmpArc(ship, leftPoint, new SimpleEntity(leftPoint), target,
                                        DamageType.ENERGY,
                                        dam,
                                        emp, // emp
                                        100000f, // max range
                                        "tachyon_lance_emp_impact",
                                        THICKNESS, // thickness
                                        FRINGE,
                                        CORE
                                );
                            } else {
                                Vector2f rightPoint = MathUtils.getPoint(ship.getLocation(), 135, ship.getFacing() - 20);
                                EmpArcEntityAPI empArcEntity = engine.spawnEmpArc(ship, rightPoint, new SimpleEntity(rightPoint), target,
                                        DamageType.ENERGY,
                                        dam,
                                        emp, // emp
                                        100000f, // max range
                                        "tachyon_lance_emp_impact",
                                        THICKNESS, // thickness
                                        FRINGE,
                                        CORE
                                );
                            }
                        }

                    }
                }
            }
        }

        public void getOut(CombatEntityAPI s){

        }

        public void init(CombatEngineAPI engine) {

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
            return false;
        }
    }

    public class CaptureNet{
        //位置
        public Vector2f location;
        //阶段
        public int allStage;
        //阶段
        public int nowStage;
        public int biggestStage = 0;
        //剩余时间
        public float time;
        //起始角度
        public float startAngle;
        //锚点
        public RC_AnchoredEntity anchoredEntity;
        //多少根线
        public int line;
        //本体
        public DamagingProjectileAPI projectile;
        public CaptureNet(Vector2f location,int allStage,int nowStage,float time,float startAngle,int line)
        {
            this.location = location;
            this.allStage = allStage;
            this.nowStage = nowStage;
            this.time = time;
            this.startAngle = startAngle;
            this.line = line;
        }
    }
}