package antiSpamFilter.frames;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Executors;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import antiSpamFilter.AntiSpamFilterAutomaticConfiguration;
import antiSpamFilter.utils.GuiUtils;
import antiSpamFilter.utils.Utils;

/**
 * Classe respons�vel por determinar o comportamento da aplica��o no que
 * respeita � funcionalidade de otimiza��o do filtro anti-spam, incluindo
 * suporte � contagem dos falsos positivos e falsos negativos e ao display da
 * configura��o �tima do vetor de pesos
 * 
 * @author Ana Pestana, Guilherme Azevedo
 */

public class Otimizacao {

	private static JFrame frame, progressFrame;
	private static final String algorithmOutputFilesPath = "./experimentBaseDirectory/referenceFronts/AntiSpamFilterProblem.r";

	/**
	 * Construtor da classe Otimiza��o
	 */
	public Otimizacao() {
		optimize();
	}

	/**
	 * Construtor da GUI da p�gina de Afina��o
	 */
	private void constructFrame() {
		frame = new JFrame();
		frame.setTitle("Otimiza��o do filtro anti-spam");

		addContents();

		frame.setSize(750, 600);
		frame.setResizable(false);
		frame.addWindowListener(new GuiUtils.OtimizacaoClose());
		GuiUtils.frameAtCenter(frame);
		visible(true);
	}

	/**
	 * Display de uma progressBar enquanto aguarda pelo output do algoritmo
	 * NSGAII
	 */
	private void optimize() {
		JProgressBar progressBar = new JProgressBar();
		progressBar.setString("A calcular...");
		progressBar.setStringPainted(true);
		progressBar.setIndeterminate(true);
		progressFrame = new JFrame();
		progressFrame.add(progressBar);
		GuiUtils.frameAtCenter(progressFrame);
		progressFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		progressFrame.pack();
		progressFrame.setResizable(false);
		progressFrame.setAlwaysOnTop(true);
		progressFrame.setVisible(true);

		Executors.newSingleThreadExecutor().execute(executeAlgorithm());
	}

