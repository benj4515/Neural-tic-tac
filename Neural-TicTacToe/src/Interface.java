import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Interface extends Application {
    @FXML
    private Canvas canvasUnified;
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
    private TextField txtPoints;
    @FXML
    private TextField txtEpochs;
    @FXML
    private TextField txtNeurons;
    @FXML
    private Button btnAddPoints;
    @FXML
    private Button btnPlotTrainingPoints;
    @FXML
    private Button btnClearGrid;
    @FXML
    private Button btnRetrain;
    @FXML
    private Slider sliderPointSize;
    @FXML
    private Slider sliderZoom;

    private Network network;
    private List<List<Double>> data;
    private List<Double> answers;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("Interface.fxml")));
        loader.setController(this);
        Parent root = loader.load();
        primaryStage.setTitle("Please wait while the network is training...");
        primaryStage.setScene(new Scene(root, 1000, 1000));
        primaryStage.show();

        // Bind canvas size to the parent container size
        canvasUnified.widthProperty().bind(((AnchorPane) canvasUnified.getParent()).widthProperty());
        canvasUnified.heightProperty().bind(((AnchorPane) canvasUnified.getParent()).heightProperty().subtract(166));

        // Training data points
        double[][] dataArray = {
                {2.0, 3.0},
                {3.0, 4.0},
                {4.0, 5.0},
                {1.0, 0.0},
                {2.0, 1.0},
                {3.0, 2.0},
                {3.2, 2.76},
                {-2.07, 3.79},
                {-5.82, -3.97},
                {3.11, -3.46},
                {-2.27, -1.55},
                {1.86, 2.16},
                {8.18, 7.61},
                {-8.31, 2.48}
        };
        // Answers for the corresponding training data points
        double[] answersArray = {1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0};

        // Determine the number of input neurons from the length of the first inner array
        int inputNeurons = dataArray[0].length;

        // Convert double[][] to List<List<Double>> and double[] to List<Double> in one line each
        data = Arrays.stream(dataArray).map(row -> Arrays.stream(row).boxed().collect(Collectors.toList())).collect(Collectors.toList());
        answers = Arrays.stream(answersArray).boxed().collect(Collectors.toList());

        // Initialize the network
        int epochs = 20000;
        int neurons = 200;

        // Initialize the network, InputNeurons are Dimensions of the dataArray, neurons is the number of neurons in the hidden layer and 1 is the output layer
        network = new Network(epochs, 0.01, new int[]{inputNeurons, neurons, 1});

        // Train the network, the sublist is the amount of data used for training.
        network.train(data, answers, List.of(List.of(3.0, 3.0), List.of(4.0, 4.0)), List.of(1.0, 1.0));

        // Set the title of the window
        primaryStage.setTitle("Neural Accuracy Map");

        // Set ReTraining TestField Text
        txtEpochs.setText(epochs + "");
        txtNeurons.setText(neurons + "");

        // Draw the line x = y
        //drawLine();

        // Set the button actions
        btnAddPoints.setOnAction(event -> handleAddPoints());
        btnPlotTrainingPoints.setOnAction(event -> plotTrainingPoints());
        btnClearGrid.setOnAction(event -> handleClearGrid());
        btnRetrain.setOnAction(event -> handleRetrain());

        // Add a shutdown hook to stop all running threads
        primaryStage.setOnCloseRequest(event -> {
            System.exit(0);
        });
    }

    private void drawOnCanvas(Runnable drawingLogic) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                Platform.runLater(drawingLogic);
                return null;
            }
        };
        new Thread(task).start();
    }

    private double getExponentialZoomValue() {
        return Math.exp(sliderZoom.getValue());
    }

    @FXML
    private void handleAddPoints() {
        try {
            int numPoints = Integer.parseInt(txtPoints.getText());
            int gridSize = (int) Math.sqrt(numPoints);
            double spacing = Double.parseDouble(txtSpacing.getText());
            double pointSize = sliderPointSize.getValue();
            double zoom = getExponentialZoomValue();
            System.out.println("Grid size: " + gridSize + ", Spacing: " + spacing + ", Point size: " + pointSize + ", Zoom: " + zoom);

            drawOnCanvas(() -> {
                GraphicsContext gc = canvasUnified.getGraphicsContext2D();
                gc.clearRect(0, 0, canvasUnified.getWidth(), canvasUnified.getHeight());

                for (int i = -gridSize / 2; i <= gridSize / 2; i++) {
                    for (int j = -gridSize / 2; j <= gridSize / 2; j++) {
                        double x = i * spacing;
                        double y = j * spacing;
                        double prediction = network.predict(x, y);

                        gc.save();
                        gc.scale(zoom, zoom);
                        if ((y > x && prediction > 0.5) || (y <= x && prediction <= 0.5)) {
                            gc.setFill(Color.GREEN);
                        } else {
                            gc.setFill(Color.RED);
                        }
                        gc.fillOval(x + canvasUnified.getWidth() / (2 * zoom), y + canvasUnified.getHeight() / (2 * zoom), pointSize / zoom, pointSize / zoom);
                        gc.restore();
                    }
                }
                // Draw the line x = y
                gc.setStroke(Color.BLACK);
                gc.setLineWidth(2);
                gc.strokeLine(0, 0, canvasUnified.getWidth(), canvasUnified.getHeight());
            });
        } catch (NumberFormatException e) {
            lbl1.setText("Invalid number of points");
        }
    }


    private void plotTrainingPoints() {
        GraphicsContext gc = canvasUnified.getGraphicsContext2D();
        gc.clearRect(0, 0, canvasUnified.getWidth(), canvasUnified.getHeight());

        double pointSize = sliderPointSize.getValue();
        double zoom = getExponentialZoomValue();

        for (int i = 0; i < data.size(); i++) {
            List<Double> point = data.get(i);
            double x = point.get(0);
            double y = point.get(1);
            double answer = answers.get(i);

            Platform.runLater(() -> {
                gc.save();
                gc.scale(zoom, zoom);
                if ((y > x && answer > 0.5) || (y <= x && answer <= 0.5)) {
                    gc.setFill(Color.GREEN);
                } else {
                    gc.setFill(Color.RED);
                }
                gc.fillOval(x + canvasUnified.getWidth() / (2 * zoom), y + canvasUnified.getHeight() / (2 * zoom), pointSize / zoom, pointSize / zoom);
                gc.restore();
            });
        }
    }

    @FXML
    private void handlePredict() {
        try {
            double value1 = Double.parseDouble(txt1.getText());
            double value2 = Double.parseDouble(txt2.getText());
            double prediction = network.predict(value1, value2);
            lbl1.setText(String.format("Prediction: %.10f", prediction));

            drawOnCanvas(() -> {
                GraphicsContext gc = canvasUnified.getGraphicsContext2D();
                double pointSize = sliderPointSize.getValue();
                double zoom = getExponentialZoomValue();

                gc.clearRect(0, 0, canvasUnified.getWidth(), canvasUnified.getHeight());

                gc.save();
                gc.scale(zoom, zoom);
                if ((value2 > value1 && prediction > 0.5) || (value2 <= value1 && prediction <= 0.5)) {
                    gc.setFill(Color.GREEN);
                    lbl2.setText("Prediction is correct");
                } else {
                    gc.setFill(Color.RED);
                    lbl2.setText("Prediction is incorrect");
                }
                gc.fillOval(value1 + canvasUnified.getWidth() / (2 * zoom), value2 + canvasUnified.getHeight() / (2 * zoom), pointSize / zoom, pointSize / zoom);
                gc.restore();
            });
        } catch (NumberFormatException e) {
            lbl1.setText("Invalid input");
        }
    }

    @FXML
    private void handleRetrain() {
        try {
            int epochs = Integer.parseInt(txtEpochs.getText());
            int neurons = Integer.parseInt(txtNeurons.getText());

            // Reinitialize the network with new parameters
            network = new Network(epochs, 0.01, new int[]{2, neurons, 1});

            // Retrain the network
            network.train(data.subList(0, 6), answers.subList(0, 6), List.of(List.of(3.0, 3.0), List.of(4.0, 4.0)), List.of(1.0, 1.0));

            lbl1.setText("Network retrained successfully");
        } catch (NumberFormatException e) {
            lbl1.setText("Invalid input for epochs or neurons");
        }
    }


    @FXML
    private void handleClearGrid() {
        GraphicsContext gcUnified = canvasUnified.getGraphicsContext2D();
        gcUnified.clearRect(0, 0, canvasUnified.getWidth(), canvasUnified.getHeight());
    }
}