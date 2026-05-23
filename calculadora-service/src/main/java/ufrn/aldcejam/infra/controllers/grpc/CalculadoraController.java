package ufrn.aldcejam.infra.controllers.grpc;

import io.grpc.stub.StreamObserver;
import ufrn.aldcejam.grpc.MatrixRequest;
import ufrn.aldcejam.grpc.MatrixResponse;
import ufrn.aldcejam.grpc.calculadora_serviceGrpc.calculadora_serviceImplBase;
import java.util.Random;

public class CalculadoraController extends calculadora_serviceImplBase {
    @Override
    public void squareMatrix(MatrixRequest request, StreamObserver<MatrixResponse> responseObserver) {
        int n = request.getRows();
        if (n != request.getCols() || n <= 0) {
            responseObserver.onError(
                    new IllegalArgumentException("A matriz deve ser quadrada (rows == cols) e tamanho > 0"));
            return;
        }

        long startTime = System.currentTimeMillis();

        // Inicializa matriz aleatoria
        int[][] matrix = new int[n][n];
        Random random = new Random();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = random.nextInt(10);
            }
        }

        // Multiplicação (O(N^3))
        int[][] result = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    result[i][j] += matrix[i][k] * matrix[k][j];
                }
            }
        }

        long endTime = System.currentTimeMillis();

        MatrixResponse response = MatrixResponse.newBuilder()
                .setRows(n)
                .setCols(n)
                .setTimeTakenMs(endTime - startTime)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
