package ru.zolotuhin.interfaces;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.event.TableModelEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import javax.swing.*;
import javax.swing.text.DateFormatter;
import java.awt.*;
import java.awt.Font;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class OP8FormApp {
    private JFrame frame;
    private JTable dishTable;
    private DefaultTableModel tableModel;
    private JTextField actNumberField;
    private JFormattedTextField actDateField;
    private JComboBox<String> orgComboBox, departmentComboBox, activityComboBox;
    private JTextField okpoField, okpoDepartmentField, okdpField;
    private JFormattedTextField periodFromField, periodToField;
    private JComboBox<String> positionComboBox, personComboBox;
    private JTextArea decisionArea;
    private final List<JComboBox<String>> commissionPositions = new ArrayList<>();
    private final List<JComboBox<String>> commissionPersons = new ArrayList<>();
    private final List<JComboBox<String>> commissionSigns = new ArrayList<>();

    // Данные для сотрудников и их должностей
    private final Map<String, String> positionToPerson = new LinkedHashMap<String, String>() {{
        put("Директор", "Иванов Иван Иванович");
        put("Заведующий складом", "Петров Петр Петрович");
        put("Бухгалтер", "Сидорова Мария Сергеевна");
    }};

    private final Map<String, String> personToPosition = new LinkedHashMap<String, String>() {{
        put("Иванов Иван Иванович", "Директор");
        put("Петров Петр Петрович", "Заведующий складом");
        put("Сидорова Мария Сергеевна", "Бухгалтер");
    }};

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            OP8FormApp window = new OP8FormApp();
            window.frame.setVisible(true);
        });
    }

    public OP8FormApp() {
        initialize();
    }

    private JFormattedTextField createDateField() {
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        DateFormatter dateFormatter = new DateFormatter(dateFormat);
        dateFormatter.setAllowsInvalid(false);
        dateFormatter.setOverwriteMode(true);

        JFormattedTextField dateField = new JFormattedTextField(dateFormatter);
        dateField.setValue(new Date());
        dateField.setColumns(10);
        return dateField;
    }

    private void initialize() {
        frame = new JFrame();
        frame.setTitle("Инвентаризация товарно-материальных ценностей");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Заголовок акта
        addActHeader(mainPanel);

        // Информация об организации
        addOrganizationInfo(mainPanel);

        // Таблица посуды
        addDishTable(mainPanel);

        // Комиссия
        addCommissionInfo(mainPanel);

        addControlButtons(mainPanel);
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        frame.getContentPane().add(scrollPane);
        frame.pack();
        frame.setSize(1280, 700);
    }

    private void addActHeader(JPanel panel) {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Акт № и Дата
        JPanel firstRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        firstRow.add(new JLabel("Акт №"));
        actNumberField = new JTextField(5);
        firstRow.add(actNumberField);

        firstRow.add(new JLabel("Дата"));
        actDateField = createDateField();
        firstRow.add(actDateField);

        headerPanel.add(firstRow);

        // Название документа
        JPanel twoRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        twoRow.add(new JLabel("Инвентаризации товарно-материальных ценностей"));
        twoRow.setFont(new Font("Arial", Font.BOLD, 20));
        headerPanel.add(twoRow);

        panel.add(headerPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
    }

    // Организация
    private void addOrganizationInfo(JPanel panel) {
        JPanel orgPanel = new JPanel();
        orgPanel.setLayout(new GridBagLayout());
        orgPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 0, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;


        gbc.gridx = 0; gbc.gridy = 0;
        orgPanel.add(new JLabel("Организация:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        orgComboBox = new JComboBox<>(new String[]{"ООО \"Ресторан Престиж\"", "ООО \"Кафе Вкусняшка\"", "ЗАО \"Столовая №1\""});
        orgPanel.add(orgComboBox, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        orgPanel.add(new JLabel("по ОКПО:"), gbc);

        gbc.gridx = 3; gbc.weightx = 0.5;
        okpoField = new JTextField(8);
        orgPanel.add(okpoField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0;
        orgPanel.add(new JLabel("Структурное подразделение:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        departmentComboBox = new JComboBox<>(new String[]{"Кухня", "Бар", "Зал обслуживания", "Склад"});
        orgPanel.add(departmentComboBox, gbc);

        gbc.gridx = 3; gbc.weightx = 0.5;
        okpoDepartmentField = new JTextField(8);
        orgPanel.add(okpoDepartmentField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.weightx = 0;
        orgPanel.add(new JLabel("Вид деятельности:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        activityComboBox = new JComboBox<>(new String[]{"Общественное питание", "Торговля", "Производство"});
        orgPanel.add(activityComboBox, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        orgPanel.add(new JLabel("по ОКДП:"), gbc);

        gbc.gridx = 3; gbc.weightx = 0.5;
        okdpField = new JTextField(8);
        orgPanel.add(okdpField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.weightx = 0;
        orgPanel.add(new JLabel("Отчетный период:"), gbc);

        JPanel periodPanel = new JPanel();
        periodPanel.setLayout(new BoxLayout(periodPanel, BoxLayout.Y_AXIS));

        JPanel labelsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel fromLabel = new JLabel("с");
        fromLabel.setPreferredSize(new Dimension(20, fromLabel.getPreferredSize().height));
        JLabel toLabel = new JLabel("по");
        toLabel.setPreferredSize(new Dimension(20, toLabel.getPreferredSize().height));
        labelsPanel.add(fromLabel);
        labelsPanel.add(Box.createHorizontalStrut(70));
        labelsPanel.add(toLabel);

        JPanel fieldsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

        periodFromField = new JFormattedTextField(dateFormat);
        periodFromField.setValue(new Date());
        periodFromField.setColumns(10);

        periodToField = new JFormattedTextField(dateFormat);
        periodToField.setValue(new Date());
        periodToField.setColumns(10);

        fieldsPanel.add(periodFromField);
        fieldsPanel.add(Box.createHorizontalStrut(10));
        fieldsPanel.add(periodToField);

        periodPanel.add(labelsPanel);
        periodPanel.add(fieldsPanel);

        gbc.gridx = 1; gbc.gridwidth = 4; gbc.weightx = 1.0;
        orgPanel.add(periodPanel, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1; gbc.weightx = 0;
        orgPanel.add(new JLabel("Материально ответственный:"), gbc);

        gbc.gridx = 2; gbc.gridy = 4; gbc.gridwidth = 1;
        positionComboBox = new JComboBox<>(positionToPerson.keySet().toArray(new String[0]));
        positionComboBox.addActionListener(e -> {
            String selectedPosition = (String) positionComboBox.getSelectedItem();
            personComboBox.setSelectedItem(positionToPerson.get(selectedPosition));
        });
        orgPanel.add(positionComboBox, gbc);

        gbc.gridx = 1; gbc.gridy = 4; gbc.gridwidth = 1;
        personComboBox = new JComboBox<>(personToPosition.keySet().toArray(new String[0]));
        personComboBox.addActionListener(e -> {
            String selectedPerson = (String) personComboBox.getSelectedItem();
            positionComboBox.setSelectedItem(personToPosition.get(selectedPerson));
        });
        orgPanel.add(personComboBox, gbc);

        panel.add(orgPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
    }

    // Таблица Акта о бое, ломе и утрате посуды и приборов
    private void addDishTable(JPanel panel) {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        Map<String, Double> itemPrices = new HashMap<>();
        itemPrices.put("Тарелка столовая", 250.0);
        itemPrices.put("Стакан", 150.0);
        itemPrices.put("Ложка столовая", 80.0);
        itemPrices.put("Вилка столовая", 90.0);

        Map<String, String> itemCodes = new HashMap<>();
        itemCodes.put("Тарелка столовая", "F-120");
        itemCodes.put("Стакан", "A-452");
        itemCodes.put("Ложка столовая", "F-121");
        itemCodes.put("Вилка столовая", "F-122");

        Map<String, String> personPositions = new HashMap<>();
        personPositions.put("Иванов И.И.", "Директор");
        personPositions.put("Петров П.П.", "Завхоз");
        personPositions.put("Сидорова С.С.", "Бухгалтер");

        // Заголовок таблицы
        JLabel tableTitle = new JLabel("АКТ О БОЕ, ЛОМЕ И УТРАТЕ ПОСУДЫ И ПРИБОРОВ", SwingConstants.CENTER);
        tableTitle.setFont(new Font("Arial", Font.BOLD, 20));
        tablePanel.add(tableTitle, BorderLayout.NORTH);

        Object[][] headerStructure = {
                {"Посуда, приборы", 2, 1, 0, 1},
                {null, "Наименование", 1, 1, 1},
                {null, "Код", 1, 1, 1},
                {"Цена, руб", 1, 1, 0, 2},
                {"Бой, лом, утрачено, пропало", 4, 1, 0, 3},
                {null, "бой, лом", 2, 1, 1},
                {null, null, "количество, шт.", 1, 2},
                {null, null, "сумма, руб", 1, 2},
                {null, "утрачено, пропало", 2, 1, 1},
                {null, null, "количество, шт.", 1, 2},
                {null, null, "сумма, руб", 1, 2},
                {"Обстоятельства и виновные", 3, 1, 0, 4},
                {null, "Обстоятельства", 1, 1, 1},
                {null, "Виновные лица", 2, 1, 1},
                {null, null, "Должность", 1, 2},
                {null, null, "ФИО", 1, 2},
                {"Примечания", 1, 1, 0, 5},
                {"Всего", 1, 1, 0, 5}
        };

        // Создаем модель данных
        String[] columnNames = {
                "№ п/п", "Наименование", "Код", "Цена, руб",
                "Бой, лом (кол-во)", "Бой, лом (сумма)",
                "Утрачено (кол-во)", "Утрачено (сумма)",
                "Обстоятельства", "Должность", "ФИО", "Примечание", "Всего"
        };

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Integer.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                // Разрешаем редактирование только определенных столбцов
                return column != 0 && column != 3 && column != 5 && column != 7 && column != 9;
            }
        };
        tableModel.addRow(new Object[]{1, "", "", "", "", "", "", "", "", "", "", ""});

        dishTable = new JTable(tableModel) {
            @Override
            protected JTableHeader createDefaultTableHeader() {
                return new GroupableTableHeader(columnModel, headerStructure);
            }
        };

        dishTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        dishTable.setRowHeight(20);
        dishTable.setFont(new Font("Arial", Font.BOLD, 14));

        JComboBox<String> itemCombo = new JComboBox<>(itemPrices.keySet().toArray(new String[0]));
        JComboBox<String> personCombo = new JComboBox<>(personPositions.keySet().toArray(new String[0]));

        dishTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(itemCombo));
        dishTable.getColumnModel().getColumn(10).setCellEditor(new DefaultCellEditor(personCombo));

        itemCombo.addActionListener(e -> {
            int row = dishTable.getEditingRow();
            if (row >= 0) {
                String selectedItem = (String) itemCombo.getSelectedItem();
                double price = itemPrices.get(selectedItem);
                String code = itemCodes.get(selectedItem);
                tableModel.setValueAt(price, row, 3);
                tableModel.setValueAt(code, row, 2);

                updateSums(row);
            }
        });

        personCombo.addActionListener(e -> {
            int row = dishTable.getEditingRow();
            if (row >= 0) {
                String selectedPerson = (String) personCombo.getSelectedItem();
                String position = personPositions.get(selectedPerson);
                tableModel.setValueAt(position, row, 9); // Устанавливаем должность
            }
        });

        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int column = e.getColumn();

                if (column == 4 || column == 6) {
                    updateSums(row);
                }
            }
        });
        int[] widths = {40, 150, 50, 80, 150, 150, 150, 150, 150, 100, 150, 150};
        for (int i = 0; i < widths.length; i++) {
            dishTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JScrollPane scrollPane = new JScrollPane(dishTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton addButton = new JButton("Добавить строку");
        addButton.setSize(30, 50);
        addButton.addActionListener(e -> {
            tableModel.addRow(new Object[]{
                    tableModel.getRowCount() + 1, "", "", "", "", "", "", "", "", "", "", ""
            });
        });

        JButton removeButton = new JButton("Удалить строку");
        removeButton.addActionListener(e -> {
            int selectedRow = dishTable.getSelectedRow();
            if (selectedRow >= 0) {
                tableModel.removeRow(selectedRow);
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    tableModel.setValueAt(i + 1, i, 0);
                }
            }
        });

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        tablePanel.add(buttonPanel, BorderLayout.SOUTH);

        panel.add(tablePanel);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
    }

    private void updateSums(int row) {
        try {
            double price = 0.0;
            if (tableModel.getValueAt(row, 3) != null) {
                price = Double.parseDouble(tableModel.getValueAt(row, 3).toString());
            }

            double breakSum = 0.0;
            if (tableModel.getValueAt(row, 4) != null && !tableModel.getValueAt(row, 4).toString().isEmpty()) {
                int count = Integer.parseInt(tableModel.getValueAt(row, 4).toString());
                breakSum = price * count;
                tableModel.setValueAt(breakSum, row, 5);
            }

            double lostSum = 0.0;
            if (tableModel.getValueAt(row, 6) != null && !tableModel.getValueAt(row, 6).toString().isEmpty()) {
                int count = Integer.parseInt(tableModel.getValueAt(row, 6).toString());
                lostSum = price * count;
                tableModel.setValueAt(lostSum, row, 7);
            }

            double total = breakSum + lostSum;
            tableModel.setValueAt(total, row, 12);

        } catch (NumberFormatException ex) {
        }
    }

    static class GroupableTableHeader extends JTableHeader {
        private Object[][] headerStructure;

        public GroupableTableHeader(TableColumnModel model, Object[][] headerStructure) {
            super(model);
            this.headerStructure = headerStructure;
            setDefaultRenderer(new GroupableHeaderRenderer());
            setFont(new Font("Arial", Font.PLAIN, 14));
        }
    }

    static class GroupableHeaderRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {

            JTableHeader header = table.getTableHeader();
            JLabel label = new JLabel(value != null ? value.toString() : "");
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setBorder(BorderFactory.createEtchedBorder());
            label.setFont(header.getFont().deriveFont(Font.BOLD));
            label.setOpaque(true);
            label.setBackground(header.getBackground());
            label.setForeground(header.getForeground());

            return label;
        }
    }

    private void addCommissionInfo(JPanel panel) {
        JPanel commissionPanel = new JPanel();
        commissionPanel.setLayout(new BoxLayout(commissionPanel, BoxLayout.Y_AXIS));
        commissionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        commissionPanel.setBorder(BorderFactory.createTitledBorder("Комиссия и решение"));

        JPanel commissionContentPanel = new JPanel();
        commissionContentPanel.setLayout(new BoxLayout(commissionContentPanel, BoxLayout.Y_AXIS));
        commissionContentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel commissionLabel = new JLabel("Комиссия в составе:");
        commissionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        commissionContentPanel.add(commissionLabel);
        commissionContentPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        for (int i = 0; i < 3; i++) {
            JPanel memberPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            memberPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JComboBox<String> position = new JComboBox<>(new String[]{
                    "Председатель комиссии", "Член комиссии", "Секретарь"
            });
            commissionPositions.add(position);
            memberPanel.add(position);

            JComboBox<String> person = new JComboBox<>(personToPosition.keySet().toArray(new String[0]));
            commissionPersons.add(person);
            memberPanel.add(person);

            JComboBox<String> sign = new JComboBox<>(new String[]{"Подпись", " "});
            commissionSigns.add(sign);
            memberPanel.add(sign);

            commissionContentPanel.add(memberPanel);
            commissionContentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }

        commissionContentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JLabel decisionLabel = new JLabel("Решение администрации:");
        decisionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        commissionContentPanel.add(decisionLabel);
        commissionContentPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        decisionArea = new JTextArea(3, 50);
        decisionArea.setLineWrap(true);
        decisionArea.setWrapStyleWord(true);
        JScrollPane decisionScroll = new JScrollPane(decisionArea);
        decisionScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        decisionScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, decisionScroll.getPreferredSize().height));
        commissionContentPanel.add(decisionScroll);

        commissionPanel.add(commissionContentPanel);
        panel.add(commissionPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
    }

    private void addControlButtons(JPanel panel) {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton saveButton = new JButton("Сохранить");
        saveButton.addActionListener(e -> exportToExcel());

        JButton printButton = new JButton("Печать");
        printButton.addActionListener(e -> printForm());

        JButton clearButton = new JButton("Очистить");
        clearButton.addActionListener(e -> clearForm());

        buttonPanel.add(clearButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(printButton);

        panel.add(buttonPanel);
    }


    private void printForm() {
        JOptionPane.showMessageDialog(frame, "Форма отправлена на печать", "Печать", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportToExcel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить как Excel");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files", "xlsx"));

        if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            if (!filePath.endsWith(".xlsx")) {
                filePath += ".xlsx";
            }

            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Акт ОП-8");

                // Создаем стили для Excel
                CellStyle headerStyle = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                headerStyle.setAlignment(HorizontalAlignment.CENTER);
                headerStyle.setBorderBottom(BorderStyle.THIN);
                headerStyle.setBorderTop(BorderStyle.THIN);
                headerStyle.setBorderLeft(BorderStyle.THIN);
                headerStyle.setBorderRight(BorderStyle.THIN);

                CellStyle regularStyle = workbook.createCellStyle();
                regularStyle.setBorderBottom(BorderStyle.THIN);
                regularStyle.setBorderTop(BorderStyle.THIN);
                regularStyle.setBorderLeft(BorderStyle.THIN);
                regularStyle.setBorderRight(BorderStyle.THIN);

                // Заголовок акта
                int rowNum = 0;
                Row headerRow = sheet.createRow(rowNum++);
                Cell headerCell = headerRow.createCell(0);
                headerCell.setCellValue("АКТ О БОЕ, ЛОМЕ И УТРАТЕ ПОСУДЫ И ПРИБОРОВ");
                headerCell.setCellStyle(headerStyle);
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 12));

                // Информация об организации
                addExcelRow(sheet, rowNum++, new Object[]{
                        "Организация:", orgComboBox.getSelectedItem(), null, null,
                        "по ОКПО:", okpoField.getText()
                }, headerStyle, regularStyle);

                addExcelRow(sheet, rowNum++, new Object[]{
                        "Структурное подразделение:", departmentComboBox.getSelectedItem()
                }, headerStyle, regularStyle);

                addExcelRow(sheet, rowNum++, new Object[]{
                        "Вид деятельности:", activityComboBox.getSelectedItem(), null, null,
                        "по ОКДП:", okdpField.getText()
                }, headerStyle, regularStyle);

                // Отчетный период
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
                addExcelRow(sheet, rowNum++, new Object[]{
                        "Отчетный период:", "с " + dateFormat.format(periodFromField.getValue()),
                        null, "по " + dateFormat.format(periodToField.getValue())
                }, headerStyle, regularStyle);

                // Материально ответственное лицо
                addExcelRow(sheet, rowNum++, new Object[]{
                        "Материально ответственный:", personComboBox.getSelectedItem(),
                        null, positionComboBox.getSelectedItem()
                }, headerStyle, regularStyle);

                // Пустая строка
                rowNum++;

                // Заголовок таблицы
                addExcelRow(sheet, rowNum++, new Object[]{
                        "№ п/п", "Посуда, приборы", null, "Цена, руб",
                        "Бой, лом, утрачено, пропало", null, null, null,
                        "Обстоятельства и виновные", null, null, "Примечание", "Всего"
                }, headerStyle, regularStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 1, 2));
                sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 4, 7));
                sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 8, 9));

                addExcelRow(sheet, rowNum++, new Object[]{
                        null, "Наименование", "Код", null,
                        "бой, лом", null, "утрачено, пропало", null,
                        "Обстоятельства", "Виновные лица", null, null, null
                }, headerStyle, regularStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 4, 5));
                sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 6, 7));

                addExcelRow(sheet, rowNum++, new Object[]{
                        null, null, null, null,
                        "кол-во, шт.", "сумма, руб", "кол-во, шт.", "сумма, руб",
                        null, "Должность", "ФИО", null, null
                }, headerStyle, regularStyle);

                // Данные таблицы
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    Row dataRow = sheet.createRow(rowNum++);
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        Object value = tableModel.getValueAt(i, j);
                        Cell cell = dataRow.createCell(j);

                        if (value instanceof Number) {
                            cell.setCellValue(((Number)value).doubleValue());
                        } else if (value != null) {
                            cell.setCellValue(value.toString());
                        }
                        cell.setCellStyle(regularStyle);
                    }
                }

                // Комиссия
                rowNum++;
                addExcelRow(sheet, rowNum++, new Object[]{"Комиссия в составе:"}, headerStyle, regularStyle);

                for (int i = 0; i < commissionPositions.size(); i++) {
                    addExcelRow(sheet, rowNum++, new Object[]{
                            commissionPositions.get(i).getSelectedItem(),
                            commissionPersons.get(i).getSelectedItem(),
                            null,
                            commissionSigns.get(i).getSelectedItem()
                    }, null, regularStyle);
                }

                // Решение администрации
                rowNum++;
                Row decisionHeader = sheet.createRow(rowNum++);
                Cell decisionCell = decisionHeader.createCell(0);
                decisionCell.setCellValue("Решение администрации:");
                decisionCell.setCellStyle(headerStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 3));

                Row decisionRow = sheet.createRow(rowNum++);
                Cell decisionContentCell = decisionRow.createCell(0);
                decisionContentCell.setCellValue(decisionArea.getText());
                decisionContentCell.setCellStyle(regularStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 3));

                // Автоподбор ширины столбцов
                for (int i = 0; i < 13; i++) {
                    sheet.autoSizeColumn(i);
                }

                // Сохранение файла
                try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                    workbook.write(outputStream);
                    JOptionPane.showMessageDialog(frame,
                            "Данные успешно экспортированы в Excel",
                            "Экспорт завершен",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame,
                        "Ошибка при экспорте в Excel: " + ex.getMessage(),
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    // Вспомогательный метод для добавления строки в Excel
    private void addExcelRow(Sheet sheet, int rowNum, Object[] values,
                             CellStyle headerStyle, CellStyle regularStyle) {
        Row row = sheet.createRow(rowNum);
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                Cell cell = row.createCell(i);
                cell.setCellValue(values[i].toString());
                cell.setCellStyle(headerStyle != null ? headerStyle : regularStyle);
            }
        }
    }

    private void clearForm() {
        int confirm = JOptionPane.showConfirmDialog(
                frame,
                "Вы действительно хотите очистить форму?",
                "Очистка формы",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            actNumberField.setText("");
            actDateField.setValue(new Date());
            orgComboBox.setSelectedIndex(0);
            departmentComboBox.setSelectedIndex(0);
            activityComboBox.setSelectedIndex(0);
            okpoField.setText("");
            okpoDepartmentField.setText("");
            okdpField.setText("");
            periodFromField.setValue(new Date());
            periodToField.setValue(new Date());
            positionComboBox.setSelectedIndex(0);
            personComboBox.setSelectedIndex(0);

            tableModel.setRowCount(0);
            tableModel.addRow(new Object[]{1, "", "", "", "", "", "", "", ""});

            for (JComboBox<?> position : commissionPositions) {
                position.setSelectedIndex(0);
            }
            for (JComboBox<?> person : commissionPersons) {
                person.setSelectedIndex(0);
            }
            for (JComboBox<?> sign : commissionSigns) {
                sign.setSelectedIndex(0);
            }

            decisionArea.setText("");
        }
    }
}
