package real_combat.combat;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;
import real_combat.weapons.RC_DragonShotOnHitEffect;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * 命中之后持续生成电弧 给一个力拉向目标
 * 导弹命中之后展开蛛网
 * 先*
 * 后端点连线向外延伸
 * 可以设定层数
 *
 * 导弹网是平的
 */

public class RC_DragonShotRenderingPlugin extends BaseCombatLayeredRenderingPlugin {
    private final static String ID = "RC_DragonShotRenderingPlugin";
    private final static Color FRINGE = new Color(20,191,255, 100);
    private final static Color CORE =  new Color(255, 255, 255, 100);
    private final static float THICKNESS = 1f;
    private final static String DRAGONSHOT_LIST="DRAGONSHOT_LIST";
    private final static float DISTANCE = 11f;

    protected IntervalUtil tracker = new IntervalUtil(0.1f, 0.1f);

    public RC_DragonShotRenderingPlugin() {}
    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (!engine.isPaused()) {
            tracker.advance(amount);
            //if (tracker.intervalElapsed()) {
                if (engine.getCustomData().get(DRAGONSHOT_LIST) != null) {
                    List<RC_DragonShotOnHitEffect.CamouflageNet> camouflageNetList = (List<RC_DragonShotOnHitEffect.CamouflageNet>) engine.getCustomData().get(DRAGONSHOT_LIST);
                    List<RC_DragonShotOnHitEffect.CamouflageNet> removeList = new ArrayList<>();
                    for (RC_DragonShotOnHitEffect.CamouflageNet c : camouflageNetList) {
                        float emp = c.projectile.getEmpAmount();
                        float dam = c.projectile.getDamageAmount();
                        //判断阶段是否结束
                        if (tracker.intervalElapsed()) {
                            if (c.allStage > c.nowStage) {
                                c.nowStage++;
                            } else {
                                removeList.add(c);
                            }
                        }
                        Map<String, List<Vector2f>> ringPoint = new HashMap<>();
                        float oneAngle = 360 / c.line;
                        float facing = c.startAngle;
                        List<EmpArcEntityAPI> empList = new ArrayList<>();
                        if (engine.getCustomData().get(c+"EMPARCENTITY_LIST") != null) {
                            empList = (List<EmpArcEntityAPI>) engine.getCustomData().get(c+"EMPARCENTITY_LIST");
                            for (EmpArcEntityAPI e:empList) {
                                e.getLocation().set(e.getLocation().getX() * (1.25f), e.getLocation().getY() * (1.25f));
                                e.getTargetLocation().set(e.getTargetLocation().getX() * (1.25f), e.getTargetLocation().getY() * (1.25f));
                            }
                        }
                        else {
                            for (int nowLine = 0; nowLine < c.line; nowLine++) {
                                Vector2f center = c.location;
                                for (int nowStage = 0; nowStage < c.allStage; nowStage++) {
                                    Vector2f topPoint = MathUtils.getPoint(center, nowStage * DISTANCE * c.time, facing);
                                    Vector2f topPointFloat = new Vector2f(topPoint.x + MathUtils.getRandomNumberInRange(-5f, 5f), topPoint.y + MathUtils.getRandomNumberInRange(-5f, 5f));
                                    List<Vector2f> topPointList = new ArrayList<>();
                                    if (ringPoint.get(nowStage + "") != null) {
                                        topPointList = ringPoint.get(nowStage + "");
                                    }
                                    topPointList.add(topPointFloat);
                                    ringPoint.put(nowStage + "", topPointList);
                                    Global.getLogger(this.getClass()).info(nowStage + "||" + c.nowStage);
                                    //if (nowStage==c.nowStage-1||nowStage==c.nowStage-2) {//
                                        EmpArcEntityAPI empArcEntity = engine.spawnEmpArc(c.projectile.getSource(), center, new SimpleEntity(center), new SimpleEntity(topPointFloat),
                                                DamageType.ENERGY,
                                                dam,
                                                emp, // emp
                                                100000f, // max range
                                                "tachyon_lance_emp_impact",
                                                THICKNESS, // thickness
                                                FRINGE,
                                                CORE
                                        );
                                        empList.add(empArcEntity);
                                    //}
                                    center = topPoint;
                                }
                                facing += oneAngle;
                            }
                            for (int stage = 0; stage < c.allStage-1; stage++) {
                                List<Vector2f> t = ringPoint.get(stage + "");
                                if (t != null) {
                                    Vector2f nowV = null;
                                    for (int index = 0; index < t.size(); index++) {
                                        Vector2f v = t.get(index);
                                        if (nowV == null) {
                                            nowV = v;
                                        } else {
                                            EmpArcEntityAPI empArcEntity = engine.spawnEmpArc(c.projectile.getSource(), nowV, new SimpleEntity(nowV), new SimpleEntity(v),
                                                    DamageType.ENERGY,
                                                    dam,
                                                    emp, // emp
                                                    100000f, // max range
                                                    "tachyon_lance_emp_impact",
                                                    THICKNESS, // thickness
                                                    FRINGE,
                                                    CORE
                                            );
                                            empList.add(empArcEntity);
                                            nowV = v;
                                        }
                                    }
                                    if (t.size() > 1) {
                                        EmpArcEntityAPI empArcEntity = engine.spawnEmpArc(c.projectile.getSource(), t.get(0), new SimpleEntity(t.get(0)), new SimpleEntity(t.get(t.size() - 1)),
                                                DamageType.ENERGY,
                                                dam,
                                                emp, // emp
                                                100000f, // max range
                                                "tachyon_lance_emp_impact",
                                                THICKNESS, // thickness
                                                FRINGE,
                                                CORE
                                        );
                                        empList.add(empArcEntity);
                                    }
                                }
                            }
                        }
                        engine.getCustomData().put(c+"EMPARCENTITY_LIST",empList);
                    }
                    for (RC_DragonShotOnHitEffect.CamouflageNet c:removeList) {
                        engine.getCustomData().remove(c+"EMPARCENTITY_LIST");
                    }
                    camouflageNetList.removeAll(removeList);
                    engine.getCustomData().put(DRAGONSHOT_LIST, camouflageNetList);
                }
            //}
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