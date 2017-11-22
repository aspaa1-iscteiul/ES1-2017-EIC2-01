package antiSpamFilter;

import java.util.ArrayList;
import java.util.List;

import org.uma.jmetal.problem.impl.AbstractDoubleProblem;
import org.uma.jmetal.solution.DoubleSolution;

import antiSpamFilter.frames.HomePage;
import antiSpamFilter.utils.Utils;

public class AntiSpamFilterProblem extends AbstractDoubleProblem {

	public AntiSpamFilterProblem() {
		/*
		 * Gera um vetor de pesos com tamanho igual ao n�mero de regras
		 * existentes
		 */
		this(Utils.rules(HomePage.config_files_path[0]).length);
	}

	public AntiSpamFilterProblem(Integer numberOfVariables) {
		setNumberOfVariables(numberOfVariables);
		setNumberOfObjectives(2);
		setName("AntiSpamFilterProblem");

		List<Double> lowerLimit = new ArrayList<>(getNumberOfVariables());
		List<Double> upperLimit = new ArrayList<>(getNumberOfVariables());

		for (int i = 0; i < getNumberOfVariables(); i++) {
			lowerLimit.add(-5.0);
			upperLimit.add(5.0);
		}

		setLowerLimit(lowerLimit);
		setUpperLimit(upperLimit);
	}

	public void evaluate(DoubleSolution solution) {
		double aux, xi, xj;
		double[] fx = new double[getNumberOfObjectives()];
		double[] x = new double[getNumberOfVariables()];
		for (int i = 0; i < solution.getNumberOfVariables(); i++) {
			x[i] = solution.getVariableValue(i);
		}

		fx[0] = 0.0;
		for (int var = 0; var < solution.getNumberOfVariables() - 1; var++) {
			fx[0] += Math.abs(x[0]); // Example for testing
		}

		fx[1] = 0.0;
		for (int var = 0; var < solution.getNumberOfVariables(); var++) {
			fx[1] += Math.abs(x[1]); // Example for testing
		}

		solution.setObjective(0, fx[0]);
		solution.setObjective(1, fx[1]);
	}
}
