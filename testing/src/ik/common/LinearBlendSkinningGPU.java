package ik.common;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import processing.core.*;
import processing.opengl.PShader;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sebchaparr on 11/03/18.
 */
public class LinearBlendSkinningGPU {
    //Skeleton & Geometry information
    protected List<PShape> _shapes;
    protected List<Node> _skeleton;
    //Shader information
    protected PShader _shader;
    protected Quaternion[] _initialOrientations;
    protected Vector[] _initialPositions;
    protected float[] _initialPositionsArray;
    protected float[] _positionsArray;
    protected float[] _orientationsArray;
    protected Map<Node, Integer> _ids;
    protected final String _fragmentPath = "/testing/src/ik/common/frag.glsl";
    protected final String _vertexPath = "/testing/src/ik/common/skinning.glsl";


    public LinearBlendSkinningGPU(List<Node> skeleton, PGraphics pg, PShape shape) {
        this._shapes = new ArrayList<>();
        _ids = new HashMap<>();
        _skeleton = skeleton;
        int joints = skeleton.size();
        for(int i = 0; i < joints; i++){
            _ids.put(_skeleton.get(i), i);
            if(_skeleton.get(i) instanceof Joint) {
                int c = Color.HSBtoRGB((i + 1.0f) / skeleton.size(), 1f, 1f);
                ((Joint) _skeleton.get(i)).setColor(c);
            }
        }

        _initialOrientations = new Quaternion[joints];
        _initialPositions = new Vector[joints];
        _initialPositionsArray = new float[joints*3];
        _positionsArray = new float[joints*3];
        _orientationsArray = new float[joints*4];
        PApplet pApplet = pg.parent;
        _shader = pApplet.loadShader(pApplet.sketchPath() + _fragmentPath,
                pApplet.sketchPath() + _vertexPath);
        initParams();
        _shapes.add(shape);
    }

    public LinearBlendSkinningGPU(List<Node> skeleton, PGraphics pg, PShape shape, String texture, float factor) {
        this._shapes = new ArrayList<>();
        _ids = new HashMap<>();
        _skeleton = skeleton;
        int joints = skeleton.size();
        for(int i = 0; i < joints; i++){
            _ids.put(_skeleton.get(i), i);
            if(_skeleton.get(i) instanceof Joint) {
                int c = Color.HSBtoRGB((i + 1.0f) / skeleton.size(), 1f, 1f);
                ((Joint) _skeleton.get(i)).setColor(c);
            }
        }
        _initialOrientations = new Quaternion[joints];
        _initialPositions = new Vector[joints];
        _initialPositionsArray = new float[joints*3];
        _positionsArray = new float[joints*3];
        _orientationsArray = new float[joints*4];
        PApplet pApplet = pg.parent;
        _shader = pApplet.loadShader(pApplet.sketchPath() + _fragmentPath,
                pApplet.sketchPath() + _vertexPath);
        initParams();
        _shapes.add(createShapeTri(pg, shape, texture, factor));
    }

    public PShader shader(){
        return _shader;
    }

    public List<PShape> shapes(){
        return _shapes;
    }

    public List<Node> skeleton(){
        return _skeleton;
    }

    public Map<Node, Integer> ids(){
        return _ids;
    }

    public void initParams() {
        for(int i = 0; i < _skeleton.size(); i++){
            Vector v = _skeleton.get(i).position();
            Quaternion q = _skeleton.get(i).orientation();
            _initialOrientations[i] = q;
            _initialPositions[i] = v.get();
            _initialPositionsArray[i*3 + 0] =  v.x();
            _initialPositionsArray[i*3 + 1] =  v.y();
            _initialPositionsArray[i*3 + 2] =  v.z();
        }
        _shader.set("bonePositionOrig", _initialPositionsArray);
        _shader.set("boneLength", _skeleton.size());
        _shader.set("paintMode", -1);
    }
    public void updateParams() {
        //TODO: IT COULD BE DONE WITH LESS OPERATIONS
        for(int i = 0; i < _skeleton.size(); i++){
            Vector v = Vector.subtract(_skeleton.get(i).position(), _initialPositions[i]);
            Quaternion q = Quaternion.compose(_skeleton.get(i).orientation(), _initialOrientations[i].inverse());
            _positionsArray[i*3 + 0] =  v.x();
            _positionsArray[i*3 + 1] =  v.y();
            _positionsArray[i*3 + 2] =  v.z();
            _orientationsArray[i*4 + 0] =  q.x();
            _orientationsArray[i*4 + 1] =  q.y();
            _orientationsArray[i*4 + 2] =  q.z();
            _orientationsArray[i*4 + 3] =  q.w();
        }
        _shader.set("bonePosition", _positionsArray);
        _shader.set("boneRotation", _orientationsArray);
    }

    public void paintAllJoints(){
        _shader.set("paintMode", 0);
    }

    public void paintJoint(int id){
        _shader.set("paintMode", id);
    }

    public void disablePaintMode(){
        _shader.set("paintMode", -1);
    }

