package real_combat.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class RC_QEEveryFrameCombatPlugin implements EveryFrameCombatPlugin {
	private CombatEngineAPI engine = Global.getCombatEngine();
	private final static Color COLOR = new Color(255, 255, 255, 255);
	private final static String FONT_FILE = "graphics/fonts/orbitron20aabold.fnt";
	private final static float SYSTEM_AMMO_FONT_SIZE = 20;
	private final static float THICKNESS = 3;
	private LazyFont font;

	public void advance(float amount, List<InputEventAPI> events) {

	}

	public void renderInUICoords(ViewportAPI viewport) {
	}

	public void init(CombatEngineAPI engine) {
		try {
			font = LazyFont.loadFont(FONT_FILE);
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
		drawFont("Q", COLOR, 90, playerShip.getCollisionRadius()*2, 0, SYSTEM_AMMO_FONT_SIZE*viewport.getViewMult());
		drawFont("E", COLOR, -90, playerShip.getCollisionRadius()*2, 0, SYSTEM_AMMO_FONT_SIZE*viewport.getViewMult());

		Vector2f point = MathUtils.getPoint(playerShip.getLocation(),playerShip.getCollisionRadius()*2F,playerShip.getFacing()+90);
		drawArc(COLOR, viewport.getAlphaMult(), 361f, point, SYSTEM_AMMO_FONT_SIZE*viewport.getViewMult(), 0f, 0f, 0f, 0f, THICKNESS);
		point = MathUtils.getPoint(playerShip.getLocation(),playerShip.getCollisionRadius()*2F,playerShip.getFacing()-90);
		drawArc(COLOR, viewport.getAlphaMult(), 361f, point, SYSTEM_AMMO_FONT_SIZE*viewport.getViewMult(), 0f, 0f, 0f, 0f, THICKNESS);
		//测试画线
		//SpriteAPI sprite = Global.getSettings().getSprite("beamfringec", "beamfringec");
		//newRenderLine(Color.GREEN, playerShip.getLocation(), sprite, playerShip.getMouseTarget(), 1f, 5f);
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
		GL11.glPushMatrix();
		//禁用纹理
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		//抗锯齿
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

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
		GL11.glPopMatrix();
	}

	public void newRenderLine(Color paramColor, Vector2f anchor, SpriteAPI lineTex, Vector2f target, float alphaMult, float width) {
		GL11.glPushMatrix();
		GL11.glTranslatef(0f, 0f, 0f);
		GL11.glRotatef(0f, 0f, 0f, 1f);

		GL11.glEnable(GL11.GL_TEXTURE_2D);
		lineTex.bindTexture();

		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		GL11.glColor4ub((byte) paramColor.getRed(), (byte) paramColor.getGreen(), (byte) paramColor.getBlue(), (byte) (paramColor.getAlpha() * alphaMult));
		GL11.glLineWidth(width);

		GL11.glBegin(GL11.GL_LINES);
		GL11.glTexCoord2f(anchor.x, anchor.y);
		GL11.glVertex2f(anchor.x, anchor.y);
		GL11.glTexCoord2f(target.x, target.y);
		GL11.glVertex2f(target.x, target.y);
		GL11.glEnd();
		GL11.glPopMatrix();
	}
}
