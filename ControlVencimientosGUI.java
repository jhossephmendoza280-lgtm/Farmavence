import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ControlVencimientosGUI extends JFrame {

    // ---------- CONSTANTES ----------
    static final int DIAS_URGENTE = 30;   // vence en 30 días o menos
    static final int DIAS_ALERTA = 60;    // vence entre 31 y 60 días
    static final Path ARCHIVO = Paths.get(System.getProperty("user.home"), "ControlVencimientos", "productos.csv");
    static final DateTimeFormatter FORMATO_ENTRADA =
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);
    static final DateTimeFormatter FORMATO_SALIDA = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    static final String[] ESTADOS = {"VENCIDO", "URGENTE", "POR VENCER", "OK"};

    // ---------- CLASE PRODUCTO ----------
    static class Producto {
        String nombre;
        String lote;
        int stock;
        LocalDate vencimiento;

        Producto(String nombre, String lote, int stock, LocalDate vencimiento) {
            this.nombre = nombre;
            this.lote = lote;
            this.stock = stock;
            this.vencimiento = vencimiento;
        }

        long diasRestantes() {
            return ChronoUnit.DAYS.between(LocalDate.now(), vencimiento);
        }
    }

    // 0 = vencido, 1 = urgente, 2 = por vencer, 3 = ok
    static int estado(Producto p) {
        long dias = p.diasRestantes();
        if (dias < 0) {
            return 0;
        } else if (dias <= DIAS_URGENTE) {
            return 1;
        } else if (dias <= DIAS_ALERTA) {
            return 2;
        }
        return 3;
    }

    // ---------- DATOS ----------
    private final List<Producto> productos = new ArrayList<>();
    private final List<Producto> visibles = new ArrayList<>(); // lo que se ve en la tabla
    private Producto enEdicion = null;

    // ---------- COMPONENTES ----------
    private final JTextField txtNombre = new JTextField(20);
    private final JTextField txtLote = new JTextField(10);
    private final JTextField txtFecha = new JTextField(10);
    private final JSpinner spStock = new JSpinner(new SpinnerNumberModel(1, 0, 100000, 1));
    private final JButton btnGuardar = new JButton("Agregar producto");
    private final JButton btnCancelar = new JButton("Cancelar edición");
    private final JButton btnLimpiar = new JButton("Limpiar");
    private final JButton btnEditar = new JButton("Editar seleccionado");
    private final JButton btnEliminar = new JButton("Eliminar seleccionado");
    private final JComboBox<String> cmbFiltro =
            new JComboBox<>(new String[]{"Ver todos los productos", "Ver solo alertas"});
    private final JLabel lblResumen = new JLabel(" ");
    private DefaultTableModel modelo;
    private JTable tabla;

    // ---------- CONSTRUCTOR ----------
    public ControlVencimientosGUI() {
        super("FarmaVence - Control de Vencimientos");
        setIconImage(crearIcono());
        cargar();
        construirInterfaz();
        refrescarTabla();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(980, 620);
        setLocationRelativeTo(null);
    }

    // ---------- ICONO DE LA VENTANA ----------
    private static Image crearIcono() {
        int s = 256;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color verde = new Color(0, 137, 123);
        g.setColor(verde);
        g.fillRoundRect(10, 10, 236, 236, 105, 105);       // fondo

        g.setColor(Color.WHITE);
        g.fillRoundRect(93, 50, 48, 134, 12, 12);           // cruz vertical
        g.fillRoundRect(50, 93, 134, 48, 12, 12);           // cruz horizontal

        g.setColor(verde);
        g.fillOval(180 - 58, 180 - 58, 116, 116);           // borde de la insignia
        g.setColor(new Color(255, 143, 0));
        g.fillOval(180 - 52, 180 - 52, 104, 104);           // insignia naranja

        g.setColor(Color.WHITE);
        g.fillRoundRect(171, 148, 18, 41, 8, 8);            // signo de exclamación
        g.fillOval(170, 197, 20, 20);

        g.dispose();
        return img;
    }

    // ---------- INTERFAZ ----------
    private void construirInterfaz() {
        setLayout(new BorderLayout(8, 8));

        // --- Formulario (arriba) ---
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Registrar producto (pasa aquí lo de tu cuaderno)",
                TitledBorder.LEFT, TitledBorder.TOP));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;

        agregar(form, c, new JLabel("Producto:"), 0, 0);
        agregar(form, c, txtNombre, 1, 0);
        agregar(form, c, new JLabel("Lote:"), 2, 0);
        agregar(form, c, txtLote, 3, 0);
        agregar(form, c, new JLabel("Stock:"), 0, 1);
        agregar(form, c, spStock, 1, 1);
        agregar(form, c, new JLabel("Vence (dd/mm/aaaa):"), 2, 1);
        agregar(form, c, txtFecha, 3, 1);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botones.add(btnGuardar);
        botones.add(btnCancelar);
        botones.add(btnLimpiar);
        btnCancelar.setVisible(false);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 4;
        form.add(botones, c);

        add(form, BorderLayout.NORTH);

        // --- Tabla (centro) ---
        String[] columnas = {"Producto", "Lote", "Stock", "Vence", "Días restantes", "Estado"};
        modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int col) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int col) {
                switch (col) {
                    case 2: return Integer.class;
                    case 3: return LocalDate.class;
                    case 4: return Long.class;
                    default: return String.class;
                }
            }
        };
        tabla = new JTable(modelo);
        tabla.setRowHeight(26);
        tabla.setFont(tabla.getFont().deriveFont(14f));
        tabla.getTableHeader().setFont(tabla.getFont().deriveFont(Font.BOLD, 14f));
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelo);
        tabla.setRowSorter(sorter);
        List<RowSorter.SortKey> orden = new ArrayList<>();
        orden.add(new RowSorter.SortKey(3, SortOrder.ASCENDING)); // el que vence primero, arriba
        sorter.setSortKeys(orden);

        ColorRenderer renderer = new ColorRenderer();
        tabla.setDefaultRenderer(Object.class, renderer);
        tabla.setDefaultRenderer(String.class, renderer);
        tabla.setDefaultRenderer(Integer.class, renderer);
        tabla.setDefaultRenderer(Long.class, renderer);
        tabla.setDefaultRenderer(LocalDate.class, renderer);

        add(new JScrollPane(tabla), BorderLayout.CENTER);

        // --- Barra inferior ---
        JPanel abajo = new JPanel(new BorderLayout());
        lblResumen.setFont(lblResumen.getFont().deriveFont(Font.BOLD, 14f));
        lblResumen.setBorder(BorderFactory.createEmptyBorder(4, 10, 8, 10));
        abajo.add(lblResumen, BorderLayout.WEST);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        acciones.add(cmbFiltro);
        acciones.add(btnEditar);
        acciones.add(btnEliminar);
        abajo.add(acciones, BorderLayout.EAST);
        add(abajo, BorderLayout.SOUTH);

        // --- Eventos ---
        btnGuardar.addActionListener(e -> guardarProducto());
        txtFecha.addActionListener(e -> guardarProducto());
        btnCancelar.addActionListener(e -> limpiarFormulario());
        btnLimpiar.addActionListener(e -> limpiarFormulario());
        btnEditar.addActionListener(e -> editarSeleccionado());
        btnEliminar.addActionListener(e -> eliminarSeleccionado());
        cmbFiltro.addActionListener(e -> refrescarTabla());
    }

    private void agregar(JPanel panel, GridBagConstraints c, Component comp, int x, int y) {
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = 1;
        panel.add(comp, c);
    }

    // Pinta cada fila según qué tan cerca está de vencer
    private class ColorRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor,
                boolean seleccionado, boolean foco, int fila, int col) {
            if (valor instanceof LocalDate) {
                valor = ((LocalDate) valor).format(FORMATO_SALIDA);
            }
            Component comp = super.getTableCellRendererComponent(t, valor, seleccionado, foco, fila, col);
            setHorizontalAlignment((col == 2 || col == 4 || col == 5) ? CENTER : LEFT);

            if (!seleccionado) {
                int filaModelo = t.convertRowIndexToModel(fila);
                Producto p = visibles.get(filaModelo);
                switch (estado(p)) {
                    case 0: comp.setBackground(new Color(255, 160, 160)); break; // vencido
                    case 1: comp.setBackground(new Color(255, 200, 140)); break; // urgente
                    case 2: comp.setBackground(new Color(255, 245, 150)); break; // por vencer
                    default: comp.setBackground(Color.WHITE);
                }
                comp.setForeground(Color.BLACK);
            }
            return comp;
        }
    }

    // ---------- ACCIONES ----------
    private void guardarProducto() {
        String nombre = txtNombre.getText().trim().replace(',', ' ');
        String lote = txtLote.getText().trim().replace(',', ' ');
        int stock = (Integer) spStock.getValue();

        if (nombre.isEmpty()) {
            error("Escribe el nombre del producto.");
            txtNombre.requestFocus();
            return;
        }
        if (lote.isEmpty()) {
            lote = "S/L"; // sin lote
        }

        LocalDate fecha;
        try {
            fecha = LocalDate.parse(txtFecha.getText().trim(), FORMATO_ENTRADA);
        } catch (DateTimeParseException ex) {
            error("La fecha no es válida. Usa el formato dd/mm/aaaa (ej. 15/03/2027).");
            txtFecha.requestFocus();
            return;
        }

        if (enEdicion != null) {
            enEdicion.nombre = nombre;
            enEdicion.lote = lote;
            enEdicion.stock = stock;
            enEdicion.vencimiento = fecha;
        } else {
            productos.add(new Producto(nombre, lote, stock, fecha));
        }
        guardar();
        limpiarFormulario();
        refrescarTabla();
        txtNombre.requestFocus();
    }

    private void editarSeleccionado() {
        Producto p = productoSeleccionado();
        if (p == null) {
            error("Primero selecciona un producto de la tabla.");
            return;
        }
        enEdicion = p;
        txtNombre.setText(p.nombre);
        txtLote.setText(p.lote);
        spStock.setValue(p.stock);
        txtFecha.setText(p.vencimiento.format(FORMATO_SALIDA));
        btnGuardar.setText("Guardar cambios");
        btnCancelar.setVisible(true);
    }

    private void eliminarSeleccionado() {
        Producto p = productoSeleccionado();
        if (p == null) {
            error("Primero selecciona un producto de la tabla.");
            return;
        }
        int r = JOptionPane.showConfirmDialog(this,
                "¿Eliminar \"" + p.nombre + "\" (lote " + p.lote + ")?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            productos.remove(p);
            if (p == enEdicion) {
                limpiarFormulario();
            }
            guardar();
            refrescarTabla();
        }
    }

    private Producto productoSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return null;
        }
        return visibles.get(tabla.convertRowIndexToModel(fila));
    }

    private void limpiarFormulario() {
        enEdicion = null;
        txtNombre.setText("");
        txtLote.setText("");
        txtFecha.setText("");
        spStock.setValue(1);
        btnGuardar.setText("Agregar producto");
        btnCancelar.setVisible(false);
    }

    private void error(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Dato incorrecto", JOptionPane.ERROR_MESSAGE);
    }

    // ---------- TABLA Y ALERTAS ----------
    private void refrescarTabla() {
        modelo.setRowCount(0);
        visibles.clear();

        List<Producto> ordenados = new ArrayList<>(productos);
        ordenados.sort(Comparator.comparing(p -> p.vencimiento));
        boolean soloAlertas = cmbFiltro.getSelectedIndex() == 1;
        int[] cont = new int[4];

        for (Producto p : ordenados) {
            int e = estado(p);
            cont[e]++;
            if (soloAlertas && e == 3) {
                continue;
            }
            visibles.add(p);
            modelo.addRow(new Object[]{p.nombre, p.lote, p.stock, p.vencimiento,
                    p.diasRestantes(), ESTADOS[e]});
        }

        lblResumen.setText("Vencidos: " + cont[0] + "   |   Urgentes: " + cont[1]
                + "   |   Por vencer: " + cont[2] + "   |   Total: " + productos.size());
        if (cont[0] > 0) {
            lblResumen.setForeground(new Color(180, 0, 0));
        } else if (cont[1] > 0) {
            lblResumen.setForeground(new Color(200, 100, 0));
        } else {
            lblResumen.setForeground(Color.BLACK);
        }
    }

    // Aviso que aparece apenas se abre el programa
    private void mostrarAlertaInicial() {
        List<Producto> ordenados = new ArrayList<>(productos);
        ordenados.sort(Comparator.comparing(p -> p.vencimiento));

        StringBuilder sb = new StringBuilder();
        int total = 0;
        for (Producto p : ordenados) {
            int e = estado(p);
            if (e == 3) {
                continue;
            }
            total++;
            if (total <= 12) {
                long d = p.diasRestantes();
                String texto = (d < 0) ? "VENCIDO hace " + (-d) + " días" : "vence en " + d + " días";
                sb.append("• ").append(p.nombre).append(" (lote ").append(p.lote)
                        .append(") - ").append(texto).append("\n");
            }
        }
        if (total == 0) {
            return;
        }
        if (total > 12) {
            sb.append("... y ").append(total - 12).append(" más (míralos en la tabla)\n");
        }
        JOptionPane.showMessageDialog(this,
                "Hay " + total + " producto(s) que requieren atención:\n\n" + sb,
                "¡ALERTA DE VENCIMIENTOS!", JOptionPane.WARNING_MESSAGE);
    }

    // ---------- GUARDAR / CARGAR ----------
    private void guardar() {
        List<String> lineas = new ArrayList<>();
        for (Producto p : productos) {
            lineas.add(p.nombre + "," + p.lote + "," + p.stock + "," + p.vencimiento.format(FORMATO_SALIDA));
        }
        try {
            Files.createDirectories(ARCHIVO.getParent());
            Files.write(ARCHIVO, lineas, StandardCharsets.UTF_8);
        } catch (IOException e) {
            error("No se pudo guardar el archivo: " + e.getMessage());
        }
    }

    private void cargar() {
        if (!Files.exists(ARCHIVO)) {
            return;
        }
        try {
            for (String linea : Files.readAllLines(ARCHIVO, StandardCharsets.UTF_8)) {
                String[] d = linea.split(",");
                if (d.length == 4) {
                    productos.add(new Producto(d[0], d[1], Integer.parseInt(d[2]),
                            LocalDate.parse(d[3], FORMATO_ENTRADA)));
                }
            }
        } catch (IOException | RuntimeException e) {
            JOptionPane.showMessageDialog(null, "No se pudo leer productos.csv: " + e.getMessage());
        }
    }

    // ---------- PROGRAMA PRINCIPAL ----------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignorada) {
                // si falla, usa el aspecto por defecto
            }
            ControlVencimientosGUI ventana = new ControlVencimientosGUI();
            ventana.setVisible(true);
            ventana.mostrarAlertaInicial();
        });
    }
}
