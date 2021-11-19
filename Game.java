import java.util.Scanner;

class Game {
    // Matrix of Chip-States
    static int x;
    static boolean[][] matrix;

    // Two sticky chips
    static int[] sticky = {-1, -1};

    static int[] score = new int[2];            // Current scores
    static boolean player = true;               // Current player (A)

    static boolean localPlayer;

    // Messages
    static String prompt = "Col-Row to flip";
    static String exInvalid = "Invalid Chip ID";
    static String exStuck = "Chip is Stuck";
    static String exDisconnected = "Disconnected, Game Over";

    // Matrix graphics
    static String matrixDivider;
    static String matrixHead, matrixFoot;

    static String matrixSeparator   = " │ ";

    // ANSI escape sequences for formatting
    static String fmtClear          = "\033[0m";
    static String fmtChipState      = "\033[1m";
    static String fmtStateX         = "\033[34m";
    static String fmtStateO         = "\033[31m";
    static String fmtOrderChange    = "\033[92;1m";
    static String fmtChipSticky     = "\033[47;1m";
    static String fmtInvalid        = "\033[41m";

    static String resetCursor;
    static String upCursor          = "\033[1F\033[J";
    static String promptCursor      = "\033[2F\033[J";

    public static void main(int p_x, boolean p_localPlayer) {
        x = p_x;
        localPlayer = p_localPlayer;

        // Initialize
        System.out.println(Main.versionString);
        if (!init()) {
            System.out.println("Incorrectly compiled, exiting");
            return;
        }

        // Primary Game Loop
        do {
            printMatrix();
            printOrder(true);
            printOrder(false);
            score();
            System.out.println("Score: " + score[0] + "," + score[1]);
            System.out.println();

            // The player must make a valid move
            do {
                try {
                    turn();
                } catch (Exception e) {
                    // Print any error message in red
                    System.out.println(promptCursor + fmtInvalid + e.getMessage() + fmtClear);
                    continue;
                }
                break;
            } while (true);

            try {
                // Output the chip flipped
                if (player == localPlayer) System.out.print(upCursor);
                System.out.print(resetCursor);

                output(encodeChipID(sticky[0]));
            } catch (Exception e) {
                System.out.println(fmtInvalid + e.getMessage() + fmtClear);
                break;
            }

            // Next player
            player = !player;

        } while (gameExists());

        // End of Game
        printMatrix();
        printOrder(true);
        printOrder(false);
        score();

        boolean winner;

        if (score[0] == score[1]) winner = player;
        else winner = score[0] > score[1];

        System.out.println(
            "Player " + 
            (winner ? "A" : "B") + 
            " Wins " + 
            score[0] + "," + score[1]
        );
    }

    // Initialize matrix
    static boolean init() {
        matrix = new boolean[x][x];                         // Initialize matrix

        for (int row = 0; row < x; row++) {
            for (int col = 0; col < x; col++) {
                matrix[row][col] =                          // For any matrix, do the centre
                    row == x / 2 && col == x / 2;

                if (x % 2 == 0) matrix[row][col] =          // For even matrices, do the whole centre
                    row <= x / 2 && row >= x / 2 - 1 &&
                    col <= x / 2 && col >= x / 2 - 1;
            }
        }

        // Generate dependencies
        sticky[0] = x % 2 == 0 ? -1 : (x * x / 2);          // Initial chip to play
        resetCursor = "\033[" + (7 + 2 * x) + "F\033[J";    // Cursor reset for re-drawing
        matrixDivider = " ├" + "───┼".repeat(x) + "───┤";   // Matrix division line

        matrixHead = matrixDivider.replace('├', '┌').replace('┼', '┬').replace('┤', '┐');
        matrixFoot = matrixDivider.replace('├', '└').replace('┼', '┴').replace('┤', '┘');

        return x <= 9;
    }

    // Get Input
    static String input() throws Exception {
        try {
            if (player == localPlayer)
                return Main.localInput.nextLine();
            else
                return Main.remoteInput.nextLine();
        }
        catch (Exception e) {
            throw new Exception(exDisconnected);
        }
    }

    // Output
    static void output(String data) throws Exception {
        try {
            if (player == localPlayer)
                Main.localOutput.println(data);

            Main.remoteOutput.println(data);
        }
        catch (Exception e) {
            throw new Exception(exDisconnected);
        }
    }

