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
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;
import java.util.Objects;

public class Brugerflade extends Application {
    @FXML
    private Canvas canvasAddPoints;
    @FXML
    private Canvas canvasPlotTrainingPoints;
    @FXML
    private Canvas canvasPredict;
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
    private Button btnAddPoints;
    @FXML
    private Button btnPlotTrainingPoints;
    @FXML
    private Button btnClearGrid;

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
        primaryStage.setTitle("Please wait while the network is training...");
        primaryStage.setScene(new Scene(root, 1000, 1000));
        primaryStage.show();

        // Bind canvas size to the parent container size
        canvasAddPoints.widthProperty().bind(((AnchorPane) canvasAddPoints.getParent()).widthProperty());
        canvasAddPoints.heightProperty().bind(((AnchorPane) canvasAddPoints.getParent()).heightProperty().subtract(166));
        canvasPlotTrainingPoints.widthProperty().bind(((AnchorPane) canvasPlotTrainingPoints.getParent()).widthProperty());
        canvasPlotTrainingPoints.heightProperty().bind(((AnchorPane) canvasPlotTrainingPoints.getParent()).heightProperty().subtract(166));
        canvasPredict.widthProperty().bind(((AnchorPane) canvasPredict.getParent()).widthProperty());
        canvasPredict.heightProperty().bind(((AnchorPane) canvasPredict.getParent()).heightProperty().subtract(166));

        // Initialize the network
        network = new Network(20000, 0.01, new int[]{2, 200, 1});

        // Training data
        data = List.of(
                List.of(2.0, 3.0), List.of(3.0, 4.0),
                List.of(4.0, 5.0), List.of(1.0, 0.0),
                List.of(2.0, 1.0), List.of(3.0, 2.0),
                List.of(3.2, 2.76), List.of(-2.07, 3.79),
                List.of(-5.82, -3.97), List.of(3.11, -3.46),
                List.of(-2.27, -1.55), List.of(1.86, 2.16),
                List.of(8.18, 7.61), List.of(-8.31, 2.48)
        );
        answers = List.of(1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0);

        // Train the network
        network.train(data.subList(0, 6), answers.subList(0, 6), List.of(List.of(3.0, 3.0), List.of(4.0, 4.0)), List.of(1.0, 1.0));

        // Set the title of the window
        primaryStage.setTitle("Neural Accuracy Map");

        // Set the button actions
        btnAddPoints.setOnAction(event -> handleAddPoints());
        btnPlotTrainingPoints.setOnAction(event -> plotTrainingPoints());
        btnClearGrid.setOnAction(event -> handleClearGrid());

