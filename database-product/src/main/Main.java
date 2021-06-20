/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialLighterContrastIJTheme;
import connection.DatabaseConnection;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import model.Model_Category;

/**
 *
 * @author RAVEN
 */
public class Main extends javax.swing.JFrame {

    /**
     * Creates new form Main
     */
    private DefaultTableModel model;
    private ResultSet resultSet;
    private boolean allowEdit;

    public Main() {
        initComponents();
        init();
        customJtable();
    }

    private void init() {
        try {
            DatabaseConnection.getInstance().connectToDatabase();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void customJtable() {
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable jtable, Object o, boolean bln, boolean bln1, int row, int column) {
                super.getTableCellRendererComponent(jtable, o, bln, bln1, row, column);
                if (column != 4) {
                    setHorizontalAlignment(JLabel.LEFT);
                } else {
                    setHorizontalAlignment(JLabel.CENTER);
                }
                return this;
            }
        });
        table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(new JTextField()));
        //  user textarea to column 2
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable jtable, Object o, boolean selected, boolean bln1, int row, int column) {
                if (column == 2) {
                    JTextArea txt = new JTextArea(o + "");
                    txt.setWrapStyleWord(true);
                    txt.setLineWrap(true);
                    txt.setBackground(getBackground());
                    JScrollPane sp = new JScrollPane(txt);
                    sp.getVerticalScrollBar().setPreferredSize(new Dimension(5, 5));
                    sp.setBorder(new EmptyBorder(0, 0, selected ? 0 : 1, 0));
                    return sp;
                } else {
                    //  remove border
                    super.getTableCellRendererComponent(jtable, o, selected, bln1, row, column);
                    setBorder(new EmptyBorder(1, 5, 1, 5));
                    return this;
                }
            }
        });
        //  set editable to column 2 as jtextarea
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            private final JTextArea txt = new JTextArea();

            @Override
            public Component getTableCellEditorComponent(JTable jtable, Object o, boolean bln, int i, int i1) {
                txt.setText(o + "");
                JScrollPane sp = new JScrollPane(txt);
                sp.getVerticalScrollBar().setPreferredSize(new Dimension(5, 5));
                txt.setBackground(jtable.getSelectionBackground());
                txt.setWrapStyleWord(true);
                txt.setLineWrap(true);
                sp.setBorder(null);
                return sp;
            }

            @Override
            public Object getCellEditorValue() {
                return txt.getText();
            }

        });
        //  set combobox to column 3
        JComboBox combo = new JComboBox();
        getCategory(combo);
        table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(combo));
        //  set image view for column 4
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable jtable, Object o, boolean selected, boolean bln1, int i, int i1) {
                Component com = super.getTableCellRendererComponent(jtable, o, selected, bln1, i, i1);
                if (o == null) {
                    //  No Image
                    JLabel label = new JLabel("No Image");
                    label.setHorizontalAlignment(JLabel.CENTER);
                    label.setOpaque(selected);
                    label.setBackground(com.getBackground());
                    return label;
                } else {
                    if (o instanceof Icon) {
                        //  Has Image
                        Icon image = (ImageIcon) o;
                        JLabel label = new JLabel(image);
                        label.setHorizontalAlignment(JLabel.CENTER);
                        label.setOpaque(selected);
                        label.setBackground(com.getBackground());
                        return label;
                    } else {
                        //  Image updating
                        JLabel label = new JLabel("Updating ...");
                        label.setHorizontalAlignment(JLabel.CENTER);
                        label.setOpaque(selected);
                        label.setBackground(com.getBackground());
                        return label;
                    }
                }
            }
        });
        //  add Table even
        //  this event work when cell editor change
        table.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent tme) {
                if (allowEdit) {
                    try {
                        int column = tme.getColumn();
                        int row = tme.getFirstRow();
                        if (column == 3) {
                            Model_Category c = (Model_Category) table.getValueAt(row, column);
                            if (c != null) {
                                if (c.getCategoryID() == -1) {
                                    //  user select add new category
                                    String text = JOptionPane.showInputDialog(Main.this, "New Category");
                                    if (text != null) {
                                        Model_Category cate = insertCategory(text);
                                        combo.insertItemAt(cate, combo.getItemCount() - 1);
                                        table.setValueAt(cate, row, column);
                                    }
                                } else {
                                    //  update database
                                    resultSet.absolute(row + 1);
                                    resultSet.updateInt(column + 1, c.getCategoryID());
                                    resultSet.updateRow();
                                }
                            }
                        } else if (column == 5) {
                            //  Update status
                            boolean status = (boolean) table.getValueAt(row, column);
                            resultSet.absolute(row + 1);
                            resultSet.updateString(column + 1, status ? "1" : "0");
                            resultSet.updateRow();
                        } else if (column == 1 || column == 2) {
                            //  update name and description
                            resultSet.absolute(row + 1);
                            resultSet.updateString(column + 1, table.getValueAt(row, column).toString());
                            resultSet.updateRow();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        menu = new javax.swing.JPopupMenu();
        menuAdd = new javax.swing.JMenuItem();
        menuDelete = new javax.swing.JMenuItem();
        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();

        menuAdd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/add.png"))); // NOI18N
        menuAdd.setText("Add New");
        menuAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuAddActionPerformed(evt);
            }
        });
        menu.add(menuAdd);

        menuDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/delete.png"))); // NOI18N
        menuDelete.setText("Delete");
        menuDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuDeleteActionPerformed(evt);
            }
        });
        menu.add(menuDelete);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "No", "Name", "Description", "Category", "Image", "Status"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        table.setRowHeight(70);
        table.setShowHorizontalLines(true);
        table.setSurrendersFocusOnKeystroke(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableMouseClicked(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableMouseReleased(evt);
            }
        });
        jScrollPane1.setViewportView(table);
        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setPreferredWidth(30);
            table.getColumnModel().getColumn(1).setPreferredWidth(200);
            table.getColumnModel().getColumn(2).setPreferredWidth(300);
            table.getColumnModel().getColumn(5).setMinWidth(50);
            table.getColumnModel().getColumn(5).setPreferredWidth(50);
            table.getColumnModel().getColumn(5).setMaxWidth(50);
        }

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1149, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 555, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        try {
            allowEdit = false;
            model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);
            PreparedStatement p = DatabaseConnection.getInstance().getConnection().prepareStatement("select * from product", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            resultSet = p.executeQuery();
            while (resultSet.next()) {
                String name = resultSet.getString(2);
                String description = resultSet.getString(3);
                int categoryID = resultSet.getInt(4);
                Icon image = null;
                if (resultSet.getObject(5) != null) {
                    image = scaledImage(new ImageIcon(resultSet.getBytes(5)));
                }
                boolean status = resultSet.getInt(6) == 1;
                model.addRow(new Object[]{table.getRowCount() + 1, name, description, getCategory(categoryID), image, status});
            }
            allowEdit = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }//GEN-LAST:event_formWindowOpened

    private void tableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMouseReleased
        if (SwingUtilities.isRightMouseButton(evt)) {
            menu.show(table, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_tableMouseReleased

    private void menuAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuAddActionPerformed
        //  Add new row to database
        try {
            resultSet.moveToInsertRow();
            resultSet.updateInt(4, 0);
            resultSet.insertRow();
            model.addRow(new Object[]{table.getRowCount() + 1, "", "", new Model_Category(0, ""), null, true});
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }//GEN-LAST:event_menuAddActionPerformed

    private void menuDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuDeleteActionPerformed
        //  Delete rows
        try {
            int rows[] = table.getSelectedRows();
            for (int i = 0; i < rows.length; i++) {
                int row = rows[i] - i;
                resultSet.absolute(row + 1);    //  absolute use to move cusor resultset to "row"
                resultSet.deleteRow();
                model.removeRow(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }//GEN-LAST:event_menuDeleteActionPerformed

    private void tableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMouseClicked
        //  Select Image for update
        if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2 && table.getSelectedColumn() == 4) {
            selectImage();
        }
    }//GEN-LAST:event_tableMouseClicked

    private List<Model_Category> category;

    private void getCategory(JComboBox combo) {
        category = new ArrayList<>();
        try {
            combo.removeAllItems();
            PreparedStatement p = DatabaseConnection.getInstance().getConnection().prepareStatement("select * from category");
            ResultSet r = p.executeQuery();
            while (r.next()) {
                int categoryID = r.getInt(1);
                String categoryName = r.getString(2);
                Model_Category c = new Model_Category(categoryID, categoryName);
                category.add(c);
                combo.addItem(c);
            }
            combo.addItem(new Model_Category(-1, "-- New --"));
            r.close();
            p.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Model_Category getCategory(int categoryID) {
        for (Model_Category c : category) {
            if (c.getCategoryID() == categoryID) {
                return c;
            }
        }
        return null;
    }

    private Model_Category insertCategory(String categoryName) throws SQLException {
        PreparedStatement p = DatabaseConnection.getInstance().getConnection().prepareStatement("insert into category (CategoryName) values (?)", PreparedStatement.RETURN_GENERATED_KEYS);
        p.setString(1, categoryName);
        p.execute();
        ResultSet r = p.getGeneratedKeys();
        r.next();
        int categoryID = r.getInt(1);
        r.close();
        p.close();
        Model_Category c = new Model_Category(categoryID, categoryName);
        category.add(c);
        return c;
    }

    private void selectImage() {
        JFileChooser ch = new JFileChooser();
        ch.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File file) {
                String name = file.getName().toLowerCase();
                return file.isDirectory() || name.endsWith(".jpg") || name.endsWith(".png");
            }

            @Override
            public String getDescription() {
                return "Image";
            }
        });
        int opt = ch.showOpenDialog(this);
        if (opt == JFileChooser.APPROVE_OPTION) {
            File file = ch.getSelectedFile();
            int row = table.getSelectedRow();
            int column = table.getSelectedColumn();
            table.setValueAt("Updating ...", row, column);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000); //  for testing
                        resultSet.absolute(row + 1);
                        resultSet.updateBlob(column + 1, Files.newInputStream(file.toPath()));
                        resultSet.updateRow();
                        Icon image = scaledImage(new ImageIcon(file.getAbsolutePath()));
                        table.setValueAt(image, row, column);
                    } catch (IOException | InterruptedException | SQLException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private ImageIcon scaledImage(Icon image) {
        int h = image.getIconHeight();
        int toH = 65;
        if (h < toH) {
            toH = h;
        }
        return new ImageIcon(((ImageIcon) image).getImage().getScaledInstance(-1, toH, Image.SCALE_SMOOTH));
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        FlatMaterialLighterContrastIJTheme.setup();
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Main().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPopupMenu menu;
    private javax.swing.JMenuItem menuAdd;
    private javax.swing.JMenuItem menuDelete;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables
}
