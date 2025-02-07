import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.control.Button;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Brugerflade extends Application {
    @FXML
    public Button btnClearGrid;
    @FXML
    private TextField txt1;
    @FXML
    private TextField txt2;
    @FXML
    private TextField txtSpacing;
    @FXML
    private Label lbl1;
    @FXML
    private Label lbl2;
    @FXML
    private LineChart<Number, Number> lineChart;
    @FXML
    private TextField txtPoints;
    @FXML
    private Button btnAddPoints;
    @FXML
    private Button btnPlotTrainingPoints;

    private Network network;
    private List<List<Double>> data;
    private List<Double> answers;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("Brugerflade.fxml")));
        loader.setController(this);
        Parent root = loader.load();
        primaryStage.setTitle("Brugerflade");
        primaryStage.setScene(new Scene(root, 1000, 1000));
        primaryStage.show();

        // Initialize the network
        network = new Network(20000, 0.01, new int[]{2, 20, 1});

        // Training data
        data = Arrays.asList(
                Arrays.asList(2.0, 3.0), Arrays.asList(3.0, 4.0),
                Arrays.asList(4.0, 5.0), Arrays.asList(1.0, 0.0),
                Arrays.asList(2.0, 1.0), Arrays.asList(3.0, 2.0),
                Arrays.asList(3.2, 2.76), Arrays.asList(-2.07, 3.79),
                Arrays.asList(-5.82, -3.97), Arrays.asList(3.11, -3.46),
                Arrays.asList(-2.27, -1.55), Arrays.asList(1.86, 2.16),
                Arrays.asList(8.18, 7.61), Arrays.asList(-8.31, 2.48)
        );
        answers = Arrays.asList(1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0);

        List<List<Double>> trainData = data.subList(0, 6);
        List<List<Double>> valData = Arrays.asList(
                Arrays.asList(3.0, 3.0), Arrays.asList(4.0, 4.0)
        );
        List<Double> trainAnswers = answers.subList(0, 6);
        List<Double> valAnswers = Arrays.asList(1.0, 1.0);

        // Train the network
        network.train(trainData, trainAnswers, valData, valAnswers);

        // Initialize the chart
        initializeChart();

        // Set the button action
        btnAddPoints.setOnAction(event -> handleAddPoints());
        btnPlotTrainingPoints.setOnAction(event -> plotTrainingPoints());
    }

    // Add this method to handle adding points
    @FXML
    private void handleAddPoints() {
        try {
            int numPoints = Integer.parseInt(txtPoints.getText());
            int gridSize = (int) Math.sqrt(numPoints);
            double spacing = Double.parseDouble(txtSpacing.getText());

            // Clear previous grid points
            lineChart.getData().removeIf(series -> series.getName().startsWith("Grid Point"));

            // Create a task to generate and add points in the background
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    List<XYChart.Series<Number, Number>> seriesList = new ArrayList<>();
                    for (int i = -gridSize / 2; i <= gridSize / 2; i++) {
                        for (int j = -gridSize / 2; j <= gridSize / 2; j++) {
                            double x = i * spacing;
                            double y = j * spacing;
                            double prediction = network.predict(x, y);

                            XYChart.Series<Number, Number> pointSeries = new XYChart.Series<>();
                            pointSeries.setName("Grid Point (" + i + "," + j + ")");
                            XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(x, y);

                            dataPoint.nodeProperty().addListener((observable, oldValue, newValue) -> {
                                if (newValue != null) {
                                    if ((y > x && prediction > 0.5) || (y <= x && prediction <= 0.5)) {
                                        newValue.setStyle("-fx-background-color: green;");
                                    } else {
                                        newValue.setStyle("-fx-background-color: red;");
                                    }
                                }
                            });

                            pointSeries.getData().add(dataPoint);
                            seriesList.add(pointSeries);
                        }
                    }

                    // Update the chart on the JavaFX Application Thread
                    Platform.runLater(() -> lineChart.getData().addAll(seriesList));
                    return null;
                }
            };

            // Start the task in a new thread
            new Thread(task).start();
        } catch (NumberFormatException e) {
            lbl1.setText("Invalid number of points");
        }
    }

    private void initializeChart() {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("x = y Line");
        for (int i = -10; i <= 10; i++) {
            series.getData().add(new XYChart.Data<>(i, i));
        }
        lineChart.getData().add(series);
    }

    private void plotTrainingPoints() {
        char label = 'A';
        for (int i = 0; i < data.size(); i++) {
            List<Double> point = data.get(i);
            double x = point.get(0);
            double y = point.get(1);
            double answer = answers.get(i);

            XYChart.Series<Number, Number> pointSeries = new XYChart.Series<>();
            pointSeries.setName("Point " + label);
            XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(x, y);

            dataPoint.nodeProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    if ((y > x && answer > 0.5) || (y <= x && answer <= 0.5)) {
                        newValue.setStyle("-fx-background-color: green;");
                    } else {
                        newValue.setStyle("-fx-background-color: red;");
                    }
                }
            });

            pointSeries.getData().add(dataPoint);
            lineChart.getData().add(pointSeries);
            label++;
        }
    }

    @FXML
    private void handleClearGrid() {
        lineChart.getData().removeIf(series ->
            series.getName().startsWith("Grid Point") ||
            series.getName().startsWith("Prediction Point") ||
            series.getName().startsWith("Point")
        );
    }

    @FXML
    private void handlePredict() {
        try {
            double value1 = Double.parseDouble(txt1.getText());
            double value2 = Double.parseDouble(txt2.getText());
            double prediction = network.predict(value1, value2);
            lbl1.setText(String.format("Prediction: %.10f", prediction));

            // Clear previous prediction points
            lineChart.getData().removeIf(series -> "Prediction Point".equals(series.getName()));

            // Collect the new prediction point
            XYChart.Series<Number, Number> pointSeries = new XYChart.Series<>();
            pointSeries.setName("Prediction Point");
            XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(value1, value2);

            dataPoint.nodeProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    if ((value2 > value1 && prediction > 0.5) || (value2 <= value1 && prediction <= 0.5)) {
                        newValue.setStyle("-fx-background-color: green;");
                        lbl2.setText("Prediction is correct");
                    } else {
                        newValue.setStyle("-fx-background-color: red;");
                        lbl2.setText("Prediction is incorrect");
                    }
                }
            });

            pointSeries.getData().add(dataPoint);

            // Add the series to the chart at once
            lineChart.getData().add(pointSeries);
        } catch (NumberFormatException e) {
            lbl1.setText("Invalid input");
        }
    }
}