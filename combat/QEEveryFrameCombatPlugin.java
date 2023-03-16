package real_combat.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;
import com.fs.starfarer.api.input.InputEventAPI;
import data.scripts.util.MagicRender;
import data.scripts.util.MagicTxt;
import data.scripts.util.MagicUI;
import org.intellij.lang.annotations.MagicConstant;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QEEveryFrameCombatPlugin implements EveryFrameCombatPlugin {
	private LazyFont font;
	private float systemAmmoFontSize = 20;
	private CombatEngineAPI engine = Global.getCombatEngine();

	private Color COLOR = new Color(255, 255, 255, 255);

	public void advance(float amount, List<InputEventAPI> events) {

	}

	public void renderInUICoords(ViewportAPI viewport) {
	}

	public void init(CombatEngineAPI engine) {
		try {
			font = LazyFont.loadFont("graphics/fonts/orbitron20aabold.fnt");
		} catch (FontException ex) {
			throw new RuntimeException("Failed to load font");
		}
	}

	private boolean hide(){
		if (engine == null || engine.getCombatUI() == null || engine.getPlayerShip() == null) return true;
		if(!engine.getPlayerShip().isAlive() || engine.getPlayerShip().isHulk()) return true;
		if (engine.getCombatUI().isShowingCommandUI() || engine.isUIShowingDialog()) return true;
		return !engine.isUIShowingHUD();
	}


	public void renderInWorldCoords(ViewportAPI viewport) {
		if(hide()) return;
		ShipAPI playerShip = engine.getPlayerShip();
		drawFont("Q", COLOR, 90, playerShip.getCollisionRadius()*2, 0, systemAmmoFontSize*viewport.getViewMult());
		drawFont("E", COLOR, -90, playerShip.getCollisionRadius()*2, 0, systemAmmoFontSize*viewport.getViewMult());

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

		Vector2f point = MathUtils.getPoint(playerShip.getLocation(),playerShip.getCollisionRadius()*2F,playerShip.getFacing()+90);
		drawArc(COLOR, viewport.getAlphaMult(), 361f, point, 20, 0f, 0f, 0f, 0f, 2);
		point = MathUtils.getPoint(playerShip.getLocation(),playerShip.getCollisionRadius()*2F,playerShip.getFacing()-90);
		drawArc(COLOR, viewport.getAlphaMult(), 361f, point, 20, 0f, 0f, 0f, 0f, 2);

		GL11.glDisable(GL11.GL_BLEND);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glPopMatrix();
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glPopMatrix();
		GL11.glPopAttrib();
	}

	public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

	}

	private void drawFont(String text, Color color, float aimAngle, float right, float up, float size){
		color = new Color(color.getRed(), color.getGreen(), color.getBlue());
		LazyFont.DrawableString toDraw = font.createText(text, color, size);
		ShipAPI playerShip = engine.getPlayerShip();
		Vector2f point = MathUtils.getPoint(playerShip.getLocation(),right,playerShip.getFacing()+aimAngle);
		point = MathUtils.getPoint(point,size/2,135F);
		point = MathUtils.getPoint(point,size/8,90F);
		toDraw.draw(
				point.getX(),
				point.getY()

		);
	}

	private void drawArc(Color color, float alpha, float angle, Vector2f loc, float radius, float aimAngle, float aimAngleTop, float x, float y, float thickness){
		GL11.glLineWidth(thickness);
		radius = radius * engine.getViewport().getViewMult();
		GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte)Math.max(0, Math.min(Math.round(alpha * 255f), 255)) );
		GL11.glBegin(GL11.GL_LINE_STRIP);
		for(int i = 0; i < Math.round(angle); i++){
			GL11.glVertex2f(
					loc.x + (radius * (float)Math.cos(Math.toRadians(aimAngleTop + i)) + x * (float)Math.cos(Math.toRadians(aimAngle - 90f)) - y * (float)Math.sin(Math.toRadians(aimAngle - 90f))),
					loc.y + (radius * (float)Math.sin(Math.toRadians(aimAngleTop + i)) + x * (float)Math.sin(Math.toRadians(aimAngle - 90f)) + y * (float)Math.cos(Math.toRadians(aimAngle - 90f)))
			);
		}
		GL11.glEnd();
		GL11.glPopMatrix();
	}
}
