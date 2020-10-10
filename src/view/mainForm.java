
package view;

import com.digitalpersona.onetouch.DPFPDataPurpose;
import com.digitalpersona.onetouch.DPFPFeatureSet;
import com.digitalpersona.onetouch.DPFPGlobal;
import com.digitalpersona.onetouch.DPFPSample;
import com.digitalpersona.onetouch.DPFPTemplate;
import com.digitalpersona.onetouch.capture.DPFPCapture;
import com.digitalpersona.onetouch.processing.DPFPEnrollment;
import com.digitalpersona.onetouch.processing.DPFPImageQualityException;
import config.FPConfig;
import java.awt.Dimension;
import java.awt.Image;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import database.Conexion;
import database.Data;
import database.dao.DaoHistorial;
import database.dao.UserDao;
import database.dao.impl.HistoryDaoImpl;
import database.dao.impl.UserDaoImpl;
import database.model.DBHistory;
import database.model.DBUser;
import java.awt.HeadlessException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import model.EnrollingListener;
import model.FPSensorBehivor;
import model.FPUser;
import model.SensorAdministrator;
import model.VerificationListener;
import service.FPSensorVerificationService;
import service.FPUserService;
import util.SensorUtils;
import util.UserUtils;

public class mainForm extends javax.swing.JFrame implements EnrollingListener, VerificationListener {

    //LOGGER SETUP
    public static Logger log = Logger.getLogger(mainForm.class.getName());
    private DPFPEnrollment dPFPEnrollment;

    //UI
    //DATA
    private Conexion connection;
    private Data data;
    private List<String> sensorIds;
    private FPConfig fpconfig;

    //DAO
    private UserDao userDao;
    private DaoHistorial histDao;

    //SERVICES    
    private FPUserService userService;
    private SensorAdministrator sensorAdministrator;
    private FPSensorVerificationService verificationService;

    public mainForm(Conexion con,FPConfig fpconfig) {
        
        this.fpconfig = fpconfig;
        // GUI
        initComponents();
        setLocationRelativeTo(null);
        lblHuella.setPreferredSize(new Dimension(300, 250));
        lblHuella.setBorder(BorderFactory.createLoweredBevelBorder());
        btnIdentify.setEnabled(false);

        // VALUES
        sensorIds = SensorUtils.getSensorsSerialIds();
        for (int i = 0; i < sensorIds.size(); i++) {
            cbVerifySensor.addItem(sensorIds.get(i).replaceAll("[{}]", ""));
            cbEnrollSensor.addItem(sensorIds.get(i).replaceAll("[{}]", ""));
        }

        // DATA
        connection = con;
        data = new Data(connection);

        //DAO
        userDao = new UserDaoImpl(connection,this.fpconfig);
        histDao = new HistoryDaoImpl(con);

        // SERVICES
        sensorAdministrator = new SensorAdministrator(userDao); // admin contains all services
        userService = sensorAdministrator.getUserService();
        verificationService = sensorAdministrator.getVerificationService();

        // BEHAVIOR
        for (int i = 0; i < sensorIds.size(); i++) {
            log.log(Level.INFO, "ID SENSOR Detected:" + sensorIds.get(i));
            sensorAdministrator.changeSensorBehivor(sensorIds.get(i), FPSensorBehivor.NONE);
        }

        sensorAdministrator.addEnrollingListener(this::enrollingEvent);
        sensorAdministrator.addVerificationListener(this::verificationEvent);
        listLastEnrollments();

    }

    @Override
    public void verificationEvent(Optional<FPUser> user) {
        if (user.isPresent()) {
            FPUser fpUser = user.get();
            Optional<DBUser> userById = userDao.getUserById((int) fpUser.getUserId());
            if (userById.isPresent()) {
                DBUser dbUser = userById.get();
                userVerificated(dbUser);

            } else {
                log.severe("USER MISSMATCH:" + fpUser + " NOT FOUNDED IN DB!");

            }
        } else {
            txtAreaState.setText("Usuario no identificado, \nintente nuevamente");

            log.info("user not found");

        }

    }

