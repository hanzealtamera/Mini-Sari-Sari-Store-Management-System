/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package gui;

/**
 *
 * @author Lenovo
 */
import javax.swing.JOptionPane; //popup
import javax.swing.table.DefaultTableModel; //pra ma change datas sa table
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.PreparedStatement;//MA ILISDAN AN ????
import java.sql.SQLException;

public class Dashbard extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Dashbard.class.getName());    //PRA MA record mga erroer
//accesisable lng dni na class - priv
 //dani lng gyud ssha na class - static
//final- d sha pede ma assign sa lain na logger kay dni lng sha, loyal - finaal
    /**
    
     */
        public Dashbard() {     //constrctor, mu dagan f naay bbag o na dasshbard
            initComponents();   //metod call,, aron makita ang mga buttons, txtfiled, lbel, chuchu sa screen
            qntity.addActionListener(this::qntityActionPerformed); //e conek an quantiy na fild to its event-handling method.
         

            setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH);    //e max ang dasbord window
            setLocationRelativeTo(null);    //center ang window sa scren
        }
    
                                                  //parameter, mag tell sha kung asa  na transaction typ e process
        private boolean processTransaction(boolean isCash) {    //fancsion na mu proses sa cash ug utang , naay t or f
            String item = items.getText().trim();   //kwaon produc sa textfield
            java.sql.Date dueDateValue = null;  //variable para sa due date men
    
    

        if (!isCash) {
            try {
                dueDateValue = java.sql.Date.valueOf(
                duedate.getText().trim());
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this,
                    "Enter due date as YYYY-MM-DD.");
                return false;
            }
        }

        try {
            int qty = Integer.parseInt(qntity.getText().trim());        //Kuhaon ang quantity

            if (item.isEmpty() || qty <= 0) {   //f wa sulod
                JOptionPane.showMessageDialog(this, "Enter item and a valid quantity.");
                return false;   //ipa stop
            }

            DefaultTableModel productModel = ProductFrame.productModel; //product table model gikan sa ProductFrame aron ma-access ang mga produkto.
            int row = -1;       //-1 means wla nakitta ang ow sa produk

            for (int i = 0; i < productModel.getRowCount(); i++) {  //loop LINEAR SEARCH
                Object value = productModel.getValueAt(i, 1);       //kwaon value

                if (value != null && value.toString().equalsIgnoreCase(item)) { //kung naa ang value ug parehas ba sa product name nga gi-input
                    row = i;
                    break;
                }
            }

            if (row == -1) {    //f wla nkita an product
                JOptionPane.showMessageDialog(this, "Product not found.");
                return false;
            }   //CONDITIONAL STATEMENT ni sila

            int id = Integer.parseInt(productModel.getValueAt(row, 0).toString());  //haon ang product ID
            double price = Double.parseDouble(productModel.getValueAt(row, 3).toString());
            int stock = Integer.parseInt(productModel.getValueAt(row, 4).toString());

            if (qty > stock) {  //dcides if mas dghan ma ang request keysa sa sToc
                JOptionPane.showMessageDialog(this, "Not enough stock.");
                return false;
            }

            if (!isCash && (lastname.getText().trim().isEmpty() //f di ba emotyt
                || firctname.getText().trim().isEmpty()
                || cntactno.getText().trim().isEmpty()
                || duedate.getText().trim().isEmpty())) {
                JOptionPane.showMessageDialog(this,
                    "Complete customer information and due date.");
                return false;
                } //CONDITONAL STTATEMENTT KAY NAAY IF-THEN      

                double totalAmount = price * qty;
                int newStock = stock - qty;

        
        String updateStockSQL =
            "UPDATE product SET stock = ? WHERE product_id = ?";

        int savedUtangId = -1;  //PRRA DI MA APIL ANG ID

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                JOptionPane.showMessageDialog(this,
                    "Database connection failed.");
                        return false;
            }

        con.setAutoCommit(false);

        try {
        
        if (isCash) {
                // SAVE CASH SALE TO MYSQL
            String saleSQL =
                "INSERT INTO sale "
                + "(product_id, quantity, amount) "
                + "VALUES (?, ?, ?)";

         try (PreparedStatement pst =
            con.prepareStatement(saleSQL)) {

            pst.setInt(1, id);
            pst.setInt(2, qty);
            pst.setDouble(3, totalAmount);

            pst.executeUpdate();
            }

        } else {
    // SAVE CUSTOMER AND UTANG TO MYSQL
    int customerId = saveOrGetCustomer(con);

    String utangSQL =
            "INSERT INTO utang_record "
            + "(customer_id, product_id, quantity, "
            + "amount, amount_paid, due_date, status) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    try (PreparedStatement pst =
            con.prepareStatement(utangSQL,
                    Statement.RETURN_GENERATED_KEYS)) {     //request nga makuha ang ID na automatic

        pst.setInt(1, customerId);
        pst.setInt(2, id);
        pst.setInt(3, qty);
        pst.setDouble(4, totalAmount);
        pst.setDouble(5, 0.0);
        pst.setDate(6, dueDateValue);
        pst.setString(7, "unpaid");

        pst.executeUpdate();    //e inset sa database men

        try (ResultSet rs = pst.getGeneratedKeys()) {
            if (rs.next()) {
                savedUtangId = rs.getInt(1);
            } else {
                throw new SQLException( //kun walay id nna makuha kay exception iya e kuan
                        "Could not get utang ID.");
            }
        }
    }
}

        // UPDATE PRODUCT STOCK IN MYSQL
        try (PreparedStatement pst =
                con.prepareStatement(updateStockSQL)) {

            pst.setInt(1, newStock);    //bgo na value ang ?
            pst.setInt(2, id);

            if (pst.executeUpdate() != 1) {
                throw new SQLException(
                        "Product stock was not updated.");
            }
        }

        con.commit();   //g confirm para ma save sa databae

        } catch (SQLException | IllegalArgumentException e) {
            con.rollback(); //g undo mga wala pa na commit na datas/infos
            throw e;        //pra ma display kung unsay error
        } finally {
            con.setAutoCommit(true);
        }
        
    }
        productModel.setValueAt(newStock, row, 4);  //update ang stock nga makita sa product table

        if (isCash) {
            StoreData.sales.addRow(new Object[]{    //ARRAY
                StoreData.saleID++, item, qty, totalAmount
            }); //kng cash ang transaction, magdugang og row sa sales table
        } else {    //KNG utang ang transaction, idugang sa Utang table
            StoreData.utang.addRow(new Object[]{
                savedUtangId,
                lastname.getText().trim(),
                firctname.getText().trim(),
                cntactno.getText().trim(),
                item,
                totalAmount,
                0.0,
                duedate.getText().trim(),
                "unpaid"
            });
        }

        JOptionPane.showMessageDialog(this,
                "Transaction recorded!\nTotal amount: ₱"
                + String.format("%.2f", totalAmount));

        clearFields();
        return true;

    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this,
                "Please enter a valid quantity or product data.");
        return false;
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this,
                "Database error: " + e.getMessage());
        return false;
    }
}


