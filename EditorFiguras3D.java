import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;

public class EditorFiguras3D extends JFrame implements GLEventListener {
    private JComboBox<String> selectorForma, selectorColor, selectorTransform;
    private JPanel panelControles, panelColor;
    private JSlider[] sliders;
    private GLJPanel glPanel;
    private JSlider sliderSesgoX, sliderSesgoY;
    private float rotationX = 0, rotationY = 0;
    private float translateX = 0, translateY = 0, translateZ = -5;
    private float scale = 1;
    private int currentShape = 0;
    private float[] colorValues = new float[3];
    private GLU glu = new GLU();
    private FPSAnimator animator;

    public EditorFiguras3D() {
        configurarVentana();
        inicializarComponentes();
        configurarEventos();
    }

    private void configurarVentana() {
        setTitle("Editor de Figuras 3D");
        setSize(1280, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(45, 45, 45));
        setLayout(new BorderLayout(10, 10));
    }

    private void inicializarComponentes() {
        // Configurar panel OpenGL
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities capabilities = new GLCapabilities(profile);
        glPanel = new GLJPanel(capabilities);
        glPanel.addGLEventListener(this);
        animator = new FPSAnimator(glPanel, 60);
        animator.start();

        // Panel izquierdo - Controles
        JPanel panelIzquierdo = new JPanel(new GridLayout(0, 1, 10, 10));
        panelIzquierdo.setBorder(new CompoundBorder(
                new LineBorder(new Color(80, 80, 80)),
                new EmptyBorder(15, 15, 15, 15))
        );
        panelIzquierdo.setBackground(new Color(60, 60, 60));

        String[] formas = {"Cubo", "Esfera", "Piramide", "Cilindro", "Cono"};
        selectorForma = crearComboBox(formas, new Color(70, 130, 180));
        
        String[] modelosColor = {"RGB", "HSL", "HSV"};
        selectorColor = crearComboBox(modelosColor, new Color(80, 160, 120));
        
        String[] transformaciones = {"Rotación", "Escala", "Traslación"};
        selectorTransform = crearComboBox(transformaciones, new Color(160, 100, 200));

        panelIzquierdo.add(crearEtiqueta("Seleccionar Figura:"));
        panelIzquierdo.add(selectorForma);
        panelIzquierdo.add(crearEtiqueta("Modelo de Color:"));
        panelIzquierdo.add(selectorColor);
        panelIzquierdo.add(crearEtiqueta("Transformación:"));
        panelIzquierdo.add(selectorTransform);

        add(panelIzquierdo, BorderLayout.WEST);
        add(glPanel, BorderLayout.CENTER);

        // Panel controles de color
        panelColor = new JPanel();
        panelColor.setLayout(new BoxLayout(panelColor, BoxLayout.Y_AXIS));
        panelColor.setBorder(new EmptyBorder(15, 15, 15, 15));
        panelColor.setBackground(new Color(70, 70, 70));
        add(panelColor, BorderLayout.EAST);

        actualizarControlesColor();
    }