    // Run one turn
    static void turn() throws Exception {
        // Prompt current player
        System.out.print(
            prompt + 
            " (" + 
            (player ? "A" : "B") + 
            ") "
        );

        int chip = decodeChipID(input());       // Get chip
        flip(chip);                             // Flip chip
    }

    // Flip a Particular Chip
    static void flip(int chip) throws Exception {
        // Invalidate if this is one of the sticky chips
        if (chip == sticky[0] || chip == sticky[1]) throw new Exception(exStuck);

        sticky[1] = sticky[0];                  // Shift sticky chips over and stick this one
        sticky[0] = chip;

        matrix [chip / x] [chip % x] = 
        ! matrix [chip / x] [chip % x];         // Flip the boolean on this row/col
    }

    // Analyse Scores
    static void score() {
        boolean p = matrix[0][0];               // Don't count the first chip as a change

        score[0] = 0;                           // Reset scores
        score[1] = 0;

        for (boolean i : order(true)) {         // Iterate through chip order
            if (i != p) score[0]++;
            p = i;
        }

        p = matrix[x - 1][0];

        for (boolean i : order(false)) {
            if (i != p) score[1]++;
            p = i;
        }
    }

    // Get player's chip order
    static boolean[] order(boolean player) {
        boolean order[] = new boolean[x * x];       // Array of ordered chip-states

        // Loop through all chips
        for (int i = 0; i < x * x; i++) {
            order[i] = player 
                ? matrix [i / x] [i % x]            // Player A goes by row
                : matrix [x - 1 - i % x] [i / x]    // Player B goes by col (and row is inverted)
            ;
        }

        return order;
    } 

    // Check if the game is over
    static boolean gameExists() {
        boolean diagonalBroken = false;
        
        // Check diagonal A
        for (int i = 0; i < x; i++) {
            if (matrix[i][i] != matrix[0][0]) diagonalBroken = true;
        }

        if (!diagonalBroken) return false;

        // Check diagonal B
        for (int i = 0; i < x; i++) {
            if (matrix[i][x - 1 - i] != matrix[x - 1][0]) return true;
        }

        return false;
    }

    // Convert a Chip ID to an integer
    static int decodeChipID(String chip) throws Exception {
        // Input is invalid
        if (chip.length() != 2) throw new Exception(exInvalid);

        // Interpret row/col
        int row = chip.charAt(1) - '1';
        int col = chip.charAt(0) - 'A';

        if (
            row >= x || row < 0 ||                  // Row is not valid
            col >= x || col < 0                     // Col is not valid
        ) throw new Exception(exInvalid);

        return row * x + col;
    }

    // Convert an Integer to a Chip ID
    static String encodeChipID(int index) {
        String chip = "";
        index %= x * x;                             // Make sure this is in-bounds

        chip += (char) ((index % x) + 'A');         // Add Row
        chip += index / x + 1;                      // Add Col

        return chip;
    }

    // Print the Entire Matrix
    static void printMatrix() {
        System.out.println(matrixHead);                                         // Print the top of the matrix

        // Print the heading
        System.out.print(matrixSeparator + " ");
        for (char i = 0; i < x; i++) {
            System.out.print(
                matrixSeparator + 
                (char) (i + 'A')
            );
        }

        System.out.println(matrixSeparator);

        // Print every row
        for (int row = 0; row < x; row++) {
            System.out.println(matrixDivider);                                  // Print a divider between rows
            System.out.print(matrixSeparator + (row + 1));                      // Row number
            
            // Print every chip
            for (int col = 0; col < x; col++) {
                System.out.print(
                    matrixSeparator + 
                    (x * row + col == sticky[0] || x * row + col == sticky[1]   // If the chip is sticky, highlight it, otherwise, just bold
                    ? fmtChipSticky : fmtChipState) +
                    (matrix[row][col] ? fmtStateX + "X" : fmtStateO + "O") +    // Print the appropriate symbol for the chip state
                    fmtClear
                );
            }

            System.out.println(matrixSeparator);
        }

        System.out.println(matrixFoot);                                         // Print the bottom of the matrix
    }

    // Print a player's chip order
    static void printOrder(boolean player) {
        boolean p = order(player)[0];
        System.out.print((player ? "A" : "B") + ": ");

        for (boolean i : order(player)) {
            if (i != p) System.out.print(fmtOrderChange);
            System.out.print((i ? "X" : "O") + fmtClear + " ");
            p = i;
        }

        System.out.println();
    }        
}
