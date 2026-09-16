package engine.lmsr;

// LMSR math calculations defined in Appendix A.
public class LMSRCalculator {

    // Calculates cost function C(q1, q2) = b * ln(e^(q1/b) + e^(q2/b)).
    public static double calculateCostFunction(int q1, int q2, int b) {
        if (b <= 0) {
            throw new IllegalArgumentException("Liquidity parameter b must be positive");
        }
        double exp1 = Math.exp((double) q1 / b);
        double exp2 = Math.exp((double) q2 / b);
        return b * Math.log(exp1 + exp2);
    }

    // Calculates purchase price: Cost = C(q_after) - C(q_before).
    public static double calculatePurchasePrice(int currentQ1, int currentQ2, int additionalShares, boolean isOption1, int b) {
        double costBefore = calculateCostFunction(currentQ1, currentQ2, b);
        double costAfter;
        if (isOption1) {
            costAfter = calculateCostFunction(currentQ1 + additionalShares, currentQ2, b);
        } else {
            costAfter = calculateCostFunction(currentQ1, currentQ2 + additionalShares, b);
        }
        return costAfter - costBefore;
    }

    // Calculates probability for Option 1: p1 = e^(q1/b) / (e^(q1/b) + e^(q2/b)).
    public static double calculateProbabilityOption1(int q1, int q2, int b) {
        if (b <= 0) {
            throw new IllegalArgumentException("Liquidity parameter b must be positive");
        }
        double exp1 = Math.exp((double) q1 / b);
        double exp2 = Math.exp((double) q2 / b);
        return exp1 / (exp1 + exp2);
    }

    // Calculates probability for Option 2: p2 = 1 - p1.
    public static double calculateProbabilityOption2(int q1, int q2, int b) {
        return 1.0 - calculateProbabilityOption1(q1, q2, b);
    }

    // Calculates initial subsidy C(0, 0) = b * ln(2).
    public static double calculateInitialSubsidy(int b) {
        return calculateCostFunction(0, 0, b);
    }
}
