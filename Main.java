import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

class Station {
    String name;
    List<Track> tracks = new ArrayList<>();

    Station(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}

class Track {
    Station to;
    int distance;

    Track(Station to, int distance) {
        this.to = to;
        this.distance = distance;
    }
}

class Train {
    String id;
    String name;
    Map<Station, String[]> schedule = new LinkedHashMap<>();

    Train(String id, String name) {
        this.id = id;
        this.name = name;
    }

    List<Station> getStops() {
        return new ArrayList<>(schedule.keySet());
    }
}

class Booking {
    String pnr;
    Train train;
    String from;
    String to;
    String date;
    String passengerName;
    String age;
    String gender;
    String travelClass;
    String seatPreference;
    String assignedSeat;
    String paymentMode;
    String phone;
    double fareAmount;

    Booking(
            String pnr,
            Train train,
            String from,
            String to,
            String date,
            String passengerName,
            String age,
            String gender,
            String travelClass,
            String seatPreference,
            String assignedSeat,
            String paymentMode,
            String phone,
            double fareAmount
    ) {
        this.pnr = pnr;
        this.train = train;
        this.from = from;
        this.to = to;
        this.date = date;
        this.passengerName = passengerName;
        this.age = age;
        this.gender = gender;
        this.travelClass = travelClass;
        this.seatPreference = seatPreference;
        this.assignedSeat = assignedSeat;
        this.paymentMode = paymentMode;
        this.phone = phone;
        this.fareAmount = fareAmount;
    }
}

class SeatInfo {
    String coachCode;
    String seatCode;
    String berthType;

    SeatInfo(String coachCode, String seatCode, String berthType) {
        this.coachCode = coachCode;
        this.seatCode = seatCode;
        this.berthType = berthType;
    }
}

public class Main {
    private static final Map<String, Station> stations = new LinkedHashMap<>();
    private static final Map<String, Train> trains = new LinkedHashMap<>();
    private static final List<Booking> bookings = new ArrayList<>();

    private static final String BOOKING_FILE = "bookings.txt";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);

    private static final Map<String, Integer> DEFAULT_CLASS_CAPACITY = createDefaultCapacityMap();
    private static final Map<String, Double> DEFAULT_CLASS_RATE_PER_KM = createDefaultRateMap();

    private static final Map<String, Map<String, Integer>> trainClassCapacityOverrides = new HashMap<>();
    private static final Map<String, Map<String, Double>> trainClassRateOverrides = new HashMap<>();
    private static final Map<String, Set<String>> reservedSeatsByJourneyClass = new HashMap<>();

    private static JFrame frame;
    private static CardLayout cardLayout;
    private static JPanel rootPanel;

    private static JComboBox<String> fromCombo;
    private static JComboBox<String> toCombo;
    private static JComboBox<Integer> dayCombo;
    private static JComboBox<String> monthCombo;
    private static JComboBox<Integer> yearCombo;
    private static JLabel searchErrorLabel;

    private static JTable trainTable;
    private static DefaultTableModel trainTableModel;
    private static JLabel resultHeader;
    private static JComboBox<String> trainQuickSelectCombo;

    private static JTextField nameField;
    private static JSpinner ageSpinner;
    private static JComboBox<String> genderCombo;
    private static JComboBox<String> classCombo;
    private static JComboBox<String> seatCombo;
    private static JComboBox<String> paymentCombo;
    private static JTextField phoneField;
    private static JLabel bookingContextLabel;
    private static JLabel seatAvailabilityLabel;
    private static JLabel bookingErrorLabel;

    private static JTextArea confirmationArea;

    private static JComboBox<String> adminTrainCombo;
    private static JComboBox<String> adminClassCombo;
    private static JSpinner adminCapacitySpinner;
    private static JSpinner adminRateSpinner;
    private static JTable adminConfigTable;
    private static DefaultTableModel adminConfigTableModel;

    private static String currentFrom;
    private static String currentTo;
    private static String currentDate;
    private static Train selectedTrain;

    public static void main(String[] args) {
        setupData();
        loadBookings();
        SwingUtilities.invokeLater(Main::createAppUI);
    }

    private static void createAppUI() {
        frame = new JFrame("Railway Reservation System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1080, 700);
        frame.setLocationRelativeTo(null);

        frame.setJMenuBar(buildMenuBar());

        cardLayout = new CardLayout();
        rootPanel = new JPanel(cardLayout);

        rootPanel.add(createLoginPanel(), "LOGIN");
        rootPanel.add(createSearchPanel(), "SEARCH");
        rootPanel.add(createResultsPanel(), "RESULTS");
        rootPanel.add(createBookingPanel(), "BOOKING");
        rootPanel.add(createConfirmationPanel(), "CONFIRMATION");
        rootPanel.add(createAdminPanel(), "ADMIN");

        frame.add(rootPanel);
        cardLayout.show(rootPanel, "LOGIN");
        frame.setVisible(true);
    }

    private static JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu bookingMenu = new JMenu("Bookings");
        bookingMenu.setMnemonic('B');

        JMenuItem viewBookings = new JMenuItem("View Booking History");
        viewBookings.setMnemonic('V');
        viewBookings.setToolTipText("Open booking history table");
        viewBookings.addActionListener(e -> openBookingHistoryDialog());

        JMenuItem cancelByPnr = new JMenuItem("Cancel Booking by PNR");
        cancelByPnr.setMnemonic('C');
        cancelByPnr.setToolTipText("Cancel a booking with time-based cancellation charge");
        cancelByPnr.addActionListener(e -> cancelBookingByPnrPrompt());

        JMenuItem clearBookings = new JMenuItem("Clear Booking File");
        clearBookings.setMnemonic('L');
        clearBookings.addActionListener(e -> clearBookingFileWithConfirmation());

        bookingMenu.add(viewBookings);
        bookingMenu.add(cancelByPnr);
        bookingMenu.add(clearBookings);

        JMenu adminMenu = new JMenu("Admin");
        adminMenu.setMnemonic('A');

        JMenuItem openAdmin = new JMenuItem("Open Admin Panel");
        openAdmin.setMnemonic('O');
        openAdmin.addActionListener(e -> {
            refreshAdminConfigTable();
            cardLayout.show(rootPanel, "ADMIN");
        });
        adminMenu.add(openAdmin);

        JMenu sessionMenu = new JMenu("Session");
        sessionMenu.setMnemonic('S');

        JMenuItem logout = new JMenuItem("Logout");
        logout.setMnemonic('L');
        logout.addActionListener(e -> {
            clearSearchState();
            cardLayout.show(rootPanel, "LOGIN");
        });

        JMenuItem exit = new JMenuItem("Exit");
        exit.setMnemonic('E');
        exit.addActionListener(e -> frame.dispose());

        sessionMenu.add(logout);
        sessionMenu.add(exit);

        menuBar.add(bookingMenu);
        menuBar.add(adminMenu);
        menuBar.add(sessionMenu);
        return menuBar;
    }

    private static JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Railway Reservation System", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 30));

        JLabel userLabel = new JLabel("Username");
        JTextField userField = new JTextField();
        userField.setToolTipText("Enter admin");
        userLabel.setLabelFor(userField);

        JLabel passLabel = new JLabel("Password");
        JPasswordField passField = new JPasswordField();
        passField.setToolTipText("Enter admin");
        passLabel.setLabelFor(passField);

