package vtp5.logic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

/*VTP5 Copyright (C) 2015  Abdel-Rahim Abdalla, Minghua Yin, Yousuf Mohamed-Ahmed and Nikunj Paliwal

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public class CSVTF implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// TODO CHANGE serialVersionUID ONCE CLASS IS FINISHED!!!

	//TODO Settings variables
	public int timeAllowed = 0;
	
	// ArrayList storing all the original "cards" for the test
	private final ArrayList<Card> origCards = new ArrayList<>();
	// ArrayList of "cards" for a particular test
	private ArrayList<Card> cards = new ArrayList<>();
	// Stores the cards the user got wrong the first time
	private ArrayList<Card> incorrectCards = new ArrayList<>();
	// The user's score
	private int score = 0;

	// Boolean storing whether the prompt is langFrom or langTo - default
	// (false) means prompt is langFrom
	private boolean isLanguageSwitched = false;

	// More logical instance variables
	private int totalNumberOfCards;
	private int numberOfIncorrectCards;
	private int totalTimesGuessed;
	private double successRate;

	private transient BufferedReader br = null;

	// "Enum"-like constants
	public static final int INCORRECT = 0;
	public static final int PARTIALLY_CORRECT = 1;
	public static final int COMPLETELY_CORRECT = 2;
	public static final int PROMPT_USER = 3;

	private File importedFile;

	@SuppressWarnings("unchecked")
	public CSVTF(File[] files) throws IOException {
		if (files.length == 1) {
			setImportedFile(files[0]);
		}

		for (File f : files) {
			getVocabFromFile(f);
		}

		for (Card c : origCards) {
			cards.add(new Card(c.getLangFromPrompt(), c.getLangToPrompt(),
					(ArrayList<String>) c.getLangFrom().clone(),
					(ArrayList<String>) c.getLangTo().clone()));
		}

		totalNumberOfCards = cards.size();
	}

	public void getVocabFromFile(File file) throws IOException,
			NullPointerException {
		System.out.println(file + " is being read.");

		// try {
		// Reading file.
		String langFromLine;
		String langToLine;
		br = new BufferedReader(new FileReader(file));
		int loop = 0;
		while ((langFromLine = br.readLine()) != null) {
			// Create new card containing relevant data and add it to the
			// ArrayList
			if (loop == 0) {
				
				String[] settings = langFromLine.split(",");

				timeAllowed = Integer.parseInt(settings[0]);
				
				loop = 1;
			} else {
				
				String[] question = langFromLine.split(",||,");
				
				ArrayList<String> langFrom = new ArrayList<>(
						Arrays.asList(question[0].split(",|,")));

				langToLine = br.readLine();
				ArrayList<String> langTo = new ArrayList<>(
						Arrays.asList(question[1].split(",|,")));

				Card card = new Card(langFromLine, langToLine, langFrom, langTo);
				origCards.add(card);
			}
		}

		if (br != null) {
			br.close();
		}

		// Printlns for debugging/helpful console messages
		for (Card c : cards) {
			System.out.println("--langFrom:--");
			for (String s1 : c.getLangFrom()) {
				System.out.println(s1);
			}

			System.out.println("--langTo:--");
			for (String s2 : c.getLangTo()) {
				System.out.println(s2);
			}

			System.out.println();
		}
	}

	@SuppressWarnings("unchecked")
	public void resetTest() {
		cards.clear();
		for (Card c : origCards) {
			cards.add(new Card(c.getLangFromPrompt(), c.getLangToPrompt(),
					(ArrayList<String>) c.getLangFrom().clone(),
					(ArrayList<String>) c.getLangTo().clone()));
		}

		incorrectCards.clear();
		score = 0;
		isLanguageSwitched = false;
		numberOfIncorrectCards = 0;
		totalTimesGuessed = 0;
		successRate = 0.0;
	}

	public ArrayList<Card> getOrigCards() {
		return origCards;
	}

	public ArrayList<Card> getCards() {
		return cards;
	}

	public String getPrompt(int index) {
		if (isLanguageSwitched) {
			// Return langTo prompt
			return cards.get(index).getLangToPrompt();
		} else {
			return cards.get(index).getLangFromPrompt();
		}
	}

	public int isCorrect(String answer, int index, boolean isExperimental) {
		// Original answer saved for spell-checker
		String origAnswer = answer;

		answer = answer.replaceAll("[^a-zA-Z0-9������������������������]",
				"");
		System.out.println(answer);

		// Find out if the user is correct, and if so, remove the correctly
		// guessed answer from the ArrayList
		boolean userIsCorrect = false;
		Card card = cards.get(index);

		// Find out whether to use langFrom or langTo
		ArrayList<String> possibleAnswers = isLanguageSwitched ? card
				.getLangFrom() : card.getLangTo();
		ArrayList<String> correctAnswers = isLanguageSwitched ? card
				.getCorrectLangFrom() : card.getCorrectLangTo();

		for (String s : possibleAnswers) {
			System.out.println("Original correct answer: " + s);
			System.out.println("Correct answer: " + s);

			if (answer.equalsIgnoreCase(s.replaceAll("[^a-zA-Z0-9]", ""))) {
				System.out.println("User is correct");
				userIsCorrect = true;
				// Remove this answer from the ArayList
				possibleAnswers.remove(s);
				// Add answer to ArrayList containing correctly guessed answers
				correctAnswers.add(s);
				break;
			}
		}

		if (userIsCorrect) {
			System.out.println("User is correct");
			// See if user has guessed all of the possible answers
			if (possibleAnswers.isEmpty()) {
				System.out.println("Completely correct");
				score++;

				// If card was previously incorrect, decrement
				// numberOfIncorrectCards
				if (incorrectCards.contains(cards.get(index))) {
					numberOfIncorrectCards--;
				}

				totalTimesGuessed++;

				// removes card once completed
				cards.remove(index);

				// Calculate success rate
				System.out.println("Calculating success rate...");
				System.out.println("totalTimesGuessed == " + totalTimesGuessed);
				System.out.print("(" + totalNumberOfCards + " - "
						+ cards.size() + ") / " + totalTimesGuessed
						+ " * 100.0");
				successRate = (totalTimesGuessed == 0) ? 0.0
						: ((double) (totalNumberOfCards - cards.size()))
								/ (double) totalTimesGuessed * 100.0;
				System.out.println(" == " + successRate);
				return COMPLETELY_CORRECT;
			} else {
				System.out.println("Partially correct");
				return PARTIALLY_CORRECT;
			}
		} else {
			System.out.println("Incorrect");
			// Check if user has already entered the answer (if it's correct)
			for (String s : correctAnswers) {

				if (answer.equalsIgnoreCase(s.replaceAll("[^a-zA-Z0-9]", ""))) {
					// If user has already guessed the answer and it's correct,
					// just return PARTIALLY_CORRECT
					return PARTIALLY_CORRECT;
				}
			}

			// If "experimental features" is enabled, work out if the program
			// should prompt the user
			if (isExperimental) {
				// Work out if user has only typed part of the answer, or if the
				// answer is part of the user's input
				for (String s : correctAnswers) {
					s = s.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
					answer = answer.toLowerCase();

					if (answer.contains(s) || s.contains(answer)) {
						// Tell the program to prompt the user
						return PROMPT_USER;
					}
				}

				for (String s : possibleAnswers) {
					s = s.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
					answer = answer.toLowerCase();

					if (answer.contains(s) || s.contains(answer)) {
						// Tell the program to prompt the user
						return PROMPT_USER;
					}
				}

				if (!isLanguageSwitched) {
					// Use the spell-checker to see if the user has made any
					// potential typos
					if (SpellCheck.containsSpellingErrors(origAnswer)) {
						return PROMPT_USER;
					}
				}
			}

			// Add card to ArrayList of "incorrect" cards and update
			// numberOfIncorrectCards
			if (!incorrectCards.contains(cards.get(index))) {
				incorrectCards.add(cards.get(index));
				numberOfIncorrectCards++;
			}

			totalTimesGuessed++;

			// Calculate success rate
			successRate = (totalTimesGuessed == 0) ? 0.0
					: ((double) (totalNumberOfCards - cards.size()))
							/ (double) totalTimesGuessed * 100.0;
			return INCORRECT;
		}
	}

	public void setCards(ArrayList<Card> cards) {
		this.cards = cards;
	}

	public ArrayList<Card> getIncorrectCards() {
		return incorrectCards;
	}

	public int getScore() {
		return score;
	}

	public boolean isLanguageSwitched() {
		return isLanguageSwitched;
	}

	public void setLanguageSwitched(boolean isLanguageSwitched) {
		this.isLanguageSwitched = isLanguageSwitched;
	}

	public Object[] getStats() {
		return new Object[] { totalNumberOfCards, numberOfIncorrectCards,
				totalTimesGuessed, successRate };
	}

	public int getTotalNumberOfCards() {
		return this.totalNumberOfCards;
	}

	public int getTotalTimesGuessed() {
		return this.totalTimesGuessed;
	}

	public double getSuccessRate() {
		return this.successRate;
	}

	public File getImportedFile() {
		return importedFile;
	}

	public void setImportedFile(File importedFile) {
		this.importedFile = importedFile;
	}
}