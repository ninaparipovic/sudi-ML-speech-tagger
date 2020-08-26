package cs10;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Sudi {
	private String trainSentences = new String();
	private String trainTags = new String();
	private String start = "#";
	private int unseen = -100;
	private Map<String, Map<String, Double>> observations;
	private Map<String, Map<String, Double>> transitions;
	
	
	
	public Sudi(String trainSentences, String trainTags) {
		this.trainSentences = trainSentences;
		this.trainTags = trainTags;
	}
	
	public Sudi(String trainSentences, String trainTags, int unseen) {
		this.trainSentences = trainSentences;
		this.trainTags = trainTags;
		this.unseen = unseen;
	}
	
	/**
	 * 
	 * @return // maps tag to map of tags (transitions) whose value is count of that transition
	 * @throws IOException
	 */
	public Map<String, Map<String, Double>> transitionsData() throws IOException{
		Map<String, Map<String, Double>> transitions = new HashMap<String, Map<String, Double>>();
		// add start as the first tag
		transitions.put("#", new HashMap<String, Double>());
		
		BufferedReader t = new BufferedReader(new FileReader(trainTags));
		String sentence = t.readLine();
		// read each line of the file one at a time 
		while (sentence != null) {
			String[] sentPieces = sentence.split(" "); // split sentences into words based on whitespace 
			Map<String, Double> startMap = transitions.get("#");
			if (startMap.containsKey(sentPieces[0])) startMap.put(sentPieces[0], startMap.get(sentPieces[0]) +1);
			else startMap.put(sentPieces[0], 1.0);
			
			// keep track of the previous tag
			int from = 0;
			// for each of the tags in the sentence
			for (int to = 1; to < sentPieces.length; to++) {
				// if it already has seen the tag
				if(transitions.containsKey(sentPieces[from])) {
					Map<String, Double> tMap = transitions.get(sentPieces[from]); // get the inner map
					// if it already contains the transition
					if (tMap.containsKey(sentPieces[to])) tMap.put(sentPieces[to], tMap.get(sentPieces[to]) +1);
					else tMap.put(sentPieces[to], 1.0); // add the transition to the inner map
				}
				else {
					// else add the unseen tag to the transitions map
					Map<String, Double> tMap = new HashMap<String, Double>();
					tMap.put(sentPieces[to], 1.0);
					transitions.put(sentPieces[from], tMap);
				}
				from ++;
			}
			String lastItem = sentPieces[sentPieces.length-1];
			if (transitions.containsKey(lastItem)) {
				if (transitions.get(lastItem).containsKey("#")) {
					transitions.get(lastItem).put("#", transitions.get(lastItem).get("#") +1.0);
				}
				else {
					transitions.get(lastItem).put("#", 1.0);
				}
			}
			else {
				Map<String, Double> lastItemMap = new TreeMap<String,Double>();
				lastItemMap.put("#", 1.0);
				transitions.put(lastItem, lastItemMap);
			}
		sentence = t.readLine();
		}
		t.close();
		return transitions;
	}
	
	
	/**
	 * @return maps tag to map of observation with count as value of inner map 
	 * @throws IOException
	 */
	public Map<String, Map<String, Double>> observationsData() throws IOException{
		Map<String, Map<String, Double>> observations = new HashMap<String, Map<String, Double>>();
		BufferedReader observation = new BufferedReader(new FileReader(trainSentences));
		BufferedReader tags = new BufferedReader(new FileReader(trainTags));
		
		String obString = observation.readLine();
		String tagString = tags.readLine();
		
		// while there is still a line to read
		while(obString != null || tagString != null) {  	
			// split each line into words/tags
			String[] obPieces = obString.split(" "); // observation pieces
			String[] tagPieces = tagString.split(" "); // tag pieces 

			for (int i=0; i < obPieces.length; i++) {
				// if the map already contains the tag
				if(observations.containsKey(tagPieces[i])) {
					Map<String, Double> tagMap = observations.get(tagPieces[i]);
					// check if the tag already has the observation
					if (tagMap.containsKey(obPieces[i].toLowerCase()))  tagMap.put(obPieces[i].toLowerCase(), tagMap.get(obPieces[i].toLowerCase()) +1); // increase the count of the word
					// else, put the observation in as a new value in the inner map
					else tagMap.put(obPieces[i].toLowerCase(), 1.0);
				}
				else {
					Map<String, Double> tagMap = new HashMap<String, Double>();
					tagMap.put(obPieces[i].toLowerCase(), 1.0);
					observations.put(tagPieces[i], tagMap);
				}
			}
			obString = observation.readLine();
			tagString = tags.readLine();
		}
		observation.close();
		tags.close();
		return observations;
	}
	

	/**
	 * 
	 * @param transitions map
	 * @return transitions map with counts converted to probabilities 
	 * @throws IOException
	 */
	public Map<String, Map<String, Double>> transitionProbabilityMap(Map<String, Map<String, Double>> transitions) throws IOException{
		for (String s: transitions.keySet()) {
			Map<String, Double> innerMap = transitions.get(s);
			double count = 0.0;
			// get the total number of transitions
			for (String t: innerMap.keySet()) {
				count += innerMap.get(t);
			}
			// calculate probability by divide each of the counts by the total 
			for (String t: innerMap.keySet()) {
				double probability = Math.log(innerMap.get(t)/count);
				innerMap.put(t, probability);
			}
		}
		return transitions;
	}
	/**
	 * 
	 * @param observations
	 * @return observation map with observations count converted into probabilities 
	 * @throws IOException
	 */
	public Map<String, Map<String, Double>> observationProbabilityMap(Map<String, Map<String, Double>> observations) throws IOException{
		for (String s: observations.keySet()) {
			Map<String, Double> innerMap = observations.get(s);
			double count = 0.0;
			// get the total number of transitions
			for (String t: innerMap.keySet()) {
				count += innerMap.get(t);
			}
			// calculate probability by divide each of the counts by the total 
			for (String t: innerMap.keySet()) {
				double probability = Math.log(innerMap.get(t)/count);
				innerMap.put(t, probability);
			}
		}
		return observations;
	
	}
	/**
	 * algorithm which creates a list of tags that most likely correspond to the observations (the given str) from the trained model
	 * @param str
	 * @param transitions map
	 * @param observations map
	 * @return array list of tags approximated by the model 
	 */
	public ArrayList<String> viterbi(String str, Map<String, Map<String, Double>> transitions, Map<String, Map<String, Double>> observations) {
		Set<String> currStates = new TreeSet<String>();
		currStates.add(start);
		Map<String, Double> currScores = new TreeMap<String, Double>();
		currScores.put(start, 0.0);
		Set<String> nextStates;
		Map<String, Double> nextScores;
		Map<Integer,Map<String, String>> tracker = new TreeMap<Integer,Map<String, String>>();
		double observationScore;
		String[] stringSet = str.split(" ");
		for (int i=0; i<stringSet.length; i++) {
			nextStates = new HashSet<String>();
			nextScores = new HashMap<String, Double>();
			for (String current: currStates) { // for each current state in currStates
				Map<String, Double> tran = transitions.get(current); // get all the tags that the tag s transitions to 
				for (String next: tran.keySet()) {  // for each transition of currState -> nextState
					nextStates.add(next); // add the transition state to next states
					if (!observations.containsKey(next) || !observations.get(next).containsKey(stringSet[i])) { // do i need first part of the or statement 
						observationScore = unseen;
					}
					else {
						observationScore = observations.get(next).get(stringSet[i]); 
					}
					double nextScore = currScores.get(current) + tran.get(next) + observationScore; 
					
					if (!nextScores.containsKey(next) || nextScore > nextScores.get(next)) {
						nextScores.put(next, nextScore);
						if (!tracker.containsKey(i)) tracker.put(i, new HashMap<String, String>()); // index as key, map as value - key: next state, value: current state
						tracker.get(i).put(next, current); // index as key, map as value - key: next state, value: current state
					}
				}
			}
			currStates = nextStates;
			currScores = nextScores;
		}
		// calculate the highest score within the final states 
		double d = Math.log(0);
		String highestScore = null;
		for (String s: currScores.keySet()) {
			if (currScores.get(s) > d) {
				d = currScores.get(s); 
				highestScore = s;
			}
		}
		// backtrack to find the most likely path 
		String currTag = highestScore;
		ArrayList<String> path = new ArrayList<String>();
		path.add(highestScore);
		for (int g = stringSet.length-1; g>=0; g--) {
			path.add(0, tracker.get(g).get(currTag));
			currTag = tracker.get(g).get(currTag);
		}
		return path;
		
	}
	/** 
	 * given train sentence and tag files, this method trains a model 
	 * @throws IOException
	 */
	public void trainModel() throws IOException {
		Map<String, Map<String, Double>> transitionsCount = transitionsData();
		Map<String, Map<String, Double>> observationsCount = observationsData();
		this.observations = observationProbabilityMap(observationsCount);
		this.transitions = transitionProbabilityMap(transitionsCount);
		}
	/**
	 * prints to the console an evaluation of how accurate the model is 
	 * @param testSentences
	 * @param testTags
	 * @throws IOException
	 */
	public void accuracy(String testSentences, String testTags) throws IOException {
		BufferedReader observation = new BufferedReader(new FileReader(testSentences));
		BufferedReader tags = new BufferedReader(new FileReader(testTags));
		
		String obString = observation.readLine();
		String tagString = tags.readLine();
		
		int tagCount = 0;
		int incorrectTags = 0;
		while(obString != null || tagString != null) {  
			ArrayList<String> modelTags = viterbi(obString, this.transitions, this.observations);
			String[] givenTags = tagString.split(" ");
			tagCount += (givenTags.length-1);
			for (int i=0; i < givenTags.length; i++) {
				if (modelTags.get(i+1) != givenTags[i]) {
					incorrectTags += 1;
				}
			}
			obString = observation.readLine();
			tagString = tags.readLine();
		}
		double accuracy = ((double) tagCount/(double) incorrectTags)*100;
		System.out.println("Number of tags wrong: "+ (incorrectTags - tagCount));
		String a = "Accuracy: " + accuracy+"%";
		System.out.println(a);
		observation.close();
		tags.close();
	}

	public static void main(String[] args) throws IOException {
		Sudi s = new Sudi("PS5/simple-train-sentences.txt", "PS5/simple-train-tags.txt");
		s.trainModel();
		s.accuracy("PS5/simple-test-sentences.txt", "PS5/simple-test-tags.txt");
		System.out.println(s.viterbi("the dog saw trains in the night .", s.transitions, s.observations));
		
		Sudi test = new Sudi("PS5/brown-train-sentences.txt", "PS5/brown-train-tags.txt");
		test.trainModel();
		test.accuracy("PS5/brown-test-sentences.txt", "PS5/brown-test-tags.txt");
	}
	
	
}
