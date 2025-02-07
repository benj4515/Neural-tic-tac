import java.util.*;

public class App {
    public static void main(String[] args) {
        App app = new App();
        app.trainAndPredict();
    }

    public void trainAndPredict() {
        List<List<Double>> data = Arrays.asList(
                Arrays.asList(2.0, 3.0), Arrays.asList(3.0, 4.0),
                Arrays.asList(4.0, 5.0), Arrays.asList(1.0, 0.0),
                Arrays.asList(2.0, 1.0), Arrays.asList(3.0, 2.0)
        );
        List<Double> answers = Arrays.asList(1.0, 0.0, 0.0, 0.0, 1.0, 1.0);

        List<List<Double>> trainData = data.subList(0, 6);
        List<List<Double>> valData = Arrays.asList(
                Arrays.asList(3.0, 3.0), Arrays.asList(4.0, 4.0)
        );
        List<Double> trainAnswers = answers.subList(0, 6);
        List<Double> valAnswers = Arrays.asList(1.0, 1.0);

        Network network = new Network(20000, 0.01, new int[]{2, 20, 1}); // Increased neurons and adjusted learning rate
        network.train(trainData, trainAnswers, valData, valAnswers);

        System.out.println(String.format("Prediction for (3, 3): %.10f", network.predict(3.0, 3.0)));
        System.out.println(String.format("Prediction for (4, 4): %.10f", network.predict(4.0, 4.0)));
    }
}

class Network {
    int epochs;
    double learnFactor;
    Layer[] layers;
    Random random = new Random(42);

    public Network(int epochs, double learnFactor, int[] layerSizes) {
        this.epochs = epochs;
        this.learnFactor = learnFactor;
        this.layers = new Layer[layerSizes.length - 1];
        for (int i = 0; i < layerSizes.length - 1; i++) {
            layers[i] = new Layer(layerSizes[i], layerSizes[i + 1], random);
        }
    }

    public double predict(double input1, double input2) {
        double[] inputs = {input1, input2};
        double[] outputs = forward(inputs);
        return outputs[0];
    }

    public void train(List<List<Double>> trainData, List<Double> trainAnswers, List<List<Double>> valData, List<Double> valAnswers) {
        double bestLoss = Double.MAX_VALUE;
        for (int epoch = 0; epoch < epochs; epoch++) {
            learnFactor *= 0.9999; // More aggressive learning rate decay

            for (int i = 0; i < trainData.size(); i++) {
                double[] inputs = trainData.get(i).stream().mapToDouble(d -> d).toArray();
                double[] targets = {trainAnswers.get(i)};
                backward(inputs, targets);
            }

            List<Double> predictions = new ArrayList<>();
            for (List<Double> data : trainData) {
                predictions.add(predict(data.get(0), data.get(1)));
            }
            double trainLoss = Util.meanSquareLoss(trainAnswers, predictions);

            if (epoch % 50 == 0) {
                List<Double> valPredictions = new ArrayList<>();
                for (List<Double> data : valData) {
                    valPredictions.add(predict(data.get(0), data.get(1)));
                }
                double valLoss = Util.meanSquareLoss(valAnswers, valPredictions);
                System.out.println(String.format("Epoch %d | Train Loss: %.10f | Val Loss: %.10f", epoch, trainLoss, valLoss));
            }

            if (trainLoss < bestLoss) {
                bestLoss = trainLoss;
                remember();
            }
        }
    }

    private double[] forward(double[] inputs) {
        double[] activations = inputs;
        for (Layer layer : layers) {
            activations = layer.forward(activations);
        }
        return activations;
    }

    private void backward(double[] inputs, double[] targets) {
        double[] outputs = forward(inputs);
        double[] errors = new double[outputs.length];
        for (int i = 0; i < outputs.length; i++) {
            errors[i] = targets[i] - outputs[i];
        }
        for (int i = layers.length - 1; i >= 0; i--) {
            errors = layers[i].backward(errors, learnFactor);
        }
    }

    private void remember() {
        for (Layer layer : layers) {
            layer.remember();
        }
    }
}

class Layer {
    int inputSize;
    int outputSize;
    double[][] weights;
    double[] biases;
    double[][] bestWeights;
    double[] bestBiases;
    double[][] lastInputs;
    double[][] lastOutputs;
    Random random;

    public Layer(int inputSize, int outputSize, Random random) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.random = random;
        this.weights = new double[outputSize][inputSize];
        this.biases = new double[outputSize];
        this.bestWeights = new double[outputSize][inputSize];
        this.bestBiases = new double[outputSize];
        initializeWeights();
    }

    private void initializeWeights() {
        for (int i = 0; i < outputSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                weights[i][j] = random.nextGaussian() * Math.sqrt(2.0 / inputSize); // Xavier initialization
            }
            biases[i] = 0.0;
        }
    }

    public double[] forward(double[] inputs) {
        lastInputs = new double[inputs.length][1];
        lastOutputs = new double[outputSize][1];
        double[] outputs = new double[outputSize];
        for (int i = 0; i < outputSize; i++) {
            double activation = biases[i];
            for (int j = 0; j < inputSize; j++) {
                activation += weights[i][j] * inputs[j];
                lastInputs[j][0] = inputs[j];
            }
            outputs[i] = Util.sigmoid(activation);
            lastOutputs[i][0] = outputs[i];
        }
        return outputs;
    }

    public double[] backward(double[] errors, double learnFactor) {
        double[] propagatedErrors = new double[inputSize];
        for (int i = 0; i < outputSize; i++) {
            double delta = errors[i] * Util.sigmoidDerivative(lastOutputs[i][0]);
            for (int j = 0; j < inputSize; j++) {
                propagatedErrors[j] += delta * weights[i][j];
                weights[i][j] += learnFactor * delta * lastInputs[j][0];
            }
            biases[i] += learnFactor * delta;
        }
        return propagatedErrors;
    }

    public void remember() {
        for (int i = 0; i < outputSize; i++) {
            System.arraycopy(weights[i], 0, bestWeights[i], 0, inputSize);
            bestBiases[i] = biases[i];
        }
    }
}

class Util {
    public static double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    public static double sigmoidDerivative(double x) {
        return x * (1 - x);
    }

    public static double meanSquareLoss(List<Double> correct, List<Double> predicted) {
        double sum = 0;
        for (int i = 0; i < correct.size(); i++) {
            double error = correct.get(i) - predicted.get(i);
            sum += error * error;
        }
        return sum / correct.size();
    }
}