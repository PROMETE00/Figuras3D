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
    private JPanel panelControles, panelColor ,  panelSesgado;
    private JSlider[] sliders;
    private GLJPanel glPanel;
    private JSlider sliderSesgoX, sliderSesgoY;
    private float rotationX = 0, rotationY = 0;
    private float translateX = 0, translateY = 0, translateZ = -5;
    private float scale = 1;
    private float shearX = 0.0f, shearY = 0.0f;
    private int currentShape = 0;
    private float[] colorValues = new float[4];
    private GLU glu = new GLU();
    private FPSAnimator animator;

    public EditorFiguras3D() {
        configurarVentana();
        inicializarComponentes();
        configurarEventos();
    }

    private void configurarVentana() {
        setTitle("Editor de Figuras 3D");
        setSize(1080, 920);
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
        
        String[] modelosColor = {"RGB", "CMYK" , "HSL" , "HSV"};
        selectorColor = crearComboBox(modelosColor, new Color(80, 160, 120));
        
        String[] transformaciones = {"Rotación", "Escala", "Traslación" , "Sesgado"};
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

        panelSesgado = new JPanel(new GridLayout(2, 1, 5, 5));
        panelSesgado.setBorder(new TitledBorder("Controles de Sesgado"));
        panelSesgado.setBackground(new Color(70, 70, 70));
        sliderSesgoX = crearSlider(-100, 100, "Sesgo X");
        sliderSesgoY = crearSlider(-100, 100, "Sesgo Y");
        panelSesgado.add(sliderSesgoX);
        panelSesgado.add(sliderSesgoY);
        panelSesgado.setVisible(false);
        add(panelSesgado, BorderLayout.SOUTH);

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
        JSlider slider = new JSlider(min, max, 0);
        slider.setBackground(new Color(70, 70, 70));
        slider.setForeground(Color.WHITE);
        slider.setBorder(BorderFactory.createTitledBorder(titulo));
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
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
        
        // En el método configurarEventos():
selectorTransform.addActionListener(e -> {
    boolean mostrarSesgado = selectorTransform.getSelectedIndex() == 3;
    panelSesgado.setVisible(mostrarSesgado);
    if(mostrarSesgado) {
        // Resetear valores al seleccionar sesgado
        shearX = 0.0f;
        shearY = 0.0f;
        sliderSesgoX.setValue(0);
        sliderSesgoY.setValue(0);
    }
    glPanel.repaint();
});

        sliderSesgoX.addChangeListener(e -> {
            shearX = sliderSesgoX.getValue() / 100.0f;
            glPanel.repaint();
        });

        sliderSesgoY.addChangeListener(e -> {
            shearY = sliderSesgoY.getValue() / 100.0f;
            glPanel.repaint();
        });
        MouseAdapter mouseAdapter = new MouseAdapter() {
            private Point lastPoint;
            
            public void mousePressed(MouseEvent e) {
                lastPoint = e.getPoint();
            }
            
            public void mouseDragged(MouseEvent e) {
                int transformIndex = selectorTransform.getSelectedIndex();
                if(transformIndex == 3) return; // No usar mouse para sesgado

                int dx = e.getX() - lastPoint.x;
                int dy = e.getY() - lastPoint.y;
                
                switch(transformIndex) {
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
        String[] etiquetas = obtenerEtiquetasColor(modelo);
        
        // Crear sliders según el modelo
        int componentes = etiquetas.length;
        colorValues = new float[4]; // Aumentar a 4 componentes
        
        for(int i = 0; i < componentes; i++) {
            JSlider slider = crearSlider(0, 100, etiquetas[i]);
            slider.setValue((int)(colorValues[i] * 100));
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
    
    private String[] obtenerEtiquetasColor(String modelo) {
        return switch (modelo) {
            case "RGB" -> new String[]{"Rojo", "Verde", "Azul"};
            case "CMYK" -> new String[]{"Cian", "Magenta", "Amarillo", "Negro"};
            case "HSL" -> new String[]{"Tono", "Saturación", "Luminosidad"};
            case "HSV" -> new String[]{"Tono", "Saturación", "Valor"};
            default -> new String[0];
        };
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
        
        // Aplicar sesgado si está seleccionado
        if(selectorTransform.getSelectedIndex() == 3) {
        // Calcular punto de anclaje (esquina inferior izquierda en 3D)
        float anchorX = -1.0f; // Coordenada X mínima del cubo
        float anchorY = -1.0f; // Coordenada Y mínima del cubo
        float anchorZ = -1.0f; // Coordenada Z mínima del cubo
        
        // Matriz de transformación compuesta
            gl.glTranslatef(anchorX, anchorY, anchorZ); // Mover al origen
        float[] shearMatrix = {
            1.0f, shearY, 0.0f, 0.0f,
            shearX, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        };
            gl.glMultMatrixf(shearMatrix, 0);
            gl.glTranslatef(-anchorX, -anchorY, -anchorZ); // Devolver a posición
        }
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
        return switch (colorModel) {
            case 0 -> new Color(values[0], values[1], values[2]); // RGB
            case 1 -> cmykToRgb(values); // CMYK
            case 2 -> hslToRgb(values[0], values[1], values[2]); // HSL
            case 3 -> Color.getHSBColor(values[0], values[1], values[2]); // HSV
            default -> Color.WHITE;
        };
    }
    
    // Nuevo método para conversión CMYK a RGB
    private Color cmykToRgb(float[] cmyk) {
        float c = cmyk[0];
        float m = cmyk[1];
        float y = cmyk[2];
        float k = cmyk[3];
        
        float r = (1 - Math.min(1, c * (1 - k) + k));
        float g = (1 - Math.min(1, m * (1 - k) + k));
        float b = (1 - Math.min(1, y * (1 - k) + k));
        
        return new Color(r, g, b);
    }
    
    private Color hslToRgb(float h, float s, float l) {
        float r, g, b;
    
        if (s == 0f) {
            r = g = b = l; // escala de grises
        } else {
            float q = l < 0.5f ? l * (1 + s) : l + s - l * s;
            float p = 2 * l - q;
            r = hueToRgb(p, q, h + 1f/3f);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1f/3f);
        }
        return new Color(r, g, b);
    }
    
    private float hueToRgb(float p, float q, float t) {
        if (t < 0f) t += 1f;
        if (t > 1f) t -= 1f;
        if (t < 1f/6f) return p + (q - p) * 6f * t;
        if (t < 1f/2f) return q;
        if (t < 2f/3f) return p + (q - p) * (2f/3f - t) * 6f;
        return p;
    }

    private void drawCube(GL2 gl) {
        gl.glBegin(GL2.GL_QUADS);
        float size = 1.0f;
        
        // Cara frontal
        gl.glVertex3f(-size, -size, size);
        gl.glVertex3f(size, -size, size);
        gl.glVertex3f(size, size, size);
        gl.glVertex3f(-size, size, size);
        
        // Cara trasera
        gl.glVertex3f(-1, -1, -1);
        gl.glVertex3f(-1, 1, -1);
        gl.glVertex3f(1, 1, -1);
        gl.glVertex3f(1, -1, -1);
        
        // Cara superior
        gl.glVertex3f(-1, 1, -1);
        gl.glVertex3f(-1, 1, 1);
        gl.glVertex3f(1, 1, 1);
        gl.glVertex3f(1, 1, -1);
        
        // Cara inferior
        gl.glVertex3f(-1, -1, -1);
        gl.glVertex3f(1, -1, -1);
        gl.glVertex3f(1, -1, 1);
        gl.glVertex3f(-1, -1, 1);
        
        // Cara izquierda
        gl.glVertex3f(-1, -1, -1);
        gl.glVertex3f(-1, -1, 1);
        gl.glVertex3f(-1, 1, 1);
        gl.glVertex3f(-1, 1, -1);
        
        // Cara derecha
        gl.glVertex3f(1, -1, -1);
        gl.glVertex3f(1, 1, -1);
        gl.glVertex3f(1, 1, 1);
        gl.glVertex3f(1, -1, 1);
        
        gl.glEnd();
    }

    private void drawSphere(GL2 gl) {
    //gl.glColor3f(1, 0.5f, 0);
    GLUquadric sphere = glu.gluNewQuadric();
    glu.gluQuadricDrawStyle(sphere, GLU.GLU_LINE); // Modo wireframe
    glu.gluSphere(sphere, 1, 50, 50); // Más segmentos 
    glu.gluDeleteQuadric(sphere);
}

private void drawPyramid(GL2 gl) {
    gl.glBegin(GL2.GL_TRIANGLES);
    
    // Base
    gl.glVertex3f(-1, -1, -1);
    gl.glVertex3f(1, -1, -1);
    gl.glVertex3f(0, -1, 1);
    
    // Caras laterales
    gl.glVertex3f(-1, -1, -1);
    gl.glVertex3f(0, 1, 0);
    gl.glVertex3f(0, -1, 1);
    
    gl.glVertex3f(0, -1, 1);
    gl.glVertex3f(0, 1, 0);
    gl.glVertex3f(1, -1, -1);
    
    gl.glVertex3f(1, -1, -1);
    gl.glVertex3f(0, 1, 0);
    gl.glVertex3f(-1, -1, -1);
    
    gl.glEnd();
}

private void drawCylinder(GL2 gl) {
    GLUquadric quadric = glu.gluNewQuadric();
    glu.gluQuadricDrawStyle(quadric, GLU.GLU_FILL);
    
    // Centrar el cilindro en el origen
    gl.glPushMatrix();
    gl.glTranslatef(0.0f, 0.0f, -1.0f);  // Mover hacia abajo la mitad de su altura
    
    glu.gluCylinder(quadric, 1, 1, 2, 64, 1);
    
    // Tapa inferior
    glu.gluDisk(quadric, 0, 1, 64, 1);
    
    // Tapa superior
    gl.glPushMatrix();
    gl.glTranslatef(0, 0, 2);
    glu.gluDisk(quadric, 0, 1, 64, 1);
    gl.glPopMatrix();
    
    gl.glPopMatrix();
    glu.gluDeleteQuadric(quadric);
}

private void drawCone(GL2 gl) {
    GLUquadric cone = glu.gluNewQuadric();
    glu.gluQuadricDrawStyle(cone, GLU.GLU_FILL);
    
    // Centrar el cono en el origen
    gl.glPushMatrix();
    gl.glTranslatef(0.0f, 0.0f, -1.0f);  // Mover hacia abajo la mitad de su altura
    glu.gluCylinder(cone, 1, 0, 2, 64, 1);
    
    gl.glPopMatrix();
    glu.gluDeleteQuadric(cone);
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