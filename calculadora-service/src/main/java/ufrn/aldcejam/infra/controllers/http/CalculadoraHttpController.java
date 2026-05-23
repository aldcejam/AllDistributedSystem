package ufrn.aldcejam.infra.controllers.http;

import br.ufrn.middleware.annotations.Body;
import br.ufrn.middleware.annotations.Controller;
import br.ufrn.middleware.annotations.Post;
import br.ufrn.middleware.lifecycle.Lifecycle;

import java.util.Random;

@Controller(path = "", lifecycle = Lifecycle.STATIC)
public class CalculadoraHttpController {

    public record MatrixRequest(int rows, int cols) {
    }

    public record MatrixResponse(int rows, int cols, long time_taken_ms) {
    }

    @Post(path = "/squareMatrix")
    public MatrixResponse squareMatrix(@Body MatrixRequest matrixRequest) {
        int n = matrixRequest.rows();

        if (n != matrixRequest.cols() || n <= 0) {
            throw new RuntimeException("A matriz deve ser quadrada (rows == cols) e tamanho > 0");
        }

        long startTime = System.currentTimeMillis();

        int[][] matrix = new int[n][n];
        Random random = new Random();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = random.nextInt(10);
            }
        }

        int[][] result = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    result[i][j] += matrix[i][k] * matrix[k][j];
                }
            }
        }

        long endTime = System.currentTimeMillis();

        return new MatrixResponse(n, n, endTime - startTime);
    }
}