    public float[] addWeights(List<Node> branch, PVector vector){
        Vector position = new Vector(vector.x, vector.y, vector.z);
        float total_dist = 0.f;
        int[] joints = new int[]{-1, -1, -1};
        float[] w = new float[]{0, 0, 0};
        float[] d = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        //Find the nearest 3 joints
        //TODO : Perhaps enable more joints - use QuickSort
        for (Node joint : branch) {
            if (joint == branch.get(0)) continue;
            if (joint.translation().magnitude() <= Float.MIN_VALUE) continue;
            float dist = (float) Math.pow(getDistance(position, joint), 10);
            //System.out.println("Distance " + dist + " pos " + position + " j " + joint.position() );
            if(dist <= d[0] || dist <= d[1] || dist <= d[2]){
                int start = dist <= d[0] ? 0 : dist <= d[1] ? 1 : 2;
                //System.out.println("Entra" );
                //swap
                //System.out.print("Update : " );

                for(int l = joints.length-1; l > start; l--){
                    joints[l] = joints[l-1];
                    d[l] = d[l-1];
                    //System.out.print(joints[l] + " ");
                }
                joints[start] = _ids.get(joint.reference());
                //System.out.println(joints[start] + " ");
                d[start] = dist;
            }
        }

        for(int k = 0; k < joints.length; k++){
            total_dist += 1.f/d[k];
        }
        for(int k = 0; k < joints.length; k++){
            w[k] += 1.f/d[k] / total_dist;
        }
        //System.out.println("Pos:" + position);
        //System.out.println("j0 pos : " + branch.get(joints[0]).position() + " j1 : " + branch.get(joints[1]).position()  + "j2 : " + branch.get(joints[2]).position());
        //System.out.println("j0 : " + joints[0] + " j1 : " + joints[1]  + "j2 : " + joints[2]);

        return new float[]{joints[0], joints[1], joints[2], w[0], w[1], w[2]};
    }

    /*
     * Get the distance from vertex to line formed by frame and the reference frame of frame
     * Distance will be measure according to root coordinates.
     * In case of reference frame of frame is root, it will return distance from vertex to frame
     * */
    public static float getDistance(Vector vertex, Node node) {
        if (node == null) return Float.MAX_VALUE;
        Vector position = node.position();
        if(node.reference() == null) return Vector.distance(position, vertex);
        Vector parentPosition = node.reference().position();
        //is the distance between line formed by b and its parent and v
        Vector line = Vector.subtract(position, parentPosition);
        Vector projection = Vector.subtract(vertex, parentPosition);
        float dot = Vector.dot(projection, line);
        float magnitude = line.magnitude();
        float u = dot * (float) 1. / (magnitude * magnitude);
        Vector distance = new Vector();
        if (u >= 0 && u <= 1) {
            distance = new Vector(parentPosition.x() + u * line.x(), parentPosition.y() + u * line.y(),
                    parentPosition.z() + u * line.z());
            distance = Vector.subtract(distance, vertex);
        }
        if (u < 0) {
            distance = Vector.subtract(position, vertex);
        }
        if (u > 1) {
            distance = Vector.subtract(position, vertex);
        }
        return distance.magnitude();
    }

    //Adapted from http://www.cutsquash.com/2015/04/better-obj-model-loading-in-processing/
    public PShape createShapeTri(PGraphics pg, PShape r, String texture, float size) {
        float scaleFactor = size / Math.max(r.getWidth(), r.getHeight());
        PImage tex = pg.parent.loadImage(texture);
        PShape s = pg.createShape();
        s.beginShape(PConstants.TRIANGLES);
        s.noStroke();
        s.texture(tex);
        s.textureMode(PConstants.NORMAL);
        if(r.getChildCount() == 0) {
            for (int j = 0; j < r.getVertexCount(); j++) {
                PVector p = r.getVertex(j).mult(scaleFactor);
                System.out.println("scale " + scaleFactor);
                System.out.println("v " + p);
                PVector n = r.getNormal(j);
                float u = r.getTextureU(j);
                float v = r.getTextureV(j);
                s.normal(n.x, n.y, n.z);
                float[] params = addWeights(_skeleton, p);
                s.attrib("joints", params[0] * 1.f, params[1] * 1.f, params[2] * 1.f);
                s.attrib("weights", params[3], params[4], params[5]);
                s.vertex(p.x, p.y, p.z, u, v);
            }
        } else {
            for (int i = 0; i < r.getChildCount(); i++) {
                for (int j = 0; j < r.getChild(i).getVertexCount(); j++) {
                    PVector p = r.getChild(i).getVertex(j).mult(scaleFactor);
                    System.out.println("scale " + scaleFactor);
                    System.out.println("p " + p);
                    PVector n = r.getChild(i).getNormal(j);
                    float u = r.getChild(i).getTextureU(j);
                    float v = r.getChild(i).getTextureV(j);
                    s.normal(n.x, n.y, n.z);
                    float[] params = addWeights(_skeleton, p);
                    System.out.println("joints " + params[0] * 1.f + " " + params[1] * 1.f + " " + params[2] * 1.f);
                    System.out.println("w " + params[3] * 1.f + " " + params[4] * 1.f + " " + params[5] * 1.f);

                    s.attrib("joints", params[0] * 1.f, params[1] * 1.f, params[2] * 1.f);
                    s.attrib("weights", params[3], params[4], params[5]);

                    s.vertex(p.x, p.y, p.z, u, v);
                }
            }
        }
        s.endShape();
        return s;
    }
}