package cimpress;

import cimpress.json.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.imageio.ImageIO;

public class Solver {
	private static final String API_KEY = "1db2e94d57694e028a0eece671e78996";
	private static final String BASE_URL = "http://techchallenge.cimpress.com";
	private static final String ENV = "trial";
	
	private static final double PHI = (1 + Math.sqrt(5)) / 2 - 1;
	private static final int SCALE = 32;
	
	private String makeRequest(URL url, String data) throws IOException {
		boolean isPost = (data != null);
		HttpURLConnection c = (HttpURLConnection) url.openConnection();
		c.setRequestMethod(!isPost ? "GET" : "POST");
		c.setUseCaches(false);
		c.setDoOutput(isPost);
		c.setDoInput(true);
		c.connect();
		
		if (isPost) {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(c.getOutputStream()));
			out.write(data);
			out.flush();
			out.close();
		}
		
		StringBuilder sb = new StringBuilder();
		BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
		String line;
		while ((line = in.readLine()) != null) {
			sb.append(line + "\n");
		}
		in.close();
		return sb.toString();
	}
	
	private String getPuzzle() throws IOException {
		URL url = new URL(String.format("%s/%s/%s/puzzle", BASE_URL, API_KEY, ENV));
		return makeRequest(url, null);
	}
	
	public boolean debugPrint;
	public int w, h;
	public int maxSize;
	public int numCells;
	public BitSet grid;
	public Map<Integer, List<Mask>> masks;
	private Set<Mask> best;
	
	public synchronized void updateBest(State s) {
		int numSquares = s.used.size();
		if (numSquares < best.size()) {
			best = new HashSet<Mask>();
			best.addAll(s.used);
			if (debugPrint) System.out.println(numSquares + ": " + best.toString());
		}
	}
	
	private Output solve(Input input) {
		w = input.width;
		h = input.height;
		maxSize = Math.min(w, h);
		numCells = w*h;
		
		// Create grid
		grid = new BitSet(numCells);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				if (!input.puzzle[y][x]) grid.set(y*w + x);
			}
		}
		
		// Generate masks
		masks = new TreeMap<Integer, List<Mask>>();
		for (int n = 1; n <= maxSize; n++) {
			List<Mask> list = new ArrayList<Mask>();
			for (int y = 0; y < h - (n-1); y++) {
				for (int x = 0; x < w - (n-1); x++) {
					Square square = new Square(x, y, n);
					BitSet bits = new BitSet(numCells);
					for (int yy = 0; yy < n; yy++) {
						for (int xx = 0; xx < n; xx++) {
							bits.set((y + yy) * w + (x + xx));
						}
					}
					list.add(new Mask(square, bits));
				}
			}
			masks.put(n, list);
		}
		
		// Find solutions
		best = new HashSet<Mask>();
		for (int i = 0; i < numCells; i++) {
			if (!grid.get(i)) best.add(masks.get(1).get(i));
		}
		List<Thread> threads = new ArrayList<Thread>();
		threads.add(new Thread(new Worker(this, 0.0, 8000)));
		threads.add(new Thread(new Worker(this, 1.0/2.0, 8000)));
		threads.add(new Thread(new Worker(this, 2.0/3.0, 8000)));
		threads.add(new Thread(new Worker(this, 3.0/4.0, 8000)));
		for (Thread t : threads) {
			t.start();
		}
		for (Thread t : threads) {
			try {
				t.join();
			} catch (InterruptedException e) {}
		}
		
		Output output = new Output(input.id);
		for (Mask mask : best) {
			output.squares.add(mask.square);
		}
		return output;
	}
	
	private String submitSolution(String json) throws IOException {
		URL url = new URL(String.format("%s/%s/%s/solution", BASE_URL, API_KEY, ENV));
		return makeRequest(url, json);
	}
	
	private void saveJSON(String s, File jsonFile) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(jsonFile));
		out.write(s);
		out.close();
	}
	
	private String loadJSON(File jsonFile) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader in = new BufferedReader(new FileReader(jsonFile));
		String line;
		while ((line = in.readLine()) != null) {
			sb.append(line + "\n");
		}
		in.close();
		return sb.toString();
	}
	
	private void saveImage(Input input, Output output, File imageFile) throws IOException {
		int w = input.width;
		int h = input.height;
		
		BufferedImage img = new BufferedImage(w * SCALE, h * SCALE, BufferedImage.TYPE_INT_RGB);
		Graphics g = img.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, w * SCALE, h * SCALE);
		g.setColor(Color.LIGHT_GRAY);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				g.drawRect(x * SCALE, y * SCALE, SCALE, SCALE);
			}
		}
		Collections.sort(output.squares);
		float hue = 0;
		for (Square s : output.squares) {
			g.setColor(Color.getHSBColor(hue, 0.5f, 0.95f));
			hue += PHI;
			hue %= 1;
			g.fillRect(s.X * SCALE, s.Y * SCALE, s.Size * SCALE, s.Size * SCALE);
			g.setColor(Color.BLACK);
			g.drawRect(s.X * SCALE, s.Y * SCALE, s.Size * SCALE, s.Size * SCALE);
		}
		g.dispose();
		ImageIO.write(img, "png", imageFile);
	}
	
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Error: No ID supplied");
			return;
		}
		try {
			String id = args[0];
			if (id.equals("new")) {
				new Solver();
			} else {
				new Solver(id);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Solver() throws IOException {
		// Main program
		System.out.println("Using API key: " + API_KEY);
		debugPrint = false;
		
		// Get a puzzle, and convert the returned JSON to a Java object
		String jsonInput = getPuzzle();
		long start = System.currentTimeMillis();
		ObjectMapper mapper = new ObjectMapper();
		Input input = mapper.readValue(jsonInput, Input.class);
		
		// Demonstrate some of the returned values
		System.out.println(String.format("You retrieved a puzzle with %d width x %d height and ID=%s", input.width, input.height, input.id));
		
		System.out.println("Generating solution");
		Output output = solve(input);
		
		System.out.println("Submitting solution");
		String jsonOutput = mapper.writeValueAsString(output);
		String jsonResult = submitSolution(jsonOutput);
		long end = System.currentTimeMillis();
		System.out.println(String.format("Took %.3f seconds", (end - start) / 1000.0));
		
		// Describe the response
		Result result = mapper.readValue(jsonResult, Result.class);
		if (result.errors.length > 0) {
			System.out.println(String.format("Your solution failed with %d problems and used %d squares.", result.errors.length, result.numberOfSquares));
		} else {
			System.out.println(String.format("Your solution succeeded with %d squares, for a score of %d, with a time penalty of %d.", result.numberOfSquares, result.score, result.timePenalty));
		}
		
		// Save data for debugging
		File data = new File("data");
		if (!data.exists()) data.mkdir();
		saveJSON(jsonInput, new File(data, String.format("%s-in.json", input.id)));
		saveJSON(jsonOutput, new File(data, String.format("%s-out.json", input.id)));
		saveJSON(jsonResult, new File(data, String.format("%s-res.json", input.id)));
		saveImage(input, output, new File(data, String.format("%s.png", input.id)));
	}
	
	public Solver(String id) throws IOException {
		// Main program
		File data = new File("data");
		debugPrint = true;
		
		// Get a puzzle, and convert the returned JSON to a Java object
		String jsonInput = loadJSON(new File(data, String.format("%s-in.json", id)));
		long start = System.currentTimeMillis();
		ObjectMapper mapper = new ObjectMapper();
		Input input = mapper.readValue(jsonInput, Input.class);
		
		// Demonstrate some of the returned values
		System.out.println(String.format("You retrieved a puzzle with %d width x %d height and ID=%s", input.width, input.height, input.id));
		
		System.out.println("Generating solution");
		Output output = solve(input);
		
		System.out.println("Outputting solution");
		//String jsonOutput = mapper.writeValueAsString(output);
		System.out.println(output.squares.toString());
		long end = System.currentTimeMillis();
		System.out.println(String.format("Took %.3f seconds", (end - start) / 1000.0));
		
		//saveJSON(jsonOutput, new File(data, String.format("%s-out.json", input.id)));
		//saveImage(input, output, new File(data, String.format("%s.png", input.id)));
	}
}