	/**
	 * Invoca o procedimento runAlgoritm() da classe
	 * AntiSpamFilterAutomaticConfiguration e compila e abre os ficheiros
	 * HV.Boxplot e AntiSpamStudy
	 * 
	 * @return do runnable que invoca o procedimento runAlgoritm() da classe
	 *         AntiSpamFilterAutomaticConfiguration e compila e abre os
	 *         ficheiros HV.Boxplot e AntiSpamStudy
	 */
	private Runnable executeAlgorithm() {
		return new Runnable() {
			@Override
			public void run() {
				try {
					AntiSpamFilterAutomaticConfiguration.runAlgorithm();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(progressFrame,
							"Ocorreu um problema durante a execu��o da framework JMetal", "Erro",
							JOptionPane.ERROR_MESSAGE);
					System.exit(1);
				}
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						try {
							ProcessBuilder builder = new ProcessBuilder(new String[] { "Rscript", "HV.Boxplot.R" });
							builder.directory(new File("./experimentBaseDirectory/AntiSpamStudy/R/").getAbsoluteFile());
							builder.start().waitFor();

							Desktop.getDesktop()
									.open(new File("./experimentBaseDirectory/AntiSpamStudy/R/HV.Boxplot.eps"));
						} catch (IOException | InterruptedException e) {
							new GuiUtils.RException(progressFrame);
						}
						try {
							ProcessBuilder builder = new ProcessBuilder(
									new String[] { "pdflatex", "AntiSpamStudy.tex" });
							builder.directory(
									new File("./experimentBaseDirectory/AntiSpamStudy/latex/").getAbsoluteFile());
							builder.start().waitFor();

							Desktop.getDesktop()
									.open(new File("./experimentBaseDirectory/AntiSpamStudy/latex/AntiSpamStudy.pdf"));
						} catch (InterruptedException | IOException e) {
							new GuiUtils.LatexException(progressFrame);
						}
						constructFrame();
						readAlgorithmOutputs();
						progressFrame.dispose();
					}
				});
			}
		};
	}

	/**
	 * Recupera a melhor solu��o produzida pelo algoritmo gen�tico NSGAII
	 */
	private void readAlgorithmOutputs() {
		ArrayList<String> lines = Utils.lines(algorithmOutputFilesPath + "f");
		int fp = Integer.MAX_VALUE, fn = Integer.MAX_VALUE;
		int index = -1;
		for (int i = 0; i < lines.size(); i++) {
			String[] values = lines.get(i).split(" ");
			double fpAux = Double.valueOf(values[0]), fnAux = Double.valueOf(values[1]);
			if (fpAux < fp || (fpAux == fp && fnAux < fn)) {
				fp = (int) fpAux;
				fn = (int) fnAux;
				index = i;
			}
		}
		lines = Utils.lines(algorithmOutputFilesPath + "s");
		String[] values = lines.get(index).split(" ");
		ArrayList<String> rulesList = new ArrayList<>(Utils.rules_weights.keySet());
		Collections.sort(rulesList);
		for (int i = 0; i < values.length; i++)
			Utils.rules_weights.put(rulesList.get(i), Double.valueOf(values[i]));

		int decimal_places = String.valueOf(Utils.hamLogRules.size()).length();
		GuiUtils.help_label_fp.setText("  Falsos Positivos (FP):  "
				+ String.format("%0" + decimal_places + "d", (int) fp) + " / " + Utils.hamLogRules.size());
		decimal_places = String.valueOf(Utils.spamLogRules.size()).length();
		GuiUtils.help_label_fn.setText("  Falsos Negativos (FN):  "
				+ String.format("%0" + decimal_places + "d", (int) fn) + " / " + Utils.spamLogRules.size());
	}

	/**
	 * Adiciona os conte�dos � janela de Otimiza��o
	 */
	private void addContents() {
		JPanel panel = new JPanel();
		GuiUtils.constructGUI(panel, true);

		JPanel buttons_panel = new JPanel();
		buttons_panel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		JButton save = new JButton("Guardar");
		save.addActionListener(new ActionListener() {
			/*
			 * Sentinela no but�o 'Guardar' respons�vel por guardar a
			 * configura��o e retornar � Home Page
			 */
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					FileWriter w = new FileWriter(Utils.config_files_path[0], false);
					for (HashMap.Entry<String, Double> entry : Utils.rules_weights.entrySet())
						w.write(entry.getKey() + " " + entry.getValue().toString() + GuiUtils.newLine);
					w.close();
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(frame,
							"N�o foi poss�vel prosseguir! O ficheiro rules.cf est� a ser editado.",
							"Configura��o dos ficheiros", JOptionPane.WARNING_MESSAGE);
					System.exit(1);
				}
				backHome();
			}
		});

		JButton cancel = new JButton("Cancelar");
		cancel.addActionListener(new ActionListener() {
			/*
			 * Sentinela no but�o 'Cancelar' respons�vel por retornar � Home
			 * Page quando o bot�o � pressionado
			 */
			@Override
			public void actionPerformed(ActionEvent e) {
				backHome();
			}
		});

		buttons_panel.add(save);
		buttons_panel.add(cancel);
		panel.add(buttons_panel, BorderLayout.SOUTH);

		frame.add(panel);
	}

	/**
	 * Descarta a GUI atual e retorna � Home Page
	 */
	public static void backHome() {
		frame.dispose();
		HomePage.visible(true);
	}

	/**
	 * Define a visibilidade da frame de Otimiza��o
	 * 
	 * @param setVisible
	 * 
	 * @see JFrame#setVisible(boolean)
	 */
	private void visible(boolean visible) {
		frame.setVisible(visible);
	}

	/**
	 * Lan�a uma nova janela de Otimiza��o
	 */
	public static void launch() {
		new Otimizacao();
	}

}
