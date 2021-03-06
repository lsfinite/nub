package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;
import processing.opengl.PShader;

public class ShadowMap extends PApplet {
  Graph.Type shadowMapType = Graph.Type.ORTHOGRAPHIC;
  Scene scene;
  Node[] shapes;
  PGraphics shadowMap;
  PShader depthShader;
  float zNear = 50;
  float zFar = 1000;
  int w = 1000;
  int h = 1000;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.setRadius(max(w, h));
    scene.fit(1);
    shapes = new Node[20];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Node(scene) {
        @Override
        public void graphics(PGraphics pg) {
          pg.pushStyle();
          if (scene.node("light") == this) {
            Scene.drawAxes(pg, 150);
            pg.fill(0, scene.hasTag(this) ? 255 : 0, 255, 120);
            Scene.drawFrustum(pg, shadowMap, shadowMapType, this, zNear, zFar);
          } else {
            if (pg == shadowMap)
              pg.noStroke();
            else {
              pg.strokeWeight(3);
              pg.stroke(0, 255, 255);
            }
            pg.fill(255, 0, 0);
            pg.box(80);
          }
          pg.popStyle();
        }
      };
      shapes[i].setPickingThreshold(0);
      shapes[i].randomize();
      // set picking precision to the pixels of the node projection
      shapes[i].setPickingThreshold(0);
      shapes[i].setHighlighting(0);
    }
    shadowMap = createGraphics(w / 2, h / 2, P3D);
    //depthShader = loadShader("/home/pierre/IdeaProjects/nub/testing/data/depth/depth_linear.glsl");
    //depthShader.set("near", zNear);
    //depthShader.set("far", zFar);
    depthShader = loadShader("/home/pierre/IdeaProjects/nub/testing/data/depth/depth_nonlinear.glsl");
    shadowMap.shader(depthShader);

    scene.tag("light", shapes[(int) random(0, shapes.length - 1)]);
    scene.node("light").setOrientation(new Quaternion(new Vector(0, 0, 1), scene.node("light").position()));
  }

  public void draw() {
    background(75, 25, 15);
    // 1. Fill in and display front-buffer
    scene.render();
    // 2. Fill in shadow map using the light point of view
    if (scene.node("light") != null) {
      shadowMap.beginDraw();
      shadowMap.background(140, 160, 125);
      scene.render(shadowMap, shadowMapType, scene.node("light"), zNear, zFar);
      shadowMap.endDraw();
      // 3. Display shadow map
      scene.beginHUD();
      image(shadowMap, w / 2, h / 2);
      scene.endHUD();
    }
  }

  public void mouseMoved(MouseEvent event) {
    if (event.isShiftDown())
      scene.mouseTag("light");
    else
      scene.mouseTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin();
    else if (mouseButton == RIGHT)
      scene.mouseTranslate();
    else
      scene.moveForward(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    if (event.isShiftDown()) {
      // custom interaction of the light node: setting the light
      // zFar plane is implemented as a custom behavior by node.interact()
      if (scene.node("light") != null)
        depthShader.set("far", zFar += event.getCount() * 20);
    }
    else
      scene.scale(event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == ' ')
      shadowMapType = shadowMapType == Graph.Type.ORTHOGRAPHIC ? Graph.Type.PERSPECTIVE : Graph.Type.ORTHOGRAPHIC;
    if (key == 'p')
      scene.togglePerspective();
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.ShadowMap"});
  }
}
