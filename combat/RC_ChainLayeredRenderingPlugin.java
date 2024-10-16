package real_combat.combat;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.weapons.RC_ChainPlugin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * 命中之后持续生成电弧 给一个力拉向目标
 * 命中盾电弧到对面
 * 命中船体电弧到最近的武器
 * 电弧断开再爆炸给一次EMP伤害
 */

public class RC_ChainLayeredRenderingPlugin extends BaseCombatLayeredRenderingPlugin {
    private final static String ID = "RC_ChainPlugin";
    private final static SpriteAPI SPRITE = Global.getSettings().getSprite("fx", "chain1");
    public RC_ChainLayeredRenderingPlugin() {}
    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (!engine.isPaused()) {
            //没有命中到达最远距离收回
            List<RC_ChainPlugin.RC_Chain> chainList = new ArrayList<>();
            if (engine.getCustomData().get(ID)!=null) {
                chainList = (List<RC_ChainPlugin.RC_Chain>) engine.getCustomData().get(ID);
            }
            List<RC_ChainPlugin.RC_Chain> removeList = new ArrayList<>();
            for (RC_ChainPlugin.RC_Chain c:chainList) {
                if (c.stage == RC_ChainPlugin.Stage.ON_HIT) {
                    //头部停止 尾部推
                    Vector2f lastLocation = null;
                    RC_ChainPlugin.RC_ChainOne last = null;
                    //第二偏移
                    int count = 0;
                    //头部移动其他跟着移动

                    for (RC_ChainPlugin.RC_ChainOne co:c.chainOneList) {
                    /*
                    for (int index = 0;index<c.chainOneList.size()-1;index++) {
                        RC_ChainPlugin.RC_ChainOne co = c.chainOneList.get(index);*/
                        if (lastLocation == null&&c.anchoredEntity!=null) {
                            co.facing = VectorUtils.getAngle(co.location,c.anchoredEntity.getLocation());
                            if (co.deflection == 0) {
                                //co.deflection = MathUtils.getRandomNumberInRange(-10f, 10f);
                            }
                            co.facing += co.deflection;
                            co.location = c.anchoredEntity.getLocation();
                            last = co;
                        }
                        else {
                            co.facing = VectorUtils.getAngle(co.location,lastLocation);
                            //如果距离过近 给予所有锁链偏移量
                            //偏移
                            if(MathUtils.getDistance(co.location,lastLocation)<SPRITE.getHeight()/4) {
                                co.location = MathUtils.getPoint(co.location, SPRITE.getHeight()/4, VectorUtils.getAngle(lastLocation, co.location));
                            }
                            else if(MathUtils.getDistance(co.location,lastLocation)>SPRITE.getHeight()/2
                            ) {
                                co.location = MathUtils.getPoint(co.location,SPRITE.getHeight()/2,VectorUtils.getAngle(co.location,lastLocation));
                            }
                        }
                        count++;
                        lastLocation = co.location;
                    }

                }
                else if (c.stage == RC_ChainPlugin.Stage.ON_FIRE) {
                    Vector2f lastLocation = null;
                    RC_ChainPlugin.RC_ChainOne last = null;
                    //头部移动其他跟着移动
                    for (RC_ChainPlugin.RC_ChainOne co:c.chainOneList) {
                        if (lastLocation == null) {
                            co.facing = c.projectile.getFacing();//VectorUtils.getAngle(co.location,c.projectile.getLocation());
                            co.location = c.projectile.getLocation();
                            last = co;
                        }
                        else {
                            co.facing = VectorUtils.getAngle(co.location,lastLocation);
                            //给其他部位速度
                            //距离超过一定距离才
                            if (MathUtils.getDistance(co.location,lastLocation)>SPRITE.getHeight()/2) {
                                co.location = MathUtils.getPoint(co.location,SPRITE.getHeight()/2,VectorUtils.getAngle(co.location,lastLocation));
                            }
                        }
                        lastLocation = co.location;
                    }
                }
                else if (c.stage == RC_ChainPlugin.Stage.ON_HIT_WAIT) {
                    //尾部移动其他跟着移动
                    Vector2f lastLocation = null;
                    RC_ChainPlugin.RC_ChainOne last = null;
                    for (int index = c.chainOneList.size()-2;index>=0;index--) {
                        RC_ChainPlugin.RC_ChainOne co = c.chainOneList.get(index);
                        if (lastLocation == null) {
                            co.facing = VectorUtils.getAngle(co.location,c.weapon.getFirePoint(0));
                            co.location = c.weapon.getFirePoint(0);
                            last = co;
                        }
                        else {
                            co.facing = VectorUtils.getAngle(co.location,lastLocation);
                            //给其他部位速度
                            //距离超过一定距离才
                            if (MathUtils.getDistance(co.location,lastLocation)>SPRITE.getHeight()/2) {
                                co.location = MathUtils.getPoint(lastLocation,SPRITE.getHeight()/2,VectorUtils.getAngle(lastLocation,co.location));
                            }
                            else if(MathUtils.getDistance(co.location,lastLocation)<SPRITE.getHeight()/4
                            ) {
                                co.location = MathUtils.getPoint(lastLocation,SPRITE.getHeight()/4,VectorUtils.getAngle(co.location,lastLocation));
                            }
                        }
                        lastLocation = co.location;
                    }
                    lastLocation = null;
                    last = null;
                    for (RC_ChainPlugin.RC_ChainOne co:c.chainOneList) {
                    /*
                    for (int index = 0;index<c.chainOneList.size()-1;index++) {
                        RC_ChainPlugin.RC_ChainOne co = c.chainOneList.get(index);*/
                        if (lastLocation == null&&c.anchoredEntity!=null) {
                            co.facing = VectorUtils.getAngle(co.location,c.anchoredEntity.getLocation());
                            if (co.deflection == 0) {
                                //co.deflection = MathUtils.getRandomNumberInRange(-10f, 10f);
                            }
                            co.facing += co.deflection;
                            co.location = c.anchoredEntity.getLocation();
                            last = co;
                        }
                        else {
                            co.facing = VectorUtils.getAngle(co.location,lastLocation);
                            //如果距离过近 给予所有锁链偏移量
                            //偏移
                            if(MathUtils.getDistance(co.location,lastLocation)<SPRITE.getHeight()/4) {
                                co.location = MathUtils.getPoint(co.location, SPRITE.getHeight()/4, VectorUtils.getAngle(lastLocation, co.location));
                            }
                            else if(MathUtils.getDistance(co.location,lastLocation)>SPRITE.getHeight()/2
                            ) {
                                co.location = MathUtils.getPoint(co.location,SPRITE.getHeight()/2,VectorUtils.getAngle(co.location,lastLocation));
                            }
                        }
                        lastLocation = co.location;
                    }
                    //拉直了就要切换状态了
                }
                //基于质量差别选择
                //持续时间
                //持续距离
                else if (c.stage == RC_ChainPlugin.Stage.ON_PULL) {
                    //
                    if (!c.isShieldHit&&c.hitTime>0) {
                        if (c.anchoredEntity!=null) {
                            CombatEntityAPI target = c.anchoredEntity.getAnchor();
                            /*
                            if (target instanceof ShipAPI) {
                                ShipAPI targetShip = (ShipAPI) target;
                                if (targetShip.isAlive()) {

                                }
                            }
                            */
                            if (!target.isExpired()) {
                                float shipToEp = VectorUtils.getAngle(c.weapon.getShip().getLocation(), c.anchoredEntity.getLocation());
                                //给目标一个力
                                CombatUtils.applyForce(target, shipToEp+180, c.weapon.getShip().getMass() * amount);
                                //给自己一个力
                                CombatUtils.applyForce(c.weapon.getShip(), shipToEp, target.getMass() * amount);
                            }
                        }
                    }

                    //尾部移动其他跟着移动
                    Vector2f lastLocation = null;
                    RC_ChainPlugin.RC_ChainOne last = null;
                    for (int index = c.chainOneList.size()-1;index>=0;index--) {
                        RC_ChainPlugin.RC_ChainOne co = c.chainOneList.get(index);
                        if (lastLocation == null) {
                            co.facing = VectorUtils.getAngle(co.location,c.weapon.getFirePoint(0));
                            /*
                            if (MathUtils.getDistance(co.location,c.weapon.getFirePoint(0))>5f) {
                                co.location = MathUtils.getPoint(co.location, SPRITE.getHeight() / 4, VectorUtils.getAngle(co.location, c.weapon.getFirePoint(0)));
                            }
                            else {
                            */
                                co.location = c.weapon.getFirePoint(0);
                            //}
                            last = co;
                        }
                        else {
                            co.facing = VectorUtils.getAngle(co.location,lastLocation);
                            //给其他部位速度
                            //距离超过一定距离才
                            if (MathUtils.getDistance(co.location,lastLocation)>SPRITE.getHeight()/2) {
                                co.location = MathUtils.getPoint(lastLocation,SPRITE.getHeight()/2,VectorUtils.getAngle(lastLocation,co.location));
                            }
                        }
                        lastLocation = co.location;
                    }
                }

                if (c.stage == RC_ChainPlugin.Stage.ON_HIT) {
                    //开始放绳子
                    c.hitTime-=amount;
                    if (c.hitTime<=0) {
                        c.stage = RC_ChainPlugin.Stage.ON_HIT_WAIT;
                        c.hitTime = 1;
                    }
                    //移动当移动超过一个宽度的时候
                    if (c.chainOneList.size()>0) {
                        if (MathUtils.getDistance(c.chainOneList.get(c.chainOneList.size() - 1).location, c.weapon.getFirePoint(0)) > 5f) {
                            if (c.chainOneList.size()%2==0) {
                                c.chainOneList.add(new RC_ChainPlugin.RC_ChainOne(c.weapon.getFirePoint(0), c.weapon.getCurrAngle(),Global.getSettings().getSprite("fx","chain1")));
                            }
                            else {
                                c.chainOneList.add(new RC_ChainPlugin.RC_ChainOne(c.weapon.getFirePoint(0), c.weapon.getCurrAngle(),Global.getSettings().getSprite("fx","chain2")));
                            }
                        }
                    }
                    else {
                        c.chainOneList.add(new RC_ChainPlugin.RC_ChainOne(c.weapon.getFirePoint(0), c.weapon.getCurrAngle(),Global.getSettings().getSprite("fx","chain1")));
                    }
                }
                else if (c.stage == RC_ChainPlugin.Stage.ON_HIT_WAIT) {
                    c.hitTime-=amount;
                    if (c.hitTime<=0) {
                        c.stage = RC_ChainPlugin.Stage.ON_PULL;
                        c.hitTime = 2;
                    }
                    //拉绳子 拉直了开始
                    if (c.chainOneList.size()>0) {
                        float distanced = Math.abs(MathUtils.getDistance(c.chainOneList.get(c.chainOneList.size() - 1).location, c.anchoredEntity.getLocation()) -
                                MathUtils.getDistance(c.weapon.getFirePoint(0), c.anchoredEntity.getLocation()));
                        if (c.chainOneList.size() > 0 && distanced > SPRITE.getHeight()) {
                            if (MathUtils.getDistance(c.chainOneList.get(c.chainOneList.size()-1).location,c.weapon.getFirePoint(0))<=0||(Double.isNaN((double) MathUtils.getDistance(c.chainOneList.get(c.chainOneList.size()-1).location,c.weapon.getFirePoint(0))))) {
                                c.chainOneList.remove(c.chainOneList.size() - 1);
                            }
                        }
                    }
                }
                else if (c.stage == RC_ChainPlugin.Stage.ON_FIRE) {
                    if (c.projectile.isFading()&&!c.projectile.didDamage()) {
                        c.stage = RC_ChainPlugin.Stage.ON_PULL;
                    }
                    if (c.chainOneList.size()>0) {
                        if (MathUtils.getDistance(c.chainOneList.get(c.chainOneList.size() - 1).location, c.weapon.getFirePoint(0)) > 5f) {
                            if (c.chainOneList.size()%2==0) {
                                c.chainOneList.add(new RC_ChainPlugin.RC_ChainOne(c.weapon.getFirePoint(0), c.weapon.getCurrAngle(),Global.getSettings().getSprite("fx","chain1")));
                            }
                            else {
                                c.chainOneList.add(new RC_ChainPlugin.RC_ChainOne(c.weapon.getFirePoint(0), c.weapon.getCurrAngle(),Global.getSettings().getSprite("fx","chain2")));
                            }
                        }
                    }
                    else {
                        c.chainOneList.add(new RC_ChainPlugin.RC_ChainOne(c.weapon.getFirePoint(0), c.weapon.getCurrAngle(),Global.getSettings().getSprite("fx","chain1")));
                    }
                } else if (c.stage == RC_ChainPlugin.Stage.ON_PULL) {
                    c.hitTime-=amount;
                    if (c.hitTime<=0) {
                        c.hitTime = 0;
                    }
                    if (c.chainOneList.size()>0) {
                        if (MathUtils.getDistance(c.chainOneList.get(c.chainOneList.size()-1).location,c.weapon.getFirePoint(0))<=0||(Double.isNaN((double) MathUtils.getDistance(c.chainOneList.get(c.chainOneList.size()-1).location,c.weapon.getFirePoint(0))))) {
                            c.chainOneList.remove(c.chainOneList.size()-1);
                        }
                    }
                    else {
                        removeList.add(c);
                    }
                }
            }
            chainList.removeAll(removeList);
            engine.getCustomData().put(ID, chainList);
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
        CombatEngineAPI engine = Global.getCombatEngine();
        //if (!engine.isPaused()) {
            List<RC_ChainPlugin.RC_Chain> chainList = new ArrayList<>();
            if (engine.getCustomData().get(ID)!=null) {
                chainList = (List<RC_ChainPlugin.RC_Chain>) engine.getCustomData().get(ID);
            }
            for (RC_ChainPlugin.RC_Chain c:chainList) {
                int count = 0;
                for (RC_ChainPlugin.RC_ChainOne co:c.chainOneList) {
                    //渲染
                    if (count%2==0) {
                        SpriteAPI sprite = co.sprite;
                        sprite.setAngle(co.facing - 90);
                        sprite.renderAtCenter(co.location.x, co.location.y);
                    }
                    count++;
                }
                count = 0;
                for (RC_ChainPlugin.RC_ChainOne co:c.chainOneList) {
                    //渲染
                    if (count%2!=0) {
                        SpriteAPI sprite = co.sprite;
                        sprite.setAngle(co.facing - 90);
                        sprite.renderAtCenter(co.location.x, co.location.y);
                    }
                    count++;
                }
                SpriteAPI rocketSprite = Global.getSettings().getSprite("fx","BSF_paralithodes_harpoon_rocket");
                if (c.anchoredEntity!=null&&c.stage!=RC_ChainPlugin.Stage.ON_PULL) {
                    float targetFace = c.anchoredEntity.getAnchor().getFacing();
                    float relativeAngle = c.anchoredEntity.getRelativeAngle();
                    rocketSprite.setAngle(relativeAngle + targetFace);
                    rocketSprite.renderAtCenter(c.anchoredEntity.getLocation().x, c.anchoredEntity.getLocation().y);
                }
                else {
                    //rocketSprite.renderAtCenter(c.weapon.getFirePoint(0).x, c.weapon.getFirePoint(0).y);
                    //float targetFace = c.anchoredEntity.getAnchor().getFacing();
                    //float relativeAngle = c.anchoredEntity.getRelativeAngle();
                    //rocketSprite.setAngle(relativeAngle + targetFace - 90);
                    //rocketSprite.renderAtCenter(c.anchoredEntity.getLocation().x, c.anchoredEntity.getLocation().y);
                }
                if (c.chainOneList.size()>0&&c.stage==RC_ChainPlugin.Stage.ON_PULL) {
                    rocketSprite.setAngle(c.chainOneList.get(0).facing+90);
                    rocketSprite.renderAtCenter(c.chainOneList.get(0).location.x,c.chainOneList.get(0).location.y);
                }
            }
        //}
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    public class Emp{
        WeaponAPI weapon;
        EmpArcEntityAPI empArcEntity;
        public Emp(WeaponAPI weapon,EmpArcEntityAPI empArcEntity)
        {
            this.weapon = weapon;
            this.empArcEntity = empArcEntity;
        }
    }
}

/**
 * 重写整个模拟
 *
 * 发射阶段最前面的一个速度
 *
 * 根据距离来拒去留后面的速度
 *
 * 更新速度为当前速度以及收到的来拒去留力
 *
 * 收回阶段给最后的一个拉力速度
 */