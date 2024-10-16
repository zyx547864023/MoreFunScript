package real_combat.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector2f;
import real_combat.RCModPlugin;
import real_combat.util.MyMath;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class RC_TransAmSystem extends BaseShipSystemScript {
    public static final float MAX_TIME_MULT = 3f;

    public static final Color JITTER_COLOR = new Color(90,165,255,55);
    public static final Color COLOR = new Color(255,0,100,255);
    public static final float DAMAGE_BONUS_PERCENT = 50f;
    public static final float EXTRA_FLUX_PERCENT = 50f;
    public static final float MAX_SPEED_PERCENT = 50f;
    private static String ID = "RC_TransAmSystem";
    private static String IS_ON = "IS_ON";
    private boolean init = false;
    private ShipAPI ship;
    private static float effectLevel;
    private static float width = 0;
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        this.effectLevel = effectLevel;
        CombatEngineAPI engine = Global.getCombatEngine();
        if(engine.isPaused()) {return;}
        ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        try {
            if (ship.getCustomData().get(ID+IS_ON)==null)
            {
                init = false;
            }
            ship.setCustomData(ID+IS_ON,true);
            addBuff(ship,id,effectLevel,engine);
            for (FighterWingAPI w:ship.getAllWings()) {
                for (ShipAPI f : w.getWingMembers()) {
                    if (f.isAlive()) {
                        addBuff(f,id,effectLevel,engine);
                    }
                }
            }
            if (!init) {
                engine.addLayeredRenderingPlugin(new RC_TransAmSystemCombatPlugin(ship));
            }
            //以下是原版时流
            boolean player = false;
            if (stats.getEntity() instanceof ShipAPI) {
                ship = (ShipAPI) stats.getEntity();
                player = ship == Global.getCombatEngine().getPlayerShip();
                id = id + "_" + ship.getId();
            } else {
                return;
            }
            float shipTimeMult = 1f + (MAX_TIME_MULT - 1f) * effectLevel;
            if (player) {
                //播放音乐
                //Global.getSoundPlayer().applyLowPassFilter(0.75f,0f);
                Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / shipTimeMult);
                if (!init) {
                    Global.getSoundPlayer().playSound("trans_am", 1f, 0.7f, ship.getLocation(), ship.getVelocity());
                }
            } else {
                Global.getCombatEngine().getTimeMult().unmodify(id);
            }
            init = true;
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    public void addBuff(ShipAPI ship, String id, float effectLevel,CombatEngineAPI engine) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        stats.getFluxDissipation().modifyPercent(id,EXTRA_FLUX_PERCENT * effectLevel);
        stats.getEnergyWeaponDamageMult().modifyPercent(id, DAMAGE_BONUS_PERCENT * effectLevel);
        stats.getMaxSpeed().modifyPercent(id, MAX_SPEED_PERCENT * effectLevel);
        stats.getMaxTurnRate().modifyPercent(id, MAX_SPEED_PERCENT * effectLevel);

        //如果启动
        //for(int i = 0; i < 2; ++i) {
        Vector2f partstartloc = MathUtils.getPointOnCircumference(ship.getLocation(), ship.getCollisionRadius() * MathUtils.getRandomNumberInRange(0.1F, 0.8F), MyMath.RANDOM.nextFloat() * 360.0F);
        Vector2f partvec = Vector2f.sub(partstartloc, ship.getLocation(), (Vector2f)null);
        partvec.scale(1.5F);
        float size = MathUtils.getRandomNumberInRange(1.0F, 5.0F);
        float damage = MathUtils.getRandomNumberInRange(0.6F, 1.0F);
        float brightness = MathUtils.getRandomNumberInRange(0.1F, 0.5F);
        engine.addSmoothParticle(partstartloc, partvec, size, brightness, damage, Color.green);
        engine.addSmoothParticle(partstartloc, partvec, size, 1F, damage, Color.white);
        //}
        ship.setJitter(ship, Color.RED, 0.5f, 3, 0f, 5f);
        //ship.addAfterimage(Color.RED, 0, 0, -ship.getVelocity().x*5,-ship.getVelocity().y*5, 0f, 0f, 0.05f, 0.1f,true, false, true);
        float shipTimeMult = 1f + (MAX_TIME_MULT - 1f) * effectLevel;
        stats.getTimeMult().modifyMult(id, shipTimeMult);

        ship.getEngineController().fadeToOtherColor(this, JITTER_COLOR, new Color(0,0,0,0), effectLevel, 0.5f);
        ship.getEngineController().extendFlame(this, -0.25f, -0.25f, -0.25f);
    }

    public void removeBuff(ShipAPI ship,String id) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        stats.getFluxDissipation().unmodifyPercent(id);
        stats.getEnergyWeaponDamageMult().unmodifyPercent(id);
        stats.getMaxSpeed().unmodifyPercent(id);
        stats.getMaxTurnRate().unmodifyPercent(id);
        stats.getTimeMult().unmodify(id);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if(engine.isPaused()) {return;}
        if (ship == null) {
            return;
        }
        try {
            ship.setCustomData(ID+IS_ON,false);
            removeBuff(ship,id);
            for (FighterWingAPI w:ship.getAllWings()) {
                for (ShipAPI f : w.getWingMembers()) {
                    if (f.isAlive()) {
                        removeBuff(f,id);
                    }
                }
            }

            init = false;

            //以下是原版时流
            ShipAPI ship = null;
            boolean player = false;
            if (stats.getEntity() instanceof ShipAPI) {
                ship = (ShipAPI) stats.getEntity();
                player = ship == Global.getCombatEngine().getPlayerShip();
                id = id + "_" + ship.getId();
            } else {
                return;
            }

            Global.getCombatEngine().getTimeMult().unmodify(id);
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("粒子全面释放", false);
        }
        else if (index == 1) {
            return new StatusData("+" + (int) (DAMAGE_BONUS_PERCENT * effectLevel) + "% 能量武器伤害" , false);
        }
        else if (index == 2) {
            return new StatusData("+" + (int) (EXTRA_FLUX_PERCENT * effectLevel) + "% 幅能耗散" , false);
        }
        else if (index == 3) {
            return new StatusData("+" + (int) (MAX_SPEED_PERCENT * effectLevel) + "% 机动性" , false);
        }
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

    public static class RC_TransAmSystemCombatPlugin extends BaseCombatLayeredRenderingPlugin {
        private final ShipAPI ship;
        private float timer = 0f;
        private float iTime = 0f;
        private final static float ADD_SHADOW_TIME = 0.05f;
        private float transAmAlphaMult = 1f;
        private boolean isDraw = false;
        private CombatEngineAPI engine = Global.getCombatEngine();
        private SpriteAPI transAmsprite = Global.getSettings().getSprite("graphics/ships/hyperion/trans_am.png");
        List<ShadowMap> shadowList = new ArrayList<>();

        public RC_TransAmSystemCombatPlugin(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void advance(float amount) {
            if (engine == null) return;
            if (engine.isPaused()) {
                return;
            }
            if (!ship.isAlive()) {
                return;
            }
            if (ship.getCustomData().get(ID + IS_ON) == null) {
                return;
            }
            float maxAlphaMult = 0f;
            float alphaMultCut = amount * 1.5f;
            if ((!(boolean) ship.getCustomData().get(ID + IS_ON))) {
                alphaMultCut = amount * 2f;
            }
            try {
                List<ShadowMap> newShadowList = new ArrayList<>();
                for (ShadowMap m : shadowList) {
                    m.alphaMult -= alphaMultCut;
                    if (m.alphaMult > maxAlphaMult) {
                        maxAlphaMult = m.alphaMult;
                    }
                    if (m.alphaMult > 0) {
                        newShadowList.add(m);
                    }
                }
                shadowList.clear();
                shadowList.addAll(newShadowList);
            } catch (Exception e) {
                Global.getLogger(this.getClass()).info(e);
            }
            if (((boolean) ship.getCustomData().get(ID + IS_ON))) {
                //shader
                iTime += amount*3;
                if (timer > ADD_SHADOW_TIME) {
                    addShadowMap(1f);
                    timer = 0f;
                } else {
                    timer += amount;
                }
                if (!ship.isFighter()) {
                    transAmAlphaMult -= amount * 3f;
                }
            }
            //如果关闭了 所有影子向本体移动
            else {
                if (timer > ADD_SHADOW_TIME && maxAlphaMult > 0) {
                    addShadowMap(maxAlphaMult);
                    timer = 0f;
                } else if (maxAlphaMult > 0) {
                    timer += amount;
                }
            }
        }

        public void addShadowMap(float maxAlphaMult) {
            addShadowMap(maxAlphaMult, ship);
            for (FighterWingAPI w : ship.getAllWings()) {
                for (ShipAPI f : w.getWingMembers()) {
                    addShadowMap(maxAlphaMult, f);
                }
            }
        }

        public void addShadowMap(float maxAlphaMult, ShipAPI ship) {
            ShadowMap shadowMap = new ShadowMap(maxAlphaMult, new ArrayList<Shadow>(), new ArrayList<Shadow>(), new ArrayList<Shadow>(), null);
            shadowMap.ship = new Shadow(ship.getFacing() - 90, new Vector2f(ship.getLocation()), ship.getHullSpec().getSpriteName(), null);
            for (WeaponAPI w : ship.getAllWeapons()) {
                if (w.getSlot().isHardpoint()) {
                    shadowMap.weapons.add(0, new Shadow(w.getCurrAngle() - 90, new Vector2f(w.getLocation()), w.getSpec().getHardpointSpriteName(), true));
                    shadowMap.unders.add(0, new Shadow(w.getCurrAngle() - 90, new Vector2f(w.getLocation()), w.getSpec().getHardpointUnderSpriteName(), true));
                } else if (w.getSlot().isTurret()) {
                    shadowMap.weapons.add(0, new Shadow(w.getCurrAngle() - 90, new Vector2f(w.getLocation()), w.getSpec().getTurretSpriteName(), false));
                    shadowMap.unders.add(0, new Shadow(w.getCurrAngle() - 90, new Vector2f(w.getLocation()), w.getSpec().getTurretUnderSpriteName(), false));
                }
                if (w.getBarrelSpriteAPI() != null) {
                    shadowMap.barrels.add(0, new Shadow(w.getCurrAngle() - 90, new Vector2f(w.getLocation()), w));
                }
            }
            if (!ship.isFighter()) {
                SpriteAPI sprite = Global.getSettings().getSprite(shadowMap.ship.spriteName);
                width = sprite.getWidth();
            }
            shadowList.add(shadowMap);
        }

        public void getOut(CombatEntityAPI s) {

        }

        public void init(CombatEngineAPI engine) {

        }

        @Override
        public float getRenderRadius() {
            return 1000000f;
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.UNDER_SHIPS_LAYER);
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (!ship.isAlive()) {
                return;
            }
            for (ShadowMap m : shadowList) {
                if (m.ship.spriteName != null) {
                    SpriteAPI sprite = Global.getSettings().getSprite(m.ship.spriteName);
                    //MagicRenderPlugin.addlocation(sprite, m.ship.location, CombatEngineLayers.UNDER_SHIPS_LAYER);
                    sprite.setAngle(m.ship.facing);
                    sprite.setAlphaMult(m.alphaMult);
                    sprite.setColor(COLOR);
                    sprite.renderAtCenter(m.ship.location.getX(), m.ship.location.getY());
                }
                for (Shadow s : m.unders) {
                    if (s.spriteName != null) {
                        SpriteAPI sprite = Global.getSettings().getSprite(s.spriteName);
                        //MagicRenderPlugin.addlocation(sprite, s.location, CombatEngineLayers.UNDER_SHIPS_LAYER);
                        sprite.setAngle(s.facing);
                        sprite.setAlphaMult(m.alphaMult);
                        sprite.setColor(COLOR);
                        sprite.renderAtCenter(s.location.getX(), s.location.getY());
                    }
                }
                for (Shadow s : m.barrels) {
                    if (s.weapon != null) {
                        WeaponAPI weapon = s.weapon;
                        SpriteAPI sprite = weapon.getBarrelSpriteAPI();
                        sprite.setAlphaMult(m.alphaMult);
                        Color oldColor = sprite.getColor();
                        sprite.setAngle(s.facing);
                        sprite.setColor(COLOR);
                        sprite.setCenter(sprite.getWidth() / 2, sprite.getHeight() / 2);
                        if (weapon.getSlot().isHardpoint()) {
                            sprite.setCenter(sprite.getWidth() / 2, sprite.getHeight() / 4);
                        }
                        sprite.renderAtCenter(s.location.x, s.location.y);
                        sprite.setColor(oldColor);
                    }
                }
                for (Shadow s : m.weapons) {
                    if (s.spriteName != null) {
                        SpriteAPI sprite = Global.getSettings().getSprite(s.spriteName);
                        //MagicRenderPlugin.addlocation(sprite, s.location, CombatEngineLayers.UNDER_SHIPS_LAYER);
                        sprite.setAngle(s.facing);
                        sprite.setAlphaMult(m.alphaMult);
                        sprite.setColor(COLOR);
                        sprite.setCenter(sprite.getWidth() / 2, sprite.getHeight() / 2);
                        if (s.isHardpoint) {
                            sprite.setCenter(sprite.getWidth() / 2, sprite.getHeight() / 4);
                        }
                        sprite.renderAtCenter(s.location.x, s.location.y);
                    }
                }
            }
            //if (engine.isPaused()) {return;}
            if (!ship.isFighter()) {
                if (transAmAlphaMult > 0) {
                    transAmsprite.setSize(width * 2, width * 2);
                    transAmsprite.setAlphaMult(transAmAlphaMult);
                    if (engine.isPaused()) {
                        if (!isDraw) {
                            org.magiclib.plugins.MagicRenderPlugin.addSingleframe(transAmsprite, ship.getLocation(), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                            isDraw = true;
                        }
                    } else {
                        org.magiclib.plugins.MagicRenderPlugin.addSingleframe(transAmsprite, ship.getLocation(), CombatEngineLayers.ABOVE_SHIPS_LAYER);
                        isDraw = false;
                    }
                    //transAmsprite.renderAtCenter(ship.getLocation().getX(), ship.getLocation().getY());
                }
            }
            //renderShader(layer,viewport);
        }

        public void renderShader(CombatEngineLayers layer, ViewportAPI viewport) {
            //if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER && Global.getCombatEngine().getPlayerShip() != null && RCModPlugin.programID != 0) {
                ShipAPI ship = Global.getCombatEngine().getPlayerShip();
                // 选用了游戏本体的一个特效贴图
                SpriteAPI sprite = Global.getSettings().getSprite("graphics/fx/radial_fx.png");
                Vector2f location = ship.getLocation();
                Vector2f size = new Vector2f(sprite.getHeight() * 0.5f, sprite.getWidth() * 0.5f);
                Vector2f uv = new Vector2f(sprite.getHeight() * 0.5f, sprite.getWidth() * 0.5f);
                float facing = ship.getFacing();
                // 计算结构值百分比
                float level = ship.getHitpoints() / ship.getMaxHitpoints();
                // 乘以sqrt(2.0)，为了在满结构值时显示完整的纹理
                float levelQ = level * 1.4142135623730951f;

                // 启用程序
                GL20.glUseProgram(RCModPlugin.programID);

                // 对于着色器绘制来说 GL11.glEnable(GL11.GL_TEXTURE_2D) 是不需要的
                // 同样，纹理单元0同样是默认打开的，这一行也可以不添加
                GL13.glActiveTexture(GL13.GL_TEXTURE0);
                // 该行代码同样可以使用 sprite.bindTexture() 替代
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite.getTextureId());
                // 假设uniform的第一个值储存了纹理uniform位置，第二个参数填0即传入绑定到纹理单元0上的纹理
                // 使用 glUniform1i 来分配纹理单元，但此处甚至可以不添加该行代码，因为默认值就是0
                GL20.glUniform3f(RCModPlugin.iResolutionUniform, viewport.getVisibleWidth()/2,viewport.getVisibleHeight()/2,0);
                GL20.glUniform2f(RCModPlugin.iLocationUniform, location.x-viewport.getCenter().x,location.y-viewport.getCenter().y);
                // 假设uniform的第二个值为state的uniform位置
                GL20.glUniform1f(RCModPlugin.iTimeUniform, iTime);
                //1是半个屏幕大小 2是四分之一屏幕 要用屏幕宽度代表船的宽度
                float shipRadius = ship.getCollisionRadius();
                float radius = viewport.getVisibleHeight()/viewport.getViewMult()/shipRadius/2/2;
                GL20.glUniform1f(RCModPlugin.iRadiusUniform, radius);
                GL20.glUniform1f(RCModPlugin.iEffectLevelUniform, effectLevel);
                GL20.glUniform1f(RCModPlugin.iViewMultUniform, viewport.getViewMult());
                // 以下是绘制部分，应当在绘制前为该次绘制配置好uniform
                GL11.glPushMatrix();
                GL11.glTranslatef(viewport.getCenter().x, viewport.getCenter().y, 0.0f);
                GL11.glRotatef(90f, 0.0f, 0.0f, 1.0f);
                // 混合函数仍会影响Shader绘制结果的显示
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glBegin(GL11.GL_QUADS);
                //GL11.glTexCoord2f(0.0f, 0.0f);
                GL11.glVertex2f(-viewport.getVisibleWidth(), -viewport.getVisibleHeight());
                //GL11.glTexCoord2f(0.0f, uv.y);
                GL11.glVertex2f(-viewport.getVisibleWidth(), viewport.getVisibleHeight());
                //GL11.glTexCoord2f(uv.x, uv.y);
                GL11.glVertex2f(viewport.getVisibleWidth(), viewport.getVisibleHeight());
                //GL11.glTexCoord2f(uv.x, 0.0f);
                GL11.glVertex2f(viewport.getVisibleWidth(), -viewport.getVisibleHeight());
                GL11.glEnd();
                GL11.glPopMatrix();
                // 绘制完毕，解除纹理绑定
                //GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

                // 关闭程序，此时一轮绘制结束
                GL20.glUseProgram(0);
            //}
        }

        @Override
        public boolean isExpired() {
            if (ship.getCustomData().get(ID + IS_ON) != null) {
                if (shadowList.size() == 0 && !(boolean) ship.getCustomData().get(ID + IS_ON)) {
                    return true;
                }
            } else {
                return true;
            }
            return false;
        }

        public class Shadow {
            float facing;
            Vector2f location;
            String spriteName;
            WeaponAPI weapon;
            Boolean isHardpoint;

            public Shadow(float facing, Vector2f location, String spriteName, Boolean isHardpoint) {
                this.facing = facing;
                this.location = location;
                this.spriteName = spriteName;
                this.isHardpoint = isHardpoint;
            }

            public Shadow(float facing, Vector2f location, WeaponAPI weapon) {
                this.facing = facing;
                this.location = location;
                this.weapon = weapon;
            }
        }

        public class ShadowMap {
            float alphaMult;
            List<Shadow> weapons;
            List<Shadow> barrels;
            List<Shadow> unders;
            Shadow ship;

            public ShadowMap(float alphaMult, List<Shadow> weapons, List<Shadow> barrels, List<Shadow> unders, Shadow ship) {
                this.alphaMult = alphaMult;
                this.weapons = weapons;
                this.barrels = barrels;
                this.unders = unders;
                this.ship = ship;
            }
        }
    }
}