    // Métodos auxiliares para crear componentes (similares a los originales)
    private JComboBox<String> crearComboBox(String[] items, Color colorFondo) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setBackground(colorFondo);
        combo.setForeground(Color.WHITE);
        combo.setRenderer(new DefaultListCellRenderer());
        return combo;
    }

    private JSlider crearSlider(int min, int max, String titulo) {
        JSlider slider = new JSlider(min, max, 50);
        slider.setBackground(new Color(70, 70, 70));
        slider.setForeground(Color.WHITE);
        slider.setBorder(BorderFactory.createTitledBorder(titulo));
        return slider;
    }

    private JLabel crearEtiqueta(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setForeground(new Color(220, 220, 220));
        return etiqueta;
    }

    private void configurarEventos() {
        selectorForma.addActionListener(e -> {
            currentShape = selectorForma.getSelectedIndex();
            glPanel.repaint();
        });
        
        selectorColor.addActionListener(e -> actualizarControlesColor());
        
        MouseAdapter mouseAdapter = new MouseAdapter() {
            private Point lastPoint;
            
            public void mousePressed(MouseEvent e) {
                lastPoint = e.getPoint();
            }
            
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastPoint.x;
                int dy = e.getY() - lastPoint.y;
                
                switch(selectorTransform.getSelectedIndex()) {
                    case 0: // Rotación
                        rotationX += dy * 0.5f;
                        rotationY += dx * 0.5f;
                        break;
                    case 1: // Escala
                        scale += dy * 0.01f;
                        break;
                    case 2: // Traslación
                        translateX += dx * 0.01f;
                        translateY -= dy * 0.01f;
                        break;
                }
                lastPoint = e.getPoint();
                glPanel.repaint();
            }
        };
        
        glPanel.addMouseListener(mouseAdapter);
        glPanel.addMouseMotionListener(mouseAdapter);
    }

    private void actualizarControlesColor() {
        panelColor.removeAll();
        String modelo = (String) selectorColor.getSelectedItem();
        String[] etiquetas = modelo.equals("RGB") ? 
            new String[]{"Rojo", "Verde", "Azul"} :
            new String[]{"Tono", "Saturación", "Luminosidad"};
        
        for(int i = 0; i < 3; i++) {
            JSlider slider = crearSlider(0, 100, etiquetas[i]);
            final int index = i;
            slider.addChangeListener(e -> {
                colorValues[index] = slider.getValue() / 100f;
                glPanel.repaint();
            });
            panelColor.add(slider);
        }
        panelColor.revalidate();
        panelColor.repaint();
    }

    // Métodos OpenGL
    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glEnable(GL2.GL_DEPTH_TEST);
        //gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);
        gl.glEnable(GL2.GL_COLOR_MATERIAL);
        gl.glClearColor(1f, 1f, 1f, 1f);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        
        gl.glLoadIdentity();
        glu.gluPerspective(45, (float)glPanel.getWidth()/glPanel.getHeight(), 0.1f, 100f);
        glu.gluLookAt(0, 0, 5, 0, 0, 0, 0, 1, 0);
        
        // Aplicar transformaciones
        gl.glTranslatef(translateX, translateY, translateZ);
        gl.glRotatef(rotationX, 1, 0, 0);
        gl.glRotatef(rotationY, 0, 1, 0);
        gl.glScalef(scale, scale, scale);
        
        // Configurar color
        Color color = convertColor(colorValues, selectorColor.getSelectedIndex());
        gl.glColor3f(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f);
        
        // Dibujar figura
        switch(currentShape) {
            case 0: drawCube(gl); break;
            case 1: drawSphere(gl); break;
            case 2: drawPyramid(gl); break;
            case 3: drawCylinder(gl); break;
            case 4: drawCone(gl); break;
        }
    }

    private Color convertColor(float[] values, int colorModel) {
        if(colorModel == 0) { // RGB
            return new Color(values[0], values[1], values[2]);
        } else { // HSL/HSV
            return Color.getHSBColor(values[0], values[1], values[2]);
        }
    }

    private void drawCube(GL2 gl) {
        gl.glBegin(GL2.GL_QUADS);
        
        // Cara frontal (roja)
        gl.glColor3f(1, 0, 0);
        gl.glVertex3f(-1, -1, 1);
        gl.glVertex3f(1, -1, 1);
        gl.glVertex3f(1, 1, 1);
        gl.glVertex3f(-1, 1, 1);
        
        // Cara trasera (verde)
        gl.glColor3f(0, 1, 0);
        gl.glVertex3f(-1, -1, -1);
        gl.glVertex3f(-1, 1, -1);
        gl.glVertex3f(1, 1, -1);
        gl.glVertex3f(1, -1, -1);
        
        // Cara superior (azul)
        gl.glColor3f(0, 0, 1);
        gl.glVertex3f(-1, 1, -1);
        gl.glVertex3f(-1, 1, 1);
        gl.glVertex3f(1, 1, 1);
        gl.glVertex3f(1, 1, -1);
        
        // Cara inferior (amarilla)
        gl.glColor3f(1, 1, 0);
        gl.glVertex3f(-1, -1, -1);
        gl.glVertex3f(1, -1, -1);
        gl.glVertex3f(1, -1, 1);
        gl.glVertex3f(-1, -1, 1);
        
        // Cara izquierda (cyan)
        gl.glColor3f(0, 1, 1);
        gl.glVertex3f(-1, -1, -1);
        gl.glVertex3f(-1, -1, 1);
        gl.glVertex3f(-1, 1, 1);
        gl.glVertex3f(-1, 1, -1);
        
        // Cara derecha (magenta)
        gl.glColor3f(1, 0, 1);
        gl.glVertex3f(1, -1, -1);
        gl.glVertex3f(1, 1, -1);
        gl.glVertex3f(1, 1, 1);
        gl.glVertex3f(1, -1, 1);
        
        gl.glEnd();
    }

    private void drawSphere(GL2 gl) {
    gl.glColor3f(1, 0.5f, 0);
    GLUquadric sphere = glu.gluNewQuadric();
    glu.gluQuadricDrawStyle(sphere, GLU.GLU_LINE); // Modo wireframe
    glu.gluSphere(sphere, 1, 64, 64); // Más segmentos para mayor detalle
}

    private void drawPyramid(GL2 gl) {
        gl.glBegin(GL2.GL_TRIANGLES);
        
        // Base (cuadrada)
        gl.glColor3f(1, 0, 0);
        gl.glVertex3f(-1, -1, -1);
        gl.glVertex3f(1, -1, -1);
        gl.glVertex3f(0, -1, 1);
        
        // Caras laterales
        gl.glColor3f(0, 1, 0);
        gl.glVertex3f(-1, -1, -1);
        gl.glVertex3f(0, 1, 0);
        gl.glVertex3f(0, -1, 1);
        
        gl.glColor3f(0, 0, 1);
        gl.glVertex3f(0, -1, 1);
        gl.glVertex3f(0, 1, 0);
        gl.glVertex3f(1, -1, -1);
        
        gl.glColor3f(1, 1, 0);
        gl.glVertex3f(1, -1, -1);
        gl.glVertex3f(0, 1, 0);
        gl.glVertex3f(-1, -1, -1);
        
        gl.glEnd();
    }

    private void drawCylinder(GL2 gl) {
        GLUquadric quadric = glu.gluNewQuadric();
        glu.gluQuadricDrawStyle(quadric, GLU.GLU_FILL);
        
        // Color principal del cilindro
        gl.glColor3f(0.2f, 0.6f, 1.0f);  // Azul claro
        glu.gluCylinder(quadric, 1, 1, 2, 64, 1);
        
        // Color diferente para las tapas
        gl.glColor3f(0.9f, 0.9f, 0.9f);  // Gris claro
        
        // Tapa inferior
        glu.gluDisk(quadric, 0, 1, 64, 1);
        
        // Tapa superior
        gl.glPushMatrix();
        gl.glTranslatef(0, 0, 2);
        glu.gluDisk(quadric, 0, 1, 64, 1);
        gl.glPopMatrix();
    }

    private void drawCone(GL2 gl) {
        GLUquadric cone = glu.gluNewQuadric();
        glu.gluQuadricDrawStyle(cone, GLU.GLU_FILL);
        glu.gluCylinder(cone, 1, 0, 2, 64, 1);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glViewport(0, 0, width, height);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        animator.stop();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EditorFiguras3D().setVisible(true));
    }
}