    @Override
    public void enrollingEvent(DPFPSample data) {

        DPFPFeatureSet features;
        DPFPCapture capture = DPFPGlobal.getCaptureFactory().createCapture();
        if (dPFPEnrollment.getFeaturesNeeded() > 0) {
            try {

                features = SensorUtils.getFeatureSet(data, DPFPDataPurpose.DATA_PURPOSE_ENROLLMENT);
                dPFPEnrollment.addFeatures(features);
                Image img = crearImagenHuella(data);
                dibujarHuella(img);

                if (features != null) {
                    log.info("Enroller status:" + dPFPEnrollment.getTemplateStatus() + "\tfeatures:" + dPFPEnrollment.getFeaturesNeeded());
                    System.out.println();
                    if (dPFPEnrollment.getFeaturesNeeded() == 1) {
                        txtAreaInfo.setText("Falta " + dPFPEnrollment.getFeaturesNeeded() + " muestra, porfavor continue.");
                    } else {
                        txtAreaInfo.setText("Faltan " + dPFPEnrollment.getFeaturesNeeded() + " muestras, porfavor continue.");
                    }
                    switch (dPFPEnrollment.getTemplateStatus()) {

                        case TEMPLATE_STATUS_READY:
                            txtAreaInfo.setText("");

                            DPFPTemplate template = dPFPEnrollment.getTemplate();
                            DBUser dbUser = new DBUser(txtName.getText(), txtRut.getText(), cbUserType.getSelectedIndex(), template.serialize());
                            System.out.println(dbUser.toString());
                            userDao.add(dbUser);
                            dPFPEnrollment.clear();
                            capture.stopCapture();
                            JOptionPane.showMessageDialog(this, "Enrolamiento exitoso\nse limpiará la ventana de enrolamiento", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                            clearEnrollFrame();
                            listLastEnrollments();
                            break;

                        case TEMPLATE_STATUS_FAILED:
                            txtAreaInfo.setText("\n Intente nuevamente porfavor, ponga su dedo en el lector");
                            dPFPEnrollment.clear();
                            capture.stopCapture();
                            capture.startCapture();
                            break;

                    }
                }

            } catch (DPFPImageQualityException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        btnIdentify = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        lblHuella = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        txtName = new javax.swing.JTextField();
        txtRut = new javax.swing.JTextField();
        cbUserType = new javax.swing.JComboBox<>();
        btnConfirmData = new javax.swing.JButton();
        cbEnrollSensor = new javax.swing.JComboBox<>();
        jLabel10 = new javax.swing.JLabel();
        btnEnroll = new javax.swing.JButton();
        btnCancelEnrollment = new javax.swing.JButton();
        jPanel9 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtAreaInfo = new javax.swing.JTextArea();
        cbVerifySensor = new javax.swing.JComboBox<>();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jPanel3 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        spTemperature = new javax.swing.JSpinner();
        lblTypeIdentificated = new javax.swing.JLabel();
        lblRutIdentificated = new javax.swing.JLabel();
        lblNombreIdentificated = new javax.swing.JLabel();
        btnSaveIdentifiedUser = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtAreaState = new javax.swing.JTextArea();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        dataTable = new javax.swing.JTable();
        btnExit = new javax.swing.JButton();
        jPanel10 = new javax.swing.JPanel();
        btnExportDailyData = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        btnIdentify.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        btnIdentify.setText("Identificar Persona con este sensor");
        btnIdentify.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnIdentify.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIdentifyActionPerformed(evt);
            }
        });

        jPanel2.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Huella Digital", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Tahoma", 0, 14))); // NOI18N
        jPanel4.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lblHuella.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jPanel4.add(lblHuella, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 20, 220, 200));

        jPanel5.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel2.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel2.setText("Nombre: ");

        jLabel3.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel3.setText("RUT:");

        jLabel5.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel5.setText("Tipo de ingreso: ");

        txtName.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N

        txtRut.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N

        cbUserType.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cbUserType.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccione un opción", "Estudiante", "Docente", "Personal", "Proveedor" }));

        btnConfirmData.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        btnConfirmData.setText("Confirmar datos y \nreservar lector");
        btnConfirmData.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnConfirmData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConfirmDataActionPerformed(evt);
            }
        });

        cbEnrollSensor.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cbEnrollSensor.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccione una opción" }));

        jLabel10.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel10.setText("Sensor:");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel2)
                    .addComponent(jLabel5)
                    .addComponent(jLabel10))
                .addGap(18, 18, 18)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(txtRut)
                            .addComponent(cbEnrollSensor, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cbUserType, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(btnConfirmData, javax.swing.GroupLayout.PREFERRED_SIZE, 227, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8))
                    .addComponent(txtName))
                .addGap(46, 46, 46))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtRut, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cbUserType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cbEnrollSensor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel10)))
                    .addComponent(btnConfirmData, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(25, Short.MAX_VALUE))
        );

        btnEnroll.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        btnEnroll.setText("Confirme datos antes de enrolar");
        btnEnroll.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnEnroll.setEnabled(false);
        btnEnroll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEnrollActionPerformed(evt);
            }
        });

        btnCancelEnrollment.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        btnCancelEnrollment.setText("Cancelar");
        btnCancelEnrollment.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnCancelEnrollment.setEnabled(false);
        btnCancelEnrollment.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelEnrollmentActionPerformed(evt);
            }
        });

        jPanel9.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Información", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Tahoma", 0, 14))); // NOI18N

        txtAreaInfo.setEditable(false);
        txtAreaInfo.setBackground(new java.awt.Color(231, 231, 231));
        txtAreaInfo.setColumns(20);
        txtAreaInfo.setRows(5);
        jScrollPane2.setViewportView(txtAreaInfo);

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2)
                .addContainerGap())
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(24, 24, 24)
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(204, 204, 204)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(btnEnroll, javax.swing.GroupLayout.PREFERRED_SIZE, 203, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnCancelEnrollment, javax.swing.GroupLayout.PREFERRED_SIZE, 203, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGap(13, 13, 13))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(178, 178, 178)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 261, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(182, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 236, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(22, 22, 22)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnEnroll, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnCancelEnrollment, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(89, 89, 89))
        );

        cbVerifySensor.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cbVerifySensor.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Seleccione una opción" }));
        cbVerifySensor.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cbVerifySensorItemStateChanged(evt);
            }
        });

        jTextArea1.setEditable(false);
        jTextArea1.setBackground(new java.awt.Color(231, 231, 231));
        jTextArea1.setColumns(20);
        jTextArea1.setFont(new java.awt.Font("Monospaced", 0, 14)); // NOI18N
        jTextArea1.setRows(5);
        jTextArea1.setText("seleccione el sensor de la lista\na la izquierda que quiera utilizar\npara identificar");
        jTextArea1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jScrollPane3.setViewportView(jTextArea1);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Identificación", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 14))); // NOI18N

        jLabel4.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel4.setText("Nombre: ");

        jLabel6.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel6.setText("Rut:");

        jLabel7.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel7.setText("Tipo:");

        jLabel8.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel8.setText("Temperatura:");

        spTemperature.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        spTemperature.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(33.0f), Float.valueOf(33.0f), Float.valueOf(40.0f), Float.valueOf(0.1f)));

        lblTypeIdentificated.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        lblTypeIdentificated.setText("--------------");

        lblRutIdentificated.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        lblRutIdentificated.setText("--------------");

        lblNombreIdentificated.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        lblNombreIdentificated.setText("--------------");

        btnSaveIdentifiedUser.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        btnSaveIdentifiedUser.setText("Guardar");
        btnSaveIdentifiedUser.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnSaveIdentifiedUser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveIdentifiedUserActionPerformed(evt);
            }
        });

        jLabel9.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        jLabel9.setText("Estado:");

        txtAreaState.setEditable(false);
        txtAreaState.setBackground(new java.awt.Color(231, 231, 231));
        txtAreaState.setColumns(15);
        txtAreaState.setFont(new java.awt.Font("Monospaced", 0, 14)); // NOI18N
        txtAreaState.setRows(5);
        jScrollPane1.setViewportView(txtAreaState);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(spTemperature, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblTypeIdentificated, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(jPanel3Layout.createSequentialGroup()
                                        .addComponent(lblRutIdentificated, javax.swing.GroupLayout.PREFERRED_SIZE, 206, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(48, 48, 48))
                                    .addGroup(jPanel3Layout.createSequentialGroup()
                                        .addComponent(lblNombreIdentificated, javax.swing.GroupLayout.PREFERRED_SIZE, 203, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel9)
                                        .addGap(18, 18, 18)))
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 186, Short.MAX_VALUE)))
                        .addContainerGap())))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnSaveIdentifiedUser, javax.swing.GroupLayout.PREFERRED_SIZE, 279, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(33, 33, 33))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(lblNombreIdentificated)
                                    .addComponent(jLabel4)))
                            .addComponent(jLabel9))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lblRutIdentificated))
                        .addGap(33, 33, 33))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(lblTypeIdentificated))
                .addGap(32, 32, 32)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(spTemperature, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addComponent(btnSaveIdentifiedUser, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Ultimos Registros", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 14))); // NOI18N

        dataTable.setFont(new java.awt.Font("Times New Roman", 1, 14)); // NOI18N
        dataTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "ID Registro", "RUT", "Nombre"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        dataTable.setRowHeight(20);
        jScrollPane5.setViewportView(dataTable);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 569, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 184, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        btnExit.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        btnExit.setText("Salir del programa");
        btnExit.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        btnExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExitActionPerformed(evt);
            }
        });

        jPanel10.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Informe", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Tahoma", 0, 14))); // NOI18N

        btnExportDailyData.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        btnExportDailyData.setText("Exportar informe del día");
        btnExportDailyData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportDailyDataActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnExportDailyData, javax.swing.GroupLayout.DEFAULT_SIZE, 273, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnExportDailyData, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(17, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(55, 55, 55)
                        .addComponent(btnExit, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(70, 70, 70)
                        .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(cbVerifySensor, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnIdentify, javax.swing.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 299, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 717, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(cbVerifySensor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(btnIdentify, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(btnExit, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(32, 32, 32))))))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSaveIdentifiedUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveIdentifiedUserActionPerformed

        if (JOptionPane.showConfirmDialog(null, "¿Los datos rescatados son los correctos?", "Confirmación", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {

            Optional<DBUser> userByRut = userDao.getUserByRut(txtRut.getText().trim());
            if(userByRut.isPresent()){
                FPUser fpUser = UserUtils.convertDBUserToFPUser(userByRut.get());
                DBHistory history = new DBHistory();
                history.setUserId(fpUser.getUserId());
                String obs = (String) spTemperature.getValue();
                history.setObservation(obs);
                histDao.add(history);

                
                JOptionPane.showMessageDialog(null, "Datos ingresados correctamente, persona registrada en la base de datos.");
                spTemperature.setValue(33);
                lblNombreIdentificated.setText("--------------");
                lblRutIdentificated.setText("--------------");
                lblTypeIdentificated.setText("--------------");
                
            }else{ // TODO: geenrar manejo
                log.warning("Rut not founded");
            }


        }
    }//GEN-LAST:event_btnSaveIdentifiedUserActionPerformed

    private void cbVerifySensorItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbVerifySensorItemStateChanged
        if (cbVerifySensor.getSelectedIndex() > 0) {
            btnIdentify.setEnabled(true);
        } else {
            btnIdentify.setEnabled(false);
        }
    }//GEN-LAST:event_cbVerifySensorItemStateChanged

    private void btnCancelEnrollmentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelEnrollmentActionPerformed
        clearEnrollFrame();
    }//GEN-LAST:event_btnCancelEnrollmentActionPerformed

    private void btnEnrollActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEnrollActionPerformed
        JOptionPane.showMessageDialog(this, "Se ha iniciado el proceso de enrolado, siga las instrucciones mostradas más arriba");
        txtAreaInfo.setText("Enrolado iniciado, porfavor ponga su dedo 4 veces en el lector, \npresionando de forma considerada y levantando cuando se capture la imagen");
        sensorAdministrator.changeSensorBehivor("{" + cbEnrollSensor.getSelectedItem().toString() + "}", FPSensorBehivor.ENROLLING);
        dPFPEnrollment = DPFPGlobal.getEnrollmentFactory().createEnrollment();
    }//GEN-LAST:event_btnEnrollActionPerformed

    private void btnConfirmDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConfirmDataActionPerformed

        if ((txtName.getText().equals("")) || (txtRut.getText().equals("")) || (cbEnrollSensor.getSelectedIndex() == 0) || (cbUserType.getSelectedIndex() == 0)) {

            JOptionPane.showMessageDialog(null, "No ha completado todos los campos, porfavor verifique");
        } else {

            btnCancelEnrollment.setText("Cancelar enrolamiento");
            btnCancelEnrollment.setEnabled(true);
            // Orden comboBox Estudiante - Docente - Personal - Proveedor
            //Confirmacion Datos Enrolamiento
            confirmEnrollmentData();

        }
    }//GEN-LAST:event_btnConfirmDataActionPerformed

    private void btnExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExitActionPerformed
        System.exit(0);
    }//GEN-LAST:event_btnExitActionPerformed

    private void btnIdentifyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIdentifyActionPerformed

        if ((cbEnrollSensor.getSelectedItem().equals(cbVerifySensor.getSelectedItem().toString())) && !(cbEnrollSensor.isEnabled())) {
            JOptionPane.showMessageDialog(this, "El sensor seleccionado está siendo usado para enrolar,\nporfavor elija otro de la lista o termine de ocuparlo");
        } else {
            userService.retriveUserListFromDatabase(); // hace el select y obtiene todos los usuarios internamente
            String sensorId = cbVerifySensor.getSelectedItem().toString();
            sensorAdministrator.changeSensorBehivor("{" + sensorId + "}", FPSensorBehivor.VALIDATING);

            JOptionPane.showMessageDialog(this, "Sensor " + cbVerifySensor.getSelectedItem().toString() + " está Identificando.\nLos Datos se mostrarán en el apartado de 'identificación'\ncuando el sensor detecte información");
            cbVerifySensor.setSelectedIndex(0);
        }
    }//GEN-LAST:event_btnIdentifyActionPerformed

    private void btnExportDailyDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportDailyDataActionPerformed
        //Exportar informe del día en formato CSV y ordenarlo en un archivo excel o pdf

        JFileChooser chooser = new JFileChooser();
        while (true) {
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = chooser.getSelectedFile();
                    if (!file.toString().toLowerCase().endsWith(".csv")) {
                        file = new File(file.toString() + ".csv");
                    }
                    if (file.exists()) {
                        int choice = JOptionPane.showConfirmDialog(this,
                                String.format("El Archivo \"%1$s\" ya existe.\n¿Quiere reemplazarlo?", file.toString()),
                                "Guardar Informe",
                                JOptionPane.YES_NO_CANCEL_OPTION);
                        if (choice == JOptionPane.NO_OPTION) {
                            continue;
                        } else if (choice == JOptionPane.CANCEL_OPTION) {
                            break;
                        }
                    }
                    FileWriter stream = new FileWriter(file);
                    String[] columnTitle = {"ID Usuario;", "Nombre;", "Rut;", "Fecha Verificacion\n"};
                    for (int i = 0; i < columnTitle.length; i++) {
                        stream.write(columnTitle[i]);
                    }
                    Optional<List<DBUser>> exportDailyData = userDao.exportDailyData();
                    if(exportDailyData.isPresent()){
                        List<DBUser> listDailyUsers = exportDailyData.get();
                        for (DBUser user : listDailyUsers) {
                            stream.write(user.getId() + ";");
                            stream.write(user.getFullname() + ";");
                            stream.write(user.getRut() + ";");
                            stream.write(user.getVerifyDate() + "\n");
                        }
                    }else{
                        log.warning("Hay nada en export dayli data");
                    }                    
                    stream.close();
                } catch (HeadlessException | IOException ex) {
                    JOptionPane.showMessageDialog(this, ex.getLocalizedMessage(), "Guardar Informe", JOptionPane.ERROR_MESSAGE);
                }
            }
            break;

        }


    }//GEN-LAST:event_btnExportDailyDataActionPerformed




    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCancelEnrollment;
    private javax.swing.JButton btnConfirmData;
    private javax.swing.JButton btnEnroll;
    private javax.swing.JButton btnExit;
    private javax.swing.JButton btnExportDailyData;
    private javax.swing.JButton btnIdentify;
    private javax.swing.JButton btnSaveIdentifiedUser;
    private javax.swing.JComboBox<String> cbEnrollSensor;
    private javax.swing.JComboBox<String> cbUserType;
    private javax.swing.JComboBox<String> cbVerifySensor;
    private javax.swing.JTable dataTable;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JLabel lblHuella;
    private javax.swing.JLabel lblNombreIdentificated;
    private javax.swing.JLabel lblRutIdentificated;
    private javax.swing.JLabel lblTypeIdentificated;
    private javax.swing.JSpinner spTemperature;
    private javax.swing.JTextArea txtAreaInfo;
    private javax.swing.JTextArea txtAreaState;
    private javax.swing.JTextField txtName;
    private javax.swing.JTextField txtRut;
    // End of variables declaration//GEN-END:variables

    public Image crearImagenHuella(DPFPSample sample) {
        return DPFPGlobal.getSampleConversionFactory().createImage(sample);
    }

    public void dibujarHuella(Image image) {

        lblHuella.setIcon(new ImageIcon(
                image.getScaledInstance(lblHuella.getWidth(), lblHuella.getHeight(), Image.SCALE_DEFAULT)));
    }

    private void confirmEnrollmentData() {

        txtAreaInfo.setText("Datos confirmados, haga click en 'enrolar' para iniciar el proceso de registro");

        btnEnroll.setEnabled(true);
        btnEnroll.setText("Enrolar");
        txtName.setEnabled(false);
        txtRut.setEnabled(false);
        cbUserType.setEnabled(false);
        cbEnrollSensor.setEnabled(false);
    }

    private void clearEnrollFrame() {

        btnEnroll.setEnabled(false);
        btnEnroll.setText("Confirme datos antes de enrolar");
        txtName.setEnabled(true);
        txtName.setText("");
        txtRut.setEnabled(true);
        txtRut.setText("");
        cbUserType.setEnabled(true);
        cbUserType.setSelectedIndex(0);
        txtAreaInfo.setText("");
        lblHuella.setIcon(new ImageIcon());
        cbEnrollSensor.setEnabled(true);
        cbEnrollSensor.setSelectedIndex(0);
        btnCancelEnrollment.setEnabled(false);
    }

    private void userVerificated(DBUser user) {

        log.log(Level.INFO, "USER DETECTED!:{0}", user);
        txtAreaState.setText("Usuario identificado correctamente!");
        String tipoPersona = "";

        //CONSULTA INNERJOIN PARA LOS DATOS COMPLETOS DEL USUARIO (NOMBRE, RUT, TIPO['String'])
        switch (user.getUserTypeIdFk()) {

            case 1:
                tipoPersona = "Estudiante";
                break;
            case 2:
                tipoPersona = "Docente";
                break;
            case 3:
                tipoPersona = "Personal";
                break;
            case 4:
                tipoPersona = "Proveedor";
                break;

        }

        lblNombreIdentificated.setText(user.getFullname());
        lblRutIdentificated.setText(user.getRut());
        lblTypeIdentificated.setText(tipoPersona);

    }

    private void listLastEnrollments() {

        List<DBUser> latestEnrolledUsers = userDao.getLatestEnrollments();
        DefaultTableModel dtm = new DefaultTableModel();

        dtm.addColumn("ID Usuario");
        dtm.addColumn("Rut");
        dtm.addColumn("Nombre");
        for (DBUser userinfo : latestEnrolledUsers) {
            String[] userData = new String[]{
                String.valueOf(userinfo.getId()), userinfo.getRut(), userinfo.getFullname()
            };
            dtm.addRow(userData);
        }
        dataTable.setModel(dtm);
        dataTable.sizeColumnsToFit(WIDTH);
        dataTable.sizeColumnsToFit(HEIGHT);

    }

}