private int saveOrGetCustomer(Connection con) throws SQLException {
    String contact = cntactno.getText().trim();
    String checkSQL = "SELECT customer_id FROM customer WHERE contact_number = ?";
    try (PreparedStatement pst = con.prepareStatement(checkSQL)) {
        pst.setString(1, contact);  //1 = ?
        try (ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("customer_id");
            }   //ng naa na ang customer, kuhaon ra niya ang existing customer_id. DI na siya  need insert og duplicate customer id
        }
    }

    String insertSQL = "INSERT INTO customer " +
            "(customer_lastname, customer_firstname, contact_number) " +
            "VALUES (?, ?, ?)";

    try (PreparedStatement pst = con.prepareStatement(
            insertSQL, Statement.RETURN_GENERATED_KEYS)) {

        pst.setString(1, lastname.getText().trim());
        pst.setString(2, firctname.getText().trim());
        pst.setString(3, contact);

        pst.executeUpdate();

        try (ResultSet rs = pst.getGeneratedKeys()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
    }

    throw new SQLException("Could not save customer.");
}

private void clearFields() {    //meathod
    lastname.setText("");
    firctname.setText("");
    cntactno.setText("");
    duedate.setText("");
    items.setText("");
    qntity.setText("");
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jPanel7 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        lastname = new javax.swing.JTextField();
        firctname = new javax.swing.JTextField();
        cntactno = new javax.swing.JTextField();
        duedate = new javax.swing.JTextField();
        items = new javax.swing.JTextField();
        qntity = new javax.swing.JTextField();
        cash = new javax.swing.JButton();
        utang = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        product = new javax.swing.JButton();
        utngrec = new javax.swing.JButton();
        repots = new javax.swing.JButton();
        logout = new javax.swing.JButton();
        REFRESH = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel2.setBackground(new java.awt.Color(0, 102, 0));
        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel3.setBackground(new java.awt.Color(255, 255, 255));
        jLabel3.setFont(new java.awt.Font("Imprint MT Shadow", 1, 48)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(0, 102, 0));
        jLabel3.setText("MINI SARI-SARI STORE MANAGEMENT");
        jPanel2.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 90, 1020, 40));

        jSeparator1.setBackground(new java.awt.Color(0, 102, 0));
        jPanel2.add(jSeparator1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 530, 240, 30));

        jPanel7.setBackground(new java.awt.Color(223, 249, 223));
        jPanel7.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED, java.awt.Color.white, java.awt.Color.white, java.awt.Color.white, java.awt.Color.white));
        jPanel7.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel8.setBackground(new java.awt.Color(0, 102, 0));

        jLabel8.setFont(new java.awt.Font("Serif", 1, 36)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(255, 255, 255));
        jLabel8.setText("SALE PROCESS");

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGap(355, 355, 355)
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 279, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(376, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel8Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel7.add(jPanel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(2, -28, 1010, 70));

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(0, 102, 0));
        jLabel7.setText("Customer Lastname:");
        jPanel7.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 100, 220, 20));

        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(0, 102, 0));
        jLabel9.setText("Customer Firstname:");
        jPanel7.add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 160, 200, -1));

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(0, 102, 0));
        jLabel10.setText("Contact Number:");
        jPanel7.add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 220, 200, -1));

        jLabel11.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(0, 102, 0));
        jLabel11.setText("Due Date:");
        jPanel7.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 280, 170, -1));

        jLabel12.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(0, 102, 0));
        jLabel12.setText("Item:");
        jPanel7.add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 90, 110, 30));

        jLabel13.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(0, 102, 0));
        jLabel13.setText("Quantity:");
        jPanel7.add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 160, 110, 20));
        jPanel7.add(lastname, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 90, 220, 40));
        jPanel7.add(firctname, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 150, 220, 40));
        jPanel7.add(cntactno, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 210, 220, 40));

        duedate.addActionListener(this::duedateActionPerformed);
        jPanel7.add(duedate, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 270, 220, 40));

        items.addActionListener(this::itemsActionPerformed);
        items.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                itemsKeyReleased(evt);
            }
        });
        jPanel7.add(items, new org.netbeans.lib.awtextra.AbsoluteConstraints(630, 90, 220, 40));

        qntity.addActionListener(this::qntityActionPerformed);
        jPanel7.add(qntity, new org.netbeans.lib.awtextra.AbsoluteConstraints(630, 150, 220, 40));

        cash.setBackground(new java.awt.Color(0, 102, 51));
        cash.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        cash.setForeground(new java.awt.Color(255, 255, 255));
        cash.setText("CASH");
        cash.addActionListener(this::cashActionPerformed);
        jPanel7.add(cash, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 370, 160, 50));

        utang.setBackground(new java.awt.Color(102, 0, 0));
        utang.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        utang.setForeground(new java.awt.Color(255, 255, 255));
        utang.setText("UTANG");
        utang.addActionListener(this::utangActionPerformed);
        jPanel7.add(utang, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 370, 160, 50));

        jPanel2.add(jPanel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 160, 950, 460));

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sarisaristore/DAHS.png"))); // NOI18N
        jPanel2.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(264, 78, 1030, 580));

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sarisaristore/m.jpg"))); // NOI18N
        jPanel2.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 0, 1020, 250));

        product.setBackground(new java.awt.Color(0, 102, 0));
        product.setFont(new java.awt.Font("Serif", 1, 12)); // NOI18N
        product.setForeground(new java.awt.Color(255, 255, 255));
        product.setText("PRODUCTS ");
        product.addActionListener(this::productActionPerformed);
        jPanel2.add(product, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 270, 200, 50));

        utngrec.setBackground(new java.awt.Color(0, 102, 0));
        utngrec.setFont(new java.awt.Font("Serif", 1, 12)); // NOI18N
        utngrec.setForeground(new java.awt.Color(255, 255, 255));
        utngrec.setText("UTANG RECORDS");
        utngrec.addActionListener(this::utngrecActionPerformed);
        jPanel2.add(utngrec, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 350, 200, 50));

        repots.setBackground(new java.awt.Color(0, 102, 0));
        repots.setFont(new java.awt.Font("Serif", 1, 12)); // NOI18N
        repots.setForeground(new java.awt.Color(255, 255, 255));
        repots.setText("REPORTS");
        repots.addActionListener(this::repotsActionPerformed);
        jPanel2.add(repots, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 430, 200, 50));

        logout.setBackground(new java.awt.Color(0, 102, 0));
        logout.setFont(new java.awt.Font("Serif", 1, 12)); // NOI18N
        logout.setForeground(new java.awt.Color(255, 255, 255));
        logout.setText("LOG OUT");
        logout.addActionListener(this::logoutActionPerformed);
        jPanel2.add(logout, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 560, 200, 50));

        REFRESH.setBackground(new java.awt.Color(0, 102, 0));
        REFRESH.setFont(new java.awt.Font("Serif", 0, 14)); // NOI18N
        REFRESH.setForeground(new java.awt.Color(255, 255, 255));
        REFRESH.setText("RERFESH");
        REFRESH.addActionListener(this::REFRESHActionPerformed);
        jPanel2.add(REFRESH, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 190, 200, 50));

        jLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/uu.jpg"))); // NOI18N
        jPanel2.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 270, 440));

        jLabel5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/uu.jpg"))); // NOI18N
        jPanel2.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 350, 270, 310));

        getContentPane().add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1290, 660));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void productActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_productActionPerformed
        ProductFrame main = new ProductFrame();     //pra maka open ug new na window
        main.setVisible(true);      //tas mubalik a mainmenu, e remove ang utang na window

        this.dispose();
    }//GEN-LAST:event_productActionPerformed
    
    private void cashActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cashActionPerformed
     processTransaction(true);
    }//GEN-LAST:event_cashActionPerformed

    private void utangActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_utangActionPerformed
        if (processTransaction(false)) {
        UtangFrame frame = new UtangFrame();
        frame.setVisible(true);
        this.dispose();
    }

    }//GEN-LAST:event_utangActionPerformed

    private void utngrecActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_utngrecActionPerformed
        UtangFrame main = new UtangFrame();     //pra maka open ug new na window
        main.setVisible(true);      //tas mubalik a mainmenu, e remove ang utang na window

        this.dispose();
    }//GEN-LAST:event_utngrecActionPerformed

    private void repotsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_repotsActionPerformed
         ARCHIVE main = new ARCHIVE();     //pra maka open ug new na window
        main.setVisible(true);      //tas mubalik a mainmenu, e remove ang utang na window

        this.dispose();
    }//GEN-LAST:event_repotsActionPerformed

    private void logoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logoutActionPerformed
       logout main = new logout();     //pra maka open ug new na window
        main.setVisible(true);      //tas mubalik a mainmenu, e remove ang utang na window

        this.dispose();
    }//GEN-LAST:event_logoutActionPerformed

    private void REFRESHActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_REFRESHActionPerformed
         clearFields();

    ProductFrame.productModel.fireTableDataChanged();
    StoreData.sales.fireTableDataChanged();
    StoreData.utang.fireTableDataChanged();
    StoreData.utangReports.fireTableDataChanged();

    JOptionPane.showMessageDialog(this, "Dashboard refreshed!");

    }//GEN-LAST:event_REFRESHActionPerformed

    private void qntityActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_qntityActionPerformed
      
    }//GEN-LAST:event_qntityActionPerformed

    private void itemsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemsActionPerformed
       
    }//GEN-LAST:event_itemsActionPerformed

    private void itemsKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_itemsKeyReleased

    }//GEN-LAST:event_itemsKeyReleased

    private void duedateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_duedateActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_duedateActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new Dashbard().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton REFRESH;
    private javax.swing.JButton cash;
    private javax.swing.JTextField cntactno;
    private javax.swing.JTextField duedate;
    private javax.swing.JTextField firctname;
    private javax.swing.JTextField items;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextField lastname;
    private javax.swing.JButton logout;
    private javax.swing.JButton product;
    private javax.swing.JTextField qntity;
    private javax.swing.JButton repots;
    private javax.swing.JButton utang;
    private javax.swing.JButton utngrec;
    // End of variables declaration//GEN-END:variables
}