        // Add a shutdown hook to stop all running threads
        primaryStage.setOnCloseRequest(event -> {
            System.exit(0);
        });
    }

    @FXML
    private void handleAddPoints() {
        try {
            int numPoints = Integer.parseInt(txtPoints.getText());
            int gridSize = (int) Math.sqrt(numPoints);
            double spacing = Double.parseDouble(txtSpacing.getText());
            System.out.println("Grid size: " + gridSize + ", Spacing: " + spacing);

            // Clear the canvas for add points
            GraphicsContext gc = canvasAddPoints.getGraphicsContext2D();
            gc.clearRect(0, 0, canvasAddPoints.getWidth(), canvasAddPoints.getHeight());

            // Calculate the scale factor to fit points within the canvas
            double scaleX = canvasAddPoints.getWidth() / (gridSize * spacing);
            double scaleY = canvasAddPoints.getHeight() / (gridSize * spacing);
            double scale = Math.min(scaleX, scaleY);

            // Create a task to generate and add points in the background
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    for (int i = -gridSize / 2; i <= gridSize / 2; i++) {
                        for (int j = -gridSize / 2; j <= gridSize / 2; j++) {
                            double x = i * spacing;
                            double y = j * spacing;
                            double prediction = network.predict(x, y);

                            // Draw the point on the canvas
                            Platform.runLater(() -> {
                                gc.save();
                                gc.scale(scale, scale);
                                if ((y > x && prediction > 0.5) || (y <= x && prediction <= 0.5)) {
                                    gc.setFill(Color.GREEN);
                                } else {
                                    gc.setFill(Color.RED);
                                }
                                gc.fillOval(x + canvasAddPoints.getWidth() / (2 * scale), y + canvasAddPoints.getHeight() / (2 * scale), 5 / scale, 5 / scale);
                                gc.restore();
                            });
                        }
                    }
                    return null;
                }
            };

            // Start the task in a new thread
            new Thread(task).start();
        } catch (NumberFormatException e) {
            lbl1.setText("Invalid number of points");
        }
    }

    private void plotTrainingPoints() {
        GraphicsContext gc = canvasPlotTrainingPoints.getGraphicsContext2D();
        gc.clearRect(0, 0, canvasPlotTrainingPoints.getWidth(), canvasPlotTrainingPoints.getHeight());

        // Calculate the scale factor to fit points within the canvas
        double maxX = data.stream().mapToDouble(point -> Math.abs(point.get(0))).max().orElse(1);
        double maxY = data.stream().mapToDouble(point -> Math.abs(point.get(1))).max().orElse(1);
        double scaleX = canvasPlotTrainingPoints.getWidth() / (2 * maxX);
        double scaleY = canvasPlotTrainingPoints.getHeight() / (2 * maxY);
        double scale = Math.min(scaleX, scaleY);

        for (int i = 0; i < data.size(); i++) {
            List<Double> point = data.get(i);
            double x = point.get(0);
            double y = point.get(1);
            double answer = answers.get(i);

            Platform.runLater(() -> {
                gc.save();
                gc.scale(scale, scale);
                if ((y > x && answer > 0.5) || (y <= x && answer <= 0.5)) {
                    gc.setFill(Color.GREEN);
                } else {
                    gc.setFill(Color.RED);
                }
                gc.fillOval(x + canvasPlotTrainingPoints.getWidth() / (2 * scale), y + canvasPlotTrainingPoints.getHeight() / (2 * scale), 5 / scale, 5 / scale);
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

            // Clear the canvas for predictions
            GraphicsContext gc = canvasPredict.getGraphicsContext2D();

            // Calculate the scale factor to fit points within the canvas
            double maxX = data.stream().mapToDouble(point -> Math.abs(point.get(0))).max().orElse(1);
            double maxY = data.stream().mapToDouble(point -> Math.abs(point.get(1))).max().orElse(1);
            double scaleX = canvasPredict.getWidth() / (2 * maxX);
            double scaleY = canvasPredict.getHeight() / (2 * maxY);
            double scale = Math.min(scaleX, scaleY);

            // Draw the prediction point on the canvas
            gc.save();
            gc.scale(scale, scale);
            if ((value2 > value1 && prediction > 0.5) || (value2 <= value1 && prediction <= 0.5)) {
                gc.setFill(Color.GREEN);
                lbl2.setText("Prediction is correct");
            } else {
                gc.setFill(Color.RED);
                lbl2.setText("Prediction is incorrect");
            }
            gc.fillOval(value1 + canvasPredict.getWidth() / (2 * scale), value2 + canvasPredict.getHeight() / (2 * scale), 5 / scale, 5 / scale);
            gc.restore();
        } catch (NumberFormatException e) {
            lbl1.setText("Invalid input");
        }
    }

    @FXML
    private void handleClearGrid() {
        GraphicsContext gcAddPoints = canvasAddPoints.getGraphicsContext2D();
        GraphicsContext gcPlotTrainingPoints = canvasPlotTrainingPoints.getGraphicsContext2D();
        GraphicsContext gcPredict = canvasPredict.getGraphicsContext2D();
        gcAddPoints.clearRect(0, 0, canvasAddPoints.getWidth(), canvasAddPoints.getHeight());
        gcPlotTrainingPoints.clearRect(0, 0, canvasPlotTrainingPoints.getWidth(), canvasPlotTrainingPoints.getHeight());
        gcPredict.clearRect(0, 0, canvasPredict.getWidth(), canvasPredict.getHeight());
    }
}