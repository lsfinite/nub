/**
 * Lamp by Jean Pierre Charalambos.
 *
 * This class is part of the Luxo example.
 *
 * Add a graphics handler to an InteractiveNode to automatically pick
 * an object. The object is described in the graphics handler procedure.
 */

class Lamp {
  Piece[] pieces;

  Lamp() {
    pieces = new Piece[4];

    for (int i = 0; i < 4; ++i) {
      pieces[i] = new Piece();
      node(i).setReference(i > 0 ? pieces[i - 1] : null);
    }

    // Initialize nodes
    node(1).setTranslation(0, 0, 8); // Base height
    node(2).setTranslation(0, 0, 50);  // Arm length
    node(3).setTranslation(0, 0, 50);  // Arm length

    node(1).setRotation(new Quaternion(new Vector(1, 0, 0), 0.6));
    node(2).setRotation(new Quaternion(new Vector(1, 0, 0), -2));
    node(3).setRotation(new Quaternion(new Vector(1, -0.3, 0), -1.7));

    // Set node graphics modes
    node(0).mode = 1;
    node(1).mode = 2;
    node(2).mode = 2;
    node(3).mode = 3;

    // Set node constraints
    WorldConstraint baseConstraint = new WorldConstraint();
    baseConstraint.setTranslationConstraint(AxisPlaneConstraint.Type.PLANE, new Vector(0, 0, 1));
    baseConstraint.setRotationConstraint(AxisPlaneConstraint.Type.AXIS, new Vector(0, 0, 1));
    node(0).setConstraint(baseConstraint);

    LocalConstraint XAxis = new LocalConstraint();
    XAxis.setTranslationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, new Vector(0, 0, 0));
    XAxis.setRotationConstraint(AxisPlaneConstraint.Type.AXIS, new Vector(1, 0, 0));
    node(1).setConstraint(XAxis);
    node(2).setConstraint(XAxis);

    LocalConstraint headConstraint = new LocalConstraint();
    headConstraint.setTranslationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, new Vector(0, 0, 0));
    node(3).setConstraint(headConstraint);
  }

  Piece node(int i) {
    return pieces[i];
  }
}