        JLabel messageLabel = new JLabel(" ", SwingConstants.CENTER);
        messageLabel.setForeground(new Color(180, 30, 30));

        JButton loginBtn = new JButton("Login");
        loginBtn.setMnemonic('L');
        loginBtn.addActionListener(e -> {
            String username = userField.getText().trim();
            String password = String.valueOf(passField.getPassword());

            if (isValidLogin(username, password)) {
                messageLabel.setText(" ");
                clearSearchState();
                cardLayout.show(rootPanel, "SEARCH");
            } else {
                messageLabel.setText("Invalid login. Use admin/admin.");
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panel.add(userLabel, gbc);
        gbc.gridx = 1;
        panel.add(userField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(passLabel, gbc);
        gbc.gridx = 1;
        panel.add(passField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(loginBtn, gbc);

        gbc.gridy = 4;
        panel.add(messageLabel, gbc);

        return panel;
    }

    private static JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Search Trains", SwingConstants.LEFT);
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        panel.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        List<String> stationNames = new ArrayList<>(stations.keySet());
        fromCombo = new JComboBox<>(stationNames.toArray(String[]::new));
        toCombo = new JComboBox<>(stationNames.toArray(String[]::new));
        fromCombo.setEditable(true);
        toCombo.setEditable(true);
        enableComboSearch(fromCombo, stationNames);
        enableComboSearch(toCombo, stationNames);

        if (stationNames.size() > 1) {
            toCombo.setSelectedIndex(1);
        }

        JLabel fromLabel = new JLabel("From");
        fromLabel.setLabelFor(fromCombo);
        JLabel toLabel = new JLabel("To");
        toLabel.setLabelFor(toCombo);

        JPanel datePanel = createDatePickerPanel();
        JLabel dateLabel = new JLabel("Journey Date");
        dateLabel.setLabelFor(dayCombo);

        searchErrorLabel = new JLabel(" ");
        searchErrorLabel.setForeground(new Color(180, 30, 30));

        JButton searchBtn = new JButton("Find Trains");
        searchBtn.setMnemonic('F');
        searchBtn.addActionListener(e -> onSearchTrains());

        JButton historyBtn = new JButton("View Booking History");
        historyBtn.setMnemonic('H');
        historyBtn.addActionListener(e -> openBookingHistoryDialog());

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(fromLabel, gbc);
        gbc.gridx = 1;
        form.add(fromCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(toLabel, gbc);
        gbc.gridx = 1;
        form.add(toCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        form.add(dateLabel, gbc);
        gbc.gridx = 1;
        form.add(datePanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        form.add(searchBtn, gbc);

        gbc.gridy = 4;
        form.add(historyBtn, gbc);

        gbc.gridy = 5;
        form.add(searchErrorLabel, gbc);

        panel.add(form, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel createDatePickerPanel() {
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        String[] monthNames = {
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        };

        dayCombo = new JComboBox<>();
        monthCombo = new JComboBox<>(monthNames);
        yearCombo = new JComboBox<>();

        int currentYear = LocalDate.now().getYear();
        for (int y = currentYear; y <= currentYear + 3; y++) {
            yearCombo.addItem(y);
        }

        LocalDate today = LocalDate.now();
        monthCombo.setSelectedIndex(today.getMonthValue() - 1);
        yearCombo.setSelectedItem(today.getYear());

        Action updateDayAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshDayOptions();
            }
        };
        monthCombo.addActionListener(updateDayAction);
        yearCombo.addActionListener(updateDayAction);
        refreshDayOptions();
        dayCombo.setSelectedItem(today.getDayOfMonth());

        datePanel.add(dayCombo);
        datePanel.add(monthCombo);
        datePanel.add(yearCombo);
        return datePanel;
    }

    private static void refreshDayOptions() {
        Integer selectedYear = (Integer) yearCombo.getSelectedItem();
        int month = monthCombo.getSelectedIndex() + 1;
        int year = selectedYear == null ? LocalDate.now().getYear() : selectedYear;

        Integer previousDay = (Integer) dayCombo.getSelectedItem();
        YearMonth ym = YearMonth.of(year, month);
        int daysInMonth = ym.lengthOfMonth();

        dayCombo.removeAllItems();
        for (int d = 1; d <= daysInMonth; d++) {
            dayCombo.addItem(d);
        }

        if (previousDay != null && previousDay <= daysInMonth) {
            dayCombo.setSelectedItem(previousDay);
        } else {
            dayCombo.setSelectedItem(1);
        }
    }

    private static JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel top = new JPanel(new BorderLayout(8, 8));
        resultHeader = new JLabel("Available Trains", SwingConstants.LEFT);
        resultHeader.setFont(new Font("SansSerif", Font.BOLD, 22));

        trainQuickSelectCombo = new JComboBox<>(new String[]{"Select train from list"});
        trainQuickSelectCombo.addActionListener(e -> syncTableWithQuickSelect());
        trainQuickSelectCombo.setToolTipText("Quick select by train ID/name");

        top.add(resultHeader, BorderLayout.NORTH);
        top.add(trainQuickSelectCombo, BorderLayout.SOUTH);
        panel.add(top, BorderLayout.NORTH);

        String[] columns = {"Train ID", "Train Name", "Departure", "Arrival", "Boarding", "Drop", "Distance (km)", "Approx Fare (Shovon)"};
        trainTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        trainTable = new JTable(trainTableModel);
        trainTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        trainTable.setRowHeight(26);
        trainTable.getSelectionModel().addListSelectionListener(e -> syncQuickSelectWithTable());
        panel.add(new JScrollPane(trainTable), BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton backBtn = new JButton("Back");
        backBtn.addActionListener(e -> cardLayout.show(rootPanel, "SEARCH"));

        JButton proceedBtn = new JButton("Book Selected Train");
        proceedBtn.setMnemonic('B');
        proceedBtn.addActionListener(e -> onProceedBooking());

        actionPanel.add(backBtn);
        actionPanel.add(proceedBtn);
        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private static JPanel createBookingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Passenger and Payment Details", SwingConstants.LEFT);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        panel.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        bookingContextLabel = new JLabel(" ");
        bookingContextLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        seatAvailabilityLabel = new JLabel("Seats left: -");
        seatAvailabilityLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JLabel nameLabel = new JLabel("Passenger Name");
        nameField = new JTextField();
        nameLabel.setLabelFor(nameField);

        JLabel ageLabel = new JLabel("Age");
        ageSpinner = new JSpinner(new SpinnerNumberModel(25, 1, 120, 1));
        ageLabel.setLabelFor(ageSpinner);

        JLabel genderLabel = new JLabel("Gender");
        genderCombo = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        genderLabel.setLabelFor(genderCombo);

        JLabel classLabel = new JLabel("Travel Class");
        classCombo = new JComboBox<>(DEFAULT_CLASS_CAPACITY.keySet().toArray(String[]::new));
        classCombo.addActionListener(e -> {
            updateSeatAvailabilityLabel();
            updateDynamicFarePreview();
        });
        classLabel.setLabelFor(classCombo);

        JLabel seatPrefLabel = new JLabel("Seat Preference");
        seatCombo = new JComboBox<>(new String[]{"Window", "Middle", "Aisle", "No Preference"});
        seatPrefLabel.setLabelFor(seatCombo);

        JLabel paymentLabel = new JLabel("Payment Mode");
        paymentCombo = new JComboBox<>(new String[]{"Card", "Mobile Banking", "Cash"});
        paymentLabel.setLabelFor(paymentCombo);

        JLabel phoneLabel = new JLabel("Phone Number");
        phoneField = new JTextField();
        phoneLabel.setLabelFor(phoneField);

        bookingErrorLabel = new JLabel(" ");
        bookingErrorLabel.setForeground(new Color(180, 30, 30));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        form.add(bookingContextLabel, gbc);

        gbc.gridy = 1;
        form.add(seatAvailabilityLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 2;
        gbc.gridx = 0;
        form.add(nameLabel, gbc);
        gbc.gridx = 1;
        form.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        form.add(ageLabel, gbc);
        gbc.gridx = 1;
        form.add(ageSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        form.add(genderLabel, gbc);
        gbc.gridx = 1;
        form.add(genderCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        form.add(classLabel, gbc);
        gbc.gridx = 1;
        form.add(classCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        form.add(seatPrefLabel, gbc);
        gbc.gridx = 1;
        form.add(seatCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        form.add(paymentLabel, gbc);
        gbc.gridx = 1;
        form.add(paymentCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 8;
        form.add(phoneLabel, gbc);
        gbc.gridx = 1;
        form.add(phoneField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = 2;
        form.add(bookingErrorLabel, gbc);

        panel.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton seatMapBtn = new JButton("View Coach Seat Map");
        seatMapBtn.addActionListener(e -> openSeatMapDialog());

        JButton backBtn = new JButton("Back");
        backBtn.addActionListener(e -> cardLayout.show(rootPanel, "RESULTS"));

        JButton confirmBtn = new JButton("Confirm Booking");
        confirmBtn.setMnemonic('C');
        confirmBtn.addActionListener(e -> onConfirmBooking());

        actions.add(seatMapBtn);
        actions.add(backBtn);
        actions.add(confirmBtn);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private static JPanel createConfirmationPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Booking Confirmed", SwingConstants.LEFT);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        panel.add(title, BorderLayout.NORTH);

        confirmationArea = new JTextArea();
        confirmationArea.setEditable(false);
        confirmationArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        panel.add(new JScrollPane(confirmationArea), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton newSearchBtn = new JButton("Book Another Ticket");
        newSearchBtn.addActionListener(e -> {
            clearSearchState();
            cardLayout.show(rootPanel, "SEARCH");
        });

        JButton historyBtn = new JButton("View Booking History");
        historyBtn.addActionListener(e -> openBookingHistoryDialog());

        actions.add(historyBtn);
        actions.add(newSearchBtn);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private static JPanel createAdminPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Admin Panel: Capacity and Fare Configuration", SwingConstants.LEFT);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        panel.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        List<String> trainOptions = new ArrayList<>();
        for (Train train : trains.values()) {
            trainOptions.add(train.id + " - " + train.name);
        }

        adminTrainCombo = new JComboBox<>(trainOptions.toArray(String[]::new));
        adminClassCombo = new JComboBox<>(DEFAULT_CLASS_CAPACITY.keySet().toArray(String[]::new));

        adminCapacitySpinner = new JSpinner(new SpinnerNumberModel(40, 1, 500, 1));
        adminRateSpinner = new JSpinner(new SpinnerNumberModel(2.0, 0.1, 20.0, 0.1));

        JLabel trainLabel = new JLabel("Train");
        trainLabel.setLabelFor(adminTrainCombo);
        JLabel classLabel = new JLabel("Class");
        classLabel.setLabelFor(adminClassCombo);
        JLabel capLabel = new JLabel("Capacity");
        capLabel.setLabelFor(adminCapacitySpinner);
        JLabel rateLabel = new JLabel("Rate per km (BDT)");
        rateLabel.setLabelFor(adminRateSpinner);

        JButton loadEffectiveBtn = new JButton("Load Effective Values");
        loadEffectiveBtn.addActionListener(e -> loadEffectiveAdminValues());

        JButton saveOverrideBtn = new JButton("Save Override");
        saveOverrideBtn.addActionListener(e -> saveAdminOverride());

        JButton resetOverrideBtn = new JButton("Reset Selected Override");
        resetOverrideBtn.addActionListener(e -> resetAdminOverride());

        JButton backBtn = new JButton("Back to Search");
        backBtn.addActionListener(e -> cardLayout.show(rootPanel, "SEARCH"));

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(trainLabel, gbc);
        gbc.gridx = 1;
        form.add(adminTrainCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(classLabel, gbc);
        gbc.gridx = 1;
        form.add(adminClassCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        form.add(capLabel, gbc);
        gbc.gridx = 1;
        form.add(adminCapacitySpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        form.add(rateLabel, gbc);
        gbc.gridx = 1;
        form.add(adminRateSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        form.add(loadEffectiveBtn, gbc);

        gbc.gridy = 5;
        form.add(saveOverrideBtn, gbc);

        gbc.gridy = 6;
        form.add(resetOverrideBtn, gbc);

        gbc.gridy = 7;
        form.add(backBtn, gbc);

        panel.add(form, BorderLayout.WEST);

        String[] columns = {"Train", "Class", "Capacity", "Rate/km", "Source"};
        adminConfigTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        adminConfigTable = new JTable(adminConfigTableModel);
        adminConfigTable.setRowHeight(24);
        panel.add(new JScrollPane(adminConfigTable), BorderLayout.CENTER);

        loadEffectiveAdminValues();
        refreshAdminConfigTable();
        return panel;
    }

    private static void onSearchTrains() {
        searchErrorLabel.setText(" ");

        String from = normalizedComboValue(fromCombo);
        String to = normalizedComboValue(toCombo);

        if (from == null || to == null || !stations.containsKey(from) || !stations.containsKey(to)) {
            searchErrorLabel.setText("Select valid stations from the dropdown.");
            return;
        }

        if (from.equals(to)) {
            searchErrorLabel.setText("Source and destination cannot be the same.");
            return;
        }

        LocalDate date;
        try {
            date = selectedDateFromPicker();
        } catch (DateTimeException ex) {
            searchErrorLabel.setText("Invalid date selection. Please choose a valid date.");
            return;
        }

        if (date.isBefore(LocalDate.now())) {
            searchErrorLabel.setText("Journey date cannot be in the past.");
            return;
        }

        List<Train> matching = getMatchingTrains(from, to);
        if (matching.isEmpty()) {
            searchErrorLabel.setText("No trains found for this route.");
            return;
        }

        currentFrom = from;
        currentTo = to;
        currentDate = date.format(DATE_FORMAT);
        refreshTrainTable(matching, from, to);
        refreshTrainQuickSelect(matching);
        resultHeader.setText("Available Trains: " + from + " to " + to + " on " + currentDate);
        cardLayout.show(rootPanel, "RESULTS");
    }

    private static LocalDate selectedDateFromPicker() {
        Integer day = (Integer) dayCombo.getSelectedItem();
        Integer year = (Integer) yearCombo.getSelectedItem();
        int month = monthCombo.getSelectedIndex() + 1;

        if (day == null || year == null || month < 1) {
            throw new DateTimeException("Date components missing");
        }

        return LocalDate.of(year, month, day);
    }

    private static void refreshTrainTable(List<Train> matchingTrains, String from, String to) {
        trainTableModel.setRowCount(0);
        int distanceKm = calculateRouteDistanceKm(from, to);

        for (Train train : matchingTrains) {
            String[] fromTimes = train.schedule.get(stations.get(from));
            String[] toTimes = train.schedule.get(stations.get(to));

            String departure = fromTimes[1];
            String arrival = toTimes[0];
            String farePreview = String.format(Locale.ENGLISH, "BDT %.2f", calculateFare(Math.max(distanceKm, 50), "Shovon", train.id));

            trainTableModel.addRow(new Object[]{
                    train.id,
                    train.name,
                    departure,
                    arrival,
                    from,
                    to,
                    distanceKm,
                    farePreview
            });
        }
    }

    private static void refreshTrainQuickSelect(List<Train> matching) {
        trainQuickSelectCombo.removeAllItems();
        trainQuickSelectCombo.addItem("Select train from list");
        for (Train train : matching) {
            trainQuickSelectCombo.addItem(train.id + " - " + train.name);
        }
        trainQuickSelectCombo.setSelectedIndex(0);
    }

    private static void syncTableWithQuickSelect() {
        int idx = trainQuickSelectCombo.getSelectedIndex();
        if (idx <= 0) {
            return;
        }
        int tableRow = idx - 1;
        if (tableRow >= 0 && tableRow < trainTable.getRowCount()) {
            trainTable.setRowSelectionInterval(tableRow, tableRow);
            trainTable.scrollRectToVisible(trainTable.getCellRect(tableRow, 0, true));
        }
    }

    private static void syncQuickSelectWithTable() {
        int row = trainTable.getSelectedRow();
        if (row >= 0 && row + 1 < trainQuickSelectCombo.getItemCount()) {
            trainQuickSelectCombo.setSelectedIndex(row + 1);
        }
    }

    private static void onProceedBooking() {
        int selectedRow = trainTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Select a train to continue.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String trainId = String.valueOf(trainTableModel.getValueAt(selectedRow, 0));
        selectedTrain = trains.get(trainId);

        bookingContextLabel.setText("Train: " + selectedTrain.name + " | " + currentFrom + " -> " + currentTo + " | " + currentDate);
        bookingErrorLabel.setText(" ");
        resetBookingForm();
        updateSeatAvailabilityLabel();
        updateDynamicFarePreview();
        cardLayout.show(rootPanel, "BOOKING");
    }

    private static void onConfirmBooking() {
        bookingErrorLabel.setText(" ");

        if (selectedTrain == null) {
            bookingErrorLabel.setText("No train selected.");
            return;
        }

        String passengerName = nameField.getText().trim();
        int ageValue = (int) ageSpinner.getValue();
        String phone = phoneField.getText().trim();

        if (passengerName.isEmpty()) {
            bookingErrorLabel.setText("Passenger name is required.");
            return;
        }

        if (!phone.matches("\\d{10,15}")) {
            bookingErrorLabel.setText("Phone must contain 10 to 15 digits.");
            return;
        }

        String selectedClass = String.valueOf(classCombo.getSelectedItem());
        String seatPreference = String.valueOf(seatCombo.getSelectedItem());

        String assignedSeat = allocateSeatNumber(selectedTrain.id, currentDate, selectedClass, seatPreference);
        if (assignedSeat == null) {
            bookingErrorLabel.setText("No seats left in " + selectedClass + ". Choose another class.");
            updateSeatAvailabilityLabel();
            return;
        }

        int distanceKm = Math.max(calculateRouteDistanceKm(currentFrom, currentTo), 50);
        double fare = calculateFare(distanceKm, selectedClass, selectedTrain.id);

        String pnr = generatePnr();
        Booking booking = new Booking(
                pnr,
                selectedTrain,
                currentFrom,
                currentTo,
                currentDate,
                passengerName,
                String.valueOf(ageValue),
                String.valueOf(genderCombo.getSelectedItem()),
                selectedClass,
                seatPreference,
                assignedSeat,
                String.valueOf(paymentCombo.getSelectedItem()),
                phone,
                fare
        );

        bookings.add(booking);
        appendBookingToFile(booking);
        updateConfirmationText(booking);
        updateSeatAvailabilityLabel();
        cardLayout.show(rootPanel, "CONFIRMATION");
    }

    private static void updateDynamicFarePreview() {
        if (selectedTrain == null || classCombo == null) {
            return;
        }
        String selectedClass = String.valueOf(classCombo.getSelectedItem());
        int distanceKm = Math.max(calculateRouteDistanceKm(currentFrom, currentTo), 50);
        double fare = calculateFare(distanceKm, selectedClass, selectedTrain.id);
        bookingContextLabel.setText(
                "Train: " + selectedTrain.name + " | " + currentFrom + " -> " + currentTo + " | " + currentDate +
                        " | Approx Fare: BDT " + String.format(Locale.ENGLISH, "%.2f", fare)
        );
    }

    private static void updateConfirmationText(Booking booking) {
        StringBuilder sb = new StringBuilder();
        sb.append("PNR            : ").append(booking.pnr).append("\n");
        sb.append("Train          : ").append(booking.train.id).append(" - ").append(booking.train.name).append("\n");
        sb.append("Route          : ").append(booking.from).append(" -> ").append(booking.to).append("\n");
        sb.append("Journey Date   : ").append(booking.date).append("\n");
        sb.append("Passenger Name : ").append(booking.passengerName).append("\n");
        sb.append("Age/Gender     : ").append(booking.age).append(" / ").append(booking.gender).append("\n");
        sb.append("Class          : ").append(booking.travelClass).append("\n");
        sb.append("Seat Preference: ").append(booking.seatPreference).append("\n");
        sb.append("Assigned Seat  : ").append(booking.assignedSeat).append("\n");
        sb.append("Payment Mode   : ").append(booking.paymentMode).append("\n");
        sb.append("Fare           : BDT ").append(String.format(Locale.ENGLISH, "%.2f", booking.fareAmount)).append("\n");
        sb.append("Phone          : ").append(booking.phone).append("\n");

        confirmationArea.setText(sb.toString());
        confirmationArea.setCaretPosition(0);
    }

    private static void openSeatMapDialog() {
        if (selectedTrain == null || currentDate == null) {
            JOptionPane.showMessageDialog(frame, "Select a train before opening seat map.", "No Train", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String selectedClass = String.valueOf(classCombo.getSelectedItem());
        int capacity = getEffectiveCapacity(selectedTrain.id, selectedClass);
        List<SeatInfo> seats = buildSeatCatalog(selectedClass, capacity);

        String journeyKey = seatInventoryKey(selectedTrain.id, currentDate, selectedClass);
        Set<String> occupied = reservedSeatsByJourneyClass.getOrDefault(journeyKey, new HashSet<>());

        Map<String, JPanel> coachPanels = new LinkedHashMap<>();
        JPanel coachesContainer = new JPanel();
        coachesContainer.setLayout(new BoxLayout(coachesContainer, BoxLayout.Y_AXIS));

        for (SeatInfo seat : seats) {
            JPanel coachPanel = coachPanels.computeIfAbsent(seat.coachCode, key -> {
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
                panel.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.GRAY), "Coach " + key));
                coachesContainer.add(panel);
                return panel;
            });

            JButton seatButton = new JButton(seat.seatCode);
            seatButton.setEnabled(false);
            seatButton.setFont(new Font("SansSerif", Font.PLAIN, 11));

            if (occupied.contains(seat.seatCode)) {
                seatButton.setBackground(new Color(220, 90, 90));
                seatButton.setToolTipText("Booked");
            } else {
                seatButton.setBackground(new Color(90, 180, 120));
                seatButton.setToolTipText("Available - " + seat.berthType);
            }
            seatButton.setOpaque(true);
            coachPanel.add(seatButton);
        }

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel available = new JLabel("Available");
        available.setOpaque(true);
        available.setBackground(new Color(90, 180, 120));
        available.setBorder(new EmptyBorder(4, 8, 4, 8));

        JLabel booked = new JLabel("Booked");
        booked.setOpaque(true);
        booked.setBackground(new Color(220, 90, 90));
        booked.setBorder(new EmptyBorder(4, 8, 4, 8));

        legend.add(available);
        legend.add(booked);

        JDialog dialog = new JDialog(frame, "Coach-wise Seat Map", true);
        dialog.setSize(900, 560);
        dialog.setLocationRelativeTo(frame);
        dialog.setLayout(new BorderLayout(8, 8));

        JLabel top = new JLabel("Train " + selectedTrain.id + " | Class: " + selectedClass + " | Date: " + currentDate);
        top.setBorder(new EmptyBorder(8, 8, 0, 8));

        dialog.add(top, BorderLayout.NORTH);
        dialog.add(new JScrollPane(coachesContainer), BorderLayout.CENTER);
        dialog.add(legend, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private static void openBookingHistoryDialog() {
        JDialog dialog = new JDialog(frame, "Booking History", true);
        dialog.setSize(1120, 420);
        dialog.setLocationRelativeTo(frame);

        String[] columns = {
                "PNR", "Train ID", "From", "To", "Date", "Passenger", "Age", "Gender", "Class", "Pref", "Seat", "Payment", "Phone", "Fare"
        };

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (Booking booking : bookings) {
            model.addRow(new Object[]{
                    booking.pnr,
                    booking.train.id,
                    booking.from,
                    booking.to,
                    booking.date,
                    booking.passengerName,
                    booking.age,
                    booking.gender,
                    booking.travelClass,
                    booking.seatPreference,
                    booking.assignedSeat,
                    booking.paymentMode,
                    booking.phone,
                    String.format(Locale.ENGLISH, "%.2f", booking.fareAmount)
            });
        }

        JTable historyTable = new JTable(model);
        historyTable.setRowHeight(24);
        dialog.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        actions.add(closeBtn);

        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private static void cancelBookingByPnrPrompt() {
        String pnr = JOptionPane.showInputDialog(frame, "Enter PNR to cancel:", "Cancel Booking", JOptionPane.QUESTION_MESSAGE);
        if (pnr == null) {
            return;
        }

        String trimmed = pnr.trim();
        if (trimmed.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "PNR cannot be empty.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Booking target = null;
        for (Booking booking : bookings) {
            if (booking.pnr.equalsIgnoreCase(trimmed)) {
                target = booking;
                break;
            }
        }

        if (target == null) {
            JOptionPane.showMessageDialog(frame, "No booking found for PNR " + trimmed + ".", "Not Found", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        CancellationBreakdown breakdown = calculateCancellationBreakdown(target);

        int confirm = JOptionPane.showConfirmDialog(
                frame,
                "Cancel booking for " + target.passengerName + "?\n"
                        + "Fare: BDT " + String.format(Locale.ENGLISH, "%.2f", target.fareAmount) + "\n"
                        + "Cancellation charge: BDT " + String.format(Locale.ENGLISH, "%.2f", breakdown.charge) + "\n"
                        + "Refund: BDT " + String.format(Locale.ENGLISH, "%.2f", breakdown.refund) + "\n"
                        + "Rule: " + breakdown.ruleLabel,
                "Confirm Cancellation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        bookings.remove(target);
        releaseSeat(target);
        rewriteAllBookingsToFile();
        updateSeatAvailabilityLabel();

        JOptionPane.showMessageDialog(
                frame,
                "Booking cancelled for PNR " + target.pnr + ".\n"
                        + "Cancellation charge: BDT " + String.format(Locale.ENGLISH, "%.2f", breakdown.charge) + "\n"
                        + "Refund amount: BDT " + String.format(Locale.ENGLISH, "%.2f", breakdown.refund),
                "Cancelled",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private static CancellationBreakdown calculateCancellationBreakdown(Booking booking) {
        LocalDate journeyDate;
        try {
            journeyDate = LocalDate.parse(booking.date, DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            return new CancellationBreakdown(booking.fareAmount, 0, "Date parse failed. Full charge applied.");
        }

        long daysUntilJourney = ChronoUnit.DAYS.between(LocalDate.now(), journeyDate);
        double chargeRatio;
        String rule;

        if (daysUntilJourney >= 7) {
            chargeRatio = 0.10;
            rule = "More than or equal to 7 days before journey: 10% charge";
        } else if (daysUntilJourney >= 2) {
            chargeRatio = 0.25;
            rule = "2 to 6 days before journey: 25% charge";
        } else if (daysUntilJourney >= 0) {
            chargeRatio = 0.50;
            rule = "0 to 1 day before journey: 50% charge";
        } else {
            chargeRatio = 1.00;
            rule = "After journey date: 100% charge";
        }

        double charge = round2(booking.fareAmount * chargeRatio);
        double refund = round2(Math.max(0.0, booking.fareAmount - charge));
        return new CancellationBreakdown(charge, refund, rule);
    }

    private static void clearBookingFileWithConfirmation() {
        int result = JOptionPane.showConfirmDialog(
                frame,
                "This will remove all booking records from memory and bookings.txt. Continue?",
                "Confirm Clear",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        bookings.clear();
        reservedSeatsByJourneyClass.clear();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(BOOKING_FILE, false))) {
            writer.write("");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Unable to clear file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(frame, "Booking history cleared.", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private static void loadEffectiveAdminValues() {
        String trainId = selectedAdminTrainId();
        String travelClass = String.valueOf(adminClassCombo.getSelectedItem());
        if (trainId == null || travelClass == null) {
            return;
        }

        adminCapacitySpinner.setValue(getEffectiveCapacity(trainId, travelClass));
        adminRateSpinner.setValue(getEffectiveRate(trainId, travelClass));
    }

    private static void saveAdminOverride() {
        String trainId = selectedAdminTrainId();
        String travelClass = String.valueOf(adminClassCombo.getSelectedItem());
        if (trainId == null || travelClass == null) {
            return;
        }

        int capacity = (int) adminCapacitySpinner.getValue();
        double rate = ((Number) adminRateSpinner.getValue()).doubleValue();

        trainClassCapacityOverrides.computeIfAbsent(trainId, k -> new HashMap<>()).put(travelClass, capacity);
        trainClassRateOverrides.computeIfAbsent(trainId, k -> new HashMap<>()).put(travelClass, rate);

        refreshAdminConfigTable();
        updateSeatAvailabilityLabel();
        JOptionPane.showMessageDialog(frame, "Override saved for train " + trainId + " / " + travelClass + ".", "Saved", JOptionPane.INFORMATION_MESSAGE);
    }

    private static void resetAdminOverride() {
        String trainId = selectedAdminTrainId();
        String travelClass = String.valueOf(adminClassCombo.getSelectedItem());
        if (trainId == null || travelClass == null) {
            return;
        }

        Map<String, Integer> capMap = trainClassCapacityOverrides.get(trainId);
        if (capMap != null) {
            capMap.remove(travelClass);
        }
        Map<String, Double> rateMap = trainClassRateOverrides.get(trainId);
        if (rateMap != null) {
            rateMap.remove(travelClass);
        }

        loadEffectiveAdminValues();
        refreshAdminConfigTable();
        JOptionPane.showMessageDialog(frame, "Override reset for " + trainId + " / " + travelClass + ".", "Reset", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String selectedAdminTrainId() {
        Object selected = adminTrainCombo.getSelectedItem();
        if (selected == null) {
            return null;
        }
        String text = String.valueOf(selected);
        int split = text.indexOf(" - ");
        if (split < 0) {
            return null;
        }
        return text.substring(0, split);
    }

    private static void refreshAdminConfigTable() {
        adminConfigTableModel.setRowCount(0);

        for (Train train : trains.values()) {
            for (String travelClass : DEFAULT_CLASS_CAPACITY.keySet()) {
                boolean capOverride = hasCapacityOverride(train.id, travelClass);
                boolean rateOverride = hasRateOverride(train.id, travelClass);
                String source = (capOverride || rateOverride) ? "Override" : "Default";

                adminConfigTableModel.addRow(new Object[]{
                        train.id + " - " + train.name,
                        travelClass,
                        getEffectiveCapacity(train.id, travelClass),
                        String.format(Locale.ENGLISH, "%.2f", getEffectiveRate(train.id, travelClass)),
                        source
                });
            }
        }
    }

    private static boolean hasCapacityOverride(String trainId, String travelClass) {
        return trainClassCapacityOverrides.containsKey(trainId) && trainClassCapacityOverrides.get(trainId).containsKey(travelClass);
    }

    private static boolean hasRateOverride(String trainId, String travelClass) {
        return trainClassRateOverrides.containsKey(trainId) && trainClassRateOverrides.get(trainId).containsKey(travelClass);
    }

    private static void clearSearchState() {
        currentFrom = null;
        currentTo = null;
        currentDate = null;
        selectedTrain = null;
        if (searchErrorLabel != null) {
            searchErrorLabel.setText(" ");
        }
        if (trainTableModel != null) {
            trainTableModel.setRowCount(0);
        }
        if (dayCombo != null && monthCombo != null && yearCombo != null) {
            LocalDate today = LocalDate.now();
            monthCombo.setSelectedIndex(today.getMonthValue() - 1);
            yearCombo.setSelectedItem(today.getYear());
            refreshDayOptions();
            dayCombo.setSelectedItem(today.getDayOfMonth());
        }
        resetBookingForm();
    }

    private static void resetBookingForm() {
        if (nameField != null) {
            nameField.setText("");
        }
        if (ageSpinner != null) {
            ageSpinner.setValue(25);
        }
        if (genderCombo != null) {
            genderCombo.setSelectedIndex(0);
        }
        if (classCombo != null) {
            classCombo.setSelectedIndex(0);
        }
        if (seatCombo != null) {
            seatCombo.setSelectedIndex(3);
        }
        if (paymentCombo != null) {
            paymentCombo.setSelectedIndex(0);
        }
        if (phoneField != null) {
            phoneField.setText("");
        }
        if (bookingErrorLabel != null) {
            bookingErrorLabel.setText(" ");
        }
        updateSeatAvailabilityLabel();
    }

    private static boolean isValidLogin(String username, String password) {
        return "admin".equals(username) && "admin".equals(password);
    }

    private static String generatePnr() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ENGLISH);
    }

    private static List<Train> getMatchingTrains(String from, String to) {
        List<Train> matches = new ArrayList<>();
        Station fromStation = stations.get(from);
        Station toStation = stations.get(to);

        if (fromStation == null || toStation == null) {
            return matches;
        }

        for (Train train : trains.values()) {
            List<Station> stops = train.getStops();
            int fromIndex = stops.indexOf(fromStation);
            int toIndex = stops.indexOf(toStation);

            if (fromIndex >= 0 && toIndex > fromIndex) {
                matches.add(train);
            }
        }
        return matches;
    }

    private static int getEffectiveCapacity(String trainId, String travelClass) {
        if (trainClassCapacityOverrides.containsKey(trainId)) {
            Integer value = trainClassCapacityOverrides.get(trainId).get(travelClass);
            if (value != null) {
                return value;
            }
        }
        return DEFAULT_CLASS_CAPACITY.getOrDefault(travelClass, 40);
    }

    private static double getEffectiveRate(String trainId, String travelClass) {
        if (trainClassRateOverrides.containsKey(trainId)) {
            Double value = trainClassRateOverrides.get(trainId).get(travelClass);
            if (value != null) {
                return value;
            }
        }
        return DEFAULT_CLASS_RATE_PER_KM.getOrDefault(travelClass, 2.0);
    }

    private static double calculateFare(int distanceKm, String travelClass, String trainId) {
        double rate = getEffectiveRate(trainId, travelClass);
        double baseFare = distanceKm * rate;
        double reservationCharge = 40.0;
        return round2(baseFare + reservationCharge);
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static int calculateRouteDistanceKm(String from, String to) {
        Station source = stations.get(from);
        Station destination = stations.get(to);
        if (source == null || destination == null) {
            return -1;
        }
        if (source == destination) {
            return 0;
        }

        Map<Station, Integer> distance = new HashMap<>();
        for (Station station : stations.values()) {
            distance.put(station, Integer.MAX_VALUE);
        }
        distance.put(source, 0);

        PriorityQueue<StationDistance> pq = new PriorityQueue<>(Comparator.comparingInt(sd -> sd.distance));
        pq.add(new StationDistance(source, 0));

        while (!pq.isEmpty()) {
            StationDistance current = pq.poll();
            if (current.distance > distance.get(current.station)) {
                continue;
            }
            if (current.station == destination) {
                return current.distance;
            }

            for (Track track : current.station.tracks) {
                int nextDistance = current.distance + track.distance;
                if (nextDistance < distance.get(track.to)) {
                    distance.put(track.to, nextDistance);
                    pq.add(new StationDistance(track.to, nextDistance));
                }
            }
        }

        return -1;
    }

    private static String seatInventoryKey(String trainId, String date, String travelClass) {
        return trainId + "|" + date + "|" + travelClass;
    }

    private static String allocateSeatNumber(String trainId, String date, String travelClass, String preference) {
        int capacity = getEffectiveCapacity(trainId, travelClass);
        String key = seatInventoryKey(trainId, date, travelClass);
        Set<String> occupied = reservedSeatsByJourneyClass.computeIfAbsent(key, k -> new HashSet<>());

        List<SeatInfo> seatCatalog = buildSeatCatalog(travelClass, capacity);
        String preferred = normalizePreference(preference);

        for (SeatInfo seat : seatCatalog) {
            if (!"No Preference".equals(preferred) && !seat.berthType.equals(preferred)) {
                continue;
            }
            if (!occupied.contains(seat.seatCode)) {
                occupied.add(seat.seatCode);
                return seat.seatCode;
            }
        }

        for (SeatInfo seat : seatCatalog) {
            if (!occupied.contains(seat.seatCode)) {
                occupied.add(seat.seatCode);
                return seat.seatCode;
            }
        }

        return null;
    }

    private static List<SeatInfo> buildSeatCatalog(String travelClass, int capacity) {
        List<SeatInfo> seats = new ArrayList<>();

        String coachPrefix;
        int seatsPerCoach;

        switch (travelClass) {
            case "AC First" -> {
                coachPrefix = "F";
                seatsPerCoach = 10;
            }
            case "AC Chair" -> {
                coachPrefix = "C";
                seatsPerCoach = 20;
            }
            case "Sleeper" -> {
                coachPrefix = "SL";
                seatsPerCoach = 24;
            }
            default -> {
                coachPrefix = "SV";
                seatsPerCoach = 30;
            }
        }

        for (int i = 1; i <= capacity; i++) {
            int coachIndex = ((i - 1) / seatsPerCoach) + 1;
            int seatInCoach = ((i - 1) % seatsPerCoach) + 1;
            String berthType = berthTypeBySeatNumber(i);
            String coachCode = coachPrefix + coachIndex;
            String seatCode = coachCode + "-" + String.format(Locale.ENGLISH, "%02d", seatInCoach) + berthMarker(berthType);
            seats.add(new SeatInfo(coachCode, seatCode, berthType));
        }

        return seats;
    }

    private static String berthTypeBySeatNumber(int seatNo) {
        int mod = seatNo % 3;
        if (mod == 1) {
            return "Window";
        }
        if (mod == 2) {
            return "Middle";
        }
        return "Aisle";
    }

    private static String berthMarker(String berthType) {
        if ("Window".equals(berthType)) {
            return "W";
        }
        if ("Middle".equals(berthType)) {
            return "M";
        }
        return "A";
    }

    private static String normalizePreference(String preference) {
        if ("Window".equals(preference) || "Middle".equals(preference) || "Aisle".equals(preference)) {
            return preference;
        }
        return "No Preference";
    }

    private static void releaseSeat(Booking booking) {
        if (booking == null || booking.assignedSeat == null || booking.assignedSeat.trim().isEmpty() || "N/A".equals(booking.assignedSeat)) {
            return;
        }

        String key = seatInventoryKey(booking.train.id, booking.date, booking.travelClass);
        Set<String> occupied = reservedSeatsByJourneyClass.get(key);
        if (occupied != null) {
            occupied.remove(booking.assignedSeat);
            if (occupied.isEmpty()) {
                reservedSeatsByJourneyClass.remove(key);
            }
        }
    }

    private static void reserveLoadedSeat(Booking booking) {
        if (booking.assignedSeat == null || booking.assignedSeat.trim().isEmpty() || "N/A".equals(booking.assignedSeat)) {
            return;
        }
        String key = seatInventoryKey(booking.train.id, booking.date, booking.travelClass);
        reservedSeatsByJourneyClass.computeIfAbsent(key, k -> new HashSet<>()).add(booking.assignedSeat);
    }

    private static void updateSeatAvailabilityLabel() {
        if (seatAvailabilityLabel == null || classCombo == null) {
            return;
        }

        String selectedClass = String.valueOf(classCombo.getSelectedItem());
        if (selectedTrain == null || currentDate == null || selectedClass == null) {
            seatAvailabilityLabel.setText("Seats left: -");
            return;
        }

        int capacity = getEffectiveCapacity(selectedTrain.id, selectedClass);
        String key = seatInventoryKey(selectedTrain.id, currentDate, selectedClass);
        int used = reservedSeatsByJourneyClass.getOrDefault(key, new HashSet<>()).size();
        int left = Math.max(0, capacity - used);
        seatAvailabilityLabel.setText("Seats left in " + selectedClass + ": " + left + " / " + capacity);
    }

    private static void appendBookingToFile(Booking booking) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(BOOKING_FILE, true))) {
            writer.write(String.join(",",
                    booking.pnr,
                    booking.train.id,
                    booking.from,
                    booking.to,
                    booking.date,
                    booking.passengerName,
                    booking.age,
                    booking.gender,
                    booking.travelClass,
                    booking.seatPreference,
                    booking.assignedSeat,
                    booking.paymentMode,
                    booking.phone,
                    String.format(Locale.ENGLISH, "%.2f", booking.fareAmount)
            ));
            writer.newLine();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Booking saved in memory but file write failed: " + ex.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static void rewriteAllBookingsToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(BOOKING_FILE, false))) {
            for (Booking booking : bookings) {
                writer.write(String.join(",",
                        booking.pnr,
                        booking.train.id,
                        booking.from,
                        booking.to,
                        booking.date,
                        booking.passengerName,
                        booking.age,
                        booking.gender,
                        booking.travelClass,
                        booking.seatPreference,
                        booking.assignedSeat,
                        booking.paymentMode,
                        booking.phone,
                        String.format(Locale.ENGLISH, "%.2f", booking.fareAmount)
                ));
                writer.newLine();
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Failed to rewrite bookings file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void loadBookings() {
        File file = new File(BOOKING_FILE);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length < 8) {
                    continue;
                }

                Booking booking;
                if (parts.length >= 14) {
                    Train train = trains.getOrDefault(parts[1], buildUnknownTrain(parts[1]));
                    booking = new Booking(
                            parts[0],
                            train,
                            parts[2],
                            parts[3],
                            parts[4],
                            parts[5],
                            parts[6],
                            parts[7],
                            parts[8],
                            parts[9],
                            parts[10],
                            parts[11],
                            parts[12],
                            parseDoubleSafely(parts[13])
                    );
                } else if (parts.length >= 12) {
                    Train train = trains.getOrDefault(parts[1], buildUnknownTrain(parts[1]));
                    booking = new Booking(
                            parts[0],
                            train,
                            parts[2],
                            parts[3],
                            parts[4],
                            parts[5],
                            parts[6],
                            parts[7],
                            parts[8],
                            parts[9],
                            "N/A",
                            parts[10],
                            parts[11],
                            calculateFare(Math.max(calculateRouteDistanceKm(parts[2], parts[3]), 50), parts[8], parts[1])
                    );
                } else {
                    Train train = trains.getOrDefault(parts[0], buildUnknownTrain(parts[0]));
                    booking = new Booking(
                            generatePnr(),
                            train,
                            parts[1],
                            parts[2],
                            parts[3],
                            parts[4],
                            "N/A",
                            parts[5],
                            "Shovon",
                            parts[6],
                            "N/A",
                            parts[7],
                            "N/A",
                            calculateFare(Math.max(calculateRouteDistanceKm(parts[1], parts[2]), 50), "Shovon", parts[0])
                    );
                }

                bookings.add(booking);
                reserveLoadedSeat(booking);
            }
        } catch (IOException ex) {
            System.err.println("Failed to load bookings: " + ex.getMessage());
        }
    }

    private static double parseDoubleSafely(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private static Train buildUnknownTrain(String id) {
        Train train = new Train(id, "Unknown Train");
        trains.putIfAbsent(id, train);
        return trains.get(id);
    }

    private static String normalizedComboValue(JComboBox<String> comboBox) {
        Object selectedRaw = comboBox.getSelectedItem();
        String selectedResolved = resolveStationName(String.valueOf(selectedRaw));
        if (selectedResolved != null) {
            return selectedResolved;
        }

        Object editorRaw = comboBox.getEditor().getItem();
        String editorResolved = resolveStationName(String.valueOf(editorRaw));
        if (editorResolved != null) {
            return editorResolved;
        }

        return null;
    }

    private static String resolveStationName(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        String entered = rawValue.trim();
        if (entered.isEmpty()) {
            return null;
        }

        // Friendly aliases for common spellings.
        String normalized = entered.toLowerCase(Locale.ENGLISH);
        if ("bangalore".equals(normalized)) {
            normalized = "bengaluru";
        } else if ("trivandrum".equals(normalized)) {
            normalized = "thiruvananthapuram";
        }

        for (String station : stations.keySet()) {
            if (station.equalsIgnoreCase(normalized)) {
                return station;
            }
        }

        // Accept unique prefix/contains matches from typed search text.
        String uniqueMatch = null;
        for (String station : stations.keySet()) {
            String stationLower = station.toLowerCase(Locale.ENGLISH);
            if (stationLower.startsWith(normalized) || stationLower.contains(normalized)) {
                if (uniqueMatch != null) {
                    return null;
                }
                uniqueMatch = station;
            }
        }
        if (uniqueMatch != null) {
            return uniqueMatch;
        }

        return null;
    }

    private static void enableComboSearch(JComboBox<String> comboBox, List<String> sourceValues) {
        JTextField editor = (JTextField) comboBox.getEditor().getEditorComponent();
        editor.addActionListener(e -> {
            String text = editor.getText().trim();
            String filter = text.toLowerCase(Locale.ENGLISH);
            comboBox.removeAllItems();
            for (String value : sourceValues) {
                if (value.toLowerCase(Locale.ENGLISH).contains(filter)) {
                    comboBox.addItem(value);
                }
            }
            if (comboBox.getItemCount() > 0) {
                comboBox.setSelectedIndex(0);
            }
            editor.setText(text);
        });

        comboBox.addPopupMenuListener(new PopupMenuListenerAdapter() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                String current = editor.getText().trim().toLowerCase(Locale.ENGLISH);
                comboBox.removeAllItems();
                for (String value : sourceValues) {
                    if (value.toLowerCase(Locale.ENGLISH).contains(current)) {
                        comboBox.addItem(value);
                    }
                }
            }
        });
    }

    private static void setupData() {
        Station bangalore = new Station("Bengaluru");
        Station mysore = new Station("Mysore");
        Station chennai = new Station("Chennai");
        Station trichy = new Station("Trichy");
        Station madurai = new Station("Madurai");
        Station coimbatore = new Station("Coimbatore");
        Station salem = new Station("Salem");
        Station palakkad = new Station("Palakkad");
        Station ernakulam = new Station("Ernakulam");
        Station thrissur = new Station("Thrissur");
        Station kozhikode = new Station("Kozhikode");
        Station kannur = new Station("Kannur");
        Station thiruvananthapuram = new Station("Thiruvananthapuram");

        stations.put("Bengaluru", bangalore);
        stations.put("Mysore", mysore);
        stations.put("Chennai", chennai);
        stations.put("Trichy", trichy);
        stations.put("Madurai", madurai);
        stations.put("Coimbatore", coimbatore);
        stations.put("Salem", salem);
        stations.put("Palakkad", palakkad);
        stations.put("Ernakulam", ernakulam);
        stations.put("Thrissur", thrissur);
        stations.put("Kozhikode", kozhikode);
        stations.put("Kannur", kannur);
        stations.put("Thiruvananthapuram", thiruvananthapuram);

        addTrack(bangalore, mysore, 145);
        addTrack(bangalore, salem, 200);
        addTrack(bangalore, chennai, 360);
        addTrack(chennai, trichy, 330);
        addTrack(trichy, madurai, 135);
        addTrack(salem, coimbatore, 165);
        addTrack(coimbatore, palakkad, 55);
        addTrack(palakkad, thrissur, 70);
        addTrack(thrissur, ernakulam, 75);
        addTrack(ernakulam, thiruvananthapuram, 220);
        addTrack(thrissur, kozhikode, 115);
        addTrack(kozhikode, kannur, 95);
        addTrack(madurai, thiruvananthapuram, 265);
        addTrack(coimbatore, ernakulam, 185);
        addTrack(trichy, salem, 160);

        createTrain("16022", "Kaveri Connector", bangalore, "06:00", "06:10", salem, "09:15", "09:25", trichy, "12:10", "12:20", madurai, "14:40", "14:50");
        createTrain("12613", "Mysore Chennai Day Express", mysore, "05:50", "06:00", bangalore, "08:30", "08:45", chennai, "13:10", "13:20");
        createTrain("12679", "Western Ghats Superfast", chennai, "06:20", "06:30", salem, "10:30", "10:40", coimbatore, "13:00", "13:10", palakkad, "14:00", "14:10", ernakulam, "17:20", "17:30");
        createTrain("16343", "Malabar Link", ernakulam, "07:00", "07:10", thrissur, "08:20", "08:30", kozhikode, "10:50", "11:00", kannur, "12:30", "12:40");
        createTrain("16316", "Kerala Coast Express", kannur, "06:10", "06:20", kozhikode, "07:40", "07:50", thrissur, "10:00", "10:10", ernakulam, "11:25", "11:35", thiruvananthapuram, "16:00", "16:10");
        createTrain("12637", "Pandian Connector", madurai, "06:30", "06:40", trichy, "08:20", "08:30", chennai, "12:00", "12:10");
        createTrain("12685", "Southern Intercity", bangalore, "07:15", "07:25", salem, "10:00", "10:10", coimbatore, "12:20", "12:30", palakkad, "13:15", "13:25", thrissur, "14:20", "14:30", ernakulam, "15:40", "15:50");
        createTrain("16318", "Anantha Corridor", coimbatore, "05:40", "05:50", ernakulam, "09:05", "09:15", thiruvananthapuram, "13:30", "13:40");
        createTrain("12007", "Cauvery Malabar Special", chennai, "05:30", "05:40", bangalore, "10:30", "10:45", mysore, "13:00", "13:10", salem, "16:10", "16:20", coimbatore, "18:20", "18:30", kozhikode, "22:10", "22:20");
    }

    private static void addTrack(Station a, Station b, int distance) {
        a.tracks.add(new Track(b, distance));
        b.tracks.add(new Track(a, distance));
    }

    private static void createTrain(String id, String name, Object... stopData) {
        Train train = new Train(id, name);
        for (int i = 0; i < stopData.length; i += 3) {
            Station station = (Station) stopData[i];
            String arrival = (String) stopData[i + 1];
            String departure = (String) stopData[i + 2];
            train.schedule.put(station, new String[]{arrival, departure});
        }
        trains.put(id, train);
    }

    private static Map<String, Integer> createDefaultCapacityMap() {
        Map<String, Integer> capacity = new LinkedHashMap<>();
        capacity.put("AC First", 20);
        capacity.put("AC Chair", 48);
        capacity.put("Sleeper", 72);
        capacity.put("Shovon", 90);
        return capacity;
    }

    private static Map<String, Double> createDefaultRateMap() {
        Map<String, Double> rates = new LinkedHashMap<>();
        rates.put("AC First", 4.5);
        rates.put("AC Chair", 3.2);
        rates.put("Sleeper", 2.4);
        rates.put("Shovon", 1.8);
        return rates;
    }

    private static class StationDistance {
        Station station;
        int distance;

        StationDistance(Station station, int distance) {
            this.station = station;
            this.distance = distance;
        }
    }

    private static class CancellationBreakdown {
        double charge;
        double refund;
        String ruleLabel;

        CancellationBreakdown(double charge, double refund, String ruleLabel) {
            this.charge = charge;
            this.refund = refund;
            this.ruleLabel = ruleLabel;
        }
    }

    private abstract static class PopupMenuListenerAdapter implements javax.swing.event.PopupMenuListener {
        @Override
        public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
        }

        @Override
        public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
        }
    }

    private static class DateTimeException extends RuntimeException {
        DateTimeException(String message) {
            super(message);
        }
    }
}
