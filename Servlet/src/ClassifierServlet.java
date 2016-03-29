import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

@WebServlet(name = "ClassifierServlet", urlPatterns = {"/classify"})
public class ClassifierServlet extends HttpServlet {


    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {


        String jsonText = request.getParameter("text");
        String jsonFilename = request.getParameter("filename");

        appendToJsonFile(getServletContext().getRealPath("files/news-sources/" + jsonFilename + ".json"), jsonText);

        System.out.println(jsonText);
        System.out.println(jsonFilename);


        //Classify the new instances.
        classify(jsonFilename);

        //Update the indexer.
        sendPostRequest(jsonFilename + "-updated");

        //Send OK response.
        response.getWriter().println("OK");

    }

    private void classify(String filename) {
        try {
            String label = "";

            switch (filename) {
                case "bbc":
                    label = "BBC";
                    break;
                case "cnn":
                    label = "CNN";
                    break;
                case "guardian":
                    label = "Guardian";
                    break;
                case "straits-times":
                    label = "Straits Times";
                    break;
                case "reuters":
                    label = "Reuters";
            }

            FileConverter.convertJSONtoARFF(getServletContext().getRealPath("files/news-sources/" + filename + ".json"));
            ClassPredictor.predict(getServletContext().getRealPath("files/news-sources/" + filename + "arff"),
                    getServletContext().getRealPath("files/adaboost-smo-classifier.model"),
                    getServletContext().getRealPath("files/string-to-word-vector-filter.model"));
            Merger.merge(getServletContext().getRealPath("files/news-sources/" + filename + ".json"),
                    getServletContext().getRealPath("files/news-sources/" + filename + "-labelled.arff"), label);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private void sendPostRequest(String filename) {

        try {
            BufferedReader br = new BufferedReader(new FileReader(getServletContext().getRealPath("files/news-sources/" + filename + ".json")));
            String jsonString = IOUtils.toString(br);
            br.close();

            String data = URLEncoder.encode("text", "UTF-8") + "=" + URLEncoder.encode(jsonString, "UTF-8");
            data += "&" + URLEncoder.encode("filename", "UTF-8") + "=" + URLEncoder.encode(filename, "UTF-8");

//            URL url = new URL("http://192.168.109.3:3000/file");
            URL url = new URL("http://solr.kenrick95.xyz:3000/file");
            HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setDoOutput(true);
            httpCon.setRequestMethod("POST");
            OutputStreamWriter out = new OutputStreamWriter(
                    httpCon.getOutputStream());
            out.write(data);
            out.close();
            System.out.println(httpCon.getResponseMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void appendToJsonFile(String jsonFile, String jsonText) {
        try{
            File file =new File(jsonFile);

            //true = append file
            FileWriter fileWritter = new FileWriter(file.getName(), true);
            BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
            bufferWritter.write(jsonText.substring(0, jsonText.length()));
            bufferWritter.close();

            System.out.println("Done");

        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
