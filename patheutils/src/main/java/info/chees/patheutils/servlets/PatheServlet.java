package info.chees.patheutils.servlets;

import info.chees.patheutils.ImdbParser;
import info.chees.patheutils.PatheParser;
import info.chees.patheutils.models.Movie;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.cloudstorage.RetryParams;

@SuppressWarnings("serial")
public class PatheServlet extends HttpServlet {
	
	private static final Logger log = Logger.getLogger(PatheServlet.class.getName());
	
	private static ObjectMapper mapper = new ObjectMapper();

	private final GcsService gcsService =
		      GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		List<Movie> movies = PatheParser.getMovies();
		for (Movie m : movies) {
			//if (Math.random() < 0.5) break;
			ImdbParser.fillImdbId(m);
			ImdbParser.fillRating(m);
		}
		
		Collections.sort(movies);
		
		for (Movie m : movies) {
			log.info(m.toString());
		}
		
		String json = mapper.writeValueAsString(movies);
		
		writeToGCS(json);
		
		resp.setContentType("text/plain");
		resp.getWriter().println("Done.");
		
		//req.setAttribute("movies", json);
		//req.getRequestDispatcher("/index.jsp").forward(req, resp);
	}
	
	private void writeToGCS(String json) throws IOException {
		String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		GcsFilename filename = new GcsFilename("patheutils.chees.info", "movies" + date + ".json");
		GcsFileOptions options = new GcsFileOptions.Builder()
				.mimeType("application/json")
				.cacheControl("Cache-Control: public, max-age=1") // TODO set higher cache
				.build();
		GcsOutputChannel outputChannel = gcsService.createOrReplace(filename, options);
		ByteBuffer buffer = ByteBuffer.wrap(json.getBytes("UTF-8"));
		outputChannel.write(buffer);
		outputChannel.close();
	}
}
